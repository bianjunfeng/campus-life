package com.campus.campus_life_backend.modules.voucher.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 浼樻儬鍒稿疄浣撶被
 */
@Data
public class Voucher {

    private Long id;

    private Long merchantId;

    private String title;

    private String subTitle;

    private String imageUrl;

    private Integer stock;

    private Integer soldCount;

    private BigDecimal amount;

    private BigDecimal payValue;

    private Integer status;  // 1-涓婃灦;0-涓嬫灦

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

