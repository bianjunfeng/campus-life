package com.campus.campus_life_backend.ai.dto;

import lombok.Data;
import java.util.Date;

/**
 * 聊天响应 DTO
 */
@Data
public class ChatResponse {
    /**
     * 用户消息内容
     */
    private String userMessage;
    
    /**
     * 智能体响应消息内容
     */
    private String agentMessage;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 时间戳
     */
    private Date timestamp;
    
    /**
     * 错误信息（如果有）
     */
    private String error;
}

