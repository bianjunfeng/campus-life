package com.campus.campus_life_backend.modules.payment.service.impl;

import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 鏀粯鏈嶅姟瀹炵幇锛堢粺涓€鍏ュ彛锛?
 */
@Service
@Primary
public class PaymentServiceImpl implements PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    
    private final AlipayPaymentServiceImpl alipayPaymentService;
    private final WechatPaymentServiceImpl wechatPaymentService;
    
    @Autowired
    public PaymentServiceImpl(
            AlipayPaymentServiceImpl alipayPaymentService,
            WechatPaymentServiceImpl wechatPaymentService) {
        this.alipayPaymentService = alipayPaymentService;
        this.wechatPaymentService = wechatPaymentService;
    }
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        PaymentMethod method = PaymentMethod.fromCode(request.getPaymentMethod());
        
        switch (method) {
            case ALIPAY:
                return alipayPaymentService.createPayment(request);
            case WECHAT:
                return wechatPaymentService.createPayment(request);
            case WALLET:
                throw new IllegalArgumentException("钱包支付请通过统一支付入口处理");
            default:
                throw new IllegalArgumentException("涓嶆敮鎸佺殑鏀粯鏂瑰紡: " + request.getPaymentMethod());
        }
    }
    
    @Override
    public boolean handlePaymentCallback(String paymentMethod, String callbackData) {
        PaymentMethod method = PaymentMethod.fromCode(paymentMethod);
        
        switch (method) {
            case ALIPAY:
                return alipayPaymentService.handlePaymentCallback(paymentMethod, callbackData);
            case WECHAT:
                return wechatPaymentService.handlePaymentCallback(paymentMethod, callbackData);
            case WALLET:
                return true;
            default:
                logger.warn("鏈煡鐨勬敮浠樻柟寮忓洖璋? {}", paymentMethod);
                return false;
        }
    }
    
    @Override
    public String queryPaymentStatus(String orderNo) {
        // 杩欓噷闇€瑕佹牴鎹鍗曞彿鏌ヨ璁㈠崟锛岃幏鍙栨敮浠樻柟寮?
        // 鏆傛椂鍏堝皾璇曚袱绉嶆柟寮忔煡璇?
        String alipayStatus = alipayPaymentService.queryPaymentStatus(orderNo);
        if (!"UNKNOWN".equals(alipayStatus)) {
            return alipayStatus;
        }
        
        String wechatStatus = wechatPaymentService.queryPaymentStatus(orderNo);
        return wechatStatus;
    }
    
    @Override
    public boolean refund(String orderNo, BigDecimal refundAmount, String refundReason) {
        // 杩欓噷闇€瑕佹牴鎹鍗曞彿鏌ヨ璁㈠崟锛岃幏鍙栨敮浠樻柟寮?
        // 鏆傛椂鍏堝皾璇曚袱绉嶆柟寮忛€€娆?
        if (alipayPaymentService.refund(orderNo, refundAmount, refundReason)) {
            return true;
        }
        
        return wechatPaymentService.refund(orderNo, refundAmount, refundReason);
    }
}

