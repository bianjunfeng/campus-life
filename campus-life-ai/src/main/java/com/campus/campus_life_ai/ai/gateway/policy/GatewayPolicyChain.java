package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GatewayPolicyChain {

    private final List<GatewayPolicy> policies;

    public GatewayPolicyChain(List<GatewayPolicy> policies) {
        this.policies = policies;
    }

    public void validate(GatewayRequest request) {
        for (GatewayPolicy policy : policies) {
            policy.validate(request);
        }
    }
}
