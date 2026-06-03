package com.campus.campus_life_ai.knowledge.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocument {
    private Long id;
    private Long kbId;
    private String title;
    private String originalFilename;
    private String filePath;
    private Long fileSize;
    private String contentHash;
    private Integer status;
    private Integer parseStatus;
    private String errorMessage;
    private Integer chunkCount;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
