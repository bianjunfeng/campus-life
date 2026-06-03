package com.campus.campus_life_backend.modules.merchant.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission;
import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.service.MerchantLocationService;
import com.campus.campus_life_backend.modules.merchant.service.MerchantQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shops")
public class MerchantController {

    private final MerchantQueryService merchantQueryService;
    private final MerchantLocationService merchantLocationService;

    public MerchantController(
            MerchantQueryService merchantQueryService,
            MerchantLocationService merchantLocationService
    ) {
        this.merchantQueryService = merchantQueryService;
        this.merchantLocationService = merchantLocationService;
    }

    /**
     * 获取商家类型列表
     * GET /api/shops/types
     */
    @GetMapping("/types")
    public ApiResponse<List<Map<String, Object>>> getMerchantTypes() {
        return ApiResponse.success(merchantQueryService.getMerchantTypes());
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getMerchants(
            @RequestParam(value = "typeId", required = false) Long typeId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return ApiResponse.success(merchantQueryService.getMerchants(typeId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Merchant> getMerchantById(@PathVariable Long id) {
        Merchant merchant = merchantQueryService.getMerchantById(id);
        if (merchant == null) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_NOT_FOUND);
        }
        return ApiResponse.success(merchant);
    }

    /**
     * 商家更新地理定位
     * POST /api/shops/{id}/location
     */
    @PostMapping("/{id}/location")
    @RequireLogin
    @RequireOwnerOrPermission(resource = ResourceTypeCode.MERCHANT, idParam = "id")
    public ApiResponse<Map<String, Object>> updateMerchantLocation(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        merchantQueryService.requireActiveMerchantForLocation(id);

        Double longitude = toDouble(request.get("longitude"));
        Double latitude = toDouble(request.get("latitude"));
        if (longitude == null || latitude == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "经纬度不能为空");
        }
        if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "经纬度范围不合法");
        }

        String province = toStringValue(request.get("province"));
        String city = toStringValue(request.get("city"));
        String district = toStringValue(request.get("district"));
        String address = toStringValue(request.get("address"));
        String geoHash = toStringValue(request.get("geoHash"));

        boolean updated = merchantLocationService.updateLocationAndGeo(
                id,
                longitude,
                latitude,
                province,
                city,
                district,
                address,
                geoHash
        );
        if (!updated) {
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "定位更新失败");
        }
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 查询附近商家（按距离升序）
     * GET /api/shops/nearby
     */
    @GetMapping("/nearby")
    public ApiResponse<Map<String, Object>> getNearbyMerchants(
            @RequestParam("lng") Double lng,
            @RequestParam("lat") Double lat,
            @RequestParam(value = "radiusMeters", defaultValue = "3000") Integer radiusMeters,
            @RequestParam(value = "typeId", required = false) Long typeId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        if (lng == null || lat == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "经纬度不能为空");
        }
        if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "经纬度范围不合法");
        }
        return ApiResponse.success(
                merchantLocationService.queryNearby(lng, lat, radiusMeters, typeId, page, size)
        );
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? null : str;
    }
}

