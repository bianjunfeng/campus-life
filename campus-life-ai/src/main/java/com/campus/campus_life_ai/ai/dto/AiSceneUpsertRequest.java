package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiSceneUpsertRequest {
    @NotBlank(message = "能力编码不能为空")
    @Size(max = 32, message = "能力编码长度不能超过32")
    @Pattern(regexp = "^[a-z0-9._-]+$", message = "能力编码格式不合法")
    private String capabilityCode;

    @NotBlank(message = "场景编码不能为空")
    @Size(max = 64, message = "场景编码长度不能超过64")
    @Pattern(regexp = "^[a-z0-9._-]+$", message = "场景编码格式不合法")
    private String sceneCode;

    @NotBlank(message = "场景名称不能为空")
    @Size(max = 128, message = "场景名称长度不能超过128")
    private String sceneName;

    @Size(max = 32, message = "供应商编码长度不能超过32")
    private String providerCode;

    @Size(max = 64, message = "模型编码长度不能超过64")
    private String modelCode;

    private Boolean enabled;

    @Size(max = 4000, message = "系统提示词模板长度不能超过4000")
    private String systemPromptTemplate;

    @Size(max = 4000, message = "输入结构长度不能超过4000")
    private String inputSchemaJson;

    @Size(max = 4000, message = "输出结构长度不能超过4000")
    private String outputSchemaJson;

    @Size(max = 16, message = "安全等级长度不能超过16")
    private String safetyLevel;

    @Min(value = 1000, message = "超时时间不能小于1000毫秒")
    private Integer timeoutMs;
}
