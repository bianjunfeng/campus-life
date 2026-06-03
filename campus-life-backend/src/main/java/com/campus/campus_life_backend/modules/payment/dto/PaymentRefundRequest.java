package com.campus.campus_life_backend.modules.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRefundRequest {

    private String orderNo;

    private BigDecimal refundAmount;

    private String reason;
}
