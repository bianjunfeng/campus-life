package com.campus.campus_life_ai.ai.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiCallLog {
    private Long id;
    private String requestId;
    private String sessionId;
    private Long userId;
    private String capabilityCode;
    private String sceneCode;
    private String providerCode;
    private String modelCode;
    private Integer success;
    private Integer latencyMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer fallbackLevel;
    private Integer promptChars;
    private Integer completionChars;
    private BigDecimal costAmount;
    private String errorCode;
    private String errorType;
    private String errorMessage;
    private LocalDateTime createdAt;
}
