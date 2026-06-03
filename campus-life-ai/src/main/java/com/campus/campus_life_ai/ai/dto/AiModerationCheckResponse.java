package com.campus.campus_life_ai.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class AiModerationCheckResponse {
    String requestId;
    String targetType;
    String result;
    Double score;
    List<String> categories;
    String reason;
    String rawResponse;
    String providerCode;
    String modelCode;
    String capabilityCode;
    String sceneCode;
    Integer latencyMs;
    Integer fallbackLevel;
    LocalDateTime createdAt;
}
