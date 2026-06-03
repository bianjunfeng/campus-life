package com.campus.campus_life_ai.ai.gateway;

import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.gateway.observe.GatewayCallLogger;
import com.campus.campus_life_ai.ai.gateway.observe.GatewayCostCalculator;
import com.campus.campus_life_ai.ai.gateway.observe.GatewayMetricsRecorder;
import com.campus.campus_life_ai.ai.gateway.policy.GatewayPolicyChain;
import com.campus.campus_life_ai.ai.gateway.policy.GatewaySafetyInspector;
import com.campus.campus_life_ai.ai.gateway.route.ModelRoutePlanner;
import com.campus.campus_life_ai.ai.gateway.route.ProviderHealthRegistry;
import com.campus.campus_life_ai.ai.gateway.route.RouteCandidate;
import com.campus.campus_life_ai.ai.mapper.AiGatewayRouteRuleMapper;
import com.campus.campus_life_ai.ai.mapper.AiModelConfigMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.ChatCompletionResult;
import com.campus.campus_life_ai.ai.provider.LlmProvider;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.provider.ProviderSelector;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import com.campus.campus_life_ai.ai.service.AiRuntimeConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultLlmGatewayFacadeTest {

    @Mock
    private ProviderSelector providerSelector;

    @Mock
    private LlmProvider llmProvider;

    @Mock
    private GatewayCallLogger gatewayCallLogger;

    @Mock
    private GatewayMetricsRecorder gatewayMetricsRecorder;

    @Mock
    private GatewaySafetyInspector gatewaySafetyInspector;

    private DefaultLlmGatewayFacade facade;

    @BeforeEach
    void setUp() {
        facade = new DefaultLlmGatewayFacade(
                providerSelector,
                gatewayCallLogger,
                gatewayMetricsRecorder,
                new GatewayCostCalculator(org.mockito.Mockito.mock(AiModelConfigMapper.class)),
                new GatewayErrorNormalizer(),
                new GatewayPolicyChain(List.of()),
                gatewaySafetyInspector,
                new ModelRoutePlanner(
                        org.mockito.Mockito.mock(AiGatewayRouteRuleMapper.class),
                        org.mockito.Mockito.mock(AiModelConfigMapper.class),
                        org.mockito.Mockito.mock(AiRuntimeConfigService.class),
                        new ObjectMapper()
                ),
                new ProviderHealthRegistry(new AiProviderProperties())
        );
    }

    @Test
    void shouldDelegateChatToResolvedProvider() {
        ProviderRuntimeConfig runtimeConfig = buildRuntimeConfig(true);
        ChatCompletionCommand command = buildCommand();
        GatewayRequest request = buildRequest(runtimeConfig, command);
        given(providerSelector.resolve("openai-compatible")).willReturn(llmProvider);
        given(llmProvider.isAvailable(runtimeConfig)).willReturn(true);
        given(llmProvider.chat(command, runtimeConfig)).willReturn(ChatCompletionResult.builder()
                .content("reply")
                .providerCode("openai-compatible")
                .modelCode("qwen-plus")
                .promptTokens(3)
                .completionTokens(5)
                .totalTokens(8)
                .latencyMs(120)
                .finishReason("stop")
                .build());

        GatewayResponse response = facade.chat(request);

        assertEquals("req-1", response.getRequestId());
        assertEquals("reply", response.getContent());
        assertEquals("openai-compatible", response.getProviderCode());
        assertEquals("qwen-plus", response.getModelCode());
        assertEquals(8, response.getUsage().getTotalTokens());
        assertEquals(120, response.getLatencyMs());
        verify(llmProvider).chat(command, runtimeConfig);
        verify(gatewayCallLogger).recordSuccess(request, response);
        verify(gatewayMetricsRecorder).recordSuccess(request, response);
    }

    @Test
    void shouldDelegateStreamDeltasToConsumer() {
        ProviderRuntimeConfig runtimeConfig = buildRuntimeConfig(true);
        ChatCompletionCommand command = buildCommand();
        GatewayRequest request = buildRequest(runtimeConfig, command);
        StringBuilder streamed = new StringBuilder();
        given(providerSelector.resolve("openai-compatible")).willReturn(llmProvider);
        given(llmProvider.isAvailable(runtimeConfig)).willReturn(true);
        given(llmProvider.stream(eq(command), eq(runtimeConfig), any())).willAnswer(invocation -> {
            invocation.<com.campus.campus_life_ai.ai.provider.ChatCompletionStreamConsumer>getArgument(2).onDelta("hel");
            invocation.<com.campus.campus_life_ai.ai.provider.ChatCompletionStreamConsumer>getArgument(2).onDelta("lo");
            return ChatCompletionResult.builder()
                    .content("hello")
                    .providerCode("openai-compatible")
                    .modelCode("qwen-plus")
                    .totalTokens(6)
                    .latencyMs(90)
                    .finishReason("stop")
                    .build();
        });

        GatewayResponse response = facade.stream(request, streamed::append);

        assertEquals("hello", response.getContent());
        assertEquals("hello", streamed.toString());
        assertEquals(6, response.getUsage().getTotalTokens());
    }

    @Test
    void shouldRejectUnavailableProvider() {
        ProviderRuntimeConfig runtimeConfig = buildRuntimeConfig(false);
        GatewayRequest request = buildRequest(runtimeConfig, buildCommand());
        given(providerSelector.resolve("openai-compatible")).willReturn(llmProvider);
        given(llmProvider.isAvailable(runtimeConfig)).willReturn(false);

        GatewayCallException exception = assertThrows(GatewayCallException.class, () -> facade.chat(request));

        assertEquals("模型供应商未启用或缺少配置", exception.getMessage());
        assertEquals("PROVIDER_UNAVAILABLE", exception.getError().getErrorCode());
        verify(gatewayCallLogger).recordFailure(eq(request), any());
        verify(gatewayMetricsRecorder).recordFailure(eq(request), any());
    }

    @Test
    void shouldFallbackToNextRouteCandidateWhenPrimaryFails() {
        ProviderRuntimeConfig primaryRuntimeConfig = buildRuntimeConfig(true);
        ProviderRuntimeConfig fallbackRuntimeConfig = ProviderRuntimeConfig.builder()
                .providerCode("openai-compatible")
                .providerName("Qwen")
                .enabled(true)
                .baseUrl("https://example.com/v1")
                .apiKey("test-key")
                .defaultModelCode("qwen-max")
                .build();
        GatewayRequest request = GatewayRequest.builder()
                .requestId("req-fallback")
                .providerCode("openai-compatible")
                .modelCode("qwen-plus")
                .runtimeConfig(primaryRuntimeConfig)
                .command(buildCommand())
                .fallbackCandidates(List.of(RouteCandidate.builder()
                        .providerCode("openai-compatible")
                        .modelCode("qwen-max")
                        .runtimeConfig(fallbackRuntimeConfig)
                        .build()))
                .build();
        given(providerSelector.resolve("openai-compatible")).willReturn(llmProvider);
        given(llmProvider.isAvailable(any())).willReturn(true);
        given(llmProvider.chat(any(), any()))
                .willThrow(new IllegalStateException("primary failed"))
                .willReturn(ChatCompletionResult.builder()
                        .content("fallback reply")
                        .providerCode("openai-compatible")
                        .modelCode("qwen-max")
                        .latencyMs(180)
                        .build());

        GatewayResponse response = facade.chat(request);

        assertEquals("fallback reply", response.getContent());
        assertEquals("qwen-max", response.getModelCode());
        verify(llmProvider, org.mockito.Mockito.times(2)).chat(any(), any());
        verify(gatewayCallLogger).recordSuccess(request, response);
    }

    @Test
    void shouldBlockUnsafeOutputWithoutRetryingFallback() {
        ProviderRuntimeConfig runtimeConfig = buildRuntimeConfig(true);
        ChatCompletionCommand command = buildCommand();
        GatewayRequest request = buildRequest(runtimeConfig, command);
        given(providerSelector.resolve("openai-compatible")).willReturn(llmProvider);
        given(llmProvider.isAvailable(runtimeConfig)).willReturn(true);
        given(llmProvider.chat(command, runtimeConfig)).willReturn(ChatCompletionResult.builder()
                .content("unsafe reply")
                .providerCode("openai-compatible")
                .modelCode("qwen-plus")
                .latencyMs(25)
                .build());
        given(gatewaySafetyInspector.inspectOutput(eq(request), any())).willReturn(
                GatewaySafetyInspector.SafetyDecision.block(
                        "AI 安全策略拦截: 输出规则",
                        List.of("policy"),
                        List.of("输出规则")
                )
        );

        GatewayCallException exception = assertThrows(GatewayCallException.class, () -> facade.chat(request));

        assertEquals(GatewayErrorCode.SAFETY_BLOCKED.getCode(), exception.getError().getErrorCode());
        verify(gatewayCallLogger).recordFailure(eq(request), any());
        verify(llmProvider).chat(command, runtimeConfig);
    }

    private GatewayRequest buildRequest(ProviderRuntimeConfig runtimeConfig, ChatCompletionCommand command) {
        return GatewayRequest.builder()
                .requestId("req-1")
                .userId(1001L)
                .sessionId("ses_1")
                .capabilityCode("chat")
                .sceneCode("chat.general")
                .providerCode("openai-compatible")
                .modelCode("qwen-plus")
                .runtimeConfig(runtimeConfig)
                .command(command)
                .build();
    }

    private ChatCompletionCommand buildCommand() {
        return ChatCompletionCommand.builder()
                .providerCode("openai-compatible")
                .modelCode("qwen-plus")
                .messages(List.of(ChatCompletionCommand.PromptMessage.builder()
                        .role("user")
                        .content("hello")
                        .build()))
                .build();
    }

    private ProviderRuntimeConfig buildRuntimeConfig(boolean enabled) {
        return ProviderRuntimeConfig.builder()
                .providerCode("openai-compatible")
                .providerName("Qwen")
                .enabled(enabled)
                .baseUrl("https://example.com/v1")
                .apiKey("test-key")
                .defaultModelCode("qwen-plus")
                .build();
    }
}
