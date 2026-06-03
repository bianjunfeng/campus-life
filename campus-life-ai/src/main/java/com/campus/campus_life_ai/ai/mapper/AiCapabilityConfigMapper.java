package com.campus.campus_life_ai.ai.mapper;

import com.campus.campus_life_ai.ai.entity.AiCapabilityConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiCapabilityConfigMapper {

    List<AiCapabilityConfig> findAll();

    AiCapabilityConfig findById(@Param("id") Long id);

    AiCapabilityConfig findByCapabilityCode(@Param("capabilityCode") String capabilityCode);

    int countByCapabilityCodeExcludingId(@Param("capabilityCode") String capabilityCode, @Param("id") Long id);

    int insert(AiCapabilityConfig config);

    int update(AiCapabilityConfig config);

    int deleteById(@Param("id") Long id);
}
