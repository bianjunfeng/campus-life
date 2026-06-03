package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.gateway.GatewayErrorCode;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(35)
public class SafetyModerationPolicy implements GatewayPolicy {

    private final GatewaySafetyInspector gatewaySafetyInspector;

    public SafetyModerationPolicy(GatewaySafetyInspector gatewaySafetyInspector) {
        this.gatewaySafetyInspector = gatewaySafetyInspector;
    }

    @Override
    public void validate(GatewayRequest request) {
        GatewaySafetyInspector.SafetyDecision decision = gatewaySafetyInspector.inspectInput(request);
        if (decision != null && decision.blocked()) {
            throw new GatewayPolicyException(GatewayErrorCode.SAFETY_BLOCKED, decision.message());
        }
    }
}
