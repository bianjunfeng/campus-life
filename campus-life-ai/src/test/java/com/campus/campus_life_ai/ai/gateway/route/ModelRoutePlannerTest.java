package com.campus.campus_life_ai.ai.gateway.route;

import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.entity.AiGatewayRouteRule;
import com.campus.campus_life_ai.ai.entity.AiModelConfig;
import com.campus.campus_life_ai.ai.mapper.AiGatewayRouteRuleMapper;
import com.campus.campus_life_ai.ai.mapper.AiModelConfigMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.service.AiRuntimeConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ModelRoutePlannerTest {

    private final AiGatewayRouteRuleMapper routeRuleMapper = mock(AiGatewayRouteRuleMapper.class);
    private final AiModelConfigMapper modelConfigMapper = mock(AiModelConfigMapper.class);
    private final AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
    private final ModelRoutePlanner planner = new ModelRoutePlanner(
            routeRuleMapper,
            modelConfigMapper,
            runtimeConfigService,
            new ObjectMapper()
    );

    @Test
    void shouldPlanPrimaryAndFallbackCandidatesWithEffectiveModelCodes() {
        RoutePlan plan = planner.plan(GatewayRequest.builder()
                .requestId("req-1")
                .providerCode("provider-a")
                .modelCode("primary-model")
                .runtimeConfig(buildRuntime("provider-a", "primary-model"))
                .command(buildCommand("provider-a", "primary-model"))
                .fallbackCandidates(List.of(RouteCandidate.builder()
                        .providerCode("provider-a")
                        .modelCode("fallback-model")
                        .build()))
                .build());

        assertEquals(2, plan.getCandidates().size());
        assertEquals("primary-model", plan.getCandidates().get(0).getCommand().getModelCode());
        assertEquals("fallback-model", plan.getCandidates().get(1).getRuntimeConfig().getDefaultModelCode());
        assertEquals("fallback-model", plan.getCandidates().get(1).getCommand().getModelCode());
        assertEquals(1, plan.getCandidates().get(1).getFallbackLevel());
    }

    @Test
    void shouldUsePersistedRuleCandidatesWhenModelsAreEnabled() {
        AiGatewayRouteRule rule = new AiGatewayRouteRule();
        rule.setCapabilityCode("chat");
        rule.setSceneCode("chat.general");
        rule.setRouteRuleJson("""
                {
                  "replacePrimary": true,
                  "candidates": [
                    { "providerCode": "provider-b", "modelCode": "rule-model" }
                  ]
                }
                """);
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode("provider-b");
        model.setModelCode("rule-model");
        model.setMaxOutputTokens(2048);
        given(routeRuleMapper.findEnabledByCapabilityCode("chat")).willReturn(List.of(rule));
        given(modelConfigMapper.findEnabledByProviderAndModelCode("provider-b", "rule-model")).willReturn(model);
        given(runtimeConfigService.resolveProvider("provider-b")).willReturn(buildRuntime("provider-b", "provider-default"));

        RoutePlan plan = planner.plan(GatewayRequest.builder()
                .requestId("req-rule")
                .capabilityCode("chat")
                .sceneCode("chat.general")
                .providerCode("provider-a")
                .modelCode("primary-model")
                .runtimeConfig(buildRuntime("provider-a", "primary-model"))
                .command(buildCommand("provider-a", "primary-model"))
                .build());

        assertEquals("provider-b", plan.getCandidates().get(0).getProviderCode());
        assertEquals("rule-model", plan.getCandidates().get(0).getModelCode());
        assertEquals(2048, plan.getCandidates().get(0).getRuntimeConfig().getMaxOutputTokens());
        assertEquals("primary-model", plan.getCandidates().get(1).getModelCode());
    }

    private ProviderRuntimeConfig buildRuntime(String providerCode, String modelCode) {
        return ProviderRuntimeConfig.builder()
                .providerCode(providerCode)
                .providerName(providerCode)
                .enabled(true)
                .defaultModelCode(modelCode)
                .build();
    }

    private ChatCompletionCommand buildCommand(String providerCode, String modelCode) {
        return ChatCompletionCommand.builder()
                .providerCode(providerCode)
                .modelCode(modelCode)
                .messages(List.of(ChatCompletionCommand.PromptMessage.builder()
                        .role("user")
                        .content("hello")
                        .build()))
                .build();
    }
}
