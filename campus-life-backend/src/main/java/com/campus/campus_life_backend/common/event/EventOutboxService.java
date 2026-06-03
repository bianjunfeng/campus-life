package com.campus.campus_life_backend.common.event;

import com.campus.campus_life_backend.common.event.mapper.EventOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EventOutboxService {

    private final EventOutboxMapper eventOutboxMapper;
    private final EventEnvelopeSupport eventEnvelopeSupport;
    private final ObjectMapper objectMapper;

    public EventOutboxService(
            EventOutboxMapper eventOutboxMapper,
            EventEnvelopeSupport eventEnvelopeSupport,
            ObjectMapper objectMapper
    ) {
        this.eventOutboxMapper = eventOutboxMapper;
        this.eventEnvelopeSupport = eventEnvelopeSupport;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String enqueue(
            String channel,
            String topic,
            String messageKey,
            String eventType,
            String aggregateType,
            String aggregateId,
            Object payload,
            String source
    ) {
        EventEnvelope envelope = eventEnvelopeSupport.build(
                resolveEventId(payload),
                eventType,
                aggregateType,
                aggregateId,
                payload,
                source
        );
        EventOutboxRecord record = new EventOutboxRecord();
        record.setEventId(envelope.getEventId());
        record.setChannel(channel);
        record.setTopic(topic);
        record.setMessageKey(messageKey);
        record.setEventType(eventType);
        record.setAggregateType(aggregateType);
        record.setAggregateId(aggregateId);
        try {
            record.setPayload(objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            throw new IllegalStateException("Serialize event envelope failed", e);
        }
        eventOutboxMapper.insert(record);
        return envelope.getEventId();
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statusCounts", eventOutboxMapper.countByStatus());
        LocalDateTime oldest = eventOutboxMapper.findOldestPendingCreatedAt();
        result.put("oldestPendingCreatedAt", oldest == null ? null : oldest.toString());
        return result;
    }

    private String resolveEventId(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            Object value = payload.getClass().getMethod("getEventId").invoke(payload);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
