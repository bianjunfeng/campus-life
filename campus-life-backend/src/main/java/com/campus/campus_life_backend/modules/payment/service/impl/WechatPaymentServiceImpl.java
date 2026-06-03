package com.campus.campus_life_backend.modules.payment.service.impl;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.service.PaymentCallbackService;
import com.campus.campus_life_backend.modules.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;



/**
 * 微信支付服务实现
 */
@Service
public class WechatPaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(WechatPaymentServiceImpl.class);

    @Value("${payment.wechat.app-id:}")
    private String appId;

    @Value("${payment.wechat.mch-id:}")
    private String mchId;

    @Value("${payment.wechat.api-v3-key:}")
    private String apiV3Key;

    @Value("${payment.wechat.private-key-path:}")
    private String privateKeyPath;

    @Value("${payment.wechat.certificate-serial-number:}")
    private String certificateSerialNumber;

    @Value("${payment.wechat.notify-url:http://localhost:8080/api/payment/wechat/notify}")
    private String notifyUrl;

    @Value("${payment.wechat.enabled:false}")
    private boolean enabled;

    private final PaymentCallbackService paymentCallbackService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public WechatPaymentServiceImpl(PaymentCallbackService paymentCallbackService) {
        this.paymentCallbackService = paymentCallbackService;
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        if (!enabled) {
            throw new BusinessException(BusinessErrorCode.WECHAT_PAYMENT_DISABLED);
        }

        // TODO: 实现微信支付统一下单
        // 1. 调用微信支付统一下单API
        // 2. 生成支付二维码或支付URL
        // 3. 返回支付响应

        logger.info("创建微信支付订单: 订单号={}, 金额={}", request.getOrderNo(), request.getAmount());

        PaymentResponse response = new PaymentResponse();
        response.setPaymentMethod("wechat");
        response.setPaymentOrderNo(request.getOrderNo());
        // TODO: 实际实现中需要调用微信支付API获取真实的支付URL或二维码
        response.setQrCode("wechat://pay?orderNo=" + request.getOrderNo());

        return response;
    }

    @Override
    public boolean handlePaymentCallback(String paymentMethod, String callbackData) {
        logger.info("处理微信支付回调: {}", callbackData);

        if (!enabled) {
            logger.warn("微信支付未启用，拒绝处理回调");
            return false;
        }

        try {
            // 解析回调数据（JSON格式）
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) objectMapper.readValue(callbackData, Map.class);

            // 安全优先：未接入官方验签前拒绝处理回调，防止伪造支付成功
            logger.warn("微信支付回调验签未实现，拒绝处理回调。订单号={}", data.get("out_trade_no"));
            return false;
        } catch (Exception e) {
            logger.error("处理微信支付回调异常", e);
            return false;
        }
    }

    @Override
    public String queryPaymentStatus(String orderNo) {
        if (!enabled) {
            return "UNKNOWN";
        }

        // TODO: 实现微信支付订单查询
        logger.info("查询微信支付状态: 订单号={}", orderNo);
        return "UNKNOWN";
    }

    @Override
    public boolean refund(String orderNo, BigDecimal refundAmount, String refundReason) {
        if (!enabled) {
            return false;
        }

        // TODO: 实现微信支付退款
        logger.info("申请微信支付退款: 订单号={}, 退款金额={}", orderNo, refundAmount);
        return false;
    }
}
