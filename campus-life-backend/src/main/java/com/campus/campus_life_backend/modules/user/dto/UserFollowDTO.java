package com.campus.campus_life_backend.modules.user.dto;

import lombok.Data;

/**
 * 用户关注关系DTO，包含用户信息
 */
@Data
public class UserFollowDTO {
    private Long id;
    private Long userId;           // 用户ID（关注者或被关注者）
    private String name;           // 用户名
    private String avatar;         // 头像
    private String bio;            // 简介
    private Long followeeId;       // 被关注者ID（用于关注列表）
    private Long followerId;       // 关注者ID（用于粉丝列表）
    private Boolean isFollowing;   // 当前登录用户是否关注了该用户（用于粉丝列表）
    private Boolean isMutual;      // 是否互相关注
}


