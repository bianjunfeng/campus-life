package com.campus.campus_life_backend.modules.message.event;

import lombok.Data;

@Data
public class SystemNotificationEvent {
    private String eventId;
    private String type;
    private Long targetUserId;
    private Long actorUserId;
    private Long postId;
    private Long commentId;
    private String message;
    private Long createEpochMillis;
}
