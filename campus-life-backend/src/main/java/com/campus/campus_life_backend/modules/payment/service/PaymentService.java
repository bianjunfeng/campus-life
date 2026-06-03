package com.campus.campus_life_backend.modules.payment.service;

import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;

/**
 * 鏀粯鏈嶅姟鎺ュ彛
 */
public interface PaymentService {
    
    /**
     * 鍒涘缓鏀粯璁㈠崟
     * @param request 鏀粯璇锋眰
     * @return 鏀粯鍝嶅簲锛堝寘鍚敮浠楿RL鎴栦簩缁寸爜锛?
     */
    PaymentResponse createPayment(PaymentRequest request);
    
    /**
     * 澶勭悊鏀粯鍥炶皟
     * @param paymentMethod 鏀粯鏂瑰紡
     * @param callbackData 鍥炶皟鏁版嵁
     * @return 鏄惁澶勭悊鎴愬姛
     */
    boolean handlePaymentCallback(String paymentMethod, String callbackData);
    
    /**
     * 鏌ヨ鏀粯鐘舵€?
     * @param orderNo 璁㈠崟鍙?
     * @return 鏀粯鐘舵€?
     */
    String queryPaymentStatus(String orderNo);
    
    /**
     * 鐢宠閫€娆?
     * @param orderNo 璁㈠崟鍙?
     * @param refundAmount 閫€娆鹃噾棰?
     * @param refundReason 閫€娆惧師鍥?
     * @return 鏄惁鎴愬姛
     */
    boolean refund(String orderNo, java.math.BigDecimal refundAmount, String refundReason);
}



