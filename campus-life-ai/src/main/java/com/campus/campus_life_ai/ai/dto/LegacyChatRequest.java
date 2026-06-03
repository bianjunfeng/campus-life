package com.campus.campus_life_ai.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class LegacyChatRequest {
    private String message;
    private String sessionId;
    private String capabilityCode;
    private String sceneCode;
    private String providerCode;
    private String modelCode;
    private List<Long> knowledgeBaseIds;
    private Boolean usePersonalKnowledge;
    private Boolean usePlatformKnowledge;
}
