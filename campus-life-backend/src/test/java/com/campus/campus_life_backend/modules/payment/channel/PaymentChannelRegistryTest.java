package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentChannelRegistryTest {

    @Test
    void require_unknownChannelCode_throwsIllegalArgumentException() {
        PaymentChannelRegistry registry = new PaymentChannelRegistry(List.of(stubHandler(PaymentMethod.ALIPAY)));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> registry.require("unknown")
        );
        assertEquals("未知的支付渠道: unknown", ex.getMessage());
    }

    @Test
    void constructor_duplicateChannelCode_throwsIllegalStateException() {
        PaymentChannelHandler alipay1 = stubHandler(PaymentMethod.ALIPAY);
        PaymentChannelHandler alipay2 = stubHandler(PaymentMethod.ALIPAY);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new PaymentChannelRegistry(List.of(alipay1, alipay2))
        );
        assertEquals("重复的支付渠道编码: alipay", ex.getMessage());
    }

    @Test
    void require_knownChannelCode_returnsHandler() {
        PaymentChannelHandler alipay = stubHandler(PaymentMethod.ALIPAY);
        PaymentChannelRegistry registry = new PaymentChannelRegistry(List.of(alipay, stubHandler(PaymentMethod.WECHAT)));

        assertSame(alipay, registry.require(PaymentMethod.ALIPAY.getCode()));
    }

    private static PaymentChannelHandler stubHandler(PaymentMethod method) {
        return new PaymentChannelHandler() {
            @Override
            public String channelCode() {
                return method.getCode();
            }

            @Override
            public PaymentResponse createChannelPayment(PaymentRequest request) {
                return null;
            }

            @Override
            public boolean handlePaymentCallback(String paymentMethod, String callbackData) {
                return false;
            }

            @Override
            public String queryPaymentStatus(String orderNo) {
                return "UNKNOWN";
            }

            @Override
            public boolean refund(String orderNo, BigDecimal refundAmount, String refundReason) {
                return false;
            }
        };
    }
}
