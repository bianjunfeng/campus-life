package com.campus.campus_life_backend.modules.order.service;

import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VoucherOrderService {

    VoucherOrder findById(Long id);

    VoucherOrder findByOrderNo(String orderNo);

    List<VoucherOrder> findExpiredPaidOrdersForAutoRefund(LocalDateTime now, int limit);

    boolean bindPaymentIdempotencyKeyIfPending(Long orderId, String idempotencyKey);

    VoucherOrder createOrder(Long userId,
                             Long voucherId,
                             BigDecimal payAmount,
                             String orderSource,
                             LocalDateTime payDeadline,
                             LocalDateTime useDeadline,
                             String orderNo);

    boolean markOrderPaid(Long orderId, String paymentMethod, LocalDateTime payTime);

    boolean markOrderPaymentFailed(Long orderId);

    boolean markOrderExpired(Long orderId, String reason, LocalDateTime now);

    boolean markOrderRefunded(Long orderId, String reason);

    boolean cancelOrder(Long orderId, Long userId, String reason);

    int closeExpiredPendingOrders(int limit);

    int closeExpiredPendingOrdersForUser(Long userId, int limit);
}
