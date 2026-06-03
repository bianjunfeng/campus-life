package com.campus.campus_life_ai.ai.gateway.observe;

import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayUsage;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Component
public class GatewayMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public GatewayMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordSuccess(GatewayRequest request, GatewayResponse response) {
        meterRegistry.counter(
                "ai.gateway.requests",
                "provider", tag(response.getProviderCode(), request.getProviderCode()),
                "model", tag(response.getModelCode(), request.getModelCode()),
                "scene", tag(request.getSceneCode()),
                "status", "success"
        ).increment();
        if (response.getLatencyMs() != null && response.getLatencyMs() >= 0) {
            meterRegistry.timer(
                    "ai.gateway.latency",
                    "provider", tag(response.getProviderCode(), request.getProviderCode()),
                    "model", tag(response.getModelCode(), request.getModelCode()),
                    "scene", tag(request.getSceneCode())
            ).record(response.getLatencyMs(), TimeUnit.MILLISECONDS);
        }
        recordTokens(request, response);
        recordCost(request, response);
    }

    public void recordFailure(GatewayRequest request, GatewayError error) {
        meterRegistry.counter(
                "ai.gateway.requests",
                "provider", tag(request == null ? null : request.getProviderCode()),
                "model", tag(request == null ? null : request.getModelCode()),
                "scene", tag(request == null ? null : request.getSceneCode()),
                "status", "failed"
        ).increment();
        meterRegistry.counter(
                "ai.gateway.errors",
                "provider", tag(request == null ? null : request.getProviderCode()),
                "model", tag(request == null ? null : request.getModelCode()),
                "scene", tag(request == null ? null : request.getSceneCode()),
                "error_type", tag(error == null ? null : error.getErrorType())
        ).increment();
    }

    private void recordTokens(GatewayRequest request, GatewayResponse response) {
        GatewayUsage usage = response.getUsage();
        if (usage == null) {
            return;
        }
        incrementTokens(request, response, "prompt", usage.getPromptTokens());
        incrementTokens(request, response, "completion", usage.getCompletionTokens());
        incrementTokens(request, response, "total", usage.getTotalTokens());
    }

    private void incrementTokens(GatewayRequest request, GatewayResponse response, String type, Integer tokens) {
        if (tokens == null || tokens <= 0) {
            return;
        }
        meterRegistry.counter(
                "ai.gateway.tokens",
                "provider", tag(response.getProviderCode(), request.getProviderCode()),
                "model", tag(response.getModelCode(), request.getModelCode()),
                "scene", tag(request.getSceneCode()),
                "type", type
        ).increment(tokens);
    }

    private void recordCost(GatewayRequest request, GatewayResponse response) {
        GatewayUsage usage = response.getUsage();
        if (usage == null || usage.getCostAmount() == null || usage.getCostAmount().signum() <= 0) {
            return;
        }
        meterRegistry.counter(
                "ai.gateway.cost",
                "provider", tag(response.getProviderCode(), request.getProviderCode()),
                "model", tag(response.getModelCode(), request.getModelCode()),
                "scene", tag(request.getSceneCode())
        ).increment(usage.getCostAmount().doubleValue());
    }

    private String tag(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
    }

    private String tag(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : tag(fallback);
    }
}
