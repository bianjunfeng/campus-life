package com.campus.campus_life_ai.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AiProviderHealthCheckResponse {
    String providerCode;
    String providerName;
    String modelCode;
    Boolean enabled;
    Boolean available;
    Boolean apiKeyConfigured;
    String baseUrl;
    String message;
    LocalDateTime checkedAt;
}
