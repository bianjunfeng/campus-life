package com.campus.campus_life_backend.common.event;

import com.campus.campus_life_backend.common.event.mapper.EventOutboxMapper;
import com.campus.campus_life_backend.modules.search.service.SearchOpsMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class EventOutboxPublisherTask {

    private static final Logger log = LoggerFactory.getLogger(EventOutboxPublisherTask.class);

    private final EventOutboxMapper eventOutboxMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SearchOpsMetricsService searchOpsMetricsService;
    private final boolean enabled;
    private final int batchSize;
    private final int baseRetryDelaySeconds;
    private final int maxRetryDelaySeconds;
    private final long sendTimeoutMs;
    private final long sendingTimeoutSeconds;

    public EventOutboxPublisherTask(
            EventOutboxMapper eventOutboxMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            SearchOpsMetricsService searchOpsMetricsService,
            @Value("${event.outbox.enabled:true}") boolean enabled,
            @Value("${event.outbox.batch-size:100}") int batchSize,
            @Value("${event.outbox.base-retry-delay-seconds:5}") int baseRetryDelaySeconds,
            @Value("${event.outbox.max-retry-delay-seconds:300}") int maxRetryDelaySeconds,
            @Value("${event.outbox.send-timeout-ms:5000}") long sendTimeoutMs,
            @Value("${event.outbox.sending-timeout-seconds:120}") long sendingTimeoutSeconds
    ) {
        this.eventOutboxMapper = eventOutboxMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.searchOpsMetricsService = searchOpsMetricsService;
        this.enabled = enabled;
        this.batchSize = Math.max(batchSize, 1);
        this.baseRetryDelaySeconds = Math.max(baseRetryDelaySeconds, 1);
        this.maxRetryDelaySeconds = Math.max(maxRetryDelaySeconds, this.baseRetryDelaySeconds);
        this.sendTimeoutMs = Math.max(sendTimeoutMs, 1000);
        this.sendingTimeoutSeconds = Math.max(sendingTimeoutSeconds, 30);
    }

    @Scheduled(
            initialDelayString = "${event.outbox.initial-delay-ms:10000}",
            fixedDelayString = "${event.outbox.fixed-delay-ms:3000}"
    )
    public void publishDueEvents() {
        if (!enabled) {
            return;
        }
        try {
            eventOutboxMapper.resetStaleSending(LocalDateTime.now().minusSeconds(sendingTimeoutSeconds));
            List<EventOutboxRecord> records = eventOutboxMapper.findDue(LocalDateTime.now(), batchSize);
            for (EventOutboxRecord record : records) {
                publishOne(record);
            }
        } catch (Exception e) {
            log.warn("Publish outbox events failed", e);
        }
    }

    private void publishOne(EventOutboxRecord record) {
        if (record == null || record.getId() == null) {
            return;
        }
        int claimed = eventOutboxMapper.markSending(record.getId());
        if (claimed <= 0) {
            return;
        }
        try {
            kafkaTemplate.send(record.getTopic(), record.getMessageKey(), record.getPayload())
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            eventOutboxMapper.markSent(record.getId());
            searchOpsMetricsService.recordProducerSuccess(record.getChannel());
            log.debug("Outbox event published eventId={}, topic={}, key={}",
                    record.getEventId(), record.getTopic(), record.getMessageKey());
        } catch (Exception e) {
            searchOpsMetricsService.recordProducerFailure(record.getChannel());
            int delaySeconds = nextRetryDelay(record.getRetryCount());
            eventOutboxMapper.markFailed(record.getId(), shortError(e), delaySeconds);
            log.warn("Outbox event publish failed eventId={}, topic={}, retryDelaySeconds={}",
                    record.getEventId(), record.getTopic(), delaySeconds, e);
        }
    }

    private int nextRetryDelay(Integer retryCount) {
        int retries = retryCount == null ? 0 : retryCount;
        int factor = 1 << Math.min(retries, 6);
        return Math.min(baseRetryDelaySeconds * factor, maxRetryDelaySeconds);
    }

    private String shortError(Throwable error) {
        if (error == null || error.getMessage() == null) {
            return "";
        }
        String message = error.getMessage();
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
