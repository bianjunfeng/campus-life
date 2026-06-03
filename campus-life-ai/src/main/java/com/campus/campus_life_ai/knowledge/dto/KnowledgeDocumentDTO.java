package com.campus.campus_life_ai.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentDTO {
    private Long id;
    private Long kbId;
    private String title;
    private String originalFilename;
    private Long fileSize;
    private Integer status;
    private Integer parseStatus;
    private String parseStatusText;
    private String errorMessage;
    private Integer chunkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
