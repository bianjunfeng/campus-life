package com.campus.campus_life_ai.ai.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiUsageQuota {
    private Long id;
    private String subjectType;
    private String subjectId;
    private String capabilityCode;
    private String sceneCode;
    private String quotaPeriod;
    private Integer maxCalls;
    private Integer maxTokens;
    private BigDecimal maxCost;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
