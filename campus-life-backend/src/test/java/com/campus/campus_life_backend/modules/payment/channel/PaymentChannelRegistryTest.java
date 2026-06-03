package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.modules.payment.dto.PaymentCallbackResult;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentChannelRegistryTest {

    @Test
    void require_unknownChannelCode_throwsIllegalArgumentException() {
        PaymentChannelRegistry registry = new PaymentChannelRegistry(allStubHandlers());

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
    void constructor_missingWalletHandler_throwsIllegalStateException() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new PaymentChannelRegistry(List.of(
                        stubHandler(PaymentMethod.ALIPAY),
                        stubHandler(PaymentMethod.WECHAT)
                ))
        );
        assertEquals("缺少支付渠道 Handler: wallet", ex.getMessage());
    }

    @Test
    void constructor_missingAlipayHandler_listsMissingChannel() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new PaymentChannelRegistry(List.of(
                        stubHandler(PaymentMethod.WECHAT),
                        stubHandler(PaymentMethod.WALLET)
                ))
        );
        assertEquals("缺少支付渠道 Handler: alipay", ex.getMessage());
    }

    @Test
    void constructor_unknownChannelCode_throwsIllegalStateException() {
        PaymentChannelHandler wild = stubHandlerWithCode("bitcoin");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> {
                    List<PaymentChannelHandler> handlers = new ArrayList<>(allStubHandlers());
                    handlers.add(wild);
                    new PaymentChannelRegistry(handlers);
                }
        );
        assertEquals("未识别的支付渠道编码: bitcoin", ex.getMessage());
    }

    @Test
    void require_knownChannelCode_returnsHandler() {
        PaymentChannelHandler alipay = stubHandler(PaymentMethod.ALIPAY);
        PaymentChannelRegistry registry = new PaymentChannelRegistry(List.of(
                alipay,
                stubHandler(PaymentMethod.WECHAT),
                stubHandler(PaymentMethod.WALLET)
        ));

        assertSame(alipay, registry.require(PaymentMethod.ALIPAY.getCode()));
    }

    private static List<PaymentChannelHandler> allStubHandlers() {
        return Arrays.stream(PaymentMethod.values())
                .map(PaymentChannelRegistryTest::stubHandler)
                .collect(Collectors.toList());
    }

    private static PaymentChannelHandler stubHandler(PaymentMethod method) {
        return stubHandlerWithCode(method.getCode());
    }

    private static PaymentChannelHandler stubHandlerWithCode(String channelCode) {
        return new PaymentChannelHandler() {
            @Override
            public String channelCode() {
                return channelCode;
            }

            @Override
            public PaymentResponse createChannelPayment(PaymentRequest request) {
                return null;
            }

            @Override
            public PaymentCallbackResult handlePaymentCallback(String paymentMethod, String callbackData) {
                return PaymentCallbackResult.processFailed();
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
