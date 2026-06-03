package com.campus.campus_life_ai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiCapabilityDescriptorDTO {
    private String capabilityCode;
    private String capabilityName;
    private boolean enabled;
    private String status;
}
