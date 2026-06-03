package com.campus.campus_life_ai.ai.gateway.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GatewayError {
    String errorCode;
    String errorType;
    String message;
}
