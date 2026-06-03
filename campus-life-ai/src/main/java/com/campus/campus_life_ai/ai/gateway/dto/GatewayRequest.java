package com.campus.campus_life_ai.ai.gateway.dto;

import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.gateway.route.RouteCandidate;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GatewayRequest {
    String requestId;
    Long userId;
    String sessionId;
    String capabilityCode;
    String sceneCode;
    String providerCode;
    String modelCode;
    ChatCompletionCommand command;
    ProviderRuntimeConfig runtimeConfig;
    List<RouteCandidate> fallbackCandidates;
}
