package com.campus.campus_life_ai.ai.controller;

import com.campus.campus_life_ai.ai.mapper.AiConversationMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.ChatCompletionResult;
import com.campus.campus_life_ai.ai.provider.LlmProvider;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.common.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentConversationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private AiConversationMapper aiConversationMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void shouldPersistAndReuseConversationRouteAcrossTwoMessages() throws Exception {
        String sessionId = "ses_route_chain";
        String accessToken = createAccessToken(1001L);
        stubActiveToken(1001L, accessToken);
        String authorization = "Bearer " + accessToken;

        mockMvc.perform(post("/api/agent/conversations/{sessionId}/messages", sessionId)
                        .header("Authorization", authorization)
                        .contentType("application/json")
                        .content("""
                                {
                                  "content": "第一轮消息",
                                  "sceneCode": "chat.general",
                                  "providerCode": "integration-test",
                                  "modelCode": "request-model"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.capabilityCode").value("chat"))
                .andExpect(jsonPath("$.data.sceneCode").value("chat.general"))
                .andExpect(jsonPath("$.data.providerCode").value("integration-test"))
                .andExpect(jsonPath("$.data.modelCode").value("request-model"))
                .andExpect(jsonPath("$.data.assistantMessage.providerCode").value("integration-test"))
                .andExpect(jsonPath("$.data.assistantMessage.modelCode").value("request-model"))
                .andExpect(jsonPath("$.data.assistantMessage.content").value("reply:第一轮消息|scene system prompt"));

        mockMvc.perform(post("/api/agent/conversations/{sessionId}/messages", sessionId)
                        .header("Authorization", authorization)
                        .contentType("application/json")
                        .content("""
                                {
                                  "content": "第二轮消息"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.providerCode").value("integration-test"))
                .andExpect(jsonPath("$.data.modelCode").value("request-model"))
                .andExpect(jsonPath("$.data.assistantMessage.modelCode").value("request-model"))
                .andExpect(jsonPath("$.data.assistantMessage.content").value("reply:第二轮消息|scene system prompt"));

        var conversation = aiConversationMapper.findBySessionId(sessionId);
        assertNotNull(conversation);
        assertEquals("chat", conversation.getCapabilityCode());
        assertEquals("chat.general", conversation.getSceneCode());
        assertEquals("integration-test", conversation.getProviderCode());
        assertEquals("request-model", conversation.getModelCode());
        assertEquals(4, conversation.getMessageCount());

        Integer messageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ai_message WHERE session_id = ?",
                Integer.class,
                sessionId
        );
        assertEquals(4, messageCount);

        String persistedProvider = jdbcTemplate.queryForObject(
                "SELECT provider_code FROM ai_call_log WHERE session_id = ? ORDER BY id DESC LIMIT 1",
                String.class,
                sessionId
        );
        String persistedModel = jdbcTemplate.queryForObject(
                "SELECT model_code FROM ai_call_log WHERE session_id = ? ORDER BY id DESC LIMIT 1",
                String.class,
                sessionId
        );
        assertEquals("integration-test", persistedProvider);
        assertEquals("request-model", persistedModel);
    }

    private String createAccessToken(Long userId) {
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", "admin")
                .claim("type", "access")
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    private void stubActiveToken(Long userId, String token) {
        given(stringRedisTemplate.hasKey("blacklist:" + token)).willReturn(false);
        given(stringRedisTemplate.hasKey("token:" + userId + ":" + token)).willReturn(true);
    }

    @TestConfiguration
    static class IntegrationTestProviderConfiguration {

        @Bean
        LlmProvider integrationTestProvider() {
            return new LlmProvider() {
                @Override
                public String providerCode() {
                    return "integration-test";
                }

                @Override
                public boolean isAvailable(ProviderRuntimeConfig runtimeConfig) {
                    return true;
                }

                @Override
                public ChatCompletionResult chat(ChatCompletionCommand command, ProviderRuntimeConfig runtimeConfig) {
                    String latestUserMessage = command.getMessages().stream()
                            .filter(message -> "user".equals(message.getRole()))
                            .reduce((first, second) -> second)
                            .map(ChatCompletionCommand.PromptMessage::getContent)
                            .orElse("");
                    return ChatCompletionResult.builder()
                            .providerCode(runtimeConfig.getProviderCode())
                            .modelCode(command.getModelCode())
                            .content("reply:" + latestUserMessage + "|" + command.getSystemPrompt())
                            .promptTokens(11)
                            .completionTokens(7)
                            .totalTokens(18)
                            .latencyMs(123)
                            .finishReason("stop")
                            .build();
                }
            };
        }
    }
}
