package com.campus.campus_life_ai.knowledge.vector;

import java.util.List;

public interface KnowledgeEmbeddingClient {

    List<List<Float>> embed(List<String> texts);

    default List<Float> embedOne(String text) {
        List<List<Float>> vectors = embed(List.of(text));
        if (vectors.isEmpty()) {
            throw new IllegalStateException("向量化服务未返回结果");
        }
        return vectors.get(0);
    }
}
