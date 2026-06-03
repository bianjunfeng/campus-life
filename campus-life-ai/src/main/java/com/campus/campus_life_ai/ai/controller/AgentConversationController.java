package com.campus.campus_life_ai.ai.controller;

import com.campus.campus_life_ai.ai.dto.AgentConversationDetailDTO;
import com.campus.campus_life_ai.ai.dto.AgentConversationSummaryDTO;
import com.campus.campus_life_ai.ai.dto.CreateConversationRequest;
import com.campus.campus_life_ai.ai.dto.RenameConversationRequest;
import com.campus.campus_life_ai.ai.dto.SendMessageRequest;
import com.campus.campus_life_ai.ai.dto.SendMessageResponse;
import com.campus.campus_life_ai.ai.service.AgentChatService;
import com.campus.campus_life_ai.ai.service.AgentConversationService;
import com.campus.campus_life_ai.common.result.ApiResponse;
import com.campus.campus_life_ai.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/agent/conversations")
public class AgentConversationController {

    private final CurrentUserAccessor currentUserAccessor;
    private final AgentConversationService agentConversationService;
    private final AgentChatService agentChatService;

    public AgentConversationController(CurrentUserAccessor currentUserAccessor,
                                       AgentConversationService agentConversationService,
                                       AgentChatService agentChatService) {
        this.currentUserAccessor = currentUserAccessor;
        this.agentConversationService = agentConversationService;
        this.agentChatService = agentChatService;
    }

    @PostMapping
    public ApiResponse<AgentConversationSummaryDTO> createConversation(@Valid @RequestBody(required = false) CreateConversationRequest request) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(agentConversationService.createConversation(userId, request == null ? new CreateConversationRequest() : request));
    }

    @GetMapping
    public ApiResponse<List<AgentConversationSummaryDTO>> listConversations() {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(agentConversationService.listConversations(userId));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<AgentConversationDetailDTO> getConversation(@PathVariable String sessionId,
                                                                   @RequestParam(required = false) Integer page,
                                                                   @RequestParam(required = false) Integer pageSize) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(agentConversationService.getConversationDetail(userId, sessionId, page, pageSize));
    }

    @PatchMapping("/{sessionId}")
    public ApiResponse<Void> renameConversation(@PathVariable String sessionId,
                                                @Valid @RequestBody RenameConversationRequest request) {
        Long userId = currentUserAccessor.requireUserId();
        agentConversationService.renameConversation(userId, sessionId, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> deleteConversation(@PathVariable String sessionId) {
        Long userId = currentUserAccessor.requireUserId();
        agentConversationService.deleteConversation(userId, sessionId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponse<AgentConversationDetailDTO> getMessages(@PathVariable String sessionId,
                                                               @RequestParam(required = false) Integer page,
                                                               @RequestParam(required = false) Integer pageSize) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(agentConversationService.getConversationDetail(userId, sessionId, page, pageSize));
    }

    @PostMapping("/{sessionId}/messages")
    public ApiResponse<SendMessageResponse> sendMessage(@PathVariable String sessionId,
                                                        @Valid @RequestBody SendMessageRequest request) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(agentChatService.sendMessage(userId, sessionId, request));
    }

    @PostMapping(value = "/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable String sessionId,
                                    @Valid @RequestBody SendMessageRequest request) {
        Long userId = currentUserAccessor.requireUserId();
        return agentChatService.streamMessage(userId, sessionId, request);
    }
}
