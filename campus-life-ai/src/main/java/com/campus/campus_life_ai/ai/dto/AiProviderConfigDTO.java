package com.campus.campus_life_ai.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiProviderConfigDTO {
    private Long id;
    private String providerCode;
    private String providerName;
    private String baseUrl;
    private String defaultModelCode;
    private boolean enabled;
    private Integer timeoutMs;
    private Integer maxContextMessages;
    private Double temperature;
    private Double topP;
    private Integer maxOutputTokens;
    private String systemPromptTemplate;
    private boolean hasApiKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
