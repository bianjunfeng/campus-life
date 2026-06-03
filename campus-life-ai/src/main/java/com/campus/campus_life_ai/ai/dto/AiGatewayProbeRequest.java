package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiGatewayProbeRequest {
    @NotBlank(message = "测试内容不能为空")
    @Size(max = 2000, message = "测试内容长度不能超过2000")
    private String content;

    @Size(max = 32, message = "能力编码长度不能超过32")
    private String capabilityCode;

    @Size(max = 64, message = "场景编码长度不能超过64")
    private String sceneCode;

    @Size(max = 32, message = "供应商编码长度不能超过32")
    private String providerCode;

    @Size(max = 64, message = "模型编码长度不能超过64")
    private String modelCode;

    @DecimalMin(value = "0.0", message = "temperature 不能小于0")
    @DecimalMax(value = "2.0", message = "temperature 不能大于2")
    private Double temperature;

    @Min(value = 1, message = "最大输出 token 不能小于1")
    private Integer maxOutputTokens;
}
