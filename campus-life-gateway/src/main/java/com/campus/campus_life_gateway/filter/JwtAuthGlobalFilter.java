package com.campus.campus_life_gateway.filter;

import com.campus.campus_life_gateway.auth.GatewayTokenStateValidator;
import com.campus.campus_life_gateway.config.GatewayAuthProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String GRAY_PREFIX = "/__gray";
    private static final String ADMIN_AI_PREFIX = "/api/admin/ai";
    private static final String MERCHANT_AI_PREFIX = "/api/merchant/ai";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String TOKEN_TYPE_HEADER = "X-Token-Type";
    private static final String SESSION_ID_HEADER = "X-Session-Id";

    private final GatewayAuthProperties authProperties;
    private final GatewayTokenStateValidator tokenStateValidator;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public JwtAuthGlobalFilter(GatewayAuthProperties authProperties,
                               GatewayTokenStateValidator tokenStateValidator,
                               ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.tokenStateValidator = tokenStateValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!authProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        String path = normalizePath(request.getPath().value());

        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }
        if (!requiresAuth(path, method)) {
            return chain.filter(exchange);
        }

        String authorization = request.getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange, "未登录");
        }
        String token = authorization.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "未登录");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Object userIdObj = claims.get("userId");
            String tokenType = String.valueOf(claims.get("type"));
            if (userIdObj == null || !"access".equals(tokenType)) {
                return unauthorized(exchange, "令牌无效");
            }

            String userId = String.valueOf(userIdObj).trim();
            if (!StringUtils.hasText(userId)) {
                return unauthorized(exchange, "令牌无效");
            }

            String role = normalizeRole(claims.get("role"));
            String sessionId = claimAsText(claims.get("sessionId"));

            return tokenStateValidator.isAccessTokenActive(userId, token)
                    .flatMap(active -> {
                        if (!Boolean.TRUE.equals(active)) {
                            return unauthorized(exchange, "令牌无效");
                        }
                        if (requiresAdmin(path) && StringUtils.hasText(role) && !"ADMIN".equals(role)) {
                            return forbidden(exchange, "无管理员权限");
                        }
                        if (requiresMerchant(path) && (!StringUtils.hasText(role) || (!"MERCHANT".equals(role) && !"ADMIN".equals(role)))) {
                            return forbidden(exchange, "无商家权限");
                        }

                        ServerHttpRequest mutated = withTrustedIdentityHeaders(request, userId, tokenType, role, sessionId);
                        return chain.filter(exchange.mutate().request(mutated).build());
                    });
        } catch (Exception e) {
            return unauthorized(exchange, "令牌无效或已过期");
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean requiresAuth(String path, HttpMethod method) {
        if (matchesAny(path, authProperties.getPublicPaths())) {
            return false;
        }
        if (matchesAny(path, authProperties.getProtectedPaths())) {
            return true;
        }
        if (isWriteMethod(method) && matchesAny(path, authProperties.getProtectedWritePaths())) {
            return true;
        }
        if (isReadMethod(method) && matchesAny(path, authProperties.getPublicGetPaths())) {
            return false;
        }
        if (startsWithAny(path, authProperties.getProtectedPrefixes())) {
            return true;
        }
        return false;
    }

    private String normalizePath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return rawPath;
        }
        if (rawPath.startsWith(GRAY_PREFIX + "/")) {
            return rawPath.substring(GRAY_PREFIX.length());
        }
        if (rawPath.equals(GRAY_PREFIX)) {
            return "/";
        }
        return rawPath;
    }

    private boolean matchesAny(String path, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (StringUtils.hasText(pattern) && antPathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWithAny(String path, List<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (StringUtils.hasText(prefix) && path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReadMethod(HttpMethod method) {
        return HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method);
    }

    private boolean isWriteMethod(HttpMethod method) {
        return method != null && !isReadMethod(method);
    }

    private boolean requiresAdmin(String path) {
        return StringUtils.hasText(path) && path.startsWith(ADMIN_AI_PREFIX);
    }

    private boolean requiresMerchant(String path) {
        return StringUtils.hasText(path) && path.startsWith(MERCHANT_AI_PREFIX);
    }

    private String normalizeRole(Object roleClaim) {
        if (roleClaim == null) {
            return null;
        }
        String role = String.valueOf(roleClaim).trim();
        if (role.startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }
        return StringUtils.hasText(role) ? role : null;
    }

    private String claimAsText(Object claim) {
        if (claim == null) {
            return null;
        }
        String value = String.valueOf(claim).trim();
        return StringUtils.hasText(value) ? value : null;
    }

    private ServerHttpRequest withTrustedIdentityHeaders(ServerHttpRequest request,
                                                        String userId,
                                                        String tokenType,
                                                        String role,
                                                        String sessionId) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                    headers.remove(TOKEN_TYPE_HEADER);
                    headers.remove(SESSION_ID_HEADER);
                    headers.set(USER_ID_HEADER, userId);
                    headers.set(TOKEN_TYPE_HEADER, tokenType);
                    if (StringUtils.hasText(role)) {
                        headers.set(USER_ROLE_HEADER, role);
                    }
                    if (StringUtils.hasText(sessionId)) {
                        headers.set(SESSION_ID_HEADER, sessionId);
                    }
                })
                .build();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = toJsonBytes(Map.of("code", 401, "message", message));
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = toJsonBytes(Map.of("code", 403, "message", message));
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private byte[] toJsonBytes(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            return "{\"code\":401,\"message\":\"未登录\"}".getBytes(StandardCharsets.UTF_8);
        }
    }
}
