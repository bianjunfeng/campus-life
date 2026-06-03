package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.AiGatewayProbeRequest;
import com.campus.campus_life_ai.ai.dto.AiGatewayProbeResponse;
import com.campus.campus_life_ai.ai.dto.AiProviderHealthCheckResponse;
import com.campus.campus_life_ai.ai.dto.SendMessageRequest;
import com.campus.campus_life_ai.ai.entity.AiProviderConfig;
import com.campus.campus_life_ai.ai.gateway.GatewayCallException;
import com.campus.campus_life_ai.ai.gateway.GatewayErrorCode;
import com.campus.campus_life_ai.ai.gateway.LlmGatewayFacade;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayUsage;
import com.campus.campus_life_ai.ai.mapper.AiProviderConfigMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.LlmProvider;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.provider.ProviderSelector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AdminAiGatewayProbeService {

    private static final String ADMIN_PROBE_SESSION = "admin-gateway-probe";
    private static final String USER_ROLE = "user";

    private final AiProviderConfigMapper aiProviderConfigMapper;
    private final AiRuntimeConfigService aiRuntimeConfigService;
    private final ProviderSelector providerSelector;
    private final AiChatRouteResolver aiChatRouteResolver;
    private final LlmGatewayFacade llmGatewayFacade;

    public AdminAiGatewayProbeService(AiProviderConfigMapper aiProviderConfigMapper,
                                      AiRuntimeConfigService aiRuntimeConfigService,
                                      ProviderSelector providerSelector,
                                      AiChatRouteResolver aiChatRouteResolver,
                                      LlmGatewayFacade llmGatewayFacade) {
        this.aiProviderConfigMapper = aiProviderConfigMapper;
        this.aiRuntimeConfigService = aiRuntimeConfigService;
        this.providerSelector = providerSelector;
        this.aiChatRouteResolver = aiChatRouteResolver;
        this.llmGatewayFacade = llmGatewayFacade;
    }

    public AiProviderHealthCheckResponse checkProvider(Long providerId) {
        AiProviderConfig config = aiProviderConfigMapper.findById(providerId);
        if (config == null) {
            throw new IllegalArgumentException("Provider 不存在: " + providerId);
        }

        LocalDateTime checkedAt = LocalDateTime.now();
        try {
            ProviderRuntimeConfig runtimeConfig = aiRuntimeConfigService.resolveProvider(config.getProviderCode());
            LlmProvider provider = providerSelector.resolve(runtimeConfig.getProviderCode());
            boolean available = provider.isAvailable(runtimeConfig);
            return AiProviderHealthCheckResponse.builder()
                    .providerCode(runtimeConfig.getProviderCode())
                    .providerName(runtimeConfig.getProviderName())
                    .modelCode(runtimeConfig.getDefaultModelCode())
                    .enabled(Boolean.TRUE.equals(runtimeConfig.getEnabled()))
                    .available(available)
                    .apiKeyConfigured(StringUtils.hasText(runtimeConfig.getApiKey()))
                    .baseUrl(runtimeConfig.getBaseUrl())
                    .message(available ? "Provider 配置可用" : "Provider 未启用或缺少必要配置")
                    .checkedAt(checkedAt)
                    .build();
        } catch (RuntimeException e) {
            return AiProviderHealthCheckResponse.builder()
                    .providerCode(config.getProviderCode())
                    .providerName(config.getProviderName())
                    .modelCode(config.getDefaultModelCode())
                    .enabled(config.getEnabled() != null && config.getEnabled() == 1)
                    .available(false)
                    .apiKeyConfigured(StringUtils.hasText(config.getApiKeyCipher()))
                    .baseUrl(config.getBaseUrl())
                    .message(StringUtils.hasText(e.getMessage()) ? e.getMessage() : "Provider 健康检查失败")
                    .checkedAt(checkedAt)
                    .build();
        }
    }

    public AiGatewayProbeResponse probe(Long userId, AiGatewayProbeRequest request) {
        String requestId = "probe-" + UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        try {
            AiChatRoute route = aiChatRouteResolver.resolve(null, toSendMessageRequest(request));
            ProviderRuntimeConfig runtimeConfig = overrideRuntime(route.getRuntimeConfig(), request);
            GatewayResponse response = llmGatewayFacade.chat(GatewayRequest.builder()
                    .requestId(requestId)
                    .userId(userId)
                    .sessionId(ADMIN_PROBE_SESSION)
                    .capabilityCode(route.getCapabilityCode())
                    .sceneCode(route.getSceneCode())
                    .providerCode(route.getProviderCode())
                    .modelCode(runtimeConfig.getDefaultModelCode())
                    .runtimeConfig(runtimeConfig)
                    .command(buildCommand(request, route, runtimeConfig))
                    .build());
            return success(requestId, route, response, createdAt);
        } catch (GatewayCallException e) {
            return failure(requestId, request, e.getError(), e.getMessage(), createdAt);
        } catch (RuntimeException e) {
            GatewayError error = GatewayError.builder()
                    .errorCode(GatewayErrorCode.INVALID_REQUEST.getCode())
                    .errorType(GatewayErrorCode.INVALID_REQUEST.getType())
                    .message(StringUtils.hasText(e.getMessage()) ? e.getMessage() : "网关测试请求失败")
                    .build();
            return failure(requestId, request, error, e.getMessage(), createdAt);
        }
    }

    private SendMessageRequest toSendMessageRequest(AiGatewayProbeRequest request) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setContent(request.getContent());
        sendMessageRequest.setCapabilityCode(trimToNull(request.getCapabilityCode()));
        sendMessageRequest.setSceneCode(trimToNull(request.getSceneCode()));
        sendMessageRequest.setProviderCode(trimToNull(request.getProviderCode()));
        sendMessageRequest.setModelCode(trimToNull(request.getModelCode()));
        return sendMessageRequest;
    }

    private ProviderRuntimeConfig overrideRuntime(ProviderRuntimeConfig runtimeConfig, AiGatewayProbeRequest request) {
        return ProviderRuntimeConfig.builder()
                .providerCode(runtimeConfig.getProviderCode())
                .providerName(runtimeConfig.getProviderName())
                .enabled(runtimeConfig.getEnabled())
                .baseUrl(runtimeConfig.getBaseUrl())
                .apiKey(runtimeConfig.getApiKey())
                .defaultModelCode(firstNonBlank(request.getModelCode(), runtimeConfig.getDefaultModelCode()))
                .maxContextMessages(runtimeConfig.getMaxContextMessages())
                .temperature(request.getTemperature() == null ? runtimeConfig.getTemperature() : request.getTemperature())
                .maxOutputTokens(request.getMaxOutputTokens() == null ? runtimeConfig.getMaxOutputTokens() : request.getMaxOutputTokens())
                .systemPrompt(runtimeConfig.getSystemPrompt())
                .connectTimeoutMs(runtimeConfig.getConnectTimeoutMs())
                .readTimeoutMs(runtimeConfig.getReadTimeoutMs())
                .build();
    }

    private ChatCompletionCommand buildCommand(AiGatewayProbeRequest request,
                                               AiChatRoute route,
                                               ProviderRuntimeConfig runtimeConfig) {
        return ChatCompletionCommand.builder()
                .providerCode(runtimeConfig.getProviderCode())
                .modelCode(runtimeConfig.getDefaultModelCode())
                .systemPrompt(runtimeConfig.getSystemPrompt())
                .temperature(runtimeConfig.getTemperature())
                .maxOutputTokens(runtimeConfig.getMaxOutputTokens())
                .messages(List.of(ChatCompletionCommand.PromptMessage.builder()
                        .role(USER_ROLE)
                        .content(request.getContent())
                        .build()))
                .toolContext(java.util.Map.of(
                        "capabilityCode", route.getCapabilityCode(),
                        "sceneCode", route.getSceneCode(),
                        "source", ADMIN_PROBE_SESSION
                ))
                .build();
    }

    private AiGatewayProbeResponse success(String requestId,
                                           AiChatRoute route,
                                           GatewayResponse response,
                                           LocalDateTime createdAt) {
        GatewayUsage usage = response.getUsage();
        return AiGatewayProbeResponse.builder()
                .requestId(requestId)
                .content(response.getContent())
                .success(true)
                .providerCode(response.getProviderCode())
                .modelCode(response.getModelCode())
                .capabilityCode(route.getCapabilityCode())
                .sceneCode(route.getSceneCode())
                .promptTokens(usage == null ? null : usage.getPromptTokens())
                .completionTokens(usage == null ? null : usage.getCompletionTokens())
                .totalTokens(usage == null ? null : usage.getTotalTokens())
                .costAmount(usage == null ? null : usage.getCostAmount())
                .latencyMs(response.getLatencyMs())
                .fallbackLevel(response.getFallbackLevel())
                .finishReason(response.getFinishReason())
                .createdAt(createdAt)
                .build();
    }

    private AiGatewayProbeResponse failure(String requestId,
                                           AiGatewayProbeRequest request,
                                           GatewayError error,
                                           String fallbackMessage,
                                           LocalDateTime createdAt) {
        String message = error == null || !StringUtils.hasText(error.getMessage())
                ? fallbackMessage
                : error.getMessage();
        return AiGatewayProbeResponse.builder()
                .requestId(requestId)
                .success(false)
                .providerCode(trimToNull(request.getProviderCode()))
                .modelCode(trimToNull(request.getModelCode()))
                .capabilityCode(firstNonBlank(request.getCapabilityCode(), "chat"))
                .sceneCode(firstNonBlank(request.getSceneCode(), "chat.general"))
                .errorCode(error == null ? GatewayErrorCode.PROVIDER_ERROR.getCode() : error.getErrorCode())
                .errorType(error == null ? GatewayErrorCode.PROVIDER_ERROR.getType() : error.getErrorType())
                .errorMessage(StringUtils.hasText(message) ? message : "网关测试请求失败")
                .createdAt(createdAt)
                .build();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
