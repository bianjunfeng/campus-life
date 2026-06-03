package com.campus.campus_life_ai.knowledge.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "knowledge.storage")
public class KnowledgeStorageProperties {
    private String rootPath = "./uploads/knowledge";
    private String allowedExtensions = "txt,md";
    private long maxFileSizeBytes = 10 * 1024 * 1024L;
    private long maxUserStorageBytes = 200 * 1024 * 1024L;
    private int maxUserBases = 20;
    private int chunkMaxSize = 800;
    private int chunkOverlap = 100;
}
