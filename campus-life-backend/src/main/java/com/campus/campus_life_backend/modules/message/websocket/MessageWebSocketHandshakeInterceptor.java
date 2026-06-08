package com.campus.campus_life_backend.modules.message.websocket;

import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Component
public class MessageWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PRINCIPAL_ATTR = "messageWsPrincipal";
    public static final String SESSION_ID_ATTR = "messageWsSessionId";

    private final MessageWebSocketTicketService ticketService;

    public MessageWebSocketHandshakeInterceptor(MessageWebSocketTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String ticket = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("ticket");
        Optional<MessageWebSocketTicketService.ConsumedTicket> consumed = ticketService.consumeTicket(ticket);
        if (consumed.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(PRINCIPAL_ATTR, consumed.get().principal());
        if (consumed.get().sessionId() != null && !consumed.get().sessionId().isBlank()) {
            attributes.put(SESSION_ID_ATTR, consumed.get().sessionId());
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
