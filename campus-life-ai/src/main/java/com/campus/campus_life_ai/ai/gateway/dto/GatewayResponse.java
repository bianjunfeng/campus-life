package com.campus.campus_life_ai.ai.gateway.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GatewayResponse {
    String requestId;
    String content;
    String providerCode;
    String modelCode;
    GatewayUsage usage;
    Integer latencyMs;
    Integer fallbackLevel;
    String finishReason;
    GatewayError error;
}
