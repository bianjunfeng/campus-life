package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.AiModerationCheckRequest;
import com.campus.campus_life_ai.ai.dto.AiModerationCheckResponse;
import com.campus.campus_life_ai.ai.entity.AiCapabilityConfig;
import com.campus.campus_life_ai.ai.entity.AiSceneConfig;
import com.campus.campus_life_ai.ai.gateway.LlmGatewayFacade;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.mapper.AiCapabilityConfigMapper;
import com.campus.campus_life_ai.ai.mapper.AiSceneConfigMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ModerationGatewayService {

    private static final String CAPABILITY_CODE = "moderation";
    private static final String DEFAULT_SCENE_CODE = "moderation.text_post";
    private static final String SESSION_ID = "moderation-check";
    private static final String USER_ROLE = "user";
    private static final String DEFAULT_TARGET_TYPE = "text";

    private static final String FALLBACK_SYSTEM_PROMPT = """
            你是校园生活平台内容安全审核器。只返回 JSON，不要输出 Markdown。
            JSON 字段必须包含 result、score、categories、reason。
            result 只能是 PASS、REJECT、REVIEW；score 是 0 到 1 的风险分；
            categories 是字符串数组；reason 用中文简述原因。
            """;

    private final AiCapabilityConfigMapper aiCapabilityConfigMapper;
    private final AiSceneConfigMapper aiSceneConfigMapper;
    private final AiRuntimeConfigService aiRuntimeConfigService;
    private final AiProviderProperties aiProviderProperties;
    private final LlmGatewayFacade llmGatewayFacade;
    private final ObjectMapper objectMapper;

    public ModerationGatewayService(AiCapabilityConfigMapper aiCapabilityConfigMapper,
                                    AiSceneConfigMapper aiSceneConfigMapper,
                                    AiRuntimeConfigService aiRuntimeConfigService,
                                    AiProviderProperties aiProviderProperties,
                                    LlmGatewayFacade llmGatewayFacade,
                                    ObjectMapper objectMapper) {
        this.aiCapabilityConfigMapper = aiCapabilityConfigMapper;
        this.aiSceneConfigMapper = aiSceneConfigMapper;
        this.aiRuntimeConfigService = aiRuntimeConfigService;
        this.aiProviderProperties = aiProviderProperties;
        this.llmGatewayFacade = llmGatewayFacade;
        this.objectMapper = objectMapper;
    }

    public AiModerationCheckResponse check(Long userId, AiModerationCheckRequest request) {
        ModerationRoute route = resolveRoute(request);
        String requestId = "moderation-" + UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        GatewayResponse response = llmGatewayFacade.chat(GatewayRequest.builder()
                .requestId(requestId)
                .userId(userId)
                .sessionId(SESSION_ID)
                .capabilityCode(route.capabilityCode())
                .sceneCode(route.sceneCode())
                .providerCode(route.providerCode())
                .modelCode(route.modelCode())
                .runtimeConfig(route.runtimeConfig())
                .command(buildCommand(request, route))
                .build());

        ModerationDecision decision = parseDecision(response.getContent());
        return AiModerationCheckResponse.builder()
                .requestId(requestId)
                .targetType(firstNonBlank(request.getTargetType(), DEFAULT_TARGET_TYPE))
                .result(decision.result())
                .score(decision.score())
                .categories(decision.categories())
                .reason(decision.reason())
                .rawResponse(response.getContent())
                .providerCode(response.getProviderCode())
                .modelCode(response.getModelCode())
                .capabilityCode(route.capabilityCode())
                .sceneCode(route.sceneCode())
                .latencyMs(response.getLatencyMs())
                .fallbackLevel(response.getFallbackLevel())
                .createdAt(createdAt)
                .build();
    }

    private ModerationRoute resolveRoute(AiModerationCheckRequest request) {
        String sceneCode = firstNonBlank(request.getSceneCode(), DEFAULT_SCENE_CODE);
        AiSceneConfig sceneConfig = requireEnabledScene(sceneCode);
        AiCapabilityConfig capabilityConfig = requireEnabledCapability(sceneConfig.getCapabilityCode());
        if (!CAPABILITY_CODE.equals(capabilityConfig.getCapabilityCode())) {
            throw new IllegalArgumentException("当前审核接口仅支持 moderation 能力");
        }

        String providerCode = firstNonBlank(
                request.getProviderCode(),
                sceneConfig.getProviderCode(),
                aiProviderProperties.getDefaultProviderCode()
        );
        ProviderRuntimeConfig baseRuntimeConfig = aiRuntimeConfigService.resolveProvider(providerCode);
        String modelCode = firstNonBlank(
                request.getModelCode(),
                sceneConfig.getModelCode(),
                baseRuntimeConfig.getDefaultModelCode(),
                aiProviderProperties.getDefaultModelCode()
        );

        ProviderRuntimeConfig runtimeConfig = ProviderRuntimeConfig.builder()
                .providerCode(baseRuntimeConfig.getProviderCode())
                .providerName(baseRuntimeConfig.getProviderName())
                .enabled(baseRuntimeConfig.getEnabled())
                .baseUrl(baseRuntimeConfig.getBaseUrl())
                .apiKey(baseRuntimeConfig.getApiKey())
                .defaultModelCode(modelCode)
                .maxContextMessages(baseRuntimeConfig.getMaxContextMessages())
                .temperature(0.0D)
                .maxOutputTokens(firstNonNull(baseRuntimeConfig.getMaxOutputTokens(), 512))
                .systemPrompt(firstNonBlank(sceneConfig.getSystemPromptTemplate(), baseRuntimeConfig.getSystemPrompt(), FALLBACK_SYSTEM_PROMPT))
                .connectTimeoutMs(baseRuntimeConfig.getConnectTimeoutMs())
                .readTimeoutMs(firstNonNull(sceneConfig.getTimeoutMs(), baseRuntimeConfig.getReadTimeoutMs()))
                .build();

        return new ModerationRoute(
                capabilityConfig.getCapabilityCode(),
                sceneConfig.getSceneCode(),
                runtimeConfig.getProviderCode(),
                runtimeConfig.getDefaultModelCode(),
                runtimeConfig
        );
    }

    private AiSceneConfig requireEnabledScene(String sceneCode) {
        AiSceneConfig sceneConfig = aiSceneConfigMapper.findBySceneCode(sceneCode);
        if (sceneConfig == null) {
            throw new IllegalArgumentException("AI 审核场景不存在: " + sceneCode);
        }
        if (sceneConfig.getEnabled() == null || sceneConfig.getEnabled() != 1) {
            throw new IllegalArgumentException("AI 审核场景未启用: " + sceneCode);
        }
        return sceneConfig;
    }

    private AiCapabilityConfig requireEnabledCapability(String capabilityCode) {
        AiCapabilityConfig capabilityConfig = aiCapabilityConfigMapper.findByCapabilityCode(capabilityCode);
        if (capabilityConfig == null) {
            throw new IllegalArgumentException("AI 审核能力不存在: " + capabilityCode);
        }
        if (capabilityConfig.getEnabled() == null || capabilityConfig.getEnabled() != 1) {
            throw new IllegalArgumentException("AI 审核能力未启用: " + capabilityCode);
        }
        return capabilityConfig;
    }

    private ChatCompletionCommand buildCommand(AiModerationCheckRequest request,
                                               ModerationRoute route) {
        ProviderRuntimeConfig runtimeConfig = route.runtimeConfig();
        String targetType = firstNonBlank(request.getTargetType(), DEFAULT_TARGET_TYPE);
        String userContent = """
                请审核以下校园生活平台内容是否可以发布。
                内容类型：%s
                待审核文本：
                %s
                """.formatted(targetType, request.getContent());

        return ChatCompletionCommand.builder()
                .providerCode(runtimeConfig.getProviderCode())
                .modelCode(runtimeConfig.getDefaultModelCode())
                .systemPrompt(runtimeConfig.getSystemPrompt())
                .temperature(runtimeConfig.getTemperature())
                .maxOutputTokens(runtimeConfig.getMaxOutputTokens())
                .messages(List.of(ChatCompletionCommand.PromptMessage.builder()
                        .role(USER_ROLE)
                        .content(userContent)
                        .build()))
                .toolContext(java.util.Map.of(
                        "capabilityCode", route.capabilityCode(),
                        "sceneCode", route.sceneCode(),
                        "source", SESSION_ID
                ))
                .build();
    }

    private ModerationDecision parseDecision(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return ModerationDecision.review("模型未返回审核结果", List.of("empty_response"), 0.5D);
        }
        try {
            JsonNode root = objectMapper.readTree(extractJson(rawContent));
            String result = normalizeResult(firstNonBlank(
                    text(root, "result"),
                    text(root, "decision"),
                    text(root, "status"),
                    text(root, "action")
            ));
            double score = clamp(number(root, "score", number(root, "riskScore", defaultScore(result))));
            List<String> categories = categories(root);
            String reason = firstNonBlank(text(root, "reason"), text(root, "message"), defaultReason(result));
            return new ModerationDecision(result, score, categories, reason);
        } catch (Exception ignored) {
            return ModerationDecision.review("模型返回无法解析，建议人工复核", List.of("parse_error"), 0.5D);
        }
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String normalizeResult(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
        if (Set.of("PASS", "ALLOW", "SAFE", "OK", "通过", "合规").contains(normalized)) {
            return "PASS";
        }
        if (Set.of("REJECT", "BLOCK", "DENY", "FAIL", "FAILED", "不通过", "拒绝", "违规").contains(normalized)) {
            return "REJECT";
        }
        return "REVIEW";
    }

    private double defaultScore(String result) {
        if ("PASS".equals(result)) {
            return 0D;
        }
        if ("REJECT".equals(result)) {
            return 1D;
        }
        return 0.5D;
    }

    private String defaultReason(String result) {
        if ("PASS".equals(result)) {
            return "未发现明显违规风险";
        }
        if ("REJECT".equals(result)) {
            return "检测到高风险内容";
        }
        return "需要人工复核";
    }

    private List<String> categories(JsonNode root) {
        Set<String> categories = new LinkedHashSet<>();
        collectCategories(categories, root.path("categories"));
        collectCategories(categories, root.path("category"));
        return categories.isEmpty() ? List.of("general") : List.copyOf(categories);
    }

    private void collectCategories(Set<String> categories, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                addCategory(categories, item.asText(null));
            }
            return;
        }
        if (node.isTextual()) {
            for (String item : node.asText().split("[,，]")) {
                addCategory(categories, item);
            }
        }
    }

    private void addCategory(Set<String> categories, String value) {
        if (StringUtils.hasText(value)) {
            categories.add(value.trim());
        }
    }

    private String text(JsonNode root, String fieldName) {
        JsonNode value = root == null ? null : root.path(fieldName);
        return value == null || value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }

    private double number(JsonNode root, String fieldName, double defaultValue) {
        JsonNode value = root == null ? null : root.path(fieldName);
        return value == null || !value.isNumber() ? defaultValue : value.asDouble(defaultValue);
    }

    private double clamp(double value) {
        return Math.max(0D, Math.min(1D, value));
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private Integer firstNonNull(Integer... candidates) {
        for (Integer candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private record ModerationRoute(String capabilityCode,
                                   String sceneCode,
                                   String providerCode,
                                   String modelCode,
                                   ProviderRuntimeConfig runtimeConfig) {
    }

    private record ModerationDecision(String result,
                                      Double score,
                                      List<String> categories,
                                      String reason) {
        private static ModerationDecision review(String reason, List<String> categories, Double score) {
            return new ModerationDecision("REVIEW", score, new ArrayList<>(categories), reason);
        }
    }
}
