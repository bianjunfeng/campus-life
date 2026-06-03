package com.campus.campus_life_backend.modules.merchant.mapper;

import com.campus.campus_life_backend.modules.merchant.entity.MerchantType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MerchantTypeMapper {

    int insertMerchantType(MerchantType merchantType);

    MerchantType findById(@Param("id") Long id);

    MerchantType findByCode(@Param("code") String code);

    List<MerchantType> findAllActive();

    int updateMerchantType(MerchantType merchantType);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}



