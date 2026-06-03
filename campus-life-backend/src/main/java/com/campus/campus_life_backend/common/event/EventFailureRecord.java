package com.campus.campus_life_backend.common.event;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventFailureRecord {

    private String id;
    private String channel;
    private String originalTopic;
    private String dltTopic;
    private String messageKey;
    private String payload;
    private String error;
    private String status;
    private Integer replayCount;
    private LocalDateTime failedAt;
    private LocalDateTime lastReplayAt;
    private String lastReplayStatus;
    private String lastReplayMessage;
}
