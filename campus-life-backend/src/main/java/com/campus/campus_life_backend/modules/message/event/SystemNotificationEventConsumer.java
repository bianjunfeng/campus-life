package com.campus.campus_life_backend.modules.message.event;

import com.campus.campus_life_backend.common.event.EventConsumeLogService;
import com.campus.campus_life_backend.common.event.EventEnvelopeSupport;
import com.campus.campus_life_backend.common.event.EventMessage;
import com.campus.campus_life_backend.modules.message.service.SystemNotificationEventHandler;
import com.campus.campus_life_backend.modules.search.service.SearchOpsMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notification.kafka", name = "enabled", havingValue = "true")
public class SystemNotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SystemNotificationEventConsumer.class);

    private final EventEnvelopeSupport eventEnvelopeSupport;
    private final EventConsumeLogService eventConsumeLogService;
    private final SystemNotificationEventHandler eventHandler;
    private final SearchOpsMetricsService searchOpsMetricsService;

    @Value("${notification.kafka.enabled:true}")
    private boolean enabled;

    @Value("${notification.kafka.group-id:system-notification-consumer}")
    private String consumerGroup;

    public SystemNotificationEventConsumer(
            EventEnvelopeSupport eventEnvelopeSupport,
            EventConsumeLogService eventConsumeLogService,
            SystemNotificationEventHandler eventHandler,
            SearchOpsMetricsService searchOpsMetricsService
    ) {
        this.eventEnvelopeSupport = eventEnvelopeSupport;
        this.eventConsumeLogService = eventConsumeLogService;
        this.eventHandler = eventHandler;
        this.searchOpsMetricsService = searchOpsMetricsService;
    }

    @KafkaListener(
            topics = "${notification.kafka.topic:system-notification-events}",
            groupId = "${notification.kafka.group-id:system-notification-consumer}",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String payload = record == null ? null : record.value();
        if (!enabled || payload == null || payload.isBlank()) {
            return;
        }
        String eventId = null;
        try {
            EventMessage<SystemNotificationEvent> message = eventEnvelopeSupport.read(payload, SystemNotificationEvent.class);
            eventId = message.eventId();
            if (!eventConsumeLogService.begin(eventId, consumerGroup, record.topic(), record.key())) {
                return;
            }
            SystemNotificationEvent event = message.payload();
            eventHandler.handle(event);
            eventConsumeLogService.success(eventId, consumerGroup);
            searchOpsMetricsService.recordConsumerSuccess(SearchOpsMetricsService.CHANNEL_NOTIFICATION);
        } catch (Exception e) {
            eventConsumeLogService.failed(eventId, consumerGroup, e);
            searchOpsMetricsService.recordConsumerFailure(SearchOpsMetricsService.CHANNEL_NOTIFICATION);
            log.error("消费系统通知事件失败 payload={}", payload, e);
            throw new IllegalStateException("消费系统通知事件失败", e);
        }
    }
}
