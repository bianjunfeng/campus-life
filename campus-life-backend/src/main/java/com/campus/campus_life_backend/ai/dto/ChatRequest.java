package com.campus.campus_life_backend.ai.dto;

import lombok.Data;

/**
 * 聊天请求 DTO
 */
@Data
public class ChatRequest {
    /**
     * 用户消息内容
     */
    private String message;
    
    /**
     * 会话ID（可选，用于多轮对话，不提供则创建新会话）
     */
    private String sessionId;
}


