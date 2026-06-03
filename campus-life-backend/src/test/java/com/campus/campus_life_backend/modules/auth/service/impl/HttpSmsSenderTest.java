package com.campus.campus_life_backend.modules.auth.service.impl;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.auth.service.VerificationScene;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class HttpSmsSenderTest {

    private MockRestServiceServer server;
    private HttpSmsSender smsSender;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        smsSender = new HttpSmsSender(
                restTemplate,
                "https://sms-gateway.example.com/send",
                "secret-key",
                "X-API-Key",
                "校园生活平台",
                "SMS_VERIFY"
        );
    }

    @Test
    void shouldPostVerificationPayloadToHttpGateway() {
        server.expect(requestTo("https://sms-gateway.example.com/send"))
                .andExpect(method(POST))
                .andExpect(header("X-API-Key", "secret-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "phone": "13800138000",
                          "scene": "login",
                          "signName": "校园生活平台",
                          "templateCode": "SMS_VERIFY",
                          "templateParams": {
                            "code": "123456"
                          }
                        }
                        """))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        smsSender.sendVerificationCode("13800138000", "123456", VerificationScene.LOGIN);

        server.verify();
    }

    @Test
    void shouldFailWhenHttpGatewayReturnsError() {
        server.expect(requestTo("https://sms-gateway.example.com/send"))
                .andRespond(withServerError());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> smsSender.sendVerificationCode("13800138000", "123456", VerificationScene.REGISTER));

        assertEquals(BusinessErrorCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
        assertEquals("验证码发送失败，请稍后重试", exception.getMessage());
        server.verify();
    }
}
