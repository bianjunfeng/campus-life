package com.campus.campus_life_backend.common.event.mapper;

import org.apache.ibatis.annotations.Param;

public interface SearchSyncCheckpointMapper {

    Long findCheckpointMillis(@Param("checkpointName") String checkpointName);

    int upsertSuccess(
            @Param("checkpointName") String checkpointName,
            @Param("checkpointMillis") long checkpointMillis
    );

    int markFailure(
            @Param("checkpointName") String checkpointName,
            @Param("lastError") String lastError
    );
}
