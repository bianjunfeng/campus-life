package com.campus.campus_life_ai.ai.mapper;

import com.campus.campus_life_ai.ai.entity.AiUsageQuota;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiUsageQuotaMapper {

    List<AiUsageQuota> findAll();

    List<AiUsageQuota> findEnabled();

    AiUsageQuota findById(@Param("id") Long id);

    int countByCapabilityCode(@Param("capabilityCode") String capabilityCode);

    int countBySceneCode(@Param("sceneCode") String sceneCode);

    int insert(AiUsageQuota quota);

    int update(AiUsageQuota quota);

    int deleteById(@Param("id") Long id);
}
