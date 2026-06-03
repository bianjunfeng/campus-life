package com.campus.campus_life_ai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiProviderDescriptorDTO {
    private String providerCode;
    private String providerName;
    private boolean enabled;
    private String defaultModelCode;
    private String baseUrl;
}
