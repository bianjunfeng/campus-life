package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentCallbackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletChannelHandlerTest {

    @Mock
    private PaymentCallbackService paymentCallbackService;

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
    void createChannelPayment_rejectsDirectChannelCall() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> walletChannelHandler.createChannelPayment(new PaymentRequest())
        );
        assertEquals("钱包支付请通过统一支付入口处理", ex.getMessage());
    }
}
