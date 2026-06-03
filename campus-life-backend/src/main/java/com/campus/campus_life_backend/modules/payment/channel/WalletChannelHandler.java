package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentCallbackService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WalletChannelHandler implements PaymentChannelHandler {

    private final PaymentCallbackService paymentCallbackService;

    public WalletChannelHandler(PaymentCallbackService paymentCallbackService) {
        this.paymentCallbackService = paymentCallbackService;
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
        return false;
    }
}
