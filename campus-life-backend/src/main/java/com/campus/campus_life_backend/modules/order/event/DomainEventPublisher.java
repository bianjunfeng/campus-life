package com.campus.campus_life_backend.modules.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${order.event.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${order.event.kafka.topic:order-domain-events}")
    private String topic;

    public DomainEventPublisher(
            ObjectMapper objectMapper,
            @Nullable KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DomainEventType type, String aggregateId, String idempotencyKey, Map<String, Object> payload) {
        DomainEvent event = new DomainEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setType(type);
        event.setAggregateId(aggregateId);
        event.setIdempotencyKey(idempotencyKey);
        event.setTimestamp(System.currentTimeMillis());
        event.setPayload(payload);

        if (!kafkaEnabled || kafkaTemplate == null) {
            log.info("DomainEvent(local): type={}, aggregateId={}, idempotencyKey={}, payload={}",
                    type, aggregateId, idempotencyKey, payload);
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, aggregateId, message)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            log.warn("DomainEvent publish failed: type={}, aggregateId={}", type, aggregateId, ex);
                        }
                    });
        } catch (Exception e) {
            log.warn("DomainEvent serialize failed: type={}, aggregateId={}", type, aggregateId, e);
        }
    }
}
