package com.campus.campus_life_backend.modules.order.event;

import com.campus.campus_life_backend.common.event.EventOutboxService;
import com.campus.campus_life_backend.modules.search.service.SearchOpsMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VoucherOrderKafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(VoucherOrderKafkaEventPublisher.class);

    private final EventOutboxService eventOutboxService;
    private final SearchOpsMetricsService searchOpsMetricsService;

    @Value("${voucher.order.kafka.enabled:false}")
    private boolean enabled;

    @Value("${voucher.order.kafka.topic:voucher-order-events}")
    private String topic;

    public VoucherOrderKafkaEventPublisher(
            EventOutboxService eventOutboxService,
            SearchOpsMetricsService searchOpsMetricsService
    ) {
        this.eventOutboxService = eventOutboxService;
        this.searchOpsMetricsService = searchOpsMetricsService;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void publish(AbstractVoucherOrderEvent event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        if (!enabled) {
            log.debug("VoucherOrder Kafka disabled, skip publish eventType={}, orderId={}",
                    event.getEventType(), event.getOrderId());
            return;
        }
        try {
            eventOutboxService.enqueue(
                    SearchOpsMetricsService.CHANNEL_VOUCHER_ORDER,
                    topic,
                    String.valueOf(event.getOrderId()),
                    event.getEventType(),
                    "VOUCHER_ORDER",
                    String.valueOf(event.getOrderId()),
                    event,
                    event.getSource()
            );
        } catch (Exception e) {
            searchOpsMetricsService.recordProducerFailure(SearchOpsMetricsService.CHANNEL_VOUCHER_ORDER);
            log.warn("VoucherOrder outbox enqueue failed topic={}, eventType={}, orderId={}",
                    topic, event.getEventType(), event.getOrderId(), e);
            throw new IllegalStateException("写入 VoucherOrder outbox 失败", e);
        }
    }
}
