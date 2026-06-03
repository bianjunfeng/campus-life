package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.SendMessageRequest;
import com.campus.campus_life_ai.ai.entity.AiCapabilityConfig;
import com.campus.campus_life_ai.ai.entity.AiConversation;
import com.campus.campus_life_ai.ai.entity.AiSceneConfig;
import com.campus.campus_life_ai.ai.mapper.AiCapabilityConfigMapper;
import com.campus.campus_life_ai.ai.mapper.AiSceneConfigMapper;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiChatRouteResolver {

    static final String CHAT_CAPABILITY_CODE = "chat";
    static final String DEFAULT_CHAT_SCENE_CODE = "chat.general";

    private final AiCapabilityConfigMapper aiCapabilityConfigMapper;
    private final AiSceneConfigMapper aiSceneConfigMapper;
    private final AiRuntimeConfigService aiRuntimeConfigService;
    private final AiProviderProperties aiProviderProperties;

    public AiChatRouteResolver(AiCapabilityConfigMapper aiCapabilityConfigMapper,
                               AiSceneConfigMapper aiSceneConfigMapper,
                               AiRuntimeConfigService aiRuntimeConfigService,
                               AiProviderProperties aiProviderProperties) {
        this.aiCapabilityConfigMapper = aiCapabilityConfigMapper;
        this.aiSceneConfigMapper = aiSceneConfigMapper;
        this.aiRuntimeConfigService = aiRuntimeConfigService;
        this.aiProviderProperties = aiProviderProperties;
    }

    public AiChatRoute resolve(AiConversation conversation, SendMessageRequest request) {
        String requestedSceneCode = firstNonBlank(
                request.getSceneCode(),
                conversation == null ? null : conversation.getSceneCode(),
                DEFAULT_CHAT_SCENE_CODE
        );
        AiSceneConfig sceneConfig = requireEnabledScene(requestedSceneCode);

        String capabilityCode = firstNonBlank(
                request.getCapabilityCode(),
                sceneConfig.getCapabilityCode(),
                conversation == null ? null : conversation.getCapabilityCode(),
                CHAT_CAPABILITY_CODE
        );
        AiCapabilityConfig capabilityConfig = requireEnabledCapability(capabilityCode);
        if (!CHAT_CAPABILITY_CODE.equals(capabilityConfig.getCapabilityCode())) {
            throw new IllegalArgumentException("当前聊天接口仅支持 chat 能力");
        }
        if (!capabilityConfig.getCapabilityCode().equals(sceneConfig.getCapabilityCode())) {
            throw new IllegalArgumentException("场景与能力不匹配: " + requestedSceneCode);
        }

        boolean sceneChanged = isSceneChanged(conversation, request);
        String providerCode = sceneChanged
                ? firstNonBlank(
                request.getProviderCode(),
                sceneConfig.getProviderCode(),
                conversation == null ? null : conversation.getProviderCode(),
                aiProviderProperties.getDefaultProviderCode()
        )
                : firstNonBlank(
                request.getProviderCode(),
                conversation == null ? null : conversation.getProviderCode(),
                sceneConfig.getProviderCode(),
                aiProviderProperties.getDefaultProviderCode()
        );

        ProviderRuntimeConfig baseRuntimeConfig = aiRuntimeConfigService.resolveProvider(providerCode);
        String modelCode = sceneChanged
                ? firstNonBlank(
                request.getModelCode(),
                sceneConfig.getModelCode(),
                conversation == null ? null : conversation.getModelCode(),
                baseRuntimeConfig.getDefaultModelCode(),
                aiProviderProperties.getDefaultModelCode()
        )
                : firstNonBlank(
                request.getModelCode(),
                conversation == null ? null : conversation.getModelCode(),
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
                .temperature(baseRuntimeConfig.getTemperature())
                .maxOutputTokens(baseRuntimeConfig.getMaxOutputTokens())
                .systemPrompt(firstNonBlank(
                        sceneConfig.getSystemPromptTemplate(),
                        baseRuntimeConfig.getSystemPrompt(),
                        aiProviderProperties.getSystemPrompt()
                ))
                .connectTimeoutMs(baseRuntimeConfig.getConnectTimeoutMs())
                .readTimeoutMs(firstNonNull(sceneConfig.getTimeoutMs(), baseRuntimeConfig.getReadTimeoutMs()))
                .build();

        return AiChatRoute.builder()
                .assistantType(firstNonBlank(
                        conversation == null ? null : conversation.getAssistantType(),
                        aiProviderProperties.getDefaultAssistantType()
                ))
                .capabilityCode(capabilityConfig.getCapabilityCode())
                .sceneCode(sceneConfig.getSceneCode())
                .providerCode(runtimeConfig.getProviderCode())
                .modelCode(runtimeConfig.getDefaultModelCode())
                .runtimeConfig(runtimeConfig)
                .build();
    }

    private AiSceneConfig requireEnabledScene(String sceneCode) {
        AiSceneConfig sceneConfig = aiSceneConfigMapper.findBySceneCode(sceneCode);
        if (sceneConfig == null) {
            throw new IllegalArgumentException("AI 场景不存在: " + sceneCode);
        }
        if (sceneConfig.getEnabled() == null || sceneConfig.getEnabled() != 1) {
            throw new IllegalArgumentException("AI 场景未启用: " + sceneCode);
        }
        return sceneConfig;
    }

    private AiCapabilityConfig requireEnabledCapability(String capabilityCode) {
        AiCapabilityConfig capabilityConfig = aiCapabilityConfigMapper.findByCapabilityCode(capabilityCode);
        if (capabilityConfig == null) {
            throw new IllegalArgumentException("AI 能力不存在: " + capabilityCode);
        }
        if (capabilityConfig.getEnabled() == null || capabilityConfig.getEnabled() != 1) {
            throw new IllegalArgumentException("AI 能力未启用: " + capabilityCode);
        }
        return capabilityConfig;
    }

    private boolean isSceneChanged(AiConversation conversation, SendMessageRequest request) {
        if (!StringUtils.hasText(request.getSceneCode())) {
            return false;
        }
        if (conversation == null || !StringUtils.hasText(conversation.getSceneCode())) {
            return true;
        }
        return !request.getSceneCode().trim().equals(conversation.getSceneCode());
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
}
