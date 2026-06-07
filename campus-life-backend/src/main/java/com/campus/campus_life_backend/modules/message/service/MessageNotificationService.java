package com.campus.campus_life_backend.modules.message.service;

import com.campus.campus_life_backend.modules.message.dto.NotificationItemDTO;
import com.campus.campus_life_backend.modules.message.mapper.MessageNotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MessageNotificationService {

    public static final String CATEGORY_LIKES_FAVORITES = "likes-favorites";
    public static final String CATEGORY_COMMENTS = "comments";
    public static final String CATEGORY_FOLLOWS = "follows";
    public static final String CATEGORY_SYSTEM = "system";
    public static final String CATEGORY_ALL = "all";

    private static final Set<String> SUPPORTED_CATEGORIES = Set.of(
            CATEGORY_LIKES_FAVORITES,
            CATEGORY_COMMENTS,
            CATEGORY_FOLLOWS,
            CATEGORY_SYSTEM,
            CATEGORY_ALL
    );

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
    public int markNotificationsAsRead(Long userId, String category) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        String normalizedCategory = normalizeCategory(category);
        return messageNotificationMapper.markNotificationsAsRead(userId, normalizedCategory);
    }

    @Transactional
    public int markNotificationAsRead(Long userId, Long notificationId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (notificationId == null || notificationId <= 0) {
            throw new IllegalArgumentException("通知ID无效");
        }
        return messageNotificationMapper.markNotificationAsRead(userId, notificationId);
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

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("通知分类不能为空");
        }
        String normalizedCategory = category.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_CATEGORIES.contains(normalizedCategory)) {
            throw new IllegalArgumentException("通知分类不支持");
        }
        return normalizedCategory;
    }
}
