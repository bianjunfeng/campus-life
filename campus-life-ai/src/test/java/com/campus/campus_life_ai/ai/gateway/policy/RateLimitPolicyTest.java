package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.entity.AiCapabilityConfig;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.mapper.AiCapabilityConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RateLimitPolicyTest {

    private final AiCapabilityConfigMapper capabilityConfigMapper = mock(AiCapabilityConfigMapper.class);
    private final RateLimitPolicy policy = new RateLimitPolicy(capabilityConfigMapper, new ObjectMapper());

    @Test
    void shouldRejectWhenUserWindowExceedsConfiguredLimit() {
        AiCapabilityConfig capabilityConfig = new AiCapabilityConfig();
        capabilityConfig.setRateLimitJson("""
                {"enabled":true,"scope":"USER","maxRequests":1,"windowSeconds":60}
                """);
        given(capabilityConfigMapper.findByCapabilityCode("chat")).willReturn(capabilityConfig);
        GatewayRequest request = GatewayRequest.builder()
                .userId(1001L)
                .capabilityCode("chat")
                .sceneCode("chat.general")
                .build();

        policy.validate(request);
        GatewayPolicyException exception = assertThrows(GatewayPolicyException.class, () -> policy.validate(request));

        assertEquals("RATE_LIMITED", exception.getError().getErrorCode());
    }
}
