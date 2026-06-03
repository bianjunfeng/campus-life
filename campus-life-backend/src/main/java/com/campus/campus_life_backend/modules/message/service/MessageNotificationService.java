package com.campus.campus_life_backend.modules.message.service;

import com.campus.campus_life_backend.modules.message.dto.NotificationItemDTO;
import com.campus.campus_life_backend.modules.message.mapper.MessageNotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageNotificationService {

    private final MessageNotificationMapper messageNotificationMapper;

    public MessageNotificationService(MessageNotificationMapper messageNotificationMapper) {
        this.messageNotificationMapper = messageNotificationMapper;
    }

    public List<NotificationItemDTO> getLikeAndFavoriteNotifications(Long userId) {
        return messageNotificationMapper.findLikeAndFavoriteNotifications(userId);
    }

    public List<NotificationItemDTO> getCommentNotifications(Long userId) {
        return messageNotificationMapper.findCommentNotifications(userId);
    }

    public List<NotificationItemDTO> getFollowNotifications(Long userId) {
        return messageNotificationMapper.findFollowNotifications(userId);
    }

    public List<NotificationItemDTO> getSystemNotifications(Long userId) {
        return messageNotificationMapper.findSystemNotifications(userId);
    }

    @Transactional
    public void createSystemNotification(
            String eventId,
            String type,
            Long targetUserId,
            Long actorUserId,
            String message
    ) {
        if (eventId == null || eventId.isBlank() || targetUserId == null || actorUserId == null || type == null || type.isBlank()) {
            return;
        }
        messageNotificationMapper.insertSystemNotification(
                eventId,
                type,
                targetUserId,
                actorUserId,
                null,
                null,
                message,
                LocalDateTime.now()
        );
    }
}

