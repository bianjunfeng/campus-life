package com.campus.campus_life_backend.modules.payment.service.impl;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlipayPaymentServiceImplTest {

    @Test
    void shouldEncodePassbackParamsForAlipay() {
        String encoded = AlipayPaymentServiceImpl.encodePassbackParams("returnTo=orders&orderTab=pending_payment");

        assertEquals("returnTo%3Dorders%26orderTab%3Dpending_payment", encoded);
    }

    @Test
    void shouldFormatAmountToTwoDecimals() {
        assertEquals("8.80", AlipayPaymentServiceImpl.formatAmount(new BigDecimal("8.8")));
    }

    @Test
    void shouldSanitizeSubjectAndBodyToSafePlainText() {
        assertEquals("校园咖啡套餐", AlipayPaymentServiceImpl.sanitizeSubject("校园咖啡套餐\uD83D\uDE80"));
        assertEquals("限时特惠 2 人餐", AlipayPaymentServiceImpl.sanitizeBody(" 限时特惠 \n 2 人餐 \uD83C\uDF89 "));
    }
}
