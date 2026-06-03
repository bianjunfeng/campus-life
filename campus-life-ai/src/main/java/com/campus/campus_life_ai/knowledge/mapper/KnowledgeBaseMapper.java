package com.campus.campus_life_ai.knowledge.mapper;

import com.campus.campus_life_ai.knowledge.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeBaseMapper {

    int insert(KnowledgeBase knowledgeBase);

    KnowledgeBase findById(@Param("id") Long id);

    KnowledgeBase findOwnedUserBase(@Param("id") Long id, @Param("userId") Long userId);

    List<KnowledgeBase> findUserBases(@Param("userId") Long userId);

    int countUserBases(@Param("userId") Long userId);

    Long sumUserStorage(@Param("userId") Long userId);

    int updateOwnedUserBase(KnowledgeBase knowledgeBase);

    int markDeletedUserBase(@Param("id") Long id, @Param("userId") Long userId);

    int refreshStats(@Param("id") Long id);
}
