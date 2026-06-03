package com.campus.campus_life_backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "登录账号不能为空")
    private String phone;
    
    private String password;
    
    private String code;

    @Pattern(
            regexp = "^(register|login|reset_password|bind_phone)$",
            message = "验证码场景不正确"
    )
    private String scene;
}
