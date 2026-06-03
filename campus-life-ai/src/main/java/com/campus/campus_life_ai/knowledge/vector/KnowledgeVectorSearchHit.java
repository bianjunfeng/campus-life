package com.campus.campus_life_ai.knowledge.vector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeVectorSearchHit {
    private String vectorId;
    private Float score;
}
