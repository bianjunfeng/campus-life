package com.campus.campus_life_backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    private String username;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String email; // 邮箱可选

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$", 
             message = "密码必须包含字母和数字，长度6-20位")
    private String password;

    // 验证码（注册时必填）
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
    private String code;

    @Pattern(
            regexp = "^(register|login|reset_password|bind_phone)$",
            message = "验证码场景不正确"
    )
    private String scene;

    // 学生信息（学生注册时必填）
    private String school;      // 学校名称
    private String studentNo;    // 学号
    private String college;      // 学院（可选）
    private String major;        // 专业（可选）
    private String grade;        // 年级（可选）
    private String className;    // 班级（可选）
    private String realName;     // 真实姓名
    
    // 商家信息（商家注册时必填）
    private String merchantName;  // 商家名称
    private Long merchantTypeId;  // 商家类型ID
    private String contactName;   // 联系人姓名
    private String contactPhone;  // 联系电话
    private String address;       // 详细地址
}


