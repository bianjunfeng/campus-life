package com.campus.campus_life_ai.ai.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiModelConfig {
    private Long id;
    private String providerCode;
    private String modelCode;
    private String modelName;
    private String capabilitiesJson;
    private Integer contextWindow;
    private Integer maxOutputTokens;
    private BigDecimal inputPricePer1k;
    private BigDecimal outputPricePer1k;
    private Integer enabled;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
