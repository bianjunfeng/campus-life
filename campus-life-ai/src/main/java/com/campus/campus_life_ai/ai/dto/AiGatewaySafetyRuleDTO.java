package com.campus.campus_life_ai.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiGatewaySafetyRuleDTO {
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
    private boolean enabled;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
