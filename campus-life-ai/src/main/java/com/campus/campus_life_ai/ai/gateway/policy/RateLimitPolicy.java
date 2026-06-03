package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.entity.AiCapabilityConfig;
import com.campus.campus_life_ai.ai.gateway.GatewayErrorCode;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.mapper.AiCapabilityConfigMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(40)
public class RateLimitPolicy implements GatewayPolicy {

    private final AiCapabilityConfigMapper aiCapabilityConfigMapper;
    private final ObjectMapper objectMapper;
    private final Map<String, CounterWindow> windows = new ConcurrentHashMap<>();

    public RateLimitPolicy(AiCapabilityConfigMapper aiCapabilityConfigMapper, ObjectMapper objectMapper) {
        this.aiCapabilityConfigMapper = aiCapabilityConfigMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void validate(GatewayRequest request) {
        RateLimitRule rule = loadRule(request);
        if (rule == null) {
            return;
        }

        long bucket = Instant.now().getEpochSecond() / rule.windowSeconds();
        String key = key(rule.scope(), request);
        CounterWindow window = windows.compute(key, (ignored, current) -> {
            if (current == null || current.bucket() != bucket) {
                return new CounterWindow(bucket, new AtomicInteger(1));
            }
            current.count().incrementAndGet();
            return current;
        });
        if (windows.size() > 2000) {
            cleanup(bucket);
        }
        if (window != null && window.count().get() > rule.maxRequests()) {
            throw new GatewayPolicyException(
                    GatewayErrorCode.RATE_LIMITED,
                    "AI 能力请求触发限流: " + request.getCapabilityCode()
            );
        }
    }

    private RateLimitRule loadRule(GatewayRequest request) {
        if (request == null || !StringUtils.hasText(request.getCapabilityCode())) {
            return null;
        }
        AiCapabilityConfig capability = aiCapabilityConfigMapper.findByCapabilityCode(request.getCapabilityCode().trim());
        if (capability == null || !StringUtils.hasText(capability.getRateLimitJson())) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(capability.getRateLimitJson());
            if (!root.path("enabled").asBoolean(true)) {
                return null;
            }
            int maxRequests = root.path("maxRequests").asInt(0);
            int windowSeconds = root.path("windowSeconds").asInt(60);
            if (maxRequests <= 0 || windowSeconds <= 0) {
                return null;
            }
            String scope = root.path("scope").asText("USER").trim().toUpperCase(Locale.ROOT);
            return new RateLimitRule(maxRequests, Math.min(windowSeconds, 86400), normalizeScope(scope));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String key(String scope, GatewayRequest request) {
        String capability = tag(request == null ? null : request.getCapabilityCode());
        String scene = tag(request == null ? null : request.getSceneCode());
        String user = request == null || request.getUserId() == null ? "anonymous" : request.getUserId().toString();
        return switch (scope) {
            case "GLOBAL" -> "global:" + capability;
            case "SCENE" -> "scene:" + capability + ":" + scene;
            case "USER_SCENE" -> "user-scene:" + user + ":" + capability + ":" + scene;
            default -> "user:" + user + ":" + capability;
        };
    }

    private String normalizeScope(String scope) {
        return switch (scope) {
            case "GLOBAL", "SCENE", "USER_SCENE" -> scope;
            default -> "USER";
        };
    }

    private String tag(String value) {
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }

    private void cleanup(long currentBucket) {
        Iterator<Map.Entry<String, CounterWindow>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CounterWindow> entry = iterator.next();
            if (entry.getValue().bucket() < currentBucket - 1) {
                iterator.remove();
            }
        }
    }

    private record RateLimitRule(int maxRequests, int windowSeconds, String scope) {
    }

    private record CounterWindow(long bucket, AtomicInteger count) {
    }
}
