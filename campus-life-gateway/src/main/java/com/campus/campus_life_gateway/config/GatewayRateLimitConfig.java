package com.campus.campus_life_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayRateLimitConfig {

    @Bean("userOrIpKeyResolver")
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (StringUtils.hasText(userId)) {
                return Mono.just("uid:" + userId);
            }
            String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (StringUtils.hasText(ip)) {
                int comma = ip.indexOf(',');
                String firstIp = comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
                return Mono.just("ip:" + firstIp);
            }
            if (exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                return Mono.just("ip:" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }
            return Mono.just("ip:unknown");
        };
    }
}
