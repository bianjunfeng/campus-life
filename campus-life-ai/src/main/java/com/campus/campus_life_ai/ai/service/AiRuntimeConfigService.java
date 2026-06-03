package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.entity.AiProviderConfig;
import com.campus.campus_life_ai.ai.mapper.AiProviderConfigMapper;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import com.campus.campus_life_ai.common.security.ApiKeyCipherService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiRuntimeConfigService {

    private final AiProviderConfigMapper aiProviderConfigMapper;
    private final AiProviderProperties aiProviderProperties;
    private final ApiKeyCipherService apiKeyCipherService;

    public AiRuntimeConfigService(AiProviderConfigMapper aiProviderConfigMapper,
                                  AiProviderProperties aiProviderProperties,
                                  ApiKeyCipherService apiKeyCipherService) {
        this.aiProviderConfigMapper = aiProviderConfigMapper;
        this.aiProviderProperties = aiProviderProperties;
        this.apiKeyCipherService = apiKeyCipherService;
    }

    public ProviderRuntimeConfig resolveProvider(String requestedProviderCode) {
        String effectiveProviderCode = StringUtils.hasText(requestedProviderCode)
                ? requestedProviderCode.trim()
                : aiProviderProperties.getDefaultProviderCode();
        AiProviderConfig dbConfig = aiProviderConfigMapper.findByProviderCode(effectiveProviderCode);
        AiProviderProperties.OpenAiCompatibleProperties fallback = aiProviderProperties.getOpenaiCompatible();

        return ProviderRuntimeConfig.builder()
                .providerCode(resolveString(dbConfig == null ? null : dbConfig.getProviderCode(), effectiveProviderCode))
                .providerName(resolveString(dbConfig == null ? null : dbConfig.getProviderName(), fallback.getProviderName()))
                .enabled(dbConfig == null ? fallback.isEnabled() : dbConfig.getEnabled() != null && dbConfig.getEnabled() == 1)
                .baseUrl(resolveString(dbConfig == null ? null : dbConfig.getBaseUrl(), fallback.getBaseUrl()))
                .apiKey(resolveString(
                        decryptDbApiKey(dbConfig == null ? null : dbConfig.getApiKeyCipher()),
                        fallback.getApiKey()
                ))
                .defaultModelCode(resolveString(
                        dbConfig == null ? null : dbConfig.getDefaultModelCode(),
                        aiProviderProperties.getDefaultModelCode(),
                        fallback.getModel()
                ))
                .maxContextMessages(resolveInteger(
                        dbConfig == null ? null : dbConfig.getMaxContextMessages(),
                        aiProviderProperties.getMaxContextMessages()
                ))
                .temperature(resolveDouble(
                        dbConfig == null ? null : dbConfig.getTemperature(),
                        aiProviderProperties.getTemperature()
                ))
                .maxOutputTokens(resolveInteger(
                        dbConfig == null ? null : dbConfig.getMaxOutputTokens(),
                        aiProviderProperties.getMaxOutputTokens()
                ))
                .systemPrompt(resolveString(
                        dbConfig == null ? null : dbConfig.getSystemPromptTemplate(),
                        aiProviderProperties.getSystemPrompt()
                ))
                .connectTimeoutMs(fallback.getConnectTimeoutMs())
                .readTimeoutMs(resolveInteger(
                        dbConfig == null ? null : dbConfig.getTimeoutMs(),
                        fallback.getReadTimeoutMs()
                ))
                .build();
    }

    private String resolveString(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private Integer resolveInteger(Integer... candidates) {
        for (Integer candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private Double resolveDouble(Double... candidates) {
        for (Double candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private String decryptDbApiKey(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        return apiKeyCipherService.decrypt(cipherText);
    }
}
