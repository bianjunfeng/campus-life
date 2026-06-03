package com.campus.campus_life_ai.knowledge.mapper;

import com.campus.campus_life_ai.knowledge.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeDocumentMapper {

    int insert(KnowledgeDocument document);

    KnowledgeDocument findById(@Param("id") Long id);

    KnowledgeDocument findActiveByIdAndKbId(@Param("id") Long id, @Param("kbId") Long kbId);

    List<KnowledgeDocument> findByKbId(@Param("kbId") Long kbId);

    int updateParseState(KnowledgeDocument document);

    int markDeleted(@Param("id") Long id, @Param("kbId") Long kbId);
}
