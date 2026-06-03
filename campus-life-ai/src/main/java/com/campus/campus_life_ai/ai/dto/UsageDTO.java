package com.campus.campus_life_ai.ai.dto;

import lombok.Data;

@Data
public class UsageDTO {
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
}
