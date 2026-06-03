package com.campus.campus_life_ai.ai.gateway.dto;

@FunctionalInterface
public interface GatewayStreamConsumer {

    void onDelta(String content);
}
