package com.campus.campus_life_backend.modules.payment.enums;

public enum PaymentBizType {
    VOUCHER("VOUCHER"),
    GOODS("GOODS"),
    MEMBER("MEMBER"),
    RECHARGE("RECHARGE");

    private final String code;

    PaymentBizType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
