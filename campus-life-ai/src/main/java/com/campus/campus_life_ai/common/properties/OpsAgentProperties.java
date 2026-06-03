package com.campus.campus_life_ai.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "ops")
public class OpsAgentProperties {

    private Prometheus prometheus = new Prometheus();
    private Logs logs = new Logs();
    private int analysisLogLimit = 12;
    private List<ServiceProbe> serviceProbes = new ArrayList<>();

    public OpsAgentProperties() {
        serviceProbes.add(probe("ai-service", "校园 AI 服务", "http://127.0.0.1:8083/actuator/health"));
        serviceProbes.add(probe("campus-backend", "校园业务后端", "http://127.0.0.1:8080/actuator/health"));
        serviceProbes.add(probe("api-gateway", "校园 API 网关", "http://127.0.0.1:8090/actuator/health"));
    }

    private static ServiceProbe probe(String code, String name, String healthUrl) {
        ServiceProbe probe = new ServiceProbe();
        probe.setCode(code);
        probe.setName(name);
        probe.setHealthUrl(healthUrl);
        return probe;
    }

    @Data
    public static class Prometheus {
        private String baseUrl = "http://localhost:9090";
        private boolean enabled = true;
        private String bearerToken;
        private int timeoutSeconds = 10;
        private String jvmHeapUsageQuery = "100 * sum by (application, instance) (jvm_memory_used_bytes{area=\"heap\"}) / sum by (application, instance) (jvm_memory_max_bytes{area=\"heap\"})";
    }

    @Data
    public static class Logs {
        private String provider = "cls";
        private String defaultRegion = "ap-guangzhou";
        private Cls cls = new Cls();
    }

    @Data
    public static class Cls {
        private boolean enabled = true;
        private String endpoint = "cls.tencentcloudapi.com";
        private String region = "ap-guangzhou";
        private String secretId;
        private String secretKey;
        private int timeoutSeconds = 10;
        private int lookbackMinutes = 30;
        private TopicIds topics = new TopicIds();
    }

    @Data
    public static class TopicIds {
        private String applicationLogs;
        private String databaseSlowQuery;
        private String systemEvents;
        private String systemMetrics;
    }

    @Data
    public static class ServiceProbe {
        private String code;
        private String name;
        private String healthUrl;
    }
}
