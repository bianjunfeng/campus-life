package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.modules.payment.dto.PaymentCallbackResult;
import com.campus.campus_life_backend.modules.payment.service.PaymentCallbackService;
import com.campus.campus_life_backend.modules.payment.service.PaymentOrderDomainService;
import com.campus.campus_life_backend.modules.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlipayChannelHandlerTest {

    @Mock
    private PaymentCallbackService paymentCallbackService;

    @Mock
    private PaymentOrderDomainService paymentOrderDomainService;

    private AlipayChannelHandler alipayChannelHandler;

    @BeforeEach
    void setUp() {
        alipayChannelHandler = new AlipayChannelHandler(paymentCallbackService, paymentOrderDomainService);
    }

    @Test
    void implementsPaymentChannelHandlerOnly() {
        assertTrue(PaymentChannelHandler.class.isAssignableFrom(AlipayChannelHandler.class));
        assertFalse(PaymentService.class.isAssignableFrom(AlipayChannelHandler.class));
    }

    @Test
    void handlePaymentCallback_signatureInvalidWhenPublicKeyMissing() {
        ReflectionTestUtils.setField(alipayChannelHandler, "skipAlipayNotifySignVerify", false);
        ReflectionTestUtils.setField(alipayChannelHandler, "alipayPublicKey", "");

        PaymentCallbackResult result = alipayChannelHandler.handlePaymentCallback(
                "alipay",
                "out_trade_no=PAY-1&total_amount=10.00&sign=abc"
        );

        assertFalse(result.isSuccess());
        assertFalse(result.isSignatureVerified());
        assertEquals("SIGNATURE_INVALID", result.getErrorCode());
        verify(paymentCallbackService, never()).handlePaymentSuccess(anyString(), anyString(), anyString());
    }

    @Test
    void handlePaymentCallback_amountMismatch() {
        ReflectionTestUtils.setField(alipayChannelHandler, "skipAlipayNotifySignVerify", true);
        when(paymentOrderDomainService.getExpectedAmountByPaymentNo("PAY-1")).thenReturn(new BigDecimal("88.00"));

        PaymentCallbackResult result = alipayChannelHandler.handlePaymentCallback(
                "alipay",
                "out_trade_no=PAY-1&total_amount=8.80&trade_status=TRADE_SUCCESS"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.isSignatureVerified());
        assertFalse(result.isAmountVerified());
        assertEquals("AMOUNT_MISMATCH", result.getErrorCode());
        verify(paymentCallbackService, never()).handlePaymentSuccess(anyString(), anyString(), anyString());
    }

    @Test
    void handlePaymentCallback_success() {
        ReflectionTestUtils.setField(alipayChannelHandler, "skipAlipayNotifySignVerify", true);
        when(paymentOrderDomainService.getExpectedAmountByPaymentNo("PAY-1")).thenReturn(new BigDecimal("10.00"));
        when(paymentCallbackService.handlePaymentSuccess("PAY-1", "alipay", "ALI-123")).thenReturn(true);

        PaymentCallbackResult result = alipayChannelHandler.handlePaymentCallback(
                "alipay",
                "out_trade_no=PAY-1&total_amount=10.00&trade_status=TRADE_SUCCESS&trade_no=ALI-123"
        );

        assertTrue(result.isSuccess());
        assertTrue(result.isSignatureVerified());
        assertTrue(result.isAmountVerified());
        verify(paymentCallbackService).handlePaymentSuccess("PAY-1", "alipay", "ALI-123");
    }

    @Test
    void shouldEncodePassbackParamsForAlipay() {
        String encoded = AlipayChannelHandler.encodePassbackParams("returnTo=orders&orderTab=pending_payment");

        assertEquals("returnTo%3Dorders%26orderTab%3Dpending_payment", encoded);
    }

    @Test
    void shouldFormatAmountToTwoDecimals() {
        assertEquals("8.80", AlipayChannelHandler.formatAmount(new BigDecimal("8.8")));
    }

    @Test
    void shouldSanitizeSubjectAndBodyToSafePlainText() {
        assertEquals("校园咖啡套餐", AlipayChannelHandler.sanitizeSubject("校园咖啡套餐\uD83D\uDE80"));
        assertEquals("限时特惠 2 人餐", AlipayChannelHandler.sanitizeBody(" 限时特惠 \n 2 人餐 \uD83C\uDF89 "));
    }
}
