package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.AgentConversationSummaryDTO;
import com.campus.campus_life_ai.ai.dto.AgentMessageDTO;
import com.campus.campus_life_ai.ai.entity.AiConversation;
import com.campus.campus_life_ai.ai.entity.AiMessage;
import org.springframework.stereotype.Component;

@Component
public class MessageAssembler {

    public AgentConversationSummaryDTO toConversationSummary(AiConversation conversation) {
        AgentConversationSummaryDTO dto = new AgentConversationSummaryDTO();
        dto.setSessionId(conversation.getSessionId());
        dto.setAssistantType(conversation.getAssistantType());
        dto.setCapabilityCode(conversation.getCapabilityCode());
        dto.setSceneCode(conversation.getSceneCode());
        dto.setProviderCode(conversation.getProviderCode());
        dto.setModelCode(conversation.getModelCode());
        dto.setTitle(conversation.getTitle());
        dto.setLastMessagePreview(conversation.getLastMessagePreview());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setMessageCount(conversation.getMessageCount());
        dto.setPinned(conversation.getPinned() != null && conversation.getPinned() == 1);
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        return dto;
    }

    public AgentMessageDTO toMessageDto(AiMessage message) {
        AgentMessageDTO dto = new AgentMessageDTO();
        dto.setId(message.getId());
        dto.setSessionId(message.getSessionId());
        dto.setRole(message.getRole());
        dto.setContentType(message.getContentType());
        dto.setContent(message.getContent());
        dto.setMessageStatus(resolveMessageStatus(message.getMessageStatus()));
        dto.setProviderCode(message.getProviderCode());
        dto.setModelCode(message.getModelCode());
        dto.setPromptTokens(message.getPromptTokens());
        dto.setCompletionTokens(message.getCompletionTokens());
        dto.setTotalTokens(message.getTotalTokens());
        dto.setLatencyMs(message.getLatencyMs());
        dto.setFinishReason(message.getFinishReason());
        dto.setErrorCode(message.getErrorCode());
        dto.setErrorMessage(message.getErrorMessage());
        dto.setReplyToMessageId(message.getReplyToMessageId());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    private String resolveMessageStatus(Integer code) {
        if (code == null) {
            return "UNKNOWN";
        }
        return switch (code) {
            case 0 -> "SUCCESS";
            case 1 -> "FAILED";
            case 2 -> "GENERATING";
            case 3 -> "CANCELLED";
            default -> "UNKNOWN";
        };
    }
}
