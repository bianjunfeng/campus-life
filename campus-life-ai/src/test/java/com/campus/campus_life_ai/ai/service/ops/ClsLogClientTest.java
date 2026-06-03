package com.campus.campus_life_ai.ai.service.ops;

import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;
import com.campus.campus_life_ai.common.properties.OpsAgentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClsLogClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseSearchLogResponse() throws Exception {
        ClsLogClient client = new ClsLogClient(new OpsAgentProperties(), objectMapper);
        String body = """
                {
                  "Response": {
                    "Results": [
                      {
                        "Time": 1778403600,
                        "Source": "10.0.0.12",
                        "FileName": "order-service.log",
                        "LogJson": "{\\"level\\":\\"ERROR\\",\\"service\\":\\"order-service\\",\\"message\\":\\"database timeout\\",\\"traceId\\":\\"t-001\\"}"
                      }
                    ],
                    "RequestId": "req-001"
                  }
                }
                """;

        List<OpsSnapshotDTO.LogEntry> logs = client.parseSearchLogResponse(body, "ap-guangzhou", "application-logs", "topic-app");

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getLevel()).isEqualTo("ERROR");
        assertThat(logs.get(0).getService()).isEqualTo("order-service");
        assertThat(logs.get(0).getMessage()).isEqualTo("database timeout");
        assertThat(logs.get(0).getFields()).containsEntry("source", "cls");
        assertThat(logs.get(0).getFields()).containsEntry("topicId", "topic-app");
    }

    @Test
    void unconfiguredClsReturnsNoLogsWithoutMockData() {
        OpsAgentProperties properties = new OpsAgentProperties();
        properties.getLogs().getCls().getTopics().setApplicationLogs("topic-app");
        ClsLogClient client = new ClsLogClient(properties, objectMapper);

        List<OpsSnapshotDTO.LogEntry> logs = client.queryLogs("ap-guangzhou", "application-logs", "level:ERROR", 10);

        assertThat(logs).isEmpty();
        assertThat(client.status().getStatus()).isEqualTo("UNCONFIGURED");
        assertThat(client.status().getMessage()).contains("CLS_SECRET_ID");
    }
}
