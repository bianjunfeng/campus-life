package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.AiModerationCheckRequest;
import com.campus.campus_life_ai.ai.dto.AiModerationCheckResponse;
import com.campus.campus_life_ai.ai.entity.AiCapabilityConfig;
import com.campus.campus_life_ai.ai.entity.AiSceneConfig;
import com.campus.campus_life_ai.ai.gateway.LlmGatewayFacade;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.mapper.AiCapabilityConfigMapper;
import com.campus.campus_life_ai.ai.mapper.AiSceneConfigMapper;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ModerationGatewayServiceTest {

    @Mock
    private AiCapabilityConfigMapper aiCapabilityConfigMapper;
    @Mock
    private AiSceneConfigMapper aiSceneConfigMapper;
    @Mock
    private AiRuntimeConfigService aiRuntimeConfigService;
    @Mock
    private LlmGatewayFacade llmGatewayFacade;

    private ModerationGatewayService service;

    @BeforeEach
    void setUp() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setDefaultProviderCode("openai");
        properties.setDefaultModelCode("gpt-test");
        service = new ModerationGatewayService(
                aiCapabilityConfigMapper,
                aiSceneConfigMapper,
                aiRuntimeConfigService,
                properties,
                llmGatewayFacade,
                new ObjectMapper()
        );
    }

    @Test
    void shouldCallGatewayAndParseJsonDecision() {
        givenEnabledModerationConfig();
        given(aiRuntimeConfigService.resolveProvider("openai")).willReturn(runtimeConfig());
        given(llmGatewayFacade.chat(any(GatewayRequest.class))).willAnswer(invocation -> {
            GatewayRequest gatewayRequest = invocation.getArgument(0);
            assertEquals("moderation", gatewayRequest.getCapabilityCode());
            assertEquals("moderation.text_post", gatewayRequest.getSceneCode());
            assertEquals("openai", gatewayRequest.getProviderCode());
            assertEquals("gpt-moderation", gatewayRequest.getModelCode());
            assertEquals("gpt-moderation", gatewayRequest.getCommand().getModelCode());
            assertEquals(0.0D, gatewayRequest.getCommand().getTemperature());
            assertTrue(gatewayRequest.getCommand().getMessages().get(0).getContent().contains("待审核文本"));
            return GatewayResponse.builder()
                    .requestId(gatewayRequest.getRequestId())
                    .content("{\"result\":\"REJECT\",\"score\":0.92,\"categories\":[\"spam\",\"ad\"],\"reason\":\"广告引流\"}")
                    .providerCode("openai")
                    .modelCode("gpt-moderation")
                    .latencyMs(66)
                    .fallbackLevel(0)
                    .build();
        });

        AiModerationCheckResponse response = service.check(9L, request("加微信返利", null));

        assertEquals("REJECT", response.getResult());
        assertEquals(0.92D, response.getScore());
        assertEquals(List.of("spam", "ad"), response.getCategories());
        assertEquals("广告引流", response.getReason());
        assertEquals("post", response.getTargetType());
        assertEquals("openai", response.getProviderCode());
        assertEquals("gpt-moderation", response.getModelCode());
    }

    @Test
    void shouldFallbackToReviewWhenModelResponseCannotBeParsed() {
        givenEnabledModerationConfig();
        given(aiRuntimeConfigService.resolveProvider("openai")).willReturn(runtimeConfig());
        given(llmGatewayFacade.chat(any(GatewayRequest.class))).willReturn(GatewayResponse.builder()
                .requestId("moderation-test")
                .content("需要人工看一下")
                .providerCode("openai")
                .modelCode("gpt-moderation")
                .build());

        AiModerationCheckResponse response = service.check(9L, request("普通内容", null));

        assertEquals("REVIEW", response.getResult());
        assertEquals(0.5D, response.getScore());
        assertEquals(List.of("parse_error"), response.getCategories());
        assertEquals("模型返回无法解析，建议人工复核", response.getReason());
    }

    @Test
    void shouldRejectNonModerationScene() {
        AiSceneConfig scene = sceneConfig();
        scene.setCapabilityCode("chat");
        given(aiSceneConfigMapper.findBySceneCode("moderation.text_post")).willReturn(scene);
        AiCapabilityConfig capability = capabilityConfig();
        capability.setCapabilityCode("chat");
        given(aiCapabilityConfigMapper.findByCapabilityCode("chat")).willReturn(capability);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.check(9L, request("hello", null))
        );

        assertEquals("当前审核接口仅支持 moderation 能力", exception.getMessage());
    }

    private void givenEnabledModerationConfig() {
        given(aiSceneConfigMapper.findBySceneCode("moderation.text_post")).willReturn(sceneConfig());
        given(aiCapabilityConfigMapper.findByCapabilityCode("moderation")).willReturn(capabilityConfig());
    }

    private AiModerationCheckRequest request(String content, String sceneCode) {
        AiModerationCheckRequest request = new AiModerationCheckRequest();
        request.setContent(content);
        request.setTargetType("post");
        request.setSceneCode(sceneCode);
        return request;
    }

    private AiSceneConfig sceneConfig() {
        AiSceneConfig config = new AiSceneConfig();
        config.setCapabilityCode("moderation");
        config.setSceneCode("moderation.text_post");
        config.setSceneName("帖子文本审核");
        config.setProviderCode("openai");
        config.setModelCode("gpt-moderation");
        config.setEnabled(1);
        config.setSystemPromptTemplate("只返回审核 JSON");
        config.setTimeoutMs(30000);
        return config;
    }

    private AiCapabilityConfig capabilityConfig() {
        AiCapabilityConfig config = new AiCapabilityConfig();
        config.setCapabilityCode("moderation");
        config.setCapabilityName("AI 内容审核");
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
                .defaultModelCode("gpt-base")
                .maxContextMessages(8)
                .temperature(0.7D)
                .maxOutputTokens(512)
                .systemPrompt("default prompt")
                .connectTimeoutMs(1000)
                .readTimeoutMs(30000)
                .build();
    }
}
