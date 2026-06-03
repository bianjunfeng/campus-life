package com.campus.campus_life_ai.ai.service.ops;

import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;
import com.campus.campus_life_ai.common.properties.OpsAgentProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ClsLogClient implements OpsLogClient {

    private static final String SERVICE = "cls";
    private static final String ACTION_SEARCH_LOG = "SearchLog";
    private static final String API_VERSION = "2020-10-16";
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));
    private static final DateTimeFormatter SIGN_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

    private final OpsAgentProperties properties;
    private final ObjectMapper objectMapper;
    private volatile OpsSnapshotDTO.DataSourceStatus status = OpsDataSourceStatuses.status(
            "cls", "腾讯云 CLS", "UNKNOWN", "尚未查询 CLS"
    );

    public ClsLogClient(OpsAgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<OpsSnapshotDTO.LogTopic> listTopics() {
        List<OpsSnapshotDTO.LogTopic> topics = new ArrayList<>();
        topics.add(topic(
                "system-metrics",
                "系统指标日志，包含 CPU、内存、磁盘使用率等资源监控数据",
                List.of("cpu_usage:>80", "memory_usage:>85", "disk_usage:>90"),
                List.of("HighCPUUsage", "HighMemoryUsage", "HighDiskUsage")
        ));
        topics.add(topic(
                "application-logs",
                "应用日志，包含错误、慢请求、下游依赖调用异常等",
                List.of("level:ERROR", "http_status:500", "response_time:>3000"),
                List.of("ServiceUnavailable", "SlowResponse")
        ));
        topics.add(topic(
                "database-slow-query",
                "数据库慢查询日志，包含慢 SQL、锁等待和扫描行数异常",
                List.of("query_time:>2", "table:orders", "query_type:SELECT"),
                List.of("SlowResponse", "ServiceUnavailable")
        ));
        topics.add(topic(
                "system-events",
                "系统事件日志，包含 Pod 重启、OOM、容器崩溃等事件",
                List.of("restart OR crash", "oom_kill", "reason:OOMKilled"),
                List.of("ServiceUnavailable", "HighMemoryUsage")
        ));
        return topics;
    }

    @Override
    public List<OpsSnapshotDTO.LogEntry> queryLogs(String region, String topic, String query, int limit) {
        String logicalTopic = StringUtils.hasText(topic) ? topic.trim() : "application-logs";
        String topicId = resolveTopicId(logicalTopic);
        String unavailable = validateConfig(logicalTopic, topicId);
        if (unavailable != null) {
            status = OpsDataSourceStatuses.status("cls", "腾讯云 CLS", "UNCONFIGURED", unavailable);
            return List.of();
        }

        String resolvedRegion = resolveRegion(region);
        try {
            Instant now = Instant.now();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("TopicId", topicId);
            payload.put("From", now.minusSeconds(Math.max(1, properties.getLogs().getCls().getLookbackMinutes()) * 60L).getEpochSecond());
            payload.put("To", now.getEpochSecond());
            payload.put("Query", StringUtils.hasText(query) ? query.trim() : "*");
            payload.put("Limit", Math.max(1, Math.min(limit, 100)));
            payload.put("UseNewAnalysis", true);

            String body = objectMapper.writeValueAsString(payload);
            String response = restClient()
                    .post()
                    .uri("https://" + properties.getLogs().getCls().getEndpoint())
                    .headers(headers -> applyTc3Headers(headers, body, resolvedRegion))
                    .body(body)
                    .retrieve()
                    .body(String.class);

            List<OpsSnapshotDTO.LogEntry> logs = parseSearchLogResponse(response, resolvedRegion, logicalTopic, topicId);
            status = OpsDataSourceStatuses.status("cls", "腾讯云 CLS", "UP",
                    logs.isEmpty() ? "CLS 查询成功，未匹配日志" : "CLS 查询成功");
            return logs;
        } catch (Exception e) {
            status = OpsDataSourceStatuses.status("cls", "腾讯云 CLS", "DOWN", "CLS 查询失败: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public OpsSnapshotDTO.DataSourceStatus status() {
        return status;
    }

    List<OpsSnapshotDTO.LogEntry> parseSearchLogResponse(String body, String region, String logicalTopic, String topicId) throws Exception {
        if (!StringUtils.hasText(body)) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode response = root.path("Response");
        JsonNode error = response.path("Error");
        if (!error.isMissingNode()) {
            throw new IllegalStateException(error.path("Code").asText("ClsApiError") + ": " + error.path("Message").asText("CLS API 返回错误"));
        }

        List<OpsSnapshotDTO.LogEntry> logs = new ArrayList<>();
        for (JsonNode result : response.path("Results")) {
            JsonNode logJson = parseLogJson(result.path("LogJson"));
            Map<String, Object> fields = logJson.isObject()
                    ? objectMapper.convertValue(logJson, new TypeReference<Map<String, Object>>() {})
                    : new LinkedHashMap<>();
            fields.put("source", "cls");
            fields.put("region", region);
            fields.put("topicId", topicId);
            fields.put("fileName", result.path("FileName").asText(""));
            fields.put("sourceIp", result.path("Source").asText(""));

            OpsSnapshotDTO.LogEntry entry = new OpsSnapshotDTO.LogEntry();
            entry.setTimestamp(resolveTimestamp(result, logJson));
            entry.setLevel(resolveText(logJson, "level", "severity", "logLevel").toUpperCase(Locale.ROOT));
            if (!StringUtils.hasText(entry.getLevel())) {
                entry.setLevel("INFO");
            }
            entry.setService(firstText(
                    resolveText(logJson, "service", "application", "app", "spring.application.name"),
                    resolveText(logJson, "container_name", "pod_name"),
                    result.path("Source").asText(null)
            ));
            entry.setTopic(logicalTopic);
            entry.setMessage(firstText(
                    resolveText(logJson, "message", "msg", "log", "content"),
                    result.path("LogJson").asText("")
            ));
            entry.setFields(fields);
            logs.add(entry);
        }
        return logs;
    }

    private JsonNode parseLogJson(JsonNode logJsonNode) throws Exception {
        if (logJsonNode.isObject()) {
            return logJsonNode;
        }
        String logJson = logJsonNode.asText("");
        if (!StringUtils.hasText(logJson)) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(logJson);
    }

    private void applyTc3Headers(HttpHeaders headers, String payload, String region) {
        long timestamp = Instant.now().getEpochSecond();
        String date = SIGN_DATE_FORMATTER.format(Instant.ofEpochSecond(timestamp));
        String host = properties.getLogs().getCls().getEndpoint();
        String canonicalHeaders = "content-type:application/json; charset=utf-8\nhost:" + host + "\n";
        String signedHeaders = "content-type;host";
        String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + sha256Hex(payload);
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
        byte[] secretDate = hmacSha256(("TC3" + properties.getLogs().getCls().getSecretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = hex(hmacSha256(secretSigning, stringToSign));

        headers.set(HttpHeaders.HOST, host);
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
        headers.set("X-TC-Action", ACTION_SEARCH_LOG);
        headers.set("X-TC-Version", API_VERSION);
        headers.set("X-TC-Timestamp", String.valueOf(timestamp));
        headers.set("X-TC-Region", region);
        headers.set(HttpHeaders.AUTHORIZATION, "TC3-HMAC-SHA256 Credential="
                + properties.getLogs().getCls().getSecretId() + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature);
    }

    private RestClient restClient() {
        int timeoutMs = Math.max(1, properties.getLogs().getCls().getTimeoutSeconds()) * 1000;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private String validateConfig(String logicalTopic, String topicId) {
        if (!"cls".equalsIgnoreCase(properties.getLogs().getProvider())) {
            return "日志数据源 provider 不是 cls";
        }
        OpsAgentProperties.Cls cls = properties.getLogs().getCls();
        if (!cls.isEnabled()) {
            return "CLS 未启用";
        }
        if (!StringUtils.hasText(cls.getEndpoint())) {
            return "CLS_ENDPOINT 未配置";
        }
        if (!StringUtils.hasText(cls.getSecretId())) {
            return "CLS_SECRET_ID 未配置";
        }
        if (!StringUtils.hasText(cls.getSecretKey())) {
            return "CLS_SECRET_KEY 未配置";
        }
        if (!StringUtils.hasText(topicId)) {
            return "CLS topic 未配置: " + logicalTopic;
        }
        return null;
    }

    private String resolveRegion(String region) {
        return firstText(region, properties.getLogs().getCls().getRegion(), properties.getLogs().getDefaultRegion(), "ap-guangzhou");
    }

    private String resolveTopicId(String topic) {
        OpsAgentProperties.TopicIds topics = properties.getLogs().getCls().getTopics();
        return switch (topic) {
            case "system-metrics" -> topics.getSystemMetrics();
            case "database-slow-query" -> topics.getDatabaseSlowQuery();
            case "system-events" -> topics.getSystemEvents();
            default -> topics.getApplicationLogs();
        };
    }

    private OpsSnapshotDTO.LogTopic topic(String name, String description, List<String> examples, List<String> alerts) {
        OpsSnapshotDTO.LogTopic topic = new OpsSnapshotDTO.LogTopic();
        topic.setTopicName(name);
        topic.setDescription(description);
        topic.setExampleQueries(examples);
        topic.setRelatedAlerts(alerts);
        return topic;
    }

    private String resolveTimestamp(JsonNode result, JsonNode logJson) {
        String explicit = resolveText(logJson, "timestamp", "time", "@timestamp");
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }
        long seconds = result.path("Time").asLong(0L);
        if (seconds > 0) {
            return DISPLAY_FORMATTER.format(Instant.ofEpochSecond(seconds));
        }
        return DISPLAY_FORMATTER.format(Instant.now());
    }

    private String resolveText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText(null);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String firstText(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return hex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }

    private byte[] hmacSha256(byte[] key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", e);
        }
    }

    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            String hex = Integer.toHexString(value & 0xff);
            if (hex.length() == 1) {
                result.append('0');
            }
            result.append(hex);
        }
        return result.toString();
    }
}
