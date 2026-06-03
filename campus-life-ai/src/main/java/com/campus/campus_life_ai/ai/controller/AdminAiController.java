package com.campus.campus_life_ai.ai.controller;

import com.campus.campus_life_ai.ai.dto.AiCapabilityConfigDTO;
import com.campus.campus_life_ai.ai.dto.AiCapabilityUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiAgentEntryDTO;
import com.campus.campus_life_ai.ai.dto.AiGatewayProbeRequest;
import com.campus.campus_life_ai.ai.dto.AiGatewayProbeResponse;
import com.campus.campus_life_ai.ai.dto.AiGatewayRouteRuleDTO;
import com.campus.campus_life_ai.ai.dto.AiGatewayRouteRuleUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiGatewaySafetyRuleDTO;
import com.campus.campus_life_ai.ai.dto.AiGatewaySafetyRuleUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiModelConfigDTO;
import com.campus.campus_life_ai.ai.dto.AiModelUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiProviderConfigDTO;
import com.campus.campus_life_ai.ai.dto.AiProviderHealthCheckResponse;
import com.campus.campus_life_ai.ai.dto.AiProviderUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiSceneConfigDTO;
import com.campus.campus_life_ai.ai.dto.AiSceneUpsertRequest;
import com.campus.campus_life_ai.ai.dto.AiUsageQuotaDTO;
import com.campus.campus_life_ai.ai.dto.AiUsageQuotaUpsertRequest;
import com.campus.campus_life_ai.ai.mapper.AiCallLogMapper;
import com.campus.campus_life_ai.ai.service.AdminAiConfigService;
import com.campus.campus_life_ai.ai.service.AdminAiGatewayProbeService;
import com.campus.campus_life_ai.common.result.ApiResponse;
import com.campus.campus_life_ai.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/ai")
public class AdminAiController {

    private final AdminAiConfigService adminAiConfigService;
    private final AdminAiGatewayProbeService adminAiGatewayProbeService;
    private final AiCallLogMapper aiCallLogMapper;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminAiController(AdminAiConfigService adminAiConfigService,
                             AdminAiGatewayProbeService adminAiGatewayProbeService,
                             AiCallLogMapper aiCallLogMapper,
                             CurrentUserAccessor currentUserAccessor) {
        this.adminAiConfigService = adminAiConfigService;
        this.adminAiGatewayProbeService = adminAiGatewayProbeService;
        this.aiCallLogMapper = aiCallLogMapper;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/agents")
    public ApiResponse<List<AiAgentEntryDTO>> getAgents() {
        return ApiResponse.success(List.of(
                new AiAgentEntryDTO(
                        "dialog",
                        "管理端对话 Agent",
                        "面向管理员的平台问答、配置说明和审核辅助对话入口。",
                        "/admin/ai/agent/chat",
                        "/api/agent/conversations",
                        "chat.general"
                ),
                new AiAgentEntryDTO(
                        "knowledge",
                        "管理端知识库 Agent",
                        "管理员账号私有知识库，支持上传文档并在对话中检索引用。",
                        "/admin/ai/agent/knowledge-bases",
                        "/api/agent/knowledge-bases",
                        "chat.personal_qa"
                ),
                new AiAgentEntryDTO(
                        "ops",
                        "运维 Agent",
                        "读取服务健康、告警、日志和 AI 调用数据，生成运维诊断报告。",
                        "/admin/ai?tab=ops",
                        "/api/admin/ai/ops",
                        "ops.diagnosis"
                )
        ));
    }

    @GetMapping("/providers")
    public ApiResponse<List<AiProviderConfigDTO>> getProviders() {
        return ApiResponse.success(adminAiConfigService.listProviders());
    }

    @GetMapping("/providers/{id}")
    public ApiResponse<AiProviderConfigDTO> getProvider(@PathVariable Long id) {
        return ApiResponse.success(adminAiConfigService.getProvider(id));
    }

    @PostMapping("/providers")
    public ApiResponse<AiProviderConfigDTO> createProvider(@Valid @RequestBody AiProviderUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.createProvider(request));
    }

    @PutMapping("/providers/{id}")
    public ApiResponse<AiProviderConfigDTO> updateProvider(@PathVariable Long id,
                                                           @Valid @RequestBody AiProviderUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.updateProvider(id, request));
    }

    @DeleteMapping("/providers/{id}")
    public ApiResponse<Void> deleteProvider(@PathVariable Long id) {
        adminAiConfigService.deleteProvider(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/providers/{id}/health-check")
    public ApiResponse<AiProviderHealthCheckResponse> checkProviderHealth(@PathVariable Long id) {
        return ApiResponse.success(adminAiGatewayProbeService.checkProvider(id));
    }

    @PostMapping("/gateway/probe")
    public ApiResponse<AiGatewayProbeResponse> probeGateway(@Valid @RequestBody AiGatewayProbeRequest request) {
        return ApiResponse.success(adminAiGatewayProbeService.probe(currentUserAccessor.requireUserId(), request));
    }

    @GetMapping("/models")
    public ApiResponse<List<AiModelConfigDTO>> getModels() {
        return ApiResponse.success(adminAiConfigService.listModels());
    }

    @GetMapping("/models/{id}")
    public ApiResponse<AiModelConfigDTO> getModel(@PathVariable Long id) {
        return ApiResponse.success(adminAiConfigService.getModel(id));
    }

    @PostMapping("/models")
    public ApiResponse<AiModelConfigDTO> createModel(@Valid @RequestBody AiModelUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.createModel(request));
    }

    @PutMapping("/models/{id}")
    public ApiResponse<AiModelConfigDTO> updateModel(@PathVariable Long id,
                                                     @Valid @RequestBody AiModelUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.updateModel(id, request));
    }

    @DeleteMapping("/models/{id}")
    public ApiResponse<Void> deleteModel(@PathVariable Long id) {
        adminAiConfigService.deleteModel(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/route-rules")
    public ApiResponse<List<AiGatewayRouteRuleDTO>> getRouteRules() {
        return ApiResponse.success(adminAiConfigService.listRouteRules());
    }

    @GetMapping("/route-rules/{id}")
    public ApiResponse<AiGatewayRouteRuleDTO> getRouteRule(@PathVariable Long id) {
        return ApiResponse.success(adminAiConfigService.getRouteRule(id));
    }

    @PostMapping("/route-rules")
    public ApiResponse<AiGatewayRouteRuleDTO> createRouteRule(@Valid @RequestBody AiGatewayRouteRuleUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.createRouteRule(request));
    }

    @PutMapping("/route-rules/{id}")
    public ApiResponse<AiGatewayRouteRuleDTO> updateRouteRule(@PathVariable Long id,
                                                              @Valid @RequestBody AiGatewayRouteRuleUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.updateRouteRule(id, request));
    }

    @DeleteMapping("/route-rules/{id}")
    public ApiResponse<Void> deleteRouteRule(@PathVariable Long id) {
        adminAiConfigService.deleteRouteRule(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/safety-rules")
    public ApiResponse<List<AiGatewaySafetyRuleDTO>> getSafetyRules() {
        return ApiResponse.success(adminAiConfigService.listSafetyRules());
    }

    @GetMapping("/safety-rules/{id}")
    public ApiResponse<AiGatewaySafetyRuleDTO> getSafetyRule(@PathVariable Long id) {
        return ApiResponse.success(adminAiConfigService.getSafetyRule(id));
    }

    @PostMapping("/safety-rules")
    public ApiResponse<AiGatewaySafetyRuleDTO> createSafetyRule(@Valid @RequestBody AiGatewaySafetyRuleUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.createSafetyRule(request));
    }

    @PutMapping("/safety-rules/{id}")
    public ApiResponse<AiGatewaySafetyRuleDTO> updateSafetyRule(@PathVariable Long id,
                                                                @Valid @RequestBody AiGatewaySafetyRuleUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.updateSafetyRule(id, request));
    }

    @DeleteMapping("/safety-rules/{id}")
    public ApiResponse<Void> deleteSafetyRule(@PathVariable Long id) {
        adminAiConfigService.deleteSafetyRule(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/quotas")
    public ApiResponse<List<AiUsageQuotaDTO>> getQuotas() {
        return ApiResponse.success(adminAiConfigService.listQuotas());
    }

    @GetMapping("/quotas/{id}")
    public ApiResponse<AiUsageQuotaDTO> getQuota(@PathVariable Long id) {
        return ApiResponse.success(adminAiConfigService.getQuota(id));
    }

    @PostMapping("/quotas")
    public ApiResponse<AiUsageQuotaDTO> createQuota(@Valid @RequestBody AiUsageQuotaUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.createQuota(request));
    }

    @PutMapping("/quotas/{id}")
    public ApiResponse<AiUsageQuotaDTO> updateQuota(@PathVariable Long id,
                                                    @Valid @RequestBody AiUsageQuotaUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.updateQuota(id, request));
    }

    @DeleteMapping("/quotas/{id}")
    public ApiResponse<Void> deleteQuota(@PathVariable Long id) {
        adminAiConfigService.deleteQuota(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/capabilities")
    public ApiResponse<List<AiCapabilityConfigDTO>> getCapabilities() {
        return ApiResponse.success(adminAiConfigService.listCapabilities());
    }

    @GetMapping("/capabilities/{id}")
    public ApiResponse<AiCapabilityConfigDTO> getCapability(@PathVariable Long id) {
        return ApiResponse.success(adminAiConfigService.getCapability(id));
    }

    @PostMapping("/capabilities")
    public ApiResponse<AiCapabilityConfigDTO> createCapability(@Valid @RequestBody AiCapabilityUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.createCapability(request));
    }

    @PutMapping("/capabilities/{id}")
    public ApiResponse<AiCapabilityConfigDTO> updateCapability(@PathVariable Long id,
                                                               @Valid @RequestBody AiCapabilityUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.updateCapability(id, request));
    }

    @DeleteMapping("/capabilities/{id}")
    public ApiResponse<Void> deleteCapability(@PathVariable Long id) {
        adminAiConfigService.deleteCapability(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/scenes")
    public ApiResponse<List<AiSceneConfigDTO>> getScenes() {
        return ApiResponse.success(adminAiConfigService.listScenes());
    }

    @GetMapping("/scenes/{id}")
    public ApiResponse<AiSceneConfigDTO> getScene(@PathVariable Long id) {
        return ApiResponse.success(adminAiConfigService.getScene(id));
    }

    @PostMapping("/scenes")
    public ApiResponse<AiSceneConfigDTO> createScene(@Valid @RequestBody AiSceneUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.createScene(request));
    }

    @PutMapping("/scenes/{id}")
    public ApiResponse<AiSceneConfigDTO> updateScene(@PathVariable Long id,
                                                     @Valid @RequestBody AiSceneUpsertRequest request) {
        return ApiResponse.success(adminAiConfigService.updateScene(id, request));
    }

    @DeleteMapping("/scenes/{id}")
    public ApiResponse<Void> deleteScene(@PathVariable Long id) {
        adminAiConfigService.deleteScene(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/stats/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        Map<String, Object> overview = aiCallLogMapper.aggregateOverview();
        return ApiResponse.success(overview == null ? Map.of(
                "totalCalls", 0,
                "successCalls", 0,
                "failedCalls", 0,
                "totalTokens", 0,
                "avgLatencyMs", 0,
                "totalCostAmount", 0
        ) : overview);
    }

    @GetMapping("/stats/usage-trend")
    public ApiResponse<?> getUsageTrend(@RequestParam(defaultValue = "14") Integer days) {
        int safeDays = days == null || days < 1 ? 14 : Math.min(days, 90);
        return ApiResponse.success(aiCallLogMapper.findUsageTrend(LocalDateTime.now().minusDays(safeDays - 1L)));
    }

    @GetMapping("/stats/model-ranking")
    public ApiResponse<?> getModelRanking(@RequestParam(defaultValue = "10") Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        return ApiResponse.success(aiCallLogMapper.findModelRanking(safeLimit));
    }

    @GetMapping("/logs")
    public ApiResponse<?> getLogs(@RequestParam(defaultValue = "20") Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 20 : Math.min(limit, 100);
        return ApiResponse.success(aiCallLogMapper.findRecent(safeLimit));
    }
}
