package com.campus.campus_life_backend.modules.search.event;

import com.campus.campus_life_backend.common.event.EventConsumeLogService;
import com.campus.campus_life_backend.common.event.EventEnvelopeSupport;
import com.campus.campus_life_backend.common.event.EventMessage;
import com.campus.campus_life_backend.modules.search.service.SearchOpsMetricsService;
import com.campus.campus_life_backend.modules.search.service.UserSearchService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "search.kafka.enabled", havingValue = "true")
public class UserSearchEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserSearchEventConsumer.class);

    private final EventEnvelopeSupport eventEnvelopeSupport;
    private final EventConsumeLogService eventConsumeLogService;
    private final UserSearchService userSearchService;
    private final SearchOpsMetricsService searchOpsMetricsService;
    private final String consumerGroup;

    public UserSearchEventConsumer(
            EventEnvelopeSupport eventEnvelopeSupport,
            EventConsumeLogService eventConsumeLogService,
            UserSearchService userSearchService,
            SearchOpsMetricsService searchOpsMetricsService,
            @Value("${search.kafka.user-group-id:forum-user-search-indexer}") String consumerGroup
    ) {
        this.eventEnvelopeSupport = eventEnvelopeSupport;
        this.eventConsumeLogService = eventConsumeLogService;
        this.userSearchService = userSearchService;
        this.searchOpsMetricsService = searchOpsMetricsService;
        this.consumerGroup = consumerGroup;
    }

    @KafkaListener(
            topics = "${search.kafka.user-topic:forum-user-search-sync}",
            groupId = "${search.kafka.user-group-id:forum-user-search-indexer}",
            containerFactory = "userSearchKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String payload = record == null ? null : record.value();
        String eventId = null;
        try {
            EventMessage<UserSearchEvent> message = eventEnvelopeSupport.read(payload, UserSearchEvent.class);
            eventId = message.eventId();
            if (!eventConsumeLogService.begin(eventId, consumerGroup, record.topic(), record.key())) {
                return;
            }
            UserSearchEvent event = message.payload();
            if (event.getUserId() == null) {
                log.warn("忽略无效的用户搜索事件 payload={}", payload);
                eventConsumeLogService.success(eventId, consumerGroup);
                return;
            }
            userSearchService.applyEvent(event);
            eventConsumeLogService.success(eventId, consumerGroup);
            searchOpsMetricsService.recordConsumerSuccess(SearchOpsMetricsService.CHANNEL_USER_SEARCH);
        } catch (Exception e) {
            eventConsumeLogService.failed(eventId, consumerGroup, e);
            searchOpsMetricsService.recordConsumerFailure(SearchOpsMetricsService.CHANNEL_USER_SEARCH);
            log.error("消费用户搜索事件失败 payload={}", payload, e);
            throw new IllegalStateException("消费用户搜索事件失败", e);
        }
    }
}
