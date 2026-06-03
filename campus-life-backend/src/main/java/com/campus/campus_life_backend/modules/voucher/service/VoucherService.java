package com.campus.campus_life_backend.modules.voucher.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import com.campus.campus_life_backend.modules.order.mapper.VoucherOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class VoucherService {

    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_CANCELLED = 5;
    private static final int DEFAULT_EXPIRE_SCAN_LIMIT = 100;
    private static final int DEFAULT_AUTO_OFFLINE_SCAN_LIMIT = 100;

    private final VoucherMapper voucherMapper;
    private final VoucherOrderMapper voucherOrderMapper;
    private final MerchantMapper merchantMapper;
    private final VoucherOrderService voucherOrderService;
    private final WelfareQueryService welfareQueryService;

    public record AdminVoucherStatusChange(Integer oldStatus, Integer newStatus) {
    }

    public record AdminVoucherCreateResult(Long id, String title, Integer status) {
    }

    public record AdminVoucherUpdateResult(Integer oldStatus, Integer newStatus) {
    }

    public VoucherService(
            VoucherMapper voucherMapper,
            VoucherOrderMapper voucherOrderMapper,
            MerchantMapper merchantMapper,
            VoucherOrderService voucherOrderService,
            WelfareQueryService welfareQueryService
    ) {
        this.voucherMapper = voucherMapper;
        this.voucherOrderMapper = voucherOrderMapper;
        this.merchantMapper = merchantMapper;
        this.voucherOrderService = voucherOrderService;
        this.welfareQueryService = welfareQueryService;
    }

    public List<Voucher> getAvailableVouchers(Integer type) {
        return voucherMapper.findAvailable();
    }

    public List<Voucher> getVouchersByMerchantId(Long merchantId) {
        return voucherMapper.findByMerchantId(merchantId);
    }

    public Voucher getVoucherById(Long id) {
        return voucherMapper.findById(id);
    }

    @Transactional
    public AdminVoucherStatusChange adminUpdateVoucherStatus(Long id, Integer status) {
        Voucher voucher = voucherMapper.findById(id);
        if (voucher == null) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_NOT_FOUND);
        }
        Integer oldStatus = voucher.getStatus();
        voucher.setStatus(status);
        voucherMapper.updateVoucher(voucher);
        welfareQueryService.evictWelfareCache();
        return new AdminVoucherStatusChange(oldStatus, status);
    }

    @Transactional
    public AdminVoucherCreateResult adminCreateVoucher(Map<String, Object> request) {
        Voucher voucher = buildVoucherForCreate(request);
        voucherMapper.insertVoucher(voucher);
        welfareQueryService.evictWelfareCache();
        return new AdminVoucherCreateResult(voucher.getId(), voucher.getTitle(), voucher.getStatus());
    }

    @Transactional
    public AdminVoucherUpdateResult adminUpdateVoucher(Long voucherId, Map<String, Object> request) {
        Voucher voucher = voucherMapper.findById(voucherId);
        if (voucher == null) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_NOT_FOUND);
        }
        Integer oldStatus = voucher.getStatus();
        applyVoucherUpdate(voucher, request);
        voucherMapper.updateVoucher(voucher);
        welfareQueryService.evictWelfareCache();
        return new AdminVoucherUpdateResult(oldStatus, voucher.getStatus());
    }

    @Transactional
    public int autoOfflineExpiredVouchers(int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_AUTO_OFFLINE_SCAN_LIMIT : limit;
        int offlineCount = voucherMapper.markExpiredAsOffline(safeLimit);
        if (offlineCount > 0) {
            welfareQueryService.evictWelfareCache();
        }
        return offlineCount;
    }

    @Transactional
    public VoucherOrder claimVoucher(Long voucherId, Long userId) {
        Voucher voucher = voucherMapper.findById(voucherId);
        if (voucher == null) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_NOT_FOUND);
        }
        if (voucher.getStock() <= 0) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_SOLD_OUT);
        }
        if (voucher.getStatus() != 1) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_OFFLINE);
        }
        
        // 一人一单：仅拦截未取消订单，已取消后允许重新下单。
        if (voucherOrderMapper.countActiveByUserAndVoucher(userId, voucherId) > 0) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_DUPLICATE_ORDER);
        }
        
        VoucherOrder order = voucherOrderService.createOrder(
                userId,
                voucherId,
                voucher.getPayValue(),
                "app",
                LocalDateTime.now().plusMinutes(30),
                voucher.getEndTime(),
                null
        );
        
        // 娉ㄦ剰锛氳繖閲屼笉绔嬪嵆鍑忓皯搴撳瓨锛岀瓑鏀粯鎴愬姛鍚庡啀鍑忓皯
        // 濡傛灉璁㈠崟瓒呮椂鏈敮浠橈紝闇€瑕侀噴鏀惧簱瀛?
        
        return order;
    }

    public List<VoucherOrder> getMyVouchers(Long userId, Integer status) {
        List<VoucherOrder> orders = voucherOrderMapper.findByUserId(userId);
        if (status != null) {
            orders = orders.stream()
                .filter(order -> order.getStatus().equals(status))
                .toList();
        }
        return orders;
    }

    public Map<String, Object> getMyOrderViews(Long userId, Integer status, Integer page, Integer size) {
        closeExpiredPendingOrdersForUser(userId, DEFAULT_EXPIRE_SCAN_LIMIT);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : Math.min(size, 50);
        int offset = (safePage - 1) * safeSize;
        List<Map<String, Object>> list = voucherOrderMapper.findOrderViewsByUserId(userId, status, offset, safeSize);
        list.forEach(this::appendStatusFields);

        Integer total = voucherOrderMapper.countOrderViewsByUserId(userId, status);
        return Map.of(
                "list", list,
                "page", safePage,
                "size", safeSize,
                "total", total == null ? 0 : total
        );
    }

    public Map<String, Object> getOrderDetailById(Long userId, Long id) {
        closeExpiredPendingOrdersForUser(userId, DEFAULT_EXPIRE_SCAN_LIMIT);
        Map<String, Object> detail = voucherOrderMapper.findOrderViewByIdAndUserId(id, userId);
        if (detail == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        appendStatusFields(detail);
        return detail;
    }

    public Map<String, Object> getOrderDetailByOrderNo(Long userId, String orderNo) {
        closeExpiredPendingOrdersForUser(userId, DEFAULT_EXPIRE_SCAN_LIMIT);
        Map<String, Object> detail = voucherOrderMapper.findOrderViewByOrderNoAndUserId(orderNo, userId);
        if (detail == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        appendStatusFields(detail);
        return detail;
    }

    private void appendStatusFields(Map<String, Object> order) {
        int status = 0;
        Object statusObj = order.get("status");
        if (statusObj instanceof Number) {
            status = ((Number) statusObj).intValue();
        }
        order.put("statusText", toStatusText(status));
        order.put("statusClass", toStatusClass(status));
        Long remainingSeconds = computeRemainingSeconds(order.get("payDeadline"), status);
        if (remainingSeconds != null) {
            order.put("remainingSeconds", remainingSeconds);
        }
    }

    @Transactional
    public void cancelOrder(Long userId, Long id) {
        VoucherOrder order = voucherOrderMapper.findById(id);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_PENDING) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_CANCELABLE);
        }
        if (!voucherOrderService.cancelOrder(id, userId, "用户主动取消")) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_CANCELABLE);
        }
    }

    @Transactional
    public int closeExpiredPendingOrdersBatch(int limit) {
        return voucherOrderService.closeExpiredPendingOrders(limit);
    }

    @Transactional
    public int closeExpiredPendingOrdersForUser(Long userId, int limit) {
        return voucherOrderService.closeExpiredPendingOrdersForUser(userId, limit);
    }

    private Long computeRemainingSeconds(Object payDeadlineObj, int status) {
        if (status != 0 || payDeadlineObj == null) {
            return null;
        }
        LocalDateTime deadline = null;
        if (payDeadlineObj instanceof LocalDateTime) {
            deadline = (LocalDateTime) payDeadlineObj;
        } else {
            try {
                deadline = LocalDateTime.parse(String.valueOf(payDeadlineObj).replace(" ", "T"));
            } catch (Exception ignored) {
            }
        }
        if (deadline == null) return null;
        long seconds = java.time.Duration.between(LocalDateTime.now(), deadline).getSeconds();
        return Math.max(seconds, 0);
    }

    private String toStatusText(int status) {
        if (status == 0) return "待支付";
        if (status == 1) return "待使用";
        if (status == 2) return "已完成";
        if (status == 3) return "已过期";
        if (status == 4) return "已退款";
        if (status == ORDER_STATUS_CANCELLED) return "已取消";
        return "未知状态";
    }

    private String toStatusClass(int status) {
        if (status == 0) return "pending";
        if (status == 1) return "ongoing";
        if (status == 2) return "success";
        if (status == 3) return "expired";
        if (status == 4) return "refund";
        if (status == 5) return "cancelled";
        return "unknown";
    }

    private Voucher buildVoucherForCreate(Map<String, Object> request) {
        Long merchantId = parseLong(request == null ? null : request.get("merchantId"), null);
        String title = trimString(request == null ? null : request.get("title"));
        String subTitle = trimString(request == null ? null : request.get("subTitle"));
        String imageUrl = trimString(request == null ? null : request.get("imageUrl"));
        Integer stock = parseInteger(request == null ? null : request.get("stock"), null);
        BigDecimal amount = parseBigDecimal(request == null ? null : request.get("amount"), null);
        BigDecimal payValue = parseBigDecimal(request == null ? null : request.get("payValue"), null);
        Integer status = parseInteger(request == null ? null : request.get("status"), 1);
        LocalDateTime beginTime = parseDateTime(request == null ? null : request.get("beginTime"));
        LocalDateTime endTime = parseDateTime(request == null ? null : request.get("endTime"));

        validateVoucherForCreate(merchantId, title, stock, amount, payValue, beginTime, endTime);

        Voucher voucher = new Voucher();
        voucher.setMerchantId(merchantId);
        voucher.setTitle(title);
        voucher.setSubTitle(isBlank(subTitle) ? null : subTitle);
        voucher.setImageUrl(isBlank(imageUrl) ? null : imageUrl);
        voucher.setStock(stock);
        voucher.setAmount(amount);
        voucher.setPayValue(payValue);
        voucher.setStatus(status == null ? 1 : status);
        voucher.setBeginTime(beginTime);
        voucher.setEndTime(endTime);
        return voucher;
    }

    private void applyVoucherUpdate(Voucher voucher, Map<String, Object> request) {
        Long merchantId = parseLong(request == null ? null : request.get("merchantId"), null);
        String title = trimString(request == null ? null : request.get("title"));
        String subTitle = trimString(request == null ? null : request.get("subTitle"));
        String imageUrl = trimString(request == null ? null : request.get("imageUrl"));
        Integer stock = parseInteger(request == null ? null : request.get("stock"), null);
        BigDecimal amount = parseBigDecimal(request == null ? null : request.get("amount"), null);
        BigDecimal payValue = parseBigDecimal(request == null ? null : request.get("payValue"), null);
        Integer status = parseInteger(request == null ? null : request.get("status"), voucher.getStatus());
        LocalDateTime beginTime = parseDateTime(request == null ? null : request.get("beginTime"));
        LocalDateTime endTime = parseDateTime(request == null ? null : request.get("endTime"));

        if (merchantId != null) {
            requireMerchantExists(merchantId);
            voucher.setMerchantId(merchantId);
        }
        if (!isBlank(title)) {
            voucher.setTitle(title);
        }
        if (!isBlank(subTitle)) {
            voucher.setSubTitle(subTitle);
        }
        if (!isBlank(imageUrl)) {
            voucher.setImageUrl(imageUrl);
        }
        if (stock != null) {
            if (stock < 0) throw new BusinessException(BusinessErrorCode.VOUCHER_STOCK_INVALID);
            voucher.setStock(stock);
        }
        if (amount != null) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessException(BusinessErrorCode.VOUCHER_AMOUNT_INVALID);
            voucher.setAmount(amount);
        }
        if (payValue != null) {
            if (payValue.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException(BusinessErrorCode.VOUCHER_PAY_VALUE_INVALID);
            voucher.setPayValue(payValue);
        }
        if (voucher.getAmount() != null && voucher.getPayValue() != null
                && voucher.getPayValue().compareTo(voucher.getAmount()) > 0) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_PAY_VALUE_EXCEEDS_AMOUNT);
        }
        if (status != null) {
            voucher.setStatus(status);
        }
        if (beginTime != null) {
            voucher.setBeginTime(beginTime);
        }
        if (endTime != null) {
            voucher.setEndTime(endTime);
        }
        if (voucher.getBeginTime() != null && voucher.getEndTime() != null
                && voucher.getBeginTime().isAfter(voucher.getEndTime())) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_TIME_RANGE_INVALID);
        }
    }

    private void validateVoucherForCreate(Long merchantId, String title, Integer stock, BigDecimal amount,
                                          BigDecimal payValue, LocalDateTime beginTime, LocalDateTime endTime) {
        if (merchantId == null) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_NOT_FOUND);
        }
        requireMerchantExists(merchantId);
        if (isBlank(title)) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_TITLE_REQUIRED);
        }
        if (stock == null || stock < 0) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_STOCK_INVALID);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_AMOUNT_INVALID);
        }
        if (payValue == null || payValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_PAY_VALUE_INVALID);
        }
        if (payValue.compareTo(amount) > 0) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_PAY_VALUE_EXCEEDS_AMOUNT);
        }
        if (beginTime != null && endTime != null && beginTime.isAfter(endTime)) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_TIME_RANGE_INVALID);
        }
    }

    private void requireMerchantExists(Long merchantId) {
        if (merchantMapper.findById(merchantId) == null) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_NOT_FOUND);
        }
    }

    private String trimString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Long parseLong(Object value, Long defaultValue) {
        if (value == null) return defaultValue;
        try {
            if (value instanceof Number num) return num.longValue();
            String str = String.valueOf(value).trim();
            if (str.isEmpty()) return defaultValue;
            return Long.parseLong(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        try {
            if (value instanceof Number num) return num.intValue();
            String str = String.valueOf(value).trim();
            if (str.isEmpty()) return defaultValue;
            return Integer.parseInt(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal parseBigDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) return defaultValue;
        try {
            String str = String.valueOf(value).trim();
            if (str.isEmpty()) return defaultValue;
            return new BigDecimal(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        String str = String.valueOf(value).trim();
        if (str.isEmpty()) return null;
        try {
            return LocalDateTime.parse(str.replace(" ", "T"));
        } catch (Exception e) {
            return null;
        }
    }
}
