package com.campus.campus_life_backend.modules.message.mapper;

import com.campus.campus_life_backend.modules.message.dto.NotificationItemDTO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageNotificationMapper {

    List<NotificationItemDTO> findLikeAndFavoriteNotifications(@Param("userId") Long userId);

    List<NotificationItemDTO> findCommentNotifications(@Param("userId") Long userId);

    List<NotificationItemDTO> findFollowNotifications(@Param("userId") Long userId);

    List<NotificationItemDTO> findSystemNotifications(@Param("userId") Long userId);

    int insertSystemNotification(
            @Param("eventId") String eventId,
            @Param("type") String type,
            @Param("targetUserId") Long targetUserId,
            @Param("actorUserId") Long actorUserId,
            @Param("postId") Long postId,
            @Param("commentId") Long commentId,
            @Param("message") String message,
            @Param("createTime") LocalDateTime createTime
    );
}
