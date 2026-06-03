package com.campus.campus_life_backend.modules.payment.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentOrder {

    private Long id;

    private String paymentNo;

    private String bizType;

    private String bizOrderNo;

    private Long userId;

    private String channel;

    private String title;

    private String description;

    private BigDecimal amount;

    private BigDecimal refundedAmount;

    private String status;

    private String channelTradeNo;

    private String idempotencyKey;

    private LocalDateTime expireTime;

    private LocalDateTime successTime;

    private LocalDateTime lastCallbackTime;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
