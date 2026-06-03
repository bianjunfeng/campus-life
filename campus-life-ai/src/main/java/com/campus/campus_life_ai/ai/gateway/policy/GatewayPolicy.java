package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;

public interface GatewayPolicy {

    void validate(GatewayRequest request);
}
