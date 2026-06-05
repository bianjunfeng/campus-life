package com.campus.campus_life_backend.modules.payment.service.impl;

import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelHandler;
import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelRegistry;
import com.campus.campus_life_backend.modules.payment.dto.PaymentCallbackResult;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentOrderDomainService;
import com.campus.campus_life_backend.modules.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 支付服务实现（统一入口）
 */
@Service
@Primary
public class PaymentServiceImpl implements PaymentService {

    private final PaymentChannelRegistry channelRegistry;
    private final PaymentOrderDomainService paymentOrderDomainService;

    @Autowired
    public PaymentServiceImpl(
            PaymentChannelRegistry channelRegistry,
            PaymentOrderDomainService paymentOrderDomainService
    ) {
        this.channelRegistry = channelRegistry;
        this.paymentOrderDomainService = paymentOrderDomainService;
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        PaymentMethod method = PaymentMethod.fromCode(request.getPaymentMethod());
        return channelRegistry.require(method.getCode()).createChannelPayment(request);
    }

    @Override
    public PaymentCallbackResult handlePaymentCallback(String paymentMethod, String callbackData) {
        PaymentMethod method = PaymentMethod.fromCode(paymentMethod);
        return channelRegistry.require(method.getCode())
                .handlePaymentCallback(paymentMethod, callbackData);
    }

    @Override
    public String queryPaymentStatus(String orderNo) {
        PaymentOrder paymentOrder = paymentOrderDomainService.resolveForChannelRouting(orderNo);
        String channelCode = resolveChannelCode(paymentOrder);
        if (channelCode == null || !isRegisteredChannel(channelCode)) {
            return "UNKNOWN";
        }
        PaymentChannelHandler handler = channelRegistry.require(channelCode);
        String queryKey = paymentOrder.getPaymentNo() != null ? paymentOrder.getPaymentNo() : orderNo;
        return handler.queryPaymentStatus(queryKey);
    }

    @Override
    public boolean refund(String orderNo, BigDecimal refundAmount, String refundReason) {
        PaymentOrder paymentOrder = paymentOrderDomainService.resolveForChannelRouting(orderNo);
        String channelCode = resolveChannelCode(paymentOrder);
        if (channelCode == null || !isRegisteredChannel(channelCode)) {
            return false;
        }
        PaymentChannelHandler handler = channelRegistry.require(channelCode);
        String refundKey = paymentOrder.getPaymentNo() != null ? paymentOrder.getPaymentNo() : orderNo;
        return handler.refund(refundKey, refundAmount, refundReason);
    }

    private static String resolveChannelCode(PaymentOrder paymentOrder) {
        if (paymentOrder == null || paymentOrder.getChannel() == null || paymentOrder.getChannel().isBlank()) {
            return null;
        }
        return paymentOrder.getChannel().trim().toLowerCase();
    }

    private boolean isRegisteredChannel(String channelCode) {
        try {
            channelRegistry.require(channelCode);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
