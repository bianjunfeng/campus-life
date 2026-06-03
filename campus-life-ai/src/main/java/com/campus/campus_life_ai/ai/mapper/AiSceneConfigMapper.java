package com.campus.campus_life_ai.ai.mapper;

import com.campus.campus_life_ai.ai.entity.AiSceneConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiSceneConfigMapper {

    List<AiSceneConfig> findAll();

    AiSceneConfig findById(@Param("id") Long id);

    AiSceneConfig findBySceneCode(@Param("sceneCode") String sceneCode);

    int countBySceneCodeExcludingId(@Param("sceneCode") String sceneCode, @Param("id") Long id);

    int countByCapabilityCode(@Param("capabilityCode") String capabilityCode);

    int countByProviderCode(@Param("providerCode") String providerCode);

    int insert(AiSceneConfig config);

    int update(AiSceneConfig config);

    int deleteById(@Param("id") Long id);
}
