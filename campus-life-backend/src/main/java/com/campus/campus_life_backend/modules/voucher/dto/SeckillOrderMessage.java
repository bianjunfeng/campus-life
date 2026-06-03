package com.campus.campus_life_backend.modules.voucher.dto;

import lombok.Data;

@Data
public class SeckillOrderMessage {
    private String orderNo;
    private Long userId;
    private Long voucherId;
    private long createEpochSecond;
}
