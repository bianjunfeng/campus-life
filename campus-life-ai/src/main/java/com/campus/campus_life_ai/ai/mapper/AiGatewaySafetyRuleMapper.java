package com.campus.campus_life_ai.ai.mapper;

import com.campus.campus_life_ai.ai.entity.AiGatewaySafetyRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiGatewaySafetyRuleMapper {

    List<AiGatewaySafetyRule> findAll();

    List<AiGatewaySafetyRule> findEnabled();

    AiGatewaySafetyRule findById(@Param("id") Long id);

    int countByCapabilityCode(@Param("capabilityCode") String capabilityCode);

    int countBySceneCode(@Param("sceneCode") String sceneCode);

    int insert(AiGatewaySafetyRule rule);

    int update(AiGatewaySafetyRule rule);

    int deleteById(@Param("id") Long id);
}
