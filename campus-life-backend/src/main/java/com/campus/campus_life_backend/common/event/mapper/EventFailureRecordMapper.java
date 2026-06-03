package com.campus.campus_life_backend.common.event.mapper;

import com.campus.campus_life_backend.common.event.EventFailureRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface EventFailureRecordMapper {

    int insert(EventFailureRecord record);

    EventFailureRecord findById(@Param("id") String id);

    List<EventFailureRecord> list(
            @Param("channel") String channel,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit
    );

    int markReplayResult(
            @Param("id") String id,
            @Param("success") boolean success,
            @Param("message") String message
    );

    int markIgnored(@Param("id") String id, @Param("message") String message);

    List<Map<String, Object>> countByChannelAndStatus();
}
