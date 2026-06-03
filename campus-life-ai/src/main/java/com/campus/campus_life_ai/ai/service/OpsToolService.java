package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;
import com.campus.campus_life_ai.ai.service.ops.OpsAlertClient;
import com.campus.campus_life_ai.ai.service.ops.OpsHealthClient;
import com.campus.campus_life_ai.ai.service.ops.OpsLogClient;
import com.campus.campus_life_ai.ai.service.ops.OpsMetricClient;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class OpsToolService {

    private final OpsHealthClient healthClient;
    private final OpsAlertClient alertClient;
    private final OpsMetricClient metricClient;
    private final OpsLogClient logClient;

    public OpsToolService(OpsHealthClient healthClient,
                          OpsAlertClient alertClient,
                          OpsMetricClient metricClient,
                          OpsLogClient logClient) {
        this.healthClient = healthClient;
        this.alertClient = alertClient;
        this.metricClient = metricClient;
        this.logClient = logClient;
    }

    public List<OpsSnapshotDTO.ServiceStatus> probeServices() {
        return healthClient.probeServices();
    }

    public OpsSnapshotDTO.RuntimeStatus runtimeStatus() {
        Runtime runtime = Runtime.getRuntime();
        File current = new File(".");
        OpsSnapshotDTO.RuntimeStatus status = new OpsSnapshotDTO.RuntimeStatus();
        status.setJavaVersion(System.getProperty("java.version"));
        status.setProcessors(runtime.availableProcessors());
        status.setHeapUsedBytes(runtime.totalMemory() - runtime.freeMemory());
        status.setHeapMaxBytes(runtime.maxMemory());
        status.setDiskTotalBytes(current.getTotalSpace());
        status.setDiskUsableBytes(current.getUsableSpace());
        return status;
    }

    public List<OpsSnapshotDTO.Alert> queryPrometheusAlerts() {
        return alertClient.queryAlerts();
    }

    public List<OpsSnapshotDTO.MetricSample> queryPrometheusMetrics() {
        return metricClient.queryJvmHeapUsage();
    }

    public List<OpsSnapshotDTO.LogTopic> getAvailableLogTopics() {
        return logClient.listTopics();
    }

    public List<OpsSnapshotDTO.LogEntry> queryLogs(String region, String logTopic, String query, Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 20 : Math.min(limit, 100);
        return logClient.queryLogs(region, logTopic, query, safeLimit);
    }

    public List<OpsSnapshotDTO.DataSourceStatus> dataSourceStatuses() {
        return List.of(
                healthClient.status(),
                alertClient.status(),
                logClient.status()
        );
    }
}
