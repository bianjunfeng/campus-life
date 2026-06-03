package com.campus.campus_life_backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendVerificationCodeRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码场景不能为空")
    @Pattern(
            regexp = "^(register|login|reset_password|bind_phone)$",
            message = "验证码场景不正确"
    )
    private String scene;
}


