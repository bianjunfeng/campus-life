package com.campus.campus_life_backend.common.event;

import com.campus.campus_life_backend.common.event.mapper.EventConsumeLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EventConsumeLogService {

    private final EventConsumeLogMapper eventConsumeLogMapper;

    public EventConsumeLogService(EventConsumeLogMapper eventConsumeLogMapper) {
        this.eventConsumeLogMapper = eventConsumeLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean begin(String eventId, String consumerGroup, String topic, String messageKey) {
        if (eventId == null || eventId.isBlank()) {
            return true;
        }
        Integer success = eventConsumeLogMapper.hasSuccess(eventId, consumerGroup);
        if (success != null && success > 0) {
            return false;
        }
        eventConsumeLogMapper.insertProcessing(eventId, consumerGroup, topic, messageKey);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(String eventId, String consumerGroup) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        eventConsumeLogMapper.markSuccess(eventId, consumerGroup, LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(String eventId, String consumerGroup, Throwable error) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        eventConsumeLogMapper.markFailed(eventId, consumerGroup, shortError(error));
    }

    private String shortError(Throwable error) {
        if (error == null || error.getMessage() == null) {
            return "";
        }
        String message = error.getMessage();
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
