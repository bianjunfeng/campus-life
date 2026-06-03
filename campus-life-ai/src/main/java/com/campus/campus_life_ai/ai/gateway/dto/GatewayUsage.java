package com.campus.campus_life_ai.ai.gateway.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class GatewayUsage {
    Integer promptTokens;
    Integer completionTokens;
    Integer totalTokens;
    Integer latencyMs;
    BigDecimal costAmount;
}
