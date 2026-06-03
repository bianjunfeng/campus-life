package com.campus.campus_life_backend.modules.voucher.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShoppingCartItem {

    private Long id;

    private Long userId;

    private Long voucherId;

    private Integer quantity;

    private Integer selected;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
