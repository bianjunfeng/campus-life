package com.campus.campus_life_ai.ai.service.tool;

import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class AgentToolRegistry {

    private static final String OPS_CAPABILITY = "ops";
    private static final String OPS_SCENE_PREFIX = "ops.";

    private final AiProviderProperties properties;
    private final List<ToolCallback> opsToolCallbacks;

    public AgentToolRegistry(AiProviderProperties properties, OpsAgentTools opsAgentTools) {
        this.properties = properties;
        this.opsToolCallbacks = buildOpsToolCallbacks(opsAgentTools);
    }

    public List<ToolCallback> resolveTools(String capabilityCode, String sceneCode) {
        if (!properties.getTools().isEnabled()) {
            return List.of();
        }
        if (isOpsScene(capabilityCode, sceneCode) && properties.getTools().isOpsEnabled()) {
            return opsToolCallbacks;
        }
        return List.of();
    }

    public Map<String, Object> buildToolContext(Long userId,
                                                String sessionId,
                                                String capabilityCode,
                                                String sceneCode) {
        return Map.of(
                "userId", userId == null ? "" : userId,
                "sessionId", StringUtils.hasText(sessionId) ? sessionId : "",
                "capabilityCode", StringUtils.hasText(capabilityCode) ? capabilityCode : "",
                "sceneCode", StringUtils.hasText(sceneCode) ? sceneCode : ""
        );
    }

    private boolean isOpsScene(String capabilityCode, String sceneCode) {
        return OPS_CAPABILITY.equalsIgnoreCase(capabilityCode)
                || (StringUtils.hasText(sceneCode) && sceneCode.toLowerCase().startsWith(OPS_SCENE_PREFIX));
    }

    private List<ToolCallback> buildOpsToolCallbacks(OpsAgentTools opsAgentTools) {
        return List.of(
                FunctionToolCallback.builder("ops_runtime_status", opsAgentTools::runtimeStatus)
                        .description("获取当前 AI 服务进程的 Java、CPU、堆内存和磁盘运行时状态。")
                        .build(),
                FunctionToolCallback.builder("ops_probe_services", opsAgentTools::probeServices)
                        .description("探测校园平台关键服务的健康状态、HTTP 状态码、延迟和错误信息。")
                        .build(),
                FunctionToolCallback.builder("ops_query_prometheus_alerts", opsAgentTools::queryPrometheusAlerts)
                        .description("查询 Prometheus 当前告警，返回告警名称、状态、严重级别、服务和描述。")
                        .build(),
                FunctionToolCallback.builder("ops_query_prometheus_metrics", opsAgentTools::queryPrometheusMetrics)
                        .description("查询 Prometheus JVM 堆内存使用率等核心运行指标。")
                        .build(),
                FunctionToolCallback.builder("ops_list_log_topics", opsAgentTools::listLogTopics)
                        .description("列出可查询的日志主题、示例查询语句和关联告警。")
                        .build(),
                FunctionToolCallback.builder("ops_query_logs", opsAgentTools::queryLogs)
                        .description("查询指定日志主题的近期日志。输入 region、topic、query、limit；limit 最大会被业务层限制为 100。")
                        .inputType(OpsAgentTools.QueryLogsRequest.class)
                        .build()
        );
    }
}
