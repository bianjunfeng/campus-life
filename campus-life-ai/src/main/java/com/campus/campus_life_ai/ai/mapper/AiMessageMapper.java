package com.campus.campus_life_ai.ai.mapper;

import com.campus.campus_life_ai.ai.entity.AiMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiMessageMapper {

    int insert(AiMessage message);

    List<AiMessage> findBySessionId(@Param("sessionId") String sessionId,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);

    List<AiMessage> findRecentContext(@Param("sessionId") String sessionId, @Param("limit") Integer limit);

    int countBySessionId(@Param("sessionId") String sessionId);
}
