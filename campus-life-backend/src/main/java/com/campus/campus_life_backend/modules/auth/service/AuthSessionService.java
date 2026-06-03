package com.campus.campus_life_backend.modules.auth.service;

import com.campus.campus_life_backend.common.util.JwtUtil;
import com.campus.campus_life_backend.modules.auth.entity.AuthSession;
import com.campus.campus_life_backend.modules.auth.mapper.AuthSessionMapper;
import com.campus.campus_life_backend.modules.user.entity.User;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class AuthSessionService {

    public static final int STATUS_LOGGED_OUT = 0;
    public static final int STATUS_ONLINE = 1;
    public static final int STATUS_FORCE_LOGOUT = 2;
    public static final int STATUS_EXPIRED = 3;

    private final AuthSessionMapper authSessionMapper;
    private final JwtUtil jwtUtil;

    public AuthSessionService(AuthSessionMapper authSessionMapper, JwtUtil jwtUtil) {
        this.authSessionMapper = authSessionMapper;
        this.jwtUtil = jwtUtil;
    }

    public void createOrUpdateSession(User user,
                                      String sessionId,
                                      String accessToken,
                                      String refreshToken,
                                      String device,
                                      String ip,
                                      String userAgent) {
        if (user == null || user.getId() == null || isBlank(sessionId) || isBlank(accessToken)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        AuthSession session = new AuthSession();
        session.setSessionId(sessionId);
        session.setUserId(user.getId());
        session.setAccessJti(jwtUtil.getJwtIdFromToken(accessToken));
        session.setRefreshJti(jwtUtil.getJwtIdFromToken(refreshToken));
        session.setAccessTokenHash(hashToken(accessToken));
        session.setRefreshTokenHash(hashToken(refreshToken));
        session.setDevice(trimToMax(device, 255));
        session.setIp(trimToMax(ip, 64));
        session.setUserAgent(trimToMax(userAgent, 512));
        session.setStatus(STATUS_ONLINE);
        session.setLoginTime(now);
        session.setLastSeenTime(now);
        session.setExpireAt(resolveAccessExpireAt(accessToken));
        authSessionMapper.upsert(session);
    }

    public void updateTokenPair(String sessionId, String accessToken, String refreshToken) {
        if (isBlank(sessionId) || isBlank(accessToken)) {
            return;
        }
        authSessionMapper.updateTokenPairBySessionId(
                sessionId,
                jwtUtil.getJwtIdFromToken(accessToken),
                jwtUtil.getJwtIdFromToken(refreshToken),
                hashToken(accessToken),
                hashToken(refreshToken),
                resolveAccessExpireAt(accessToken)
        );
    }

    public void markLoggedOutByAccessToken(String accessToken) {
        updateStatusByAccessToken(accessToken, STATUS_LOGGED_OUT);
    }

    public void markLoggedOutBySessionId(String sessionId) {
        updateStatusBySessionId(sessionId, STATUS_LOGGED_OUT);
    }

    public void markForceLogoutByUserId(Long userId) {
        updateUserActiveSessionsStatus(userId, STATUS_FORCE_LOGOUT);
    }

    public void updateStatusByAccessToken(String accessToken, int status) {
        if (isBlank(accessToken)) {
            return;
        }
        authSessionMapper.updateStatusByAccessTokenHash(hashToken(accessToken), status);
    }

    public void updateStatusBySessionId(String sessionId, int status) {
        if (isBlank(sessionId)) {
            return;
        }
        authSessionMapper.updateStatusBySessionId(sessionId, status);
    }

    public void updateUserActiveSessionsStatus(Long userId, int status) {
        if (userId == null) {
            return;
        }
        authSessionMapper.updateActiveStatusByUserId(userId, status);
    }

    public void touchLastSeen(String sessionId) {
        if (isBlank(sessionId)) {
            return;
        }
        authSessionMapper.updateLastSeenBySessionId(sessionId, LocalDateTime.now());
    }

    public List<Map<String, Object>> findUserSessions(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<Map<String, Object>> sessions = authSessionMapper.findUserSessions(userId);
        return sessions == null ? List.of() : sessions;
    }

    public boolean sessionBelongsToUser(String sessionId, Long userId) {
        if (isBlank(sessionId) || userId == null) {
            return false;
        }
        return authSessionMapper.countBySessionIdAndUserId(sessionId, userId) > 0;
    }

    public Map<String, Object> getOnlineUsers(Integer page, Integer size) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int offset = (p - 1) * s;
        List<Map<String, Object>> list = authSessionMapper.findOnlineUsers(offset, s);
        long total = authSessionMapper.countOnlineUsers();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list == null ? List.of() : list);
        result.put("page", p);
        result.put("size", s);
        result.put("total", total);
        result.put("totalOnline", total);
        return result;
    }

    public String hashToken(String token) {
        if (token == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法计算 token hash", e);
        }
    }

    private LocalDateTime resolveAccessExpireAt(String accessToken) {
        var expiration = jwtUtil.getExpirationDateFromToken(accessToken);
        if (expiration == null) {
            return LocalDateTime.now().plusMinutes(30);
        }
        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? 20 : size;
    }

    private String trimToMax(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
