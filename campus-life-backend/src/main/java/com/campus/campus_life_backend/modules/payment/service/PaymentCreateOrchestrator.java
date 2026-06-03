package com.campus.campus_life_backend.modules.payment.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelHandler;
import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelRegistry;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import org.springframework.stereotype.Service;

@Service
public class PaymentCreateOrchestrator {

    private final PaymentChannelRegistry channelRegistry;
    private final PaymentOrderDomainService paymentOrderDomainService;

    public PaymentCreateOrchestrator(
            PaymentChannelRegistry channelRegistry,
            PaymentOrderDomainService paymentOrderDomainService
    ) {
        this.channelRegistry = channelRegistry;
        this.paymentOrderDomainService = paymentOrderDomainService;
    }

    public PaymentResponse create(
            VoucherOrder order,
            Long userId,
            PaymentRequest request,
            String idempotencyKey
    ) {
        PaymentChannelHandler handler = channelRegistry.require(request.getPaymentMethod());
        PaymentOrder paymentOrder = paymentOrderDomainService.createVoucherPaymentOrder(
                order,
                userId,
                request.getPaymentMethod(),
                request.getSubject(),
                request.getDescription(),
                idempotencyKey
        );

        if (!handler.isExternalChannel()) {
            return handler.paySynchronously(order, userId, request, paymentOrder);
        }

        if ("SUCCESS".equals(paymentOrder.getStatus())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_STATUS_CHANGED, "订单已支付");
        }

        PaymentRequest channelRequest = buildChannelPaymentRequest(request, paymentOrder.getPaymentNo());
        PaymentResponse response = handler.createChannelPayment(channelRequest);
        paymentOrderDomainService.markWaitingForPay(paymentOrder);
        response.setPaymentOrderNo(paymentOrder.getPaymentNo());
        return response;
    }

    private PaymentRequest buildChannelPaymentRequest(PaymentRequest request, String paymentNo) {
        PaymentRequest channelRequest = new PaymentRequest();
        channelRequest.setOrderNo(paymentNo);
        channelRequest.setPaymentMethod(request.getPaymentMethod());
        channelRequest.setAmount(request.getAmount());
        channelRequest.setSubject(request.getSubject());
        channelRequest.setDescription(request.getDescription());
        channelRequest.setPassbackParams(request.getPassbackParams());
        return channelRequest;
    }
}
