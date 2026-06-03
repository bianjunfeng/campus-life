package com.campus.campus_life_gateway.filter;

import com.campus.campus_life_gateway.config.GatewayGrayProperties;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class GrayTrafficWebFilter implements WebFilter, Ordered {

    private static final String API_PREFIX = "/api";

    private final GatewayGrayProperties grayProperties;

    public GrayTrafficWebFilter(GatewayGrayProperties grayProperties) {
        this.grayProperties = grayProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!grayProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        if (!path.startsWith(API_PREFIX) || path.startsWith(grayProperties.getInternalPrefix())) {
            return chain.filter(exchange);
        }

        String headerName = grayProperties.getHeaderName();
        String headerValue = grayProperties.getHeaderValue();
        String actual = request.getHeaders().getFirst(headerName);
        if (!StringUtils.hasText(actual) || !headerValue.equals(actual.trim())) {
            return chain.filter(exchange);
        }

        String grayPath = grayProperties.getInternalPrefix() + path;
        URI newUri = URI.create(grayPath
                + (request.getURI().getRawQuery() == null ? "" : "?" + request.getURI().getRawQuery()));

        ServerHttpRequest mutated = request.mutate()
                .uri(newUri)
                .path(grayPath)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
