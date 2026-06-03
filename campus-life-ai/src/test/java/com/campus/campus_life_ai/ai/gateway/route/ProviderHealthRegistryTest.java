package com.campus.campus_life_ai.ai.gateway.route;

import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderHealthRegistryTest {

    @Test
    void shouldCooldownCandidateAfterFailureThresholdAndResetOnSuccess() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.getGateway().setHealthFailureThreshold(2);
        properties.getGateway().setHealthCooldownMs(60000L);
        ProviderHealthRegistry registry = new ProviderHealthRegistry(properties);
        RouteCandidate candidate = RouteCandidate.builder()
                .providerCode("provider-a")
                .modelCode("model-a")
                .build();

        registry.recordFailure(candidate);
        assertTrue(registry.isHealthy(candidate));

        registry.recordFailure(candidate);
        assertFalse(registry.isHealthy(candidate));

        registry.recordSuccess(candidate);
        assertTrue(registry.isHealthy(candidate));
    }
}
