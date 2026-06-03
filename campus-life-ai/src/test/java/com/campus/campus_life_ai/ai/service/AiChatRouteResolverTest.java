package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.SendMessageRequest;
import com.campus.campus_life_ai.ai.entity.AiCapabilityConfig;
import com.campus.campus_life_ai.ai.entity.AiConversation;
import com.campus.campus_life_ai.ai.entity.AiSceneConfig;
import com.campus.campus_life_ai.ai.mapper.AiCapabilityConfigMapper;
import com.campus.campus_life_ai.ai.mapper.AiSceneConfigMapper;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AiChatRouteResolverTest {

    @Mock
    private AiCapabilityConfigMapper aiCapabilityConfigMapper;

    @Mock
    private AiSceneConfigMapper aiSceneConfigMapper;

    @Mock
    private AiRuntimeConfigService aiRuntimeConfigService;

    private AiProviderProperties aiProviderProperties;

    private AiChatRouteResolver aiChatRouteResolver;

    @BeforeEach
    void setUp() {
        aiProviderProperties = new AiProviderProperties();
        aiProviderProperties.setDefaultProviderCode("default-provider");
        aiProviderProperties.setDefaultModelCode("default-model");
        aiProviderProperties.setDefaultAssistantType("general");
        aiProviderProperties.setSystemPrompt("global system");
        aiChatRouteResolver = new AiChatRouteResolver(
                aiCapabilityConfigMapper,
                aiSceneConfigMapper,
                aiRuntimeConfigService,
                aiProviderProperties
        );
    }

    @Test
    void shouldUseSceneRouteWhenSwitchingSceneWithoutExplicitProviderOrModel() {
        AiConversation conversation = new AiConversation();
        conversation.setAssistantType("general");
        conversation.setCapabilityCode("chat");
        conversation.setSceneCode("chat.old");
        conversation.setProviderCode("saved-provider");
        conversation.setModelCode("saved-model");

        SendMessageRequest request = new SendMessageRequest();
        request.setContent("你好");
        request.setSceneCode("chat.general");

        given(aiSceneConfigMapper.findBySceneCode("chat.general")).willReturn(buildScene("chat.general", "scene-provider", "scene-model", "scene system"));
        given(aiCapabilityConfigMapper.findByCapabilityCode("chat")).willReturn(buildCapability("chat"));
        given(aiRuntimeConfigService.resolveProvider("scene-provider")).willReturn(buildRuntime("scene-provider", "provider-model", "provider system"));

        AiChatRoute route = aiChatRouteResolver.resolve(conversation, request);

        assertEquals("chat", route.getCapabilityCode());
        assertEquals("chat.general", route.getSceneCode());
        assertEquals("scene-provider", route.getProviderCode());
        assertEquals("scene-model", route.getModelCode());
        assertEquals("scene system", route.getRuntimeConfig().getSystemPrompt());
    }

    @Test
    void shouldReuseConversationRouteWhenRequestDoesNotOverrideIt() {
        AiConversation conversation = new AiConversation();
        conversation.setAssistantType("general");
        conversation.setCapabilityCode("chat");
        conversation.setSceneCode("chat.general");
        conversation.setProviderCode("saved-provider");
        conversation.setModelCode("saved-model");

        SendMessageRequest request = new SendMessageRequest();
        request.setContent("继续");

        given(aiSceneConfigMapper.findBySceneCode("chat.general")).willReturn(buildScene("chat.general", "scene-provider", "scene-model", "scene system"));
        given(aiCapabilityConfigMapper.findByCapabilityCode("chat")).willReturn(buildCapability("chat"));
        given(aiRuntimeConfigService.resolveProvider("saved-provider")).willReturn(buildRuntime("saved-provider", "provider-model", "provider system"));

        AiChatRoute route = aiChatRouteResolver.resolve(conversation, request);

        assertEquals("saved-provider", route.getProviderCode());
        assertEquals("saved-model", route.getModelCode());
        assertEquals("scene system", route.getRuntimeConfig().getSystemPrompt());
    }

    private AiCapabilityConfig buildCapability(String capabilityCode) {
        AiCapabilityConfig capabilityConfig = new AiCapabilityConfig();
        capabilityConfig.setCapabilityCode(capabilityCode);
        capabilityConfig.setEnabled(1);
        return capabilityConfig;
    }

    private AiSceneConfig buildScene(String sceneCode, String providerCode, String modelCode, String systemPrompt) {
        AiSceneConfig sceneConfig = new AiSceneConfig();
        sceneConfig.setCapabilityCode("chat");
        sceneConfig.setSceneCode(sceneCode);
        sceneConfig.setProviderCode(providerCode);
        sceneConfig.setModelCode(modelCode);
        sceneConfig.setSystemPromptTemplate(systemPrompt);
        sceneConfig.setEnabled(1);
        sceneConfig.setTimeoutMs(18000);
        return sceneConfig;
    }

    private ProviderRuntimeConfig buildRuntime(String providerCode, String modelCode, String systemPrompt) {
        return ProviderRuntimeConfig.builder()
                .providerCode(providerCode)
                .providerName(providerCode)
                .enabled(true)
                .baseUrl("https://example.com/v1")
                .apiKey("test-key")
                .defaultModelCode(modelCode)
                .maxContextMessages(12)
                .temperature(0.3D)
                .maxOutputTokens(512)
                .systemPrompt(systemPrompt)
                .connectTimeoutMs(5000)
                .readTimeoutMs(30000)
                .build();
    }
}
