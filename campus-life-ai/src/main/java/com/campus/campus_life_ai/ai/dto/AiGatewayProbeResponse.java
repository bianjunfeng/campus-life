package com.campus.campus_life_ai.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class AiGatewayProbeResponse {
    String requestId;
    String content;
    Boolean success;
    String providerCode;
    String modelCode;
    String capabilityCode;
    String sceneCode;
    Integer promptTokens;
    Integer completionTokens;
    Integer totalTokens;
    BigDecimal costAmount;
    Integer latencyMs;
    Integer fallbackLevel;
    String finishReason;
    String errorCode;
    String errorType;
    String errorMessage;
    LocalDateTime createdAt;
}
