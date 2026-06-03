package com.campus.campus_life_ai.knowledge.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "knowledge.vector")
public class KnowledgeVectorProperties {
    private String client = "spring-ai";
    private Boolean enabled = true;
    private Boolean fallbackKeywordSearch = true;
    private String uri = "http://localhost:19530";
    private String token = "";
    private String username = "";
    private String password = "";
    private String databaseName = "default";
    private String collectionName = "campus_life_knowledge_chunks";
    private String vectorFieldName = "embedding";
    private String metricType = "COSINE";
    private Integer connectTimeoutMs = 5000;
    private Integer rpcDeadlineMs = 30000;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public boolean isFallbackKeywordSearch() {
        return Boolean.TRUE.equals(fallbackKeywordSearch);
    }
}
