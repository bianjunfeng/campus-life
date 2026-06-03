package com.campus.campus_life_ai.ai.controller;

import com.campus.campus_life_ai.ai.dto.AgentConversationDetailDTO;
import com.campus.campus_life_ai.ai.dto.LegacyChatRequest;
import com.campus.campus_life_ai.ai.dto.LegacyChatResponse;
import com.campus.campus_life_ai.ai.dto.SendMessageRequest;
import com.campus.campus_life_ai.ai.dto.SendMessageResponse;
import com.campus.campus_life_ai.ai.service.AgentChatService;
import com.campus.campus_life_ai.ai.service.AgentConversationService;
import com.campus.campus_life_ai.common.result.ApiResponse;
import com.campus.campus_life_ai.common.security.CurrentUserAccessor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentCompatibilityController {

    private final CurrentUserAccessor currentUserAccessor;
    private final AgentChatService agentChatService;
    private final AgentConversationService agentConversationService;

    public AgentCompatibilityController(CurrentUserAccessor currentUserAccessor,
                                        AgentChatService agentChatService,
                                        AgentConversationService agentConversationService) {
        this.currentUserAccessor = currentUserAccessor;
        this.agentChatService = agentChatService;
        this.agentConversationService = agentConversationService;
    }

    @PostMapping("/chat")
    public ApiResponse<LegacyChatResponse> legacyChat(@RequestBody LegacyChatRequest request) {
        Long userId = currentUserAccessor.requireUserId();
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setContent(request.getMessage());
        sendMessageRequest.setCapabilityCode(request.getCapabilityCode());
        sendMessageRequest.setSceneCode(request.getSceneCode());
        sendMessageRequest.setProviderCode(request.getProviderCode());
        sendMessageRequest.setModelCode(request.getModelCode());
        sendMessageRequest.setKnowledgeBaseIds(request.getKnowledgeBaseIds());
        sendMessageRequest.setUsePersonalKnowledge(request.getUsePersonalKnowledge());
        sendMessageRequest.setUsePlatformKnowledge(request.getUsePlatformKnowledge());
        SendMessageResponse response = agentChatService.sendMessage(userId, request.getSessionId(), sendMessageRequest);

        LegacyChatResponse legacyChatResponse = new LegacyChatResponse();
        legacyChatResponse.setUserMessage(response.getUserMessage().getContent());
        legacyChatResponse.setAgentMessage(response.getAssistantMessage().getContent());
        legacyChatResponse.setSessionId(response.getSessionId());
        legacyChatResponse.setTimestamp(response.getAssistantMessage().getCreatedAt());
        legacyChatResponse.setError(response.getAssistantMessage().getErrorMessage());
        return ApiResponse.success(legacyChatResponse);
    }

    @GetMapping("/history")
    public ApiResponse<List<LegacyChatResponse>> legacyHistory(@RequestParam(required = false) String sessionId) {
        Long userId = currentUserAccessor.requireUserId();
        if (sessionId == null || sessionId.isBlank()) {
            return ApiResponse.success(agentConversationService.listConversations(userId).stream().map(summary -> {
                LegacyChatResponse response = new LegacyChatResponse();
                response.setSessionId(summary.getSessionId());
                response.setAgentMessage(summary.getLastMessagePreview());
                response.setTimestamp(summary.getLastMessageAt());
                return response;
            }).toList());
        }

        AgentConversationDetailDTO detail = agentConversationService.getConversationDetail(userId, sessionId, 1, 200);
        return ApiResponse.success(detail.getMessages().stream().map(message -> {
            LegacyChatResponse response = new LegacyChatResponse();
            response.setSessionId(message.getSessionId());
            if ("user".equals(message.getRole())) {
                response.setUserMessage(message.getContent());
            } else {
                response.setAgentMessage(message.getContent());
                response.setError(message.getErrorMessage());
            }
            response.setTimestamp(message.getCreatedAt());
            return response;
        }).toList());
    }

    @DeleteMapping("/history")
    public ApiResponse<Void> legacyDelete(@RequestParam(required = false) String sessionId) {
        Long userId = currentUserAccessor.requireUserId();
        if (sessionId != null && !sessionId.isBlank()) {
            agentConversationService.deleteConversation(userId, sessionId);
            return ApiResponse.success(null);
        }
        agentConversationService.listConversations(userId)
                .forEach(conversation -> agentConversationService.deleteConversation(userId, conversation.getSessionId()));
        return ApiResponse.success(null);
    }
}
