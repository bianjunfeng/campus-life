package com.campus.campus_life_ai.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentMessageDTO {
    private Long id;
    private String sessionId;
    private String role;
    private String contentType;
    private String content;
    private String messageStatus;
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
