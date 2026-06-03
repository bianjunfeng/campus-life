package com.campus.campus_life_ai.ai.gateway.observe;

import com.campus.campus_life_ai.ai.entity.AiCallLog;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayUsage;
import com.campus.campus_life_ai.ai.mapper.AiCallLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GatewayCallLoggerTest {

    private final AiCallLogMapper aiCallLogMapper = mock(AiCallLogMapper.class);
    private final GatewayCallLogger logger = new GatewayCallLogger(aiCallLogMapper);

    @Test
    void shouldPersistSuccessfulGatewayCall() {
        GatewayRequest request = buildRequest();
        GatewayResponse response = GatewayResponse.builder()
                .requestId("req-1")
                .content("reply text")
                .providerCode("provider-from-result")
                .modelCode("model-from-result")
                .latencyMs(145)
                .fallbackLevel(1)
                .usage(GatewayUsage.builder()
                        .promptTokens(10)
                        .completionTokens(6)
                        .totalTokens(16)
                        .costAmount(new java.math.BigDecimal("0.000160"))
                        .build())
                .build();

        logger.recordSuccess(request, response);

        AiCallLog callLog = captureInsertedLog();
        assertEquals("req-1", callLog.getRequestId());
        assertEquals("ses-1", callLog.getSessionId());
        assertEquals(1001L, callLog.getUserId());
        assertEquals("chat", callLog.getCapabilityCode());
        assertEquals("chat.general", callLog.getSceneCode());
        assertEquals("provider-from-result", callLog.getProviderCode());
        assertEquals("model-from-result", callLog.getModelCode());
        assertEquals(1, callLog.getSuccess());
        assertEquals(145, callLog.getLatencyMs());
        assertEquals(10, callLog.getPromptTokens());
        assertEquals(6, callLog.getCompletionTokens());
        assertEquals(16, callLog.getTotalTokens());
        assertEquals(1, callLog.getFallbackLevel());
        assertEquals(6, callLog.getPromptChars());
        assertEquals(10, callLog.getCompletionChars());
        assertEquals(new java.math.BigDecimal("0.000160"), callLog.getCostAmount());
        assertNotNull(callLog.getCreatedAt());
    }

    @Test
    void shouldPersistNormalizedGatewayFailure() {
        GatewayError error = GatewayError.builder()
                .errorCode("PROVIDER_TIMEOUT")
                .errorType("provider_timeout")
                .message("model timed out")
                .build();

        logger.recordFailure(buildRequest(), error);

        AiCallLog callLog = captureInsertedLog();
        assertEquals("provider-from-request", callLog.getProviderCode());
        assertEquals("model-from-request", callLog.getModelCode());
        assertEquals(0, callLog.getSuccess());
        assertEquals("PROVIDER_TIMEOUT", callLog.getErrorCode());
        assertEquals("provider_timeout", callLog.getErrorType());
        assertEquals("model timed out", callLog.getErrorMessage());
    }

    private GatewayRequest buildRequest() {
        return GatewayRequest.builder()
                .requestId("req-1")
                .sessionId("ses-1")
                .userId(1001L)
                .capabilityCode("chat")
                .sceneCode("chat.general")
                .providerCode("provider-from-request")
                .modelCode("model-from-request")
                .command(com.campus.campus_life_ai.ai.provider.ChatCompletionCommand.builder()
                        .messages(java.util.List.of(com.campus.campus_life_ai.ai.provider.ChatCompletionCommand.PromptMessage.builder()
                                .role("user")
                                .content("hello!")
                                .build()))
                        .build())
                .build();
    }

    private AiCallLog captureInsertedLog() {
        ArgumentCaptor<AiCallLog> captor = ArgumentCaptor.forClass(AiCallLog.class);
        verify(aiCallLogMapper).insert(captor.capture());
        return captor.getValue();
    }
}
