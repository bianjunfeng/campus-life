package com.campus.campus_life_ai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiSceneDescriptorDTO {
    private String capabilityCode;
    private String sceneCode;
    private String sceneName;
    private boolean enabled;
}
