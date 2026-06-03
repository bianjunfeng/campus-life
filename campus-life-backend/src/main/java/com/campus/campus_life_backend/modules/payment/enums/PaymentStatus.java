package com.campus.campus_life_backend.modules.payment.enums;

/**
 * 支付状态枚举
 */
public enum PaymentStatus {
    /**
     * 待支付
     */
    PENDING(0, "待支付"),
    
    /**
     * 支付成功
     */
    SUCCESS(1, "支付成功"),
    
    /**
     * 支付失败
     */
    FAILED(2, "支付失败"),
    
    /**
     * 已退款
     */
    REFUNDED(3, "已退款"),
    
    /**
     * 已取消
     */
    CANCELLED(4, "已取消");
    
    private final Integer code;
    private final String description;
    
    PaymentStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static PaymentStatus fromCode(Integer code) {
        for (PaymentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("鏈煡鐨勬敮浠樼姸鎬? " + code);
    }
}



