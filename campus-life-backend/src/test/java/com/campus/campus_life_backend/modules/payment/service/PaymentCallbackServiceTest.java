package com.campus.campus_life_backend.modules.payment.service;

import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.order.event.OrderPaidEvent;
import com.campus.campus_life_backend.modules.order.event.VoucherOrderKafkaEventPublisher;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentOrderStatus;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentOrderMapper;
import com.campus.campus_life_backend.modules.user.mapper.UserWalletMapper;
import com.campus.campus_life_backend.modules.user.mapper.WalletTransactionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackServiceTest {

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    @Mock
    private VoucherOrderService voucherOrderService;

    @Mock
    private UserWalletMapper userWalletMapper;

    @Mock
    private WalletTransactionMapper walletTransactionMapper;

    @Mock
    private PaymentIdempotencyService paymentIdempotencyService;

    @Mock
    private VoucherOrderKafkaEventPublisher voucherOrderKafkaEventPublisher;

    @InjectMocks
    private PaymentCallbackService paymentCallbackService;

    @Test
    void payByWalletShouldMarkOrderPaidEvenWhenKafkaEnabled() {
        VoucherOrder order = new VoucherOrder();
        order.setId(10L);
        order.setOrderNo("ORD-1001");
        order.setUserId(1L);
        order.setPaymentStatus(0);
        order.setPayAmount(new BigDecimal("12.50"));

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-1001");
        paymentOrder.setStatus(PaymentOrderStatus.INIT.getCode());
        paymentOrder.setVersion(0);

        when(userWalletMapper.deductBalanceAndAddSpent(1L, new BigDecimal("12.50"))).thenReturn(1);
        when(paymentOrderMapper.markSuccessByPaymentNo(
                eq("PAY-1001"),
                eq("WALLET_PAY-1001"),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(0)
        )).thenReturn(1);
        when(voucherOrderService.markOrderPaid(eq(10L), eq("wallet"), any(LocalDateTime.class))).thenReturn(true);
        when(voucherOrderKafkaEventPublisher.isEnabled()).thenReturn(true);
        when(userWalletMapper.findByUserId(1L)).thenReturn(Map.of("balance", new BigDecimal("987.50")));

        paymentCallbackService.payByWallet(order, paymentOrder, 1L);

        verify(voucherOrderService).markOrderPaid(eq(10L), eq("wallet"), any(LocalDateTime.class));
        ArgumentCaptor<OrderPaidEvent> eventCaptor = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(voucherOrderKafkaEventPublisher).publish(eventCaptor.capture());
        OrderPaidEvent event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals("ORD-1001", event.getOrderNo());
        assertEquals("PAY-1001", event.getPaymentNo());
        assertEquals("wallet", event.getPaymentMethod());
        verify(walletTransactionMapper).insertExpenseTransaction(
                eq(1L),
                eq(new BigDecimal("12.50")),
                eq(new BigDecimal("987.50")),
                eq("ORD-1001"),
                eq("钱包支付")
        );
    }

    @Test
    void payByWalletShouldRepairPendingOrderWhenPaymentOrderAlreadySucceeded() {
        VoucherOrder order = new VoucherOrder();
        order.setId(11L);
        order.setOrderNo("ORD-1002");
        order.setUserId(1L);
        order.setPaymentStatus(0);
        order.setPayAmount(new BigDecimal("8.00"));

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-1002");
        paymentOrder.setStatus(PaymentOrderStatus.SUCCESS.getCode());
        paymentOrder.setSuccessTime(LocalDateTime.now().minusSeconds(5));
        paymentOrder.setVersion(1);

        when(voucherOrderService.markOrderPaid(eq(11L), eq("wallet"), any(LocalDateTime.class))).thenReturn(true);

        paymentCallbackService.payByWallet(order, paymentOrder, 1L);

        verify(voucherOrderService).markOrderPaid(eq(11L), eq("wallet"), any(LocalDateTime.class));
        verify(userWalletMapper, never()).deductBalanceAndAddSpent(any(), any());
        verify(walletTransactionMapper, never()).insertExpenseTransaction(any(), any(), any(), any(), any());
    }
}
