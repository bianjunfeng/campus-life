package com.campus.campus_life_backend.modules.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRefundReviewRequest {

    private BigDecimal approvedAmount;

    private String reason;
}
