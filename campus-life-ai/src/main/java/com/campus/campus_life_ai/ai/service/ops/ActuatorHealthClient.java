package com.campus.campus_life_ai.ai.service.ops;

import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;
import com.campus.campus_life_ai.common.properties.OpsAgentProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class ActuatorHealthClient implements OpsHealthClient {

    private final OpsAgentProperties properties;
    private volatile OpsSnapshotDTO.DataSourceStatus status = OpsDataSourceStatuses.status(
            "actuator", "Actuator 健康检查", "UNKNOWN", "尚未执行健康检查"
    );

    public ActuatorHealthClient(OpsAgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<OpsSnapshotDTO.ServiceStatus> probeServices() {
        List<OpsAgentProperties.ServiceProbe> probes = properties.getServiceProbes();
        if (probes == null || probes.isEmpty()) {
            status = OpsDataSourceStatuses.status("actuator", "Actuator 健康检查", "UNCONFIGURED", "未配置服务健康检查地址");
            return List.of();
        }
        List<OpsSnapshotDTO.ServiceStatus> services = probes.stream()
                .map(this::probeService)
                .toList();
        long upCount = services.stream().filter(service -> "UP".equals(service.getStatus())).count();
        if (upCount == services.size()) {
            status = OpsDataSourceStatuses.status("actuator", "Actuator 健康检查", "UP", "服务健康检查全部可用");
        } else if (upCount > 0) {
            status = OpsDataSourceStatuses.status("actuator", "Actuator 健康检查", "DEGRADED", "部分服务健康检查失败");
        } else {
            status = OpsDataSourceStatuses.status("actuator", "Actuator 健康检查", "DOWN", "所有服务健康检查失败");
        }
        return services;
    }

    @Override
    public OpsSnapshotDTO.DataSourceStatus status() {
        return status;
    }

    private OpsSnapshotDTO.ServiceStatus probeService(OpsAgentProperties.ServiceProbe probe) {
        OpsSnapshotDTO.ServiceStatus service = new OpsSnapshotDTO.ServiceStatus();
        service.setCode(probe.getCode());
        service.setName(probe.getName());
        service.setHealthUrl(probe.getHealthUrl());
        long start = System.currentTimeMillis();
        try {
            String body = buildRestClient(3000)
                    .get()
                    .uri(probe.getHealthUrl())
                    .retrieve()
                    .body(String.class);
            service.setLatencyMs((int) (System.currentTimeMillis() - start));
            service.setHttpStatus(200);
            service.setStatus(isUpBody(body) ? "UP" : "UNKNOWN");
            service.setMessage(abbreviate(body, 240));
        } catch (Exception e) {
            service.setLatencyMs((int) (System.currentTimeMillis() - start));
            service.setStatus("DOWN");
            service.setMessage(e.getMessage());
        }
        return service;
    }

    private boolean isUpBody(String body) {
        if (!StringUtils.hasText(body)) {
            return true;
        }
        return body.contains("\"status\":\"UP\"") || body.contains("\"status\": \"UP\"") || body.contains("UP");
    }

    private RestClient buildRestClient(int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private String abbreviate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
