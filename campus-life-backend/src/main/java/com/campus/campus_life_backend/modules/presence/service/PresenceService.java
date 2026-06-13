package com.campus.campus_life_backend.modules.presence.service;

import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class PresenceService {

    public static final long ONLINE_TTL_SECONDS = 90L;

    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);
    private static final String SESSION_PREFIX = "presence:session:";
    private static final String USER_PREFIX = "presence:user:";
    private static final String USER_SESSIONS_SUFFIX = ":sessions";
    private static final String ONLINE_SESSIONS_KEY = "presence:online:sessions";
    private static final String ONLINE_USERS_KEY = "presence:online:users";

    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;

    public PresenceService(StringRedisTemplate redisTemplate, UserMapper userMapper) {
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
    }

    public void markOnline(LoginPrincipal principal, String sessionId, String portal, String ip, String userAgent) {
        if (principal == null) {
            return;
        }
        markOnline(principal.getUserId(), sessionId,
                principal.getRoleCode() == null ? null : principal.getRoleCode().name(),
                portal, ip, userAgent);
    }

    public void markOnline(Long userId, String sessionId, String role, String portal, String ip, String userAgent) {
        if (userId == null || !StringUtils.hasText(sessionId)) {
            return;
        }
        try {
            long nowMillis = System.currentTimeMillis();
            String nowText = LocalDateTime.now().toString();
            String sessionKey = sessionKey(sessionId);
            Map<Object, Object> existing = redisTemplate.opsForHash().entries(sessionKey);
            User user = userMapper.findById(userId);

            Map<String, String> data = new HashMap<>();
            data.put("sessionId", sessionId);
            data.put("userId", String.valueOf(userId));
            data.put("username", user == null ? "" : nullToEmpty(user.getUsername()));
            data.put("nickname", user == null ? "" : nullToEmpty(user.getUsername()));
            data.put("phone", user == null ? "" : nullToEmpty(user.getPhone()));
            data.put("email", user == null ? "" : nullToEmpty(user.getEmail()));
            data.put("role", StringUtils.hasText(role) ? role : resolveRoleName(user));
            data.put("portal", firstText(portal, existing.get("portal")));
            data.put("ip", firstText(ip, existing.get("ip")));
            data.put("loginIp", firstText(ip, existing.get("loginIp"), existing.get("ip")));
            data.put("userAgent", firstText(userAgent, existing.get("userAgent")));
            data.put("loginTime", firstText(existing.get("loginTime"), nowText));
            data.put("lastSeenTime", nowText);
            data.put("lastActiveTime", nowText);
            data.put("lastSeenMillis", String.valueOf(nowMillis));

            redisTemplate.opsForHash().putAll(sessionKey, data);
            redisTemplate.expire(sessionKey, Duration.ofSeconds(ONLINE_TTL_SECONDS));

            String userSessionsKey = userSessionsKey(userId);
            redisTemplate.opsForSet().add(userSessionsKey, sessionId);
            redisTemplate.expire(userSessionsKey, ONLINE_TTL_SECONDS, TimeUnit.SECONDS);

            redisTemplate.opsForZSet().add(ONLINE_SESSIONS_KEY, sessionId, nowMillis);
            redisTemplate.opsForZSet().add(ONLINE_USERS_KEY, String.valueOf(userId), nowMillis);
            cleanupExpired(nowMillis);
        } catch (Exception ex) {
            logger.warn("刷新在线状态失败，userId={}, sessionId={}", userId, sessionId, ex);
        }
    }

    public void markOffline(Long userId, String sessionId) {
        if (userId == null || !StringUtils.hasText(sessionId)) {
            return;
        }
        try {
            removeSession(userId, sessionId);
        } catch (Exception ex) {
            logger.warn("清理在线会话失败，userId={}, sessionId={}", userId, sessionId, ex);
        }
    }

    public void markUserOffline(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey(userId));
            if (sessionIds != null) {
                for (String sessionId : sessionIds) {
                    removeSession(userId, sessionId);
                }
            }
            redisTemplate.delete(userSessionsKey(userId));
            redisTemplate.opsForZSet().remove(ONLINE_USERS_KEY, String.valueOf(userId));
        } catch (Exception ex) {
            logger.warn("清理用户在线状态失败，userId={}", userId, ex);
        }
    }

    public Map<String, Object> getOnlineUsers(Integer page,
                                              Integer size,
                                              String username,
                                              String ip) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        long nowMillis = System.currentTimeMillis();
        cleanupExpired(nowMillis);

        List<Map<String, Object>> filtered = new ArrayList<>();
        Set<String> distinctUserIds = new HashSet<>();
        Set<String> sessionIds = redisTemplate.opsForZSet()
                .reverseRangeByScore(ONLINE_SESSIONS_KEY, onlineCutoff(nowMillis), nowMillis);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                Map<String, Object> item = readActiveSession(sessionId, nowMillis);
                if (item == null) {
                    continue;
                }
                if (!matches(item, username, ip)) {
                    continue;
                }
                filtered.add(item);
                Object userId = item.get("userId");
                if (userId != null) {
                    distinctUserIds.add(String.valueOf(userId));
                }
            }
        }

        int total = filtered.size();
        int from = Math.min((p - 1) * s, total);
        int to = Math.min(from + s, total);

        Map<String, Object> result = new HashMap<>();
        result.put("list", filtered.subList(from, to));
        result.put("page", p);
        result.put("size", s);
        result.put("total", total);
        result.put("totalOnline", total);
        result.put("totalOnlineSessions", total);
        result.put("totalOnlineUsers", distinctUserIds.size());
        return result;
    }

    private void removeSession(Long userId, String sessionId) {
        redisTemplate.delete(sessionKey(sessionId));
        redisTemplate.opsForZSet().remove(ONLINE_SESSIONS_KEY, sessionId);
        redisTemplate.opsForSet().remove(userSessionsKey(userId), sessionId);
        if (!hasActiveSession(userId)) {
            redisTemplate.delete(userSessionsKey(userId));
            redisTemplate.opsForZSet().remove(ONLINE_USERS_KEY, String.valueOf(userId));
        }
    }

    private boolean hasActiveSession(Long userId) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey(userId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return false;
        }
        boolean active = false;
        for (String sessionId : sessionIds) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(sessionId)))) {
                active = true;
            } else {
                redisTemplate.opsForSet().remove(userSessionsKey(userId), sessionId);
                redisTemplate.opsForZSet().remove(ONLINE_SESSIONS_KEY, sessionId);
            }
        }
        return active;
    }

    private Map<String, Object> readActiveSession(String sessionId, long nowMillis) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(sessionKey(sessionId));
        if (raw == null || raw.isEmpty()) {
            redisTemplate.opsForZSet().remove(ONLINE_SESSIONS_KEY, sessionId);
            return null;
        }
        String userIdText = asText(raw.get("userId"));
        if (!StringUtils.hasText(userIdText)) {
            redisTemplate.delete(sessionKey(sessionId));
            redisTemplate.opsForZSet().remove(ONLINE_SESSIONS_KEY, sessionId);
            return null;
        }
        String lastSeenMillisText = asText(raw.get("lastSeenMillis"));
        if (StringUtils.hasText(lastSeenMillisText)) {
            try {
                if (Long.parseLong(lastSeenMillisText) < onlineCutoff(nowMillis)) {
                    removeSession(Long.valueOf(userIdText), sessionId);
                    return null;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        Map<String, Object> item = new HashMap<>();
        item.put("id", asText(raw.get("id")));
        item.put("sessionId", sessionId);
        item.put("userId", parseLong(userIdText));
        item.put("username", asText(raw.get("username")));
        item.put("nickname", asText(raw.get("nickname")));
        item.put("phone", asText(raw.get("phone")));
        item.put("email", asText(raw.get("email")));
        item.put("role", asText(raw.get("role")));
        item.put("portal", asText(raw.get("portal")));
        item.put("ip", asText(raw.get("ip")));
        item.put("loginIp", firstText(raw.get("loginIp"), raw.get("ip")));
        item.put("loginLocation", "");
        item.put("userAgent", asText(raw.get("userAgent")));
        item.put("loginTime", asText(raw.get("loginTime")));
        item.put("lastSeenTime", asText(raw.get("lastSeenTime")));
        item.put("lastActiveTime", firstText(raw.get("lastActiveTime"), raw.get("lastSeenTime")));
        item.put("status", 1);
        return item;
    }

    private boolean matches(Map<String, Object> item, String username, String ip) {
        if (StringUtils.hasText(username)) {
            String needle = username.trim().toLowerCase();
            String name = nullToEmpty(asText(item.get("username"))).toLowerCase();
            String nick = nullToEmpty(asText(item.get("nickname"))).toLowerCase();
            if (!name.contains(needle) && !nick.contains(needle)) {
                return false;
            }
        }
        if (StringUtils.hasText(ip)) {
            String actualIp = nullToEmpty(asText(item.get("ip")));
            String loginIp = nullToEmpty(asText(item.get("loginIp")));
            String needle = ip.trim();
            return actualIp.contains(needle) || loginIp.contains(needle);
        }
        return true;
    }

    private void cleanupExpired(long nowMillis) {
        double cutoff = onlineCutoff(nowMillis);
        redisTemplate.opsForZSet().removeRangeByScore(ONLINE_SESSIONS_KEY, 0, cutoff - 1);
        redisTemplate.opsForZSet().removeRangeByScore(ONLINE_USERS_KEY, 0, cutoff - 1);
    }

    private double onlineCutoff(long nowMillis) {
        return nowMillis - ONLINE_TTL_SECONDS * 1000;
    }

    private String resolveRoleName(User user) {
        if (user == null) {
            return "";
        }
        return RoleCode.fromDbRole(user.getRole()).name();
    }

    private String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    private String userSessionsKey(Long userId) {
        return USER_PREFIX + userId + USER_SESSIONS_SUFFIX;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, 200);
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            String text = asText(value);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return "";
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullToEmpty(String value) {
        return Objects.toString(value, "");
    }
}
