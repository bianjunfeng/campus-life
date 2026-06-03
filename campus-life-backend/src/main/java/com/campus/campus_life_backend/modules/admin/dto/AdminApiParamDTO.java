package com.campus.campus_life_backend.modules.admin.dto;

import lombok.Data;

@Data
public class AdminApiParamDTO {
    private String name;
    private String type;
    private boolean required;
    private String description;
}
