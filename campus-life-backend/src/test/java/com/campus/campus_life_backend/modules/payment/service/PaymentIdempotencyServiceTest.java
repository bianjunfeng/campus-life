package com.campus.campus_life_backend.modules.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PaymentIdempotencyService paymentIdempotencyService;

    @BeforeEach
    void setUp() {
        paymentIdempotencyService = new PaymentIdempotencyService(stringRedisTemplate);
    }

    @Test
    void shouldAcquireCreateLockWhenKeyIsFree() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("payment:idem:create:1"), anyString(), eq(Duration.ofSeconds(60))))
                .thenReturn(true);

        Optional<PaymentIdempotencyService.IdempotencyLock> lock =
                paymentIdempotencyService.tryAcquireCreate("payment:idem:create:1", Duration.ofSeconds(60));

        assertTrue(lock.isPresent());
    }

    @Test
    void shouldRejectCreateLockWhenKeyIsBusy() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("payment:idem:create:1"), anyString(), eq(Duration.ofSeconds(60))))
                .thenReturn(false);

        Optional<PaymentIdempotencyService.IdempotencyLock> lock =
                paymentIdempotencyService.tryAcquireCreate("payment:idem:create:1", Duration.ofSeconds(60));

        assertFalse(lock.isPresent());
    }

    @Test
    void shouldCompleteCreateWithOwnerScript() {
        PaymentIdempotencyService.IdempotencyLock lock =
                new PaymentIdempotencyService.IdempotencyLock("payment:idem:create:1", "token-1");

        paymentIdempotencyService.completeCreate(lock);

        ArgumentCaptor<DefaultRedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(DefaultRedisScript.class);
        verify(stringRedisTemplate).execute(
                scriptCaptor.capture(),
                eq(java.util.Collections.singletonList("payment:idem:create:1")),
                eq("token-1"),
                eq("DONE:token-1"),
                eq(String.valueOf(PaymentIdempotencyService.CREATE_COMPLETED_TTL.getSeconds()))
        );
    }

    @Test
    void shouldReleaseCreateWithOwnerScript() {
        PaymentIdempotencyService.IdempotencyLock lock =
                new PaymentIdempotencyService.IdempotencyLock("payment:idem:create:1", "token-1");

        paymentIdempotencyService.releaseCreate(lock);

        verify(stringRedisTemplate).execute(
                any(DefaultRedisScript.class),
                eq(java.util.Collections.singletonList("payment:idem:create:1")),
                eq("token-1")
        );
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }
}
