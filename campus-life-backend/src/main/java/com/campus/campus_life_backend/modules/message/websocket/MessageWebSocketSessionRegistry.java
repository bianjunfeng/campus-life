package com.campus.campus_life_backend.modules.message.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class MessageWebSocketSessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(MessageWebSocketSessionRegistry.class);

    private final ConcurrentMap<Long, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> userIdBySessionId = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        sessionsByUserId.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        sessionsById.put(session.getId(), session);
        userIdBySessionId.put(session.getId(), userId);
    }

    public void unregister(WebSocketSession session) {
        if (session == null) {
            return;
        }
        String sessionId = session.getId();
        sessionsById.remove(sessionId);
        Long userId = userIdBySessionId.remove(sessionId);
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.removeIf(item -> sessionId.equals(item.getId()) || !item.isOpen());
        if (sessions.isEmpty()) {
            sessionsByUserId.remove(userId, sessions);
        }
    }

    public void sendToSession(String sessionId, String payload) {
        WebSocketSession session = sessionsById.get(sessionId);
        if (session != null) {
            send(session, payload);
        }
    }

    public void sendToUser(Long userId, String payload) {
        sendToUserExcept(userId, null, payload);
    }

    public void sendToUserExcept(Long userId, String excludedSessionId, String payload) {
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            if (excludedSessionId != null && excludedSessionId.equals(session.getId())) {
                continue;
            }
            send(session, payload);
        }
    }

    private void send(WebSocketSession session, String payload) {
        if (session == null || !session.isOpen()) {
            unregister(session);
            return;
        }
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (IOException ex) {
            logger.warn("发送WebSocket消息失败，sessionId={}", session.getId(), ex);
            unregister(session);
        }
    }
}
