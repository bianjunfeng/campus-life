package com.campus.campus_life_backend.modules.search.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserSearchSourceDTO {
    private Long id;
    private String username;
    private String avatarUrl;
    private String bio;
    private String phone;
    private Integer role;
    private Integer status;
    private Integer postCount;
    private Integer followerCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
