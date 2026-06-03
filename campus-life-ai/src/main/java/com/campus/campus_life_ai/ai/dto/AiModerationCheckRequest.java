package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiModerationCheckRequest {

    @NotBlank(message = "审核内容不能为空")
    @Size(max = 5000, message = "审核内容长度不能超过5000")
    private String content;

    @Size(max = 32, message = "内容类型长度不能超过32")
    private String targetType;

    @Size(max = 64, message = "场景编码长度不能超过64")
    private String sceneCode;

    @Size(max = 32, message = "供应商编码长度不能超过32")
    private String providerCode;

    @Size(max = 64, message = "模型编码长度不能超过64")
    private String modelCode;
}
