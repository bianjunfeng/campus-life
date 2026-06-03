package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiCapabilityUpsertRequest {
    @NotBlank(message = "能力编码不能为空")
    @Size(max = 32, message = "能力编码长度不能超过32")
    @Pattern(regexp = "^[a-z0-9._-]+$", message = "能力编码格式不合法")
    private String capabilityCode;

    @NotBlank(message = "能力名称不能为空")
    @Size(max = 64, message = "能力名称长度不能超过64")
    private String capabilityName;

    private Boolean enabled;

    private Boolean grayEnabled;

    @Size(max = 4000, message = "灰度规则长度不能超过4000")
    private String grayRuleJson;

    @Size(max = 4000, message = "限流规则长度不能超过4000")
    private String rateLimitJson;

    @Size(max = 4000, message = "配额规则长度不能超过4000")
    private String quotaRuleJson;
}
