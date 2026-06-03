package com.campus.campus_life_backend.modules.payment.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentCallbackLog {

    private Long id;

    private String channel;

    private String callbackType;

    private String paymentNo;

    private String bizOrderNo;

    private String rawBody;

    private Integer signatureVerified;

    private Integer amountVerified;

    private String processStatus;

    private String errorMessage;

    private LocalDateTime createTime;
}
