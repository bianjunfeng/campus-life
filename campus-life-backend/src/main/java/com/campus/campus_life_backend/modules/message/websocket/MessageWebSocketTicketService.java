package com.campus.campus_life_backend.modules.message.websocket;

import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.service.LoginPrincipalFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class MessageWebSocketTicketService {

    public static final long EXPIRES_IN_SECONDS = 60L;
    private static final String KEY_PREFIX = "message:ws:ticket:";

    private final StringRedisTemplate stringRedisTemplate;
    private final LoginPrincipalFactory loginPrincipalFactory;

    public MessageWebSocketTicketService(StringRedisTemplate stringRedisTemplate,
                                         LoginPrincipalFactory loginPrincipalFactory) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.loginPrincipalFactory = loginPrincipalFactory;
    }

    public String createTicket(LoginPrincipal principal) {
        return createTicket(principal, null);
    }

    public String createTicket(LoginPrincipal principal, String sessionId) {
        if (principal == null || principal.getUserId() == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        String ticket = UUID.randomUUID().toString().replace("-", "");
        String value = principal.getUserId() + ":" + (sessionId == null ? "" : sessionId);
        stringRedisTemplate.opsForValue().set(key(ticket), value, Duration.ofSeconds(EXPIRES_IN_SECONDS));
        return ticket;
    }

    public Optional<ConsumedTicket> consumeTicket(String ticket) {
        if (!StringUtils.hasText(ticket)) {
            return Optional.empty();
        }
        String ticketValue = stringRedisTemplate.opsForValue().getAndDelete(key(ticket));
        if (!StringUtils.hasText(ticketValue)) {
            return Optional.empty();
        }
        try {
            String[] parts = ticketValue.split(":", 2);
            String userIdText = parts[0];
            String sessionId = parts.length > 1 && StringUtils.hasText(parts[1]) ? parts[1] : null;
            Long userId = Long.valueOf(userIdText);
            LoginPrincipal principal = loginPrincipalFactory.create(userId);
            if (principal == null) {
                return Optional.empty();
            }
            return Optional.of(new ConsumedTicket(principal, sessionId));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String key(String ticket) {
        return KEY_PREFIX + ticket;
    }

    public record ConsumedTicket(LoginPrincipal principal, String sessionId) {
    }
}
