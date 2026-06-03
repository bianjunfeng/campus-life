package com.campus.campus_life_backend.modules.auth.service.impl;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.auth.service.SmsSender;
import com.campus.campus_life_backend.modules.auth.service.VerificationScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpSmsSender implements SmsSender {

    public static final String DEFAULT_API_KEY_HEADER = "X-API-Key";

    private static final Logger logger = LoggerFactory.getLogger(HttpSmsSender.class);

    private final RestTemplate restTemplate;
    private final URI endpoint;
    private final String apiKey;
    private final String apiKeyHeader;
    private final String signName;
    private final String templateCode;

    public HttpSmsSender(RestTemplate restTemplate,
                         String endpoint,
                         String apiKey,
                         String apiKeyHeader,
                         String signName,
                         String templateCode) {
        this.restTemplate = restTemplate;
        this.endpoint = URI.create(endpoint);
        this.apiKey = apiKey;
        this.apiKeyHeader = StringUtils.hasText(apiKeyHeader) ? apiKeyHeader : DEFAULT_API_KEY_HEADER;
        this.signName = signName;
        this.templateCode = templateCode;
    }

    @Override
    public void sendVerificationCode(String phone, String code, VerificationScene scene) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(apiKeyHeader, apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("phone", phone);
        body.put("scene", scene.code());
        body.put("signName", signName);
        body.put("templateCode", templateCode);
        body.put("templateParams", Map.of("code", code));

        try {
            restTemplate.postForEntity(endpoint, new HttpEntity<>(body, headers), String.class);
        } catch (RestClientResponseException e) {
            logger.warn("短信网关发送失败 status={}, phone={}, scene={}",
                    e.getStatusCode().value(), phone, scene.code());
            throw sendFailed(e);
        } catch (RestClientException e) {
            logger.warn("短信网关请求失败 phone={}, scene={}", phone, scene.code());
            throw sendFailed(e);
        }
    }

    private BusinessException sendFailed(Exception cause) {
        return new BusinessException(
                BusinessErrorCode.SERVICE_UNAVAILABLE,
                "验证码发送失败，请稍后重试",
                cause
        );
    }
}
