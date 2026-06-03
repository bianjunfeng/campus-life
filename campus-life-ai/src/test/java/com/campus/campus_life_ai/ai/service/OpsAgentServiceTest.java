package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.OpsAnalyzeRequest;
import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;
import com.campus.campus_life_ai.ai.gateway.LlmGatewayFacade;
import com.campus.campus_life_ai.ai.mapper.AiCallLogMapper;
import com.campus.campus_life_ai.ai.provider.LlmProvider;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.provider.ProviderSelector;
import com.campus.campus_life_ai.ai.service.ops.OpsAlertClient;
import com.campus.campus_life_ai.ai.service.ops.OpsHealthClient;
import com.campus.campus_life_ai.ai.service.ops.OpsLogClient;
import com.campus.campus_life_ai.ai.service.ops.OpsMetricClient;
import com.campus.campus_life_ai.ai.service.tool.AgentToolRegistry;
import com.campus.campus_life_ai.common.properties.OpsAgentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class OpsAgentServiceTest {

    @Test
    void buildSnapshotReturnsPartialDataSourceFailureStatus() {
        OpsToolService toolService = new OpsToolService(
                new FixedHealthClient(status("actuator", "Actuator 健康检查", "UP", "ok")),
                new FixedAlertClient(status("prometheus", "Prometheus", "DOWN", "Prometheus 查询失败")),
                new FixedMetricClient(status("prometheus", "Prometheus", "DOWN", "Prometheus 查询失败")),
                new FixedLogClient(status("cls", "腾讯云 CLS", "UP", "ok"))
        );
        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        ProviderSelector providerSelector = mock(ProviderSelector.class);
        AiCallLogMapper aiCallLogMapper = mock(AiCallLogMapper.class);
        LlmProvider provider = mock(LlmProvider.class);
        ProviderRuntimeConfig runtimeConfig = ProviderRuntimeConfig.builder()
                .providerCode("openai-compatible")
                .providerName("Qwen")
                .enabled(true)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("key")
                .defaultModelCode("qwen-plus")
                .maxOutputTokens(1024)
                .build();
        given(runtimeConfigService.resolveProvider(any())).willReturn(runtimeConfig);
        given(providerSelector.resolve("openai-compatible")).willReturn(provider);
        given(provider.isAvailable(runtimeConfig)).willReturn(true);
        given(aiCallLogMapper.findRecent(anyInt())).willReturn(List.of());
        given(aiCallLogMapper.aggregateOverview()).willReturn(Map.of(
                "totalCalls", 0,
                "successCalls", 0,
                "failedCalls", 0,
                "totalTokens", 0,
                "avgLatencyMs", 0
        ));
        OpsAgentService service = new OpsAgentService(
                toolService,
                new OpsAgentProperties(),
                runtimeConfigService,
                providerSelector,
                mock(LlmGatewayFacade.class),
                aiCallLogMapper,
                new ObjectMapper(),
                mock(AgentToolRegistry.class)
        );

        OpsSnapshotDTO snapshot = service.buildSnapshot(new OpsAnalyzeRequest());

        assertThat(snapshot.getAlerts()).isEmpty();
        assertThat(snapshot.getDataSources())
                .anySatisfy(dataSource -> {
                    assertThat(dataSource.getCode()).isEqualTo("prometheus");
                    assertThat(dataSource.getStatus()).isEqualTo("DOWN");
                    assertThat(dataSource.getMessage()).contains("查询失败");
                });
    }

    private static OpsSnapshotDTO.DataSourceStatus status(String code, String name, String state, String message) {
        OpsSnapshotDTO.DataSourceStatus status = new OpsSnapshotDTO.DataSourceStatus();
        status.setCode(code);
        status.setName(name);
        status.setStatus(state);
        status.setMessage(message);
        return status;
    }

    private record FixedHealthClient(OpsSnapshotDTO.DataSourceStatus status) implements OpsHealthClient {
        @Override
        public List<OpsSnapshotDTO.ServiceStatus> probeServices() {
            return List.of();
        }
    }

    private record FixedAlertClient(OpsSnapshotDTO.DataSourceStatus status) implements OpsAlertClient {
        @Override
        public List<OpsSnapshotDTO.Alert> queryAlerts() {
            return List.of();
        }
    }

    private record FixedMetricClient(OpsSnapshotDTO.DataSourceStatus status) implements OpsMetricClient {
        @Override
        public List<OpsSnapshotDTO.MetricSample> queryJvmHeapUsage() {
            return List.of();
        }
    }

    private record FixedLogClient(OpsSnapshotDTO.DataSourceStatus status) implements OpsLogClient {
        @Override
        public List<OpsSnapshotDTO.LogTopic> listTopics() {
            return List.of();
        }

        @Override
        public List<OpsSnapshotDTO.LogEntry> queryLogs(String region, String topic, String query, int limit) {
            return List.of();
        }
    }
}
