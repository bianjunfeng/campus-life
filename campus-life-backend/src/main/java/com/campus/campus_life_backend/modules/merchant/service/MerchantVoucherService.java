package com.campus.campus_life_backend.modules.merchant.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.voucher.entity.SeckillVoucher;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.voucher.mapper.SeckillVoucherMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MerchantVoucherService {

    private static final String TYPE_GROUP = "group";
    private static final String TYPE_SECKILL = "seckill";

    private final VoucherMapper voucherMapper;
    private final MerchantMapper merchantMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;

    public MerchantVoucherService(VoucherMapper voucherMapper,
                                  MerchantMapper merchantMapper,
                                  SeckillVoucherMapper seckillVoucherMapper) {
        this.voucherMapper = voucherMapper;
        this.merchantMapper = merchantMapper;
        this.seckillVoucherMapper = seckillVoucherMapper;
    }

    public Map<String, Object> getMerchantVouchers(Long userId, Integer status, Integer page, Integer size) {
        Merchant merchant = requireVerifiedMerchant(userId);
        List<Voucher> vouchers = voucherMapper.findAllByMerchantId(merchant.getId());
        if (status != null) {
            vouchers = vouchers.stream()
                    .filter(v -> Objects.equals(v.getStatus(), status))
                    .toList();
        }

        int total = vouchers.size();
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        int start = (safePage - 1) * safeSize;
        List<Voucher> pagedVouchers = start >= total
                ? List.of()
                : vouchers.subList(start, Math.min(start + safeSize, total));

        Map<String, Object> result = new HashMap<>();
        result.put("list", pagedVouchers.stream().map(this::buildVoucherView).toList());
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", total);
        return result;
    }

    public Map<String, Object> getVoucherDetail(Long userId, Long id) {
        requireVerifiedMerchant(userId);
        return buildVoucherView(requireVoucher(id));
    }

    @Transactional
    public Map<String, Object> createVoucher(Long userId, Map<String, Object> request) {
        Merchant merchant = requireVerifiedMerchant(userId);
        String voucherType = parseVoucherTypeStrict(request == null ? null : request.get("voucherType"));
        String title = parseString(request == null ? null : request.get("title"));
        String subTitle = parseString(request == null ? null : request.get("subTitle"));
        Integer stock = parseInteger(request == null ? null : request.get("stock"), null);
        Integer seckillStock = parseInteger(request == null ? null : request.get("seckillStock"), null);
        BigDecimal amount = parseDecimal(request == null ? null : request.get("amount"));
        BigDecimal payValue = parseDecimal(request == null ? null : request.get("payValue"));
        LocalDateTime beginTime = parseDateTime(request == null ? null : request.get("beginTime"));
        LocalDateTime endTime = parseDateTime(request == null ? null : request.get("endTime"));
        LocalDateTime seckillStartTime = parseDateTime(request == null ? null : request.get("seckillStartTime"));
        LocalDateTime seckillEndTime = parseDateTime(request == null ? null : request.get("seckillEndTime"));

        validateCommon(title, voucherType, amount, payValue);

        Voucher voucher = new Voucher();
        voucher.setMerchantId(merchant.getId());
        voucher.setTitle(title);
        voucher.setSubTitle(subTitle);
        voucher.setAmount(amount);
        voucher.setPayValue(payValue);
        voucher.setStatus(0);

        if (TYPE_SECKILL.equals(voucherType)) {
            applySeckillCreate(voucher, seckillStock, seckillStartTime, seckillEndTime);
        } else {
            applyGroupCreate(voucher, stock, beginTime, endTime);
        }

        voucherMapper.insertVoucher(voucher);
        if (TYPE_SECKILL.equals(voucherType)) {
            insertSeckillVoucher(voucher);
        }
        return buildVoucherView(voucherMapper.findById(voucher.getId()));
    }

    @Transactional
    public Map<String, Object> updateVoucher(Long userId, Long id, Map<String, Object> request) {
        requireVerifiedMerchant(userId);
        Voucher voucher = requireVoucher(id);
        SeckillVoucher currentSeckill = seckillVoucherMapper.findByVoucherId(id);
        String voucherType = resolveUpdateVoucherType(request, currentSeckill);

        String title = parseString(request == null ? null : request.get("title"));
        String subTitle = parseString(request == null ? null : request.get("subTitle"));
        Integer stock = parseInteger(request == null ? null : request.get("stock"), null);
        Integer seckillStock = parseInteger(request == null ? null : request.get("seckillStock"), null);
        BigDecimal amount = parseDecimal(request == null ? null : request.get("amount"));
        BigDecimal payValue = parseDecimal(request == null ? null : request.get("payValue"));
        LocalDateTime beginTime = parseDateTime(request == null ? null : request.get("beginTime"));
        LocalDateTime endTime = parseDateTime(request == null ? null : request.get("endTime"));
        LocalDateTime seckillStartTime = parseDateTime(request == null ? null : request.get("seckillStartTime"));
        LocalDateTime seckillEndTime = parseDateTime(request == null ? null : request.get("seckillEndTime"));

        applyCommonUpdate(voucher, title, subTitle, amount, payValue);
        if (TYPE_SECKILL.equals(voucherType)) {
            upsertSeckillVoucher(voucher, currentSeckill, stock, seckillStock, beginTime, endTime,
                    seckillStartTime, seckillEndTime);
        } else {
            updateGroupVoucher(voucher, currentSeckill, stock, beginTime, endTime);
        }
        return buildVoucherView(voucherMapper.findById(voucher.getId()));
    }

    @Transactional
    public Voucher updateStatus(Long userId, Long id, Integer status) {
        requireVerifiedMerchant(userId);
        Voucher voucher = requireVoucher(id);
        voucher.setStatus(status);
        voucherMapper.updateVoucher(voucher);
        return voucher;
    }

    @Transactional
    public void deleteVoucher(Long userId, Long id) {
        updateStatus(userId, id, 0);
    }

    private Merchant requireVerifiedMerchant(Long userId) {
        Merchant merchant = merchantMapper.findByUserId(userId);
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new ForbiddenException("商家认证未通过，暂不可使用商品管理");
        }
        return merchant;
    }

    private Voucher requireVoucher(Long id) {
        Voucher voucher = voucherMapper.findById(id);
        if (voucher == null) {
            throw new NotFoundException("优惠券不存在");
        }
        return voucher;
    }

    private void validateCommon(String title, String voucherType, BigDecimal amount, BigDecimal payValue) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("标题不能为空");
        }
        if (voucherType == null) {
            throw new ValidationException("券类型不能为空，且只能为 group 或 seckill");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("面值必须大于0");
        }
        if (payValue != null && payValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("支付金额不能小于0");
        }
        if (payValue != null && payValue.compareTo(amount) > 0) {
            throw new ValidationException("支付金额不能大于面值");
        }
    }

    private void applySeckillCreate(Voucher voucher, Integer seckillStock,
                                    LocalDateTime seckillStartTime, LocalDateTime seckillEndTime) {
        if (seckillStock == null || seckillStock <= 0) {
            throw new ValidationException("秒杀券库存必须大于0");
        }
        if (seckillStartTime == null || seckillEndTime == null) {
            throw new ValidationException("秒杀券必须填写开始和结束时间");
        }
        if (!seckillStartTime.isBefore(seckillEndTime)) {
            throw new ValidationException("秒杀开始时间必须早于结束时间");
        }
        voucher.setStock(seckillStock);
        voucher.setBeginTime(seckillStartTime);
        voucher.setEndTime(seckillEndTime);
    }

    private void applyGroupCreate(Voucher voucher, Integer stock, LocalDateTime beginTime, LocalDateTime endTime) {
        if (stock == null || stock < 0) {
            throw new ValidationException("团购券库存必须大于等于0");
        }
        if (endTime == null) {
            throw new ValidationException("团购券必须填写截止时间");
        }
        if (beginTime != null && beginTime.isAfter(endTime)) {
            throw new ValidationException("开始时间不能晚于结束时间");
        }
        voucher.setStock(stock);
        voucher.setBeginTime(beginTime);
        voucher.setEndTime(endTime);
    }

    private String resolveUpdateVoucherType(Map<String, Object> request, SeckillVoucher currentSeckill) {
        String oldType = currentSeckill == null ? TYPE_GROUP : TYPE_SECKILL;
        String voucherTypeRaw = parseString(request == null ? null : request.get("voucherType"));
        if (voucherTypeRaw == null || voucherTypeRaw.isBlank()) {
            return oldType;
        }
        String voucherType = parseVoucherTypeStrict(voucherTypeRaw);
        if (voucherType == null) {
            throw new ValidationException("券类型非法，只能为 group 或 seckill");
        }
        return voucherType;
    }

    private void applyCommonUpdate(Voucher voucher, String title, String subTitle,
                                   BigDecimal amount, BigDecimal payValue) {
        if (title != null && title.isBlank()) {
            throw new ValidationException("标题不能为空");
        }
        if (title != null) {
            voucher.setTitle(title);
        }
        voucher.setSubTitle(subTitle);

        if (amount != null) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("面值必须大于0");
            }
            voucher.setAmount(amount);
        }
        if (payValue != null) {
            if (payValue.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("支付金额不能小于0");
            }
            voucher.setPayValue(payValue);
        }
        if (voucher.getPayValue() != null
                && voucher.getAmount() != null
                && voucher.getPayValue().compareTo(voucher.getAmount()) > 0) {
            throw new ValidationException("支付金额不能大于面值");
        }
    }

    private void upsertSeckillVoucher(Voucher voucher, SeckillVoucher currentSeckill,
                                      Integer stock, Integer seckillStock,
                                      LocalDateTime beginTime, LocalDateTime endTime,
                                      LocalDateTime seckillStartTime, LocalDateTime seckillEndTime) {
        Integer targetStock = seckillStock != null ? seckillStock : (stock != null ? stock : voucher.getStock());
        LocalDateTime targetStart = seckillStartTime != null
                ? seckillStartTime
                : (beginTime != null ? beginTime : voucher.getBeginTime());
        LocalDateTime targetEnd = seckillEndTime != null
                ? seckillEndTime
                : (endTime != null ? endTime : voucher.getEndTime());

        if (targetStock == null || targetStock <= 0) {
            throw new ValidationException("秒杀券库存必须大于0");
        }
        if (targetStart == null || targetEnd == null) {
            throw new ValidationException("秒杀券必须填写开始和结束时间");
        }
        if (!targetStart.isBefore(targetEnd)) {
            throw new ValidationException("秒杀开始时间必须早于结束时间");
        }

        voucher.setStock(targetStock);
        voucher.setBeginTime(targetStart);
        voucher.setEndTime(targetEnd);
        voucherMapper.updateVoucher(voucher);

        if (currentSeckill == null) {
            currentSeckill = new SeckillVoucher();
            currentSeckill.setVoucherId(voucher.getId());
            currentSeckill.setStock(targetStock);
            currentSeckill.setStartTime(targetStart);
            currentSeckill.setEndTime(targetEnd);
            if (seckillVoucherMapper.insertSeckillVoucher(currentSeckill) <= 0) {
                throw new BusinessException(BusinessErrorCode.SECKILL_VOUCHER_CREATE_FAILED);
            }
            return;
        }

        currentSeckill.setStock(targetStock);
        currentSeckill.setStartTime(targetStart);
        currentSeckill.setEndTime(targetEnd);
        if (seckillVoucherMapper.updateSeckillVoucher(currentSeckill) <= 0) {
            throw new BusinessException(BusinessErrorCode.SECKILL_VOUCHER_UPDATE_FAILED);
        }
    }

    private void updateGroupVoucher(Voucher voucher, SeckillVoucher currentSeckill,
                                    Integer stock, LocalDateTime beginTime, LocalDateTime endTime) {
        if (stock != null) {
            if (stock < 0) {
                throw new ValidationException("团购券库存必须大于等于0");
            }
            voucher.setStock(stock);
        }
        if (beginTime != null) {
            voucher.setBeginTime(beginTime);
        }
        if (endTime != null) {
            voucher.setEndTime(endTime);
        }
        if (voucher.getBeginTime() != null
                && voucher.getEndTime() != null
                && voucher.getBeginTime().isAfter(voucher.getEndTime())) {
            throw new ValidationException("开始时间不能晚于结束时间");
        }
        voucherMapper.updateVoucher(voucher);

        if (currentSeckill != null) {
            seckillVoucherMapper.deleteByVoucherId(voucher.getId());
        }
    }

    private void insertSeckillVoucher(Voucher voucher) {
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setStartTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        if (seckillVoucherMapper.insertSeckillVoucher(seckillVoucher) <= 0) {
            throw new BusinessException(BusinessErrorCode.SECKILL_VOUCHER_CREATE_FAILED);
        }
    }

    private Map<String, Object> buildVoucherView(Voucher voucher) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", voucher.getId());
        item.put("merchantId", voucher.getMerchantId());
        item.put("title", voucher.getTitle());
        item.put("subTitle", voucher.getSubTitle());
        item.put("imageUrl", voucher.getImageUrl());
        item.put("stock", voucher.getStock());
        item.put("soldCount", voucher.getSoldCount());
        item.put("amount", voucher.getAmount());
        item.put("payValue", voucher.getPayValue());
        item.put("status", voucher.getStatus());
        item.put("beginTime", voucher.getBeginTime());
        item.put("endTime", voucher.getEndTime());
        item.put("createTime", voucher.getCreateTime());
        item.put("updateTime", voucher.getUpdateTime());

        SeckillVoucher seckill = seckillVoucherMapper.findByVoucherId(voucher.getId());
        if (seckill != null) {
            item.put("voucherType", TYPE_SECKILL);
            item.put("seckillStock", seckill.getStock());
            item.put("seckillStartTime", seckill.getStartTime());
            item.put("seckillEndTime", seckill.getEndTime());
        } else {
            item.put("voucherType", TYPE_GROUP);
            item.put("seckillStock", null);
            item.put("seckillStartTime", null);
            item.put("seckillEndTime", null);
        }
        return item;
    }

    private String parseVoucherTypeStrict(Object value) {
        String str = parseString(value);
        if (str == null || str.isBlank()) {
            return null;
        }
        String lowered = str.trim().toLowerCase();
        if (TYPE_GROUP.equals(lowered) || TYPE_SECKILL.equals(lowered)) {
            return lowered;
        }
        return null;
    }

    private String parseString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            if (value instanceof Number num) {
                return num.intValue();
            }
            String str = String.valueOf(value).trim();
            if (str.isEmpty()) {
                return defaultValue;
            }
            return Integer.parseInt(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String str = String.valueOf(value).trim();
            if (str.isEmpty()) {
                return null;
            }
            return new BigDecimal(str);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            return null;
        }
        String normalized = raw.replace("T", " ");
        if (normalized.length() == 16) {
            normalized += ":00";
        }
        return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static class ForbiddenException extends BusinessException {
        public ForbiddenException(String message) {
            super(BusinessErrorCode.FORBIDDEN, message);
        }
    }

    public static class NotFoundException extends BusinessException {
        public NotFoundException(String message) {
            super(BusinessErrorCode.VOUCHER_NOT_FOUND, message);
        }
    }

    public static class ValidationException extends BusinessException {
        public ValidationException(String message) {
            super(BusinessErrorCode.INVALID_PARAM, message);
        }
    }
}
