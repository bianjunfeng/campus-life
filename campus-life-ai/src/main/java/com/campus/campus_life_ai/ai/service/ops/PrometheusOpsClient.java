package com.campus.campus_life_ai.ai.service.ops;

import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;
import com.campus.campus_life_ai.common.properties.OpsAgentProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class PrometheusOpsClient implements OpsAlertClient, OpsMetricClient {

    private final OpsAgentProperties properties;
    private final ObjectMapper objectMapper;
    private volatile OpsSnapshotDTO.DataSourceStatus status = OpsDataSourceStatuses.status(
            "prometheus", "Prometheus", "UNKNOWN", "尚未查询 Prometheus"
    );

    public PrometheusOpsClient(OpsAgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<OpsSnapshotDTO.Alert> queryAlerts() {
        String unavailable = validateConfig();
        if (unavailable != null) {
            status = OpsDataSourceStatuses.status("prometheus", "Prometheus", "UNCONFIGURED", unavailable);
            return List.of();
        }
        try {
            String response = restClient()
                    .get()
                    .uri(trimTrailingSlash(properties.getPrometheus().getBaseUrl()) + "/api/v1/alerts")
                    .headers(this::applyAuth)
                    .retrieve()
                    .body(String.class);
            List<OpsSnapshotDTO.Alert> alerts = parseAlerts(response);
            status = OpsDataSourceStatuses.status("prometheus", "Prometheus", "UP", "Prometheus 告警查询成功");
            return alerts;
        } catch (Exception e) {
            status = OpsDataSourceStatuses.status("prometheus", "Prometheus", "DOWN", "Prometheus 告警查询失败: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<OpsSnapshotDTO.MetricSample> queryJvmHeapUsage() {
        String unavailable = validateConfig();
        if (unavailable != null) {
            status = OpsDataSourceStatuses.status("prometheus", "Prometheus", "UNCONFIGURED", unavailable);
            return List.of();
        }
        try {
            URI url = UriComponentsBuilder
                    .fromHttpUrl(trimTrailingSlash(properties.getPrometheus().getBaseUrl()) + "/api/v1/query")
                    .queryParam("query", properties.getPrometheus().getJvmHeapUsageQuery())
                    .build()
                    .encode()
                    .toUri();
            String response = restClient()
                    .get()
                    .uri(url)
                    .headers(this::applyAuth)
                    .retrieve()
                    .body(String.class);
            List<OpsSnapshotDTO.MetricSample> samples = parseInstantVector(response, "jvm_heap_usage_percent", "%");
            status = OpsDataSourceStatuses.status("prometheus", "Prometheus", "UP", "Prometheus 指标查询成功");
            return samples;
        } catch (Exception e) {
            status = OpsDataSourceStatuses.status("prometheus", "Prometheus", "DOWN", "Prometheus 指标查询失败: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public OpsSnapshotDTO.DataSourceStatus status() {
        return status;
    }

    List<OpsSnapshotDTO.Alert> parseAlerts(String body) throws Exception {
        if (!StringUtils.hasText(body)) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(body);
        ensureSuccess(root);
        List<OpsSnapshotDTO.Alert> alerts = new ArrayList<>();
        for (JsonNode alertNode : root.path("data").path("alerts")) {
            JsonNode labels = alertNode.path("labels");
            JsonNode annotations = alertNode.path("annotations");
            OpsSnapshotDTO.Alert alert = new OpsSnapshotDTO.Alert();
            alert.setAlertName(labels.path("alertname").asText("UnknownAlert"));
            alert.setDescription(firstText(
                    annotations.path("description").asText(null),
                    annotations.path("summary").asText(null),
                    alertNode.path("value").asText(null)
            ));
            alert.setState(alertNode.path("state").asText("unknown"));
            alert.setActiveAt(alertNode.path("activeAt").asText(null));
            alert.setDuration(calculateDuration(alert.getActiveAt()));
            alert.setSeverity(labels.path("severity").asText("warning"));
            alert.setService(firstText(
                    labels.path("service").asText(null),
                    labels.path("application").asText(null),
                    labels.path("app").asText(null),
                    labels.path("job").asText(null),
                    labels.path("pod").asText(null)
            ));
            alert.setSource("prometheus");
            alerts.add(alert);
        }
        return alerts;
    }

    List<OpsSnapshotDTO.MetricSample> parseInstantVector(String body, String metricName, String unit) throws Exception {
        if (!StringUtils.hasText(body)) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(body);
        ensureSuccess(root);
        List<OpsSnapshotDTO.MetricSample> samples = new ArrayList<>();
        for (JsonNode result : root.path("data").path("result")) {
            JsonNode labels = result.path("metric");
            JsonNode value = result.path("value");
            if (!value.isArray() || value.size() < 2) {
                continue;
            }
            OpsSnapshotDTO.MetricSample sample = new OpsSnapshotDTO.MetricSample();
            sample.setName(metricName);
            sample.setService(firstText(
                    labels.path("service").asText(null),
                    labels.path("application").asText(null),
                    labels.path("app").asText(null),
                    labels.path("job").asText(null)
            ));
            sample.setInstance(labels.path("instance").asText(null));
            sample.setValue(parseDouble(value.get(1).asText(null)));
            sample.setUnit(unit);
            sample.setTimestamp(parseEpochSeconds(value.get(0).asText(null)));
            sample.setLabels(labelsToMap(labels));
            sample.setSource("prometheus");
            samples.add(sample);
        }
        return samples;
    }

    private void ensureSuccess(JsonNode root) {
        if (!"success".equals(root.path("status").asText())) {
            throw new IllegalStateException(root.path("error").asText("Prometheus API 返回非成功状态"));
        }
    }

    private RestClient restClient() {
        int timeoutMs = Math.max(1, properties.getPrometheus().getTimeoutSeconds()) * 1000;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private void applyAuth(HttpHeaders headers) {
        if (StringUtils.hasText(properties.getPrometheus().getBearerToken())) {
            headers.setBearerAuth(properties.getPrometheus().getBearerToken().trim());
        }
    }

    private String validateConfig() {
        if (!properties.getPrometheus().isEnabled()) {
            return "Prometheus 未启用";
        }
        if (!StringUtils.hasText(properties.getPrometheus().getBaseUrl())) {
            return "PROMETHEUS_BASE_URL 未配置";
        }
        return null;
    }

    private String calculateDuration(String activeAtStr) {
        try {
            Instant activeAt = Instant.parse(activeAtStr);
            Duration duration = Duration.between(activeAt, Instant.now());
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.getSeconds() % 60;
            if (hours > 0) {
                return String.format("%dh%dm%ds", hours, minutes, seconds);
            }
            if (minutes > 0) {
                return String.format("%dm%ds", minutes, seconds);
            }
            return String.format("%ds", seconds);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Map<String, Object> labelsToMap(JsonNode labels) {
        Map<String, Object> values = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = labels.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            values.put(field.getKey(), field.getValue().asText());
        }
        return values;
    }

    private Double parseDouble(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String parseEpochSeconds(String value) {
        Double seconds = parseDouble(value);
        if (seconds == null) {
            return null;
        }
        long millis = (long) (seconds * 1000);
        return Instant.ofEpochMilli(millis).toString();
    }

    private String firstText(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
