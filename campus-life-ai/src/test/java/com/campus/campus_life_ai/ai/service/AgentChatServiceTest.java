package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.SendMessageRequest;
import com.campus.campus_life_ai.ai.dto.SendMessageResponse;
import com.campus.campus_life_ai.ai.entity.AiConversation;
import com.campus.campus_life_ai.ai.entity.AiMessage;
import com.campus.campus_life_ai.ai.gateway.DefaultLlmGatewayFacade;
import com.campus.campus_life_ai.ai.gateway.GatewayErrorNormalizer;
import com.campus.campus_life_ai.ai.gateway.observe.GatewayCallLogger;
import com.campus.campus_life_ai.ai.gateway.observe.GatewayCostCalculator;
import com.campus.campus_life_ai.ai.gateway.observe.GatewayMetricsRecorder;
import com.campus.campus_life_ai.ai.gateway.policy.GatewayPolicyChain;
import com.campus.campus_life_ai.ai.gateway.policy.GatewaySafetyInspector;
import com.campus.campus_life_ai.ai.gateway.route.ModelRoutePlanner;
import com.campus.campus_life_ai.ai.gateway.route.ProviderHealthRegistry;
import com.campus.campus_life_ai.ai.mapper.AiCallLogMapper;
import com.campus.campus_life_ai.ai.mapper.AiConversationMapper;
import com.campus.campus_life_ai.ai.mapper.AiGatewayRouteRuleMapper;
import com.campus.campus_life_ai.ai.mapper.AiMessageMapper;
import com.campus.campus_life_ai.ai.mapper.AiModelConfigMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionResult;
import com.campus.campus_life_ai.ai.provider.LlmProvider;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.provider.ProviderSelector;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.service.memory.ConversationMemoryAdvisor;
import com.campus.campus_life_ai.ai.service.tool.AgentToolRegistry;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import com.campus.campus_life_ai.knowledge.service.KnowledgePromptContext;
import com.campus.campus_life_ai.knowledge.service.KnowledgeSearchService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AgentChatServiceTest {

    @Mock
    private AgentConversationService conversationService;

    @Mock
    private AiConversationMapper aiConversationMapper;

    @Mock
    private AiMessageMapper aiMessageMapper;

    @Mock
    private AiCallLogMapper aiCallLogMapper;

    @Mock
    private ProviderSelector providerSelector;

    @Mock
    private LlmProvider llmProvider;

    @Mock
    private AiChatRouteResolver aiChatRouteResolver;

    @Mock
    private KnowledgeSearchService knowledgeSearchService;

    @Mock
    private ConversationMemoryAdvisor conversationMemoryAdvisor;

    @Mock
    private AgentToolRegistry agentToolRegistry;

    private final MessageAssembler messageAssembler = new MessageAssembler();

    private AiProviderProperties providerProperties;

    private AgentChatService agentChatService;

    @BeforeEach
    void setUp() {
        providerProperties = new AiProviderProperties();
        providerProperties.setDefaultProviderCode("openai-compatible");
        providerProperties.setDefaultModelCode("gpt-4o-mini");
        providerProperties.setDefaultAssistantType("general");
        providerProperties.setSystemPrompt("system");
        providerProperties.setMaxContextMessages(20);
        providerProperties.setTemperature(0.7D);
        providerProperties.setMaxOutputTokens(1024);
        agentChatService = new AgentChatService(
                conversationService,
                aiConversationMapper,
                aiMessageMapper,
                new DefaultLlmGatewayFacade(
                        providerSelector,
                        new GatewayCallLogger(aiCallLogMapper),
                        new GatewayMetricsRecorder(new SimpleMeterRegistry()),
                        new GatewayCostCalculator(org.mockito.Mockito.mock(AiModelConfigMapper.class)),
                        new GatewayErrorNormalizer(),
                        new GatewayPolicyChain(List.of()),
                        org.mockito.Mockito.mock(GatewaySafetyInspector.class),
                        new ModelRoutePlanner(
                                org.mockito.Mockito.mock(AiGatewayRouteRuleMapper.class),
                                org.mockito.Mockito.mock(AiModelConfigMapper.class),
                                org.mockito.Mockito.mock(AiRuntimeConfigService.class),
                                new ObjectMapper()
                        ),
                        new ProviderHealthRegistry(providerProperties)
                ),
                messageAssembler,
                aiChatRouteResolver,
                knowledgeSearchService,
                conversationMemoryAdvisor,
                agentToolRegistry
        );
        given(knowledgeSearchService.searchForAgent(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new KnowledgePromptContext());
    }

    @Test
    void shouldSendMessageSuccessfully() {
        AiConversation conversation = buildConversation("ses_123", 1001L, "你好");
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("你好");
        ProviderRuntimeConfig runtimeConfig = ProviderRuntimeConfig.builder()
                .providerCode("openai-compatible")
                .providerName("OpenAI Compatible")
                .enabled(true)
                .baseUrl("https://api.example.com/v1")
                .apiKey("db-key")
                .defaultModelCode("db-model")
                .maxContextMessages(8)
                .temperature(0.25D)
                .maxOutputTokens(256)
                .systemPrompt("db-system")
                .connectTimeoutMs(5000)
                .readTimeoutMs(12000)
                .build();
        AiChatRoute route = AiChatRoute.builder()
                .assistantType("general")
                .capabilityCode("chat")
                .sceneCode("chat.general")
                .providerCode("openai-compatible")
                .modelCode("db-model")
                .runtimeConfig(runtimeConfig)
                .build();

        given(conversationService.findConversation("ses_123", 1001L)).willReturn(null);
        given(aiChatRouteResolver.resolve(null, request)).willReturn(route);
        given(conversationService.createConversationIfAbsent(eq(1001L), eq("ses_123"), eq("general"), eq("你好"),
                eq("chat"), eq("chat.general"), eq("openai-compatible"), eq("db-model")))
                .willReturn(conversation);
        given(conversationMemoryAdvisor.buildPromptMessages("ses_123", 1L, "你好", 8))
                .willReturn(List.of(ChatCompletionCommand.PromptMessage.builder()
                        .role("user")
                        .content("你好")
                        .build()));
        given(agentToolRegistry.resolveTools("chat", "chat.general")).willReturn(List.of());
        given(agentToolRegistry.buildToolContext(1001L, "ses_123", "chat", "chat.general")).willReturn(Map.of());
        given(providerSelector.resolve("openai-compatible")).willReturn(llmProvider);
        given(llmProvider.isAvailable(runtimeConfig)).willReturn(true);
        given(llmProvider.chat(any(), eq(runtimeConfig))).willReturn(ChatCompletionResult.builder()
                .providerCode("openai-compatible")
                .modelCode("db-model")
                .content("你好，我是 AI 助手")
                .promptTokens(10)
                .completionTokens(15)
                .totalTokens(25)
                .latencyMs(680)
                .finishReason("stop")
                .build());
        given(conversationService.requireConversation("ses_123", 1001L)).willReturn(conversation);
        given(aiMessageMapper.countBySessionId("ses_123")).willReturn(2);

        doAnswer(invocation -> {
            AiMessage message = invocation.getArgument(0);
            if ("user".equals(message.getRole())) {
                message.setId(1L);
            } else {
                message.setId(2L);
            }
            return 1;
        }).when(aiMessageMapper).insert(any(AiMessage.class));

        SendMessageResponse response = agentChatService.sendMessage(1001L, "ses_123", request);

        assertEquals("ses_123", response.getSessionId());
        assertEquals("你好", response.getConversationTitle());
        assertEquals("chat", response.getCapabilityCode());
        assertEquals("chat.general", response.getSceneCode());
        assertEquals("openai-compatible", response.getProviderCode());
        assertEquals("db-model", response.getModelCode());
        assertEquals("你好，我是 AI 助手", response.getAssistantMessage().getContent());
        assertEquals(25, response.getUsage().getTotalTokens());

        ArgumentCaptor<com.campus.campus_life_ai.ai.provider.ChatCompletionCommand> commandCaptor =
                ArgumentCaptor.forClass(com.campus.campus_life_ai.ai.provider.ChatCompletionCommand.class);
        verify(llmProvider).chat(commandCaptor.capture(), eq(runtimeConfig));
        assertEquals("db-model", commandCaptor.getValue().getModelCode());
        assertEquals("db-system", commandCaptor.getValue().getSystemPrompt());
        assertEquals(0.25D, commandCaptor.getValue().getTemperature());
        assertEquals(256, commandCaptor.getValue().getMaxOutputTokens());

        ArgumentCaptor<AiMessage> messageCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(aiMessageMapper, times(2)).insert(messageCaptor.capture());
        assertEquals("user", messageCaptor.getAllValues().get(0).getRole());
        assertEquals("assistant", messageCaptor.getAllValues().get(1).getRole());
        verify(aiMessageMapper).countBySessionId("ses_123");

        ArgumentCaptor<AiConversation> conversationCaptor = ArgumentCaptor.forClass(AiConversation.class);
        verify(aiConversationMapper).updateMessageState(conversationCaptor.capture());
        assertEquals("chat", conversationCaptor.getValue().getCapabilityCode());
        assertEquals("chat.general", conversationCaptor.getValue().getSceneCode());
        assertEquals("openai-compatible", conversationCaptor.getValue().getProviderCode());
        assertEquals("db-model", conversationCaptor.getValue().getModelCode());
        verify(aiCallLogMapper).insert(any());
    }

    @Test
    void shouldFallbackWhenProviderFails() {
        AiConversation conversation = buildConversation("ses_456", 1002L, "帮我总结一下");
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("帮我总结一下");
        ProviderRuntimeConfig runtimeConfig = ProviderRuntimeConfig.builder()
                .providerCode("openai-compatible")
                .providerName("OpenAI Compatible")
                .enabled(false)
                .baseUrl("https://api.example.com/v1")
                .apiKey("db-key")
                .defaultModelCode("db-model")
                .maxContextMessages(20)
                .temperature(0.7D)
                .maxOutputTokens(1024)
                .systemPrompt("db-system")
                .connectTimeoutMs(5000)
                .readTimeoutMs(30000)
                .build();
        AiChatRoute route = AiChatRoute.builder()
                .assistantType("general")
                .capabilityCode("chat")
                .sceneCode("chat.general")
                .providerCode("openai-compatible")
                .modelCode("db-model")
                .runtimeConfig(runtimeConfig)
                .build();

        given(conversationService.findConversation("ses_456", 1002L)).willReturn(null);
        given(aiChatRouteResolver.resolve(null, request)).willReturn(route);
        given(conversationService.createConversationIfAbsent(eq(1002L), eq("ses_456"), eq("general"), eq("帮我总结一下"),
                eq("chat"), eq("chat.general"), eq("openai-compatible"), eq("db-model")))
                .willReturn(conversation);
        given(providerSelector.resolve("openai-compatible")).willReturn(llmProvider);
        given(llmProvider.isAvailable(runtimeConfig)).willReturn(false);
        given(conversationService.requireConversation("ses_456", 1002L)).willReturn(conversation);
        given(aiMessageMapper.countBySessionId("ses_456")).willReturn(2);

        doAnswer(invocation -> {
            AiMessage message = invocation.getArgument(0);
            if ("user".equals(message.getRole())) {
                message.setId(11L);
            } else {
                message.setId(12L);
            }
            return 1;
        }).when(aiMessageMapper).insert(any(AiMessage.class));

        SendMessageResponse response = agentChatService.sendMessage(1002L, "ses_456", request);

        assertEquals("AI 服务暂不可用，请稍后重试", response.getAssistantMessage().getContent());
        assertEquals("FAILED", response.getAssistantMessage().getMessageStatus());
        assertNotNull(response.getAssistantMessage().getErrorMessage());
        assertTrue(response.getAssistantMessage().getErrorMessage().contains("模型供应商未启用"));
        verify(aiCallLogMapper).insert(any());
        verify(aiConversationMapper).updateMessageState(any());
    }

    private AiConversation buildConversation(String sessionId, Long userId, String title) {
        AiConversation conversation = new AiConversation();
        conversation.setId(1L);
        conversation.setSessionId(sessionId);
        conversation.setUserId(userId);
        conversation.setAssistantType("general");
        conversation.setTitle(title);
        conversation.setStatus(0);
        return conversation;
    }
}
