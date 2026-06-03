package com.campus.campus_life_ai.ai.gateway;

import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;

public class GatewayCallException extends RuntimeException {

    private final GatewayError error;

    public GatewayCallException(GatewayError error, Throwable cause) {
        super(error == null ? null : error.getMessage(), cause);
        this.error = error;
    }

    public GatewayError getError() {
        return error;
    }
}
