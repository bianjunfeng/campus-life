package com.campus.campus_life_backend.modules.admin.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminApiInfoDTO {
    private String path;
    private String method;
    private String description;
    private String module;
    private List<AdminApiParamDTO> params = new ArrayList<>();
}
