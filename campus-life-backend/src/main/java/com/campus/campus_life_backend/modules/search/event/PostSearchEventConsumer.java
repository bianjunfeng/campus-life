package com.campus.campus_life_backend.modules.search.event;

import com.campus.campus_life_backend.common.event.EventConsumeLogService;
import com.campus.campus_life_backend.common.event.EventEnvelopeSupport;
import com.campus.campus_life_backend.common.event.EventMessage;
import com.campus.campus_life_backend.modules.search.service.PostSearchService;
import com.campus.campus_life_backend.modules.search.service.SearchOpsMetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "search.kafka.enabled", havingValue = "true")
public class PostSearchEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PostSearchEventConsumer.class);

    private final EventEnvelopeSupport eventEnvelopeSupport;
    private final EventConsumeLogService eventConsumeLogService;
    private final PostSearchService postSearchService;
    private final SearchOpsMetricsService searchOpsMetricsService;
    private final String consumerGroup;

    public PostSearchEventConsumer(
            EventEnvelopeSupport eventEnvelopeSupport,
            EventConsumeLogService eventConsumeLogService,
            PostSearchService postSearchService,
            SearchOpsMetricsService searchOpsMetricsService,
            @Value("${search.kafka.group-id:forum-search-indexer}") String consumerGroup
    ) {
        this.eventEnvelopeSupport = eventEnvelopeSupport;
        this.eventConsumeLogService = eventConsumeLogService;
        this.postSearchService = postSearchService;
        this.searchOpsMetricsService = searchOpsMetricsService;
        this.consumerGroup = consumerGroup;
    }

    @KafkaListener(
            topics = "${search.kafka.topic:forum-post-search-sync}",
            groupId = "${search.kafka.group-id:forum-search-indexer}",
            containerFactory = "searchKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String payload = record == null ? null : record.value();
        String eventId = null;
        try {
            EventMessage<PostSearchEvent> message = eventEnvelopeSupport.read(payload, PostSearchEvent.class);
            eventId = message.eventId();
            if (!eventConsumeLogService.begin(eventId, consumerGroup, record.topic(), record.key())) {
                return;
            }
            PostSearchEvent event = message.payload();
            if (event.getPostId() == null) {
                log.warn("忽略无效的帖子搜索事件 payload={}", payload);
                eventConsumeLogService.success(eventId, consumerGroup);
                return;
            }
            postSearchService.applyEvent(event);
            eventConsumeLogService.success(eventId, consumerGroup);
            searchOpsMetricsService.recordConsumerSuccess(SearchOpsMetricsService.CHANNEL_POST_SEARCH);
        } catch (Exception e) {
            eventConsumeLogService.failed(eventId, consumerGroup, e);
            searchOpsMetricsService.recordConsumerFailure(SearchOpsMetricsService.CHANNEL_POST_SEARCH);
            log.error("消费帖子搜索事件失败 payload={}", payload, e);
            throw new IllegalStateException("消费帖子搜索事件失败", e);
        }
    }
}
