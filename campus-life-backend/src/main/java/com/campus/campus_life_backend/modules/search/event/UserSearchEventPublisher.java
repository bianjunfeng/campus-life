package com.campus.campus_life_backend.modules.search.event;

import com.campus.campus_life_backend.common.event.EventOutboxService;
import com.campus.campus_life_backend.modules.search.service.SearchOpsMetricsService;
import com.campus.campus_life_backend.modules.search.service.UserSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserSearchEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserSearchEventPublisher.class);

    private final EventOutboxService eventOutboxService;
    private final UserSearchService userSearchService;
    private final SearchOpsMetricsService searchOpsMetricsService;

    @Value("${search.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${search.kafka.user-topic:forum-user-search-sync}")
    private String topic;

    public UserSearchEventPublisher(
            EventOutboxService eventOutboxService,
            UserSearchService userSearchService,
            SearchOpsMetricsService searchOpsMetricsService
    ) {
        this.eventOutboxService = eventOutboxService;
        this.userSearchService = userSearchService;
        this.searchOpsMetricsService = searchOpsMetricsService;
    }

    public void publishUpsert(Long userId, String reason) {
        publish(userId, "UPSERT", reason);
    }

    public void publishDelete(Long userId, String reason) {
        publish(userId, "DELETE", reason);
    }

    private void publish(Long userId, String action, String reason) {
        if (userId == null) {
            return;
        }
        UserSearchEvent event = new UserSearchEvent();
        event.setUserId(userId);
        event.setAction(action);
        event.setReason(reason);
        event.setTimestamp(System.currentTimeMillis());
        event.setSyncVersion(userSearchService.resolveSyncVersion(userId));

        if (!kafkaEnabled) {
            fallbackHandle(event);
            return;
        }

        try {
            eventOutboxService.enqueue(
                    SearchOpsMetricsService.CHANNEL_USER_SEARCH,
                    topic,
                    String.valueOf(userId),
                    "USER_" + action,
                    "USER",
                    String.valueOf(userId),
                    event,
                    "user-search"
            );
        } catch (Exception e) {
            searchOpsMetricsService.recordProducerFailure(SearchOpsMetricsService.CHANNEL_USER_SEARCH);
            log.error("写入用户搜索 outbox 失败，降级直写ES topic={}, userId={}, action={}, reason={}",
                    topic, userId, action, reason, e);
            fallbackHandle(event);
        }
    }

    private void fallbackHandle(UserSearchEvent event) {
        try {
            userSearchService.applyEvent(event);
        } catch (Exception e) {
            log.error("用户搜索事件降级处理失败 userId={}, action={}, reason={}",
                    event.getUserId(), event.getAction(), event.getReason(), e);
        }
    }
}
