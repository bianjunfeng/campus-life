package com.campus.campus_life_backend.modules.payment.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentReconciliationIssue {

    private Long id;
    private String issueKey;
    private String bizType;
    private String paymentNo;
    private String bizOrderNo;
    private Long userId;
    private String channel;
    private String issueType;
    private String issueLevel;
    private String issueStatus;
    private String issueMessage;
    private String paymentOrderStatus;
    private Integer voucherOrderStatus;
    private Integer voucherPaymentStatus;
    private BigDecimal paymentAmount;
    private BigDecimal recordedRefundedAmount;
    private BigDecimal actualRefundedAmount;
    private LocalDateTime lastCheckedTime;
    private LocalDateTime resolvedTime;
    private Long resolvedBy;
    private String resolveNote;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
