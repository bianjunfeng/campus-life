package com.campus.campus_life_ai.ai.gateway.route;

import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ProviderHealthRegistry {

    private final ConcurrentMap<String, HealthState> healthStates = new ConcurrentHashMap<>();
    private final AiProviderProperties aiProviderProperties;

    public ProviderHealthRegistry(AiProviderProperties aiProviderProperties) {
        this.aiProviderProperties = aiProviderProperties;
    }

    public boolean isHealthy(RouteCandidate candidate) {
        HealthState state = healthStates.get(key(candidate));
        return state == null || state.unhealthyUntilMs <= System.currentTimeMillis();
    }

    public void recordSuccess(RouteCandidate candidate) {
        healthStates.remove(key(candidate));
    }

    public void recordFailure(RouteCandidate candidate) {
        String key = key(candidate);
        int failureThreshold = Math.max(1, aiProviderProperties.getGateway().getHealthFailureThreshold());
        long cooldownMs = Math.max(0L, aiProviderProperties.getGateway().getHealthCooldownMs());
        healthStates.compute(key, (ignored, current) -> {
            int failureCount = current == null ? 1 : current.failureCount + 1;
            long unhealthyUntilMs = current == null ? 0L : current.unhealthyUntilMs;
            if (failureCount >= failureThreshold && cooldownMs > 0L) {
                unhealthyUntilMs = System.currentTimeMillis() + cooldownMs;
            }
            return new HealthState(failureCount, unhealthyUntilMs);
        });
    }

    private String key(RouteCandidate candidate) {
        if (candidate == null) {
            return "unknown:unknown";
        }
        return tag(candidate.getProviderCode()) + ":" + tag(candidate.getModelCode());
    }

    private String tag(String value) {
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }

    private record HealthState(int failureCount, long unhealthyUntilMs) {
    }
}
