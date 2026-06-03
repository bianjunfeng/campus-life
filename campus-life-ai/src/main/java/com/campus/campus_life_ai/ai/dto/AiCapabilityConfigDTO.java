package com.campus.campus_life_ai.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiCapabilityConfigDTO {
    private Long id;
    private String capabilityCode;
    private String capabilityName;
    private boolean enabled;
    private boolean grayEnabled;
    private String grayRuleJson;
    private String rateLimitJson;
    private String quotaRuleJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
