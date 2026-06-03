package com.campus.campus_life_ai.knowledge.vector;

import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.service.AiRuntimeConfigService;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeEmbeddingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "knowledge.embedding.client", havingValue = "legacy")
public class DashScopeEmbeddingClient implements KnowledgeEmbeddingClient {

    private final KnowledgeEmbeddingProperties properties;
    private final AiRuntimeConfigService aiRuntimeConfigService;
    private final RestClient restClient;

    public DashScopeEmbeddingClient(KnowledgeEmbeddingProperties properties,
                                    AiRuntimeConfigService aiRuntimeConfigService) {
        this.properties = properties;
        this.aiRuntimeConfigService = aiRuntimeConfigService;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<List<Float>> embed(List<String> texts) {
        List<String> normalizedTexts = normalize(texts);
        if (normalizedTexts.isEmpty()) {
            return List.of();
        }
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("未配置 DashScope Embedding API Key");
        }

        int batchSize = Math.max(1, Math.min(properties.getMaxBatchSize(), 10));
        List<List<Float>> result = new ArrayList<>(normalizedTexts.size());
        for (int start = 0; start < normalizedTexts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, normalizedTexts.size());
            result.addAll(requestEmbeddingBatch(normalizedTexts.subList(start, end), apiKey));
        }
        return result;
    }

    String resolveApiKey() {
        if (StringUtils.hasText(properties.getApiKey())) {
            return properties.getApiKey().trim();
        }
        ProviderRuntimeConfig runtimeConfig = aiRuntimeConfigService.resolveProvider(null);
        return runtimeConfig == null ? null : runtimeConfig.getApiKey();
    }

    private List<List<Float>> requestEmbeddingBatch(List<String> texts, String apiKey) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", properties.getModel());
        payload.put("input", texts);
        payload.put("dimensions", properties.getDimensions());
        payload.put("encoding_format", "float");

        JsonNode response = restClient.post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        JsonNode data = response == null ? null : response.path("data");
        if (data == null || !data.isArray() || data.size() != texts.size()) {
            throw new IllegalStateException("向量化服务返回结果数量不匹配");
        }

        List<List<Float>> vectors = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            vectors.add(null);
        }
        for (JsonNode item : data) {
            int index = item.path("index").asInt(-1);
            if (index < 0 || index >= texts.size()) {
                throw new IllegalStateException("向量化服务返回非法索引");
            }
            vectors.set(index, readVector(item.path("embedding")));
        }
        if (vectors.stream().anyMatch(vector -> vector == null || vector.isEmpty())) {
            throw new IllegalStateException("向量化服务返回空向量");
        }
        return vectors;
    }

    private List<Float> readVector(JsonNode node) {
        if (node == null || !node.isArray()) {
            throw new IllegalStateException("向量化服务返回非法向量");
        }
        List<Float> vector = new ArrayList<>(node.size());
        for (JsonNode value : node) {
            vector.add((float) value.asDouble());
        }
        return vector;
    }

    private List<String> normalize(List<String> texts) {
        if (texts == null) {
            return List.of();
        }
        return texts.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }
}
