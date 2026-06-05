package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.payment.dto.PaymentCallbackResult;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentCallbackService;
import com.campus.campus_life_backend.modules.payment.service.PaymentOrderDomainService;
import com.campus.campus_life_backend.modules.user.mapper.UserWalletMapper;
import com.campus.campus_life_backend.modules.user.mapper.WalletTransactionMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 钱包内部支付渠道。
 *
 * <p>能力矩阵：
 * <pre>
 * | 能力                 | 行为                                      |
 * |----------------------|-------------------------------------------|
 * | paySynchronously     | 同步扣款（编排器调用）                    |
 * | createChannelPayment | 拒绝直连                                  |
 * | handlePaymentCallback| 无外部回调，恒 true                       |
 * | queryPaymentStatus   | 按本地 PaymentOrder 状态映射              |
 * | refund               | 退回钱包余额                              |
 * </pre>
 */
@Component
public class WalletChannelHandler implements PaymentChannelHandler {

    private final PaymentCallbackService paymentCallbackService;
    private final PaymentOrderDomainService paymentOrderDomainService;
    private final UserWalletMapper userWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    public WalletChannelHandler(
            PaymentCallbackService paymentCallbackService,
            PaymentOrderDomainService paymentOrderDomainService,
            UserWalletMapper userWalletMapper,
            WalletTransactionMapper walletTransactionMapper
    ) {
        this.paymentCallbackService = paymentCallbackService;
        this.paymentOrderDomainService = paymentOrderDomainService;
        this.userWalletMapper = userWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
    }

    @Override
    public String channelCode() {
        return PaymentMethod.WALLET.getCode();
    }

    @Override
    public boolean isExternalChannel() {
        return false;
    }

    @Override
    public PaymentResponse paySynchronously(
            VoucherOrder order,
            Long userId,
            PaymentRequest request,
            PaymentOrder paymentOrder
    ) {
        paymentCallbackService.payByWallet(order, paymentOrder, userId);
        PaymentResponse response = new PaymentResponse();
        response.setPaymentMethod(PaymentMethod.WALLET.getCode());
        response.setPaymentOrderNo(paymentOrder.getPaymentNo());
        return response;
    }

    @Override
    public PaymentResponse createChannelPayment(PaymentRequest request) {
        throw new IllegalArgumentException("钱包支付请通过统一支付入口处理");
    }

    @Override
    public PaymentCallbackResult handlePaymentCallback(String paymentMethod, String callbackData) {
        return PaymentCallbackResult.processed();
    }

    @Override
    public String queryPaymentStatus(String orderNo) {
        PaymentOrder paymentOrder = paymentOrderDomainService.findByPaymentNo(orderNo);
        if (paymentOrder == null) {
            return "UNKNOWN";
        }
        return mapLocalPaymentStatus(paymentOrder.getStatus());
    }

    static String mapLocalPaymentStatus(String paymentOrderStatus) {
        if (paymentOrderStatus == null || paymentOrderStatus.isBlank()) {
            return "PENDING";
        }
        String status = paymentOrderStatus.toUpperCase();
        if ("SUCCESS".equals(status)
                || "PARTIAL_REFUNDED".equals(status)
                || "FULL_REFUNDED".equals(status)) {
            return "SUCCESS";
        }
        if ("FAILED".equals(status) || "CLOSED".equals(status)) {
            return "FAILED";
        }
        return "PENDING";
    }

    @Override
    public boolean refund(String orderNo, BigDecimal refundAmount, String refundReason) {
        PaymentOrder paymentOrder = paymentOrderDomainService.findByPaymentNo(orderNo);
        if (paymentOrder == null || paymentOrder.getUserId() == null) {
            return false;
        }
        BigDecimal amount = refundAmount == null ? BigDecimal.ZERO : refundAmount;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        userWalletMapper.initWalletIfAbsent(paymentOrder.getUserId(), BigDecimal.ZERO);
        int updated = userWalletMapper.addBalanceAndReduceSpent(paymentOrder.getUserId(), amount);
        if (updated <= 0) {
            return false;
        }
        recordWalletRefundTransaction(paymentOrder, amount, refundReason);
        return true;
    }

    private void recordWalletRefundTransaction(PaymentOrder paymentOrder, BigDecimal refundAmount, String refundReason) {
        var wallet = userWalletMapper.findByUserId(paymentOrder.getUserId());
        BigDecimal balanceAfter = wallet != null && wallet.get("balance") instanceof BigDecimal
                ? (BigDecimal) wallet.get("balance")
                : BigDecimal.ZERO;
        String bizOrderNo = paymentOrder.getBizOrderNo() == null ? "" : paymentOrder.getBizOrderNo();
        String remark = refundReason == null || refundReason.isBlank()
                ? "钱包退款"
                : "钱包退款：" + refundReason;
        walletTransactionMapper.insertRefundTransaction(
                paymentOrder.getUserId(),
                refundAmount,
                balanceAfter,
                bizOrderNo,
                remark
        );
    }
}
