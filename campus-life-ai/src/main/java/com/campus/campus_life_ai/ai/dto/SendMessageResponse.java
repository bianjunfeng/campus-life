package com.campus.campus_life_ai.ai.dto;

import com.campus.campus_life_ai.knowledge.dto.KnowledgeReferenceDTO;
import lombok.Data;

import java.util.List;

@Data
public class SendMessageResponse {
    private String sessionId;
    private String conversationTitle;
    private String capabilityCode;
    private String sceneCode;
    private String providerCode;
    private String modelCode;
    private AgentMessageDTO userMessage;
    private AgentMessageDTO assistantMessage;
    private UsageDTO usage;
    private List<KnowledgeReferenceDTO> knowledgeReferences;
}
