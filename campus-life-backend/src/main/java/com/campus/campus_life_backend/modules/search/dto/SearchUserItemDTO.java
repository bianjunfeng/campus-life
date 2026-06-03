package com.campus.campus_life_backend.modules.search.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchUserItemDTO {
    private Long id;
    private String username;
    private String avatarUrl;
    private String bio;
    private Integer role;
    private Integer postCount;
    private Integer followerCount;
    private LocalDateTime createTime;
}

