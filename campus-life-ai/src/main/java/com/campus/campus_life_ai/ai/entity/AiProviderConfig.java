package com.campus.campus_life_ai.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiProviderConfig {
    private Long id;
    private String providerCode;
    private String providerName;
    private String baseUrl;
    private String apiKeyCipher;
    private String defaultModelCode;
    private Integer enabled;
    private Integer timeoutMs;
    private Integer maxContextMessages;
    private Double temperature;
    private Double topP;
    private Integer maxOutputTokens;
    private String systemPromptTemplate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
