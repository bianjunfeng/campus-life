package com.campus.campus_life_ai.ai.gateway.observe;

import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayUsage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayMetricsRecorderTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final GatewayMetricsRecorder recorder = new GatewayMetricsRecorder(meterRegistry);

    @Test
    void shouldRecordSuccessCountersLatencyAndTokens() {
        GatewayRequest request = buildRequest();
        GatewayResponse response = GatewayResponse.builder()
                .providerCode("provider-from-result")
                .modelCode("model-from-result")
                .latencyMs(220)
                .usage(GatewayUsage.builder()
                        .promptTokens(12)
                        .completionTokens(8)
                        .totalTokens(20)
                        .costAmount(new BigDecimal("0.001200"))
                        .build())
                .build();

        recorder.recordSuccess(request, response);

        assertEquals(1D, meterRegistry.find("ai.gateway.requests")
                .tag("provider", "provider-from-result")
                .tag("model", "model-from-result")
                .tag("scene", "chat.general")
                .tag("status", "success")
                .counter()
                .count());
        assertEquals(1L, meterRegistry.find("ai.gateway.latency")
                .tag("provider", "provider-from-result")
                .tag("model", "model-from-result")
                .tag("scene", "chat.general")
                .timer()
                .count());
        assertEquals(20D, meterRegistry.find("ai.gateway.tokens")
                .tag("provider", "provider-from-result")
                .tag("model", "model-from-result")
                .tag("scene", "chat.general")
                .tag("type", "total")
                .counter()
                .count());
        assertEquals(0.0012D, meterRegistry.find("ai.gateway.cost")
                .tag("provider", "provider-from-result")
                .tag("model", "model-from-result")
                .tag("scene", "chat.general")
                .counter()
                .count(), 0.000001D);
    }

    @Test
    void shouldRecordFailureCounters() {
        recorder.recordFailure(buildRequest(), GatewayError.builder()
                .errorCode("PROVIDER_ERROR")
                .errorType("provider_error")
                .message("failed")
                .build());

        assertEquals(1D, meterRegistry.find("ai.gateway.requests")
                .tag("provider", "provider-from-request")
                .tag("model", "model-from-request")
                .tag("scene", "chat.general")
                .tag("status", "failed")
                .counter()
                .count());
        assertEquals(1D, meterRegistry.find("ai.gateway.errors")
                .tag("provider", "provider-from-request")
                .tag("model", "model-from-request")
                .tag("scene", "chat.general")
                .tag("error_type", "provider_error")
                .counter()
                .count());
    }

    private GatewayRequest buildRequest() {
        return GatewayRequest.builder()
                .sceneCode("chat.general")
                .providerCode("provider-from-request")
                .modelCode("model-from-request")
                .build();
    }
}
