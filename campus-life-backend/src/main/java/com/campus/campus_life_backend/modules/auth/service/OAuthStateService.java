package com.campus.campus_life_backend.modules.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OAuth state 服务，防止第三方登录回调被伪造或复用。
 */
@Service
public class OAuthStateService {

    private static final Logger logger = LoggerFactory.getLogger(OAuthStateService.class);
    private static final String STATE_PREFIX = "oauth:state:";
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;
    private final Cache<String, String> localStateCache = Caffeine.newBuilder()
            .expireAfterWrite(STATE_TTL)
            .maximumSize(10_000)
            .build();
    private boolean useRedis = false;

    @Autowired(required = false)
    public OAuthStateService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set("test:oauth:state", "ok", 1, TimeUnit.SECONDS);
                stringRedisTemplate.delete("test:oauth:state");
                this.useRedis = true;
                logger.info("Redis 连接成功，启用 OAuth state 存储");
            } catch (Exception e) {
                this.useRedis = false;
                logger.warn("Redis 连接失败，OAuth state 使用本地缓存降级: {}", e.getMessage());
            }
        } else {
            logger.info("Redis 未配置，OAuth state 使用本地缓存");
        }
    }

    public String createState(String provider, String redirectUri) {
        String state = UUID.randomUUID().toString().replace("-", "");
        String key = buildKey(provider, state);
        String value = normalizeRedirectUri(redirectUri);

        if (useRedis && stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set(key, value, STATE_TTL.getSeconds(), TimeUnit.SECONDS);
                return state;
            } catch (Exception e) {
                useRedis = false;
                logger.warn("Redis 存储 OAuth state 失败，降级为本地缓存: {}", e.getMessage());
            }
        }

        localStateCache.put(key, value);
        return state;
    }

    public boolean validateAndConsumeState(String provider, String state, String redirectUri) {
        if (provider == null || provider.isBlank() || state == null || state.isBlank()) {
            return false;
        }

        String key = buildKey(provider, state);
        String expectedRedirectUri = normalizeRedirectUri(redirectUri);

        if (useRedis && stringRedisTemplate != null) {
            try {
                String storedRedirectUri = stringRedisTemplate.opsForValue().get(key);
                if (!expectedRedirectUri.equals(storedRedirectUri)) {
                    return false;
                }
                stringRedisTemplate.delete(key);
                return true;
            } catch (Exception e) {
                useRedis = false;
                logger.warn("Redis 校验 OAuth state 失败，降级为本地缓存: {}", e.getMessage());
            }
        }

        String storedRedirectUri = localStateCache.getIfPresent(key);
        if (!expectedRedirectUri.equals(storedRedirectUri)) {
            return false;
        }
        localStateCache.invalidate(key);
        return true;
    }

    private String buildKey(String provider, String state) {
        return STATE_PREFIX + provider + ":" + state;
    }

    private String normalizeRedirectUri(String redirectUri) {
        return redirectUri == null ? "" : redirectUri.trim();
    }
}
