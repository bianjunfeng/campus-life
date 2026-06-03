package com.campus.campus_life_backend.common.util;

import com.campus.campus_life_backend.common.security.model.RoleCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具类
 * 用于生成和解析 JWT Token
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:campus-life-secret-key-2025-please-change-in-production}")
    private String secret;

    @Value("${jwt.expiration:1800000}") // 默认30分钟（毫秒）
    private Long expiration = 1800000L;

    @Value("${jwt.refresh-expiration:2592000000}") // 默认30天（毫秒）
    private Long refreshExpiration = 2592000000L;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成访问令牌（Access Token）
     * @param userId 用户ID
     * @return JWT Token
     */
    public String generateAccessToken(Long userId) {
        return generateAccessToken(userId, null);
    }

    public String generateAccessToken(Long userId, RoleCode roleCode) {
        return generateAccessToken(userId, roleCode, null);
    }

    public String generateAccessToken(Long userId, RoleCode roleCode, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "access");
        if (sessionId != null && !sessionId.isBlank()) {
            claims.put("sessionId", sessionId);
        }
        if (roleCode != null) {
            claims.put("role", roleCode.name());
        }
        return generateToken(claims, expiration);
    }

    /**
     * 生成刷新令牌（Refresh Token）
     * @param userId 用户ID
     * @return JWT Refresh Token
     */
    public String generateRefreshToken(Long userId) {
        return generateRefreshToken(userId, null);
    }

    public String generateRefreshToken(Long userId, RoleCode roleCode) {
        return generateRefreshToken(userId, roleCode, null);
    }

    public String generateRefreshToken(Long userId, RoleCode roleCode, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        if (sessionId != null && !sessionId.isBlank()) {
            claims.put("sessionId", sessionId);
        }
        if (roleCode != null) {
            claims.put("role", roleCode.name());
        }
        return generateToken(claims, refreshExpiration);
    }

    /**
     * 生成 Token
     */
    private String generateToken(Map<String, Object> claims, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            } else if (userIdObj instanceof String) {
                return Long.parseLong((String) userIdObj);
            }
        }
        return null;
    }

    /**
     * 从 Token 中获取 Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            // Token 无效或已过期
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return false;
            }
            // 检查是否过期（getClaimsFromToken 已经会检查过期）
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Token 的过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * 检查 Token 是否即将过期（在指定时间内）
     * @param token Token
     * @param minutes 分钟数
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token, int minutes) {
        Date expiration = getExpirationDateFromToken(token);
        if (expiration == null) {
            return true;
        }
        long timeUntilExpiry = expiration.getTime() - System.currentTimeMillis();
        return timeUntilExpiry < (minutes * 60 * 1000);
    }

    /**
     * 获取 Token 类型（access 或 refresh）
     */
    public String getTokenType(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object type = claims.get("type");
            return type != null ? type.toString() : null;
        }
        return null;
    }

    /**
     * 获取登录会话ID
     */
    public String getSessionIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object sessionId = claims.get("sessionId");
            return sessionId == null ? null : sessionId.toString();
        }
        return null;
    }

    /**
     * 获取 JWT ID（jti）
     */
    public String getJwtIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims == null ? null : claims.getId();
    }
}


