package com.campus.campus_life_backend.modules.voucher.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.modules.voucher.config.VoucherMqConstants;
import com.campus.campus_life_backend.modules.voucher.config.WelfareCacheKeys;
import com.campus.campus_life_backend.modules.voucher.dto.SeckillOrderMessage;
import com.campus.campus_life_backend.modules.voucher.entity.SeckillVoucher;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.voucher.mapper.SeckillVoucherMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import com.campus.campus_life_backend.modules.order.mapper.VoucherOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class VoucherSeckillService {

    private static final String RESULT_PENDING = "PENDING";
    private static final String RESULT_SUCCESS = "SUCCESS";

    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final VoucherMapper voucherMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final WelfareQueryService welfareQueryService;
    private final SeckillOrderProcessService seckillOrderProcessService;
    private final SeckillMonitorMetricsService seckillMonitorMetricsService;
    private final SeckillReservationService seckillReservationService;
    private final VoucherOrderMapper voucherOrderMapper;
    private final DefaultRedisScript<Long> seckillPrecheckScript;

    public VoucherSeckillService(
            StringRedisTemplate stringRedisTemplate,
            SeckillVoucherMapper seckillVoucherMapper,
            VoucherMapper voucherMapper,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            WelfareQueryService welfareQueryService,
            SeckillOrderProcessService seckillOrderProcessService,
            SeckillMonitorMetricsService seckillMonitorMetricsService,
            SeckillReservationService seckillReservationService,
            VoucherOrderMapper voucherOrderMapper
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.seckillVoucherMapper = seckillVoucherMapper;
        this.voucherMapper = voucherMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.welfareQueryService = welfareQueryService;
        this.seckillOrderProcessService = seckillOrderProcessService;
        this.seckillMonitorMetricsService = seckillMonitorMetricsService;
        this.seckillReservationService = seckillReservationService;
        this.voucherOrderMapper = voucherOrderMapper;
        this.seckillPrecheckScript = new DefaultRedisScript<>();
        this.seckillPrecheckScript.setLocation(new ClassPathResource("lua/seckill_precheck.lua"));
        this.seckillPrecheckScript.setResultType(Long.class);
    }

    @RequirePermission(anyOf = {"voucher:order:self"})
    public Map<String, Object> submitSeckill(Long voucherId, Long userId) {
        return submitSeckillInternal(voucherId, userId);
    }

    public Map<String, Object> submitSeckillInternal(Long voucherId, Long userId) {
        return submitSeckillCore(voucherId, userId, true);
    }

    public Map<String, Object> submitSeckillPerfHotPath(Long voucherId, Long userId) {
        return submitSeckillCore(voucherId, userId, false);
    }

    private Map<String, Object> submitSeckillCore(Long voucherId, Long userId, boolean validateAndWarmCache) {
        seckillMonitorMetricsService.recordSubmit(voucherId);
        if (validateAndWarmCache) {
            SeckillVoucher seckillVoucher = seckillVoucherMapper.findByVoucherId(voucherId);
            if (seckillVoucher == null) {
                seckillMonitorMetricsService.recordProcessFail(null, "该券不是秒杀券", "submit");
                throw new BusinessException(BusinessErrorCode.NOT_SECKILL_VOUCHER);
            }
            ensureSeckillCacheReady(voucherId, seckillVoucher);
        }

        String orderNo = generateOrderNo(userId);
        long now = System.currentTimeMillis() / 1000;
        Long luaResult = stringRedisTemplate.execute(
                seckillPrecheckScript,
                List.of(
                        WelfareCacheKeys.stockKey(voucherId),
                        WelfareCacheKeys.userSetKey(voucherId),
                        WelfareCacheKeys.beginKey(voucherId),
                        WelfareCacheKeys.endKey(voucherId)
                ),
                String.valueOf(userId),
                String.valueOf(now)
        );

        if (luaResult == null) {
            seckillMonitorMetricsService.recordLuaReject(voucherId, null);
            throw new BusinessException(BusinessErrorCode.SECKILL_BUSY);
        }
        if (luaResult == 1L) {
            seckillMonitorMetricsService.recordLuaReject(voucherId, luaResult);
            throw new BusinessException(BusinessErrorCode.STOCK_NOT_ENOUGH);
        }
        if (luaResult == 2L) {
            seckillMonitorMetricsService.recordLuaReject(voucherId, luaResult);
            throw new BusinessException(BusinessErrorCode.SECKILL_DUPLICATE);
        }
        if (luaResult == 3L) {
            seckillMonitorMetricsService.recordLuaReject(voucherId, luaResult);
            throw new BusinessException(BusinessErrorCode.SECKILL_NOT_STARTED);
        }
        if (luaResult == 4L) {
            seckillMonitorMetricsService.recordLuaReject(voucherId, luaResult);
            throw new BusinessException(BusinessErrorCode.SECKILL_ENDED);
        }

        seckillMonitorMetricsService.recordAccepted(voucherId);
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setOrderNo(orderNo);
        message.setUserId(userId);
        message.setVoucherId(voucherId);
        message.setCreateEpochSecond(now);

        stringRedisTemplate.opsForValue().set(
                WelfareCacheKeys.resultKey(orderNo),
                RESULT_PENDING,
                10,
                TimeUnit.MINUTES
        );
        stringRedisTemplate.opsForValue().set(
                WelfareCacheKeys.orderOwnerKey(orderNo),
                String.valueOf(userId),
                10,
                TimeUnit.MINUTES
        );

        try {
            rabbitTemplate.convertAndSend(
                    VoucherMqConstants.SECKILL_ORDER_EXCHANGE,
                    VoucherMqConstants.SECKILL_ORDER_ROUTING_KEY,
                    objectMapper.writeValueAsString(message)
            );
            seckillMonitorMetricsService.recordMqPublishSuccess(voucherId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "排队中");
            result.put("orderNo", orderNo);
            return result;
        } catch (Exception e) {
            seckillMonitorMetricsService.recordMqPublishFail(voucherId, e.getMessage());
            return processSeckillFallback(message, e);
        }
    }

    private void ensureSeckillCacheReady(Long voucherId, SeckillVoucher seckillVoucher) {
        String stockKey = WelfareCacheKeys.stockKey(voucherId);
        String beginKey = WelfareCacheKeys.beginKey(voucherId);
        String endKey = WelfareCacheKeys.endKey(voucherId);

        boolean stockMissing = !Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey));
        boolean beginMissing = seckillVoucher.getStartTime() != null
                && !Boolean.TRUE.equals(stringRedisTemplate.hasKey(beginKey));
        boolean endMissing = seckillVoucher.getEndTime() != null
                && !Boolean.TRUE.equals(stringRedisTemplate.hasKey(endKey));

        if (!stockMissing && !beginMissing && !endMissing) {
            return;
        }

        if (stockMissing) {
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(seckillVoucher.getStock()));
        }
        if (seckillVoucher.getStartTime() != null && beginMissing) {
            stringRedisTemplate.opsForValue().set(
                    beginKey,
                    String.valueOf(seckillVoucher.getStartTime().toEpochSecond(ZoneOffset.ofHours(8)))
            );
        }
        if (seckillVoucher.getEndTime() != null && endMissing) {
            stringRedisTemplate.opsForValue().set(
                    endKey,
                    String.valueOf(seckillVoucher.getEndTime().toEpochSecond(ZoneOffset.ofHours(8)))
            );
        }
    }

    @PostConstruct
    public void preheatOnStartup() {
        try {
            List<SeckillVoucher> activeList = seckillVoucherMapper.findActive();
            for (SeckillVoucher sv : activeList) {
                preheatSeckillVoucher(sv.getVoucherId());
            }
        } catch (Exception ignored) {
        }
    }

    @RequirePermission(anyOf = {"voucher:order:self"})
    public Map<String, Object> querySeckillResult(String orderNo, Long userId) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException(BusinessErrorCode.ORDER_NO_REQUIRED);
        }
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.LOGIN_REQUIRED);
        }
        ensureOrderOwnership(orderNo, userId);

        String result = stringRedisTemplate.opsForValue().get(WelfareCacheKeys.resultKey(orderNo));

        Map<String, Object> response = new HashMap<>();
        response.put("orderNo", orderNo);

        if (result == null) {
            response.put("status", "NOT_FOUND");
            response.put("message", "订单不存在或已过期");
            return response;
        }

        if (RESULT_SUCCESS.equals(result)) {
            response.put("status", "SUCCESS");
            response.put("message", "抢购成功");
            return response;
        }

        if (RESULT_PENDING.equals(result)) {
            response.put("status", "PENDING");
            response.put("message", "排队处理中");
            return response;
        }

        response.put("status", "FAIL");
        response.put("message", result);
        return response;
    }

    private void ensureOrderOwnership(String orderNo, Long userId) {
        String ownerText = stringRedisTemplate.opsForValue().get(WelfareCacheKeys.orderOwnerKey(orderNo));
        if (ownerText != null && !ownerText.isBlank()) {
            try {
                Long ownerId = Long.parseLong(ownerText);
                if (!userId.equals(ownerId)) {
                    throw new BusinessException(BusinessErrorCode.ORDER_QUERY_FORBIDDEN);
                }
                return;
            } catch (NumberFormatException ignored) {
                // 继续使用数据库回退校验
            }
        }

        Map<String, Object> orderView = voucherOrderMapper.findOrderViewByOrderNoAndUserId(orderNo, userId);
        if (orderView == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_QUERY_FORBIDDEN);
        }
    }

    public void markResultSuccess(String orderNo) {
        stringRedisTemplate.opsForValue().set(WelfareCacheKeys.resultKey(orderNo), RESULT_SUCCESS, 10, TimeUnit.MINUTES);
    }

    public void markResultFail(String orderNo, String reason) {
        stringRedisTemplate.opsForValue().set(WelfareCacheKeys.resultKey(orderNo), reason, 10, TimeUnit.MINUTES);
    }

    public boolean tryMarkConsuming(String orderNo) {
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                WelfareCacheKeys.consumeKey(orderNo),
                "1",
                10,
                TimeUnit.MINUTES
        );
        return Boolean.TRUE.equals(ok);
    }

    public void rollbackReservation(Long voucherId, Long userId) {
        seckillReservationService.rollbackReservation(voucherId, userId);
    }

    public void preheatSeckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherMapper.findByVoucherId(voucherId);
        Voucher voucher = voucherMapper.findById(voucherId);
        if (seckillVoucher == null || voucher == null) {
            throw new BusinessException(BusinessErrorCode.SECKILL_VOUCHER_NOT_FOUND);
        }

        stringRedisTemplate.opsForValue().set(WelfareCacheKeys.stockKey(voucherId), String.valueOf(seckillVoucher.getStock()));
        if (seckillVoucher.getStartTime() != null) {
            stringRedisTemplate.opsForValue().set(
                    WelfareCacheKeys.beginKey(voucherId),
                    String.valueOf(seckillVoucher.getStartTime().toEpochSecond(ZoneOffset.ofHours(8)))
            );
        }
        if (seckillVoucher.getEndTime() != null) {
            stringRedisTemplate.opsForValue().set(
                    WelfareCacheKeys.endKey(voucherId),
                    String.valueOf(seckillVoucher.getEndTime().toEpochSecond(ZoneOffset.ofHours(8)))
            );
        }

        welfareQueryService.evictWelfareCache();
    }

    public Map<String, Object> getSeckillMonitor(Long voucherId) {
        Map<String, Object> monitor = new HashMap<>();
        Voucher voucher = voucherMapper.findById(voucherId);
        SeckillVoucher seckillVoucher = seckillVoucherMapper.findByVoucherId(voucherId);
        String redisStock = stringRedisTemplate.opsForValue().get(WelfareCacheKeys.stockKey(voucherId));
        Long userCount = stringRedisTemplate.opsForSet().size(WelfareCacheKeys.userSetKey(voucherId));

        monitor.put("voucherId", voucherId);
        monitor.put("dbVoucherStock", voucher != null ? voucher.getStock() : null);
        monitor.put("dbSeckillStock", seckillVoucher != null ? seckillVoucher.getStock() : null);
        monitor.put("redisStock", redisStock);
        monitor.put("reservedUserCount", userCount == null ? 0 : userCount);
        return monitor;
    }

    private String generateOrderNo(Long userId) {
        Long seq = stringRedisTemplate.opsForValue().increment(WelfareCacheKeys.SECKILL_ORDER_ID_SEQ);
        return "SK" + (System.currentTimeMillis() / 1000) + userId + (seq == null ? 0 : seq);
    }

    private void rollbackRedisReservation(Long voucherId, Long userId) {
        rollbackReservation(voucherId, userId);
    }

    private Map<String, Object> processSeckillFallback(SeckillOrderMessage message, Exception mqException) {
        seckillMonitorMetricsService.recordFallback(message.getVoucherId());
        long start = System.currentTimeMillis();
        try {
            if (!tryMarkConsuming(message.getOrderNo())) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "排队处理中");
                result.put("orderNo", message.getOrderNo());
                return result;
            }
            SeckillOrderProcessService.ProcessStatus status = seckillOrderProcessService.processOrder(message);
            if (status == SeckillOrderProcessService.ProcessStatus.SUCCESS) {
                seckillMonitorMetricsService.recordProcessSuccess(message, System.currentTimeMillis() - start, "fallback");
                markResultSuccess(message.getOrderNo());
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "抢购成功");
                result.put("orderNo", message.getOrderNo());
                result.put("fallback", true);
                return result;
            }
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "排队处理中");
            result.put("orderNo", message.getOrderNo());
            return result;
        } catch (SeckillOrderProcessService.SeckillNonRetryableException e) {
            seckillMonitorMetricsService.recordProcessFail(message, e.getMessage(), "fallback");
            markResultFail(message.getOrderNo(), e.getMessage());
            rollbackRedisReservation(message.getVoucherId(), message.getUserId());
            throw new BusinessException(BusinessErrorCode.STOCK_NOT_ENOUGH, e.getMessage(), e);
        } catch (Exception ex) {
            seckillMonitorMetricsService.recordProcessFail(message, ex.getMessage(), "fallback");
            markResultFail(message.getOrderNo(), "系统繁忙，请稍后重试");
            rollbackRedisReservation(message.getVoucherId(), message.getUserId());
            throw new BusinessException(BusinessErrorCode.SECKILL_QUEUE_FAILED, mqException);
        }
    }
}
