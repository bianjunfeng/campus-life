package com.campus.campus_life_backend.modules.merchant.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.merchant.entity.MerchantAuthRequest;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantAuthRequestMapper;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 鍟嗗璁よ瘉鐢宠鏈嶅姟
 */
@Service
public class MerchantAuthService {

    private static final Logger logger = LoggerFactory.getLogger(MerchantAuthService.class);

    private final MerchantAuthRequestMapper merchantAuthRequestMapper;
    private final MerchantMapper merchantMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserService userService;

    public record AdminMerchantReviewResult(Integer oldStatus, Integer newStatus) {
    }

    public MerchantAuthService(MerchantAuthRequestMapper merchantAuthRequestMapper,
                               MerchantMapper merchantMapper,
                               StringRedisTemplate stringRedisTemplate,
                               UserService userService) {
        this.merchantAuthRequestMapper = merchantAuthRequestMapper;
        this.merchantMapper = merchantMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userService = userService;
    }

    /**
     * 鎻愪氦鍟嗗璁よ瘉鐢宠
     */
    @Transactional
    public MerchantAuthRequest submitMerchantAuthRequest(Long userId, Map<String, Object> requestData) {
        String lockKey = "auth:merchant:submit:" + userId;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 8, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(BusinessErrorCode.TOO_MANY_REQUESTS, "提交过于频繁，请稍后重试");
        }
        try {
            if (userService.hasAnyAuthVerified(userId)) {
                throw new BusinessException(BusinessErrorCode.MERCHANT_AUTH_ALREADY_APPROVED_OTHER);
            }

            // 妫€鏌ユ槸鍚﹀凡鏈夊緟瀹℃牳鐨勭敵璇?
            MerchantAuthRequest existing = merchantAuthRequestMapper.findByUserId(userId);

            MerchantAuthRequest request = new MerchantAuthRequest();
            request.setUserId(userId);
            request.setMerchantName(toStr(requestData.get("merchantName")));
            request.setLicenseNo(toStr(requestData.get("licenseNo")));
            request.setLicenseImg(toStr(requestData.get("licenseImg")));
            request.setAddress(toStr(requestData.get("address")));
            request.setStatus(0); // 寰呭鏍?

            if (existing != null) {
                Integer status = existing.getStatus();
                if (status != null && status == 0) {
                    throw new BusinessException(BusinessErrorCode.MERCHANT_AUTH_PENDING_DUPLICATE);
                }
                if (status != null && status == 1) {
                    throw new BusinessException(BusinessErrorCode.MERCHANT_AUTH_ALREADY_APPROVED);
                }
                if (status != null && status != 2) {
                    throw new BusinessException(BusinessErrorCode.MERCHANT_AUTH_STATUS_NOT_ALLOWED);
                }
                merchantAuthRequestMapper.insert(request);
                logger.info("创建新的商家认证申请(上次已驳回): userId={}", userId);
            } else {
                // 鍒涘缓鏂扮敵璇?
                merchantAuthRequestMapper.insert(request);
                logger.info("鍒涘缓鍟嗗璁よ瘉鐢宠: userId={}", userId);
            }

            return merchantAuthRequestMapper.findByUserId(userId);
        } finally {
            String current = stringRedisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(current)) {
                stringRedisTemplate.delete(lockKey);
            }
        }
    }

    /**
     * 鑾峰彇鐢ㄦ埛鐨勫晢瀹惰璇佺敵璇风姸鎬?
     */
    public MerchantAuthRequest getMerchantAuthRequest(Long userId) {
        return merchantAuthRequestMapper.findByUserId(userId);
    }

    @Transactional
    public AdminMerchantReviewResult adminApproveMerchant(Long merchantId, Long reviewerId, String reason) {
        Merchant merchant = merchantMapper.findById(merchantId);
        if (merchant == null) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_NOT_FOUND);
        }

        Integer oldStatus = merchant.getStatus();
        merchant.setStatus(1);
        merchantMapper.updateStatus(merchantId, 1);

        MerchantAuthRequest authRequest = merchantAuthRequestMapper.findByUserId(merchant.getUserId());
        if (authRequest != null) {
            authRequest.setStatus(1);
            authRequest.setReason(isBlank(reason) ? "后台审核通过" : reason.trim());
            authRequest.setReviewerId(reviewerId);
            authRequest.setReviewTime(java.time.LocalDateTime.now());
            merchantAuthRequestMapper.update(authRequest);

            if (!isBlank(authRequest.getMerchantName())) {
                merchant.setName(authRequest.getMerchantName());
            }
            if (!isBlank(authRequest.getAddress())) {
                merchant.setAddress(authRequest.getAddress());
            }
            merchantMapper.updateMerchant(merchant);
        }

        return new AdminMerchantReviewResult(oldStatus, 1);
    }

    @Transactional
    public AdminMerchantReviewResult adminRejectMerchant(Long merchantId, Long reviewerId, String reason) {
        Merchant merchant = merchantMapper.findById(merchantId);
        if (merchant == null) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_NOT_FOUND);
        }

        Integer oldStatus = merchant.getStatus();
        merchant.setStatus(3);
        merchantMapper.updateStatus(merchantId, 3);

        MerchantAuthRequest authRequest = merchantAuthRequestMapper.findByUserId(merchant.getUserId());
        if (authRequest != null) {
            authRequest.setStatus(2);
            authRequest.setReason(isBlank(reason) ? "后台审核驳回" : reason.trim());
            authRequest.setReviewerId(reviewerId);
            authRequest.setReviewTime(java.time.LocalDateTime.now());
            merchantAuthRequestMapper.update(authRequest);
        }

        return new AdminMerchantReviewResult(oldStatus, 3);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String toStr(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }
}


