package com.campus.campus_life_backend.modules.forum.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.forum.config.AiModerationProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiContentModerationServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private AiModerationProperties properties;
    private AiContentModerationService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        properties = new AiModerationProperties();
        properties.setBaseUrl("http://ai.test");
        service = new AiContentModerationService(restTemplate, properties);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        server.verify();
    }

    @Test
    void shouldForwardAuthorizationAndAllowPassedPost() {
        bindAuthorization("Bearer user-token");
        server.expect(once(), requestTo("http://ai.test/api/ai/moderation/check"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(jsonPath("$.targetType").value("post"))
                .andExpect(jsonPath("$.content").value("标题\n正常内容"))
                .andRespond(withSuccess("""
                        {"code":200,"message":"success","data":{"result":"PASS","score":0.01,"categories":[],"reason":"ok"}}
                        """, MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> service.checkPost(7L, "标题", "正常内容"));
    }

    @Test
    void shouldRejectContentWhenAiReturnsReject() {
        bindAuthorization("Bearer user-token");
        server.expect(once(), requestTo("http://ai.test/api/ai/moderation/check"))
                .andRespond(withSuccess("""
                        {"code":200,"message":"success","data":{"result":"REJECT","score":0.94,"categories":["spam"],"reason":"广告引流"}}
                        """, MediaType.APPLICATION_JSON));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.checkComment(7L, "加微信返利")
        );

        assertEquals(BusinessErrorCode.INVALID_PARAM.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("评论内容未通过 AI 审核"));
        assertTrue(exception.getMessage().contains("广告引流"));
    }

    @Test
    void shouldFailOpenWhenAiServiceUnavailable() {
        bindAuthorization("Bearer user-token");
        server.expect(once(), requestTo("http://ai.test/api/ai/moderation/check"))
                .andRespond(withServerError());

        assertDoesNotThrow(() -> service.checkPost(7L, "标题", "内容"));
    }

    @Test
    void shouldFailClosedWhenConfigured() {
        properties.setFailOpen(false);
        bindAuthorization("Bearer user-token");
        server.expect(once(), requestTo("http://ai.test/api/ai/moderation/check"))
                .andRespond(withServerError());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.checkPost(7L, "标题", "内容")
        );

        assertEquals(BusinessErrorCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
    }

    @Test
    void shouldSkipWhenDisabled() {
        properties.setEnabled(false);
        bindAuthorization("Bearer user-token");

        assertDoesNotThrow(() -> service.checkPost(7L, "标题", "内容"));
    }

    private void bindAuthorization(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
