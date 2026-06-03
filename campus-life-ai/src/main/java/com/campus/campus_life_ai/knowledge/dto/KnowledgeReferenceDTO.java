package com.campus.campus_life_ai.knowledge.dto;

import lombok.Data;

@Data
public class KnowledgeReferenceDTO {
    private Long kbId;
    private String knowledgeBaseName;
    private Long documentId;
    private String documentTitle;
    private Integer chunkIndex;
    private Integer score;
}
