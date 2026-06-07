package com.campus.campus_life_backend.modules.message.websocket;

import com.campus.campus_life_backend.modules.message.dto.MessageDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MessageRealtimeNotifier {

    private static final Logger logger = LoggerFactory.getLogger(MessageRealtimeNotifier.class);

    private final MessageWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public MessageRealtimeNotifier(MessageWebSocketSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    public void publishChatMessage(MessageDTO message,
                                   @Nullable String sourceSessionId,
                                   @Nullable String clientMessageId) {
        if (message == null) {
            return;
        }

        if (sourceSessionId != null) {
            Map<String, Object> ack = new LinkedHashMap<>();
            ack.put("type", "chat.ack");
            ack.put("clientMessageId", clientMessageId);
            ack.put("message", message);
            sessionRegistry.sendToSession(sourceSessionId, toJson(ack));
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "chat.message");
        event.put("message", message);
        String payload = toJson(event);

        sessionRegistry.sendToUser(message.getToUserId(), payload);
        if (sourceSessionId == null) {
            sessionRegistry.sendToUser(message.getFromUserId(), payload);
        } else {
            sessionRegistry.sendToUserExcept(message.getFromUserId(), sourceSessionId, payload);
        }
    }

    public void sendPong(WebSocketSession session) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "pong");
        payload.put("serverTime", Instant.now().toEpochMilli());
        sessionRegistry.sendToSession(session.getId(), toJson(payload));
    }

    public void sendError(WebSocketSession session,
                          @Nullable String clientMessageId,
                          String code,
                          String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "chat.error");
        payload.put("clientMessageId", clientMessageId);
        payload.put("code", code);
        payload.put("message", message);
        sessionRegistry.sendToSession(session.getId(), toJson(payload));
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            logger.warn("序列化WebSocket消息失败", ex);
            return "{\"type\":\"chat.error\",\"code\":\"INTERNAL_ERROR\",\"message\":\"消息序列化失败\"}";
        }
    }
}
