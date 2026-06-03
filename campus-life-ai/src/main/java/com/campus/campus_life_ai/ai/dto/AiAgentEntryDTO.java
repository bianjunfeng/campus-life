package com.campus.campus_life_ai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentEntryDTO {
    private String code;
    private String name;
    private String description;
    private String frontendPath;
    private String backendPath;
    private String sceneCode;
}
