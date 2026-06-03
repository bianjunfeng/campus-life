package com.campus.campus_life_gateway.auth;

import com.campus.campus_life_gateway.config.GatewayAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Component
public class GatewayTokenStateValidator {

    private static final Logger logger = LoggerFactory.getLogger(GatewayTokenStateValidator.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final GatewayAuthProperties authProperties;

    public GatewayTokenStateValidator(ReactiveStringRedisTemplate redisTemplate, GatewayAuthProperties authProperties) {
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
    }

    public Mono<Boolean> isAccessTokenActive(String userId, String accessToken) {
        if (!authProperties.isStateCheckEnabled()) {
            return Mono.just(true);
        }
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(accessToken)) {
            return Mono.just(false);
        }
        if (redisTemplate == null) {
            return Mono.just(authProperties.isFailOpen());
        }

        String blacklistKey = authProperties.getRedisBlacklistPrefix() + accessToken;
        String tokenKey = authProperties.getRedisTokenPrefix() + userId + ":" + accessToken;
        return redisTemplate.hasKey(blacklistKey)
                .defaultIfEmpty(false)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return Mono.just(false);
                    }
                    return redisTemplate.hasKey(tokenKey)
                            .defaultIfEmpty(false)
                            .map(Boolean.TRUE::equals);
                })
                .onErrorResume(e -> {
                    logger.warn("Redis token state validation failed for userId={}, failOpen={}: {}",
                            userId, authProperties.isFailOpen(), e.getMessage());
                    return Mono.just(authProperties.isFailOpen());
                });
    }
}
