package com.campus.campus_life_backend.modules.payment.service;

import com.campus.campus_life_backend.modules.message.service.MessageNotificationService;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRefundResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.entity.PaymentRefundOrder;
import com.campus.campus_life_backend.modules.payment.entity.PaymentRefundReviewLog;
import com.campus.campus_life_backend.modules.payment.enums.PaymentBizType;
import com.campus.campus_life_backend.modules.payment.enums.PaymentRefundStatus;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentOrderMapper;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentRefundOrderMapper;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentRefundReviewLogMapper;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentAutoRefundService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAutoRefundService.class);
    private static final String AUTO_EXPIRE_REASON = "券到期自动失效";
    private static final String AUTO_REFUND_REASON = "券到期自动退款";
    private static final String REVIEWER_SYSTEM = "SYSTEM";
    private static final Long SYSTEM_ACTOR_USER_ID = 1L;
    private static final String AUTO_REFUND_IDEMPOTENCY_PREFIX = "AUTO_EXPIRE_REFUND:";
    private static final DateTimeFormatter REFUND_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final VoucherOrderService voucherOrderService;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper paymentRefundOrderMapper;
    private final PaymentRefundReviewLogMapper paymentRefundReviewLogMapper;
    private final PaymentRefundDomainService paymentRefundDomainService;
    private final PaymentService paymentService;
    private final MessageNotificationService messageNotificationService;
    private final VoucherMapper voucherMapper;
    private final TransactionTemplate transactionTemplate;

    public PaymentAutoRefundService(
            VoucherOrderService voucherOrderService,
            PaymentOrderMapper paymentOrderMapper,
            PaymentRefundOrderMapper paymentRefundOrderMapper,
            PaymentRefundReviewLogMapper paymentRefundReviewLogMapper,
            PaymentRefundDomainService paymentRefundDomainService,
            PaymentService paymentService,
            MessageNotificationService messageNotificationService,
            VoucherMapper voucherMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.voucherOrderService = voucherOrderService;
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentRefundOrderMapper = paymentRefundOrderMapper;
        this.paymentRefundReviewLogMapper = paymentRefundReviewLogMapper;
        this.paymentRefundDomainService = paymentRefundDomainService;
        this.paymentService = paymentService;
        this.messageNotificationService = messageNotificationService;
        this.voucherMapper = voucherMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public int autoRefundExpiredVoucherOrders(int limit) {
        int safeLimit = limit <= 0 ? 100 : Math.min(limit, 500);
        LocalDateTime now = LocalDateTime.now();
        List<VoucherOrder> candidates = voucherOrderService.findExpiredPaidOrdersForAutoRefund(now, safeLimit);
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }

        int refundedCount = 0;
        for (VoucherOrder candidate : candidates) {
            Boolean refunded = transactionTemplate.execute(status -> processSingleCandidate(candidate, now));
            if (Boolean.TRUE.equals(refunded)) {
                refundedCount++;
            }
        }
        return refundedCount;
    }

    private Boolean processSingleCandidate(VoucherOrder candidate, LocalDateTime now) {
        VoucherOrder latestOrder = voucherOrderService.findById(candidate.getId());
        if (!isAutoRefundCandidate(latestOrder, now)) {
            return false;
        }

        if (latestOrder.getStatus() != null && latestOrder.getStatus() == 1) {
            if (!voucherOrderService.markOrderExpired(latestOrder.getId(), AUTO_EXPIRE_REASON, now)) {
                return false;
            }
            latestOrder = voucherOrderService.findById(latestOrder.getId());
            if (!isAutoRefundCandidate(latestOrder, now)) {
                return false;
            }
        }

        PaymentOrder paymentOrder = paymentOrderMapper.findLatestByBizOrderNo(PaymentBizType.VOUCHER.getCode(), latestOrder.getOrderNo());
        if (paymentOrder == null) {
            log.warn("到期自动退款跳过，支付单不存在: orderNo={}", latestOrder.getOrderNo());
            return false;
        }

        BigDecimal refundedAmount = paymentRefundOrderMapper.sumSuccessfulRefundAmountByPaymentNo(paymentOrder.getPaymentNo());
        BigDecimal alreadyRefunded = refundedAmount == null ? BigDecimal.ZERO : refundedAmount;
        BigDecimal paymentAmount = paymentOrder.getAmount() == null ? BigDecimal.ZERO : paymentOrder.getAmount();
        BigDecimal remainingAmount = paymentAmount.subtract(alreadyRefunded);
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return finalizeOrderAsRefunded(latestOrder, AUTO_REFUND_REASON);
        }

        PaymentRefundOrder refundOrder = prepareAutoRefundOrder(paymentOrder, latestOrder, remainingAmount);
        if (refundOrder == null) {
            return false;
        }
        if (PaymentRefundStatus.SUCCESS.getCode().equalsIgnoreCase(refundOrder.getStatus())) {
            return finalizeOrderAsRefunded(latestOrder, AUTO_REFUND_REASON);
        }
        if (!PaymentRefundStatus.PROCESSING.getCode().equalsIgnoreCase(refundOrder.getStatus())) {
            log.warn("到期自动退款跳过，退款单状态不可执行: orderNo={}, refundNo={}, status={}",
                    latestOrder.getOrderNo(), refundOrder.getRefundNo(), refundOrder.getStatus());
            return false;
        }

        try {
            boolean success = paymentService.refund(
                    paymentOrder.getPaymentNo(),
                    refundOrder.getRefundAmount(),
                    refundOrder.getReason()
            );
            if (!success) {
                paymentRefundDomainService.markRefundFailed(refundOrder);
                insertReviewLog(refundOrder.getRefundNo(), "SYSTEM_AUTO_REFUND_FAILED",
                        PaymentRefundStatus.PROCESSING.getCode(), PaymentRefundStatus.FAILED.getCode(), "渠道退款失败");
                notifyUser(latestOrder.getUserId(), "AUTO_REFUND_FAILED",
                        String.format("订单%s 已过期，但自动退款失败，请联系平台处理", latestOrder.getOrderNo()));
                return false;
            }
            PaymentRefundResponse response = paymentRefundDomainService.markRefundSuccess(refundOrder, latestOrder);
            insertReviewLog(refundOrder.getRefundNo(), "SYSTEM_AUTO_REFUND_SUCCESS",
                    PaymentRefundStatus.PROCESSING.getCode(), PaymentRefundStatus.SUCCESS.getCode(), AUTO_REFUND_REASON);
            notifyUser(latestOrder.getUserId(), "AUTO_REFUND_SUCCESS",
                    String.format("订单%s 已过期，系统已自动退款 ¥%s", latestOrder.getOrderNo(), response.getRefundAmount()));
            return true;
        } catch (Exception e) {
            log.error("到期自动退款执行失败: orderNo={}, refundNo={}", latestOrder.getOrderNo(), refundOrder.getRefundNo(), e);
            paymentRefundDomainService.markRefundFailed(refundOrder);
            insertReviewLog(refundOrder.getRefundNo(), "SYSTEM_AUTO_REFUND_FAILED",
                    PaymentRefundStatus.PROCESSING.getCode(), PaymentRefundStatus.FAILED.getCode(), e.getMessage());
            notifyUser(latestOrder.getUserId(), "AUTO_REFUND_FAILED",
                    String.format("订单%s 已过期，但自动退款失败，请联系平台处理", latestOrder.getOrderNo()));
            return false;
        }
    }

    private PaymentRefundOrder prepareAutoRefundOrder(PaymentOrder paymentOrder, VoucherOrder voucherOrder, BigDecimal refundAmount) {
        String requestKey = AUTO_REFUND_IDEMPOTENCY_PREFIX + voucherOrder.getOrderNo();
        PaymentRefundOrder existing = paymentRefundOrderMapper.findByRequestIdempotencyKey(requestKey);
        if (existing == null) {
            PaymentRefundOrder refundOrder = new PaymentRefundOrder();
            refundOrder.setRefundNo(generateRefundNo());
            refundOrder.setPaymentNo(paymentOrder.getPaymentNo());
            refundOrder.setBizType(PaymentBizType.VOUCHER.getCode());
            refundOrder.setBizOrderNo(voucherOrder.getOrderNo());
            refundOrder.setUserId(voucherOrder.getUserId());
            if (voucherOrder.getVoucherId() != null) {
                var voucher = voucherMapper.findById(voucherOrder.getVoucherId());
                if (voucher != null) {
                    refundOrder.setMerchantId(voucher.getMerchantId());
                }
            }
            refundOrder.setChannel(paymentOrder.getChannel());
            refundOrder.setRefundAmount(refundAmount.stripTrailingZeros());
            refundOrder.setRequestedAmount(refundAmount.stripTrailingZeros());
            refundOrder.setApprovedAmount(refundAmount.stripTrailingZeros());
            refundOrder.setReason(AUTO_REFUND_REASON);
            refundOrder.setStatus(PaymentRefundStatus.PROCESSING.getCode());
            refundOrder.setCurrentReviewerRole(REVIEWER_SYSTEM);
            refundOrder.setCurrentReviewerId(SYSTEM_ACTOR_USER_ID);
            refundOrder.setRequestIdempotencyKey(requestKey);
            paymentRefundOrderMapper.insert(refundOrder);
            insertReviewLog(refundOrder.getRefundNo(), "SYSTEM_AUTO_REFUND_SUBMIT",
                    null, PaymentRefundStatus.PROCESSING.getCode(), AUTO_REFUND_REASON);
            return refundOrder;
        }

        if (PaymentRefundStatus.SUCCESS.getCode().equalsIgnoreCase(existing.getStatus())
                || PaymentRefundStatus.PROCESSING.getCode().equalsIgnoreCase(existing.getStatus())) {
            return existing;
        }

        if (PaymentRefundStatus.FAILED.getCode().equalsIgnoreCase(existing.getStatus())) {
            int updated = paymentRefundOrderMapper.markProcessingFromFailed(
                    existing.getRefundNo(),
                    PaymentRefundStatus.FAILED.getCode()
            );
            if (updated > 0) {
                insertReviewLog(existing.getRefundNo(), "SYSTEM_AUTO_REFUND_RETRY",
                        PaymentRefundStatus.FAILED.getCode(), PaymentRefundStatus.PROCESSING.getCode(), AUTO_REFUND_REASON);
                return paymentRefundOrderMapper.findByRefundNo(existing.getRefundNo());
            }
        }
        return existing;
    }

    private boolean finalizeOrderAsRefunded(VoucherOrder voucherOrder, String reason) {
        if (voucherOrder == null || voucherOrder.getId() == null) {
            return false;
        }
        return voucherOrderService.markOrderRefunded(voucherOrder.getId(), reason);
    }

    private boolean isAutoRefundCandidate(VoucherOrder order, LocalDateTime now) {
        if (order == null || order.getStatus() == null || order.getPaymentStatus() == null) {
            return false;
        }
        if (order.getUseTime() != null || order.getUseDeadline() == null) {
            return false;
        }
        if (order.getPaymentStatus() != 1) {
            return false;
        }
        if (order.getStatus() != 1 && order.getStatus() != 3) {
            return false;
        }
        return !order.getUseDeadline().isAfter(now);
    }

    private void insertReviewLog(String refundNo, String action, String fromStatus, String toStatus, String comment) {
        PaymentRefundReviewLog reviewLog = new PaymentRefundReviewLog();
        reviewLog.setRefundNo(refundNo);
        reviewLog.setOperatorRole(REVIEWER_SYSTEM);
        reviewLog.setOperatorId(SYSTEM_ACTOR_USER_ID);
        reviewLog.setAction(action);
        reviewLog.setFromStatus(fromStatus);
        reviewLog.setToStatus(toStatus);
        reviewLog.setComment(comment);
        paymentRefundReviewLogMapper.insert(reviewLog);
    }

    private void notifyUser(Long targetUserId, String type, String message) {
        if (targetUserId == null) {
            return;
        }
        messageNotificationService.createSystemNotification(
                type + ":" + targetUserId + ":" + System.currentTimeMillis(),
                type,
                targetUserId,
                SYSTEM_ACTOR_USER_ID,
                message
        );
    }

    private String generateRefundNo() {
        return "R" + LocalDateTime.now().format(REFUND_NO_FORMATTER)
                + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
