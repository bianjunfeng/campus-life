package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiModelUpsertRequest {
    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 32, message = "供应商编码长度不能超过32")
    @Pattern(regexp = "^[a-z0-9._-]+$", message = "供应商编码格式不合法")
    private String providerCode;

    @NotBlank(message = "模型编码不能为空")
    @Size(max = 64, message = "模型编码长度不能超过64")
    private String modelCode;

    @NotBlank(message = "模型名称不能为空")
    @Size(max = 128, message = "模型名称长度不能超过128")
    private String modelName;

    @Size(max = 4000, message = "模型能力 JSON 长度不能超过4000")
    private String capabilitiesJson;

    @Min(value = 1, message = "上下文窗口不能小于1")
    private Integer contextWindow;

    @Min(value = 1, message = "最大输出 token 不能小于1")
    private Integer maxOutputTokens;

    @DecimalMin(value = "0.0", message = "输入价格不能小于0")
    private BigDecimal inputPricePer1k;

    @DecimalMin(value = "0.0", message = "输出价格不能小于0")
    private BigDecimal outputPricePer1k;

    private Boolean enabled;

    @Min(value = 0, message = "优先级不能小于0")
    private Integer priority;
}
