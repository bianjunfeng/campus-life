package com.campus.campus_life_backend.modules.payment.enums;

public enum PaymentRefundStatus {
    INIT("INIT"),
    WAIT_MERCHANT_REVIEW("WAIT_MERCHANT_REVIEW"),
    WAIT_ADMIN_REVIEW("WAIT_ADMIN_REVIEW"),
    PROCESSING("PROCESSING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    REJECTED("REJECTED"),
    CLOSED("CLOSED");

    private final String code;

    PaymentRefundStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
