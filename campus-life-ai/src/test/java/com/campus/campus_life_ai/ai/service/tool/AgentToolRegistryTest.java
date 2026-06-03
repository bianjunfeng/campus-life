package com.campus.campus_life_ai.ai.service.tool;

import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;
import com.campus.campus_life_ai.ai.service.OpsToolService;
import com.campus.campus_life_ai.ai.service.ops.OpsAlertClient;
import com.campus.campus_life_ai.ai.service.ops.OpsHealthClient;
import com.campus.campus_life_ai.ai.service.ops.OpsLogClient;
import com.campus.campus_life_ai.ai.service.ops.OpsMetricClient;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AgentToolRegistryTest {

    @Test
    void shouldReturnOpsToolsOnlyForOpsScene() {
        AgentToolRegistry registry = new AgentToolRegistry(new AiProviderProperties(), buildOpsAgentTools());

        List<ToolCallback> chatTools = registry.resolveTools("chat", "chat.general");
        List<ToolCallback> opsTools = registry.resolveTools("ops", "ops.diagnosis");

        assertThat(chatTools).isEmpty();
        assertThat(opsTools).hasSize(6);
        Set<String> names = opsTools.stream()
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toSet());
        assertThat(names).contains(
                "ops_runtime_status",
                "ops_probe_services",
                "ops_query_prometheus_alerts",
                "ops_query_prometheus_metrics",
                "ops_list_log_topics",
                "ops_query_logs"
        );
    }

    @Test
    void shouldDisableToolsByConfiguration() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.getTools().setEnabled(false);
        AgentToolRegistry registry = new AgentToolRegistry(properties, buildOpsAgentTools());

        assertThat(registry.resolveTools("ops", "ops.diagnosis")).isEmpty();
    }

    private OpsAgentTools buildOpsAgentTools() {
        OpsToolService toolService = new OpsToolService(
                new EmptyHealthClient(),
                new EmptyAlertClient(),
                new EmptyMetricClient(),
                new EmptyLogClient()
        );
        return new OpsAgentTools(toolService, new ObjectMapper());
    }

    private static OpsSnapshotDTO.DataSourceStatus statusOf(String code) {
        OpsSnapshotDTO.DataSourceStatus status = new OpsSnapshotDTO.DataSourceStatus();
        status.setCode(code);
        status.setName(code);
        status.setStatus("UP");
        status.setMessage("ok");
        return status;
    }

    private static class EmptyHealthClient implements OpsHealthClient {
        @Override
        public List<OpsSnapshotDTO.ServiceStatus> probeServices() {
            return List.of();
        }

        @Override
        public OpsSnapshotDTO.DataSourceStatus status() {
            return statusOf("actuator");
        }
    }

    private static class EmptyAlertClient implements OpsAlertClient {
        @Override
        public List<OpsSnapshotDTO.Alert> queryAlerts() {
            return List.of();
        }

        @Override
        public OpsSnapshotDTO.DataSourceStatus status() {
            return statusOf("prometheus-alerts");
        }
    }

    private static class EmptyMetricClient implements OpsMetricClient {
        @Override
        public List<OpsSnapshotDTO.MetricSample> queryJvmHeapUsage() {
            return List.of();
        }

        @Override
        public OpsSnapshotDTO.DataSourceStatus status() {
            return statusOf("prometheus-metrics");
        }
    }

    private static class EmptyLogClient implements OpsLogClient {
        @Override
        public List<OpsSnapshotDTO.LogTopic> listTopics() {
            return List.of();
        }

        @Override
        public List<OpsSnapshotDTO.LogEntry> queryLogs(String region, String topic, String query, int limit) {
            return List.of();
        }

        @Override
        public OpsSnapshotDTO.DataSourceStatus status() {
            return statusOf("logs");
        }
    }
}
