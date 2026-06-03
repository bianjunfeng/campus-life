package com.campus.campus_life_backend.modules.payment.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentRefundOrder {

    private Long id;

    private String refundNo;

    private String paymentNo;

    private String bizType;

    private String bizOrderNo;

    private Long userId;

    private Long merchantId;

    private String channel;

    private BigDecimal refundAmount;

    private BigDecimal requestedAmount;

    private BigDecimal approvedAmount;

    private String reason;

    private String status;

    private String currentReviewerRole;

    private Long currentReviewerId;

    private LocalDateTime reviewDeadline;

    private String merchantReviewReason;

    private LocalDateTime merchantReviewTime;

    private Long merchantReviewerId;

    private String adminReviewReason;

    private LocalDateTime adminReviewTime;

    private Long adminReviewerId;

    private String channelRefundNo;

    private String requestIdempotencyKey;

    private LocalDateTime successTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
