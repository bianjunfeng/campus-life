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
        if (principal == null || principal.getUserId() == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        String ticket = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(key(ticket), principal.getUserId().toString(), Duration.ofSeconds(EXPIRES_IN_SECONDS));
        return ticket;
    }

    public Optional<LoginPrincipal> consumeTicket(String ticket) {
        if (!StringUtils.hasText(ticket)) {
            return Optional.empty();
        }
        String userIdText = stringRedisTemplate.opsForValue().getAndDelete(key(ticket));
        if (!StringUtils.hasText(userIdText)) {
            return Optional.empty();
        }
        try {
            Long userId = Long.valueOf(userIdText);
            return Optional.of(loginPrincipalFactory.create(userId));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String key(String ticket) {
        return KEY_PREFIX + ticket;
    }
}
