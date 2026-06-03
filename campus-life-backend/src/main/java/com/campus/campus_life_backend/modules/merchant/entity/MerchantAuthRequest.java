package com.campus.campus_life_backend.modules.merchant.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 鍟嗗璁よ瘉鐢宠瀹炰綋绫?
 */
@Data
public class MerchantAuthRequest {

    private Long id;

    private Long userId;

    private String merchantName;

    private String licenseNo;

    private String licenseImg;

    private String address;

    private Integer status;  // 0-寰呭鏍?1-閫氳繃;2-椹冲洖

    private String reason;

    private Long reviewerId;

    private LocalDateTime createTime;

    private LocalDateTime reviewTime;
}



