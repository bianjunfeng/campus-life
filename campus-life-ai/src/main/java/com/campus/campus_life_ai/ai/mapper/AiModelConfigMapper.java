package com.campus.campus_life_ai.ai.mapper;

import com.campus.campus_life_ai.ai.entity.AiModelConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiModelConfigMapper {

    List<AiModelConfig> findAll();

    AiModelConfig findById(@Param("id") Long id);

    AiModelConfig findByProviderAndModelCode(@Param("providerCode") String providerCode,
                                             @Param("modelCode") String modelCode);

    AiModelConfig findEnabledByProviderAndModelCode(@Param("providerCode") String providerCode,
                                                    @Param("modelCode") String modelCode);

    int countByProviderAndModelExcludingId(@Param("providerCode") String providerCode,
                                           @Param("modelCode") String modelCode,
                                           @Param("id") Long id);

    int countByProviderCode(@Param("providerCode") String providerCode);

    int insert(AiModelConfig config);

    int update(AiModelConfig config);

    int deleteById(@Param("id") Long id);
}
