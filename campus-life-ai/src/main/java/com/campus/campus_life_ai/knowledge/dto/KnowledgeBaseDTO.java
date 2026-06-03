package com.campus.campus_life_ai.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseDTO {
    private Long id;
    private String ownerType;
    private String name;
    private String description;
    private String visibility;
    private Integer documentCount;
    private Long totalSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
