package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.gateway.GatewayCallException;
import com.campus.campus_life_ai.ai.gateway.GatewayErrorCode;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;

public class GatewayPolicyException extends GatewayCallException {

    public GatewayPolicyException(GatewayErrorCode errorCode, String message) {
        super(GatewayError.builder()
                .errorCode(errorCode.getCode())
                .errorType(errorCode.getType())
                .message(message)
                .build(), null);
    }
}
