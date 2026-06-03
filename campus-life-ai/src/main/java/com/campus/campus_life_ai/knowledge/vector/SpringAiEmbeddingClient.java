package com.campus.campus_life_ai.knowledge.vector;

import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.service.AiRuntimeConfigService;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeEmbeddingProperties;
import io.micrometer.observation.ObservationRegistry;
import io.netty.channel.ChannelOption;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@ConditionalOnProperty(name = "knowledge.embedding.client", havingValue = "spring-ai", matchIfMissing = true)
public class SpringAiEmbeddingClient implements KnowledgeEmbeddingClient {

    private static final String EMBEDDINGS_PATH = "/v1/embeddings";

    private final KnowledgeEmbeddingProperties properties;
    private final AiRuntimeConfigService aiRuntimeConfigService;

    public SpringAiEmbeddingClient(KnowledgeEmbeddingProperties properties,
                                   AiRuntimeConfigService aiRuntimeConfigService) {
        this.properties = properties;
        this.aiRuntimeConfigService = aiRuntimeConfigService;
    }

    @Override
    public List<List<Float>> embed(List<String> texts) {
        List<String> normalizedTexts = normalize(texts);
        if (normalizedTexts.isEmpty()) {
            return List.of();
        }
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Embedding API key is not configured");
        }

        OpenAiEmbeddingModel embeddingModel = buildEmbeddingModel(apiKey);
        int batchSize = Math.max(1, Math.min(properties.getMaxBatchSize(), 10));
        List<List<Float>> result = new ArrayList<>(normalizedTexts.size());
        for (int start = 0; start < normalizedTexts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, normalizedTexts.size());
            result.addAll(requestEmbeddingBatch(embeddingModel, normalizedTexts.subList(start, end)));
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

    private OpenAiEmbeddingModel buildEmbeddingModel(String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(resolveTimeout(properties.getConnectTimeoutMs(), 5000));
        requestFactory.setReadTimeout(resolveTimeout(properties.getReadTimeoutMs(), 30000));

        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, resolveTimeout(properties.getConnectTimeoutMs(), 5000))
                .responseTimeout(Duration.ofMillis(resolveTimeout(properties.getReadTimeoutMs(), 30000)));
        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .apiKey(apiKey)
                .embeddingsPath(EMBEDDINGS_PATH)
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();

        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                buildOptions(),
                RetryTemplate.defaultInstance(),
                ObservationRegistry.NOOP
        );
    }

    private OpenAiEmbeddingOptions buildOptions() {
        return OpenAiEmbeddingOptions.builder()
                .model(properties.getModel())
                .encodingFormat("float")
                .dimensions(properties.getDimensions())
                .build();
    }

    private List<List<Float>> requestEmbeddingBatch(OpenAiEmbeddingModel embeddingModel, List<String> texts) {
        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(texts, buildOptions()));
        List<Embedding> embeddings = response == null ? null : response.getResults();
        if (embeddings == null || embeddings.size() != texts.size()) {
            throw new IllegalStateException("Embedding result count does not match input count");
        }

        List<Embedding> orderedEmbeddings = embeddings.stream()
                .sorted(Comparator.comparing(embedding -> embedding.getIndex() == null ? 0 : embedding.getIndex()))
                .toList();

        List<List<Float>> vectors = new ArrayList<>(texts.size());
        for (Embedding embedding : orderedEmbeddings) {
            float[] output = embedding.getOutput();
            if (output == null || output.length == 0) {
                throw new IllegalStateException("Embedding service returned an empty vector");
            }
            vectors.add(toFloatList(output));
        }
        return vectors;
    }

    private List<Float> toFloatList(float[] vector) {
        List<Float> result = new ArrayList<>(vector.length);
        for (float value : vector) {
            result.add(value);
        }
        return result;
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

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized;
    }

    private int resolveTimeout(Integer timeoutMs, int fallbackMs) {
        return timeoutMs == null || timeoutMs <= 0 ? fallbackMs : timeoutMs;
    }
}
