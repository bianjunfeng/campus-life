package com.campus.campus_life_ai.ai.gateway.route;

import com.campus.campus_life_ai.ai.entity.AiGatewayRouteRule;
import com.campus.campus_life_ai.ai.entity.AiModelConfig;
import com.campus.campus_life_ai.ai.gateway.GatewayCallException;
import com.campus.campus_life_ai.ai.gateway.GatewayErrorCode;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.mapper.AiGatewayRouteRuleMapper;
import com.campus.campus_life_ai.ai.mapper.AiModelConfigMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.service.AiRuntimeConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ModelRoutePlanner {

    private final AiGatewayRouteRuleMapper aiGatewayRouteRuleMapper;
    private final AiModelConfigMapper aiModelConfigMapper;
    private final AiRuntimeConfigService aiRuntimeConfigService;
    private final ObjectMapper objectMapper;

    public ModelRoutePlanner(AiGatewayRouteRuleMapper aiGatewayRouteRuleMapper,
                             AiModelConfigMapper aiModelConfigMapper,
                             AiRuntimeConfigService aiRuntimeConfigService,
                             ObjectMapper objectMapper) {
        this.aiGatewayRouteRuleMapper = aiGatewayRouteRuleMapper;
        this.aiModelConfigMapper = aiModelConfigMapper;
        this.aiRuntimeConfigService = aiRuntimeConfigService;
        this.objectMapper = objectMapper;
    }

    public RoutePlan plan(GatewayRequest request) {
        if (request == null || request.getRuntimeConfig() == null || request.getCommand() == null) {
            throw routeUnavailable("缺少可用的大模型路由配置");
        }

        List<RouteCandidate> rawCandidates = new ArrayList<>();
        rawCandidates.add(RouteCandidate.builder()
                .providerCode(request.getProviderCode())
                .modelCode(request.getModelCode())
                .runtimeConfig(request.getRuntimeConfig())
                .command(request.getCommand())
                .build());
        if (request.getFallbackCandidates() != null) {
            rawCandidates.addAll(request.getFallbackCandidates());
        }
        RuleCandidates ruleCandidates = loadRuleCandidates(request);
        if (!ruleCandidates.replacePrimaryCandidates().isEmpty()) {
            rawCandidates.addAll(0, ruleCandidates.replacePrimaryCandidates());
        }
        rawCandidates.addAll(ruleCandidates.fallbackCandidates());

        Map<String, RouteCandidate> deduplicated = new LinkedHashMap<>();
        int fallbackLevel = 0;
        for (RouteCandidate rawCandidate : rawCandidates) {
            RouteCandidate normalized = normalize(request, rawCandidate, fallbackLevel++);
            deduplicated.putIfAbsent(key(normalized), normalized);
        }
        if (deduplicated.isEmpty()) {
            throw routeUnavailable("未规划到可用的大模型候选路由");
        }
        return RoutePlan.builder().candidates(List.copyOf(deduplicated.values())).build();
    }

    private RuleCandidates loadRuleCandidates(GatewayRequest request) {
        if (!StringUtils.hasText(request.getCapabilityCode())) {
            return RuleCandidates.empty();
        }
        List<AiGatewayRouteRule> rules = aiGatewayRouteRuleMapper.findEnabledByCapabilityCode(request.getCapabilityCode());
        if (rules == null || rules.isEmpty()) {
            return RuleCandidates.empty();
        }

        List<RouteCandidate> replacements = new ArrayList<>();
        List<RouteCandidate> fallbacks = new ArrayList<>();
        for (AiGatewayRouteRule rule : rules) {
            if (!sceneMatches(rule, request.getSceneCode())) {
                continue;
            }
            ParsedRule parsedRule = parseRule(rule);
            if (parsedRule.replacePrimary()) {
                replacements.addAll(parsedRule.candidates());
            } else {
                fallbacks.addAll(parsedRule.candidates());
            }
        }
        return new RuleCandidates(replacements, fallbacks);
    }

    private ParsedRule parseRule(AiGatewayRouteRule rule) {
        if (rule == null || !StringUtils.hasText(rule.getRouteRuleJson())) {
            return ParsedRule.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(rule.getRouteRuleJson());
            JsonNode candidates = root.isArray() ? root : root.path("candidates");
            if (!candidates.isArray()) {
                return ParsedRule.empty();
            }

            List<RouteCandidate> routeCandidates = new ArrayList<>();
            for (JsonNode candidateNode : candidates) {
                RouteCandidate candidate = parseCandidate(candidateNode);
                if (candidate != null) {
                    routeCandidates.add(candidate);
                }
            }
            boolean replacePrimary = !root.isArray() && root.path("replacePrimary").asBoolean(false);
            return new ParsedRule(replacePrimary, routeCandidates);
        } catch (Exception ignored) {
            return ParsedRule.empty();
        }
    }

    private RouteCandidate parseCandidate(JsonNode candidateNode) {
        String providerCode = candidateNode == null ? null : candidateNode.path("providerCode").asText(null);
        String modelCode = candidateNode == null ? null : candidateNode.path("modelCode").asText(null);
        if (!StringUtils.hasText(providerCode) || !StringUtils.hasText(modelCode)) {
            return null;
        }
        AiModelConfig modelConfig = aiModelConfigMapper.findEnabledByProviderAndModelCode(providerCode.trim(), modelCode.trim());
        if (modelConfig == null) {
            return null;
        }
        ProviderRuntimeConfig runtimeConfig = aiRuntimeConfigService.resolveProvider(providerCode.trim());
        return RouteCandidate.builder()
                .providerCode(providerCode.trim())
                .modelCode(modelCode.trim())
                .runtimeConfig(copyRuntimeConfig(runtimeConfig, providerCode.trim(), modelCode.trim(), modelConfig.getMaxOutputTokens()))
                .build();
    }

    private boolean sceneMatches(AiGatewayRouteRule rule, String sceneCode) {
        return rule != null
                && (!StringUtils.hasText(rule.getSceneCode())
                || rule.getSceneCode().trim().equals(sceneCode));
    }

    private RouteCandidate normalize(GatewayRequest request, RouteCandidate rawCandidate, int fallbackLevel) {
        RouteCandidate candidate = rawCandidate == null ? RouteCandidate.builder().build() : rawCandidate;
        ProviderRuntimeConfig sourceRuntimeConfig = candidate.getRuntimeConfig() == null
                ? request.getRuntimeConfig()
                : candidate.getRuntimeConfig();
        String providerCode = firstNonBlank(
                candidate.getProviderCode(),
                sourceRuntimeConfig == null ? null : sourceRuntimeConfig.getProviderCode(),
                request.getProviderCode(),
                request.getRuntimeConfig().getProviderCode()
        );
        String modelCode = firstNonBlank(
                candidate.getModelCode(),
                sourceRuntimeConfig == null ? null : sourceRuntimeConfig.getDefaultModelCode(),
                request.getModelCode(),
                request.getRuntimeConfig().getDefaultModelCode()
        );
        if (!StringUtils.hasText(providerCode) || !StringUtils.hasText(modelCode)) {
            throw routeUnavailable("候选路由缺少 Provider 或模型编码");
        }
        if (candidate.getRuntimeConfig() == null
                && StringUtils.hasText(candidate.getProviderCode())
                && !providerCode.equals(request.getRuntimeConfig().getProviderCode())) {
            throw routeUnavailable("跨 Provider 后备路由缺少运行时配置: " + providerCode);
        }

        ProviderRuntimeConfig runtimeConfig = copyRuntimeConfig(sourceRuntimeConfig, providerCode, modelCode, null);
        ChatCompletionCommand command = copyCommand(
                candidate.getCommand() == null ? request.getCommand() : candidate.getCommand(),
                providerCode,
                modelCode
        );
        return RouteCandidate.builder()
                .providerCode(providerCode)
                .modelCode(modelCode)
                .runtimeConfig(runtimeConfig)
                .command(command)
                .fallbackLevel(fallbackLevel)
                .build();
    }

    private ProviderRuntimeConfig copyRuntimeConfig(ProviderRuntimeConfig source,
                                                    String providerCode,
                                                    String modelCode,
                                                    Integer maxOutputTokens) {
        if (source == null) {
            throw routeUnavailable("候选路由缺少 Provider 运行时配置");
        }
        return ProviderRuntimeConfig.builder()
                .providerCode(providerCode)
                .providerName(source.getProviderName())
                .enabled(source.getEnabled())
                .baseUrl(source.getBaseUrl())
                .apiKey(source.getApiKey())
                .defaultModelCode(modelCode)
                .maxContextMessages(source.getMaxContextMessages())
                .temperature(source.getTemperature())
                .maxOutputTokens(maxOutputTokens == null ? source.getMaxOutputTokens() : maxOutputTokens)
                .systemPrompt(source.getSystemPrompt())
                .connectTimeoutMs(source.getConnectTimeoutMs())
                .readTimeoutMs(source.getReadTimeoutMs())
                .build();
    }

    private ChatCompletionCommand copyCommand(ChatCompletionCommand source, String providerCode, String modelCode) {
        if (source == null) {
            throw routeUnavailable("候选路由缺少调用命令");
        }
        return ChatCompletionCommand.builder()
                .providerCode(providerCode)
                .modelCode(modelCode)
                .systemPrompt(source.getSystemPrompt())
                .temperature(source.getTemperature())
                .maxOutputTokens(source.getMaxOutputTokens())
                .messages(source.getMessages())
                .toolCallbacks(source.getToolCallbacks())
                .toolContext(source.getToolContext())
                .build();
    }

    private String key(RouteCandidate candidate) {
        return candidate.getProviderCode() + ":" + candidate.getModelCode();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private GatewayCallException routeUnavailable(String message) {
        return new GatewayCallException(GatewayError.builder()
                .errorCode(GatewayErrorCode.ROUTE_UNAVAILABLE.getCode())
                .errorType(GatewayErrorCode.ROUTE_UNAVAILABLE.getType())
                .message(message)
                .build(), null);
    }

    private record RuleCandidates(List<RouteCandidate> replacePrimaryCandidates,
                                  List<RouteCandidate> fallbackCandidates) {
        private static RuleCandidates empty() {
            return new RuleCandidates(List.of(), List.of());
        }
    }

    private record ParsedRule(boolean replacePrimary, List<RouteCandidate> candidates) {
        private static ParsedRule empty() {
            return new ParsedRule(false, List.of());
        }
    }
}
