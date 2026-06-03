package com.campus.campus_life_backend.modules.payment.dto;

import lombok.Data;

/**
 * 鏀粯璇锋眰 DTO
 */
@Data
public class PaymentRequest {
    /**
     * 璁㈠崟鍙?
     */
    private String orderNo;
    
    /**
     * 鏀粯鏂瑰紡锛歛lipay(鏀粯瀹?銆亀echat(寰俊鏀粯)
     */
    private String paymentMethod;
    
    /**
     * 鏀粯閲戦
     */
    private java.math.BigDecimal amount;
    
    /**
     * 璁㈠崟鏍囬
     */
    private String subject;
    
    /**
     * 璁㈠崟鎻忚堪
     */
    private String description;

    /**
     * 支付宝透传参数（用于支付完成后回跳前端页面上下文）
     * 例如: returnTo=detail&couponId=12
     */
    private String passbackParams;
}


