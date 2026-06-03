package com.campus.campus_life_backend.common.event;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventOutboxRecord {

    private Long id;
    private String eventId;
    private String channel;
    private String topic;
    private String messageKey;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;
}
