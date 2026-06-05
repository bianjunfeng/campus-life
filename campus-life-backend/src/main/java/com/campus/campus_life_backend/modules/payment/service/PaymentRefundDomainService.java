package com.campus.campus_life_backend.modules.payment.service;

import com.campus.campus_life_backend.modules.order.event.OrderRefundedEvent;
import com.campus.campus_life_backend.modules.order.event.VoucherOrderKafkaEventPublisher;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRefundResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.entity.PaymentRefundOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentBizType;
import com.campus.campus_life_backend.modules.payment.enums.PaymentOrderStatus;
import com.campus.campus_life_backend.modules.payment.enums.PaymentRefundStatus;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentOrderMapper;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentRefundOrderMapper;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@Service
public class PaymentRefundDomainService {

    private static final DateTimeFormatter REFUND_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper paymentRefundOrderMapper;
    private final VoucherOrderService voucherOrderService;
    private final VoucherOrderKafkaEventPublisher voucherOrderKafkaEventPublisher;

    public PaymentRefundDomainService(
            PaymentOrderMapper paymentOrderMapper,
            PaymentRefundOrderMapper paymentRefundOrderMapper,
            VoucherOrderService voucherOrderService,
            VoucherOrderKafkaEventPublisher voucherOrderKafkaEventPublisher
    ) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentRefundOrderMapper = paymentRefundOrderMapper;
        this.voucherOrderService = voucherOrderService;
        this.voucherOrderKafkaEventPublisher = voucherOrderKafkaEventPublisher;
    }

    public PaymentRefundOrder createVoucherRefundOrder(
            PaymentOrder paymentOrder,
            Long userId,
            BigDecimal refundAmount,
            String reason,
            String requestIdempotencyKey
    ) {
        PaymentRefundOrder existing = paymentRefundOrderMapper.findByRequestIdempotencyKey(requestIdempotencyKey);
        if (existing != null) {
            if (PaymentRefundStatus.FAILED.getCode().equalsIgnoreCase(existing.getStatus())
                    || PaymentRefundStatus.CLOSED.getCode().equalsIgnoreCase(existing.getStatus())) {
                requestIdempotencyKey = requestIdempotencyKey + ":retry:" + System.currentTimeMillis();
            } else {
            return existing;
            }
        }

        BigDecimal refundedAmount = paymentRefundOrderMapper.sumSuccessfulRefundAmountByPaymentNo(paymentOrder.getPaymentNo());
        BigDecimal alreadyRefunded = refundedAmount == null ? BigDecimal.ZERO : refundedAmount;
        BigDecimal targetAmount = alreadyRefunded.add(refundAmount == null ? BigDecimal.ZERO : refundAmount);
        if (targetAmount.compareTo(paymentOrder.getAmount()) > 0) {
            throw new IllegalArgumentException("退款金额超过可退金额");
        }

        PaymentRefundOrder refundOrder = new PaymentRefundOrder();
        refundOrder.setRefundNo(generateRefundNo());
        refundOrder.setPaymentNo(paymentOrder.getPaymentNo());
        refundOrder.setBizType(PaymentBizType.VOUCHER.getCode());
        refundOrder.setBizOrderNo(paymentOrder.getBizOrderNo());
        refundOrder.setUserId(userId);
        refundOrder.setChannel(paymentOrder.getChannel());
        refundOrder.setRefundAmount(refundAmount);
        refundOrder.setReason(reason);
        refundOrder.setStatus(PaymentRefundStatus.INIT.getCode());
        refundOrder.setRequestIdempotencyKey(requestIdempotencyKey);
        paymentRefundOrderMapper.insert(refundOrder);
        return refundOrder;
    }

    @Transactional
    public PaymentRefundResponse markRefundSuccess(PaymentRefundOrder refundOrder, VoucherOrder voucherOrder) {
        int updated = paymentRefundOrderMapper.markSuccess(
                refundOrder.getRefundNo(),
                refundOrder.getChannelRefundNo(),
                LocalDateTime.now(),
                PaymentRefundStatus.PROCESSING.getCode()
        );
        if (updated <= 0) {
            throw new IllegalStateException("退款单状态已变更，请刷新后重试");
        }

        BigDecimal refundedAmount = paymentRefundOrderMapper.sumSuccessfulRefundAmountByPaymentNo(refundOrder.getPaymentNo());
        PaymentOrder paymentOrder = paymentOrderMapper.findByPaymentNo(refundOrder.getPaymentNo());
        if (paymentOrder == null) {
            throw new IllegalStateException("支付单不存在");
        }

        String nextPaymentStatus = refundedAmount.compareTo(paymentOrder.getAmount()) >= 0
                ? PaymentOrderStatus.FULL_REFUNDED.getCode()
                : PaymentOrderStatus.PARTIAL_REFUNDED.getCode();
        paymentOrderMapper.markRefunded(refundOrder.getPaymentNo(), refundedAmount, nextPaymentStatus);

        if (PaymentOrderStatus.FULL_REFUNDED.getCode().equals(nextPaymentStatus)
                && voucherOrder != null
                && voucherOrder.getId() != null) {
            if (voucherOrderKafkaEventPublisher.isEnabled()) {
                publishOrderRefundedEvent(voucherOrder, refundOrder, paymentOrder.getPaymentNo(), "refund_success");
            } else {
                voucherOrderService.markOrderRefunded(voucherOrder.getId(), refundOrder.getReason());
            }
        }

        PaymentRefundResponse response = new PaymentRefundResponse();
        response.setRefundNo(refundOrder.getRefundNo());
        response.setPaymentNo(refundOrder.getPaymentNo());
        response.setRefundAmount(refundOrder.getRefundAmount());
        response.setRequestedAmount(refundOrder.getRequestedAmount());
        response.setApprovedAmount(refundOrder.getApprovedAmount() == null ? refundOrder.getRefundAmount() : refundOrder.getApprovedAmount());
        response.setStatus(PaymentRefundStatus.SUCCESS.getCode());
        return response;
    }

    public void markRefundFailed(PaymentRefundOrder refundOrder) {
        paymentRefundOrderMapper.markFailed(refundOrder.getRefundNo(), PaymentRefundStatus.PROCESSING.getCode());
    }

    private String generateRefundNo() {
        return "R" + LocalDateTime.now().format(REFUND_NO_FORMATTER)
                + ThreadLocalRandom.current().nextInt(100000, 999999);
    }

    private void publishOrderRefundedEvent(
            VoucherOrder voucherOrder,
            PaymentRefundOrder refundOrder,
            String paymentNo,
            String source
    ) {
        if (voucherOrder == null || voucherOrder.getId() == null || refundOrder == null) {
            return;
        }
        OrderRefundedEvent event = new OrderRefundedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrderId(voucherOrder.getId());
        event.setOrderNo(voucherOrder.getOrderNo());
        event.setPaymentNo(paymentNo);
        event.setUserId(voucherOrder.getUserId());
        event.setReason(refundOrder.getReason());
        event.setRequestIdempotencyKey(refundOrder.getRequestIdempotencyKey());
        event.setEventTime(LocalDateTime.now());
        event.setSource(source);
        voucherOrderKafkaEventPublisher.publish(event);
    }
}
