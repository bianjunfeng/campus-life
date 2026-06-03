package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiGatewayRouteRuleUpsertRequest {
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称长度不能超过128")
    private String ruleName;

    @NotBlank(message = "能力编码不能为空")
    @Size(max = 32, message = "能力编码长度不能超过32")
    @Pattern(regexp = "^[a-z0-9._-]+$", message = "能力编码格式不合法")
    private String capabilityCode;

    @Size(max = 64, message = "场景编码长度不能超过64")
    private String sceneCode;

    @Size(max = 4000, message = "匹配规则 JSON 长度不能超过4000")
    private String matchRuleJson;

    @NotBlank(message = "路由规则 JSON 不能为空")
    @Size(max = 8000, message = "路由规则 JSON 长度不能超过8000")
    private String routeRuleJson;

    private Boolean enabled;

    @Min(value = 0, message = "优先级不能小于0")
    private Integer priority;
}
