package com.campus.campus_life_backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterAppRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
    private String code;

    @Pattern(
            regexp = "^(register|login|reset_password|bind_phone)$",
            message = "验证码场景不正确"
    )
    private String scene;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 20, message = "昵称长度必须在2-20个字符之间")
    private String nickName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$",
            message = "密码必须包含字母和数字，长度6-20位")
    private String password;

    // 学生注册字段
    private String school;
    private String studentId;
    private String college;
    private String major;
    private String grade;
    private String className;
    private String realName;

    // 商家注册字段
    private String merchantName;
    private Long merchantTypeId;
    private String contactName;
    private String contactPhone;
    private String address;
}
