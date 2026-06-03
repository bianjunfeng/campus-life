package com.campus.campus_life_backend.modules.payment.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class PaymentIdempotencyService {

    private static final String CREATE_PREFIX = "payment:idem:create:";
    private static final String CALLBACK_PREFIX = "payment:idem:callback:";
    private static final String REFUND_PREFIX = "payment:idem:refund:";

    private final StringRedisTemplate stringRedisTemplate;

    public PaymentIdempotencyService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String resolveCreateKey(String idempotencyKey, String orderNo, Long userId, String paymentMethod) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return CREATE_PREFIX + idempotencyKey.trim();
        }
        return CREATE_PREFIX + userId + ":" + orderNo + ":" + paymentMethod;
    }

    public boolean acquireCreate(String key, Duration ttl) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, UUID.randomUUID().toString(), ttl);
        return Boolean.TRUE.equals(success);
    }

    public void releaseCreate(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ignored) {
        }
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

    public boolean acquireRefund(String key, Duration ttl) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, UUID.randomUUID().toString(), ttl);
        return Boolean.TRUE.equals(success);
    }

    public void releaseRefund(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }
}
