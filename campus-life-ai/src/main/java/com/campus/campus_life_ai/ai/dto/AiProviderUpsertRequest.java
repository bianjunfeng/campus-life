package com.campus.campus_life_ai.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiProviderUpsertRequest {
    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 32, message = "供应商编码长度不能超过32")
    @Pattern(regexp = "^[a-z0-9._-]+$", message = "供应商编码格式不合法")
    private String providerCode;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 64, message = "供应商名称长度不能超过64")
    private String providerName;

    @NotBlank(message = "基础地址不能为空")
    @Size(max = 255, message = "基础地址长度不能超过255")
    private String baseUrl;

    @Size(max = 512, message = "API Key 长度不能超过512")
    private String apiKeyCipher;

    @NotBlank(message = "默认模型编码不能为空")
    @Size(max = 64, message = "默认模型编码长度不能超过64")
    private String defaultModelCode;

    private Boolean enabled;

    @Min(value = 1000, message = "超时时间不能小于1000毫秒")
    private Integer timeoutMs;

    @Min(value = 1, message = "上下文消息数不能小于1")
    private Integer maxContextMessages;

    @DecimalMin(value = "0.0", message = "temperature 不能小于0")
    @DecimalMax(value = "2.0", message = "temperature 不能大于2")
    private Double temperature;

    @DecimalMin(value = "0.0", message = "topP 不能小于0")
    @DecimalMax(value = "1.0", message = "topP 不能大于1")
    private Double topP;

    @Min(value = 1, message = "最大输出 token 不能小于1")
    private Integer maxOutputTokens;

    @Size(max = 4000, message = "系统提示词模板长度不能超过4000")
    private String systemPromptTemplate;
}
