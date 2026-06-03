package com.campus.campus_life_backend.modules.merchant.service;

import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MerchantLocationService {

    private static final String SHOP_GEO_KEY = "shop:geo:index";

    private final MerchantMapper merchantMapper;
    private final RedisTemplate<String, String> redisTemplate;

    public MerchantLocationService(
            MerchantMapper merchantMapper,
            ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider
    ) {
        this.merchantMapper = merchantMapper;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    @PostConstruct
    public void preloadGeoIndex() {
        if (redisTemplate == null) {
            return;
        }
        try {
            List<Merchant> merchants = merchantMapper.findAllLocatable();
            if (merchants == null || merchants.isEmpty()) {
                return;
            }
            GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
            for (Merchant merchant : merchants) {
                if (merchant.getId() == null || merchant.getLongitude() == null || merchant.getLatitude() == null) {
                    continue;
                }
                geoOps.add(SHOP_GEO_KEY, new Point(merchant.getLongitude(), merchant.getLatitude()), String.valueOf(merchant.getId()));
            }
        } catch (Exception ignored) {
        }
    }

    @RequirePermission(anyOf = {"merchant:location:update:self"})
    public boolean updateLocationAndGeo(
            Long merchantId,
            Double longitude,
            Double latitude,
            String province,
            String city,
            String district,
            String address,
            String geoHash
    ) {
        int updated = merchantMapper.updateLocation(
                merchantId, longitude, latitude, province, city, district, address, geoHash
        );
        if (updated <= 0) {
            return false;
        }
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForGeo().add(
                        SHOP_GEO_KEY,
                        new Point(longitude, latitude),
                        String.valueOf(merchantId)
                );
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public Map<String, Object> queryNearby(
            Double lng,
            Double lat,
            Integer radiusMeters,
            Long typeId,
            Integer page,
            Integer size
    ) {
        int safeRadius = radiusMeters == null || radiusMeters <= 0 ? 3000 : Math.min(radiusMeters, 50000);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : Math.min(size, 50);
        int offset = (safePage - 1) * safeSize;

        List<Map<String, Object>> list = queryNearbyByRedis(lng, lat, safeRadius, typeId, offset, safeSize);
        if (list == null) {
            list = merchantMapper.findNearbyWithDistance(lng, lat, safeRadius, typeId, offset, safeSize);
        }
        Integer total = merchantMapper.countNearbyWithDistance(lng, lat, safeRadius, typeId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list == null ? new ArrayList<>() : list);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", total == null ? 0 : total);
        result.put("radiusMeters", safeRadius);
        return result;
    }

    private List<Map<String, Object>> queryNearbyByRedis(
            Double lng,
            Double lat,
            Integer radiusMeters,
            Long typeId,
            Integer offset,
            Integer size
    ) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            int fetchLimit = Math.min(Math.max((offset + size) * 3, size), 1000);
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().search(
                    SHOP_GEO_KEY,
                    GeoReference.fromCoordinate(lng, lat),
                    new Distance(radiusMeters, RedisGeoCommands.DistanceUnit.METERS),
                    RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                            .includeDistance()
                            .sortAscending()
                            .limit(fetchLimit)
            );
            if (results == null || results.getContent().isEmpty()) {
                return null;
            }
            List<Map<String, Object>> filtered = new ArrayList<>();
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> item : results) {
                RedisGeoCommands.GeoLocation<String> content = item.getContent();
                if (content == null || content.getName() == null) {
                    continue;
                }
                Long merchantId;
                try {
                    merchantId = Long.parseLong(content.getName());
                } catch (Exception e) {
                    continue;
                }
                Merchant merchant = merchantMapper.findById(merchantId);
                if (merchant == null) {
                    continue;
                }
                if (merchant.getStatus() == null || merchant.getStatus() != 1) {
                    continue;
                }
                if (typeId != null && (merchant.getTypeId() == null || !typeId.equals(merchant.getTypeId()))) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("id", merchant.getId());
                row.put("userId", merchant.getUserId());
                row.put("name", merchant.getName());
                row.put("contactName", merchant.getContactName());
                row.put("contactPhone", merchant.getContactPhone());
                row.put("address", merchant.getAddress());
                row.put("province", merchant.getProvince());
                row.put("city", merchant.getCity());
                row.put("district", merchant.getDistrict());
                row.put("typeId", merchant.getTypeId());
                row.put("status", merchant.getStatus());
                row.put("longitude", merchant.getLongitude());
                row.put("latitude", merchant.getLatitude());
                row.put("distanceMeters", item.getDistance() == null ? null : item.getDistance().getValue());
                filtered.add(row);
            }
            if (filtered.isEmpty() || offset >= filtered.size()) {
                return new ArrayList<>();
            }
            int toIndex = Math.min(offset + size, filtered.size());
            return new ArrayList<>(filtered.subList(offset, toIndex));
        } catch (Exception e) {
            return null;
        }
    }
}
