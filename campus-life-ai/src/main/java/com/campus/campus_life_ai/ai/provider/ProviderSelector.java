package com.campus.campus_life_ai.ai.provider;

import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class ProviderSelector {

    private final List<LlmProvider> providers;
    private final AiProviderProperties properties;

    public ProviderSelector(List<LlmProvider> providers, AiProviderProperties properties) {
        this.providers = providers;
        this.properties = properties;
    }

    public LlmProvider resolve(String providerCode) {
        String effectiveCode = StringUtils.hasText(providerCode) ? providerCode : properties.getDefaultProviderCode();
        return providers.stream()
                .filter(provider -> provider.providerCode().equals(effectiveCode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到模型供应商: " + effectiveCode));
    }
}
