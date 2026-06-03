package com.campus.campus_life_backend.modules.payment.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentProductionConfigValidatorTest {

    @Test
    void shouldRejectSandboxAlipayConfigInProd() {
        MockEnvironment environment = prodEnvironment()
                .withProperty("payment.alipay.enabled", "true")
                .withProperty("payment.alipay.app-id", "app-id")
                .withProperty("payment.alipay.private-key", "private-key")
                .withProperty("payment.alipay.public-key", "public-key")
                .withProperty("payment.alipay.skip-notify-sign-verify", "true")
                .withProperty("payment.alipay.gateway-url", "https://openapi-sandbox.dl.alipaydev.com/gateway.do")
                .withProperty("payment.alipay.notify-url", "https://demo.ngrok-free.dev/api/payment/alipay/notify")
                .withProperty("payment.alipay.return-url", "http://localhost:5173/payment/result")
                .withProperty("payment.frontend-base-url", "http://localhost:5173");

        assertThrows(IllegalStateException.class, () -> new PaymentProductionConfigValidator(environment).validate());
    }

    @Test
    void shouldAcceptProductionAlipayConfigInProd() {
        MockEnvironment environment = prodEnvironment()
                .withProperty("payment.alipay.enabled", "true")
                .withProperty("payment.alipay.app-id", "app-id")
                .withProperty("payment.alipay.private-key", "private-key")
                .withProperty("payment.alipay.public-key", "public-key")
                .withProperty("payment.alipay.skip-notify-sign-verify", "false")
                .withProperty("payment.alipay.gateway-url", "https://openapi.alipay.com/gateway.do")
                .withProperty("payment.alipay.notify-url", "https://api.campus.example.cn/api/payment/alipay/notify")
                .withProperty("payment.alipay.return-url", "https://www.campus.example.cn/payment/result")
                .withProperty("payment.frontend-base-url", "https://www.campus.example.cn");

        assertDoesNotThrow(() -> new PaymentProductionConfigValidator(environment).validate());
    }

    @Test
    void shouldSkipValidationOutsideProd() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("payment.alipay.enabled", "true")
                .withProperty("payment.alipay.skip-notify-sign-verify", "true")
                .withProperty("payment.alipay.gateway-url", "https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        environment.setActiveProfiles("dev");

        assertDoesNotThrow(() -> new PaymentProductionConfigValidator(environment).validate());
    }

    private MockEnvironment prodEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }
}
