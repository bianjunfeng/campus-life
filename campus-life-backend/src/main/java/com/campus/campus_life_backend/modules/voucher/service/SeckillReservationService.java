package com.campus.campus_life_backend.modules.voucher.service;

import com.campus.campus_life_backend.modules.voucher.config.WelfareCacheKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeckillReservationService {

    private final StringRedisTemplate stringRedisTemplate;

    public SeckillReservationService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void rollbackReservation(Long voucherId, Long userId) {
        if (voucherId == null || userId == null) {
            return;
        }
        stringRedisTemplate.opsForValue().increment(WelfareCacheKeys.stockKey(voucherId));
        stringRedisTemplate.opsForSet().remove(WelfareCacheKeys.userSetKey(voucherId), String.valueOf(userId));
    }
}
