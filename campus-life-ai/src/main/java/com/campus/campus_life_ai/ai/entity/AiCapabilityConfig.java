package com.campus.campus_life_ai.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiCapabilityConfig {
    private Long id;
    private String capabilityCode;
    private String capabilityName;
    private Integer enabled;
    private Integer grayEnabled;
    private String grayRuleJson;
    private String rateLimitJson;
    private String quotaRuleJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
