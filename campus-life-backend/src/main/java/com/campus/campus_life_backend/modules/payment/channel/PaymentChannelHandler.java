package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.payment.dto.PaymentCallbackResult;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;

import java.math.BigDecimal;

/**
 * 支付渠道能力；对外统一入口见 {@link com.campus.campus_life_backend.modules.payment.service.PaymentService}。
 */
public interface PaymentChannelHandler {

    String channelCode();

    /**
     * 是否为需跳转/回调的第三方渠道；钱包等内部渠道为 false。
     */
    default boolean isExternalChannel() {
        return true;
    }

    /**
     * 内部渠道同步扣款（仅 {@code isExternalChannel() == false} 时由编排器调用）。
     */
    default PaymentResponse paySynchronously(
            VoucherOrder order,
            Long userId,
            PaymentRequest request,
            PaymentOrder paymentOrder
    ) {
        throw new UnsupportedOperationException("渠道不支持同步支付: " + channelCode());
    }

    PaymentResponse createChannelPayment(PaymentRequest request);

    PaymentCallbackResult handlePaymentCallback(String paymentMethod, String callbackData);

    String queryPaymentStatus(String orderNo);

    boolean refund(String orderNo, BigDecimal refundAmount, String refundReason);
}
