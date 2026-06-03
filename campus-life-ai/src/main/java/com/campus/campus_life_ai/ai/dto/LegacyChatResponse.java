package com.campus.campus_life_ai.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LegacyChatResponse {
    private String userMessage;
    private String agentMessage;
    private String sessionId;
    private LocalDateTime timestamp;
    private String error;
}
