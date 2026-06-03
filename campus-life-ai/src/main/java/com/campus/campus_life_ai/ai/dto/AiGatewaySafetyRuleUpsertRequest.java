package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiGatewaySafetyRuleUpsertRequest {
    @NotBlank(message = "安全规则名称不能为空")
    @Size(max = 128, message = "安全规则名称长度不能超过128")
    private String ruleName;

    @Size(max = 32, message = "能力编码长度不能超过32")
    private String capabilityCode;

    @Size(max = 64, message = "场景编码长度不能超过64")
    private String sceneCode;

    @NotBlank(message = "规则方向不能为空")
    @Size(max = 16, message = "规则方向长度不能超过16")
    private String direction;

    @NotBlank(message = "规则动作不能为空")
    @Size(max = 16, message = "规则动作长度不能超过16")
    private String action;

    @NotBlank(message = "匹配类型不能为空")
    @Size(max = 16, message = "匹配类型长度不能超过16")
    private String matchType;

    @NotBlank(message = "匹配内容不能为空")
    @Size(max = 512, message = "匹配内容长度不能超过512")
    private String patternText;

    @NotBlank(message = "安全分类不能为空")
    @Size(max = 64, message = "安全分类长度不能超过64")
    private String category;

    @NotBlank(message = "风险等级不能为空")
    @Size(max = 16, message = "风险等级长度不能超过16")
    private String severity;

    private Boolean enabled;

    private Integer priority;
}
