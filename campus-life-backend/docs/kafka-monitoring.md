# Kafka reliability monitoring

The backend exposes Kafka reliability metrics through the existing Spring Boot
Prometheus endpoint:

```text
GET /actuator/prometheus
```

Metrics added for event reliability:

```text
campus_event_outbox_records{channel,status}
campus_event_outbox_pending_oldest_age_seconds{channel}
campus_event_failure_records{channel,status}
campus_event_reliability_db_scrape_success
campus_kafka_consumer_lag_records{channel,group,topic,partition}
campus_kafka_consumer_lag_total_records{channel,group,topic}
campus_kafka_consumer_lag_scrape_success
```

Prometheus rule file:

```text
ops/prometheus/campus-life-kafka-alerts.yml
```

Grafana dashboard file:

```text
ops/grafana/dashboards/campus-life-kafka-reliability.json
```

Minimal Prometheus scrape config:

```yaml
scrape_configs:
  - job_name: campus-life-backend
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - localhost:8080

rule_files:
  - ops/prometheus/campus-life-kafka-alerts.yml
```

Runtime switches:

```yaml
event:
  monitoring:
    enabled: true
    initial-delay-ms: 15000
    fixed-delay-ms: 30000
    kafka-lag:
      enabled: true
      timeout-ms: 5000
```
