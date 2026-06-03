package com.campus.campus_life_backend.modules.message.service;

import com.campus.campus_life_backend.modules.message.event.SystemNotificationEvent;
import com.campus.campus_life_backend.modules.message.mapper.MessageNotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class SystemNotificationEventHandler {

    private final MessageNotificationMapper messageNotificationMapper;

    public SystemNotificationEventHandler(MessageNotificationMapper messageNotificationMapper) {
        this.messageNotificationMapper = messageNotificationMapper;
    }

    @Transactional
    public void handle(SystemNotificationEvent event) {
        if (event == null
                || event.getEventId() == null
                || event.getTargetUserId() == null
                || event.getActorUserId() == null
                || event.getType() == null) {
            return;
        }
        if (event.getTargetUserId().equals(event.getActorUserId())) {
            return;
        }
        LocalDateTime createTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(event.getCreateEpochMillis() == null ? System.currentTimeMillis() : event.getCreateEpochMillis()),
                ZoneId.systemDefault()
        );
        messageNotificationMapper.insertSystemNotification(
                event.getEventId(),
                event.getType(),
                event.getTargetUserId(),
                event.getActorUserId(),
                event.getPostId(),
                event.getCommentId(),
                event.getMessage(),
                createTime
        );
    }
}
