package com.campus.campus_life_ai.knowledge.vector;

import java.util.List;

public interface KnowledgeVectorStore {

    void upsert(List<KnowledgeVectorRecord> records);

    void deleteByDocumentId(Long documentId);

    List<KnowledgeVectorSearchHit> search(List<Float> queryVector,
                                          Long userId,
                                          List<Long> knowledgeBaseIds,
                                          boolean includePersonal,
                                          boolean includePlatform,
                                          int topK);
}
