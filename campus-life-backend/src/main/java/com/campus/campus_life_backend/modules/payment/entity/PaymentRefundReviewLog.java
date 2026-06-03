package com.campus.campus_life_backend.modules.payment.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentRefundReviewLog {

    private Long id;

    private String refundNo;

    private String operatorRole;

    private Long operatorId;

    private String action;

    private String fromStatus;

    private String toStatus;

    private String comment;

    private LocalDateTime createTime;
}
