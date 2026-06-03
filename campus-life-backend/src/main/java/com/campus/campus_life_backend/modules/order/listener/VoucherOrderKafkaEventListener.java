package com.campus.campus_life_backend.modules.order.listener;

import com.campus.campus_life_backend.common.event.EventConsumeLogService;
import com.campus.campus_life_backend.common.event.EventEnvelopeSupport;
import com.campus.campus_life_backend.modules.order.event.AbstractVoucherOrderEvent;
import com.campus.campus_life_backend.modules.order.event.OrderCancelledEvent;
import com.campus.campus_life_backend.modules.order.event.OrderPaidEvent;
import com.campus.campus_life_backend.modules.order.event.OrderPaymentFailedEvent;
import com.campus.campus_life_backend.modules.order.event.OrderRefundedEvent;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.search.service.SearchOpsMetricsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "voucher.order.kafka", name = "enabled", havingValue = "true")
public class VoucherOrderKafkaEventListener {

    private static final Logger log = LoggerFactory.getLogger(VoucherOrderKafkaEventListener.class);

    private final ObjectMapper objectMapper;
    private final EventEnvelopeSupport eventEnvelopeSupport;
    private final EventConsumeLogService eventConsumeLogService;
    private final VoucherOrderService voucherOrderService;
    private final SearchOpsMetricsService searchOpsMetricsService;
    private final String consumerGroup;

    public VoucherOrderKafkaEventListener(
            ObjectMapper objectMapper,
            EventEnvelopeSupport eventEnvelopeSupport,
            EventConsumeLogService eventConsumeLogService,
            VoucherOrderService voucherOrderService,
            SearchOpsMetricsService searchOpsMetricsService,
            @Value("${voucher.order.kafka.group-id:voucher-order-consumer}") String consumerGroup
    ) {
        this.objectMapper = objectMapper;
        this.eventEnvelopeSupport = eventEnvelopeSupport;
        this.eventConsumeLogService = eventConsumeLogService;
        this.voucherOrderService = voucherOrderService;
        this.searchOpsMetricsService = searchOpsMetricsService;
        this.consumerGroup = consumerGroup;
    }

    @KafkaListener(
            topics = "${voucher.order.kafka.topic:voucher-order-events}",
            groupId = "${voucher.order.kafka.group-id:voucher-order-consumer}",
            containerFactory = "voucherOrderKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String payload = record == null ? null : record.value();
        String eventId = null;
        try {
            eventId = eventEnvelopeSupport.eventId(payload);
            if (!eventConsumeLogService.begin(eventId, consumerGroup, record.topic(), record.key())) {
                return;
            }
            JsonNode root = eventEnvelopeSupport.payloadNode(payload);
            String eventType = eventEnvelopeSupport.eventType(payload);
            if (eventType == null || eventType.isBlank()) {
                eventType = root.path("eventType").asText(null);
            }
            if (eventType == null || eventType.isBlank()) {
                log.warn("Ignore voucher order event without eventType payload={}", payload);
                eventConsumeLogService.success(eventId, consumerGroup);
                return;
            }
            switch (eventType) {
                case OrderPaidEvent.EVENT_TYPE -> handleOrderPaid(objectMapper.treeToValue(root, OrderPaidEvent.class));
                case OrderPaymentFailedEvent.EVENT_TYPE -> handlePaymentFailed(objectMapper.treeToValue(root, OrderPaymentFailedEvent.class));
                case OrderRefundedEvent.EVENT_TYPE -> handleOrderRefunded(objectMapper.treeToValue(root, OrderRefundedEvent.class));
                case OrderCancelledEvent.EVENT_TYPE -> handleOrderCancelled(objectMapper.treeToValue(root, OrderCancelledEvent.class));
                default -> log.warn("Ignore unknown voucher order eventType={} payload={}", eventType, payload);
            }
            eventConsumeLogService.success(eventId, consumerGroup);
            searchOpsMetricsService.recordConsumerSuccess(SearchOpsMetricsService.CHANNEL_VOUCHER_ORDER);
        } catch (Exception e) {
            eventConsumeLogService.failed(eventId, consumerGroup, e);
            searchOpsMetricsService.recordConsumerFailure(SearchOpsMetricsService.CHANNEL_VOUCHER_ORDER);
            log.error("Consume voucher order event failed payload={}", payload, e);
            throw new IllegalStateException("消费 VoucherOrder Kafka 事件失败", e);
        }
    }

    private void handleOrderPaid(OrderPaidEvent event) {
        if (!isValid(event)) {
            return;
        }
        voucherOrderService.markOrderPaid(event.getOrderId(), event.getPaymentMethod(), event.getPayTime());
    }

    private void handlePaymentFailed(OrderPaymentFailedEvent event) {
        if (!isValid(event)) {
            return;
        }
        voucherOrderService.markOrderPaymentFailed(event.getOrderId());
    }

    private void handleOrderRefunded(OrderRefundedEvent event) {
        if (!isValid(event)) {
            return;
        }
        voucherOrderService.markOrderRefunded(event.getOrderId(), event.getReason());
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        if (!isValid(event) || event.getUserId() == null) {
            return;
        }
        voucherOrderService.cancelOrder(event.getOrderId(), event.getUserId(), event.getReason());
    }

    private boolean isValid(AbstractVoucherOrderEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Ignore invalid voucher order event event={}", event);
            return false;
        }
        return true;
    }
}
