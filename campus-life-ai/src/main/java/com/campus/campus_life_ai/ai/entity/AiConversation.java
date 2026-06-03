package com.campus.campus_life_ai.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiConversation {
    private Long id;
    private String sessionId;
    private Long userId;
    private String assistantType;
    private String capabilityCode;
    private String sceneCode;
    private String providerCode;
    private String modelCode;
    private String title;
    private Integer status;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Integer messageCount;
    private Integer pinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
