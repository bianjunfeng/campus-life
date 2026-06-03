package com.campus.campus_life_backend.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EventEnvelopeSupport {

    private final ObjectMapper objectMapper;

    public EventEnvelopeSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventEnvelope build(
            String eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            Object payload,
            String source
    ) {
        EventEnvelope envelope = new EventEnvelope();
        envelope.setEventId(eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId);
        envelope.setEventType(eventType);
        envelope.setEventVersion(1);
        envelope.setAggregateType(aggregateType);
        envelope.setAggregateId(aggregateId);
        envelope.setOccurredAt(System.currentTimeMillis());
        envelope.setSource(source == null || source.isBlank() ? "campus-life-backend" : source);
        envelope.setPayload(objectMapper.valueToTree(payload));
        return envelope;
    }

    public <T> EventMessage<T> read(String rawPayload, Class<T> payloadType) throws Exception {
        JsonNode root = objectMapper.readTree(rawPayload);
        if (isEnvelope(root)) {
            JsonNode payloadNode = root.path("payload");
            T payload = objectMapper.treeToValue(payloadNode, payloadType);
            return new EventMessage<>(
                    text(root, "eventId"),
                    text(root, "eventType"),
                    text(root, "aggregateId"),
                    true,
                    payload
            );
        }
        T payload = objectMapper.treeToValue(root, payloadType);
        return new EventMessage<>(
                text(root, "eventId"),
                firstText(root, "eventType", "action", "type"),
                firstText(root, "orderId", "postId", "userId"),
                false,
                payload
        );
    }

    public JsonNode payloadNode(String rawPayload) throws Exception {
        JsonNode root = objectMapper.readTree(rawPayload);
        return isEnvelope(root) ? root.path("payload") : root;
    }

    public String eventId(String rawPayload) throws Exception {
        JsonNode root = objectMapper.readTree(rawPayload);
        if (isEnvelope(root)) {
            return text(root, "eventId");
        }
        return text(root, "eventId");
    }

    public String eventType(String rawPayload) throws Exception {
        JsonNode root = objectMapper.readTree(rawPayload);
        if (isEnvelope(root)) {
            return text(root, "eventType");
        }
        return firstText(root, "eventType", "action", "type");
    }

    public String aggregateId(String rawPayload) throws Exception {
        JsonNode root = objectMapper.readTree(rawPayload);
        if (isEnvelope(root)) {
            return text(root, "aggregateId");
        }
        return firstText(root, "orderId", "postId", "userId");
    }

    private boolean isEnvelope(JsonNode root) {
        return root != null
                && root.has("payload")
                && (root.has("eventId") || root.has("eventType") || root.has("aggregateType"));
    }

    private String firstText(JsonNode root, String... names) {
        for (String name : names) {
            String value = text(root, name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode root, String name) {
        JsonNode node = root == null ? null : root.get(name);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}
