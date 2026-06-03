package com.campus.campus_life_backend.modules.merchant.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 鍟嗗绫诲瀷瀹炰綋绫?
 */
@Data
public class MerchantType {

    private Long id;

    private String code;

    private String name;

    private String description;

    private Integer sortOrder;

    private Integer status;  // 1-鍚敤;0-绂佺敤

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}



