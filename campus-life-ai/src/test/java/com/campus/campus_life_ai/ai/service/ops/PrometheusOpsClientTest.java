package com.campus.campus_life_ai.ai.service.ops;

import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;
import com.campus.campus_life_ai.common.properties.OpsAgentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrometheusOpsClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseAlertsFromPrometheusResponse() throws Exception {
        PrometheusOpsClient client = new PrometheusOpsClient(new OpsAgentProperties(), objectMapper);

        String body = """
                {
                  "status": "success",
                  "data": {
                    "alerts": [
                      {
                        "labels": {
                          "alertname": "HighJvmHeapUsage",
                          "severity": "warning",
                          "application": "order-service",
                          "instance": "10.0.0.8:8080"
                        },
                        "annotations": {
                          "description": "order-service heap usage is above 85%"
                        },
                        "state": "firing",
                        "activeAt": "2026-05-10T09:00:00Z"
                      }
                    ]
                  }
                }
                """;

        List<OpsSnapshotDTO.Alert> alerts = client.parseAlerts(body);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getAlertName()).isEqualTo("HighJvmHeapUsage");
        assertThat(alerts.get(0).getService()).isEqualTo("order-service");
        assertThat(alerts.get(0).getSeverity()).isEqualTo("warning");
        assertThat(alerts.get(0).getSource()).isEqualTo("prometheus");
    }

    @Test
    void unconfiguredPrometheusReturnsNoAlertsWithoutMockData() {
        OpsAgentProperties properties = new OpsAgentProperties();
        properties.getPrometheus().setBaseUrl("");
        PrometheusOpsClient client = new PrometheusOpsClient(properties, objectMapper);

        List<OpsSnapshotDTO.Alert> alerts = client.queryAlerts();

        assertThat(alerts).isEmpty();
        assertThat(client.status().getStatus()).isEqualTo("UNCONFIGURED");
        assertThat(client.status().getMessage()).contains("PROMETHEUS_BASE_URL");
    }
}
