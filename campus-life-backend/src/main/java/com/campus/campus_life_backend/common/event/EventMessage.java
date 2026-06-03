package com.campus.campus_life_backend.common.event;

public record EventMessage<T>(
        String eventId,
        String eventType,
        String aggregateId,
        boolean enveloped,
        T payload
) {
}
