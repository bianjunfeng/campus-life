package com.campus.campus_life_ai.ai.service.tool;

import com.campus.campus_life_ai.ai.service.OpsToolService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OpsAgentTools {

    private final OpsToolService opsToolService;
    private final ObjectMapper objectMapper;

    public OpsAgentTools(OpsToolService opsToolService, ObjectMapper objectMapper) {
        this.opsToolService = opsToolService;
        this.objectMapper = objectMapper;
    }

    public String runtimeStatus() {
        return writeJson(opsToolService.runtimeStatus());
    }

    public String probeServices() {
        return writeJson(opsToolService.probeServices());
    }

    public String queryPrometheusAlerts() {
        return writeJson(opsToolService.queryPrometheusAlerts());
    }

    public String queryPrometheusMetrics() {
        return writeJson(opsToolService.queryPrometheusMetrics());
    }

    public String listLogTopics() {
        return writeJson(opsToolService.getAvailableLogTopics());
    }

    public String queryLogs(QueryLogsRequest request) {
        QueryLogsRequest safeRequest = request == null ? new QueryLogsRequest(null, null, null, null) : request;
        return writeJson(opsToolService.queryLogs(
                safeRequest.region(),
                safeRequest.topic(),
                safeRequest.query(),
                safeRequest.limit()
        ));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize tool result", e);
        }
    }

    public record QueryLogsRequest(String region, String topic, String query, Integer limit) {
    }
}
