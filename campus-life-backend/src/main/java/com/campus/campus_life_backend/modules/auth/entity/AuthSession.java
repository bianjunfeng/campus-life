package com.campus.campus_life_backend.modules.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthSession {

    private Long id;
    private String sessionId;
    private Long userId;
    private String accessJti;
    private String refreshJti;
    private String accessTokenHash;
    private String refreshTokenHash;
    private String device;
    private String ip;
    private String userAgent;
    private Integer status;
    private LocalDateTime loginTime;
    private LocalDateTime lastSeenTime;
    private LocalDateTime logoutTime;
    private LocalDateTime expireAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
