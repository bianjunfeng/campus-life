package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.gateway.GatewayErrorCode;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class GatewaySwitchPolicy implements GatewayPolicy {

    private final AiProviderProperties aiProviderProperties;

    public GatewaySwitchPolicy(AiProviderProperties aiProviderProperties) {
        this.aiProviderProperties = aiProviderProperties;
    }

    @Override
    public void validate(GatewayRequest request) {
        if (!aiProviderProperties.getGateway().isEnabled()) {
            throw new GatewayPolicyException(GatewayErrorCode.GATEWAY_DISABLED, "大模型网关未启用");
        }
    }
}
