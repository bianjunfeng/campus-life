package com.campus.campus_life_backend.modules.message.websocket;

import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.security.exception.PermissionDeniedException;
import com.campus.campus_life_backend.common.security.exception.UnauthenticatedException;
import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.model.PermissionCode;
import com.campus.campus_life_backend.modules.message.service.MessageSendService;
import com.campus.campus_life_backend.modules.presence.service.PresenceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final MessageSendService messageSendService;
    private final MessageRealtimeNotifier realtimeNotifier;
    private final MessageWebSocketSessionRegistry sessionRegistry;
    private final PresenceService presenceService;

    public MessageWebSocketHandler(ObjectMapper objectMapper,
                                   MessageSendService messageSendService,
                                   MessageRealtimeNotifier realtimeNotifier,
                                   MessageWebSocketSessionRegistry sessionRegistry,
                                   PresenceService presenceService) {
        this.objectMapper = objectMapper;
        this.messageSendService = messageSendService;
        this.realtimeNotifier = realtimeNotifier;
        this.sessionRegistry = sessionRegistry;
        this.presenceService = presenceService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LoginPrincipal principal = resolvePrincipal(session);
        if (principal == null || principal.getUserId() == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        sessionRegistry.register(principal.getUserId(), session);
        markPresence(session, principal);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String clientMessageId = null;
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = text(root, "type");
            clientMessageId = text(root, "clientMessageId");

            if ("ping".equals(type)) {
                LoginPrincipal principal = resolvePrincipal(session);
                if (principal != null) {
                    markPresence(session, principal);
                }
                realtimeNotifier.sendPong(session);
                return;
            }
            if (!"chat.send".equals(type)) {
                realtimeNotifier.sendError(session, clientMessageId, "UNSUPPORTED_TYPE", "不支持的消息类型");
                return;
            }

            Long toUserId = longValue(root, "toUserId");
            String content = text(root, "content");
            LoginPrincipal principal = resolvePrincipal(session);
            if (principal == null || principal.getUserId() == null) {
                realtimeNotifier.sendError(session, clientMessageId, "UNAUTHORIZED", "未登录");
                return;
            }

            String finalClientMessageId = clientMessageId;
            runWithPrincipal(principal, () ->
                    messageSendService.sendMessage(
                            principal.getUserId(),
                            toUserId,
                            content,
                            session.getId(),
                            finalClientMessageId
                    )
            );
        } catch (IllegalArgumentException ex) {
            realtimeNotifier.sendError(session, clientMessageId, "INVALID_PARAM", ex.getMessage());
        } catch (PermissionDeniedException | UnauthenticatedException ex) {
            realtimeNotifier.sendError(session, clientMessageId, "FORBIDDEN", ex.getMessage());
        } catch (BusinessException ex) {
            realtimeNotifier.sendError(session, clientMessageId, String.valueOf(ex.getCode()), ex.getMessage());
        } catch (Exception ex) {
            logger.error("处理WebSocket私信失败，sessionId={}", session.getId(), ex);
            realtimeNotifier.sendError(session, clientMessageId, "INTERNAL_ERROR", "发送消息失败，请稍后重试");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.warn("WebSocket连接异常，sessionId={}", session.getId(), exception);
        sessionRegistry.unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private LoginPrincipal resolvePrincipal(WebSocketSession session) {
        Object value = session.getAttributes().get(MessageWebSocketHandshakeInterceptor.PRINCIPAL_ATTR);
        return value instanceof LoginPrincipal principal ? principal : null;
    }

    private void markPresence(WebSocketSession session, LoginPrincipal principal) {
        Object sessionId = session.getAttributes().get(MessageWebSocketHandshakeInterceptor.SESSION_ID_ATTR);
        if (sessionId instanceof String value && !value.isBlank()) {
            presenceService.markOnline(principal, value, "message-ws", null, null);
        }
    }

    private void runWithPrincipal(LoginPrincipal principal, Runnable action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                "websocket",
                buildAuthorities(principal)
        ));
        SecurityContextHolder.setContext(context);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
            if (previous != null && previous.getAuthentication() != null) {
                SecurityContextHolder.setContext(previous);
            }
        }
    }

    private Collection<? extends GrantedAuthority> buildAuthorities(LoginPrincipal principal) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(principal.getRoleCode().authority()));
        for (String permission : principal.getPermissions()) {
            authorities.add(new SimpleGrantedAuthority(PermissionCode.toAuthority(permission)));
        }
        return authorities;
    }

    private String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Long longValue(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.longValue();
        }
        try {
            return Long.valueOf(value.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
