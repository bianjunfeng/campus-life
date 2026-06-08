package com.campus.campus_life_backend.modules.message.websocket;

import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.service.LoginPrincipalFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageWebSocketTicketServiceTest {

    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private LoginPrincipalFactory loginPrincipalFactory;
    private MessageWebSocketTicketService ticketService;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        loginPrincipalFactory = mock(LoginPrincipalFactory.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        ticketService = new MessageWebSocketTicketService(stringRedisTemplate, loginPrincipalFactory);
    }

    @Test
    void shouldCreateShortLivedTicketForCurrentUser() {
        LoginPrincipal principal = new LoginPrincipal(1L, RoleCode.STUDENT, Set.of("message:use"));

        String ticket = ticketService.createTicket(principal, "session-1");

        assertFalse(ticket.isBlank());
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                keyCaptor.capture(),
                eq("1:session-1"),
                eq(Duration.ofSeconds(MessageWebSocketTicketService.EXPIRES_IN_SECONDS))
        );
        assertTrue(keyCaptor.getValue().startsWith("message:ws:ticket:"));
    }

    @Test
    void shouldConsumeTicketOnceAndResolvePrincipal() {
        LoginPrincipal principal = new LoginPrincipal(1L, RoleCode.STUDENT, Set.of("message:use"));
        when(valueOperations.getAndDelete("message:ws:ticket:ticket-1")).thenReturn("1:session-1");
        when(loginPrincipalFactory.create(1L)).thenReturn(principal);

        Optional<MessageWebSocketTicketService.ConsumedTicket> result = ticketService.consumeTicket("ticket-1");

        assertTrue(result.isPresent());
        assertEquals(principal, result.get().principal());
        assertEquals("session-1", result.get().sessionId());
        verify(valueOperations).getAndDelete("message:ws:ticket:ticket-1");
    }

    @Test
    void shouldRejectMissingTicket() {
        assertTrue(ticketService.consumeTicket("").isEmpty());
    }
}
