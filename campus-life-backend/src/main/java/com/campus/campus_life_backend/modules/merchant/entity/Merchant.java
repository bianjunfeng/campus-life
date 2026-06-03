package com.campus.campus_life_backend.modules.merchant.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 鍟嗗瀹炰綋绫?
 */
@Data
public class Merchant {

    private Long id;

    private Long userId;

    private String name;

    private String contactName;

    private String contactPhone;

    private String address;

    private String businessHours;

    private String province;

    private String city;

    private String district;

    private Double longitude;

    private Double latitude;

    private String geoHash;

    private Integer locationStatus;

    private LocalDateTime locationUpdatedTime;

    private Long typeId;

    private Integer status;  // 0-寰呭鏍?1-姝ｅ父;2-鍐荤粨;3-鍏抽棴

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}


