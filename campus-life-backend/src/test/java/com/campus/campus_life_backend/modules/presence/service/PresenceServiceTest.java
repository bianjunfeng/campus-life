package com.campus.campus_life_backend.modules.presence.service;

import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private UserMapper userMapper;

    private PresenceService presenceService;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        presenceService = new PresenceService(redisTemplate, userMapper);
    }

    @Test
    void markOnlineShouldWriteSessionHashAndIndexes() {
        User user = new User();
        user.setId(1L);
        user.setUsername("wangxiaohu");
        user.setPhone("13800138000");
        user.setRole(0);
        given(hashOperations.entries("presence:session:session-1")).willReturn(Map.of());
        given(userMapper.findById(1L)).willReturn(user);

        presenceService.markOnline(1L, "session-1", "STUDENT", "consumer", "127.0.0.1", "JUnit");

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq("presence:session:session-1"), dataCaptor.capture());
        Map<String, String> data = dataCaptor.getValue();
        assertEquals("session-1", data.get("sessionId"));
        assertEquals("1", data.get("userId"));
        assertEquals("wangxiaohu", data.get("username"));
        assertEquals("consumer", data.get("portal"));
        assertEquals("127.0.0.1", data.get("ip"));
        assertFalse(data.get("lastSeenMillis").isBlank());

        verify(redisTemplate).expire("presence:session:session-1", Duration.ofSeconds(PresenceService.ONLINE_TTL_SECONDS));
        verify(setOperations).add("presence:user:1:sessions", "session-1");
        verify(redisTemplate).expire("presence:user:1:sessions", PresenceService.ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
        verify(zSetOperations).add(eq("presence:online:sessions"), eq("session-1"), anyDouble());
        verify(zSetOperations).add(eq("presence:online:users"), eq("1"), anyDouble());
    }

    @Test
    void getOnlineUsersShouldReturnFilteredActiveSessions() {
        long now = System.currentTimeMillis();
        given(zSetOperations.reverseRangeByScore(eq("presence:online:sessions"), anyDouble(), anyDouble()))
                .willReturn(new LinkedHashSet<>(List.of("session-1", "session-2")));
        given(hashOperations.entries("presence:session:session-1")).willReturn(Map.of(
                "sessionId", "session-1",
                "userId", "1",
                "username", "wangxiaohu",
                "nickname", "wangxiaohu",
                "portal", "consumer",
                "ip", "127.0.0.1",
                "loginIp", "127.0.0.1",
                "lastSeenMillis", String.valueOf(now),
                "lastSeenTime", "2026-06-08T12:00:00",
                "loginTime", "2026-06-08T11:59:00"
        ));
        given(hashOperations.entries("presence:session:session-2")).willReturn(Map.of(
                "sessionId", "session-2",
                "userId", "2",
                "username", "other",
                "nickname", "other",
                "portal", "admin",
                "ip", "10.0.0.1",
                "loginIp", "10.0.0.1",
                "lastSeenMillis", String.valueOf(now),
                "lastSeenTime", "2026-06-08T12:00:00",
                "loginTime", "2026-06-08T11:59:00"
        ));

        Map<String, Object> result = presenceService.getOnlineUsers(1, 20, "wang", "127.0.0.1");

        assertEquals(1, result.get("total"));
        assertEquals(1, result.get("totalOnlineUsers"));
        assertEquals(1, result.get("totalOnlineSessions"));
        List<?> list = (List<?>) result.get("list");
        assertEquals(1, list.size());
        Map<?, ?> first = (Map<?, ?>) list.get(0);
        assertEquals("session-1", first.get("sessionId"));
        assertEquals(1L, first.get("userId"));
    }
}
