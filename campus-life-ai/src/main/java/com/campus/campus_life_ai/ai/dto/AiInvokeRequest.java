package com.campus.campus_life_ai.ai.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AiInvokeRequest {
    private String capabilityCode;
    private String sceneCode;
    private String providerCode;
    private String modelCode;
    private Map<String, Object> input;
    private Map<String, Object> options;
}
