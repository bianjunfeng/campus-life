package com.campus.campus_life_ai.ai.mapper;

import com.campus.campus_life_ai.ai.entity.AiCallLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

public interface AiCallLogMapper {

    int insert(AiCallLog callLog);

    List<AiCallLog> findRecent(@Param("limit") Integer limit);

    Map<String, Object> aggregateOverview();

    Map<String, Object> aggregateQuotaUsage(@Param("periodStart") LocalDateTime periodStart,
                                            @Param("userId") Long userId,
                                            @Param("capabilityCode") String capabilityCode,
                                            @Param("sceneCode") String sceneCode);

    List<Map<String, Object>> findUsageTrend(@Param("from") LocalDateTime from);

    List<Map<String, Object>> findModelRanking(@Param("limit") Integer limit);
}
