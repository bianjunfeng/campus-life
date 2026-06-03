package com.campus.campus_life_backend.modules.search.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.event.EventEnvelopeSupport;
import com.campus.campus_life_backend.common.event.EventOutboxService;
import com.campus.campus_life_backend.modules.message.event.SystemNotificationEvent;
import com.campus.campus_life_backend.modules.message.service.SystemNotificationEventHandler;
import com.campus.campus_life_backend.modules.order.event.OrderCancelledEvent;
import com.campus.campus_life_backend.modules.order.event.OrderPaidEvent;
import com.campus.campus_life_backend.modules.order.event.OrderPaymentFailedEvent;
import com.campus.campus_life_backend.modules.order.event.OrderRefundedEvent;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.search.event.PostSearchEvent;
import com.campus.campus_life_backend.modules.search.event.UserSearchEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchOpsService {

    private final PostSearchService postSearchService;
    private final UserSearchService userSearchService;
    private final SearchOpsMetricsService searchOpsMetricsService;
    private final SearchIndexCompensationTask searchIndexCompensationTask;
    private final EventOutboxService eventOutboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final EventEnvelopeSupport eventEnvelopeSupport;
    private final SystemNotificationEventHandler systemNotificationEventHandler;
    private final VoucherOrderService voucherOrderService;
    private final boolean searchKafkaEnabled;
    private final boolean notificationKafkaEnabled;
    private final boolean voucherOrderKafkaEnabled;
    private final String searchTopic;
    private final String userSearchTopic;
    private final String notificationTopic;
    private final String voucherOrderTopic;

    public SearchOpsService(
            PostSearchService postSearchService,
            UserSearchService userSearchService,
            SearchOpsMetricsService searchOpsMetricsService,
            SearchIndexCompensationTask searchIndexCompensationTask,
            EventOutboxService eventOutboxService,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            EventEnvelopeSupport eventEnvelopeSupport,
            SystemNotificationEventHandler systemNotificationEventHandler,
            VoucherOrderService voucherOrderService,
            @Value("${search.kafka.enabled:false}") boolean searchKafkaEnabled,
            @Value("${notification.kafka.enabled:true}") boolean notificationKafkaEnabled,
            @Value("${voucher.order.kafka.enabled:false}") boolean voucherOrderKafkaEnabled,
            @Value("${search.kafka.topic:forum-post-search-sync}") String searchTopic,
            @Value("${search.kafka.user-topic:forum-user-search-sync}") String userSearchTopic,
            @Value("${notification.kafka.topic:system-notification-events}") String notificationTopic,
            @Value("${voucher.order.kafka.topic:voucher-order-events}") String voucherOrderTopic
    ) {
        this.postSearchService = postSearchService;
        this.userSearchService = userSearchService;
        this.searchOpsMetricsService = searchOpsMetricsService;
        this.searchIndexCompensationTask = searchIndexCompensationTask;
        this.eventOutboxService = eventOutboxService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.eventEnvelopeSupport = eventEnvelopeSupport;
        this.systemNotificationEventHandler = systemNotificationEventHandler;
        this.voucherOrderService = voucherOrderService;
        this.searchKafkaEnabled = searchKafkaEnabled;
        this.notificationKafkaEnabled = notificationKafkaEnabled;
        this.voucherOrderKafkaEnabled = voucherOrderKafkaEnabled;
        this.searchTopic = searchTopic;
        this.userSearchTopic = userSearchTopic;
        this.notificationTopic = notificationTopic;
        this.voucherOrderTopic = voucherOrderTopic;
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("postSearch", postSearchService.getHealthStatus());
        result.put("userSearch", userSearchService.getHealthStatus());
        result.put("metrics", searchOpsMetricsService.snapshot());
        result.put("outbox", eventOutboxService.snapshot());
        result.put("compensation", searchIndexCompensationTask.snapshot());
        result.put("kafka", Map.of(
                "searchEnabled", searchKafkaEnabled,
                "notificationEnabled", notificationKafkaEnabled,
                "voucherOrderEnabled", voucherOrderKafkaEnabled,
                "postSearchTopic", searchTopic,
                "userSearchTopic", userSearchTopic,
                "notificationTopic", notificationTopic,
                "voucherOrderTopic", voucherOrderTopic
        ));
        return result;
    }

    public Map<String, Object> listFailures(String channel, Long startMillis, Long endMillis, Integer limit) {
        return searchOpsMetricsService.listFailures(channel, startMillis, endMillis, limit == null ? 50 : limit);
    }

    public Map<String, Object> replayFailure(String recordId) {
        Map<String, String> record = searchOpsMetricsService.getFailureRecord(recordId);
        if (record == null || record.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.SEARCH_FAILURE_NOT_FOUND);
        }
        String channel = record.get("channel");
        String key = record.get("messageKey");
        String payload = record.get("payload");
        try {
            if (SearchOpsMetricsService.CHANNEL_POST_SEARCH.equals(channel)) {
                replayPostSearch(key, payload);
            } else if (SearchOpsMetricsService.CHANNEL_USER_SEARCH.equals(channel)) {
                replayUserSearch(key, payload);
            } else if (SearchOpsMetricsService.CHANNEL_NOTIFICATION.equals(channel)) {
                replayNotification(key, payload);
            } else if (SearchOpsMetricsService.CHANNEL_VOUCHER_ORDER.equals(channel)) {
                replayVoucherOrder(key, payload);
            } else {
                throw new BusinessException(BusinessErrorCode.SEARCH_FAILURE_CHANNEL_UNSUPPORTED,
                        "不支持的失败记录通道: " + channel);
            }
            searchOpsMetricsService.markReplayResult(recordId, true, "replayed");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("recordId", recordId);
            result.put("channel", channel);
            result.put("status", "SUCCESS");
            return result;
        } catch (BusinessException e) {
            searchOpsMetricsService.markReplayResult(recordId, false, e.getMessage());
            throw e;
        } catch (Exception e) {
            searchOpsMetricsService.markReplayResult(recordId, false, e.getMessage());
            throw new BusinessException(BusinessErrorCode.SEARCH_REPLAY_FAILED,
                    "重放失败: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> replayFailures(List<String> recordIds) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (recordIds == null || recordIds.isEmpty()) {
            return Map.of("total", 0, "success", 0, "failed", 0, "list", results);
        }
        int success = 0;
        int failed = 0;
        for (String recordId : recordIds) {
            try {
                Map<String, Object> result = replayFailure(recordId);
                results.add(result);
                success++;
            } catch (Exception e) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("recordId", recordId);
                result.put("status", "FAILED");
                result.put("message", e.getMessage());
                results.add(result);
                failed++;
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", recordIds.size());
        summary.put("success", success);
        summary.put("failed", failed);
        summary.put("list", results);
        return summary;
    }

    public Map<String, Object> ignoreFailure(String recordId, String reason) {
        Map<String, String> record = searchOpsMetricsService.getFailureRecord(recordId);
        if (record == null || record.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.SEARCH_FAILURE_NOT_FOUND);
        }
        searchOpsMetricsService.markIgnored(recordId, reason == null ? "ignored by admin" : reason);
        return Map.of("recordId", recordId, "status", "IGNORED");
    }

    private void replayPostSearch(String key, String payload) throws Exception {
        if (searchKafkaEnabled) {
            kafkaTemplate.send(searchTopic, key, payload).get();
            return;
        }
        PostSearchEvent event = eventEnvelopeSupport.read(payload, PostSearchEvent.class).payload();
        postSearchService.applyEvent(event);
    }

    private void replayUserSearch(String key, String payload) throws Exception {
        if (searchKafkaEnabled) {
            kafkaTemplate.send(userSearchTopic, key, payload).get();
            return;
        }
        UserSearchEvent event = eventEnvelopeSupport.read(payload, UserSearchEvent.class).payload();
        userSearchService.applyEvent(event);
    }

    private void replayNotification(String key, String payload) throws Exception {
        if (notificationKafkaEnabled) {
            kafkaTemplate.send(notificationTopic, key, payload).get();
            return;
        }
        SystemNotificationEvent event = eventEnvelopeSupport.read(payload, SystemNotificationEvent.class).payload();
        systemNotificationEventHandler.handle(event);
    }

    private void replayVoucherOrder(String key, String payload) throws Exception {
        if (voucherOrderKafkaEnabled) {
            kafkaTemplate.send(voucherOrderTopic, key, payload).get();
            return;
        }
        JsonNode root = eventEnvelopeSupport.payloadNode(payload);
        String eventType = eventEnvelopeSupport.eventType(payload);
        if (eventType == null || eventType.isBlank()) {
            eventType = root.path("eventType").asText(null);
        }
        if (OrderPaidEvent.EVENT_TYPE.equals(eventType)) {
            OrderPaidEvent event = objectMapper.treeToValue(root, OrderPaidEvent.class);
            voucherOrderService.markOrderPaid(event.getOrderId(), event.getPaymentMethod(), event.getPayTime());
        } else if (OrderPaymentFailedEvent.EVENT_TYPE.equals(eventType)) {
            OrderPaymentFailedEvent event = objectMapper.treeToValue(root, OrderPaymentFailedEvent.class);
            voucherOrderService.markOrderPaymentFailed(event.getOrderId());
        } else if (OrderRefundedEvent.EVENT_TYPE.equals(eventType)) {
            OrderRefundedEvent event = objectMapper.treeToValue(root, OrderRefundedEvent.class);
            voucherOrderService.markOrderRefunded(event.getOrderId(), event.getReason());
        } else if (OrderCancelledEvent.EVENT_TYPE.equals(eventType)) {
            OrderCancelledEvent event = objectMapper.treeToValue(root, OrderCancelledEvent.class);
            voucherOrderService.cancelOrder(event.getOrderId(), event.getUserId(), event.getReason());
        } else {
            throw new BusinessException(BusinessErrorCode.SEARCH_FAILURE_CHANNEL_UNSUPPORTED,
                    "不支持的订单事件类型: " + eventType);
        }
    }
}
