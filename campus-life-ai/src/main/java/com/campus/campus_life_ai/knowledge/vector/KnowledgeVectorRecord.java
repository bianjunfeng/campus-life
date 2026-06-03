package com.campus.campus_life_ai.knowledge.vector;

import com.campus.campus_life_ai.knowledge.entity.KnowledgeBase;
import com.campus.campus_life_ai.knowledge.entity.KnowledgeChunk;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KnowledgeVectorRecord {
    private KnowledgeBase knowledgeBase;
    private KnowledgeChunk chunk;
    private List<Float> embedding;
}
