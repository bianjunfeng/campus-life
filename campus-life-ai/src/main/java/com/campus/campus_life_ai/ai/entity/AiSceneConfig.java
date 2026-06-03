package com.campus.campus_life_ai.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiSceneConfig {
    private Long id;
    private String capabilityCode;
    private String sceneCode;
    private String sceneName;
    private String providerCode;
    private String modelCode;
    private Integer enabled;
    private String systemPromptTemplate;
    private String inputSchemaJson;
    private String outputSchemaJson;
    private String safetyLevel;
    private Integer timeoutMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
