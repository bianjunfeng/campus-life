package com.campus.campus_life_backend.modules.search.event;

import com.campus.campus_life_backend.common.event.EventOutboxService;
import com.campus.campus_life_backend.modules.search.service.PostSearchService;
import com.campus.campus_life_backend.modules.search.service.SearchOpsMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PostSearchEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PostSearchEventPublisher.class);

    private final EventOutboxService eventOutboxService;
    private final PostSearchService postSearchService;
    private final SearchOpsMetricsService searchOpsMetricsService;

    @Value("${search.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${search.kafka.topic:forum-post-search-sync}")
    private String topic;

    public PostSearchEventPublisher(
            EventOutboxService eventOutboxService,
            PostSearchService postSearchService,
            SearchOpsMetricsService searchOpsMetricsService
    ) {
        this.eventOutboxService = eventOutboxService;
        this.postSearchService = postSearchService;
        this.searchOpsMetricsService = searchOpsMetricsService;
    }

    public void publishUpsert(Long postId, String reason) {
        publish(postId, "UPSERT", reason);
    }

    public void publishDelete(Long postId, String reason) {
        publish(postId, "DELETE", reason);
    }

    private void publish(Long postId, String action, String reason) {
        if (postId == null) {
            return;
        }
        PostSearchEvent event = new PostSearchEvent();
        event.setPostId(postId);
        event.setAction(action);
        event.setReason(reason);
        event.setTimestamp(System.currentTimeMillis());
        event.setSyncVersion(postSearchService.resolveSyncVersion(postId));

        if (!kafkaEnabled) {
            fallbackHandle(event);
            return;
        }

        try {
            eventOutboxService.enqueue(
                    SearchOpsMetricsService.CHANNEL_POST_SEARCH,
                    topic,
                    String.valueOf(postId),
                    "POST_" + action,
                    "POST",
                    String.valueOf(postId),
                    event,
                    "post-search"
            );
        } catch (Exception e) {
            searchOpsMetricsService.recordProducerFailure(SearchOpsMetricsService.CHANNEL_POST_SEARCH);
            log.error("写入帖子搜索 outbox 失败，降级直写ES topic={}, postId={}, action={}, reason={}",
                    topic, postId, action, reason, e);
            fallbackHandle(event);
        }
    }

    private void fallbackHandle(PostSearchEvent event) {
        try {
            postSearchService.applyEvent(event);
        } catch (Exception e) {
            log.error("帖子搜索事件降级处理失败 postId={}, action={}, reason={}",
                    event.getPostId(), event.getAction(), event.getReason(), e);
        }
    }
}
