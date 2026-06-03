package com.campus.campus_life_backend.modules.auth.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 登录Token实体类
 */
@Data
public class LoginToken {

    private Long id;

    private Long userId;

    private String token;

    private String device;

    private String ip;

    private Integer status;  // 1-有效;0-已失效

    private LocalDateTime expireAt;

    private LocalDateTime createTime;
}


