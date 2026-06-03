package com.campus.campus_life_backend.common.util;

import com.campus.campus_life_backend.common.security.model.RoleCode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    @Test
    void shouldGenerateDifferentAccessTokensWithinSameSecond() {
        JwtUtil jwtUtil = new JwtUtil();
        setField(jwtUtil, "secret", "campus-life-secret-key-2025-please-change-in-production");
        setField(jwtUtil, "expiration", 1800000L);
        setField(jwtUtil, "refreshExpiration", 2592000000L);

        String firstToken = jwtUtil.generateAccessToken(2L);
        String secondToken = jwtUtil.generateAccessToken(2L);

        assertNotEquals(firstToken, secondToken);
        assertEquals(2L, jwtUtil.getUserIdFromToken(firstToken));
        assertEquals("access", jwtUtil.getTokenType(firstToken));
        assertNotNull(jwtUtil.getJwtIdFromToken(firstToken));
    }

    @Test
    void shouldEmbedRoleClaimWhenProvided() {
        JwtUtil jwtUtil = new JwtUtil();
        setField(jwtUtil, "secret", "campus-life-secret-key-2025-please-change-in-production");
        setField(jwtUtil, "expiration", 1800000L);
        setField(jwtUtil, "refreshExpiration", 2592000000L);

        String token = jwtUtil.generateAccessToken(2L, RoleCode.ADMIN);
        Claims claims = jwtUtil.getClaimsFromToken(token);

        assertEquals("ADMIN", String.valueOf(claims.get("role")));
    }

    @Test
    void shouldGenerateDifferentRefreshTokensWithinSameSecond() {
        JwtUtil jwtUtil = new JwtUtil();
        setField(jwtUtil, "secret", "campus-life-secret-key-2025-please-change-in-production");
        setField(jwtUtil, "expiration", 1800000L);
        setField(jwtUtil, "refreshExpiration", 2592000000L);

        String firstToken = jwtUtil.generateRefreshToken(2L);
        String secondToken = jwtUtil.generateRefreshToken(2L);

        assertNotEquals(firstToken, secondToken);
        assertEquals(2L, jwtUtil.getUserIdFromToken(firstToken));
        assertEquals("refresh", jwtUtil.getTokenType(firstToken));
        assertNotNull(jwtUtil.getJwtIdFromToken(firstToken));
    }

    @Test
    void shouldUseThirtyMinuteAccessTokenExpiration() {
        JwtUtil jwtUtil = new JwtUtil();
        setField(jwtUtil, "secret", "campus-life-secret-key-2025-please-change-in-production");
        setField(jwtUtil, "expiration", 1800000L);
        setField(jwtUtil, "refreshExpiration", 2592000000L);

        long before = System.currentTimeMillis();
        String token = jwtUtil.generateAccessToken(2L);
        long remainingMillis = jwtUtil.getExpirationDateFromToken(token).getTime() - before;

        assertTrue(remainingMillis <= 1800000L + 1000L);
        assertTrue(remainingMillis > 1700000L);
    }

    @Test
    void accessAndRefreshTokensShouldHaveDifferentJwtIds() {
        JwtUtil jwtUtil = new JwtUtil();
        setField(jwtUtil, "secret", "campus-life-secret-key-2025-please-change-in-production");
        setField(jwtUtil, "expiration", 1800000L);
        setField(jwtUtil, "refreshExpiration", 2592000000L);

        String accessToken = jwtUtil.generateAccessToken(2L);
        String refreshToken = jwtUtil.generateRefreshToken(2L);

        assertNotEquals(jwtUtil.getJwtIdFromToken(accessToken), jwtUtil.getJwtIdFromToken(refreshToken));
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to set field: " + fieldName, e);
        }
    }
}
