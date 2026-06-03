package com.campus.campus_life_backend.modules.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginAuditLog {

    private Long id;
    private Long userId;
    private String account;
    private Integer role;
    private String loginType;
    private Integer success;
    private String failureReason;
    private String ip;
    private String userAgent;
    private String sessionId;
    private LocalDateTime createTime;
}
