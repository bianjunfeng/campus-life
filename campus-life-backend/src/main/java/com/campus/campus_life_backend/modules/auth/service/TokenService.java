package com.campus.campus_life_backend.modules.auth.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.presence.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token 服务类
 * 使用 Redis 管理 Token，支持 Token 黑名单和刷新机制
 * 安全策略：Redis 不可用时，令牌校验失败（fail-closed）
 */
@Service
public class TokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthSessionService authSessionService;
    private final PresenceService presenceService;
    private boolean useRedis = false;
    @Value("${jwt.refresh-expiration:2592000000}")
    private Long refreshExpirationMillis = 2592000000L;
    private static final String TOKEN_PREFIX = "token:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USED_REFRESH_TOKEN_PREFIX = "used_refresh_token:";
    private static final String REFRESH_LOCK_PREFIX = "refresh:";
    private static final String REFRESH_LOCK_SUFFIX = ":lock";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String USER_SESSION_PREFIX = "auth:user:";
    private static final String USER_SESSION_SUFFIX = ":sessions";
    private static final String SESSION_PREFIX = "auth:session:";
    private static final String SESSION_USER_SUFFIX = ":user";
    private static final String SESSION_ACCESS_SUFFIX = ":access";
    private static final String SESSION_REFRESH_SUFFIX = ":refresh";
    private static final long REFRESH_LOCK_TTL_SECONDS = 10L;
    
    public TokenService(RedisTemplate<String, String> redisTemplate) {
        this(redisTemplate, null, null);
    }

    public TokenService(RedisTemplate<String, String> redisTemplate,
                        ObjectProvider<AuthSessionService> authSessionServiceProvider) {
        this(redisTemplate, authSessionServiceProvider, null);
    }

    @Autowired(required = false)
    public TokenService(RedisTemplate<String, String> redisTemplate,
                        ObjectProvider<AuthSessionService> authSessionServiceProvider,
                        ObjectProvider<PresenceService> presenceServiceProvider) {
        this.redisTemplate = redisTemplate;
        this.authSessionService = authSessionServiceProvider == null ? null : authSessionServiceProvider.getIfAvailable();
        this.presenceService = presenceServiceProvider == null ? null : presenceServiceProvider.getIfAvailable();
        // 测试 Redis 连接
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set("test:token:connection", "ok", 1, TimeUnit.SECONDS);
                redisTemplate.delete("test:token:connection");
                this.useRedis = true;
                logger.info("Redis 连接成功，启用 Token 黑名单功能");
            } catch (Exception e) {
                this.useRedis = false;
                logger.warn("Redis 连接失败，Token 状态校验将 fail-closed: {}", e.getMessage());
            }
        } else {
            this.useRedis = false;
            logger.info("Redis 未配置，Token 状态校验将 fail-closed");
        }
    }
    
    /**
     * 存储访问令牌和刷新令牌的关联关系
     * @param userId 用户ID
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param accessTokenExpiration 访问令牌过期时间（秒）
     */
    public void storeTokens(Long userId, String accessToken, String refreshToken, long accessTokenExpiration) {
        storeTokens(userId, null, accessToken, refreshToken, accessTokenExpiration);
    }

    public void storeTokens(Long userId,
                            String sessionId,
                            String accessToken,
                            String refreshToken,
                            long accessTokenExpiration) {
        if (!useRedis || redisTemplate == null) {
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE);
        }
        
        try {
            String accessKey = TOKEN_PREFIX + userId + ":" + accessToken;
            String refreshKey = REFRESH_TOKEN_PREFIX + userId + ":" + refreshToken;
            long accessExpiration = Math.max(1L, accessTokenExpiration);
            long refreshTokenExpiration = Math.max(1L, refreshExpirationMillis / 1000L);
            
            // 存储访问令牌和刷新令牌的映射关系
            redisTemplate.opsForValue().set(accessKey, refreshToken, accessExpiration, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(refreshKey, accessToken, refreshTokenExpiration, TimeUnit.SECONDS);
            storeSessionIndexes(userId, sessionId, accessToken, refreshToken, accessExpiration, refreshTokenExpiration);
        } catch (Exception e) {
            logger.error("Redis 操作失败，令牌存储失败: {}", e.getMessage(), e);
            useRedis = false;
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE, e);
        }
    }

    public boolean isTokenStoreAvailable() {
        return useRedis && redisTemplate != null;
    }

    public void requireTokenStoreAvailable() {
        if (!isTokenStoreAvailable()) {
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE);
        }
        try {
            redisTemplate.opsForValue().set("test:token:health", "ok", 1, TimeUnit.SECONDS);
            redisTemplate.delete("test:token:health");
        } catch (Exception e) {
            useRedis = false;
            logger.error("Redis 健康检查失败，拒绝签发令牌: {}", e.getMessage(), e);
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE, e);
        }
    }
    
    /**
     * 验证访问令牌是否有效
     * @param userId 用户ID
     * @param token 访问令牌
     * @return 是否有效
     */
    public boolean isAccessTokenValid(Long userId, String token) {
        if (!useRedis || redisTemplate == null || userId == null || token == null || token.isBlank()) {
            logger.error("Redis 不可用，拒绝访问令牌（fail-closed）: userId={}", userId);
            return false;
        }
        
        try {
            String blacklistKey = BLACKLIST_PREFIX + token;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                return false;
            }

            String tokenKey = TOKEN_PREFIX + userId + ":" + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
        } catch (Exception e) {
            logger.error("Redis 操作失败: {}", e.getMessage(), e);
            useRedis = false;
            return false; // Redis 失败时，拒绝通过
        }
    }
    
    /**
     * 验证刷新令牌是否有效
     * @param userId 用户ID
     * @param refreshToken 刷新令牌
     * @return 是否有效
     */
    public boolean isRefreshTokenValid(Long userId, String refreshToken) {
        if (!useRedis || redisTemplate == null) {
            logger.error("Redis 不可用，拒绝刷新令牌（fail-closed）: userId={}", userId);
            return false;
        }
        
        try {
            String refreshKey = REFRESH_TOKEN_PREFIX + userId + ":" + refreshToken;
            return Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey));
        } catch (Exception e) {
            logger.error("Redis 操作失败: {}", e.getMessage(), e);
            useRedis = false;
            return false; // Redis 失败时，拒绝通过
        }
    }
    
    /**
     * 将访问令牌加入黑名单（登出时使用）
     * @param userId 用户ID
     * @param token 访问令牌
     * @param expiration 过期时间（秒）
     */
    public void blacklistAccessToken(Long userId, String token, long expiration) {
        if (!useRedis || redisTemplate == null) {
            // Redis 不可用时，无法使用黑名单功能
            return;
        }
        
        try {
            String blacklistKey = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(blacklistKey, "1", expiration, TimeUnit.SECONDS);
            
            // 删除原来的令牌记录
            String tokenKey = TOKEN_PREFIX + userId + ":" + token;
            redisTemplate.delete(tokenKey);
        } catch (Exception e) {
            logger.error("Redis 操作失败，Token 状态校验将 fail-closed: {}", e.getMessage(), e);
            useRedis = false;
        }
    }
    
    /**
     * 删除用户的所有令牌（强制登出）
     * @param userId 用户ID
     */
    public void revokeAllUserTokens(Long userId) {
        revokeAllUserTokens(userId, AuthSessionService.STATUS_FORCE_LOGOUT);
    }

    /**
     * 删除用户的所有令牌，并按调用场景同步会话状态
     */
    public void revokeAllUserTokens(Long userId, int sessionStatus) {
        if (userId == null) {
            return;
        }
        try {
            requireTokenStoreAvailable();

            Set<String> sessionIds = findUserSessionIds(userId);
            for (String sessionId : sessionIds) {
                revokeSessionInternal(userId, sessionId, sessionStatus);
            }

            // 兼容历史未写入 session 索引的 token。
            String accessPattern = TOKEN_PREFIX + userId + ":*";
            String refreshPattern = REFRESH_TOKEN_PREFIX + userId + ":*";
            Set<String> accessKeys = redisTemplate.keys(accessPattern);
            Set<String> refreshKeys = redisTemplate.keys(refreshPattern);
            if (accessKeys != null && !accessKeys.isEmpty()) {
                redisTemplate.delete(accessKeys);
            }
            if (refreshKeys != null && !refreshKeys.isEmpty()) {
                redisTemplate.delete(refreshKeys);
            }
            redisTemplate.delete(userSessionsKey(userId));
            markUserSessions(userId, sessionStatus);
            markPresenceUserOffline(userId);
            logger.info("已吊销用户全部令牌: userId={}, sessions={}, legacyAccessKeys={}, legacyRefreshKeys={}",
                    userId,
                    sessionIds.size(),
                    accessKeys == null ? 0 : accessKeys.size(),
                    refreshKeys == null ? 0 : refreshKeys.size());
        } catch (Exception e) {
            handleRedisFailure("批量吊销用户令牌失败", e);
        }
    }

    public void revokeOtherUserSessions(Long userId, String currentSessionId, int sessionStatus) {
        if (userId == null || currentSessionId == null || currentSessionId.isBlank()) {
            return;
        }

        try {
            requireTokenStoreAvailable();
            Set<String> sessionIds = findUserSessionIds(userId);
            for (String sessionId : sessionIds) {
                if (!currentSessionId.equals(sessionId)) {
                    revokeSessionInternal(userId, sessionId, sessionStatus);
                }
            }
        } catch (Exception e) {
            handleRedisFailure("吊销其他设备会话失败", e);
        }
    }

    public void revokeSession(Long userId, String sessionId, int sessionStatus) {
        if (userId == null || sessionId == null || sessionId.isBlank()) {
            return;
        }

        try {
            requireTokenStoreAvailable();
            revokeSessionInternal(userId, sessionId, sessionStatus);
        } catch (Exception e) {
            handleRedisFailure("吊销指定会话失败", e);
        }
    }

    public Set<String> findUserSessionIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        requireTokenStoreAvailable();
        try {
            Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey(userId));
            return sessionIds == null ? Set.of() : new LinkedHashSet<>(sessionIds);
        } catch (Exception e) {
            logger.error("Redis 操作失败，读取用户会话索引失败: {}", e.getMessage(), e);
            useRedis = false;
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * 同一个 refresh token 只能有一个刷新请求进入轮换流程。
     */
    public boolean tryAcquireRefreshLock(String refreshJti, String owner) {
        if (!useRedis || redisTemplate == null || refreshJti == null || refreshJti.isBlank()) {
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE);
        }

        try {
            String lockKey = refreshLockKey(refreshJti);
            String lockValue = owner == null || owner.isBlank() ? "1" : owner;
            return Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, REFRESH_LOCK_TTL_SECONDS, TimeUnit.SECONDS));
        } catch (Exception e) {
            logger.error("Redis 操作失败，刷新令牌加锁失败: {}", e.getMessage(), e);
            useRedis = false;
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE, e);
        }
    }

    public void releaseRefreshLock(String refreshJti) {
        if (!useRedis || redisTemplate == null || refreshJti == null || refreshJti.isBlank()) {
            return;
        }

        try {
            redisTemplate.delete(refreshLockKey(refreshJti));
        } catch (Exception e) {
            logger.error("Redis 操作失败，刷新令牌解锁失败: {}", e.getMessage(), e);
            useRedis = false;
        }
    }

    public boolean isRefreshTokenUsed(String refreshJti) {
        if (!useRedis || redisTemplate == null || refreshJti == null || refreshJti.isBlank()) {
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE);
        }

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(USED_REFRESH_TOKEN_PREFIX + refreshJti));
        } catch (Exception e) {
            logger.error("Redis 操作失败，刷新令牌复用检查失败: {}", e.getMessage(), e);
            useRedis = false;
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE, e);
        }
    }

    public void markRefreshTokenUsed(String refreshJti, Long userId, String sessionId, long expirationSeconds) {
        if (!useRedis || redisTemplate == null || refreshJti == null || refreshJti.isBlank()) {
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE);
        }

        try {
            String value = userId == null ? "" : String.valueOf(userId);
            if (sessionId != null && !sessionId.isBlank()) {
                value = value.isBlank() ? sessionId : value + ":" + sessionId;
            }
            redisTemplate.opsForValue().set(
                    USED_REFRESH_TOKEN_PREFIX + refreshJti,
                    value.isBlank() ? "used" : value,
                    Math.max(1L, expirationSeconds),
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            logger.error("Redis 操作失败，刷新令牌使用标记写入失败: {}", e.getMessage(), e);
            useRedis = false;
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE, e);
        }
    }

    public String consumeRefreshToken(Long userId, String refreshToken) {
        if (!useRedis || redisTemplate == null || userId == null || refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE);
        }

        try {
            String refreshKey = REFRESH_TOKEN_PREFIX + userId + ":" + refreshToken;
            String accessToken = redisTemplate.opsForValue().get(refreshKey);
            if (accessToken == null || accessToken.isBlank()) {
                return null;
            }
            redisTemplate.delete(refreshKey);
            return accessToken;
        } catch (Exception e) {
            logger.error("Redis 操作失败，刷新令牌消费失败: {}", e.getMessage(), e);
            useRedis = false;
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE, e);
        }
    }

    public void blacklistAccessTokenForRefresh(Long userId, String token, long expiration) {
        if (!useRedis || redisTemplate == null || userId == null || token == null || token.isBlank()) {
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE);
        }

        try {
            if (expiration > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", expiration, TimeUnit.SECONDS);
            }
            redisTemplate.delete(TOKEN_PREFIX + userId + ":" + token);
        } catch (Exception e) {
            logger.error("Redis 操作失败，旧访问令牌吊销失败: {}", e.getMessage(), e);
            useRedis = false;
            throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * 吊销当前访问令牌及其对应的刷新令牌
     * @param userId 用户ID
     * @param accessToken 访问令牌
     */
    public void revokeTokenPair(Long userId, String accessToken) {
        if (userId == null || accessToken == null || accessToken.isBlank()) {
            return;
        }

        try {
            if (useRedis && redisTemplate != null) {
                String accessKey = TOKEN_PREFIX + userId + ":" + accessToken;
                String refreshToken = redisTemplate.opsForValue().get(accessKey);
                redisTemplate.delete(accessKey);

                if (refreshToken != null && !refreshToken.isBlank()) {
                    String refreshKey = REFRESH_TOKEN_PREFIX + userId + ":" + refreshToken;
                    redisTemplate.delete(refreshKey);
                }
            }
        } catch (Exception e) {
            logger.error("吊销令牌对失败，Token 状态校验将 fail-closed: {}", e.getMessage(), e);
            useRedis = false;
        } finally {
            markLoggedOutByAccessToken(accessToken);
        }
    }
    
    /**
     * 删除刷新令牌（刷新令牌使用后删除）
     * @param userId 用户ID
     * @param refreshToken 刷新令牌
     */
    public void deleteRefreshToken(Long userId, String refreshToken) {
        if (!useRedis || redisTemplate == null) {
            return;
        }
        
        try {
            String refreshKey = REFRESH_TOKEN_PREFIX + userId + ":" + refreshToken;
            redisTemplate.delete(refreshKey);
        } catch (Exception e) {
            logger.error("Redis 操作失败，Token 状态校验将 fail-closed: {}", e.getMessage(), e);
            useRedis = false;
        }
    }
    
    /**
     * 获取刷新令牌对应的访问令牌
     * @param userId 用户ID
     * @param refreshToken 刷新令牌
     * @return 访问令牌（如果存在）
     */
    public String getAccessTokenByRefreshToken(Long userId, String refreshToken) {
        if (!useRedis || redisTemplate == null) {
            return null;
        }
        
        try {
            String refreshKey = REFRESH_TOKEN_PREFIX + userId + ":" + refreshToken;
            return redisTemplate.opsForValue().get(refreshKey);
        } catch (Exception e) {
            logger.error("Redis 操作失败: {}", e.getMessage(), e);
            useRedis = false;
            return null;
        }
    }

    private void markLoggedOutByAccessToken(String accessToken) {
        try {
            if (authSessionService != null) {
                authSessionService.markLoggedOutByAccessToken(accessToken);
            }
        } catch (Exception e) {
            logger.warn("同步登出会话状态失败: {}", e.getMessage());
        }
    }

    private void markUserSessions(Long userId, int sessionStatus) {
        try {
            if (authSessionService != null) {
                if (sessionStatus == AuthSessionService.STATUS_FORCE_LOGOUT) {
                    authSessionService.markForceLogoutByUserId(userId);
                } else {
                    authSessionService.updateUserActiveSessionsStatus(userId, sessionStatus);
                }
            }
        } catch (Exception e) {
            logger.warn("同步用户会话状态失败: userId={}, status={}, error={}", userId, sessionStatus, e.getMessage());
        }
    }

    private String refreshLockKey(String refreshJti) {
        return REFRESH_LOCK_PREFIX + refreshJti + REFRESH_LOCK_SUFFIX;
    }

    private void storeSessionIndexes(Long userId,
                                     String sessionId,
                                     String accessToken,
                                     String refreshToken,
                                     long accessTokenExpiration,
                                     long refreshTokenExpiration) {
        if (userId == null || sessionId == null || sessionId.isBlank()) {
            return;
        }
        String userSessionsKey = userSessionsKey(userId);
        redisTemplate.opsForSet().add(userSessionsKey, sessionId);
        redisTemplate.expire(userSessionsKey, refreshTokenExpiration, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(sessionUserKey(sessionId), String.valueOf(userId), refreshTokenExpiration, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(sessionAccessKey(sessionId), accessToken, accessTokenExpiration, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(sessionRefreshKey(sessionId), refreshToken, refreshTokenExpiration, TimeUnit.SECONDS);
    }

    private void revokeSessionInternal(Long userId, String sessionId, int sessionStatus) {
        String owner = redisTemplate.opsForValue().get(sessionUserKey(sessionId));
        if (owner != null && !owner.isBlank() && !String.valueOf(userId).equals(owner)) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN);
        }

        String accessSessionKey = sessionAccessKey(sessionId);
        String refreshSessionKey = sessionRefreshKey(sessionId);
        String accessToken = redisTemplate.opsForValue().get(accessSessionKey);
        String refreshToken = redisTemplate.opsForValue().get(refreshSessionKey);

        if (accessToken != null && !accessToken.isBlank()) {
            Long accessTtlSeconds = redisTemplate.getExpire(accessSessionKey, TimeUnit.SECONDS);
            redisTemplate.delete(TOKEN_PREFIX + userId + ":" + accessToken);
            if (accessTtlSeconds != null && accessTtlSeconds > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + accessToken, "1", accessTtlSeconds, TimeUnit.SECONDS);
            }
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId + ":" + refreshToken);
        }

        redisTemplate.delete(Set.of(sessionUserKey(sessionId), accessSessionKey, refreshSessionKey));
        redisTemplate.opsForSet().remove(userSessionsKey(userId), sessionId);
        markSessionStatus(sessionId, sessionStatus);
        markPresenceSessionOffline(userId, sessionId);
    }

    private void markSessionStatus(String sessionId, int sessionStatus) {
        try {
            if (authSessionService == null) {
                return;
            }
            if (sessionStatus == AuthSessionService.STATUS_FORCE_LOGOUT) {
                authSessionService.updateStatusBySessionId(sessionId, AuthSessionService.STATUS_FORCE_LOGOUT);
            } else {
                authSessionService.markLoggedOutBySessionId(sessionId);
            }
        } catch (Exception e) {
            logger.warn("同步会话状态失败: sessionId={}, status={}, error={}", sessionId, sessionStatus, e.getMessage());
        }
    }

    private void markPresenceSessionOffline(Long userId, String sessionId) {
        try {
            if (presenceService != null) {
                presenceService.markOffline(userId, sessionId);
            }
        } catch (Exception e) {
            logger.warn("同步在线状态失败: userId={}, sessionId={}, error={}", userId, sessionId, e.getMessage());
        }
    }

    private void markPresenceUserOffline(Long userId) {
        try {
            if (presenceService != null) {
                presenceService.markUserOffline(userId);
            }
        } catch (Exception e) {
            logger.warn("同步用户在线状态失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    private void handleRedisFailure(String message, Exception e) {
        if (e instanceof BusinessException businessException) {
            throw businessException;
        }
        logger.error("{}，Token 状态校验将 fail-closed: {}", message, e.getMessage(), e);
        useRedis = false;
        throw new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE, e);
    }

    private String userSessionsKey(Long userId) {
        return USER_SESSION_PREFIX + userId + USER_SESSION_SUFFIX;
    }

    private String sessionUserKey(String sessionId) {
        return SESSION_PREFIX + sessionId + SESSION_USER_SUFFIX;
    }

    private String sessionAccessKey(String sessionId) {
        return SESSION_PREFIX + sessionId + SESSION_ACCESS_SUFFIX;
    }

    private String sessionRefreshKey(String sessionId) {
        return SESSION_PREFIX + sessionId + SESSION_REFRESH_SUFFIX;
    }
}
