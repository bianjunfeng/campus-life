package com.campus.campus_life_backend.modules.order.service.impl;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.order.event.DomainEventPublisher;
import com.campus.campus_life_backend.modules.order.event.DomainEventType;
import com.campus.campus_life_backend.modules.order.mapper.VoucherOrderMapper;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.voucher.mapper.SeckillVoucherMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import com.campus.campus_life_backend.modules.voucher.service.SeckillReservationService;
import com.campus.campus_life_backend.modules.voucher.service.WelfareQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VoucherOrderServiceImpl implements VoucherOrderService {

    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_PAID = 1;
    private static final int ORDER_STATUS_EXPIRED = 3;
    private static final int ORDER_STATUS_REFUNDED = 4;
    private static final int ORDER_STATUS_CANCELLED = 5;
    private static final int PAYMENT_STATUS_PENDING = 0;
    private static final int PAYMENT_STATUS_PAID = 1;
    private static final String AUTO_CLOSE_REASON = "支付超时自动取消";
    private static final int DEFAULT_EXPIRE_SCAN_LIMIT = 100;

    private final VoucherOrderMapper voucherOrderMapper;
    private final VoucherMapper voucherMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final SeckillReservationService seckillReservationService;
    private final WelfareQueryService welfareQueryService;
    private final DomainEventPublisher domainEventPublisher;

    public VoucherOrderServiceImpl(
            VoucherOrderMapper voucherOrderMapper,
            VoucherMapper voucherMapper,
            SeckillVoucherMapper seckillVoucherMapper,
            SeckillReservationService seckillReservationService,
            WelfareQueryService welfareQueryService,
            DomainEventPublisher domainEventPublisher
    ) {
        this.voucherOrderMapper = voucherOrderMapper;
        this.voucherMapper = voucherMapper;
        this.seckillVoucherMapper = seckillVoucherMapper;
        this.seckillReservationService = seckillReservationService;
        this.welfareQueryService = welfareQueryService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public VoucherOrder findById(Long id) {
        if (id == null) {
            return null;
        }
        return voucherOrderMapper.findById(id);
    }

    @Override
    public VoucherOrder findByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return null;
        }
        return voucherOrderMapper.findByOrderNo(orderNo);
    }

    @Override
    public List<VoucherOrder> findExpiredPaidOrdersForAutoRefund(LocalDateTime now, int limit) {
        LocalDateTime safeNow = now == null ? LocalDateTime.now() : now;
        int safeLimit = limit <= 0 ? 100 : Math.min(limit, 500);
        return voucherOrderMapper.findExpiredPaidOrdersForAutoRefund(safeNow, safeLimit);
    }

    @Override
    public boolean bindPaymentIdempotencyKeyIfPending(Long orderId, String idempotencyKey) {
        if (orderId == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        return voucherOrderMapper.bindPaymentIdempotencyKeyIfPending(orderId, idempotencyKey) > 0;
    }

    @Override
    @Transactional
    public VoucherOrder createOrder(Long userId,
                                    Long voucherId,
                                    BigDecimal payAmount,
                                    String orderSource,
                                    LocalDateTime payDeadline,
                                    LocalDateTime useDeadline,
                                    String orderNo) {
        if (userId == null || voucherId == null) {
            throw new IllegalArgumentException("用户和优惠券不能为空");
        }
        VoucherOrder order = new VoucherOrder();
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        order.setStatus(ORDER_STATUS_PENDING);
        order.setOrderNo(orderNo == null || orderNo.isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : orderNo);
        order.setPayAmount(payAmount);
        order.setPaymentStatus(PAYMENT_STATUS_PENDING);
        order.setOrderSource(orderSource == null || orderSource.isBlank() ? "app" : orderSource);
        order.setPayDeadline(payDeadline == null ? LocalDateTime.now().plusMinutes(30) : payDeadline);
        order.setUseDeadline(useDeadline);

        voucherOrderMapper.insertVoucherOrder(order);
        publishOrderCreated(order, "seckill".equalsIgnoreCase(order.getOrderSource())
                ? "seckill_consumer"
                : "voucher_claim");
        if ("seckill".equalsIgnoreCase(order.getOrderSource())) {
            publishVoucherStockChanged(order, -1, "seckill_order_created");
            welfareQueryService.evictWelfareCache();
        }
        return order;
    }

    @Override
    @Transactional
    public boolean markOrderPaid(Long orderId, String paymentMethod, LocalDateTime payTime) {
        VoucherOrder order = findById(orderId);
        if (order == null || order.getUpdateTime() == null) {
            return false;
        }
        if (isAlreadyPaid(order)) {
            return true;
        }
        if (!isPendingOrder(order)) {
            return false;
        }

        LocalDateTime actualPayTime = payTime == null ? LocalDateTime.now() : payTime;
        int updated = voucherOrderMapper.markOrderPaidWithVersion(
                order.getId(),
                paymentMethod,
                actualPayTime,
                order.getUpdateTime()
        );
        if (updated <= 0) {
            return false;
        }

        order.setStatus(ORDER_STATUS_PAID);
        order.setPaymentStatus(PAYMENT_STATUS_PAID);
        order.setPaymentMethod(paymentMethod);
        order.setPayTime(actualPayTime);

        if (order.getVoucherId() != null) {
            boolean seckillOrder = "seckill".equalsIgnoreCase(String.valueOf(order.getOrderSource()));
            if (!seckillOrder) {
                int stockUpdated = voucherMapper.decrementStockIfAvailable(order.getVoucherId());
                if (stockUpdated <= 0) {
                    throw new BusinessException(BusinessErrorCode.VOUCHER_STOCK_DEDUCT_FAILED);
                }
                publishVoucherStockChanged(order, -1, "payment_success_decrement");
            }
            int soldUpdated = voucherMapper.incrementSoldCount(order.getVoucherId());
            if (soldUpdated <= 0) {
                throw new BusinessException(BusinessErrorCode.VOUCHER_SALES_UPDATE_FAILED);
            }
            welfareQueryService.evictWelfareCache();
        }

        publishPaymentSucceeded(order, paymentMethod);
        return true;
    }

    @Override
    @Transactional
    public boolean markOrderPaymentFailed(Long orderId) {
        VoucherOrder order = findById(orderId);
        if (order == null || order.getUpdateTime() == null) {
            return false;
        }
        if (!isPendingOrder(order)) {
            return true;
        }
        return voucherOrderMapper.markOrderPaymentFailedWithVersion(order.getId(), order.getUpdateTime()) > 0;
    }

    @Override
    @Transactional
    public boolean markOrderExpired(Long orderId, String reason, LocalDateTime now) {
        VoucherOrder order = findById(orderId);
        if (order == null || order.getUpdateTime() == null) {
            return false;
        }
        if (order.getStatus() != null && order.getStatus() == ORDER_STATUS_EXPIRED) {
            return true;
        }
        LocalDateTime safeNow = now == null ? LocalDateTime.now() : now;
        return voucherOrderMapper.markOrderExpiredWithVersion(
                order.getId(),
                reason,
                safeNow,
                order.getUpdateTime()
        ) > 0;
    }

    @Override
    @Transactional
    public boolean markOrderRefunded(Long orderId, String reason) {
        VoucherOrder order = findById(orderId);
        if (order == null || order.getUpdateTime() == null) {
            return false;
        }
        if (order.getStatus() != null && order.getStatus() == ORDER_STATUS_REFUNDED) {
            return true;
        }
        return voucherOrderMapper.markOrderRefundedWithVersion(
                order.getId(),
                reason,
                order.getUpdateTime()
        ) > 0;
    }

    @Override
    @Transactional
    public boolean cancelOrder(Long orderId, Long userId, String reason) {
        VoucherOrder order = findById(orderId);
        if (order == null || userId == null || !userId.equals(order.getUserId())) {
            return false;
        }
        if (!isPendingOrder(order)) {
            return false;
        }

        int updated = voucherOrderMapper.cancelPendingOrderByUserAndIdWithVersion(
                orderId,
                userId,
                reason,
                order.getUpdateTime()
        );
        if (updated <= 0) {
            return false;
        }

        afterPendingOrderClosed(order, reason, "manual_cancel_restore");
        welfareQueryService.evictWelfareCache();
        return true;
    }

    @Override
    @Transactional
    public int closeExpiredPendingOrders(int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_EXPIRE_SCAN_LIMIT : Math.min(limit, 500);
        List<VoucherOrder> expiredOrders = voucherOrderMapper.findExpiredPendingOrders(safeLimit);
        if (expiredOrders == null || expiredOrders.isEmpty()) {
            return 0;
        }
        int closed = 0;
        for (VoucherOrder order : expiredOrders) {
            closed += closePendingOrderInternal(order, AUTO_CLOSE_REASON, "auto_close_restore") ? 1 : 0;
        }
        if (closed > 0) {
            welfareQueryService.evictWelfareCache();
        }
        return closed;
    }

    @Override
    @Transactional
    public int closeExpiredPendingOrdersForUser(Long userId, int limit) {
        if (userId == null) {
            return 0;
        }
        int safeLimit = limit <= 0 ? DEFAULT_EXPIRE_SCAN_LIMIT : Math.min(limit, 200);
        List<VoucherOrder> expiredOrders = voucherOrderMapper.findExpiredPendingOrdersByUser(userId, safeLimit);
        if (expiredOrders == null || expiredOrders.isEmpty()) {
            return 0;
        }
        int closed = 0;
        for (VoucherOrder order : expiredOrders) {
            closed += closePendingOrderInternal(order, AUTO_CLOSE_REASON, "auto_close_restore") ? 1 : 0;
        }
        if (closed > 0) {
            welfareQueryService.evictWelfareCache();
        }
        return closed;
    }

    private boolean closePendingOrderInternal(VoucherOrder order, String reason, String stockChangeReason) {
        if (order == null || order.getId() == null || order.getUpdateTime() == null) {
            return false;
        }
        int updated = voucherOrderMapper.cancelPendingOrderByIdWithVersion(
                order.getId(),
                reason,
                order.getUpdateTime()
        );
        if (updated <= 0) {
            return false;
        }
        afterPendingOrderClosed(order, reason, stockChangeReason);
        return true;
    }

    private void afterPendingOrderClosed(VoucherOrder order, String reason, String stockChangeReason) {
        if (order == null) {
            return;
        }
        if ("seckill".equalsIgnoreCase(String.valueOf(order.getOrderSource()))) {
            voucherMapper.incrementStock(order.getVoucherId());
            seckillVoucherMapper.incrementStockByVoucherId(order.getVoucherId());
            seckillReservationService.rollbackReservation(order.getVoucherId(), order.getUserId());
            publishVoucherStockChanged(order, 1, stockChangeReason);
        }
        publishOrderClosed(order, reason);
    }

    private void publishOrderCreated(VoucherOrder order, String source) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId());
        payload.put("orderNo", order.getOrderNo());
        payload.put("userId", order.getUserId());
        payload.put("voucherId", order.getVoucherId());
        payload.put("payAmount", order.getPayAmount());
        payload.put("source", source);
        domainEventPublisher.publish(
                DomainEventType.OrderCreated,
                String.valueOf(order.getOrderNo()),
                order.getOrderNo(),
                payload
        );
    }

    private boolean isPendingOrder(VoucherOrder order) {
        return order.getStatus() != null
                && order.getPaymentStatus() != null
                && order.getStatus() == ORDER_STATUS_PENDING
                && order.getPaymentStatus() == PAYMENT_STATUS_PENDING;
    }

    private boolean isAlreadyPaid(VoucherOrder order) {
        return order.getPaymentStatus() != null
                && order.getPaymentStatus() == PAYMENT_STATUS_PAID
                && order.getStatus() != null
                && order.getStatus() >= ORDER_STATUS_PAID
                && order.getStatus() != ORDER_STATUS_CANCELLED;
    }

    private void publishPaymentSucceeded(VoucherOrder order, String paymentMethod) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderNo", order.getOrderNo());
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUserId());
        payload.put("voucherId", order.getVoucherId());
        payload.put("payAmount", order.getPayAmount());
        payload.put("paymentMethod", paymentMethod);
        payload.put("orderSource", order.getOrderSource());
        domainEventPublisher.publish(
                DomainEventType.PaymentSucceeded,
                String.valueOf(order.getOrderNo()),
                order.getPaymentIdempotencyKey(),
                payload
        );
    }

    private void publishOrderClosed(VoucherOrder order, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId());
        payload.put("orderNo", order.getOrderNo());
        payload.put("userId", order.getUserId());
        payload.put("voucherId", order.getVoucherId());
        payload.put("reason", reason);
        payload.put("orderSource", order.getOrderSource());
        domainEventPublisher.publish(
                DomainEventType.OrderClosed,
                String.valueOf(order.getOrderNo()),
                order.getOrderNo(),
                payload
        );
    }

    private void publishVoucherStockChanged(VoucherOrder order, int delta, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("voucherId", order.getVoucherId());
        payload.put("orderNo", order.getOrderNo());
        payload.put("userId", order.getUserId());
        payload.put("delta", delta);
        payload.put("reason", reason);
        domainEventPublisher.publish(
                DomainEventType.VoucherStockChanged,
                String.valueOf(order.getVoucherId()),
                order.getOrderNo(),
                payload
        );
    }
}
