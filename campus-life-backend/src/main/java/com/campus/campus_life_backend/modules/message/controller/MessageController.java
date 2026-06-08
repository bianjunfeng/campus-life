package com.campus.campus_life_backend.modules.message.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.message.dto.ConversationDTO;
import com.campus.campus_life_backend.modules.message.dto.MarkNotificationsReadRequest;
import com.campus.campus_life_backend.modules.message.dto.MessageDTO;
import com.campus.campus_life_backend.modules.message.dto.MessageWebSocketTicketResponse;
import com.campus.campus_life_backend.modules.message.dto.NotificationItemDTO;
import com.campus.campus_life_backend.modules.message.service.MessageNotificationService;
import com.campus.campus_life_backend.modules.message.service.MessageSendService;
import com.campus.campus_life_backend.modules.message.service.MessageService;
import com.campus.campus_life_backend.modules.message.websocket.MessageWebSocketTicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
@RequirePermission(anyOf = {"message:use"})
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final CurrentUserAccessor currentUserAccessor;
    private final MessageService messageService;
    private final MessageSendService messageSendService;
    private final MessageNotificationService messageNotificationService;
    private final MessageWebSocketTicketService webSocketTicketService;

    public MessageController(CurrentUserAccessor currentUserAccessor,
                             MessageService messageService,
                             MessageSendService messageSendService,
                             MessageNotificationService messageNotificationService,
                             MessageWebSocketTicketService webSocketTicketService) {
        this.currentUserAccessor = currentUserAccessor;
        this.messageService = messageService;
        this.messageSendService = messageSendService;
        this.messageNotificationService = messageNotificationService;
        this.webSocketTicketService = webSocketTicketService;
    }

    @PostMapping("/send")
    public ApiResponse<MessageDTO> sendMessage(@RequestBody Map<String, Object> request) {
        Long fromUserId = currentUserAccessor.requireUserId();
        Long toUserId = parseLong(request.get("toUserId"));
        String content = (String) request.get("content");
        if (toUserId == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "接收方用户ID无效");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "消息内容不能为空");
        }

        try {
            MessageDTO message = messageSendService.sendMessage(fromUserId, toUserId, content);
            return ApiResponse.success(message);
        } catch (BusinessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("发送消息失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "发送消息失败，请稍后重试", e);
        }
    }

    @PostMapping("/ws-ticket")
    public ApiResponse<MessageWebSocketTicketResponse> createWebSocketTicket() {
        String ticket = webSocketTicketService.createTicket(currentUserAccessor.requirePrincipal());
        return ApiResponse.success(new MessageWebSocketTicketResponse(
                ticket,
                MessageWebSocketTicketService.EXPIRES_IN_SECONDS
        ));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationDTO>> getConversations() {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(messageService.getConversationsByUserId(userId));
    }

    @GetMapping("/conversation/{conversationId}/messages")
    public ApiResponse<Map<String, Object>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            List<MessageDTO> messages = messageService.getMessagesByConversationId(conversationId, userId, page, pageSize);
            Map<String, Object> result = new HashMap<>();
            result.put("messages", messages);
            result.put("page", page);
            result.put("pageSize", pageSize);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    @GetMapping("/conversation/{conversationId}/messages/sync")
    public ApiResponse<Map<String, Object>> syncMessages(
            @PathVariable String conversationId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Long afterId,
            @RequestParam(defaultValue = "100") Integer limit) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            List<MessageDTO> messages = messageService.syncMessagesByConversationId(conversationId, userId, beforeId, afterId, limit);
            Map<String, Object> result = new HashMap<>();
            result.put("conversationId", conversationId);
            result.put("messages", messages);
            result.put("beforeId", beforeId);
            result.put("afterId", afterId);
            result.put("limit", normalizeLimit(limit));
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    @GetMapping("/user/{otherUserId}/messages")
    public ApiResponse<Map<String, Object>> getMessagesByUserId(
            @PathVariable Long otherUserId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            String conversationId = messageService.getOrCreateConversationId(userId, otherUserId);
            List<MessageDTO> messages = messageService.getMessagesByUserId(userId, otherUserId, page, pageSize);

            Map<String, Object> result = new HashMap<>();
            result.put("conversationId", conversationId);
            result.put("messages", messages);
            result.put("page", page);
            result.put("pageSize", pageSize);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    @GetMapping("/user/{otherUserId}/messages/sync")
    public ApiResponse<Map<String, Object>> syncMessagesByUserId(
            @PathVariable Long otherUserId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Long afterId,
            @RequestParam(defaultValue = "100") Integer limit) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            String conversationId = messageService.getOrCreateConversationId(userId, otherUserId);
            List<MessageDTO> messages = messageService.syncMessagesByUserId(userId, otherUserId, beforeId, afterId, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("conversationId", conversationId);
            result.put("messages", messages);
            result.put("beforeId", beforeId);
            result.put("afterId", afterId);
            result.put("limit", normalizeLimit(limit));
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    @PutMapping("/conversation/{conversationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable String conversationId) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            messageService.markConversationAsRead(conversationId, userId);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> getUnreadCount() {
        Long userId = currentUserAccessor.requireUserId();
        int unreadCount = messageService.getUnreadCount(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", unreadCount);
        return ApiResponse.success(result);
    }

    @GetMapping("/notifications/likes-favorites")
    public ApiResponse<List<NotificationItemDTO>> getLikeAndFavoriteNotifications() {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(messageNotificationService.getLikeAndFavoriteNotifications(userId));
    }

    @GetMapping("/notifications/comments")
    public ApiResponse<List<NotificationItemDTO>> getCommentNotifications() {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(messageNotificationService.getCommentNotifications(userId));
    }

    @GetMapping("/notifications/follows")
    public ApiResponse<List<NotificationItemDTO>> getFollowNotifications() {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(messageNotificationService.getFollowNotifications(userId));
    }

    @GetMapping("/notifications/system")
    public ApiResponse<List<NotificationItemDTO>> getSystemNotifications() {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(messageNotificationService.getSystemNotifications(userId));
    }

    @PutMapping("/notifications/read")
    public ApiResponse<Map<String, Object>> markNotificationsAsRead(@RequestBody(required = false) MarkNotificationsReadRequest request) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            int updatedCount = messageNotificationService.markNotificationsAsRead(userId, request == null ? null : request.getCategory());
            Map<String, Object> result = new HashMap<>();
            result.put("updatedCount", updatedCount);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    @PutMapping("/notifications/{notificationId}/read")
    public ApiResponse<Map<String, Object>> markNotificationAsRead(@PathVariable String notificationId) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            int updatedCount = messageNotificationService.markNotificationAsRead(userId, parseNotificationId(notificationId));
            Map<String, Object> result = new HashMap<>();
            result.put("updatedCount", updatedCount);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseNotificationId(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.startsWith("sys_") ? value.substring(4) : value;
        return parseLong(normalizedValue);
    }

    private int normalizeLimit(Integer limit) {
        return limit == null || limit < 1 ? 100 : Math.min(limit, 200);
    }
}
