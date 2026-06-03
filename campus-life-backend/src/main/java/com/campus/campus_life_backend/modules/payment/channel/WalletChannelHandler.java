package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
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
    public boolean handlePaymentCallback(String paymentMethod, String callbackData) {
        return true;
    }

    @Override
    public String queryPaymentStatus(String orderNo) {
        return "UNKNOWN";
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
