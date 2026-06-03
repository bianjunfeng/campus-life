package com.campus.campus_life_backend.modules.payment.service.impl;

import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelHandler;
import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelRegistry;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentOrderDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        when(alipayHandler.channelCode()).thenReturn(PaymentMethod.ALIPAY.getCode());
        when(wechatHandler.channelCode()).thenReturn(PaymentMethod.WECHAT.getCode());
        PaymentChannelRegistry registry = new PaymentChannelRegistry(List.of(alipayHandler, wechatHandler));
        paymentService = new PaymentServiceImpl(registry, paymentOrderDomainService);
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
}
