package com.campus.campus_life_ai.knowledge.service;

import lombok.Data;

@Data
public class KnowledgeSearchHit {
    private String vectorId;
    private Long kbId;
    private String knowledgeBaseName;
    private Long documentId;
    private String documentTitle;
    private Integer chunkIndex;
    private String content;
    private Integer score;
}
