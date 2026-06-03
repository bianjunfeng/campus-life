package com.campus.campus_life_ai.ai.mapper;

import com.campus.campus_life_ai.ai.entity.AiGatewayRouteRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiGatewayRouteRuleMapper {

    List<AiGatewayRouteRule> findAll();

    AiGatewayRouteRule findById(@Param("id") Long id);

    List<AiGatewayRouteRule> findEnabledByCapabilityCode(@Param("capabilityCode") String capabilityCode);

    int countByCapabilityCode(@Param("capabilityCode") String capabilityCode);

    int countBySceneCode(@Param("sceneCode") String sceneCode);

    int insert(AiGatewayRouteRule config);

    int update(AiGatewayRouteRule config);

    int deleteById(@Param("id") Long id);
}
