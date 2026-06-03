package com.campus.campus_life_ai.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AgentConversationDetailDTO {
    private AgentConversationSummaryDTO conversation;
    private List<AgentMessageDTO> messages;
}
