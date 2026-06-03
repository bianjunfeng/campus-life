package com.campus.campus_life_gateway.filter;

import com.campus.campus_life_gateway.auth.GatewayTokenStateValidator;
import com.campus.campus_life_gateway.config.GatewayAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthGlobalFilterTest {

    @Test
    void shouldAllowPublicGetPathEvenWhenProtectedPrefixMatches() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api/vouchers"));
        properties.setPublicGetPaths(List.of("/api/vouchers/available"));

        FilterResult result = execute(properties, HttpMethod.GET, "/api/vouchers/available");

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
    }

    @Test
    void shouldBlockProtectedGetPathWhenProtectedPrefixMatches() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api/vouchers"));
        properties.setPublicGetPaths(List.of("/api/vouchers/available"));

        FilterResult result = execute(properties, HttpMethod.GET, "/api/vouchers/my-orders");

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void shouldBlockWriteRequestOnPublicReadableForumPath() {
        GatewayAuthProperties properties = buildProperties();
        properties.setPublicGetPaths(List.of("/api/forum/posts"));
        properties.setProtectedWritePaths(List.of("/api/forum/posts"));

        FilterResult result = execute(properties, HttpMethod.POST, "/api/forum/posts");

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void shouldAllowPaymentNotifyCallbackWithoutToken() {
        GatewayAuthProperties properties = buildProperties();
        properties.setPublicPaths(List.of("/api/payment/alipay/notify"));
        properties.setProtectedPrefixes(List.of("/api/payment"));

        FilterResult result = execute(properties, HttpMethod.POST, "/api/payment/alipay/notify");

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
    }

    @Test
    void shouldAllowVoucherServicePublicGetPathEvenWhenProtectedPrefixMatches() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api/voucher-service"));
        properties.setPublicGetPaths(List.of("/api/voucher-service/coupons/*"));

        FilterResult result = execute(properties, HttpMethod.GET, "/api/voucher-service/coupons/1");

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
    }

    @Test
    void shouldAllowPublicUserFollowersGetEvenWhenUsersPrefixIsProtected() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api/users"));
        properties.setPublicGetPaths(List.of("/api/users/*/followers"));

        FilterResult result = execute(properties, HttpMethod.GET, "/api/users/12/followers");

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
    }

    @Test
    void shouldAllowPublicUserProfileGetEvenWhenUsersPrefixIsProtected() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api/users"));
        properties.setPublicGetPaths(List.of("/api/users/*"));

        FilterResult result = execute(properties, HttpMethod.GET, "/api/users/12");

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
    }

    @Test
    void shouldBlockUserProfileWriteWhenUsersPrefixIsProtected() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api/users"));
        properties.setPublicGetPaths(List.of("/api/users/*/followers"));

        FilterResult result = execute(properties, HttpMethod.PUT, "/api/users/me/profile");

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void shouldAllowPublicShopNearbyGetEvenWhenShopsPrefixIsProtected() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api/shops"));
        properties.setPublicGetPaths(List.of("/api/shops/nearby"));

        FilterResult result = execute(properties, HttpMethod.GET, "/api/shops/nearby");

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
    }

    @Test
    void shouldBlockShopLocationWriteWhenShopsPrefixIsProtected() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api/shops"));
        properties.setPublicGetPaths(List.of("/api/shops/nearby"));

        FilterResult result = execute(properties, HttpMethod.POST, "/api/shops/10/location");

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void shouldAllowSearchGetEvenWhenSearchPrefixIsProtected() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api/search"));
        properties.setPublicGetPaths(List.of("/api/search/**"));

        FilterResult result = execute(properties, HttpMethod.GET, "/api/search/posts");

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
    }

    @Test
    void shouldAllowForumPostDetailGetEvenWhenForumPrefixIsProtected() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api/forum"));
        properties.setPublicGetPaths(List.of("/api/forum/posts/*"));

        FilterResult result = execute(properties, HttpMethod.GET, "/api/forum/posts/9");

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
    }

    @Test
    void shouldBlockForumWriteWhenForumPrefixIsProtected() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api/forum"));
        properties.setPublicGetPaths(List.of("/api/forum/posts/*"));
        properties.setProtectedWritePaths(List.of("/api/forum/posts/*/comments"));

        FilterResult result = execute(properties, HttpMethod.POST, "/api/forum/posts/9/comments");

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void shouldAllowOAuthAuthorizeUnderAuthPrefixWithExactOauthWhitelist() {
        GatewayAuthProperties properties = buildProperties();
        properties.setPublicPaths(List.of("/api/auth/oauth/**"));

        FilterResult result = execute(properties, HttpMethod.GET, "/api/auth/oauth/wechat/authorize");

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
    }

    @Test
    void shouldAllowPublicGetWhenApiPrefixIsProtectedByDefault() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api"));
        properties.setPublicGetPaths(List.of("/api/forum/posts/*"));

        FilterResult result = execute(properties, HttpMethod.GET, "/api/forum/posts/1");

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
    }

    @Test
    void shouldBlockAuthenticatedApiWhenApiPrefixIsProtectedByDefault() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api"));
        properties.setPublicGetPaths(List.of("/api/forum/posts/*"));

        FilterResult result = execute(properties, HttpMethod.GET, "/api/payment/status/ORDER123");

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void shouldBlockNonAdminTokenForAdminAiPath() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api"));

        FilterResult result = execute(
                properties,
                HttpMethod.GET,
                "/api/admin/ai/providers",
                bearerToken(1001L, "STUDENT")
        );

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.FORBIDDEN, result.status());
    }

    @Test
    void shouldAllowAdminTokenForAdminAiPathAndForwardRoleHeader() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api"));

        FilterResult result = execute(
                properties,
                HttpMethod.GET,
                "/api/admin/ai/providers",
                bearerToken(1L, "ADMIN")
        );

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
        assertEquals("ADMIN", result.forwardedRole());
    }

    @Test
    void shouldBlockStudentTokenForMerchantAiPath() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api"));

        FilterResult result = execute(
                properties,
                HttpMethod.POST,
                "/api/merchant/ai/chat",
                bearerToken(1001L, "STUDENT")
        );

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.FORBIDDEN, result.status());
    }

    @Test
    void shouldAllowMerchantTokenForMerchantAiPathAndForwardRoleHeader() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api"));

        FilterResult result = execute(
                properties,
                HttpMethod.POST,
                "/api/merchant/ai/chat",
                bearerToken(2001L, "MERCHANT")
        );

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
        assertEquals("MERCHANT", result.forwardedRole());
    }

    @Test
    void shouldAllowPublicAuthTokenPathWhenApiPrefixIsProtectedByDefault() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api"));
        properties.setPublicPaths(List.of("/api/auth/tokens"));

        FilterResult result = execute(properties, HttpMethod.POST, "/api/auth/tokens");

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
    }

    @Test
    void shouldRejectValidJwtWhenRedisTokenKeyMissing() {
        GatewayAuthProperties properties = buildStateCheckProperties();
        String token = bearerToken(1001L, "STUDENT");
        GatewayTokenStateValidator validator = validatorWithRedisState(properties, token, 1001L, false, false);

        FilterResult result = execute(
                properties,
                HttpMethod.GET,
                "/api/payment/status/ORDER123",
                token,
                validator
        );

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void shouldRejectValidJwtWhenTokenIsBlacklisted() {
        GatewayAuthProperties properties = buildStateCheckProperties();
        String token = bearerToken(1001L, "STUDENT");
        GatewayTokenStateValidator validator = validatorWithRedisState(properties, token, 1001L, true, false);

        FilterResult result = execute(
                properties,
                HttpMethod.GET,
                "/api/payment/status/ORDER123",
                token,
                validator
        );

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void shouldForwardTrustedHeadersWhenRedisStatePasses() {
        GatewayAuthProperties properties = buildStateCheckProperties();
        String token = bearerToken(2001L, "MERCHANT", "session-1");
        GatewayTokenStateValidator validator = validatorWithRedisState(properties, token, 2001L, false, true);

        FilterResult result = execute(
                properties,
                HttpMethod.POST,
                "/api/merchant/ai/chat",
                token,
                validator
        );

        assertTrue(result.chainInvoked());
        assertEquals(null, result.status());
        assertEquals("2001", result.forwardedUserId());
        assertEquals("MERCHANT", result.forwardedRole());
        assertEquals("access", result.forwardedTokenType());
        assertEquals("session-1", result.forwardedSessionId());
    }

    @Test
    void shouldOverrideForgedUserIdHeader() {
        GatewayAuthProperties properties = buildStateCheckProperties();
        String token = bearerToken(42L, "STUDENT", "session-42");
        GatewayTokenStateValidator validator = validatorWithRedisState(properties, token, 42L, false, true);

        FilterResult result = execute(
                properties,
                HttpMethod.GET,
                "/api/payment/status/ORDER123",
                token,
                validator,
                Map.of("X-User-Id", "999", "X-User-Role", "ADMIN", "X-Session-Id", "forged")
        );

        assertTrue(result.chainInvoked());
        assertEquals("42", result.forwardedUserId());
        assertEquals("STUDENT", result.forwardedRole());
        assertEquals("session-42", result.forwardedSessionId());
    }

    @Test
    void shouldNotAllowLogoutAnonymouslyByLegacyAuthWildcard() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api"));
        properties.setPublicPaths(authPublicPaths());
        properties.setProtectedPaths(List.of("/api/auth/logout", "/api/auth/logout-all", "/api/auth/sessions/**"));

        FilterResult result = execute(properties, HttpMethod.POST, "/api/auth/logout");

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void shouldAllowExactPublicAuthPathsAnonymously() {
        GatewayAuthProperties properties = buildProperties();
        properties.setProtectedPrefixes(List.of("/api"));
        properties.setPublicPaths(authPublicPaths());

        for (String path : List.of("/api/auth/tokens", "/api/auth/users", "/api/auth/refresh")) {
            FilterResult result = execute(properties, HttpMethod.POST, path);
            assertTrue(result.chainInvoked(), path);
            assertEquals(null, result.status(), path);
        }
    }

    @Test
    void shouldFailClosedWhenRedisStateCheckErrorsByDefault() {
        GatewayAuthProperties properties = buildStateCheckProperties();
        String token = bearerToken(1001L, "STUDENT");
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        when(redisTemplate.hasKey("blacklist:" + token)).thenReturn(Mono.error(new IllegalStateException("redis down")));
        GatewayTokenStateValidator validator = new GatewayTokenStateValidator(redisTemplate, properties);

        FilterResult result = execute(
                properties,
                HttpMethod.GET,
                "/api/payment/status/ORDER123",
                token,
                validator
        );

        assertFalse(result.chainInvoked());
        assertEquals(HttpStatus.UNAUTHORIZED, result.status());
    }

    private GatewayAuthProperties buildProperties() {
        GatewayAuthProperties properties = new GatewayAuthProperties();
        properties.setEnabled(true);
        properties.setJwtSecret("campus-life-secret-key-2025-please-change-in-production");
        properties.setStateCheckEnabled(false);
        return properties;
    }

    private GatewayAuthProperties buildStateCheckProperties() {
        GatewayAuthProperties properties = buildProperties();
        properties.setStateCheckEnabled(true);
        properties.setFailOpen(false);
        properties.setProtectedPrefixes(List.of("/api"));
        properties.setRedisTokenPrefix("token:");
        properties.setRedisBlacklistPrefix("blacklist:");
        return properties;
    }

    private List<String> authPublicPaths() {
        return List.of(
                "/api/auth/users",
                "/api/auth/tokens",
                "/api/auth/send-code",
                "/api/auth/refresh",
                "/api/auth/oauth/**"
        );
    }

    private GatewayTokenStateValidator validatorWithRedisState(GatewayAuthProperties properties,
                                                              String token,
                                                              Long userId,
                                                              boolean blacklisted,
                                                              boolean tokenStored) {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        when(redisTemplate.hasKey("blacklist:" + token)).thenReturn(Mono.just(blacklisted));
        if (!blacklisted) {
            when(redisTemplate.hasKey("token:" + userId + ":" + token)).thenReturn(Mono.just(tokenStored));
        }
        return new GatewayTokenStateValidator(redisTemplate, properties);
    }

    private FilterResult execute(GatewayAuthProperties properties, HttpMethod method, String path) {
        return execute(properties, method, path, null);
    }

    private FilterResult execute(GatewayAuthProperties properties, HttpMethod method, String path, String bearerToken) {
        return execute(properties, method, path, bearerToken, new GatewayTokenStateValidator(null, properties));
    }

    private FilterResult execute(GatewayAuthProperties properties,
                                 HttpMethod method,
                                 String path,
                                 String bearerToken,
                                 GatewayTokenStateValidator tokenStateValidator) {
        return execute(properties, method, path, bearerToken, tokenStateValidator, Map.of());
    }

    private FilterResult execute(GatewayAuthProperties properties,
                                 HttpMethod method,
                                 String path,
                                 String bearerToken,
                                 GatewayTokenStateValidator tokenStateValidator,
                                 Map<String, String> headers) {
        JwtAuthGlobalFilter filter = new JwtAuthGlobalFilter(properties, tokenStateValidator, new ObjectMapper());
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        String[] forwardedUserId = new String[1];
        String[] forwardedRole = new String[1];
        String[] forwardedTokenType = new String[1];
        String[] forwardedSessionId = new String[1];
        var requestBuilder = MockServerHttpRequest.method(method, path);
        headers.forEach(requestBuilder::header);
        if (bearerToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }
        MockServerWebExchange exchange = MockServerWebExchange.from(requestBuilder.build());
        GatewayFilterChain chain = serverWebExchange -> {
            chainInvoked.set(true);
            HttpHeaders forwardedHeaders = serverWebExchange.getRequest().getHeaders();
            forwardedUserId[0] = forwardedHeaders.getFirst("X-User-Id");
            forwardedRole[0] = forwardedHeaders.getFirst("X-User-Role");
            forwardedTokenType[0] = forwardedHeaders.getFirst("X-Token-Type");
            forwardedSessionId[0] = forwardedHeaders.getFirst("X-Session-Id");
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        return new FilterResult(
                chainInvoked.get(),
                exchange.getResponse().getStatusCode(),
                forwardedUserId[0],
                forwardedRole[0],
                forwardedTokenType[0],
                forwardedSessionId[0]
        );
    }

    private String bearerToken(Long userId, String role) {
        return bearerToken(userId, role, null);
    }

    private String bearerToken(Long userId, String role, String sessionId) {
        SecretKey signingKey = Keys.hmacShaKeyFor(
                "campus-life-secret-key-2025-please-change-in-production".getBytes(StandardCharsets.UTF_8)
        );
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "access");
        if (sessionId != null) {
            claims.put("sessionId", sessionId);
        }
        if (role != null) {
            claims.put("role", role);
        }
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60000))
                .signWith(signingKey)
                .compact();
    }

    private record FilterResult(boolean chainInvoked,
                                HttpStatusCode status,
                                String forwardedUserId,
                                String forwardedRole,
                                String forwardedTokenType,
                                String forwardedSessionId) {
    }
}
