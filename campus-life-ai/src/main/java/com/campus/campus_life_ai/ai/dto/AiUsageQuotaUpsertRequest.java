package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiUsageQuotaUpsertRequest {
    @NotBlank(message = "配额主体类型不能为空")
    @Size(max = 16, message = "配额主体类型长度不能超过16")
    private String subjectType;

    @Size(max = 64, message = "配额主体长度不能超过64")
    private String subjectId;

    @Size(max = 32, message = "能力编码长度不能超过32")
    private String capabilityCode;

    @Size(max = 64, message = "场景编码长度不能超过64")
    private String sceneCode;

    @NotBlank(message = "配额周期不能为空")
    @Size(max = 16, message = "配额周期长度不能超过16")
    private String quotaPeriod;

    @Min(value = 1, message = "最大调用数必须大于0")
    private Integer maxCalls;

    @Min(value = 1, message = "最大 Token 数必须大于0")
    private Integer maxTokens;

    @DecimalMin(value = "0.0001", message = "最大成本必须大于0")
    private BigDecimal maxCost;

    private Boolean enabled;
}
