package com.campus.campus_life_backend.modules.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRefundResponse {

    private String refundNo;

    private String paymentNo;

    private BigDecimal refundAmount;

    private BigDecimal requestedAmount;

    private BigDecimal approvedAmount;

    private String status;

    private String currentReviewerRole;
}
