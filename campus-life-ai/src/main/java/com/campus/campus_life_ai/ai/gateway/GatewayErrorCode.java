package com.campus.campus_life_ai.ai.gateway;

public enum GatewayErrorCode {
    INVALID_REQUEST("INVALID_REQUEST", "REQUEST_VALIDATION"),
    GATEWAY_DISABLED("GATEWAY_DISABLED", "POLICY_REJECTED"),
    CAPABILITY_DISABLED("CAPABILITY_DISABLED", "POLICY_REJECTED"),
    SCENE_DISABLED("SCENE_DISABLED", "POLICY_REJECTED"),
    RATE_LIMITED("RATE_LIMITED", "RATE_LIMITED"),
    QUOTA_EXCEEDED("QUOTA_EXCEEDED", "QUOTA_EXCEEDED"),
    SAFETY_BLOCKED("SAFETY_BLOCKED", "SAFETY"),
    ROUTE_UNAVAILABLE("ROUTE_UNAVAILABLE", "ROUTING"),
    PROVIDER_UNAVAILABLE("PROVIDER_UNAVAILABLE", "PROVIDER_ERROR"),
    PROVIDER_TIMEOUT("PROVIDER_TIMEOUT", "TIMEOUT"),
    CLIENT_ABORTED("CLIENT_ABORTED", "CLIENT_ABORTED"),
    PROVIDER_ERROR("PROVIDER_ERROR", "PROVIDER_ERROR");

    private final String code;
    private final String type;

    GatewayErrorCode(String code, String type) {
        this.code = code;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}
