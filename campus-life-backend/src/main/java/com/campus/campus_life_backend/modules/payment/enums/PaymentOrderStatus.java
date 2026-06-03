package com.campus.campus_life_backend.modules.payment.enums;

public enum PaymentOrderStatus {
    INIT("INIT"),
    WAIT_PAY("WAIT_PAY"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    CLOSED("CLOSED"),
    PARTIAL_REFUNDED("PARTIAL_REFUNDED"),
    FULL_REFUNDED("FULL_REFUNDED");

    private final String code;

    PaymentOrderStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
