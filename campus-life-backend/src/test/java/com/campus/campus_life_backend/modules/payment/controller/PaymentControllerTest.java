package com.campus.campus_life_backend.modules.payment.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.order.service.OrderFacadeService;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.service.PaymentCallbackService;
import com.campus.campus_life_backend.modules.payment.service.PaymentIdempotencyService;
import com.campus.campus_life_backend.modules.payment.service.PaymentOrderDomainService;
import com.campus.campus_life_backend.modules.payment.service.PaymentRefundWorkflowService;
import com.campus.campus_life_backend.modules.payment.service.PaymentService;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentCallbackService paymentCallbackService;

    @Mock
    private PaymentIdempotencyService paymentIdempotencyService;

    @Mock
    private PaymentOrderDomainService paymentOrderDomainService;

    @Mock
    private PaymentRefundWorkflowService paymentRefundWorkflowService;

    @Mock
    private OrderFacadeService orderFacadeService;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void shouldUseServerSideOrderAmountWhenCreatePayment() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderNo("ORD-1001");
        request.setPaymentMethod("alipay");
        request.setAmount(new BigDecimal("0.01"));

        VoucherOrder order = new VoucherOrder();
        order.setOrderNo("ORD-1001");
        order.setUserId(1L);
        order.setStatus(0);
        order.setPaymentStatus(0);
        order.setPayAmount(new BigDecimal("99.00"));

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setPaymentMethod("alipay");
        paymentResponse.setPaymentOrderNo("PAY-1001");

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId(1L);
        paymentOrder.setPaymentNo("PAY-1001");
        paymentOrder.setStatus("INIT");
        paymentOrder.setVersion(0);

        when(currentUserAccessor.requireUserId()).thenReturn(1L);
        when(orderFacadeService.findOwnedOrderByOrderNo("ORD-1001", 1L)).thenReturn(order);
        when(orderFacadeService.buildPaymentOrderInfo(order)).thenReturn(Map.of(
                "subject", "优惠券订单",
                "description", "优惠券购买"
        ));
        when(paymentIdempotencyService.resolveCreateKey(null, "ORD-1001", 1L, "alipay")).thenReturn("create-key");
        when(paymentIdempotencyService.acquireCreate("create-key", java.time.Duration.ofSeconds(8))).thenReturn(true);
        when(paymentOrderDomainService.createVoucherPaymentOrder(order, 1L, "alipay", "优惠券订单", "优惠券购买", "create-key"))
                .thenReturn(paymentOrder);
        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(paymentResponse);

        ApiResponse<PaymentResponse> response = paymentController.createPayment(null, request);

        ArgumentCaptor<PaymentRequest> requestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).createPayment(requestCaptor.capture());
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(0, new BigDecimal("99.00").compareTo(requestCaptor.getValue().getAmount()));
    }

    @Test
    void shouldHideCreatePaymentFailureDetails() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderNo("ORD-1002");
        request.setPaymentMethod("alipay");

        VoucherOrder order = new VoucherOrder();
        order.setOrderNo("ORD-1002");
        order.setUserId(1L);
        order.setStatus(0);
        order.setPaymentStatus(0);
        order.setPayAmount(new BigDecimal("19.90"));

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId(2L);
        paymentOrder.setPaymentNo("PAY-1002");
        paymentOrder.setStatus("INIT");
        paymentOrder.setVersion(0);

        when(currentUserAccessor.requireUserId()).thenReturn(1L);
        when(orderFacadeService.findOwnedOrderByOrderNo("ORD-1002", 1L)).thenReturn(order);
        when(orderFacadeService.buildPaymentOrderInfo(order)).thenReturn(Map.of(
                "subject", "优惠券订单",
                "description", "优惠券购买"
        ));
        when(paymentIdempotencyService.resolveCreateKey(null, "ORD-1002", 1L, "alipay")).thenReturn("create-key");
        when(paymentIdempotencyService.acquireCreate("create-key", java.time.Duration.ofSeconds(8))).thenReturn(true);
        when(paymentOrderDomainService.createVoucherPaymentOrder(order, 1L, "alipay", "优惠券订单", "优惠券购买", "create-key"))
                .thenReturn(paymentOrder);
        when(paymentService.createPayment(any(PaymentRequest.class))).thenThrow(new RuntimeException("alipay appId invalid"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentController.createPayment(null, request)
        );

        assertEquals(BusinessErrorCode.PAYMENT_CREATE_FAILED.getCode(), exception.getCode());
        assertEquals("创建支付订单失败，请稍后重试", exception.getMessage());
    }

    @Test
    void shouldRejectAlipayNotifyWhenPaidAmountDoesNotMatchOrder() {
        ReflectionTestUtils.setField(paymentController, "skipAlipayNotifySignVerify", true);

        when(paymentOrderDomainService.getExpectedAmountByPaymentNo("ORD-2001")).thenReturn(new BigDecimal("88.00"));

        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ORD-2001");
        params.put("total_amount", "8.80");

        String result = paymentController.alipayNotify(params);

        assertEquals("fail", result);
        verify(paymentService, never()).handlePaymentCallback(anyString(), anyString());
    }
}
