package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiChatRoute {
    String assistantType;
    String capabilityCode;
    String sceneCode;
    String providerCode;
    String modelCode;
    ProviderRuntimeConfig runtimeConfig;
}
