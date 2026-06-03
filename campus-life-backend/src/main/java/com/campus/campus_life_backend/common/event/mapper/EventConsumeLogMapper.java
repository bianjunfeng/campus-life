package com.campus.campus_life_backend.common.event.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface EventConsumeLogMapper {

    Integer hasSuccess(@Param("eventId") String eventId, @Param("consumerGroup") String consumerGroup);

    int insertProcessing(
            @Param("eventId") String eventId,
            @Param("consumerGroup") String consumerGroup,
            @Param("topic") String topic,
            @Param("messageKey") String messageKey
    );

    int markSuccess(
            @Param("eventId") String eventId,
            @Param("consumerGroup") String consumerGroup,
            @Param("processedAt") LocalDateTime processedAt
    );

    int markFailed(
            @Param("eventId") String eventId,
            @Param("consumerGroup") String consumerGroup,
            @Param("lastError") String lastError
    );
}
