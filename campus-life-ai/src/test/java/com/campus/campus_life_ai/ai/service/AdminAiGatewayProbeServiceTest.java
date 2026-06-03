package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.AiGatewayProbeRequest;
import com.campus.campus_life_ai.ai.dto.AiGatewayProbeResponse;
import com.campus.campus_life_ai.ai.dto.AiProviderHealthCheckResponse;
import com.campus.campus_life_ai.ai.entity.AiProviderConfig;
import com.campus.campus_life_ai.ai.gateway.GatewayCallException;
import com.campus.campus_life_ai.ai.gateway.GatewayErrorCode;
import com.campus.campus_life_ai.ai.gateway.LlmGatewayFacade;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayUsage;
import com.campus.campus_life_ai.ai.mapper.AiProviderConfigMapper;
import com.campus.campus_life_ai.ai.provider.LlmProvider;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.provider.ProviderSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminAiGatewayProbeServiceTest {

    @Mock
    private AiProviderConfigMapper aiProviderConfigMapper;
    @Mock
    private AiRuntimeConfigService aiRuntimeConfigService;
    @Mock
    private ProviderSelector providerSelector;
    @Mock
    private AiChatRouteResolver aiChatRouteResolver;
    @Mock
    private LlmGatewayFacade llmGatewayFacade;
    @Mock
    private LlmProvider llmProvider;

    private AdminAiGatewayProbeService service;

    @BeforeEach
    void setUp() {
        service = new AdminAiGatewayProbeService(
                aiProviderConfigMapper,
                aiRuntimeConfigService,
                providerSelector,
                aiChatRouteResolver,
                llmGatewayFacade
        );
    }

    @Test
    void shouldCheckProviderHealthFromRuntimeConfig() {
        AiProviderConfig config = providerConfig();
        ProviderRuntimeConfig runtimeConfig = runtimeConfig();
        given(aiProviderConfigMapper.findById(1L)).willReturn(config);
        given(aiRuntimeConfigService.resolveProvider("openai")).willReturn(runtimeConfig);
        given(providerSelector.resolve("openai")).willReturn(llmProvider);
        given(llmProvider.isAvailable(runtimeConfig)).willReturn(true);

        AiProviderHealthCheckResponse response = service.checkProvider(1L);

        assertEquals("openai", response.getProviderCode());
        assertTrue(response.getEnabled());
        assertTrue(response.getAvailable());
        assertTrue(response.getApiKeyConfigured());
        assertEquals("Provider 配置可用", response.getMessage());
    }

    @Test
    void shouldReturnUnavailableHealthWhenProviderResolverFails() {
        AiProviderConfig config = providerConfig();
        given(aiProviderConfigMapper.findById(1L)).willReturn(config);
        given(aiRuntimeConfigService.resolveProvider("openai")).willThrow(new IllegalStateException("missing provider"));

        AiProviderHealthCheckResponse response = service.checkProvider(1L);

        assertEquals("openai", response.getProviderCode());
        assertFalse(response.getAvailable());
        assertEquals("missing provider", response.getMessage());
    }

    @Test
    void shouldProbeGatewayAndMapSuccessResponse() {
        AiGatewayProbeRequest request = probeRequest();
        ProviderRuntimeConfig runtimeConfig = runtimeConfig();
        AiChatRoute route = AiChatRoute.builder()
                .capabilityCode("chat")
                .sceneCode("chat.general")
                .providerCode("openai")
                .modelCode("gpt-test")
                .runtimeConfig(runtimeConfig)
                .build();
        given(aiChatRouteResolver.resolve(eq(null), any())).willReturn(route);
        given(llmGatewayFacade.chat(any(GatewayRequest.class))).willAnswer(invocation -> {
            GatewayRequest gatewayRequest = invocation.getArgument(0);
            assertEquals(7L, gatewayRequest.getUserId());
            assertEquals("chat", gatewayRequest.getCapabilityCode());
            assertEquals("chat.general", gatewayRequest.getSceneCode());
            assertEquals("openai", gatewayRequest.getProviderCode());
            assertEquals("gpt-test", gatewayRequest.getCommand().getModelCode());
            assertEquals("ping", gatewayRequest.getCommand().getMessages().get(0).getContent());
            return GatewayResponse.builder()
                    .requestId(gatewayRequest.getRequestId())
                    .content("pong")
                    .providerCode("openai")
                    .modelCode("gpt-test")
                    .usage(GatewayUsage.builder()
                            .promptTokens(3)
                            .completionTokens(4)
                            .totalTokens(7)
                            .costAmount(new BigDecimal("0.0012"))
                            .build())
                    .latencyMs(88)
                    .fallbackLevel(1)
                    .finishReason("stop")
                    .build();
        });

        AiGatewayProbeResponse response = service.probe(7L, request);

        assertTrue(response.getSuccess());
        assertNotNull(response.getRequestId());
        assertEquals("pong", response.getContent());
        assertEquals(7, response.getTotalTokens());
        assertEquals(new BigDecimal("0.0012"), response.getCostAmount());
        assertEquals(1, response.getFallbackLevel());
    }

    @Test
    void shouldMapGatewayFailureAsProbeResponse() {
        AiGatewayProbeRequest request = probeRequest();
        ProviderRuntimeConfig runtimeConfig = runtimeConfig();
        AiChatRoute route = AiChatRoute.builder()
                .capabilityCode("chat")
                .sceneCode("chat.general")
                .providerCode("openai")
                .modelCode("gpt-test")
                .runtimeConfig(runtimeConfig)
                .build();
        GatewayError error = GatewayError.builder()
                .errorCode(GatewayErrorCode.RATE_LIMITED.getCode())
                .errorType(GatewayErrorCode.RATE_LIMITED.getType())
                .message("too many requests")
                .build();
        given(aiChatRouteResolver.resolve(eq(null), any())).willReturn(route);
        given(llmGatewayFacade.chat(any(GatewayRequest.class))).willThrow(new GatewayCallException(error, null));

        AiGatewayProbeResponse response = service.probe(7L, request);

        assertFalse(response.getSuccess());
        assertEquals("RATE_LIMITED", response.getErrorCode());
        assertEquals("too many requests", response.getErrorMessage());
    }

    private AiProviderConfig providerConfig() {
        AiProviderConfig config = new AiProviderConfig();
        config.setId(1L);
        config.setProviderCode("openai");
        config.setProviderName("OpenAI");
        config.setBaseUrl("https://api.example.com");
        config.setDefaultModelCode("gpt-test");
        config.setApiKeyCipher("cipher");
        config.setEnabled(1);
        return config;
    }

    private ProviderRuntimeConfig runtimeConfig() {
        return ProviderRuntimeConfig.builder()
                .providerCode("openai")
                .providerName("OpenAI")
                .enabled(true)
                .baseUrl("https://api.example.com")
                .apiKey("sk-test")
                .defaultModelCode("gpt-test")
                .maxContextMessages(8)
                .temperature(0.7D)
                .maxOutputTokens(256)
                .systemPrompt("You are helpful.")
                .connectTimeoutMs(1000)
                .readTimeoutMs(30000)
                .build();
    }

    private AiGatewayProbeRequest probeRequest() {
        AiGatewayProbeRequest request = new AiGatewayProbeRequest();
        request.setContent("ping");
        request.setCapabilityCode("chat");
        request.setSceneCode("chat.general");
        request.setTemperature(0.2D);
        request.setMaxOutputTokens(128);
        return request;
    }
}
