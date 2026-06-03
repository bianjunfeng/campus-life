package com.campus.campus_life_ai.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiGatewayRouteRule {
    private Long id;
    private String ruleName;
    private String capabilityCode;
    private String sceneCode;
    private String matchRuleJson;
    private String routeRuleJson;
    private Integer enabled;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
