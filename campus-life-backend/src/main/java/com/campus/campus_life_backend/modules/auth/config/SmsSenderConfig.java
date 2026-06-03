package com.campus.campus_life_backend.modules.auth.config;

import com.campus.campus_life_backend.modules.auth.service.SmsSender;
import com.campus.campus_life_backend.modules.auth.service.impl.DevLoggingSmsSender;
import com.campus.campus_life_backend.modules.auth.service.impl.FailingSmsSender;
import com.campus.campus_life_backend.modules.auth.service.impl.HttpSmsSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;

@Configuration
public class SmsSenderConfig {

    @Bean
    @ConditionalOnMissingBean(SmsSender.class)
    public SmsSender smsSender(Environment environment,
                               RestTemplateBuilder restTemplateBuilder,
                               @Value("${auth.sms.provider:}") String provider,
                               @Value("${auth.sms.http.endpoint:}") String httpEndpoint,
                               @Value("${auth.sms.http.api-key:}") String httpApiKey,
                               @Value("${auth.sms.http.api-key-header:X-API-Key}") String httpApiKeyHeader,
                               @Value("${auth.sms.http.sign-name:}") String signName,
                               @Value("${auth.sms.http.template-code:}") String templateCode,
                               @Value("${auth.sms.http.connect-timeout-ms:3000}") long connectTimeoutMs,
                               @Value("${auth.sms.http.read-timeout-ms:5000}") long readTimeoutMs) {
        String normalizedProvider = provider == null ? "" : provider.trim().toLowerCase();
        if (isNonProduction(environment) && (normalizedProvider.isBlank() || "dev-log".equals(normalizedProvider))) {
            return new DevLoggingSmsSender();
        }
        if ("http".equals(normalizedProvider)) {
            if (!StringUtils.hasText(httpEndpoint) || !StringUtils.hasText(httpApiKey)) {
                return new FailingSmsSender("短信HTTP网关配置不完整，无法发送验证码");
            }
            return new HttpSmsSender(
                    restTemplateBuilder
                            .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                            .readTimeout(Duration.ofMillis(readTimeoutMs))
                            .build(),
                    httpEndpoint,
                    httpApiKey,
                    httpApiKeyHeader,
                    signName,
                    templateCode
            );
        }
        return new FailingSmsSender();
    }

    private boolean isNonProduction(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "local".equals(profile) || "dev".equals(profile) || "test".equals(profile));
    }
}
