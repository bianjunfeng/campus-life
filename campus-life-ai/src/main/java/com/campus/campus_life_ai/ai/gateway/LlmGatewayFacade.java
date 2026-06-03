package com.campus.campus_life_ai.ai.gateway;

import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayStreamConsumer;

public interface LlmGatewayFacade {

    GatewayResponse chat(GatewayRequest request);

    GatewayResponse stream(GatewayRequest request, GatewayStreamConsumer consumer);
}
