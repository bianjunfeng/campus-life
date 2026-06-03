package com.campus.campus_life_backend.modules.payment.service.impl;

import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelRegistry;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 支付服务实现（统一入口）
 */
@Service
@Primary
public class PaymentServiceImpl implements PaymentService {

    private final PaymentChannelRegistry channelRegistry;
    private final AlipayPaymentServiceImpl alipayPaymentService;
    private final WechatPaymentServiceImpl wechatPaymentService;

    @Autowired
    public PaymentServiceImpl(
            PaymentChannelRegistry channelRegistry,
            AlipayPaymentServiceImpl alipayPaymentService,
            WechatPaymentServiceImpl wechatPaymentService) {
        this.channelRegistry = channelRegistry;
        this.alipayPaymentService = alipayPaymentService;
        this.wechatPaymentService = wechatPaymentService;
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        PaymentMethod method = PaymentMethod.fromCode(request.getPaymentMethod());
        if (method == PaymentMethod.WALLET) {
            throw new IllegalArgumentException("钱包支付请通过统一支付入口处理");
        }
        return channelRegistry.require(method.getCode()).createChannelPayment(request);
    }

    @Override
    public boolean handlePaymentCallback(String paymentMethod, String callbackData) {
        PaymentMethod method = PaymentMethod.fromCode(paymentMethod);
        if (method == PaymentMethod.WALLET) {
            return true;
        }
        return channelRegistry.require(method.getCode())
                .handlePaymentCallback(paymentMethod, callbackData);
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

