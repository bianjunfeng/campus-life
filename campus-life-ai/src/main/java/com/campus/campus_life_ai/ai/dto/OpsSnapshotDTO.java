package com.campus.campus_life_ai.ai.dto;

import com.campus.campus_life_ai.ai.entity.AiCallLog;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class OpsSnapshotDTO {
    private LocalDateTime generatedAt;
    private RuntimeStatus runtime;
    private ProviderStatus provider;
    private Map<String, Object> aiOverview;
    private List<DataSourceStatus> dataSources = new ArrayList<>();
    private List<ServiceStatus> services = new ArrayList<>();
    private List<Alert> alerts = new ArrayList<>();
    private List<MetricSample> metrics = new ArrayList<>();
    private List<LogTopic> logTopics = new ArrayList<>();
    private List<LogEntry> recentLogs = new ArrayList<>();
    private List<AiCallLog> recentAiCalls = new ArrayList<>();

    @Data
    public static class RuntimeStatus {
        private String javaVersion;
        private int processors;
        private long heapUsedBytes;
        private long heapMaxBytes;
        private long diskTotalBytes;
        private long diskUsableBytes;
    }

    @Data
    public static class ProviderStatus {
        private String providerCode;
        private String providerName;
        private String baseUrl;
        private String defaultModelCode;
        private boolean enabled;
        private boolean available;
        private boolean apiKeyConfigured;
    }

    @Data
    public static class ServiceStatus {
        private String code;
        private String name;
        private String healthUrl;
        private String status;
        private Integer httpStatus;
        private Integer latencyMs;
        private String message;
    }

    @Data
    public static class Alert {
        private String alertName;
        private String description;
        private String state;
        private String activeAt;
        private String duration;
        private String severity;
        private String service;
        private String source;
    }

    @Data
    public static class MetricSample {
        private String name;
        private String service;
        private String instance;
        private Double value;
        private String unit;
        private String timestamp;
        private Map<String, Object> labels;
        private String source;
    }

    @Data
    public static class LogTopic {
        private String topicName;
        private String description;
        private List<String> exampleQueries = new ArrayList<>();
        private List<String> relatedAlerts = new ArrayList<>();
    }

    @Data
    public static class LogEntry {
        private String timestamp;
        private String level;
        private String service;
        private String topic;
        private String message;
        private Map<String, Object> fields;
    }

    @Data
    public static class DataSourceStatus {
        private String code;
        private String name;
        private String status;
        private String message;
    }
}
