package com.campus.campus_life_ai.ai.gateway.route;

import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RouteCandidate {
    String providerCode;
    String modelCode;
    ProviderRuntimeConfig runtimeConfig;
    ChatCompletionCommand command;
    int fallbackLevel;
}
