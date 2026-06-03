package com.campus.campus_life_backend.modules.payment.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.order.service.OrderFacadeService;
import com.campus.campus_life_backend.modules.payment.dto.PaymentCallbackResult;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.service.PaymentCreateOrchestrator;
import com.campus.campus_life_backend.modules.payment.service.PaymentIdempotencyService;
import com.campus.campus_life_backend.modules.payment.service.PaymentIdempotencyService.IdempotencyLock;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentCreateOrchestrator paymentCreateOrchestrator;

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

        when(currentUserAccessor.requireUserId()).thenReturn(1L);
        when(orderFacadeService.findOwnedOrderByOrderNo("ORD-1001", 1L)).thenReturn(order);
        when(orderFacadeService.buildPaymentOrderInfo(order)).thenReturn(Map.of(
                "subject", "优惠券订单",
                "description", "优惠券购买"
        ));
        IdempotencyLock createLock = new IdempotencyLock("create-key", "token-1");
        when(paymentIdempotencyService.resolveCreateKey(null, "ORD-1001", 1L, "alipay")).thenReturn("create-key");
        when(paymentIdempotencyService.tryAcquireCreate("create-key", PaymentIdempotencyService.CREATE_PROCESSING_TTL))
                .thenReturn(Optional.of(createLock));
        when(paymentCreateOrchestrator.create(eq(order), eq(1L), any(PaymentRequest.class), eq("create-key")))
                .thenReturn(paymentResponse);

        ApiResponse<PaymentResponse> response = paymentController.createPayment(null, request);

        ArgumentCaptor<PaymentRequest> requestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentCreateOrchestrator).create(eq(order), eq(1L), requestCaptor.capture(), eq("create-key"));
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(0, new BigDecimal("99.00").compareTo(requestCaptor.getValue().getAmount()));
        assertEquals("PAY-1001", response.getData().getPaymentOrderNo());
        verify(paymentIdempotencyService).completeCreate(createLock);
        verify(paymentIdempotencyService, never()).releaseCreate(createLock);
        verify(paymentService, never()).createPayment(any());
    }

    @Test
    void shouldCreateWalletPaymentViaOrchestrator() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderNo("ORD-W100");
        request.setPaymentMethod("wallet");

        VoucherOrder order = new VoucherOrder();
        order.setOrderNo("ORD-W100");
        order.setUserId(1L);
        order.setStatus(0);
        order.setPaymentStatus(0);
        order.setPayAmount(new BigDecimal("25.00"));

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setPaymentMethod("wallet");
        paymentResponse.setPaymentOrderNo("PAY-W100");

        when(currentUserAccessor.requireUserId()).thenReturn(1L);
        when(orderFacadeService.findOwnedOrderByOrderNo("ORD-W100", 1L)).thenReturn(order);
        when(orderFacadeService.buildPaymentOrderInfo(order)).thenReturn(Map.of(
                "subject", "优惠券订单",
                "description", "优惠券购买"
        ));
        IdempotencyLock createLock = new IdempotencyLock("create-key-wallet", "token-w");
        when(paymentIdempotencyService.resolveCreateKey(null, "ORD-W100", 1L, "wallet")).thenReturn("create-key-wallet");
        when(paymentIdempotencyService.tryAcquireCreate("create-key-wallet", PaymentIdempotencyService.CREATE_PROCESSING_TTL))
                .thenReturn(Optional.of(createLock));
        when(paymentCreateOrchestrator.create(eq(order), eq(1L), any(PaymentRequest.class), eq("create-key-wallet")))
                .thenReturn(paymentResponse);

        ApiResponse<PaymentResponse> response = paymentController.createPayment(null, request);

        assertEquals(200, response.getCode());
        assertEquals("wallet", response.getData().getPaymentMethod());
        assertEquals("PAY-W100", response.getData().getPaymentOrderNo());
        verify(paymentCreateOrchestrator).create(eq(order), eq(1L), any(PaymentRequest.class), eq("create-key-wallet"));
        verify(paymentIdempotencyService).completeCreate(createLock);
        verify(paymentService, never()).createPayment(any());
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

        when(currentUserAccessor.requireUserId()).thenReturn(1L);
        when(orderFacadeService.findOwnedOrderByOrderNo("ORD-1002", 1L)).thenReturn(order);
        when(orderFacadeService.buildPaymentOrderInfo(order)).thenReturn(Map.of(
                "subject", "优惠券订单",
                "description", "优惠券购买"
        ));
        IdempotencyLock createLock = new IdempotencyLock("create-key", "token-2");
        when(paymentIdempotencyService.resolveCreateKey(null, "ORD-1002", 1L, "alipay")).thenReturn("create-key");
        when(paymentIdempotencyService.tryAcquireCreate("create-key", PaymentIdempotencyService.CREATE_PROCESSING_TTL))
                .thenReturn(Optional.of(createLock));
        when(paymentCreateOrchestrator.create(eq(order), eq(1L), any(PaymentRequest.class), eq("create-key")))
                .thenThrow(new RuntimeException("alipay appId invalid"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentController.createPayment(null, request)
        );

        assertEquals(BusinessErrorCode.PAYMENT_CREATE_FAILED.getCode(), exception.getCode());
        assertEquals("创建支付订单失败，请稍后重试", exception.getMessage());
        verify(paymentIdempotencyService).releaseCreate(createLock);
        verify(paymentIdempotencyService, never()).completeCreate(createLock);
    }

    @Test
    void shouldRejectAlipayNotifyWhenPaidAmountDoesNotMatchOrder() {
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ORD-2001");
        params.put("total_amount", "8.80");

        when(paymentOrderDomainService.getBizOrderNoByPaymentNo("ORD-2001")).thenReturn("BIZ-2001");
        when(paymentService.handlePaymentCallback(eq("alipay"), anyString()))
                .thenReturn(PaymentCallbackResult.amountMismatch());

        String result = paymentController.alipayNotify(params);

        assertEquals("fail", result);
        verify(paymentService).handlePaymentCallback(eq("alipay"), anyString());
        verify(paymentOrderDomainService).saveCallbackLog(
                eq("alipay"),
                eq("PAY_NOTIFY"),
                eq("ORD-2001"),
                eq("BIZ-2001"),
                anyString(),
                eq(true),
                eq(false),
                eq("FAILED"),
                eq("AMOUNT_MISMATCH")
        );
    }
}
