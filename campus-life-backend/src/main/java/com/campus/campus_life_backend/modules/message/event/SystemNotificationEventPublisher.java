package com.campus.campus_life_backend.modules.message.event;

import com.campus.campus_life_backend.common.event.EventOutboxService;
import com.campus.campus_life_backend.modules.message.service.SystemNotificationEventHandler;
import com.campus.campus_life_backend.modules.search.service.SearchOpsMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SystemNotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SystemNotificationEventPublisher.class);

    private final EventOutboxService eventOutboxService;
    private final SystemNotificationEventHandler eventHandler;
    private final SearchOpsMetricsService searchOpsMetricsService;

    @Value("${notification.kafka.enabled:true}")
    private boolean enabled;

    @Value("${notification.kafka.topic:system-notification-events}")
    private String topic;

    public SystemNotificationEventPublisher(
            EventOutboxService eventOutboxService,
            SystemNotificationEventHandler eventHandler,
            SearchOpsMetricsService searchOpsMetricsService
    ) {
        this.eventOutboxService = eventOutboxService;
        this.eventHandler = eventHandler;
        this.searchOpsMetricsService = searchOpsMetricsService;
    }

    public void publish(SystemNotificationEvent event) {
        if (event == null) {
            return;
        }
        if (!enabled) {
            fallbackHandle(event);
            return;
        }
        try {
            eventOutboxService.enqueue(
                    SearchOpsMetricsService.CHANNEL_NOTIFICATION,
                    topic,
                    String.valueOf(event.getTargetUserId()),
                    event.getType(),
                    "NOTIFICATION",
                    event.getTargetUserId() == null ? null : String.valueOf(event.getTargetUserId()),
                    event,
                    "system-notification"
            );
        } catch (Exception e) {
            handleSendFailure(event, e);
        }
    }

    private void handleSendFailure(SystemNotificationEvent event, Throwable throwable) {
        searchOpsMetricsService.recordProducerFailure(SearchOpsMetricsService.CHANNEL_NOTIFICATION);
        log.warn("Kafka通知事件发送失败，降级本地处理 topic={}, eventId={}, targetUserId={}",
                topic, event.getEventId(), event.getTargetUserId(), throwable);
        fallbackHandle(event);
    }

    private void fallbackHandle(SystemNotificationEvent event) {
        try {
            eventHandler.handle(event);
        } catch (Exception e) {
            log.error("通知事件降级处理失败 eventId={}, targetUserId={}",
                    event.getEventId(), event.getTargetUserId(), e);
        }
    }
}
