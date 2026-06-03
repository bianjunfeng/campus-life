package com.campus.campus_life_backend.modules.voucher.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 绉掓潃鍒稿疄浣撶被
 */
@Data
public class SeckillVoucher {

    private Long id;

    private Long voucherId;

    private Integer stock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;
}



