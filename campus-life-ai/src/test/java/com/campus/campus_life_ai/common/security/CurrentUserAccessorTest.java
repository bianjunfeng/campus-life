package com.campus.campus_life_ai.common.security;

import com.campus.campus_life_ai.common.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CurrentUserAccessorTest {

    private JwtProperties jwtProperties;

    @Mock
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("campus-life-secret-key-2025-please-change-in-production");
    }

    @Test
    void requireUserIdShouldRejectSignedJwtWhenRedisAccessKeyMissing() {
        String token = createAccessToken(42L);
        CurrentUserAccessor accessor = accessorWithToken(token);

        given(redisTemplate.hasKey("blacklist:" + token)).willReturn(false);
        given(redisTemplate.hasKey("token:42:" + token)).willReturn(false);

        assertThrows(UnauthorizedException.class, accessor::requireUserId);
    }

    @Test
    void requireUserIdShouldRejectBlacklistedAccessToken() {
        String token = createAccessToken(42L);
        CurrentUserAccessor accessor = accessorWithToken(token);

        given(redisTemplate.hasKey("blacklist:" + token)).willReturn(true);

        assertThrows(UnauthorizedException.class, accessor::requireUserId);
    }

    @Test
    void requireUserIdShouldReturnUserIdWhenTokenKeyExistsAndTokenIsNotBlacklisted() {
        String token = createAccessToken(42L);
        CurrentUserAccessor accessor = accessorWithToken(token);

        given(redisTemplate.hasKey("blacklist:" + token)).willReturn(false);
        given(redisTemplate.hasKey("token:42:" + token)).willReturn(true);

        assertEquals(42L, accessor.requireUserId());
    }

    private CurrentUserAccessor accessorWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return new CurrentUserAccessor(request, jwtProperties, redisTemplate);
    }

    private String createAccessToken(Long userId) {
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", "USER")
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }
}
