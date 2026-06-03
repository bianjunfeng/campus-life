package com.campus.campus_life_ai.knowledge.vector;

import com.campus.campus_life_ai.knowledge.entity.KnowledgeBase;
import com.campus.campus_life_ai.knowledge.entity.KnowledgeChunk;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeEmbeddingProperties;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeVectorProperties;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "knowledge.vector.client", havingValue = "legacy")
public class MilvusKnowledgeVectorStore implements KnowledgeVectorStore {

    private static final String FIELD_VECTOR_ID = "vector_id";
    private static final String FIELD_KB_ID = "kb_id";
    private static final String FIELD_DOCUMENT_ID = "document_id";
    private static final String FIELD_OWNER_TYPE = "owner_type";
    private static final String FIELD_OWNER_ID = "owner_id";
    private static final String FIELD_VISIBILITY = "visibility";
    private static final String FIELD_CHUNK_INDEX = "chunk_index";

    private final KnowledgeVectorProperties vectorProperties;
    private final KnowledgeEmbeddingProperties embeddingProperties;
    private final Gson gson = new Gson();
    private volatile MilvusClientV2 client;
    private volatile boolean collectionReady;

    public MilvusKnowledgeVectorStore(KnowledgeVectorProperties vectorProperties,
                                      KnowledgeEmbeddingProperties embeddingProperties) {
        this.vectorProperties = vectorProperties;
        this.embeddingProperties = embeddingProperties;
    }

    @Override
    public void upsert(List<KnowledgeVectorRecord> records) {
        if (!vectorProperties.isEnabled() || records == null || records.isEmpty()) {
            return;
        }
        ensureCollection();
        List<JsonObject> rows = records.stream()
                .map(this::toRow)
                .toList();
        client().upsert(UpsertReq.builder()
                .collectionName(vectorProperties.getCollectionName())
                .data(rows)
                .build());
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (!vectorProperties.isEnabled() || documentId == null || documentId <= 0) {
            return;
        }
        ensureCollection();
        client().delete(DeleteReq.builder()
                .collectionName(vectorProperties.getCollectionName())
                .filter(FIELD_DOCUMENT_ID + " == " + documentId)
                .build());
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
        ensureCollection();
        String filter = buildAccessFilter(userId, knowledgeBaseIds, includePersonal, includePlatform);
        if (!StringUtils.hasText(filter)) {
            return List.of();
        }

        SearchResp response = client().search(SearchReq.builder()
                .collectionName(vectorProperties.getCollectionName())
                .annsField(vectorProperties.getVectorFieldName())
                .metricType(resolveMetricType())
                .data(Collections.singletonList(new FloatVec(queryVector)))
                .filter(filter)
                .topK(topK)
                .outputFields(List.of(FIELD_VECTOR_ID))
                .consistencyLevel(ConsistencyLevel.STRONG)
                .build());

        List<List<SearchResp.SearchResult>> results = response.getSearchResults();
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        List<KnowledgeVectorSearchHit> hits = new ArrayList<>();
        for (SearchResp.SearchResult result : results.get(0)) {
            String vectorId = readVectorId(result);
            if (!StringUtils.hasText(vectorId)) {
                continue;
            }
            hits.add(KnowledgeVectorSearchHit.builder()
                    .vectorId(vectorId)
                    .score(result.getScore())
                    .build());
        }
        return hits;
    }

    private JsonObject toRow(KnowledgeVectorRecord record) {
        KnowledgeBase knowledgeBase = record.getKnowledgeBase();
        KnowledgeChunk chunk = record.getChunk();

        JsonObject row = new JsonObject();
        row.addProperty(FIELD_VECTOR_ID, chunk.getVectorId());
        row.addProperty(FIELD_KB_ID, chunk.getKbId());
        row.addProperty(FIELD_DOCUMENT_ID, chunk.getDocumentId());
        row.addProperty(FIELD_OWNER_TYPE, knowledgeBase.getOwnerType());
        row.addProperty(FIELD_OWNER_ID, knowledgeBase.getOwnerId() == null ? 0L : knowledgeBase.getOwnerId());
        row.addProperty(FIELD_VISIBILITY, knowledgeBase.getVisibility());
        row.addProperty(FIELD_CHUNK_INDEX, chunk.getChunkIndex() == null ? 0L : chunk.getChunkIndex().longValue());
        row.add(vectorProperties.getVectorFieldName(), gson.toJsonTree(record.getEmbedding()));
        return row;
    }

    private synchronized void ensureCollection() {
        if (collectionReady) {
            return;
        }
        MilvusClientV2 current = client();
        boolean exists = current.hasCollection(HasCollectionReq.builder()
                .collectionName(vectorProperties.getCollectionName())
                .build());
        if (!exists) {
            CreateCollectionReq.CollectionSchema schema = current.createSchema();
            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_VECTOR_ID)
                    .dataType(DataType.VarChar)
                    .maxLength(128)
                    .isPrimaryKey(Boolean.TRUE)
                    .autoID(Boolean.FALSE)
                    .build());
            schema.addField(AddFieldReq.builder().fieldName(FIELD_KB_ID).dataType(DataType.Int64).build());
            schema.addField(AddFieldReq.builder().fieldName(FIELD_DOCUMENT_ID).dataType(DataType.Int64).build());
            schema.addField(AddFieldReq.builder().fieldName(FIELD_OWNER_TYPE).dataType(DataType.VarChar).maxLength(16).build());
            schema.addField(AddFieldReq.builder().fieldName(FIELD_OWNER_ID).dataType(DataType.Int64).build());
            schema.addField(AddFieldReq.builder().fieldName(FIELD_VISIBILITY).dataType(DataType.VarChar).maxLength(16).build());
            schema.addField(AddFieldReq.builder().fieldName(FIELD_CHUNK_INDEX).dataType(DataType.Int64).build());
            schema.addField(AddFieldReq.builder()
                    .fieldName(vectorProperties.getVectorFieldName())
                    .dataType(DataType.FloatVector)
                    .dimension(embeddingProperties.getDimensions())
                    .build());

            IndexParam indexParam = IndexParam.builder()
                    .fieldName(vectorProperties.getVectorFieldName())
                    .indexType(IndexParam.IndexType.AUTOINDEX)
                    .metricType(resolveMetricType())
                    .build();
            current.createCollection(CreateCollectionReq.builder()
                    .collectionName(vectorProperties.getCollectionName())
                    .description("Campus life knowledge chunk embeddings")
                    .collectionSchema(schema)
                    .indexParams(List.of(indexParam))
                    .consistencyLevel(ConsistencyLevel.STRONG)
                    .build());
        }

        current.loadCollection(LoadCollectionReq.builder()
                .collectionName(vectorProperties.getCollectionName())
                .async(Boolean.FALSE)
                .timeout(Long.valueOf(vectorProperties.getRpcDeadlineMs()))
                .build());
        collectionReady = true;
    }

    private MilvusClientV2 client() {
        MilvusClientV2 current = client;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (client == null) {
                ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                        .uri(vectorProperties.getUri())
                        .connectTimeoutMs(vectorProperties.getConnectTimeoutMs())
                        .rpcDeadlineMs(vectorProperties.getRpcDeadlineMs());
                if (StringUtils.hasText(vectorProperties.getDatabaseName())) {
                    builder.dbName(vectorProperties.getDatabaseName());
                }
                if (StringUtils.hasText(vectorProperties.getToken())) {
                    builder.token(vectorProperties.getToken());
                }
                if (StringUtils.hasText(vectorProperties.getUsername())) {
                    builder.username(vectorProperties.getUsername());
                }
                if (StringUtils.hasText(vectorProperties.getPassword())) {
                    builder.password(vectorProperties.getPassword());
                }
                client = new MilvusClientV2(builder.build());
            }
            return client;
        }
    }

    private String buildAccessFilter(Long userId,
                                     List<Long> knowledgeBaseIds,
                                     boolean includePersonal,
                                     boolean includePlatform) {
        List<String> filters = new ArrayList<>();
        if (includePersonal && userId != null) {
            String personalFilter = FIELD_OWNER_TYPE + " == \"USER\" && " + FIELD_OWNER_ID + " == " + userId;
            List<Long> ids = normalizeIds(knowledgeBaseIds);
            if (!ids.isEmpty()) {
                personalFilter += " && " + FIELD_KB_ID + " in [" + ids.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")) + "]";
            }
            filters.add("(" + personalFilter + ")");
        }
        if (includePlatform) {
            filters.add("(" + FIELD_OWNER_TYPE + " == \"PLATFORM\" && " + FIELD_VISIBILITY + " == \"PUBLIC\")");
        }
        return String.join(" || ", filters);
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

    private IndexParam.MetricType resolveMetricType() {
        if (!StringUtils.hasText(vectorProperties.getMetricType())) {
            return IndexParam.MetricType.COSINE;
        }
        return IndexParam.MetricType.valueOf(vectorProperties.getMetricType().trim().toUpperCase(Locale.ROOT));
    }

    private String readVectorId(SearchResp.SearchResult result) {
        Map<String, Object> entity = result.getEntity();
        if (entity != null && entity.get(FIELD_VECTOR_ID) != null) {
            return String.valueOf(entity.get(FIELD_VECTOR_ID));
        }
        Object id = result.getId();
        return id == null ? null : String.valueOf(id);
    }
}
