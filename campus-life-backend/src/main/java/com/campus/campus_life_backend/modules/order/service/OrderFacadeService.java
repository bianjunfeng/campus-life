package com.campus.campus_life_backend.modules.order.service;

import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderFacadeService {

    private final VoucherOrderService voucherOrderService;
    private final VoucherMapper voucherMapper;

    public OrderFacadeService(VoucherOrderService voucherOrderService, VoucherMapper voucherMapper) {
        this.voucherOrderService = voucherOrderService;
        this.voucherMapper = voucherMapper;
    }

    public VoucherOrder findOrderByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return null;
        }
        return voucherOrderService.findByOrderNo(orderNo);
    }

    public VoucherOrder findOwnedOrderByOrderNo(String orderNo, Long userId) {
        VoucherOrder order = findOrderByOrderNo(orderNo);
        if (order == null || userId == null || !userId.equals(order.getUserId())) {
            return null;
        }
        return order;
    }

    public Map<String, Object> buildPaymentOrderInfo(VoucherOrder order) {
        Map<String, Object> result = new HashMap<>();
        if (order == null) {
            return result;
        }
        Voucher voucher = order.getVoucherId() != null ? voucherMapper.findById(order.getVoucherId()) : null;
        result.put("orderNo", order.getOrderNo());
        result.put("amount", order.getPayAmount());
        result.put("subject", voucher != null ? voucher.getTitle() : "优惠券订单");
        result.put("description", voucher != null ? voucher.getSubTitle() : "优惠券购买");
        result.put("paymentStatus", order.getPaymentStatus());
        result.put("orderStatus", order.getStatus());
        return result;
    }

    public BigDecimal getExpectedPayAmount(String orderNo) {
        VoucherOrder order = findOrderByOrderNo(orderNo);
        return order == null ? null : order.getPayAmount();
    }

    public boolean bindPaymentIdempotencyKeyIfPending(Long orderId, String idempotencyKey) {
        if (orderId == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        return voucherOrderService.bindPaymentIdempotencyKeyIfPending(orderId, idempotencyKey);
    }
}
