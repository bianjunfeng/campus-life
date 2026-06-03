package com.campus.campus_life_backend.common.event;

import com.campus.campus_life_backend.common.event.mapper.EventFailureRecordMapper;
import com.campus.campus_life_backend.common.event.mapper.EventOutboxMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(prefix = "event.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventReliabilityMetricsService {

    private static final Logger log = LoggerFactory.getLogger(EventReliabilityMetricsService.class);

    private static final String OUTBOX_RECORDS = "campus.event.outbox.records";
    private static final String OUTBOX_OLDEST_AGE_SECONDS = "campus.event.outbox.pending.oldest.age.seconds";
    private static final String FAILURE_RECORDS = "campus.event.failure.records";
    private static final String DB_SCRAPE_SUCCESS = "campus.event.reliability.db.scrape.success";
    private static final String CONSUMER_LAG = "campus.kafka.consumer.lag.records";
    private static final String CONSUMER_LAG_TOTAL = "campus.kafka.consumer.lag.total.records";
    private static final String CONSUMER_LAG_SCRAPE_SUCCESS = "campus.kafka.consumer.lag.scrape.success";

    private static final List<String> CHANNELS = List.of("post-search", "user-search", "notification", "voucher-order");
    private static final List<String> OUTBOX_STATUSES = List.of("NEW", "FAILED", "SENDING", "SENT");
    private static final List<String> FAILURE_STATUSES = List.of("NEW", "REPLAYED", "IGNORED");

    private final MeterRegistry meterRegistry;
    private final EventOutboxMapper eventOutboxMapper;
    private final EventFailureRecordMapper eventFailureRecordMapper;
    private final KafkaAdmin kafkaAdmin;
    private final boolean kafkaLagEnabled;
    private final long kafkaTimeoutMs;
    private final List<WatchedConsumer> watchedConsumers;
    private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();

    public EventReliabilityMetricsService(
            MeterRegistry meterRegistry,
            EventOutboxMapper eventOutboxMapper,
            EventFailureRecordMapper eventFailureRecordMapper,
            ObjectProvider<KafkaAdmin> kafkaAdminProvider,
            @Value("${event.monitoring.kafka-lag.enabled:true}") boolean kafkaLagEnabled,
            @Value("${event.monitoring.kafka-lag.timeout-ms:5000}") long kafkaTimeoutMs,
            @Value("${search.kafka.topic:forum-post-search-sync}") String postSearchTopic,
            @Value("${search.kafka.group-id:forum-search-indexer}") String postSearchGroup,
            @Value("${search.kafka.user-topic:forum-user-search-sync}") String userSearchTopic,
            @Value("${search.kafka.user-group-id:forum-user-search-indexer}") String userSearchGroup,
            @Value("${notification.kafka.topic:system-notification-events}") String notificationTopic,
            @Value("${notification.kafka.group-id:system-notification-consumer}") String notificationGroup,
            @Value("${voucher.order.kafka.topic:voucher-order-events}") String voucherOrderTopic,
            @Value("${voucher.order.kafka.group-id:voucher-order-consumer}") String voucherOrderGroup
    ) {
        this.meterRegistry = meterRegistry;
        this.eventOutboxMapper = eventOutboxMapper;
        this.eventFailureRecordMapper = eventFailureRecordMapper;
        this.kafkaAdmin = kafkaAdminProvider.getIfAvailable();
        this.kafkaLagEnabled = kafkaLagEnabled;
        this.kafkaTimeoutMs = Math.max(kafkaTimeoutMs, 1000);
        this.watchedConsumers = List.of(
                new WatchedConsumer("post-search", postSearchTopic, postSearchGroup),
                new WatchedConsumer("user-search", userSearchTopic, userSearchGroup),
                new WatchedConsumer("notification", notificationTopic, notificationGroup),
                new WatchedConsumer("voucher-order", voucherOrderTopic, voucherOrderGroup)
        );
        bindKnownGauges();
    }

    @Scheduled(
            initialDelayString = "${event.monitoring.initial-delay-ms:15000}",
            fixedDelayString = "${event.monitoring.fixed-delay-ms:30000}"
    )
    public void refresh() {
        refreshDatabaseMetrics();
        refreshConsumerLagMetrics();
    }

    private void bindKnownGauges() {
        for (String channel : CHANNELS) {
            gauge(OUTBOX_OLDEST_AGE_SECONDS, "Age of the oldest pending outbox event in seconds",
                    "channel", channel);
            for (String status : OUTBOX_STATUSES) {
                gauge(OUTBOX_RECORDS, "Outbox record count by channel and status",
                        "channel", channel, "status", status);
            }
            for (String status : FAILURE_STATUSES) {
                gauge(FAILURE_RECORDS, "Failed/DLT event record count by channel and status",
                        "channel", channel, "status", status);
            }
        }
        for (WatchedConsumer consumer : watchedConsumers) {
            gauge(CONSUMER_LAG_TOTAL, "Kafka consumer lag total by channel, group and topic",
                    "channel", consumer.channel(), "group", consumer.groupId(), "topic", consumer.topic());
        }
        gauge(DB_SCRAPE_SUCCESS, "Whether DB-backed event reliability metrics refreshed successfully");
        gauge(CONSUMER_LAG_SCRAPE_SUCCESS, "Whether Kafka consumer lag metrics refreshed successfully");
    }

    private void refreshDatabaseMetrics() {
        try {
            resetKnownDatabaseGauges();
            for (Map<String, Object> row : eventOutboxMapper.countByChannelAndStatus()) {
                String channel = stringValue(row, "channel");
                String status = stringValue(row, "status");
                if (channel == null || status == null) {
                    continue;
                }
                gauge(OUTBOX_RECORDS, "Outbox record count by channel and status",
                        "channel", channel, "status", status)
                        .set(longValue(row, "count"));
            }
            for (Map<String, Object> row : eventOutboxMapper.findOldestPendingCreatedAtByChannel()) {
                String channel = stringValue(row, "channel");
                LocalDateTime oldest = localDateTimeValue(row, "oldest_created_at");
                if (channel == null || oldest == null) {
                    continue;
                }
                long ageSeconds = Math.max(Duration.between(oldest, LocalDateTime.now()).toSeconds(), 0);
                gauge(OUTBOX_OLDEST_AGE_SECONDS, "Age of the oldest pending outbox event in seconds",
                        "channel", channel)
                        .set(ageSeconds);
            }
            for (Map<String, Object> row : eventFailureRecordMapper.countByChannelAndStatus()) {
                String channel = stringValue(row, "channel");
                String status = stringValue(row, "status");
                if (channel == null || status == null) {
                    continue;
                }
                gauge(FAILURE_RECORDS, "Failed/DLT event record count by channel and status",
                        "channel", channel, "status", status)
                        .set(longValue(row, "count"));
            }
            gauge(DB_SCRAPE_SUCCESS, "Whether DB-backed event reliability metrics refreshed successfully").set(1);
        } catch (Exception e) {
            gauge(DB_SCRAPE_SUCCESS, "Whether DB-backed event reliability metrics refreshed successfully").set(0);
            log.warn("Refresh event reliability DB metrics failed", e);
        }
    }

    private void refreshConsumerLagMetrics() {
        if (!kafkaLagEnabled || kafkaAdmin == null) {
            gauge(CONSUMER_LAG_SCRAPE_SUCCESS, "Whether Kafka consumer lag metrics refreshed successfully").set(0);
            return;
        }
        try (AdminClient adminClient = KafkaAdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            for (WatchedConsumer consumer : watchedConsumers) {
                refreshConsumerLagMetrics(adminClient, consumer);
            }
            gauge(CONSUMER_LAG_SCRAPE_SUCCESS, "Whether Kafka consumer lag metrics refreshed successfully").set(1);
        } catch (Exception e) {
            gauge(CONSUMER_LAG_SCRAPE_SUCCESS, "Whether Kafka consumer lag metrics refreshed successfully").set(0);
            log.warn("Refresh Kafka consumer lag metrics failed", e);
        }
    }

    private void refreshConsumerLagMetrics(AdminClient adminClient, WatchedConsumer consumer) throws Exception {
        Map<TopicPartition, OffsetAndMetadata> committed = adminClient
                .listConsumerGroupOffsets(consumer.groupId())
                .partitionsToOffsetAndMetadata()
                .get(kafkaTimeoutMs, TimeUnit.MILLISECONDS);
        Map<TopicPartition, OffsetAndMetadata> watchedOffsets = new LinkedHashMap<>();
        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : committed.entrySet()) {
            if (Objects.equals(consumer.topic(), entry.getKey().topic())) {
                watchedOffsets.put(entry.getKey(), entry.getValue());
            }
        }
        if (watchedOffsets.isEmpty()) {
            gauge(CONSUMER_LAG_TOTAL, "Kafka consumer lag total by channel, group and topic",
                    "channel", consumer.channel(), "group", consumer.groupId(), "topic", consumer.topic())
                    .set(0);
            return;
        }

        Map<TopicPartition, OffsetSpec> latestOffsetsRequest = new HashMap<>();
        for (TopicPartition topicPartition : watchedOffsets.keySet()) {
            latestOffsetsRequest.put(topicPartition, OffsetSpec.latest());
        }
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets = adminClient
                .listOffsets(latestOffsetsRequest)
                .all()
                .get(kafkaTimeoutMs, TimeUnit.MILLISECONDS);

        long totalLag = 0L;
        Set<Integer> seenPartitions = ConcurrentHashMap.newKeySet();
        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : watchedOffsets.entrySet()) {
            TopicPartition topicPartition = entry.getKey();
            long committedOffset = Math.max(entry.getValue().offset(), 0);
            long endOffset = latestOffsets.get(topicPartition) == null
                    ? committedOffset
                    : latestOffsets.get(topicPartition).offset();
            long lag = Math.max(endOffset - committedOffset, 0);
            totalLag += lag;
            seenPartitions.add(topicPartition.partition());
            gauge(CONSUMER_LAG, "Kafka consumer lag by channel, group, topic and partition",
                    "channel", consumer.channel(),
                    "group", consumer.groupId(),
                    "topic", consumer.topic(),
                    "partition", String.valueOf(topicPartition.partition()))
                    .set(lag);
        }
        resetStalePartitionLagGauges(consumer, seenPartitions);
        gauge(CONSUMER_LAG_TOTAL, "Kafka consumer lag total by channel, group and topic",
                "channel", consumer.channel(), "group", consumer.groupId(), "topic", consumer.topic())
                .set(totalLag);
    }

    private void resetKnownDatabaseGauges() {
        for (String channel : CHANNELS) {
            gauge(OUTBOX_OLDEST_AGE_SECONDS, "Age of the oldest pending outbox event in seconds",
                    "channel", channel)
                    .set(0);
            for (String status : OUTBOX_STATUSES) {
                gauge(OUTBOX_RECORDS, "Outbox record count by channel and status",
                        "channel", channel, "status", status)
                        .set(0);
            }
            for (String status : FAILURE_STATUSES) {
                gauge(FAILURE_RECORDS, "Failed/DLT event record count by channel and status",
                        "channel", channel, "status", status)
                        .set(0);
            }
        }
    }

    private void resetStalePartitionLagGauges(WatchedConsumer consumer, Set<Integer> seenPartitions) {
        List<String> staleKeys = new ArrayList<>();
        String prefix = CONSUMER_LAG
                + "|channel=" + consumer.channel()
                + "|group=" + consumer.groupId()
                + "|topic=" + consumer.topic()
                + "|partition=";
        for (String key : gauges.keySet()) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            String partition = key.substring(prefix.length());
            try {
                if (!seenPartitions.contains(Integer.parseInt(partition))) {
                    staleKeys.add(key);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        for (String key : staleKeys) {
            gauges.get(key).set(0);
        }
    }

    private AtomicLong gauge(String name, String description, String... tags) {
        String key = key(name, tags);
        return gauges.computeIfAbsent(key, ignored -> {
            AtomicLong value = new AtomicLong(0);
            Gauge.builder(name, value, AtomicLong::get)
                    .description(description)
                    .tags(tags)
                    .register(meterRegistry);
            return value;
        });
    }

    private String key(String name, String... tags) {
        StringBuilder builder = new StringBuilder(name);
        for (int i = 0; i + 1 < tags.length; i += 2) {
            builder.append('|').append(tags[i]).append('=').append(tags[i + 1]);
        }
        return builder.toString();
    }

    private String stringValue(Map<String, Object> row, String key) {
        Object value = rowValue(row, key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private long longValue(Map<String, Object> row, String key) {
        Object value = rowValue(row, key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? 0L : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private LocalDateTime localDateTimeValue(Map<String, Object> row, String key) {
        Object value = rowValue(row, key);
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }

    private Object rowValue(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return null;
        }
        Object direct = row.get(key);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private record WatchedConsumer(String channel, String topic, String groupId) {
    }
}
