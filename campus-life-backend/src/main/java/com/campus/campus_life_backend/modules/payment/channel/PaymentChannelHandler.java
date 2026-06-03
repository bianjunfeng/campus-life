package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;

import java.math.BigDecimal;

/**
 * 第三方支付渠道能力（与 {@link com.campus.campus_life_backend.modules.payment.service.PaymentService} 渠道侧对齐）。
 */
public interface PaymentChannelHandler {

    String channelCode();

    PaymentResponse createChannelPayment(PaymentRequest request);

    boolean handlePaymentCallback(String paymentMethod, String callbackData);

    String queryPaymentStatus(String orderNo);

    boolean refund(String orderNo, BigDecimal refundAmount, String refundReason);
}
