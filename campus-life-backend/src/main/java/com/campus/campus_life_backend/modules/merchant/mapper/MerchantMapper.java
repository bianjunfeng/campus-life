package com.campus.campus_life_backend.modules.merchant.mapper;

import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface MerchantMapper {

    int insertMerchant(Merchant merchant);

    Merchant findById(@Param("id") Long id);

    Merchant findByUserId(@Param("userId") Long userId);

    List<Merchant> findByTypeId(@Param("typeId") Long typeId);

    List<Merchant> findAllActive();

    List<Merchant> findAllLocatable();

    List<Merchant> findAll();

    int updateMerchant(Merchant merchant);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateLocation(
            @Param("id") Long id,
            @Param("longitude") Double longitude,
            @Param("latitude") Double latitude,
            @Param("province") String province,
            @Param("city") String city,
            @Param("district") String district,
            @Param("address") String address,
            @Param("geoHash") String geoHash
    );

    List<Map<String, Object>> findNearbyWithDistance(
            @Param("lng") Double lng,
            @Param("lat") Double lat,
            @Param("radiusMeters") Integer radiusMeters,
            @Param("typeId") Long typeId,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );

    int countNearbyWithDistance(
            @Param("lng") Double lng,
            @Param("lat") Double lat,
            @Param("radiusMeters") Integer radiusMeters,
            @Param("typeId") Long typeId
    );

    long countAllForAdmin();

    long countByStatusForAdmin(@Param("status") Integer status);

    long countMerchantsSince(@Param("since") java.time.LocalDateTime since);

    List<Map<String, Object>> countByTypeForAdmin();
}
