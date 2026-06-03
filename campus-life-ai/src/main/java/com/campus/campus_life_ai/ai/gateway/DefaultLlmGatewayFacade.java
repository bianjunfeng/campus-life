package com.campus.campus_life_ai.ai.gateway;

import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayStreamConsumer;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayUsage;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;
import com.campus.campus_life_ai.ai.gateway.observe.GatewayCallLogger;
import com.campus.campus_life_ai.ai.gateway.observe.GatewayCostCalculator;
import com.campus.campus_life_ai.ai.gateway.observe.GatewayMetricsRecorder;
import com.campus.campus_life_ai.ai.gateway.policy.GatewayPolicyChain;
import com.campus.campus_life_ai.ai.gateway.policy.GatewayPolicyException;
import com.campus.campus_life_ai.ai.gateway.policy.GatewaySafetyInspector;
import com.campus.campus_life_ai.ai.gateway.route.ModelRoutePlanner;
import com.campus.campus_life_ai.ai.gateway.route.ProviderHealthRegistry;
import com.campus.campus_life_ai.ai.gateway.route.RouteCandidate;
import com.campus.campus_life_ai.ai.gateway.route.RoutePlan;
import com.campus.campus_life_ai.ai.provider.ChatCompletionResult;
import com.campus.campus_life_ai.ai.provider.LlmProvider;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.provider.ProviderSelector;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
public class DefaultLlmGatewayFacade implements LlmGatewayFacade {

    private final ProviderSelector providerSelector;
    private final GatewayCallLogger gatewayCallLogger;
    private final GatewayMetricsRecorder gatewayMetricsRecorder;
    private final GatewayCostCalculator gatewayCostCalculator;
    private final GatewayErrorNormalizer gatewayErrorNormalizer;
    private final GatewayPolicyChain gatewayPolicyChain;
    private final GatewaySafetyInspector gatewaySafetyInspector;
    private final ModelRoutePlanner modelRoutePlanner;
    private final ProviderHealthRegistry providerHealthRegistry;

    public DefaultLlmGatewayFacade(ProviderSelector providerSelector,
                                   GatewayCallLogger gatewayCallLogger,
                                   GatewayMetricsRecorder gatewayMetricsRecorder,
                                   GatewayCostCalculator gatewayCostCalculator,
                                   GatewayErrorNormalizer gatewayErrorNormalizer,
                                   GatewayPolicyChain gatewayPolicyChain,
                                   GatewaySafetyInspector gatewaySafetyInspector,
                                   ModelRoutePlanner modelRoutePlanner,
                                   ProviderHealthRegistry providerHealthRegistry) {
        this.providerSelector = providerSelector;
        this.gatewayCallLogger = gatewayCallLogger;
        this.gatewayMetricsRecorder = gatewayMetricsRecorder;
        this.gatewayCostCalculator = gatewayCostCalculator;
        this.gatewayErrorNormalizer = gatewayErrorNormalizer;
        this.gatewayPolicyChain = gatewayPolicyChain;
        this.gatewaySafetyInspector = gatewaySafetyInspector;
        this.modelRoutePlanner = modelRoutePlanner;
        this.providerHealthRegistry = providerHealthRegistry;
    }

    @Override
    public GatewayResponse chat(GatewayRequest request) {
        return invoke(request, candidate -> {
            LlmProvider provider = resolveAvailableProvider(candidate);
            return provider.chat(candidate.getCommand(), candidate.getRuntimeConfig());
        });
    }

    @Override
    public GatewayResponse stream(GatewayRequest request, GatewayStreamConsumer consumer) {
        return invoke(request, candidate -> {
            LlmProvider provider = resolveAvailableProvider(candidate);
            return provider.stream(
                    candidate.getCommand(),
                    candidate.getRuntimeConfig(),
                    delta -> {
                        if (consumer != null) {
                            consumer.onDelta(delta);
                        }
                    }
            );
        });
    }

    private GatewayResponse invoke(GatewayRequest request, Function<RouteCandidate, ChatCompletionResult> invocation) {
        RuntimeException lastFailure = null;
        GatewayError lastError = null;
        try {
            gatewayPolicyChain.validate(request);
            RoutePlan routePlan = modelRoutePlanner.plan(request);
            for (RouteCandidate candidate : healthyCandidates(routePlan)) {
                try {
                    GatewayResponse response = toResponse(request, candidate, invocation.apply(candidate));
                    validateOutputSafety(request, response);
                    providerHealthRegistry.recordSuccess(candidate);
                    gatewayCallLogger.recordSuccess(request, response);
                    gatewayMetricsRecorder.recordSuccess(request, response);
                    return response;
                } catch (RuntimeException e) {
                    GatewayError error = gatewayErrorNormalizer.normalize(e);
                    if (gatewayErrorNormalizer.isClientAborted(error) || isNonRetryable(error)) {
                        throw e;
                    }
                    providerHealthRegistry.recordFailure(candidate);
                    lastFailure = e;
                    lastError = error;
                }
            }
        } catch (RuntimeException e) {
            lastFailure = e;
            lastError = gatewayErrorNormalizer.normalize(e);
        }

        GatewayError error = lastError == null
                ? gatewayErrorNormalizer.normalize(lastFailure)
                : lastError;
        gatewayCallLogger.recordFailure(request, error);
        gatewayMetricsRecorder.recordFailure(request, error);
        if (lastFailure != null) {
            if (gatewayErrorNormalizer.isClientAborted(error)) {
                throw lastFailure;
            }
            if (lastFailure instanceof GatewayCallException gatewayCallException) {
                throw gatewayCallException;
            }
        }
        throw new GatewayCallException(error, lastFailure);
    }

    private List<RouteCandidate> healthyCandidates(RoutePlan routePlan) {
        List<RouteCandidate> candidates = routePlan == null || routePlan.getCandidates() == null
                ? List.of()
                : routePlan.getCandidates();
        List<RouteCandidate> healthyCandidates = new ArrayList<>();
        for (RouteCandidate candidate : candidates) {
            if (providerHealthRegistry.isHealthy(candidate)) {
                healthyCandidates.add(candidate);
            }
        }
        if (!healthyCandidates.isEmpty()) {
            return healthyCandidates;
        }
        return candidates.isEmpty() ? List.of() : List.of(candidates.get(0));
    }

    private LlmProvider resolveAvailableProvider(RouteCandidate candidate) {
        ProviderRuntimeConfig runtimeConfig = candidate.getRuntimeConfig();
        LlmProvider provider = providerSelector.resolve(candidate.getProviderCode());
        if (!provider.isAvailable(runtimeConfig)) {
            throw new IllegalStateException("模型供应商未启用或缺少配置");
        }
        return provider;
    }

    private void validateOutputSafety(GatewayRequest request, GatewayResponse response) {
        GatewaySafetyInspector.SafetyDecision decision = gatewaySafetyInspector.inspectOutput(request, response);
        if (decision != null && decision.blocked()) {
            throw new GatewayPolicyException(GatewayErrorCode.SAFETY_BLOCKED, decision.message());
        }
    }

    private GatewayResponse toResponse(GatewayRequest request, RouteCandidate candidate, ChatCompletionResult result) {
        GatewayUsage usage = GatewayUsage.builder()
                .promptTokens(result.getPromptTokens())
                .completionTokens(result.getCompletionTokens())
                .totalTokens(result.getTotalTokens())
                .latencyMs(result.getLatencyMs())
                .costAmount(gatewayCostCalculator.calculate(
                        resolve(result.getProviderCode(), candidate.getProviderCode()),
                        resolve(result.getModelCode(), candidate.getModelCode()),
                        result.getPromptTokens(),
                        result.getCompletionTokens()
                ))
                .build();

        return GatewayResponse.builder()
                .requestId(request.getRequestId())
                .content(result.getContent())
                .providerCode(resolve(result.getProviderCode(), candidate.getProviderCode()))
                .modelCode(resolve(result.getModelCode(), candidate.getModelCode()))
                .usage(usage)
                .latencyMs(result.getLatencyMs())
                .fallbackLevel(candidate.getFallbackLevel())
                .finishReason(result.getFinishReason())
                .build();
    }

    private String resolve(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private boolean isNonRetryable(GatewayError error) {
        return error != null && GatewayErrorCode.SAFETY_BLOCKED.getCode().equals(error.getErrorCode());
    }
}
