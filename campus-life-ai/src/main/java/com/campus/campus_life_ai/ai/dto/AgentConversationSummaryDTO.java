package com.campus.campus_life_ai.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentConversationSummaryDTO {
    private String sessionId;
    private String assistantType;
    private String capabilityCode;
    private String sceneCode;
    private String providerCode;
    private String modelCode;
    private String title;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Integer messageCount;
    private Boolean pinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
