package com.campus.campus_life_backend.modules.message.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationItemDTO {
    private String id;
    private String type;
    private Long actorUserId;
    private String actorUserName;
    private String actorUserAvatar;
    private String message;
    private LocalDateTime createTime;
    private Long postId;
    private Boolean unread;
}

