package com.campus.campus_life_ai.knowledge.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "knowledge.embedding")
public class KnowledgeEmbeddingProperties {
    private String client = "spring-ai";
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String apiKey;
    private String model = "text-embedding-v4";
    private Integer dimensions = 1024;
    private Integer maxBatchSize = 10;
    private Integer connectTimeoutMs = 5000;
    private Integer readTimeoutMs = 30000;
}
