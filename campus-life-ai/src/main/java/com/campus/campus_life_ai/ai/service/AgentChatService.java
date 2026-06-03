package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.AgentMessageDTO;
import com.campus.campus_life_ai.ai.dto.SendMessageRequest;
import com.campus.campus_life_ai.ai.dto.SendMessageResponse;
import com.campus.campus_life_ai.ai.dto.UsageDTO;
import com.campus.campus_life_ai.ai.entity.AiConversation;
import com.campus.campus_life_ai.ai.entity.AiMessage;
import com.campus.campus_life_ai.ai.gateway.GatewayCallException;
import com.campus.campus_life_ai.ai.gateway.GatewayErrorCode;
import com.campus.campus_life_ai.ai.gateway.LlmGatewayFacade;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayUsage;
import com.campus.campus_life_ai.ai.mapper.AiConversationMapper;
import com.campus.campus_life_ai.ai.mapper.AiMessageMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.ChatCompletionResult;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.service.memory.ConversationMemoryAdvisor;
import com.campus.campus_life_ai.ai.service.tool.AgentToolRegistry;
import com.campus.campus_life_ai.knowledge.service.KnowledgePromptContext;
import com.campus.campus_life_ai.knowledge.service.KnowledgeSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AgentChatService {

    private final AgentConversationService conversationService;
    private final AiConversationMapper aiConversationMapper;
    private final AiMessageMapper aiMessageMapper;
    private final LlmGatewayFacade llmGatewayFacade;
    private final MessageAssembler messageAssembler;
    private final AiChatRouteResolver aiChatRouteResolver;
    private final KnowledgeSearchService knowledgeSearchService;
    private final ConversationMemoryAdvisor conversationMemoryAdvisor;
    private final AgentToolRegistry agentToolRegistry;

    public AgentChatService(AgentConversationService conversationService,
                            AiConversationMapper aiConversationMapper,
                            AiMessageMapper aiMessageMapper,
                            LlmGatewayFacade llmGatewayFacade,
                            MessageAssembler messageAssembler,
                            AiChatRouteResolver aiChatRouteResolver,
                            KnowledgeSearchService knowledgeSearchService,
                            ConversationMemoryAdvisor conversationMemoryAdvisor,
                            AgentToolRegistry agentToolRegistry) {
        this.conversationService = conversationService;
        this.aiConversationMapper = aiConversationMapper;
        this.aiMessageMapper = aiMessageMapper;
        this.llmGatewayFacade = llmGatewayFacade;
        this.messageAssembler = messageAssembler;
        this.aiChatRouteResolver = aiChatRouteResolver;
        this.knowledgeSearchService = knowledgeSearchService;
        this.conversationMemoryAdvisor = conversationMemoryAdvisor;
        this.agentToolRegistry = agentToolRegistry;
    }

    @Transactional
    public SendMessageResponse sendMessage(Long userId, String sessionId, SendMessageRequest request) {
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        AiConversation existingConversation = conversationService.findConversation(sessionId, userId);
        AiChatRoute route = aiChatRouteResolver.resolve(existingConversation, request);
        AiConversation conversation = conversationService.createConversationIfAbsent(
                userId,
                sessionId,
                route.getAssistantType(),
                buildTitle(content),
                route.getCapabilityCode(),
                route.getSceneCode(),
                route.getProviderCode(),
                route.getModelCode()
        );

        AiMessage userMessage = new AiMessage();
        userMessage.setSessionId(conversation.getSessionId());
        userMessage.setUserId(userId);
        userMessage.setRole("user");
        userMessage.setContentType("text");
        userMessage.setContent(content);
        userMessage.setMessageStatus(0);
        userMessage.setCreatedAt(LocalDateTime.now());
        aiMessageMapper.insert(userMessage);

        String requestId = UUID.randomUUID().toString();
        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setSessionId(conversation.getSessionId());
        assistantMessage.setUserId(userId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContentType("text");
        assistantMessage.setReplyToMessageId(userMessage.getId());
        assistantMessage.setCreatedAt(LocalDateTime.now());

        ChatCompletionResult result;
        ProviderRuntimeConfig runtimeConfig = route.getRuntimeConfig();
        KnowledgePromptContext knowledgeContext = new KnowledgePromptContext();
        try {
            knowledgeContext = knowledgeSearchService.searchForAgent(
                    userId,
                    route.getSceneCode(),
                    content,
                    request.getKnowledgeBaseIds(),
                    request.getUsePersonalKnowledge(),
                    request.getUsePlatformKnowledge(),
                    5
            );
            String modelContent = knowledgeContext.buildPrompt(content);
            GatewayResponse gatewayResponse = llmGatewayFacade.chat(buildGatewayRequest(
                    requestId,
                    userId,
                    conversation.getSessionId(),
                    userMessage.getId(),
                    modelContent,
                    runtimeConfig,
                    route
            ));
            result = toChatCompletionResult(gatewayResponse);

            assistantMessage.setContent(result.getContent());
            assistantMessage.setMessageStatus(0);
            assistantMessage.setProviderCode(result.getProviderCode());
            assistantMessage.setModelCode(result.getModelCode());
            assistantMessage.setPromptTokens(result.getPromptTokens());
            assistantMessage.setCompletionTokens(result.getCompletionTokens());
            assistantMessage.setTotalTokens(result.getTotalTokens());
            assistantMessage.setLatencyMs(result.getLatencyMs());
            assistantMessage.setFinishReason(result.getFinishReason());
            aiMessageMapper.insert(assistantMessage);
        } catch (Exception e) {
            log.error("AI 对话调用失败, sessionId={}, userId={}, providerCode={}, modelCode={}",
                    conversation.getSessionId(), userId, runtimeConfig.getProviderCode(),
                    runtimeConfig.getDefaultModelCode(), e);
            GatewayError gatewayError = resolveGatewayError(e);
            assistantMessage.setContent("AI 服务暂不可用，请稍后重试");
            assistantMessage.setMessageStatus(1);
            assistantMessage.setErrorCode(gatewayError.getErrorCode());
            assistantMessage.setErrorMessage(gatewayError.getMessage());
            aiMessageMapper.insert(assistantMessage);
        }

        refreshConversation(conversation.getSessionId(), userId, route, assistantMessage.getContent(), assistantMessage.getCreatedAt());

        SendMessageResponse response = new SendMessageResponse();
        response.setSessionId(conversation.getSessionId());
        response.setConversationTitle(resolveConversationTitle(conversation, content));
        response.setCapabilityCode(route.getCapabilityCode());
        response.setSceneCode(route.getSceneCode());
        response.setProviderCode(route.getProviderCode());
        response.setModelCode(route.getModelCode());
        response.setUserMessage(messageAssembler.toMessageDto(userMessage));
        response.setAssistantMessage(messageAssembler.toMessageDto(assistantMessage));
        response.setUsage(toUsage(assistantMessage));
        response.setKnowledgeReferences(knowledgeContext.getReferences());
        return response;
    }

    public SseEmitter streamMessage(Long userId, String sessionId, SendMessageRequest request) {
        SseEmitter emitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> doStreamMessage(userId, sessionId, request, emitter));
        return emitter;
    }

    private void doStreamMessage(Long userId, String sessionId, SendMessageRequest request, SseEmitter emitter) {
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (!StringUtils.hasText(content)) {
            sendErrorAndComplete(emitter, "消息内容不能为空");
            return;
        }

        String requestId = UUID.randomUUID().toString();
        AiChatRoute route = null;
        ProviderRuntimeConfig runtimeConfig = null;
        AiConversation conversation = null;
        AiMessage userMessage = null;
        boolean assistantPersisted = false;
        KnowledgePromptContext knowledgeContext = new KnowledgePromptContext();

        try {
            AiConversation existingConversation = conversationService.findConversation(sessionId, userId);
            route = aiChatRouteResolver.resolve(existingConversation, request);
            conversation = conversationService.createConversationIfAbsent(
                    userId,
                    sessionId,
                    route.getAssistantType(),
                    buildTitle(content),
                    route.getCapabilityCode(),
                    route.getSceneCode(),
                    route.getProviderCode(),
                    route.getModelCode()
            );

            userMessage = new AiMessage();
            userMessage.setSessionId(conversation.getSessionId());
            userMessage.setUserId(userId);
            userMessage.setRole("user");
            userMessage.setContentType("text");
            userMessage.setContent(content);
            userMessage.setMessageStatus(0);
            userMessage.setCreatedAt(LocalDateTime.now());
            aiMessageMapper.insert(userMessage);

            runtimeConfig = route.getRuntimeConfig();
            knowledgeContext = knowledgeSearchService.searchForAgent(
                    userId,
                    route.getSceneCode(),
                    content,
                    request.getKnowledgeBaseIds(),
                    request.getUsePersonalKnowledge(),
                    request.getUsePlatformKnowledge(),
                    5
            );

            sendEvent(emitter, "meta", buildStreamMeta(conversation, route, userMessage, knowledgeContext));

            String modelContent = knowledgeContext.buildPrompt(content);
            GatewayResponse gatewayResponse = llmGatewayFacade.stream(
                    buildGatewayRequest(
                            requestId,
                            userId,
                            conversation.getSessionId(),
                            userMessage.getId(),
                            modelContent,
                            runtimeConfig,
                            route
                    ),
                    delta -> {
                        Map<String, Object> deltaPayload = new HashMap<>();
                        deltaPayload.put("content", delta);
                        sendEvent(emitter, "delta", deltaPayload);
                    }
            );
            ChatCompletionResult result = toChatCompletionResult(gatewayResponse);

            AiMessage assistantMessage = buildAssistantMessage(conversation, userMessage, result);
            aiMessageMapper.insert(assistantMessage);
            assistantPersisted = true;
            refreshConversation(conversation.getSessionId(), userId, route, assistantMessage.getContent(), assistantMessage.getCreatedAt());

            SendMessageResponse response = buildSendMessageResponse(conversation, route, userMessage, assistantMessage, knowledgeContext);
            sendEvent(emitter, "done", response);
            emitter.complete();
        } catch (ClientStreamClosedException e) {
            log.info("AI 流式对话客户端已中断, sessionId={}, userId={}", sessionId, userId);
            emitter.complete();
        } catch (Exception e) {
            log.error("AI 流式对话调用失败, sessionId={}, userId={}", sessionId, userId, e);
            if (!assistantPersisted && conversation != null && userMessage != null && route != null) {
                AiMessage assistantMessage = buildAssistantErrorMessage(conversation, userMessage, e);
                aiMessageMapper.insert(assistantMessage);
                refreshConversation(conversation.getSessionId(), userId, route, assistantMessage.getContent(), assistantMessage.getCreatedAt());
            }
            sendErrorAndComplete(emitter, e.getMessage());
        }
    }

    private GatewayRequest buildGatewayRequest(String requestId,
                                               Long userId,
                                               String sessionId,
                                               Long latestUserMessageId,
                                               String latestContent,
                                               ProviderRuntimeConfig runtimeConfig,
                                               AiChatRoute route) {
        return GatewayRequest.builder()
                .requestId(requestId)
                .userId(userId)
                .sessionId(sessionId)
                .capabilityCode(route.getCapabilityCode())
                .sceneCode(route.getSceneCode())
                .providerCode(route.getProviderCode())
                .modelCode(route.getModelCode())
                .runtimeConfig(runtimeConfig)
                .command(buildCommand(
                        sessionId,
                        latestUserMessageId,
                        latestContent,
                        runtimeConfig,
                        route,
                        userId
                ))
                .build();
    }

    private ChatCompletionCommand buildCommand(String sessionId,
                                               Long latestUserMessageId,
                                               String latestContent,
                                               ProviderRuntimeConfig runtimeConfig,
                                               AiChatRoute route,
                                               Long userId) {
        List<ChatCompletionCommand.PromptMessage> promptMessages = conversationMemoryAdvisor.buildPromptMessages(
                sessionId,
                latestUserMessageId,
                latestContent,
                runtimeConfig.getMaxContextMessages()
        );

        return ChatCompletionCommand.builder()
                .providerCode(runtimeConfig.getProviderCode())
                .modelCode(runtimeConfig.getDefaultModelCode())
                .systemPrompt(runtimeConfig.getSystemPrompt())
                .temperature(runtimeConfig.getTemperature())
                .maxOutputTokens(runtimeConfig.getMaxOutputTokens())
                .messages(promptMessages)
                .toolCallbacks(agentToolRegistry.resolveTools(route.getCapabilityCode(), route.getSceneCode()))
                .toolContext(agentToolRegistry.buildToolContext(
                        userId,
                        sessionId,
                        route.getCapabilityCode(),
                        route.getSceneCode()
                ))
                .build();
    }

    private ChatCompletionResult toChatCompletionResult(GatewayResponse response) {
        GatewayUsage usage = response.getUsage();
        return ChatCompletionResult.builder()
                .content(response.getContent())
                .providerCode(response.getProviderCode())
                .modelCode(response.getModelCode())
                .promptTokens(usage == null ? null : usage.getPromptTokens())
                .completionTokens(usage == null ? null : usage.getCompletionTokens())
                .totalTokens(usage == null ? null : usage.getTotalTokens())
                .latencyMs(response.getLatencyMs())
                .finishReason(response.getFinishReason())
                .build();
    }

    private AiMessage buildAssistantMessage(AiConversation conversation, AiMessage userMessage, ChatCompletionResult result) {
        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setSessionId(conversation.getSessionId());
        assistantMessage.setUserId(conversation.getUserId());
        assistantMessage.setRole("assistant");
        assistantMessage.setContentType("text");
        assistantMessage.setReplyToMessageId(userMessage.getId());
        assistantMessage.setContent(result.getContent());
        assistantMessage.setMessageStatus(0);
        assistantMessage.setProviderCode(result.getProviderCode());
        assistantMessage.setModelCode(result.getModelCode());
        assistantMessage.setPromptTokens(result.getPromptTokens());
        assistantMessage.setCompletionTokens(result.getCompletionTokens());
        assistantMessage.setTotalTokens(result.getTotalTokens());
        assistantMessage.setLatencyMs(result.getLatencyMs());
        assistantMessage.setFinishReason(result.getFinishReason());
        assistantMessage.setCreatedAt(LocalDateTime.now());
        return assistantMessage;
    }

    private AiMessage buildAssistantErrorMessage(AiConversation conversation, AiMessage userMessage, Exception e) {
        GatewayError gatewayError = resolveGatewayError(e);
        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setSessionId(conversation.getSessionId());
        assistantMessage.setUserId(conversation.getUserId());
        assistantMessage.setRole("assistant");
        assistantMessage.setContentType("text");
        assistantMessage.setReplyToMessageId(userMessage.getId());
        assistantMessage.setContent("AI 服务暂不可用，请稍后重试");
        assistantMessage.setMessageStatus(1);
        assistantMessage.setErrorCode(gatewayError.getErrorCode());
        assistantMessage.setErrorMessage(gatewayError.getMessage());
        assistantMessage.setCreatedAt(LocalDateTime.now());
        return assistantMessage;
    }

    private GatewayError resolveGatewayError(Exception exception) {
        if (exception instanceof GatewayCallException gatewayCallException && gatewayCallException.getError() != null) {
            return gatewayCallException.getError();
        }
        return GatewayError.builder()
                .errorCode(GatewayErrorCode.PROVIDER_ERROR.getCode())
                .errorType(GatewayErrorCode.PROVIDER_ERROR.getType())
                .message(StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : "AI 对话调用失败")
                .build();
    }

    private SendMessageResponse buildSendMessageResponse(AiConversation conversation,
                                                         AiChatRoute route,
                                                         AiMessage userMessage,
                                                         AiMessage assistantMessage,
                                                         KnowledgePromptContext knowledgeContext) {
        SendMessageResponse response = new SendMessageResponse();
        response.setSessionId(conversation.getSessionId());
        response.setConversationTitle(resolveConversationTitle(conversation, userMessage.getContent()));
        response.setCapabilityCode(route.getCapabilityCode());
        response.setSceneCode(route.getSceneCode());
        response.setProviderCode(route.getProviderCode());
        response.setModelCode(route.getModelCode());
        response.setUserMessage(messageAssembler.toMessageDto(userMessage));
        response.setAssistantMessage(messageAssembler.toMessageDto(assistantMessage));
        response.setUsage(toUsage(assistantMessage));
        response.setKnowledgeReferences(knowledgeContext.getReferences());
        return response;
    }

    private Map<String, Object> buildStreamMeta(AiConversation conversation,
                                                AiChatRoute route,
                                                AiMessage userMessage,
                                                KnowledgePromptContext knowledgeContext) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("sessionId", conversation.getSessionId());
        meta.put("conversationTitle", resolveConversationTitle(conversation, userMessage.getContent()));
        meta.put("capabilityCode", route.getCapabilityCode());
        meta.put("sceneCode", route.getSceneCode());
        meta.put("providerCode", route.getProviderCode());
        meta.put("modelCode", route.getModelCode());
        meta.put("userMessage", messageAssembler.toMessageDto(userMessage));
        meta.put("knowledgeReferences", knowledgeContext.getReferences());
        return meta;
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            throw new ClientStreamClosedException(e);
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", StringUtils.hasText(message) ? message : "流式对话失败");
            emitter.send(SseEmitter.event().name("error").data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException ignored) {
            // Client may have disconnected.
        } finally {
            emitter.complete();
        }
    }

    private void refreshConversation(String sessionId, Long userId, AiChatRoute route, String lastMessage, LocalDateTime lastMessageAt) {
        AiConversation conversation = conversationService.requireConversation(sessionId, userId);
        conversation.setAssistantType(route.getAssistantType());
        conversation.setCapabilityCode(route.getCapabilityCode());
        conversation.setSceneCode(route.getSceneCode());
        conversation.setProviderCode(route.getProviderCode());
        conversation.setModelCode(route.getModelCode());
        conversation.setLastMessagePreview(abbreviate(lastMessage, 500));
        conversation.setLastMessageAt(lastMessageAt);
        conversation.setMessageCount(aiMessageMapper.countBySessionId(sessionId));
        conversation.setUpdatedAt(LocalDateTime.now());
        if (!StringUtils.hasText(conversation.getTitle()) || "新对话".equals(conversation.getTitle())) {
            conversation.setTitle(buildTitle(lastMessage));
        }
        aiConversationMapper.updateMessageState(conversation);
    }

    private UsageDTO toUsage(AiMessage message) {
        UsageDTO usage = new UsageDTO();
        usage.setPromptTokens(message.getPromptTokens());
        usage.setCompletionTokens(message.getCompletionTokens());
        usage.setTotalTokens(message.getTotalTokens());
        usage.setLatencyMs(message.getLatencyMs());
        return usage;
    }

    private String resolveConversationTitle(AiConversation conversation, String fallback) {
        if (StringUtils.hasText(conversation.getTitle()) && !"新对话".equals(conversation.getTitle())) {
            return conversation.getTitle();
        }
        return buildTitle(fallback);
    }

    private String buildTitle(String content) {
        return abbreviate(content, 20);
    }

    private String abbreviate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static class ClientStreamClosedException extends RuntimeException {
        ClientStreamClosedException(Throwable cause) {
            super(cause);
        }
    }
}
