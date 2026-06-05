package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentCallbackService;
import com.campus.campus_life_backend.modules.payment.service.PaymentOrderDomainService;
import com.campus.campus_life_backend.modules.user.mapper.UserWalletMapper;
import com.campus.campus_life_backend.modules.user.mapper.WalletTransactionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletChannelHandlerTest {

    @Mock
    private PaymentCallbackService paymentCallbackService;

    @Mock
    private PaymentOrderDomainService paymentOrderDomainService;

    @Mock
    private UserWalletMapper userWalletMapper;

    @Mock
    private WalletTransactionMapper walletTransactionMapper;

    @InjectMocks
    private WalletChannelHandler walletChannelHandler;

    @Test
    void channelCode_isWallet() {
        assertEquals(PaymentMethod.WALLET.getCode(), walletChannelHandler.channelCode());
        assertFalse(walletChannelHandler.isExternalChannel());
    }

    @Test
    void paySynchronously_success_returnsWalletResponse() {
        VoucherOrder order = new VoucherOrder();
        order.setOrderNo("ORD-W1");
        order.setUserId(1L);
        order.setPayAmount(new BigDecimal("10.00"));

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-W1");

        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod("wallet");
        request.setOrderNo("ORD-W1");

        PaymentResponse response = walletChannelHandler.paySynchronously(order, 1L, request, paymentOrder);

        verify(paymentCallbackService).payByWallet(order, paymentOrder, 1L);
        assertEquals("wallet", response.getPaymentMethod());
        assertEquals("PAY-W1", response.getPaymentOrderNo());
    }

    @Test
    void paySynchronously_insufficientBalance_propagatesBusinessException() {
        VoucherOrder order = new VoucherOrder();
        order.setOrderNo("ORD-W2");
        order.setUserId(2L);

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-W2");

        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod("wallet");

        doThrow(new BusinessException(BusinessErrorCode.WALLET_BALANCE_NOT_ENOUGH))
                .when(paymentCallbackService).payByWallet(order, paymentOrder, 2L);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> walletChannelHandler.paySynchronously(order, 2L, request, paymentOrder)
        );
        assertEquals(BusinessErrorCode.WALLET_BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    void refund_creditsWalletAndReturnsTrue() {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-W3");
        paymentOrder.setUserId(3L);
        paymentOrder.setBizOrderNo("ORD-W3");

        when(paymentOrderDomainService.findByPaymentNo("PAY-W3")).thenReturn(paymentOrder);
        when(userWalletMapper.addBalanceAndReduceSpent(3L, new BigDecimal("5.00"))).thenReturn(1);

        assertTrue(walletChannelHandler.refund("PAY-W3", new BigDecimal("5.00"), "用户申请"));

        verify(userWalletMapper).initWalletIfAbsent(3L, BigDecimal.ZERO);
        verify(walletTransactionMapper).insertRefundTransaction(
                eq(3L),
                eq(new BigDecimal("5.00")),
                any(),
                eq("ORD-W3"),
                eq("钱包退款：用户申请")
        );
    }

    @Test
    void refund_missingPaymentOrder_returnsFalse() {
        when(paymentOrderDomainService.findByPaymentNo("PAY-MISS")).thenReturn(null);
        assertFalse(walletChannelHandler.refund("PAY-MISS", BigDecimal.ONE, "r"));
        verify(userWalletMapper, never()).addBalanceAndReduceSpent(any(), any());
    }

    @Test
    void refund_walletCreditFails_returnsFalse() {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-W4");
        paymentOrder.setUserId(4L);
        paymentOrder.setBizOrderNo("ORD-W4");

        when(paymentOrderDomainService.findByPaymentNo("PAY-W4")).thenReturn(paymentOrder);
        when(userWalletMapper.addBalanceAndReduceSpent(4L, new BigDecimal("3.00"))).thenReturn(0);

        assertFalse(walletChannelHandler.refund("PAY-W4", new BigDecimal("3.00"), "失败场景"));

        verify(userWalletMapper).initWalletIfAbsent(4L, BigDecimal.ZERO);
        verify(walletTransactionMapper, never()).insertRefundTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void refund_blankReason_usesDefaultRemark() {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-W5");
        paymentOrder.setUserId(5L);
        paymentOrder.setBizOrderNo("ORD-W5");

        when(paymentOrderDomainService.findByPaymentNo("PAY-W5")).thenReturn(paymentOrder);
        when(userWalletMapper.addBalanceAndReduceSpent(5L, BigDecimal.ONE)).thenReturn(1);

        assertTrue(walletChannelHandler.refund("PAY-W5", BigDecimal.ONE, "  "));

        verify(walletTransactionMapper).insertRefundTransaction(
                eq(5L),
                eq(BigDecimal.ONE),
                any(),
                eq("ORD-W5"),
                eq("钱包退款")
        );
    }

    @Test
    void createChannelPayment_rejectsDirectChannelCall() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> walletChannelHandler.createChannelPayment(new PaymentRequest())
        );
        assertEquals("钱包支付请通过统一支付入口处理", ex.getMessage());
    }

    @Test
    void queryPaymentStatus_mapsLocalPaymentOrderStatus() {
        PaymentOrder success = new PaymentOrder();
        success.setPaymentNo("PAY-W6");
        success.setStatus("SUCCESS");

        PaymentOrder waitPay = new PaymentOrder();
        waitPay.setPaymentNo("PAY-W7");
        waitPay.setStatus("WAIT_PAY");

        PaymentOrder failed = new PaymentOrder();
        failed.setPaymentNo("PAY-W8");
        failed.setStatus("CLOSED");

        when(paymentOrderDomainService.findByPaymentNo("PAY-W6")).thenReturn(success);
        when(paymentOrderDomainService.findByPaymentNo("PAY-W7")).thenReturn(waitPay);
        when(paymentOrderDomainService.findByPaymentNo("PAY-W8")).thenReturn(failed);
        when(paymentOrderDomainService.findByPaymentNo("PAY-MISS")).thenReturn(null);

        assertEquals("SUCCESS", walletChannelHandler.queryPaymentStatus("PAY-W6"));
        assertEquals("PENDING", walletChannelHandler.queryPaymentStatus("PAY-W7"));
        assertEquals("FAILED", walletChannelHandler.queryPaymentStatus("PAY-W8"));
        assertEquals("UNKNOWN", walletChannelHandler.queryPaymentStatus("PAY-MISS"));
    }

    @Test
    void mapLocalPaymentStatus_matrix() {
        assertEquals("SUCCESS", WalletChannelHandler.mapLocalPaymentStatus("FULL_REFUNDED"));
        assertEquals("FAILED", WalletChannelHandler.mapLocalPaymentStatus("FAILED"));
        assertEquals("PENDING", WalletChannelHandler.mapLocalPaymentStatus("INIT"));
        assertEquals("PENDING", WalletChannelHandler.mapLocalPaymentStatus(null));
    }
}
