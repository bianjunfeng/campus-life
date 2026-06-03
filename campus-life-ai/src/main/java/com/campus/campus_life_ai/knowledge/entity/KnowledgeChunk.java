package com.campus.campus_life_ai.knowledge.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeChunk {
    private Long id;
    private Long kbId;
    private Long documentId;
    private String vectorId;
    private Integer chunkIndex;
    private String content;
    private String contentHash;
    private Integer tokenCount;
    private LocalDateTime createdAt;
}
