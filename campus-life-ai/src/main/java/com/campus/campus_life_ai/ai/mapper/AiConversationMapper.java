package com.campus.campus_life_ai.ai.mapper;

import com.campus.campus_life_ai.ai.entity.AiConversation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiConversationMapper {

    int insert(AiConversation conversation);

    AiConversation findBySessionId(@Param("sessionId") String sessionId);

    AiConversation findBySessionIdAndUserId(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    List<AiConversation> findByUserId(@Param("userId") Long userId);

    int updateTitle(@Param("sessionId") String sessionId, @Param("userId") Long userId, @Param("title") String title);

    int updateMessageState(AiConversation conversation);

    int markDeleted(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}
