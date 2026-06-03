package com.campus.campus_life_backend.modules.order.event;

import lombok.Data;

import java.util.Map;

@Data
public class DomainEvent {

    private String eventId;

    private String aggregateId;

    private String idempotencyKey;

    private Long timestamp;

    private DomainEventType type;

    private Map<String, Object> payload;
}
