package com.campus.campus_life_ai.knowledge.dto;

import lombok.Data;

@Data
public class KnowledgeSearchResultDTO {
    private Long kbId;
    private String knowledgeBaseName;
    private Long documentId;
    private String documentTitle;
    private Integer chunkIndex;
    private String content;
    private Integer score;
}
