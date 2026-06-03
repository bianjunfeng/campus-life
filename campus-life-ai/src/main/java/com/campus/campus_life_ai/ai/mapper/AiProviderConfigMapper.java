package com.campus.campus_life_ai.ai.mapper;

import com.campus.campus_life_ai.ai.entity.AiProviderConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiProviderConfigMapper {

    List<AiProviderConfig> findAll();

    AiProviderConfig findById(@Param("id") Long id);

    AiProviderConfig findByProviderCode(@Param("providerCode") String providerCode);

    int countByProviderCodeExcludingId(@Param("providerCode") String providerCode, @Param("id") Long id);

    int insert(AiProviderConfig config);

    int update(AiProviderConfig config);

    int deleteById(@Param("id") Long id);
}
