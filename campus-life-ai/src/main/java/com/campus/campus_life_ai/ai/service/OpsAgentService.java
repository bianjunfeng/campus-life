package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.OpsAnalysisResponse;
import com.campus.campus_life_ai.ai.dto.OpsAnalyzeRequest;
import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;
import com.campus.campus_life_ai.ai.gateway.LlmGatewayFacade;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayUsage;
import com.campus.campus_life_ai.ai.mapper.AiCallLogMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.ChatCompletionResult;
import com.campus.campus_life_ai.ai.provider.LlmProvider;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.provider.ProviderSelector;
import com.campus.campus_life_ai.ai.service.tool.AgentToolRegistry;
import com.campus.campus_life_ai.common.properties.OpsAgentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class OpsAgentService {

    private static final String OPS_CAPABILITY = "ops";
    private static final String OPS_SCENE = "ops.diagnosis";

    private final OpsToolService opsToolService;
    private final OpsAgentProperties opsAgentProperties;
    private final AiRuntimeConfigService aiRuntimeConfigService;
    private final ProviderSelector providerSelector;
    private final LlmGatewayFacade llmGatewayFacade;
    private final AiCallLogMapper aiCallLogMapper;
    private final ObjectMapper objectMapper;
    private final AgentToolRegistry agentToolRegistry;

    public OpsAgentService(OpsToolService opsToolService,
                           OpsAgentProperties opsAgentProperties,
                           AiRuntimeConfigService aiRuntimeConfigService,
                           ProviderSelector providerSelector,
                           LlmGatewayFacade llmGatewayFacade,
                           AiCallLogMapper aiCallLogMapper,
                           ObjectMapper objectMapper,
                           AgentToolRegistry agentToolRegistry) {
        this.opsToolService = opsToolService;
        this.opsAgentProperties = opsAgentProperties;
        this.aiRuntimeConfigService = aiRuntimeConfigService;
        this.providerSelector = providerSelector;
        this.llmGatewayFacade = llmGatewayFacade;
        this.aiCallLogMapper = aiCallLogMapper;
        this.objectMapper = objectMapper;
        this.agentToolRegistry = agentToolRegistry;
    }

    public OpsSnapshotDTO buildSnapshot(OpsAnalyzeRequest request) {
        OpsAnalyzeRequest safeRequest = request == null ? new OpsAnalyzeRequest() : request;
        OpsSnapshotDTO snapshot = new OpsSnapshotDTO();
        snapshot.setGeneratedAt(LocalDateTime.now());
        snapshot.setRuntime(opsToolService.runtimeStatus());
        snapshot.setServices(opsToolService.probeServices());
        snapshot.setLogTopics(opsToolService.getAvailableLogTopics());

        if (!Boolean.FALSE.equals(safeRequest.getIncludeAlerts())) {
            snapshot.setAlerts(opsToolService.queryPrometheusAlerts());
            snapshot.setMetrics(opsToolService.queryPrometheusMetrics());
        }
        if (!Boolean.FALSE.equals(safeRequest.getIncludeLogs())) {
            snapshot.setRecentLogs(opsToolService.queryLogs(
                    null,
                    "application-logs",
                    "level:ERROR OR response_time:>3000 OR downstream",
                    resolveLimit(safeRequest.getLogLimit())
            ));
        }
        OpsSnapshotDTO.DataSourceStatus aiCallLogStatus = dataSourceStatus("ai-call-log", "AI 调用日志", "UP", "AI 调用日志查询成功");
        if (!Boolean.FALSE.equals(safeRequest.getIncludeAiLogs())) {
            try {
                snapshot.setRecentAiCalls(aiCallLogMapper.findRecent(resolveLimit(safeRequest.getLogLimit())));
            } catch (Exception e) {
                aiCallLogStatus = dataSourceStatus("ai-call-log", "AI 调用日志", "DOWN", "AI 调用日志查询失败: " + e.getMessage());
                snapshot.setRecentAiCalls(List.of());
            }
        }

        try {
            Map<String, Object> overview = aiCallLogMapper.aggregateOverview();
            snapshot.setAiOverview(overview == null ? emptyAiOverview() : overview);
        } catch (Exception e) {
            aiCallLogStatus = dataSourceStatus("ai-call-log", "AI 调用日志", "DOWN", "AI 调用日志汇总失败: " + e.getMessage());
            snapshot.setAiOverview(emptyAiOverview());
        }
        snapshot.setProvider(buildProviderStatus());
        List<OpsSnapshotDTO.DataSourceStatus> dataSources = new ArrayList<>(opsToolService.dataSourceStatuses());
        dataSources.add(aiCallLogStatus);
        snapshot.setDataSources(dataSources);
        return snapshot;
    }

    public OpsAnalysisResponse analyze(Long userId, OpsAnalyzeRequest request) {
        OpsAnalyzeRequest safeRequest = request == null ? new OpsAnalyzeRequest() : request;
        OpsSnapshotDTO snapshot = buildSnapshot(safeRequest);
        ProviderRuntimeConfig runtimeConfig = aiRuntimeConfigService.resolveProvider(null);

        String requestId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        try {
            ChatCompletionResult result = toChatCompletionResult(llmGatewayFacade.chat(buildGatewayRequest(
                    userId,
                    requestId,
                    runtimeConfig,
                    buildCommand(userId, safeRequest, snapshot, runtimeConfig)
            )));
            return OpsAnalysisResponse.builder()
                    .report(result.getContent())
                    .snapshot(snapshot)
                    .providerCode(result.getProviderCode())
                    .modelCode(result.getModelCode())
                    .latencyMs(result.getLatencyMs())
                    .generatedAt(LocalDateTime.now())
                    .build();
        } finally {
            log.info("运维 Agent 分析完成, latencyMs={}", System.currentTimeMillis() - start);
        }
    }

    public SseEmitter analyzeStream(Long userId, OpsAnalyzeRequest request) {
        SseEmitter emitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> doAnalyzeStream(userId, request, emitter));
        return emitter;
    }

    public List<OpsSnapshotDTO.Alert> queryAlerts() {
        return opsToolService.queryPrometheusAlerts();
    }

    public List<OpsSnapshotDTO.LogTopic> getLogTopics() {
        return opsToolService.getAvailableLogTopics();
    }

    public List<OpsSnapshotDTO.LogEntry> queryLogs(String region, String topic, String query, Integer limit) {
        return opsToolService.queryLogs(region, topic, query, resolveLimit(limit));
    }

    private void doAnalyzeStream(Long userId, OpsAnalyzeRequest request, SseEmitter emitter) {
        OpsAnalyzeRequest safeRequest = request == null ? new OpsAnalyzeRequest() : request;
        String requestId = UUID.randomUUID().toString();
        try {
            OpsSnapshotDTO snapshot = buildSnapshot(safeRequest);
            ProviderRuntimeConfig runtimeConfig = aiRuntimeConfigService.resolveProvider(null);

            sendEvent(emitter, "meta", Map.of(
                    "snapshot", snapshot,
                    "providerCode", runtimeConfig.getProviderCode(),
                    "modelCode", runtimeConfig.getDefaultModelCode()
            ));

            ChatCompletionResult result = toChatCompletionResult(llmGatewayFacade.stream(
                    buildGatewayRequest(
                            userId,
                            requestId,
                            runtimeConfig,
                            buildCommand(userId, safeRequest, snapshot, runtimeConfig)
                    ),
                    delta -> sendEvent(emitter, "delta", Map.of("content", delta))
            ));
            sendEvent(emitter, "done", OpsAnalysisResponse.builder()
                    .report(result.getContent())
                    .snapshot(snapshot)
                    .providerCode(result.getProviderCode())
                    .modelCode(result.getModelCode())
                    .latencyMs(result.getLatencyMs())
                    .generatedAt(LocalDateTime.now())
                    .build());
            emitter.complete();
        } catch (ClientStreamClosedException e) {
            log.info("运维 Agent 流式响应客户端已中断");
            emitter.complete();
        } catch (Exception e) {
            log.error("运维 Agent 流式分析失败", e);
            sendEvent(emitter, "error", Map.of("message", e.getMessage()));
            emitter.complete();
        }
    }

    private GatewayRequest buildGatewayRequest(Long userId,
                                               String requestId,
                                               ProviderRuntimeConfig runtimeConfig,
                                               ChatCompletionCommand command) {
        return GatewayRequest.builder()
                .requestId(requestId)
                .userId(userId)
                .capabilityCode(OPS_CAPABILITY)
                .sceneCode(OPS_SCENE)
                .providerCode(runtimeConfig.getProviderCode())
                .modelCode(runtimeConfig.getDefaultModelCode())
                .runtimeConfig(runtimeConfig)
                .command(command)
                .build();
    }

    private ChatCompletionResult toChatCompletionResult(GatewayResponse response) {
        GatewayUsage usage = response.getUsage();
        return ChatCompletionResult.builder()
                .content(response.getContent())
                .providerCode(response.getProviderCode())
                .modelCode(response.getModelCode())
                .promptTokens(usage == null ? null : usage.getPromptTokens())
                .completionTokens(usage == null ? null : usage.getCompletionTokens())
                .totalTokens(usage == null ? null : usage.getTotalTokens())
                .latencyMs(response.getLatencyMs())
                .finishReason(response.getFinishReason())
                .build();
    }

    private ChatCompletionCommand buildCommand(Long userId,
                                               OpsAnalyzeRequest request,
                                               OpsSnapshotDTO snapshot,
                                               ProviderRuntimeConfig runtimeConfig) {
        String question = StringUtils.hasText(request.getQuestion())
                ? request.getQuestion().trim()
                : "请基于当前服务健康、告警、日志和 AI 调用情况，输出一份校园生活平台运维巡检报告。";
        String snapshotJson = writeJson(snapshot);
        String userPrompt = """
                运维任务：
                %s

                工具快照 JSON：
                ```json
                %s
                ```

                输出要求：
                1. 必须基于工具快照，不要编造未出现的指标、日志、告警或服务。
                2. 先给出当前风险等级和影响范围。
                3. 分析服务健康、Prometheus 告警、应用日志、AI 调用失败率。
                4. 给出可执行的排查步骤，区分“立即处理”“继续观察”“长期优化”。
                5. 所有结论必须基于工具快照中的真实数据源；如果 dataSources 中某项为 DOWN、DEGRADED 或 UNCONFIGURED，必须标注“数据缺失/查询失败”，禁止补充演示指标。
                """.formatted(question, snapshotJson);

        return ChatCompletionCommand.builder()
                .providerCode(runtimeConfig.getProviderCode())
                .modelCode(runtimeConfig.getDefaultModelCode())
                .systemPrompt("""
                        你是校园生活平台的运维 Agent，负责 SRE 诊断、告警归因和处置建议。
                        你可以使用已采集的服务健康、Prometheus 告警、日志主题、日志样本、AI 调用日志和运行时指标。
                        数据源不可用时要保守说明缺口，不要编造未采集到的指标或日志。
                        输出必须是中文 Markdown，结构为：运行结论、关键证据、根因分析、处置步骤、验证方式、后续改进。
                        诊断要保守、可验证，禁止把推测写成事实。
                        """)
                .temperature(0.2D)
                .maxOutputTokens(runtimeConfig.getMaxOutputTokens())
                .messages(List.of(ChatCompletionCommand.PromptMessage.builder()
                        .role("user")
                        .content(userPrompt)
                        .build()))
                .toolCallbacks(agentToolRegistry.resolveTools(OPS_CAPABILITY, OPS_SCENE))
                .toolContext(agentToolRegistry.buildToolContext(userId, null, OPS_CAPABILITY, OPS_SCENE))
                .build();
    }

    private OpsSnapshotDTO.ProviderStatus buildProviderStatus() {
        ProviderRuntimeConfig runtimeConfig = aiRuntimeConfigService.resolveProvider(null);
        LlmProvider provider = providerSelector.resolve(runtimeConfig.getProviderCode());
        OpsSnapshotDTO.ProviderStatus status = new OpsSnapshotDTO.ProviderStatus();
        status.setProviderCode(runtimeConfig.getProviderCode());
        status.setProviderName(runtimeConfig.getProviderName());
        status.setBaseUrl(runtimeConfig.getBaseUrl());
        status.setDefaultModelCode(runtimeConfig.getDefaultModelCode());
        status.setEnabled(Boolean.TRUE.equals(runtimeConfig.getEnabled()));
        status.setApiKeyConfigured(StringUtils.hasText(runtimeConfig.getApiKey()));
        status.setAvailable(provider.isAvailable(runtimeConfig));
        return status;
    }

    private Map<String, Object> emptyAiOverview() {
        return Map.of(
                "totalCalls", 0,
                "successCalls", 0,
                "failedCalls", 0,
                "totalTokens", 0,
                "avgLatencyMs", 0
        );
    }

    private OpsSnapshotDTO.DataSourceStatus dataSourceStatus(String code, String name, String status, String message) {
        OpsSnapshotDTO.DataSourceStatus dataSource = new OpsSnapshotDTO.DataSourceStatus();
        dataSource.setCode(code);
        dataSource.setName(name);
        dataSource.setStatus(status);
        dataSource.setMessage(message);
        return dataSource;
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return opsAgentProperties.getAnalysisLogLimit();
        }
        return Math.min(limit, 100);
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            throw new ClientStreamClosedException(e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static class ClientStreamClosedException extends RuntimeException {
        ClientStreamClosedException(Throwable cause) {
            super(cause);
        }
    }
}
