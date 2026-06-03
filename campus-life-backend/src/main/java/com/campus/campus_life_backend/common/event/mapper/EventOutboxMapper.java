package com.campus.campus_life_backend.common.event.mapper;

import com.campus.campus_life_backend.common.event.EventOutboxRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface EventOutboxMapper {

    int insert(EventOutboxRecord record);

    List<EventOutboxRecord> findDue(@Param("now") LocalDateTime now, @Param("limit") int limit);

    int markSending(@Param("id") Long id);

    int markSent(@Param("id") Long id);

    int markFailed(
            @Param("id") Long id,
            @Param("lastError") String lastError,
            @Param("delaySeconds") int delaySeconds
    );

    int resetStaleSending(@Param("threshold") LocalDateTime threshold);

    List<Map<String, Object>> countByStatus();

    List<Map<String, Object>> countByChannelAndStatus();

    LocalDateTime findOldestPendingCreatedAt();

    List<Map<String, Object>> findOldestPendingCreatedAtByChannel();
}
