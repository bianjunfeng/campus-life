package com.campus.campus_life_backend.modules.payment.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentIdempotencyService {

    private static final String CREATE_PREFIX = "payment:idem:create:";
    private static final String CALLBACK_PREFIX = "payment:idem:callback:";
    private static final String REFUND_PREFIX = "payment:idem:refund:";
    private static final String COMPLETED_VALUE_PREFIX = "DONE:";

    public static final Duration CREATE_PROCESSING_TTL = Duration.ofSeconds(60);
    public static final Duration CREATE_COMPLETED_TTL = Duration.ofMinutes(5);
    public static final Duration REFUND_PROCESSING_TTL = Duration.ofSeconds(60);
    public static final Duration REFUND_COMPLETED_TTL = Duration.ofMinutes(10);

    private static final DefaultRedisScript<Long> RELEASE_IF_OWNER_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private static final DefaultRedisScript<Long> COMPLETE_IF_OWNER_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('set', KEYS[1], ARGV[2], 'EX', ARGV[3]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    public PaymentIdempotencyService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public record IdempotencyLock(String key, String token) {
    }

    public String resolveCreateKey(String idempotencyKey, String orderNo, Long userId, String paymentMethod) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return CREATE_PREFIX + idempotencyKey.trim();
        }
        return CREATE_PREFIX + userId + ":" + orderNo + ":" + paymentMethod;
    }

    public Optional<IdempotencyLock> tryAcquireCreate(String key, Duration ttl) {
        return tryAcquire(key, ttl);
    }

    /**
     * 请求成功完成后保留幂等键直至 TTL，防止并发重复创建渠道支付单。
     */
    public void completeCreate(IdempotencyLock lock) {
        complete(lock, CREATE_COMPLETED_TTL);
    }

    /**
     * 仅释放当前请求持有的锁，便于用户立即重试。
     */
    public void releaseCreate(IdempotencyLock lock) {
        release(lock);
    }

    public String callbackKey(String paymentMethod, String orderNo) {
        return CALLBACK_PREFIX + paymentMethod + ":" + orderNo;
    }

    public boolean acquireCallback(String key, Duration ttl) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, UUID.randomUUID().toString(), ttl);
        return Boolean.TRUE.equals(success);
    }

    public void releaseCallback(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }

    public String resolveRefundKey(String idempotencyKey, String paymentNo, Long userId) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return REFUND_PREFIX + idempotencyKey.trim();
        }
        return REFUND_PREFIX + userId + ":" + paymentNo;
    }

    public Optional<IdempotencyLock> tryAcquireRefund(String key, Duration ttl) {
        return tryAcquire(key, ttl);
    }

    public void completeRefund(IdempotencyLock lock) {
        complete(lock, REFUND_COMPLETED_TTL);
    }

    public void releaseRefund(IdempotencyLock lock) {
        release(lock);
    }

    private Optional<IdempotencyLock> tryAcquire(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(success) ? Optional.of(new IdempotencyLock(key, token)) : Optional.empty();
    }

    private void complete(IdempotencyLock lock, Duration completedTtl) {
        if (lock == null) {
            return;
        }
        try {
            stringRedisTemplate.execute(
                    COMPLETE_IF_OWNER_SCRIPT,
                    Collections.singletonList(lock.key()),
                    lock.token(),
                    COMPLETED_VALUE_PREFIX + lock.token(),
                    String.valueOf(completedTtl.getSeconds())
            );
        } catch (Exception ignored) {
        }
    }

    private void release(IdempotencyLock lock) {
        if (lock == null) {
            return;
        }
        try {
            stringRedisTemplate.execute(
                    RELEASE_IF_OWNER_SCRIPT,
                    Collections.singletonList(lock.key()),
                    lock.token()
            );
        } catch (Exception ignored) {
        }
    }
}
