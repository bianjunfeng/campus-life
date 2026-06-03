package com.campus.campus_life_backend.modules.voucher.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.voucher.entity.SeckillVoucher;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.voucher.mapper.SeckillVoucherMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProductQueryService {

    private final VoucherMapper voucherMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final MerchantMapper merchantMapper;

    public ProductQueryService(VoucherMapper voucherMapper, SeckillVoucherMapper seckillVoucherMapper,
                               MerchantMapper merchantMapper) {
        this.voucherMapper = voucherMapper;
        this.seckillVoucherMapper = seckillVoucherMapper;
        this.merchantMapper = merchantMapper;
    }

    public Map<String, Object> getProductDetail(Long productId) {
        Voucher voucher = voucherMapper.findById(productId);
        if (voucher == null) {
            throw new ProductNotFoundException("商品不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", voucher.getId());
        result.put("title", voucher.getTitle());
        result.put("description", voucher.getSubTitle());
        result.put("price", voucher.getPayValue());
        result.put("originalPrice", voucher.getAmount());
        result.put("stock", voucher.getStock());
        result.put("image", voucher.getImageUrl());

        if (voucher.getAmount() != null && voucher.getPayValue() != null) {
            result.put("discount", voucher.getAmount()
                    .divide(voucher.getPayValue(), 2, java.math.RoundingMode.HALF_UP)
                    .doubleValue());
        }

        if (voucher.getMerchantId() != null) {
            Merchant merchant = merchantMapper.findById(voucher.getMerchantId());
            if (merchant != null) {
                result.put("shopName", merchant.getName());
                result.put("shopAddress", merchant.getAddress());
            }
        }

        SeckillVoucher seckill = seckillVoucherMapper.findByVoucherId(productId);
        if (seckill != null) {
            result.put("totalStock", seckill.getStock());
            appendCountdown(result, seckill);
        }
        return result;
    }

    private void appendCountdown(Map<String, Object> result, SeckillVoucher seckill) {
        if (seckill.getEndTime() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = seckill.getEndTime();
        if (end.isAfter(now)) {
            long seconds = java.time.Duration.between(now, end).getSeconds();
            Map<String, Integer> countdown = new HashMap<>();
            countdown.put("hours", (int) (seconds / 3600));
            countdown.put("minutes", (int) ((seconds % 3600) / 60));
            countdown.put("seconds", (int) (seconds % 60));
            result.put("countdown", countdown);
        }
    }

    public static class ProductNotFoundException extends BusinessException {
        public ProductNotFoundException(String message) {
            super(BusinessErrorCode.VOUCHER_NOT_FOUND, message);
        }
    }
}
