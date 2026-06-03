package com.campus.campus_life_ai.knowledge.vector;

import com.campus.campus_life_ai.knowledge.entity.KnowledgeBase;
import com.campus.campus_life_ai.knowledge.entity.KnowledgeChunk;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeEmbeddingProperties;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeVectorProperties;
import io.micrometer.observation.ObservationRegistry;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.milvus.MilvusSearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "knowledge.vector.client", havingValue = "spring-ai", matchIfMissing = true)
public class SpringAiMilvusKnowledgeVectorStore implements KnowledgeVectorStore {

    private static final String FIELD_VECTOR_ID = "vector_id";
    private static final String FIELD_KB_ID = "kb_id";
    private static final String FIELD_DOCUMENT_ID = "document_id";
    private static final String FIELD_OWNER_TYPE = "owner_type";
    private static final String FIELD_OWNER_ID = "owner_id";
    private static final String FIELD_VISIBILITY = "visibility";
    private static final String FIELD_CHUNK_INDEX = "chunk_index";
    private static final String CONTENT_FIELD_NAME = "content";
    private static final String METADATA_FIELD_NAME = "metadata";
    private static final String QUERY_PLACEHOLDER = "__precomputed_query_vector__";

    private final KnowledgeVectorProperties vectorProperties;
    private final KnowledgeEmbeddingProperties embeddingProperties;
    private final PrecomputedEmbeddingModel embeddingModel;
    private volatile MilvusVectorStore vectorStore;

    public SpringAiMilvusKnowledgeVectorStore(KnowledgeVectorProperties vectorProperties,
                                              KnowledgeEmbeddingProperties embeddingProperties) {
        this.vectorProperties = vectorProperties;
        this.embeddingProperties = embeddingProperties;
        this.embeddingModel = new PrecomputedEmbeddingModel(embeddingProperties.getDimensions());
    }

    @Override
    public void upsert(List<KnowledgeVectorRecord> records) {
        if (!vectorProperties.isEnabled() || records == null || records.isEmpty()) {
            return;
        }

        List<KnowledgeVectorRecord> validRecords = records.stream()
                .filter(this::isValidRecord)
                .toList();
        if (validRecords.isEmpty()) {
            return;
        }

        MilvusVectorStore store = vectorStore();
        List<String> vectorIds = validRecords.stream()
                .map(record -> record.getChunk().getVectorId())
                .distinct()
                .toList();
        store.delete(vectorIds);

        List<Document> documents = validRecords.stream()
                .map(this::toDocument)
                .toList();
        List<List<Float>> embeddings = validRecords.stream()
                .map(KnowledgeVectorRecord::getEmbedding)
                .toList();
        embeddingModel.withBatchEmbeddings(embeddings, () -> {
            store.add(documents);
            return null;
        });
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (!vectorProperties.isEnabled() || documentId == null || documentId <= 0) {
            return;
        }
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq(FIELD_DOCUMENT_ID, documentId)
                .build();
        vectorStore().delete(expression);
    }

    @Override
    public List<KnowledgeVectorSearchHit> search(List<Float> queryVector,
                                                 Long userId,
                                                 List<Long> knowledgeBaseIds,
                                                 boolean includePersonal,
                                                 boolean includePlatform,
                                                 int topK) {
        if (!vectorProperties.isEnabled() || queryVector == null || queryVector.isEmpty() || topK <= 0) {
            return List.of();
        }
        String filter = buildAccessFilter(userId, knowledgeBaseIds, includePersonal, includePlatform);
        if (!StringUtils.hasText(filter)) {
            return List.of();
        }

        MilvusSearchRequest request = MilvusSearchRequest.milvusBuilder()
                .query(QUERY_PLACEHOLDER)
                .topK(topK)
                .similarityThresholdAll()
                .nativeExpression(filter)
                .searchParamsJson("{}")
                .build();

        List<Document> documents = embeddingModel.withQueryEmbedding(queryVector,
                () -> vectorStore().similaritySearch(request));
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .map(document -> KnowledgeVectorSearchHit.builder()
                        .vectorId(document.getId())
                        .score(document.getScore() == null ? null : document.getScore().floatValue())
                        .build())
                .filter(hit -> StringUtils.hasText(hit.getVectorId()))
                .toList();
    }

    private Document toDocument(KnowledgeVectorRecord record) {
        KnowledgeBase knowledgeBase = record.getKnowledgeBase();
        KnowledgeChunk chunk = record.getChunk();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(FIELD_VECTOR_ID, chunk.getVectorId());
        metadata.put(FIELD_KB_ID, chunk.getKbId());
        metadata.put(FIELD_DOCUMENT_ID, chunk.getDocumentId());
        metadata.put(FIELD_OWNER_TYPE, knowledgeBase.getOwnerType());
        metadata.put(FIELD_OWNER_ID, knowledgeBase.getOwnerId() == null ? 0L : knowledgeBase.getOwnerId());
        metadata.put(FIELD_VISIBILITY, knowledgeBase.getVisibility());
        metadata.put(FIELD_CHUNK_INDEX, chunk.getChunkIndex() == null ? 0L : chunk.getChunkIndex().longValue());

        String content = StringUtils.hasText(chunk.getContent()) ? chunk.getContent() : chunk.getVectorId();
        return Document.builder()
                .id(chunk.getVectorId())
                .text(content)
                .metadata(metadata)
                .build();
    }

    private boolean isValidRecord(KnowledgeVectorRecord record) {
        return record != null
                && record.getKnowledgeBase() != null
                && record.getChunk() != null
                && StringUtils.hasText(record.getChunk().getVectorId())
                && record.getEmbedding() != null
                && !record.getEmbedding().isEmpty();
    }

    private String buildAccessFilter(Long userId,
                                     List<Long> knowledgeBaseIds,
                                     boolean includePersonal,
                                     boolean includePlatform) {
        List<String> filters = new ArrayList<>();
        if (includePersonal && userId != null) {
            String personalFilter = metadataKey(FIELD_OWNER_TYPE) + " == \"USER\" && "
                    + metadataKey(FIELD_OWNER_ID) + " == " + userId;
            List<Long> ids = normalizeIds(knowledgeBaseIds);
            if (!ids.isEmpty()) {
                personalFilter += " && " + metadataKey(FIELD_KB_ID) + " in [" + ids.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")) + "]";
            }
            filters.add("(" + personalFilter + ")");
        }
        if (includePlatform) {
            filters.add("(" + metadataKey(FIELD_OWNER_TYPE) + " == \"PLATFORM\" && "
                    + metadataKey(FIELD_VISIBILITY) + " == \"PUBLIC\")");
        }
        return String.join(" || ", filters);
    }

    private String metadataKey(String key) {
        return METADATA_FIELD_NAME + "[\"" + key + "\"]";
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private MilvusVectorStore vectorStore() {
        MilvusVectorStore current = vectorStore;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (vectorStore == null) {
                vectorStore = buildVectorStore();
                try {
                    vectorStore.afterPropertiesSet();
                } catch (Exception ex) {
                    throw new IllegalStateException("Failed to initialize Spring AI Milvus vector store", ex);
                }
            }
            return vectorStore;
        }
    }

    private MilvusVectorStore buildVectorStore() {
        return MilvusVectorStore.builder(buildMilvusClient(), embeddingModel)
                .databaseName(vectorProperties.getDatabaseName())
                .collectionName(vectorProperties.getCollectionName())
                .iDFieldName(FIELD_VECTOR_ID)
                .contentFieldName(CONTENT_FIELD_NAME)
                .metadataFieldName(METADATA_FIELD_NAME)
                .embeddingFieldName(vectorProperties.getVectorFieldName())
                .embeddingDimension(embeddingProperties.getDimensions())
                .indexType(IndexType.AUTOINDEX)
                .indexParameters("{}")
                .metricType(resolveMetricType())
                .initializeSchema(true)
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    private MilvusServiceClient buildMilvusClient() {
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withUri(vectorProperties.getUri())
                .withConnectTimeout(resolveTimeout(vectorProperties.getConnectTimeoutMs(), 5000), TimeUnit.MILLISECONDS)
                .withRpcDeadline(resolveTimeout(vectorProperties.getRpcDeadlineMs(), 30000), TimeUnit.MILLISECONDS);
        if (StringUtils.hasText(vectorProperties.getDatabaseName())) {
            builder.withDatabaseName(vectorProperties.getDatabaseName());
        }
        if (StringUtils.hasText(vectorProperties.getToken())) {
            builder.withToken(vectorProperties.getToken());
        }
        if (StringUtils.hasText(vectorProperties.getUsername()) || StringUtils.hasText(vectorProperties.getPassword())) {
            builder.withAuthorization(vectorProperties.getUsername(), vectorProperties.getPassword());
        }
        return new MilvusServiceClient(builder.build());
    }

    private MetricType resolveMetricType() {
        if (!StringUtils.hasText(vectorProperties.getMetricType())) {
            return MetricType.COSINE;
        }
        return MetricType.valueOf(vectorProperties.getMetricType().trim().toUpperCase(Locale.ROOT));
    }

    private long resolveTimeout(Integer timeoutMs, int fallbackMs) {
        return timeoutMs == null || timeoutMs <= 0 ? fallbackMs : timeoutMs.longValue();
    }

    private static final class PrecomputedEmbeddingModel implements EmbeddingModel {

        private final int dimensions;
        private final ThreadLocal<List<float[]>> batchEmbeddings = new ThreadLocal<>();
        private final ThreadLocal<float[]> queryEmbedding = new ThreadLocal<>();

        private PrecomputedEmbeddingModel(Integer dimensions) {
            this.dimensions = dimensions == null || dimensions <= 0 ? 1536 : dimensions;
        }

        private <T> T withBatchEmbeddings(List<List<Float>> embeddings, Supplier<T> action) {
            batchEmbeddings.set(embeddings.stream()
                    .map(this::toPrimitive)
                    .toList());
            try {
                return action.get();
            } finally {
                batchEmbeddings.remove();
            }
        }

        private <T> T withQueryEmbedding(List<Float> embedding, Supplier<T> action) {
            queryEmbedding.set(toPrimitive(embedding));
            try {
                return action.get();
            } finally {
                queryEmbedding.remove();
            }
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<String> inputs = request.getInstructions();
            List<float[]> vectors = resolveEmbeddings(inputs == null ? 0 : inputs.size());
            List<Embedding> embeddings = new ArrayList<>(vectors.size());
            for (int i = 0; i < vectors.size(); i++) {
                embeddings.add(new Embedding(vectors.get(i), i));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return resolveEmbeddings(1).get(0);
        }

        @Override
        public float[] embed(String text) {
            return resolveEmbeddings(1).get(0);
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            return resolveEmbeddings(texts == null ? 0 : texts.size());
        }

        @Override
        public List<float[]> embed(List<Document> documents,
                                   EmbeddingOptions options,
                                   BatchingStrategy batchingStrategy) {
            return resolveEmbeddings(documents == null ? 0 : documents.size());
        }

        @Override
        public int dimensions() {
            return dimensions;
        }

        private List<float[]> resolveEmbeddings(int expectedSize) {
            if (expectedSize <= 0) {
                return List.of();
            }
            float[] queryVector = queryEmbedding.get();
            if (queryVector != null) {
                if (expectedSize != 1) {
                    throw new IllegalStateException("Query embedding can only satisfy one input");
                }
                return List.of(queryVector.clone());
            }

            List<float[]> vectors = batchEmbeddings.get();
            if (vectors == null || vectors.size() != expectedSize) {
                throw new IllegalStateException("Precomputed embeddings are not available for this vector operation");
            }
            return vectors.stream()
                    .map(float[]::clone)
                    .toList();
        }

        private float[] toPrimitive(List<Float> vector) {
            if (vector == null || vector.isEmpty()) {
                throw new IllegalArgumentException("Embedding vector must not be empty");
            }
            float[] result = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) {
                Float value = vector.get(i);
                result[i] = value == null ? 0.0f : value;
            }
            return result;
        }
    }
}
