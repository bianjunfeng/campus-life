package com.campus.campus_life_backend.modules.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * ClassName: User
 * Description: 用户实体类
 *
 * @Author Junfeng Bian
 * @Create 2025/11/29 20:18
 * @Version 1.0
 */
@Data
public class User {

    private Long id;

    private String phone;

    private String wechatOpenid;  // 微信OpenID

    private String qqOpenid;  // QQ OpenID

    private String email;

    private String username;

    private String passwordHash;

    private String salt;

    private Integer role;  // 0-学生;1-商家;2-管理员

    private Integer status;  // 0-禁用;1-正常

    private String avatarUrl;

    private String bio;

    private Integer gender;  // 性别：0-保密;1-男;2-女

    private java.time.LocalDate birthday;  // 生日

    private String region;  // 地区（省份/直辖市）

    private String occupation;  // 职业

    private Integer isStudentVerified;  // 0-未认证;1-已认证

    private Integer isMerchant;  // 0-否;1-是

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}


