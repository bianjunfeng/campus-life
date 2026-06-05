package com.campus.campus_life_backend.modules.payment.service.impl;

import com.campus.campus_life_backend.modules.payment.channel.AlipayChannelHandler;
import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelHandler;
import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelRegistry;
import com.campus.campus_life_backend.modules.payment.channel.WechatChannelHandler;
import com.campus.campus_life_backend.modules.payment.dto.PaymentCallbackResult;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentOrderDomainService;
import com.campus.campus_life_backend.modules.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentOrderDomainService paymentOrderDomainService;

    @Mock
    private PaymentChannelHandler alipayHandler;

    @Mock
    private PaymentChannelHandler wechatHandler;

    @Mock
    private PaymentChannelHandler walletHandler;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        when(alipayHandler.channelCode()).thenReturn(PaymentMethod.ALIPAY.getCode());
        when(wechatHandler.channelCode()).thenReturn(PaymentMethod.WECHAT.getCode());
        when(walletHandler.channelCode()).thenReturn(PaymentMethod.WALLET.getCode());
        PaymentChannelRegistry registry = new PaymentChannelRegistry(
                List.of(alipayHandler, wechatHandler, walletHandler));
        paymentService = new PaymentServiceImpl(registry, paymentOrderDomainService);
    }

    @Test
    void channelHandlers_doNotImplementPaymentService() {
        assertFalse(PaymentService.class.isAssignableFrom(AlipayChannelHandler.class));
        assertFalse(PaymentService.class.isAssignableFrom(WechatChannelHandler.class));
    }

    @Test
    void createPayment_alipay_delegatesToCreateChannelPayment() {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod("alipay");
        request.setOrderNo("ORD-1");
        PaymentResponse channelResponse = new PaymentResponse();
        channelResponse.setPaymentMethod("alipay");
        when(alipayHandler.createChannelPayment(request)).thenReturn(channelResponse);

        PaymentResponse response = paymentService.createPayment(request);

        assertEquals("alipay", response.getPaymentMethod());
        verify(alipayHandler).createChannelPayment(request);
        verify(wechatHandler, never()).createChannelPayment(any());
    }

    @Test
    void createPayment_wallet_delegatesToHandler() {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod("wallet");
        when(walletHandler.createChannelPayment(request))
                .thenThrow(new IllegalArgumentException("钱包支付请通过统一支付入口处理"));

        assertThrows(IllegalArgumentException.class, () -> paymentService.createPayment(request));

        verify(walletHandler).createChannelPayment(request);
        verify(alipayHandler, never()).createChannelPayment(any());
        verify(wechatHandler, never()).createChannelPayment(any());
    }

    @Test
    void handlePaymentCallback_routesToAlipayOnly() {
        when(alipayHandler.handlePaymentCallback("alipay", "out_trade_no=PAY-1"))
                .thenReturn(PaymentCallbackResult.processed());

        assertTrue(paymentService.handlePaymentCallback("alipay", "out_trade_no=PAY-1").isSuccess());

        verify(alipayHandler).handlePaymentCallback("alipay", "out_trade_no=PAY-1");
        verify(wechatHandler, never()).handlePaymentCallback(any(), any());
    }

    @Test
    void handlePaymentCallback_wallet_delegatesToHandler() {
        when(walletHandler.handlePaymentCallback("wallet", "{}"))
                .thenReturn(PaymentCallbackResult.processed());

        assertTrue(paymentService.handlePaymentCallback("wallet", "{}").isSuccess());

        verify(walletHandler).handlePaymentCallback("wallet", "{}");
        verify(alipayHandler, never()).handlePaymentCallback(any(), any());
        verify(wechatHandler, never()).handlePaymentCallback(any(), any());
    }

    @Test
    void queryPaymentStatus_routesToAlipayOnly() {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-ALI-1");
        paymentOrder.setChannel("alipay");
        when(paymentOrderDomainService.resolveForChannelRouting("PAY-ALI-1")).thenReturn(paymentOrder);
        when(alipayHandler.queryPaymentStatus("PAY-ALI-1")).thenReturn("TRADE_SUCCESS");

        String status = paymentService.queryPaymentStatus("PAY-ALI-1");

        assertEquals("TRADE_SUCCESS", status);
        verify(alipayHandler).queryPaymentStatus("PAY-ALI-1");
        verify(wechatHandler, never()).queryPaymentStatus(any());
    }

    @Test
    void queryPaymentStatus_noPaymentOrder_returnsUnknown() {
        when(paymentOrderDomainService.resolveForChannelRouting("MISSING")).thenReturn(null);

        assertEquals("UNKNOWN", paymentService.queryPaymentStatus("MISSING"));
        verify(alipayHandler, never()).queryPaymentStatus(any());
        verify(wechatHandler, never()).queryPaymentStatus(any());
    }

    @Test
    void queryPaymentStatus_blankChannel_returnsUnknown() {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-1");
        paymentOrder.setChannel("  ");
        when(paymentOrderDomainService.resolveForChannelRouting("PAY-1")).thenReturn(paymentOrder);

        assertEquals("UNKNOWN", paymentService.queryPaymentStatus("PAY-1"));
        verify(alipayHandler, never()).queryPaymentStatus(any());
    }

    @Test
    void queryPaymentStatus_unknownChannel_returnsUnknown() {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-1");
        paymentOrder.setChannel("unionpay");
        when(paymentOrderDomainService.resolveForChannelRouting("PAY-1")).thenReturn(paymentOrder);

        assertEquals("UNKNOWN", paymentService.queryPaymentStatus("PAY-1"));
        verify(alipayHandler, never()).queryPaymentStatus(any());
    }

    @Test
    void refund_routesToAlipayOnly() {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-ALI-2");
        paymentOrder.setChannel("alipay");
        when(paymentOrderDomainService.resolveForChannelRouting("PAY-ALI-2")).thenReturn(paymentOrder);
        when(alipayHandler.refund("PAY-ALI-2", new BigDecimal("10.00"), "reason")).thenReturn(true);

        boolean ok = paymentService.refund("PAY-ALI-2", new BigDecimal("10.00"), "reason");

        assertEquals(true, ok);
        verify(alipayHandler).refund(eq("PAY-ALI-2"), eq(new BigDecimal("10.00")), eq("reason"));
        verify(wechatHandler, never()).refund(any(), any(), any());
    }

    @Test
    void refund_noPaymentOrder_returnsFalse() {
        when(paymentOrderDomainService.resolveForChannelRouting("MISSING")).thenReturn(null);

        assertFalse(paymentService.refund("MISSING", BigDecimal.ONE, "r"));
        verify(alipayHandler, never()).refund(any(), any(), any());
    }

    @Test
    void queryPaymentStatus_routesToWallet() {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-WLT-1");
        paymentOrder.setChannel("wallet");
        when(paymentOrderDomainService.resolveForChannelRouting("PAY-WLT-1")).thenReturn(paymentOrder);
        when(walletHandler.queryPaymentStatus("PAY-WLT-1")).thenReturn("SUCCESS");

        String status = paymentService.queryPaymentStatus("PAY-WLT-1");

        assertEquals("SUCCESS", status);
        verify(walletHandler).queryPaymentStatus("PAY-WLT-1");
        verify(alipayHandler, never()).queryPaymentStatus(any());
        verify(wechatHandler, never()).queryPaymentStatus(any());
    }

    @Test
    void refund_routesToWallet() {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-WLT-2");
        paymentOrder.setChannel("wallet");
        when(paymentOrderDomainService.resolveForChannelRouting("PAY-WLT-2")).thenReturn(paymentOrder);
        when(walletHandler.refund("PAY-WLT-2", new BigDecimal("5.00"), "wallet refund")).thenReturn(true);

        boolean ok = paymentService.refund("PAY-WLT-2", new BigDecimal("5.00"), "wallet refund");

        assertTrue(ok);
        verify(walletHandler).refund(eq("PAY-WLT-2"), eq(new BigDecimal("5.00")), eq("wallet refund"));
        verify(alipayHandler, never()).refund(any(), any(), any());
        verify(wechatHandler, never()).refund(any(), any(), any());
    }
}
