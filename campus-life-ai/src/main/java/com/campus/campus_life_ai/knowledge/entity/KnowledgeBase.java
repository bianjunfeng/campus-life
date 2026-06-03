package com.campus.campus_life_ai.knowledge.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBase {
    private Long id;
    private String ownerType;
    private Long ownerId;
    private String name;
    private String description;
    private String visibility;
    private Integer status;
    private Integer documentCount;
    private Long totalSize;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
