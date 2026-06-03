package com.campus.campus_life_backend.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class EventEnvelope {

    private String eventId;
    private String eventType;
    private Integer eventVersion;
    private String aggregateType;
    private String aggregateId;
    private Long occurredAt;
    private String source;
    private String traceId;
    private JsonNode payload;
}
