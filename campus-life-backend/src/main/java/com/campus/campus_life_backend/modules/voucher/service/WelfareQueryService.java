package com.campus.campus_life_backend.modules.voucher.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.voucher.config.WelfareCacheKeys;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.voucher.entity.SeckillVoucher;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.voucher.mapper.SeckillVoucherMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import com.campus.campus_life_backend.modules.order.mapper.VoucherOrderMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class WelfareQueryService {

    private final VoucherMapper voucherMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final VoucherOrderMapper voucherOrderMapper;
    private final MerchantMapper merchantMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Cache<String, String> localCache;
    private final long redisTtlSeconds;

    public WelfareQueryService(
            VoucherMapper voucherMapper,
            SeckillVoucherMapper seckillVoucherMapper,
            VoucherOrderMapper voucherOrderMapper,
            MerchantMapper merchantMapper,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${seckill.cache.redis-ttl-seconds:60}") long redisTtlSeconds,
            @Value("${seckill.cache.caffeine-max-size:200}") long caffeineMaxSize,
            @Value("${seckill.cache.caffeine-expire-seconds:30}") long caffeineExpireSeconds
    ) {
        this.voucherMapper = voucherMapper;
        this.seckillVoucherMapper = seckillVoucherMapper;
        this.voucherOrderMapper = voucherOrderMapper;
        this.merchantMapper = merchantMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.redisTtlSeconds = redisTtlSeconds;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(caffeineMaxSize)
                .expireAfterWrite(Duration.ofSeconds(caffeineExpireSeconds))
                .build();
    }

    public Map<String, Object> getWelfareHome() {
        Map<String, Object> result = new HashMap<>();
        result.put("coupons", getCoupons());
        result.put("flashSales", getFlashSales());
        return result;
    }

    public List<Map<String, Object>> getCoupons() {
        return readThroughCache(
                WelfareCacheKeys.COUPON_LIST,
                new TypeReference<List<Map<String, Object>>>() {},
                this::buildCouponList
        );
    }

    public List<Map<String, Object>> getCoupons(Integer categoryId) {
        List<Map<String, Object>> allCoupons = getCoupons();
        if (categoryId == null) {
            return allCoupons;
        }
        return allCoupons.stream()
                .filter(item -> {
                    Object value = item.get("categoryId");
                    if (value == null) {
                        return false;
                    }
                    try {
                        return Long.parseLong(String.valueOf(value)) == categoryId.longValue();
                    } catch (Exception ignored) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getFlashSales() {
        return readThroughCache(
                WelfareCacheKeys.FLASH_SALE_LIST,
                new TypeReference<List<Map<String, Object>>>() {},
                this::buildFlashSaleList
        );
    }

    public Map<String, Object> getCouponDetail(Long couponId) {
        Voucher voucher = voucherMapper.findById(couponId);
        if (voucher == null || voucher.getStatus() == null || voucher.getStatus() != 1) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_NOT_AVAILABLE);
        }
        Merchant merchant = merchantMapper.findById(voucher.getMerchantId());
        int salesCount = voucher.getSoldCount() == null ? 0 : voucher.getSoldCount();
        Map<String, Object> item = buildCouponItem(voucher, merchant, salesCount);
        SeckillVoucher seckillVoucher = seckillVoucherMapper.findByVoucherId(couponId);
        if (seckillVoucher != null) {
            item.put("isSeckill", true);
            item.put("stock", seckillVoucher.getStock());
            item.put("totalStock", seckillVoucher.getStock());
            item.put("countdown", buildCountdown(seckillVoucher.getEndTime()));
        } else {
            item.put("isSeckill", false);
        }
        return item;
    }

    public void evictWelfareCache() {
        localCache.invalidate(WelfareCacheKeys.COUPON_LIST);
        localCache.invalidate(WelfareCacheKeys.FLASH_SALE_LIST);
        localCache.invalidate(WelfareCacheKeys.WELFARE_HOME);
        stringRedisTemplate.delete(List.of(
                WelfareCacheKeys.COUPON_LIST,
                WelfareCacheKeys.FLASH_SALE_LIST,
                WelfareCacheKeys.WELFARE_HOME
        ));
    }

    private List<Map<String, Object>> buildCouponList() {
        List<Voucher> vouchers = voucherMapper.findAvailable();
        return vouchers.stream().map(voucher -> {
            Merchant merchant = merchantMapper.findById(voucher.getMerchantId());
            int salesCount = voucher.getSoldCount() == null ? 0 : voucher.getSoldCount();
            return buildCouponItem(voucher, merchant, salesCount);
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildFlashSaleList() {
        List<SeckillVoucher> seckillVouchers = seckillVoucherMapper.findActive();
        return seckillVouchers.stream().map(seckill -> {
            Voucher voucher = voucherMapper.findById(seckill.getVoucherId());
            if (voucher == null) {
                return null;
            }
            Map<String, Object> item = new HashMap<>();
            Merchant merchant = merchantMapper.findById(voucher.getMerchantId());
            int salesCount = voucher.getSoldCount() == null ? 0 : voucher.getSoldCount();
            item.putAll(buildCouponItem(voucher, merchant, salesCount));
            item.put("stock", seckill.getStock());
            item.put("totalStock", seckill.getStock());
            item.put("countdown", buildCountdown(seckill.getEndTime()));
            item.put("isSeckill", true);
            return item;
        }).filter(item -> item != null).collect(Collectors.toList());
    }

    private Map<String, Object> buildCouponItem(Voucher voucher, Merchant merchant, int salesCount) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", voucher.getId());
        item.put("title", voucher.getTitle());
        item.put("description", voucher.getSubTitle());
        item.put("price", voucher.getPayValue());
        item.put("originalPrice", voucher.getAmount());
        item.put("categoryId", merchant != null && merchant.getTypeId() != null ? merchant.getTypeId() : null);
        item.put("merchantId", voucher.getMerchantId());
        item.put("shopName", merchant != null ? merchant.getName() : "商家");
        item.put("shopAddress", merchant != null ? merchant.getAddress() : "");
        item.put("shopProvince", merchant != null ? merchant.getProvince() : null);
        item.put("shopCity", merchant != null ? merchant.getCity() : null);
        item.put("shopDistrict", merchant != null ? merchant.getDistrict() : null);
        item.put("shopLongitude", merchant != null ? merchant.getLongitude() : null);
        item.put("shopLatitude", merchant != null ? merchant.getLatitude() : null);
        item.put("shopLocation", buildShopLocation(merchant));
        item.put("shopAddressFull", buildShopAddressFull(merchant));
        item.put("validPeriod", "有效期至 " + (voucher.getEndTime() != null ? voucher.getEndTime().toLocalDate() : "长期"));
        item.put("stock", voucher.getStock());
        item.put("grabbed", false);
        item.put("image", voucher.getImageUrl());
        item.put("salesCount", salesCount);
        item.put("rating", 4.6);
        item.put("businessHours", merchant != null && merchant.getBusinessHours() != null && !merchant.getBusinessHours().trim().isEmpty()
                ? merchant.getBusinessHours()
                : "10:00-22:00");
        item.put("distance", null);
        item.put("isSeckill", false);
        item.put("discount", (voucher.getAmount() != null && voucher.getPayValue() != null && voucher.getAmount().doubleValue() > 0)
                ? Math.round((voucher.getPayValue().doubleValue() / voucher.getAmount().doubleValue()) * 100) / 10.0
                : null);
        item.put("couponInfo", voucher.getSubTitle());
        return item;
    }

    private String buildShopLocation(Merchant merchant) {
        if (merchant == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendPart(sb, merchant.getProvince());
        appendPart(sb, merchant.getCity());
        appendPart(sb, merchant.getDistrict());
        return sb.toString();
    }

    private String buildShopAddressFull(Merchant merchant) {
        if (merchant == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendPart(sb, merchant.getProvince());
        appendPart(sb, merchant.getCity());
        appendPart(sb, merchant.getDistrict());
        appendPart(sb, merchant.getAddress());
        return sb.toString();
    }

    private void appendPart(StringBuilder sb, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        sb.append(trimmed);
    }

    private Map<String, Integer> buildCountdown(LocalDateTime endTime) {
        Map<String, Integer> countdown = new HashMap<>();
        countdown.put("hours", 0);
        countdown.put("minutes", 0);
        countdown.put("seconds", 0);
        if (endTime == null) {
            return countdown;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!endTime.isAfter(now)) {
            return countdown;
        }
        long seconds = Duration.between(now, endTime).getSeconds();
        countdown.put("hours", (int) (seconds / 3600));
        countdown.put("minutes", (int) ((seconds % 3600) / 60));
        countdown.put("seconds", (int) (seconds % 60));
        return countdown;
    }

    private <T> T readThroughCache(String key, TypeReference<T> typeRef, DataBuilder<T> builder) {
        String localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            try {
                return objectMapper.readValue(localValue, typeRef);
            } catch (Exception ignored) {
            }
        }

        String redisValue = stringRedisTemplate.opsForValue().get(key);
        if (redisValue != null) {
            localCache.put(key, redisValue);
            try {
                return objectMapper.readValue(redisValue, typeRef);
            } catch (Exception ignored) {
            }
        }

        T data = builder.build();
        try {
            String json = objectMapper.writeValueAsString(data);
            localCache.put(key, json);
            stringRedisTemplate.opsForValue().set(key, json, redisTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        return data;
    }

    @FunctionalInterface
    private interface DataBuilder<T> {
        T build();
    }
}
