package com.campus.campus_life_ai.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiMessage {
    private Long id;
    private String sessionId;
    private Long userId;
    private String role;
    private String contentType;
    private String content;
    private Integer messageStatus;
    private String providerCode;
    private String modelCode;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private String finishReason;
    private String errorCode;
    private String errorMessage;
    private Long replyToMessageId;
    private LocalDateTime createdAt;
}
