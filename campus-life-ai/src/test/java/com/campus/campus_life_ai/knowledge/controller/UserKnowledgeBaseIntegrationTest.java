package com.campus.campus_life_ai.knowledge.controller;

import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.ChatCompletionResult;
import com.campus.campus_life_ai.ai.provider.LlmProvider;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.common.properties.JwtProperties;
import com.campus.campus_life_ai.knowledge.vector.KnowledgeEmbeddingClient;
import com.campus.campus_life_ai.knowledge.vector.KnowledgeVectorRecord;
import com.campus.campus_life_ai.knowledge.vector.KnowledgeVectorSearchHit;
import com.campus.campus_life_ai.knowledge.vector.KnowledgeVectorStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "knowledge.vector.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserKnowledgeBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void shouldRunPersonalKnowledgeBaseFullChain() throws Exception {
        String accessToken = createAccessToken(1001L);
        stubActiveToken(1001L, accessToken);
        String authorization = "Bearer " + accessToken;

        String createResponse = mockMvc.perform(post("/api/agent/knowledge-bases")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "校园服务资料",
                                  "description": "个人上传的校园生活资料"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("校园服务资料"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        Long kbId = objectMapper.readTree(createResponse).path("data").path("id").asLong();
        assertTrue(kbId > 0);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "campus-card.md",
                "text/markdown",
                """
                        # 校园一卡通

                        校园一卡通充值流程：打开校园生活服务平台，进入钱包页面，选择一卡通充值。
                        如果支付成功但余额未更新，请在订单详情页查看支付状态并联系客服。
                        """.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/agent/knowledge-bases/{kbId}/documents", kbId)
                        .file(file)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.originalFilename").value("campus-card.md"))
                .andExpect(jsonPath("$.data.parseStatusText").value("INDEXED"))
                .andExpect(jsonPath("$.data.chunkCount").value(1));

        mockMvc.perform(get("/api/agent/knowledge-bases/{kbId}/documents", kbId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].originalFilename").value("campus-card.md"))
                .andExpect(jsonPath("$.data[0].parseStatusText").value("INDEXED"));

        mockMvc.perform(post("/api/ai/knowledge/search")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "一卡通怎么充值",
                                  "knowledgeBaseIds": [%d],
                                  "usePersonalKnowledge": true,
                                  "topK": 3
                                }
                                """.formatted(kbId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].documentTitle").value("campus-card"))
                .andExpect(jsonPath("$.data[0].content").value(containsString("校园一卡通充值流程")));

        String chatResponse = mockMvc.perform(post("/api/agent/conversations/{sessionId}/messages", "ses_personal_kb_chain")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "一卡通怎么充值？",
                                  "sceneCode": "chat.personal_qa",
                                  "providerCode": "integration-test",
                                  "modelCode": "request-model",
                                  "knowledgeBaseIds": [%d],
                                  "usePersonalKnowledge": true
                                }
                                """.formatted(kbId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sceneCode").value("chat.personal_qa"))
                .andExpect(jsonPath("$.data.knowledgeReferences[0].documentTitle").value("campus-card"))
                .andExpect(jsonPath("$.data.assistantMessage.content").value(containsString("校园一卡通充值流程")))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode response = objectMapper.readTree(chatResponse);
        assertEquals("request-model", response.path("data").path("modelCode").asText());

        Integer chunkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ai_knowledge_chunk WHERE kb_id = ?",
                Integer.class,
                kbId
        );
        assertEquals(1, chunkCount);
    }

    private String createAccessToken(Long userId) {
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", "USER")
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
        @Primary
        KnowledgeEmbeddingClient testEmbeddingClient() {
            return texts -> texts.stream()
                    .map(text -> List.of(
                            text.contains("一卡通") ? 1.0f : 0.0f,
                            text.contains("充值") ? 1.0f : 0.0f,
                            text.contains("客服") ? 1.0f : 0.0f
                    ))
                    .toList();
        }

        @Bean
        @Primary
        KnowledgeVectorStore testVectorStore() {
            return new InMemoryKnowledgeVectorStore();
        }

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

    private static class InMemoryKnowledgeVectorStore implements KnowledgeVectorStore {

        private final Map<String, StoredVector> vectors = new ConcurrentHashMap<>();

        @Override
        public void upsert(List<KnowledgeVectorRecord> records) {
            records.forEach(record -> vectors.put(record.getChunk().getVectorId(), new StoredVector(
                    record.getKnowledgeBase().getOwnerType(),
                    record.getKnowledgeBase().getOwnerId(),
                    record.getKnowledgeBase().getVisibility(),
                    record.getChunk().getKbId(),
                    record.getChunk().getDocumentId(),
                    record.getChunk().getVectorId(),
                    record.getEmbedding()
            )));
        }

        @Override
        public void deleteByDocumentId(Long documentId) {
            vectors.entrySet().removeIf(entry -> documentId.equals(entry.getValue().documentId()));
        }

        @Override
        public List<KnowledgeVectorSearchHit> search(List<Float> queryVector,
                                                     Long userId,
                                                     List<Long> knowledgeBaseIds,
                                                     boolean includePersonal,
                                                     boolean includePlatform,
                                                     int topK) {
            return vectors.values().stream()
                    .filter(vector -> isAccessible(vector, userId, knowledgeBaseIds, includePersonal, includePlatform))
                    .map(vector -> KnowledgeVectorSearchHit.builder()
                            .vectorId(vector.vectorId())
                            .score(dot(queryVector, vector.embedding()))
                            .build())
                    .sorted(Comparator.comparing(KnowledgeVectorSearchHit::getScore).reversed())
                    .limit(topK)
                    .toList();
        }

        private boolean isAccessible(StoredVector vector,
                                     Long userId,
                                     List<Long> knowledgeBaseIds,
                                     boolean includePersonal,
                                     boolean includePlatform) {
            if (includePersonal && "USER".equals(vector.ownerType()) && userId.equals(vector.ownerId())) {
                return knowledgeBaseIds == null || knowledgeBaseIds.isEmpty() || knowledgeBaseIds.contains(vector.kbId());
            }
            return includePlatform && "PLATFORM".equals(vector.ownerType()) && "PUBLIC".equals(vector.visibility());
        }

        private float dot(List<Float> left, List<Float> right) {
            float result = 0.0f;
            for (int i = 0; i < Math.min(left.size(), right.size()); i++) {
                result += left.get(i) * right.get(i);
            }
            return result;
        }
    }

    private record StoredVector(String ownerType,
                                Long ownerId,
                                String visibility,
                                Long kbId,
                                Long documentId,
                                String vectorId,
                                List<Float> embedding) {
    }
}
