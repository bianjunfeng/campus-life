package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentCallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class WechatChannelHandlerTest {

    @Mock
    private PaymentCallbackService paymentCallbackService;

    private WechatChannelHandler wechatChannelHandler;

    @BeforeEach
    void setUp() {
        wechatChannelHandler = new WechatChannelHandler(paymentCallbackService);
    }

    /**
     * 能力矩阵（与 {@link WechatChannelHandler} 类注释一致）：
     * enabled=false -> create/query/refund 抛 WECHAT_PAYMENT_DISABLED；callback 拒绝
     * enabled=true  -> create/query/refund 抛 WECHAT_PAYMENT_NOT_INTEGRATED；callback 验签未实现拒绝
     */
    @ParameterizedTest(name = "enabled={0} operation={1} expects code {2}")
    @CsvSource({
            "false, create, 503601",
            "false, query, 503601",
            "false, refund, 503601",
            "true, create, 503603",
            "true, query, 503603",
            "true, refund, 503603",
    })
    void capabilityMatrix_enabledSwitch(boolean enabled, String operation, int expectedCode) {
        ReflectionTestUtils.setField(wechatChannelHandler, "enabled", enabled);
        PaymentRequest request = new PaymentRequest();
        request.setOrderNo("ORD-WX-1");
        request.setAmount(BigDecimal.TEN);

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            switch (operation) {
                case "create" -> wechatChannelHandler.createChannelPayment(request);
                case "query" -> wechatChannelHandler.queryPaymentStatus("PAY-WX-1");
                case "refund" -> wechatChannelHandler.refund("PAY-WX-1", BigDecimal.ONE, "r");
                default -> throw new IllegalArgumentException(operation);
            }
        });
        assertEquals(expectedCode, ex.getCode());
    }

    @Test
    void channelCode_isWechat() {
        assertEquals(PaymentMethod.WECHAT.getCode(), wechatChannelHandler.channelCode());
    }

    @Test
    void handlePaymentCallback_disabled_rejectsWithDisabledLogPath() {
        ReflectionTestUtils.setField(wechatChannelHandler, "enabled", false);
        assertFalse(wechatChannelHandler.handlePaymentCallback("wechat", "{\"out_trade_no\":\"PAY-1\"}").isSuccess());
    }

    @Test
    void handlePaymentCallback_enabled_rejectsWhenVerifyNotImplemented() {
        ReflectionTestUtils.setField(wechatChannelHandler, "enabled", true);
        assertFalse(wechatChannelHandler.handlePaymentCallback(
                "wechat",
                "{\"out_trade_no\":\"PAY-1\",\"trade_state\":\"SUCCESS\"}"
        ).isSuccess());
    }

    @Test
    void handlePaymentCallback_enabled_invalidJson_rejects() {
        ReflectionTestUtils.setField(wechatChannelHandler, "enabled", true);
        assertFalse(wechatChannelHandler.handlePaymentCallback("wechat", "not-json").isSuccess());
    }

    @Test
    void disabled_throwsWechatPaymentDisabledCode() {
        ReflectionTestUtils.setField(wechatChannelHandler, "enabled", false);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> wechatChannelHandler.createChannelPayment(new PaymentRequest())
        );
        assertEquals(BusinessErrorCode.WECHAT_PAYMENT_DISABLED.getCode(), ex.getCode());
    }

    @Test
    void enabled_throwsNotIntegratedCode() {
        ReflectionTestUtils.setField(wechatChannelHandler, "enabled", true);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> wechatChannelHandler.createChannelPayment(new PaymentRequest())
        );
        assertEquals(BusinessErrorCode.WECHAT_PAYMENT_NOT_INTEGRATED.getCode(), ex.getCode());
    }
}
