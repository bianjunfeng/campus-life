package com.campus.campus_life_backend.modules.payment.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.order.event.OrderPaidEvent;
import com.campus.campus_life_backend.modules.order.event.OrderPaymentFailedEvent;
import com.campus.campus_life_backend.modules.order.event.VoucherOrderKafkaEventPublisher;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentOrderStatus;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentOrderMapper;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.user.mapper.UserWalletMapper;
import com.campus.campus_life_backend.modules.user.mapper.WalletTransactionMapper;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 鏀粯鍥炶皟澶勭悊鏈嶅姟
 */
@Service
public class PaymentCallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentCallbackService.class);

    private final PaymentOrderMapper paymentOrderMapper;
    private final VoucherOrderService voucherOrderService;
    private final UserWalletMapper userWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final PaymentIdempotencyService paymentIdempotencyService;
    private final VoucherOrderKafkaEventPublisher voucherOrderKafkaEventPublisher;

    @Autowired
    public PaymentCallbackService(
            PaymentOrderMapper paymentOrderMapper,
            VoucherOrderService voucherOrderService,
            UserWalletMapper userWalletMapper,
            WalletTransactionMapper walletTransactionMapper,
            PaymentIdempotencyService paymentIdempotencyService,
            VoucherOrderKafkaEventPublisher voucherOrderKafkaEventPublisher
    ) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.voucherOrderService = voucherOrderService;
        this.userWalletMapper = userWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.paymentIdempotencyService = paymentIdempotencyService;
        this.voucherOrderKafkaEventPublisher = voucherOrderKafkaEventPublisher;
    }
    
    /**
     * 澶勭悊鏀粯鎴愬姛鍥炶皟
     * @param paymentNo 支付单号
     * @param paymentMethod 鏀粯鏂瑰紡
     */
    @Transactional
    public boolean handlePaymentSuccess(String paymentNo, String paymentMethod, String channelTradeNo) {
        String callbackKey = paymentIdempotencyService.callbackKey(paymentMethod, paymentNo);
        if (!paymentIdempotencyService.acquireCallback(callbackKey, Duration.ofMinutes(10))) {
            logger.info("支付回调幂等命中，忽略重复处理: paymentNo={}, method={}", paymentNo, paymentMethod);
            return true;
        }
        try {
            PaymentOrder paymentOrder = paymentOrderMapper.findByPaymentNo(paymentNo);
            if (paymentOrder == null) {
                logger.warn("支付单不存在: {}", paymentNo);
                paymentIdempotencyService.releaseCallback(callbackKey);
                return false;
            }
            VoucherOrder order = voucherOrderService.findByOrderNo(paymentOrder.getBizOrderNo());
            if (order == null) {
                logger.warn("业务订单不存在: paymentNo={}, bizOrderNo={}", paymentNo, paymentOrder.getBizOrderNo());
                paymentIdempotencyService.releaseCallback(callbackKey);
                return false;
            }

            if (isPaymentOrderSuccess(paymentOrder)) {
                logger.info("支付单已成功，补偿确认业务订单状态: paymentNo={}, orderNo={}",
                        paymentNo, order.getOrderNo());
                LocalDateTime payTime = paymentOrder.getSuccessTime() == null
                        ? LocalDateTime.now()
                        : paymentOrder.getSuccessTime();
                return ensureOrderPaid(order, paymentOrder, paymentMethod, payTime, "payment_callback_reconcile");
            }
            if (isPaymentOrderRefunded(paymentOrder)) {
                logger.info("支付单已完成退款处理，跳过重复回调: {}", paymentNo);
                return true;
            }

            if (!markPaymentOrderSuccess(paymentOrder, channelTradeNo)) {
                PaymentOrder latestPaymentOrder = paymentOrderMapper.findByPaymentNo(paymentNo);
                if (latestPaymentOrder != null
                        && PaymentOrderStatus.SUCCESS.getCode().equals(latestPaymentOrder.getStatus())) {
                    return true;
                }
                logger.warn("支付单状态更新失败: paymentNo={}, status={}", paymentNo, paymentOrder.getStatus());
                return false;
            }

            if (order.getPaymentStatus() != null && order.getPaymentStatus() == 1) {
                logger.info("业务订单已支付，跳过重复处理: {}", order.getOrderNo());
                return true;
            }
            LocalDateTime payTime = LocalDateTime.now();
            if (!ensureOrderPaid(order, paymentOrder, paymentMethod, payTime, "payment_callback")) {
                return false;
            }

            logger.info("支付成功处理完成: paymentNo={}, orderNo={}, paymentMethod={}",
                    paymentNo, order.getOrderNo(), paymentMethod);
            return true;
        } catch (Exception e) {
            paymentIdempotencyService.releaseCallback(callbackKey);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("处理支付成功回调失败: paymentNo={}", paymentNo, e);
            return false;
        }
    }

    @Transactional
    public void payByWallet(VoucherOrder order, PaymentOrder paymentOrder, Long userId) {
        if (order == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_FORBIDDEN);
        }
        if (paymentOrder == null || paymentOrder.getPaymentNo() == null) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_RECORD_NOT_FOUND);
        }
        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 1) {
            return;
        }
        if (isPaymentOrderSuccess(paymentOrder)) {
            LocalDateTime payTime = paymentOrder.getSuccessTime() == null
                    ? LocalDateTime.now()
                    : paymentOrder.getSuccessTime();
            if (!ensureOrderPaid(order, paymentOrder, "wallet", payTime, "wallet_payment_reconcile")) {
                throw new BusinessException(BusinessErrorCode.ORDER_STATUS_CHANGED);
            }
            return;
        }
        if (isPaymentOrderRefunded(paymentOrder)) {
            return;
        }

        BigDecimal payAmount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(BusinessErrorCode.ORDER_AMOUNT_INVALID);
        }

        userWalletMapper.initWalletIfAbsent(userId, new BigDecimal("1000.00"));
        int updated = userWalletMapper.deductBalanceAndAddSpent(userId, payAmount);
        if (updated <= 0) {
            throw new BusinessException(BusinessErrorCode.WALLET_BALANCE_NOT_ENOUGH);
        }

        if (!markWalletPaymentOrderSuccess(paymentOrder)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_STATUS_CHANGED);
        }
        LocalDateTime payTime = LocalDateTime.now();
        if (!ensureOrderPaid(order, paymentOrder, "wallet", payTime, "wallet_payment")) {
            throw new BusinessException(BusinessErrorCode.ORDER_STATUS_CHANGED);
        }
        recordWalletTransaction(order, "钱包支付");
    }

    /**
     * 澶勭悊鏀粯澶辫触鍥炶皟
     * @param paymentNo 支付单号
     */
    @Transactional
    public boolean handlePaymentFailure(String paymentNo) {
        try {
            PaymentOrder paymentOrder = paymentOrderMapper.findByPaymentNo(paymentNo);
            if (paymentOrder == null) {
                logger.warn("支付单不存在: {}", paymentNo);
                return false;
            }

            VoucherOrder order = voucherOrderService.findByOrderNo(paymentOrder.getBizOrderNo());
            if (order == null) {
                logger.warn("业务订单不存在: paymentNo={}, bizOrderNo={}", paymentNo, paymentOrder.getBizOrderNo());
                return false;
            }

            paymentOrderMapper.markFailedByPaymentNo(paymentNo, LocalDateTime.now(), paymentOrder.getVersion());

            // 仅允许待支付 -> 支付失败（状态机约束）
            if (order.getStatus() == null || order.getPaymentStatus() == null
                    || order.getStatus() != 0 || order.getPaymentStatus() != 0) {
                return true;
            }
            if (voucherOrderKafkaEventPublisher.isEnabled()) {
                publishOrderPaymentFailedEvent(order, paymentOrder.getPaymentNo(), "payment_failure_callback");
            } else {
                voucherOrderService.markOrderPaymentFailed(order.getId());
            }

            logger.info("支付失败处理完成: paymentNo={}, orderNo={}", paymentNo, order.getOrderNo());
            return true;
        } catch (Exception e) {
            logger.error("处理支付失败回调失败: paymentNo={}", paymentNo, e);
            return false;
        }
    }

    private boolean markPaymentOrderSuccess(PaymentOrder paymentOrder, String channelTradeNo) {
        if (paymentOrder == null || paymentOrder.getPaymentNo() == null || paymentOrder.getVersion() == null) {
            return false;
        }
        int updated = paymentOrderMapper.markSuccessByPaymentNo(
                paymentOrder.getPaymentNo(),
                channelTradeNo,
                LocalDateTime.now(),
                LocalDateTime.now(),
                paymentOrder.getVersion()
        );
        if (updated > 0) {
            paymentOrder.setStatus(PaymentOrderStatus.SUCCESS.getCode());
            paymentOrder.setVersion(paymentOrder.getVersion() + 1);
        }
        return updated > 0;
    }

    private boolean markWalletPaymentOrderSuccess(PaymentOrder paymentOrder) {
        return markPaymentOrderSuccess(paymentOrder, "WALLET_" + paymentOrder.getPaymentNo());
    }

    private void recordWalletTransaction(VoucherOrder order, String remark) {
        Map<String, Object> wallet = userWalletMapper.findByUserId(order.getUserId());
        BigDecimal balanceAfter = wallet != null && wallet.get("balance") instanceof BigDecimal
                ? (BigDecimal) wallet.get("balance")
                : BigDecimal.ZERO;
        walletTransactionMapper.insertExpenseTransaction(
                order.getUserId(),
                order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount(),
                balanceAfter,
                order.getOrderNo(),
                remark
        );
    }

    private boolean ensureOrderPaid(
            VoucherOrder order,
            PaymentOrder paymentOrder,
            String paymentMethod,
            LocalDateTime payTime,
            String source
    ) {
        if (order == null || paymentOrder == null) {
            return false;
        }
        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 1) {
            return true;
        }
        if (!voucherOrderService.markOrderPaid(order.getId(), paymentMethod, payTime)) {
            logger.warn("订单支付状态更新失败: orderNo={}, status={}, paymentStatus={}",
                    order.getOrderNo(), order.getStatus(), order.getPaymentStatus());
            return false;
        }
        publishOrderPaidEventIfEnabled(order, paymentOrder.getPaymentNo(), paymentMethod, payTime, source);
        return true;
    }

    private void publishOrderPaidEventIfEnabled(
            VoucherOrder order,
            String paymentNo,
            String paymentMethod,
            LocalDateTime payTime,
            String source
    ) {
        if (!voucherOrderKafkaEventPublisher.isEnabled()) {
            return;
        }
        try {
            publishOrderPaidEvent(order, paymentNo, paymentMethod, payTime, source);
        } catch (Exception e) {
            logger.warn("订单支付状态已落库，但发布支付成功事件失败: orderNo={}, paymentNo={}",
                    order == null ? null : order.getOrderNo(), paymentNo, e);
        }
    }

    private boolean isPaymentOrderSuccess(PaymentOrder paymentOrder) {
        return paymentOrder != null
                && PaymentOrderStatus.SUCCESS.getCode().equals(paymentOrder.getStatus());
    }

    private boolean isPaymentOrderRefunded(PaymentOrder paymentOrder) {
        if (paymentOrder == null) {
            return false;
        }
        return PaymentOrderStatus.PARTIAL_REFUNDED.getCode().equals(paymentOrder.getStatus())
                || PaymentOrderStatus.FULL_REFUNDED.getCode().equals(paymentOrder.getStatus());
    }

    private void publishOrderPaidEvent(
            VoucherOrder order,
            String paymentNo,
            String paymentMethod,
            LocalDateTime payTime,
            String source
    ) {
        if (order == null || order.getId() == null) {
            return;
        }
        OrderPaidEvent event = new OrderPaidEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrderId(order.getId());
        event.setOrderNo(order.getOrderNo());
        event.setPaymentNo(paymentNo);
        event.setUserId(order.getUserId());
        event.setPaymentMethod(paymentMethod);
        event.setRequestIdempotencyKey(order.getPaymentIdempotencyKey());
        event.setPayTime(payTime);
        event.setEventTime(LocalDateTime.now());
        event.setSource(source);
        voucherOrderKafkaEventPublisher.publish(event);
    }

    private void publishOrderPaymentFailedEvent(VoucherOrder order, String paymentNo, String source) {
        if (order == null || order.getId() == null) {
            return;
        }
        OrderPaymentFailedEvent event = new OrderPaymentFailedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrderId(order.getId());
        event.setOrderNo(order.getOrderNo());
        event.setPaymentNo(paymentNo);
        event.setUserId(order.getUserId());
        event.setRequestIdempotencyKey(order.getPaymentIdempotencyKey());
        event.setEventTime(LocalDateTime.now());
        event.setSource(source);
        voucherOrderKafkaEventPublisher.publish(event);
    }
}
