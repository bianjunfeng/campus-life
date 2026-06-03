package com.campus.campus_life_backend.modules.auth.mapper;

import com.campus.campus_life_backend.modules.auth.entity.AuthSession;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AuthSessionMapper {

    int upsert(AuthSession session);

    int updateTokenPairBySessionId(
            @Param("sessionId") String sessionId,
            @Param("accessJti") String accessJti,
            @Param("refreshJti") String refreshJti,
            @Param("accessTokenHash") String accessTokenHash,
            @Param("refreshTokenHash") String refreshTokenHash,
            @Param("expireAt") LocalDateTime expireAt
    );

    int updateStatusByAccessTokenHash(@Param("accessTokenHash") String accessTokenHash, @Param("status") Integer status);

    int updateStatusBySessionId(@Param("sessionId") String sessionId, @Param("status") Integer status);

    int updateActiveStatusByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    int updateLastSeenBySessionId(@Param("sessionId") String sessionId, @Param("lastSeenTime") LocalDateTime lastSeenTime);

    List<Map<String, Object>> findOnlineUsers(@Param("offset") Integer offset, @Param("limit") Integer limit);

    long countOnlineUsers();

    List<Map<String, Object>> findUserSessions(@Param("userId") Long userId);

    int countBySessionIdAndUserId(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}
