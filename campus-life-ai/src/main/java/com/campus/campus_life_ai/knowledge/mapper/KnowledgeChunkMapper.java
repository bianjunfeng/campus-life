package com.campus.campus_life_ai.knowledge.mapper;

import com.campus.campus_life_ai.knowledge.entity.KnowledgeChunk;
import com.campus.campus_life_ai.knowledge.service.KnowledgeSearchHit;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeChunkMapper {

    int insert(KnowledgeChunk chunk);

    int deleteByDocumentId(@Param("documentId") Long documentId);

    List<KnowledgeSearchHit> findAccessibleChunksByVectorIds(@Param("userId") Long userId,
                                                             @Param("vectorIds") List<String> vectorIds,
                                                             @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
                                                             @Param("includePersonal") boolean includePersonal,
                                                             @Param("includePlatform") boolean includePlatform);

    List<KnowledgeSearchHit> searchAccessibleChunks(@Param("userId") Long userId,
                                                    @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
                                                    @Param("includePersonal") boolean includePersonal,
                                                    @Param("includePlatform") boolean includePlatform,
                                                    @Param("keywords") List<String> keywords,
                                                    @Param("limit") int limit);
}
