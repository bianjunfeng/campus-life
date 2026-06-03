package com.campus.campus_life_backend.service;

import com.campus.campus_life_backend.modules.auth.service.AuthSessionService;
import com.campus.campus_life_backend.modules.auth.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private AuthSessionService authSessionService;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        tokenService = new TokenService(redisTemplate);
    }

    @Test
    void isAccessTokenValidShouldReturnFalseWhenAccessKeyMissing() {
        String token = "access-token";
        given(redisTemplate.hasKey("blacklist:" + token)).willReturn(false);
        given(redisTemplate.hasKey("token:1:" + token)).willReturn(false);

        assertFalse(tokenService.isAccessTokenValid(1L, token));
    }

    @Test
    void storeTokensShouldWriteSessionIndexes() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        tokenService.storeTokens(1L, "session-1", "access-token", "refresh-token", 60);

        verify(valueOperations).set("token:1:access-token", "refresh-token", 60, TimeUnit.SECONDS);
        verify(valueOperations).set("refresh_token:1:refresh-token", "access-token", 2592000, TimeUnit.SECONDS);
        verify(setOperations).add("auth:user:1:sessions", "session-1");
        verify(redisTemplate).expire("auth:user:1:sessions", 2592000, TimeUnit.SECONDS);
        verify(valueOperations).set("auth:session:session-1:user", "1", 2592000, TimeUnit.SECONDS);
        verify(valueOperations).set("auth:session:session-1:access", "access-token", 60, TimeUnit.SECONDS);
        verify(valueOperations).set("auth:session:session-1:refresh", "refresh-token", 2592000, TimeUnit.SECONDS);
    }

    @Test
    void revokeTokenPairShouldMarkSessionLoggedOut() {
        ObjectProvider<AuthSessionService> provider = mock(ObjectProvider.class);
        given(provider.getIfAvailable()).willReturn(authSessionService);
        TokenService service = new TokenService(redisTemplate, provider);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("token:1:access-token")).willReturn("refresh-token");

        service.revokeTokenPair(1L, "access-token");

        verify(redisTemplate).delete("token:1:access-token");
        verify(redisTemplate).delete("refresh_token:1:refresh-token");
        verify(authSessionService).markLoggedOutByAccessToken("access-token");
    }

    @Test
    void revokeAllUserTokensShouldMarkActiveSessionsWithGivenStatus() {
        ObjectProvider<AuthSessionService> provider = mock(ObjectProvider.class);
        given(provider.getIfAvailable()).willReturn(authSessionService);
        TokenService service = new TokenService(redisTemplate, provider);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.members("auth:user:1:sessions")).willReturn(Set.of("session-1", "session-2"));
        given(valueOperations.get("auth:session:session-1:user")).willReturn("1");
        given(valueOperations.get("auth:session:session-1:access")).willReturn("access-token-1");
        given(valueOperations.get("auth:session:session-1:refresh")).willReturn("refresh-token-1");
        given(redisTemplate.getExpire("auth:session:session-1:access", TimeUnit.SECONDS)).willReturn(60L);
        given(valueOperations.get("auth:session:session-2:user")).willReturn("1");
        given(valueOperations.get("auth:session:session-2:access")).willReturn("access-token-2");
        given(valueOperations.get("auth:session:session-2:refresh")).willReturn("refresh-token-2");
        given(redisTemplate.getExpire("auth:session:session-2:access", TimeUnit.SECONDS)).willReturn(120L);

        service.revokeAllUserTokens(1L, AuthSessionService.STATUS_LOGGED_OUT);

        verify(redisTemplate).delete("token:1:access-token-1");
        verify(redisTemplate).delete("refresh_token:1:refresh-token-1");
        verify(valueOperations).set("blacklist:access-token-1", "1", 60L, TimeUnit.SECONDS);
        verify(redisTemplate).delete("token:1:access-token-2");
        verify(redisTemplate).delete("refresh_token:1:refresh-token-2");
        verify(valueOperations).set("blacklist:access-token-2", "1", 120L, TimeUnit.SECONDS);
        verify(authSessionService).markLoggedOutBySessionId("session-1");
        verify(authSessionService).markLoggedOutBySessionId("session-2");
        verify(authSessionService).updateUserActiveSessionsStatus(1L, AuthSessionService.STATUS_LOGGED_OUT);
    }

    @Test
    void revokeOtherUserSessionsShouldKeepCurrentSession() {
        ObjectProvider<AuthSessionService> provider = mock(ObjectProvider.class);
        given(provider.getIfAvailable()).willReturn(authSessionService);
        TokenService service = new TokenService(redisTemplate, provider);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.members("auth:user:1:sessions")).willReturn(Set.of("session-current", "session-other"));
        given(valueOperations.get("auth:session:session-other:user")).willReturn("1");
        given(valueOperations.get("auth:session:session-other:access")).willReturn("other-access");
        given(valueOperations.get("auth:session:session-other:refresh")).willReturn("other-refresh");
        given(redisTemplate.getExpire("auth:session:session-other:access", TimeUnit.SECONDS)).willReturn(60L);

        service.revokeOtherUserSessions(1L, "session-current", AuthSessionService.STATUS_LOGGED_OUT);

        verify(redisTemplate).delete("token:1:other-access");
        verify(redisTemplate).delete("refresh_token:1:other-refresh");
        verify(valueOperations).set("blacklist:other-access", "1", 60L, TimeUnit.SECONDS);
        verify(authSessionService).markLoggedOutBySessionId("session-other");
    }

    @Test
    void revokeSessionShouldClearSessionIndexesAndMarkLoggedOut() {
        ObjectProvider<AuthSessionService> provider = mock(ObjectProvider.class);
        given(provider.getIfAvailable()).willReturn(authSessionService);
        TokenService service = new TokenService(redisTemplate, provider);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(valueOperations.get("auth:session:session-1:user")).willReturn("1");
        given(valueOperations.get("auth:session:session-1:access")).willReturn("access-token");
        given(valueOperations.get("auth:session:session-1:refresh")).willReturn("refresh-token");
        given(redisTemplate.getExpire("auth:session:session-1:access", TimeUnit.SECONDS)).willReturn(60L);

        service.revokeSession(1L, "session-1", AuthSessionService.STATUS_LOGGED_OUT);

        verify(redisTemplate).delete("token:1:access-token");
        verify(redisTemplate).delete("refresh_token:1:refresh-token");
        verify(valueOperations).set("blacklist:access-token", "1", 60L, TimeUnit.SECONDS);
        verify(setOperations).remove("auth:user:1:sessions", "session-1");
        verify(authSessionService).markLoggedOutBySessionId("session-1");
    }

    @Test
    void tryAcquireRefreshLockShouldUseShortRedisLock() {
        given(valueOperations.setIfAbsent("refresh:refresh-jti:lock", "1:session-1", 10, TimeUnit.SECONDS))
                .willReturn(true);

        assertTrue(tokenService.tryAcquireRefreshLock("refresh-jti", "1:session-1"));
    }

    @Test
    void markRefreshTokenUsedShouldStoreJtiMarker() {
        tokenService.markRefreshTokenUsed("refresh-jti", 1L, "session-1", 3600);

        verify(valueOperations).set("used_refresh_token:refresh-jti", "1:session-1", 3600, TimeUnit.SECONDS);
    }

    @Test
    void consumeRefreshTokenShouldDeleteRefreshKeyAndReturnOldAccessToken() {
        given(valueOperations.get("refresh_token:1:refresh-token")).willReturn("access-token");

        String accessToken = tokenService.consumeRefreshToken(1L, "refresh-token");

        assertEquals("access-token", accessToken);
        verify(redisTemplate).delete("refresh_token:1:refresh-token");
    }

    @Test
    void blacklistAccessTokenForRefreshShouldBlacklistAndDeleteAccessKey() {
        tokenService.blacklistAccessTokenForRefresh(1L, "access-token", 60);

        verify(valueOperations).set("blacklist:access-token", "1", 60, TimeUnit.SECONDS);
        verify(redisTemplate).delete("token:1:access-token");
    }
}
