package com.campus.campus_life_ai.ai.provider;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProviderRuntimeConfig {
    String providerCode;
    String providerName;
    Boolean enabled;
    String baseUrl;
    String apiKey;
    String defaultModelCode;
    Integer maxContextMessages;
    Double temperature;
    Integer maxOutputTokens;
    String systemPrompt;
    Integer connectTimeoutMs;
    Integer readTimeoutMs;
}
