package com.campus.campus_life_ai.common.security;

import com.campus.campus_life_ai.common.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class CurrentUserAccessor {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserAccessor.class);

    private static final String CLAIMS_ATTRIBUTE = CurrentUserAccessor.class.getName() + ".CLAIMS";
    private static final Object INVALID_CLAIMS = new Object();
    private static final String TOKEN_PREFIX = "token:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final HttpServletRequest request;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    public CurrentUserAccessor(HttpServletRequest request, JwtProperties jwtProperties, StringRedisTemplate redisTemplate) {
        this.request = request;
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
    }

    public Long requireUserId() {
        Claims claims = requireAccessClaims();
        Long userId = extractUserId(claims);
        if (userId == null) {
            throw new UnauthorizedException("未登录或缺少用户信息");
        }
        return userId;
    }

    public String getCurrentRole() {
        Claims claims = resolveAccessClaims();
        if (claims == null) {
            return null;
        }
        return normalizeRole(claims.get("role"));
    }

    public String requireAuthorizationHeader() {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("未登录或缺少访问令牌");
        }
        String token = authorization.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("未登录或缺少访问令牌");
        }
        return authorization;
    }

    private Claims requireAccessClaims() {
        Claims claims = resolveAccessClaims();
        if (claims == null) {
            throw new UnauthorizedException("未登录或访问令牌无效");
        }
        return claims;
    }

    private Claims resolveAccessClaims() {
        Object cached = request.getAttribute(CLAIMS_ATTRIBUTE);
        if (cached instanceof Claims claims) {
            return claims;
        }
        if (cached == INVALID_CLAIMS) {
            return null;
        }

        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            request.setAttribute(CLAIMS_ATTRIBUTE, INVALID_CLAIMS);
            return null;
        }
        String token = authorization.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            request.setAttribute(CLAIMS_ATTRIBUTE, INVALID_CLAIMS);
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = extractUserId(claims);
            if (!"access".equals(String.valueOf(claims.get("type"))) || userId == null) {
                request.setAttribute(CLAIMS_ATTRIBUTE, INVALID_CLAIMS);
                return null;
            }
            if (!isAccessTokenActive(userId, token)) {
                request.setAttribute(CLAIMS_ATTRIBUTE, INVALID_CLAIMS);
                return null;
            }
            request.setAttribute(CLAIMS_ATTRIBUTE, claims);
            return claims;
        } catch (Exception e) {
            request.setAttribute(CLAIMS_ATTRIBUTE, INVALID_CLAIMS);
            return null;
        }
    }

    private boolean isAccessTokenActive(Long userId, String token) {
        if (redisTemplate == null) {
            log.error("Redis 未配置，拒绝 AI 访问令牌（fail-closed）: userId={}", userId);
            return false;
        }
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token))) {
                return false;
            }
            return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_PREFIX + userId + ":" + token));
        } catch (Exception e) {
            log.error("Redis 会话校验失败，拒绝 AI 访问令牌（fail-closed）: userId={}", userId, e);
            return false;
        }
    }

    private Long extractUserId(Claims claims) {
        Object userId = claims == null ? null : claims.get("userId");
        if (userId == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(userId));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeRole(Object role) {
        if (role == null) {
            return null;
        }
        String normalized = String.valueOf(role).trim();
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
