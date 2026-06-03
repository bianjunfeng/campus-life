package com.campus.campus_life_ai.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiGatewaySafetyRule {
    private Long id;
    private String ruleName;
    private String capabilityCode;
    private String sceneCode;
    private String direction;
    private String action;
    private String matchType;
    private String patternText;
    private String category;
    private String severity;
    private Integer enabled;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
