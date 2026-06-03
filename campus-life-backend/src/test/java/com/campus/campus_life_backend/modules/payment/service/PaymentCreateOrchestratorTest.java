package com.campus.campus_life_backend.modules.payment.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelHandler;
import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelRegistry;
import com.campus.campus_life_backend.modules.payment.channel.WalletChannelHandler;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCreateOrchestratorTest {

    @Mock
    private PaymentChannelRegistry channelRegistry;

    @Mock
    private PaymentOrderDomainService paymentOrderDomainService;

    @Mock
    private WalletChannelHandler walletChannelHandler;

    @Mock
    private PaymentChannelHandler alipayChannelHandler;

    @Mock
    private PaymentChannelHandler wechatChannelHandler;

    @InjectMocks
    private PaymentCreateOrchestrator paymentCreateOrchestrator;

    @Test
    void create_wallet_delegatesToPaySynchronously() {
        VoucherOrder order = voucherOrder("ORD-1");
        PaymentRequest request = paymentRequest("wallet");
        PaymentOrder paymentOrder = paymentOrder("PAY-1", "INIT");

        when(channelRegistry.require("wallet")).thenReturn(walletChannelHandler);
        when(walletChannelHandler.isExternalChannel()).thenReturn(false);
        when(paymentOrderDomainService.createVoucherPaymentOrder(
                order, 1L, "wallet", "优惠券订单", "优惠券购买", "key-1"
        )).thenReturn(paymentOrder);

        PaymentResponse walletResponse = new PaymentResponse();
        walletResponse.setPaymentMethod("wallet");
        walletResponse.setPaymentOrderNo("PAY-1");
        when(walletChannelHandler.paySynchronously(order, 1L, request, paymentOrder)).thenReturn(walletResponse);

        PaymentResponse response = paymentCreateOrchestrator.create(order, 1L, request, "key-1");

        assertEquals("wallet", response.getPaymentMethod());
        assertEquals("PAY-1", response.getPaymentOrderNo());
        verify(walletChannelHandler).paySynchronously(order, 1L, request, paymentOrder);
    }

    @Test
    void create_alipay_alreadySuccess_throwsPaymentStatusChanged() {
        VoucherOrder order = voucherOrder("ORD-2");
        PaymentRequest request = paymentRequest("alipay");
        PaymentOrder paymentOrder = paymentOrder("PAY-2", "SUCCESS");

        when(channelRegistry.require("alipay")).thenReturn(alipayChannelHandler);
        when(alipayChannelHandler.isExternalChannel()).thenReturn(true);
        when(paymentOrderDomainService.createVoucherPaymentOrder(
                order, 1L, "alipay", "优惠券订单", "优惠券购买", "key-2"
        )).thenReturn(paymentOrder);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> paymentCreateOrchestrator.create(order, 1L, request, "key-2")
        );
        assertEquals(BusinessErrorCode.PAYMENT_STATUS_CHANGED.getCode(), ex.getCode());
        assertEquals("订单已支付", ex.getMessage());
    }

    @Test
    void create_alipay_externalChannel_createsChannelPaymentAndMarksWaiting() {
        VoucherOrder order = voucherOrder("ORD-3");
        PaymentRequest request = paymentRequest("alipay");
        request.setAmount(new BigDecimal("19.90"));
        PaymentOrder paymentOrder = paymentOrder("PAY-3", "INIT");

        when(channelRegistry.require("alipay")).thenReturn(alipayChannelHandler);
        when(alipayChannelHandler.isExternalChannel()).thenReturn(true);
        when(paymentOrderDomainService.createVoucherPaymentOrder(
                order, 1L, "alipay", "优惠券订单", "优惠券购买", "key-3"
        )).thenReturn(paymentOrder);

        PaymentResponse channelResponse = new PaymentResponse();
        channelResponse.setPaymentMethod("alipay");
        channelResponse.setPayForm("<form></form>");
        when(alipayChannelHandler.createChannelPayment(any(PaymentRequest.class))).thenReturn(channelResponse);

        PaymentResponse response = paymentCreateOrchestrator.create(order, 1L, request, "key-3");

        assertEquals("PAY-3", response.getPaymentOrderNo());
        verify(alipayChannelHandler).createChannelPayment(any(PaymentRequest.class));
        verify(paymentOrderDomainService).markWaitingForPay(paymentOrder);
    }

    @Test
    void create_wechat_externalChannel_createsChannelPaymentAndMarksWaiting() {
        VoucherOrder order = voucherOrder("ORD-4");
        PaymentRequest request = paymentRequest("wechat");
        request.setAmount(new BigDecimal("29.90"));
        PaymentOrder paymentOrder = paymentOrder("PAY-4", "INIT");

        when(channelRegistry.require("wechat")).thenReturn(wechatChannelHandler);
        when(wechatChannelHandler.isExternalChannel()).thenReturn(true);
        when(paymentOrderDomainService.createVoucherPaymentOrder(
                order, 1L, "wechat", "优惠券订单", "优惠券购买", "key-4"
        )).thenReturn(paymentOrder);

        PaymentResponse channelResponse = new PaymentResponse();
        channelResponse.setPaymentMethod("wechat");
        channelResponse.setPayUrl("weixin://wxpay/bizpayurl?pr=mock");
        when(wechatChannelHandler.createChannelPayment(any(PaymentRequest.class))).thenReturn(channelResponse);

        PaymentResponse response = paymentCreateOrchestrator.create(order, 1L, request, "key-4");

        assertEquals("PAY-4", response.getPaymentOrderNo());
        verify(wechatChannelHandler).createChannelPayment(any(PaymentRequest.class));
        verify(paymentOrderDomainService).markWaitingForPay(paymentOrder);
    }

    private static VoucherOrder voucherOrder(String orderNo) {
        VoucherOrder order = new VoucherOrder();
        order.setOrderNo(orderNo);
        order.setPayAmount(new BigDecimal("19.90"));
        return order;
    }

    private static PaymentRequest paymentRequest(String method) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderNo("ORD-x");
        request.setPaymentMethod(method);
        request.setSubject("优惠券订单");
        request.setDescription("优惠券购买");
        return request;
    }

    private static PaymentOrder paymentOrder(String paymentNo, String status) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo(paymentNo);
        paymentOrder.setStatus(status);
        paymentOrder.setVersion(0);
        return paymentOrder;
    }
}
