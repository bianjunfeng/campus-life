package com.campus.campus_life_backend.modules.payment.enums;


/**
 * 支付方式枚举
 */
public enum PaymentMethod {
    /**
     * 支付宝
     */
    ALIPAY("alipay", "支付宝"),

    /**
     * 微信支付
     */
    WECHAT("wechat", "微信支付"),

    /**
     * 钱包支付
     */
    WALLET("wallet", "钱包支付");

    private final String code;
    private final String name;

    PaymentMethod(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static PaymentMethod fromCode(String code) {
        for (PaymentMethod method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("未知的支付方式: " + code);
    }
}
