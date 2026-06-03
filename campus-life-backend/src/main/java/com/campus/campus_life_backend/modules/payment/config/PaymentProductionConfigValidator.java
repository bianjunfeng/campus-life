package com.campus.campus_life_backend.modules.payment.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class PaymentProductionConfigValidator {

    private final Environment environment;

    public PaymentProductionConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        if (!isProdProfileActive()) {
            return;
        }

        List<String> errors = new ArrayList<>();
        validateAlipay(errors);
        validateWechat(errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid production payment configuration: " + String.join("; ", errors));
        }
    }

    private boolean isProdProfileActive() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    private void validateAlipay(List<String> errors) {
        if (!environment.getProperty("payment.alipay.enabled", Boolean.class, false)) {
            return;
        }

        requirePresent(errors, "payment.alipay.app-id");
        requirePresent(errors, "payment.alipay.private-key");
        requirePresent(errors, "payment.alipay.public-key");

        if (environment.getProperty("payment.alipay.skip-notify-sign-verify", Boolean.class, false)) {
            errors.add("payment.alipay.skip-notify-sign-verify must be false in prod");
        }

        String gatewayUrl = environment.getProperty("payment.alipay.gateway-url", "");
        if (isBlank(gatewayUrl)) {
            errors.add("payment.alipay.gateway-url is required when Alipay is enabled");
        } else if (containsAny(gatewayUrl, "alipaydev.com", "openapi-sandbox")) {
            errors.add("payment.alipay.gateway-url must use the production Alipay gateway in prod");
        }

        requirePublicHttpsUrl(errors, "payment.alipay.notify-url");
        requirePublicHttpsUrl(errors, "payment.alipay.return-url");
        requirePublicHttpsUrl(errors, "payment.frontend-base-url");
    }

    private void validateWechat(List<String> errors) {
        if (!environment.getProperty("payment.wechat.enabled", Boolean.class, false)) {
            return;
        }

        requirePresent(errors, "payment.wechat.app-id");
        requirePresent(errors, "payment.wechat.mch-id");
        requirePresent(errors, "payment.wechat.api-v3-key");
        requirePresent(errors, "payment.wechat.private-key-path");
        requirePresent(errors, "payment.wechat.certificate-serial-number");
        requirePublicHttpsUrl(errors, "payment.wechat.notify-url");
    }

    private void requirePresent(List<String> errors, String key) {
        if (isBlank(environment.getProperty(key))) {
            errors.add(key + " is required");
        }
    }

    private void requirePublicHttpsUrl(List<String> errors, String key) {
        String value = environment.getProperty(key, "");
        if (isBlank(value)) {
            errors.add(key + " is required");
            return;
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException e) {
            errors.add(key + " must be a valid URL");
            return;
        }

        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            errors.add(key + " must use HTTPS in prod");
        }
        if (isBlank(host) || isNonProductionHost(host)) {
            errors.add(key + " must use a real production domain");
        }
    }

    private boolean isNonProductionHost(String host) {
        String normalized = host.toLowerCase();
        return normalized.equals("localhost")
                || normalized.equals("127.0.0.1")
                || normalized.endsWith(".local")
                || normalized.contains("ngrok")
                || normalized.contains("your-domain")
                || normalized.contains("example.com");
    }

    private boolean containsAny(String value, String... fragments) {
        String normalized = value.toLowerCase();
        return Arrays.stream(fragments).anyMatch(normalized::contains);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
