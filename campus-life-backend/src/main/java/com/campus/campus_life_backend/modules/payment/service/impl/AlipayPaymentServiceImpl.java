package com.campus.campus_life_backend.modules.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.payment.channel.PaymentChannelHandler;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentCallbackService;
import com.campus.campus_life_backend.modules.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


/**
 * 支付宝支付服务实现
 */
@Service
public class AlipayPaymentServiceImpl implements PaymentService, PaymentChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(AlipayPaymentServiceImpl.class);

    @Value("${payment.alipay.app-id:}")
    private String appId;

    @Value("${payment.alipay.private-key:}")
    private String privateKey;

    @Value("${payment.alipay.public-key:}")
    private String alipayPublicKey;

    @Value("${payment.alipay.gateway-url:https://openapi.alipaydev.com/gateway.do}")
    private String gatewayUrl;

    @Value("${payment.alipay.notify-url:http://localhost:8080/api/payment/alipay/notify}")
    private String notifyUrl;

    @Value("${payment.alipay.return-url:http://localhost:8080/api/payment/alipay/return}")
    private String returnUrl;

    @Value("${payment.alipay.enabled:false}")
    private boolean enabled;

    private final PaymentCallbackService paymentCallbackService;

    @Autowired
    public AlipayPaymentServiceImpl(PaymentCallbackService paymentCallbackService) {
        this.paymentCallbackService = paymentCallbackService;
    }

    /**
     * 获取支付宝客户端
     */
    private AlipayClient getAlipayClient() {
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                privateKey,
                "JSON",
                "UTF-8",
                alipayPublicKey,
                "RSA2"
        );
    }

    @Override
    public String channelCode() {
        return PaymentMethod.ALIPAY.getCode();
    }

    @Override
    public PaymentResponse createChannelPayment(PaymentRequest request) {
        return createPayment(request);
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        if (!enabled) {
            throw new BusinessException(BusinessErrorCode.ALIPAY_PAYMENT_DISABLED);
        }

        try {
            AlipayClient alipayClient = getAlipayClient();
            AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();

            // 设置回调地址
            alipayRequest.setNotifyUrl(notifyUrl);
            alipayRequest.setReturnUrl(returnUrl);

            // 设置业务参数
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(request.getOrderNo());
            model.setTotalAmount(formatAmount(request.getAmount()));
            model.setSubject(sanitizeSubject(request.getSubject()));
            model.setBody(sanitizeBody(request.getDescription()));
            model.setProductCode("FAST_INSTANT_TRADE_PAY"); // 固定值
            if (request.getPassbackParams() != null && !request.getPassbackParams().isBlank()) {
                model.setPassbackParams(encodePassbackParams(request.getPassbackParams()));
            }

            alipayRequest.setBizModel(model);

            // 执行请求
            AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);

            if (response.isSuccess()) {
                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setPaymentMethod("alipay");
                paymentResponse.setPaymentOrderNo(request.getOrderNo());
                paymentResponse.setPayForm(response.getBody()); // 支付宝返回的是HTML表单
                return paymentResponse;
            } else {
                logger.error("支付宝创建支付失败: {}", response.getSubMsg());
                throw new BusinessException(BusinessErrorCode.PAYMENT_CREATE_FAILED);
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝支付异常", e);
            throw new BusinessException(BusinessErrorCode.PAYMENT_CREATE_FAILED, e);
        }
    }

    @Override
    public boolean handlePaymentCallback(String paymentMethod, String callbackData) {
        logger.info("处理支付宝回调: {}", callbackData);

        try {
            if (!"alipay".equalsIgnoreCase(paymentMethod)) {
                logger.warn("支付宝回调收到非 alipay 支付方式标识: {}", paymentMethod);
                return false;
            }
            // 解析回调参数
            Map<String, String> params = parseCallbackData(callbackData);
            // 验签与金额一致性校验统一在 PaymentController#alipayNotify 完成，
            // 服务层只处理"已通过校验"的业务状态流转，避免职责分散。

            String paymentNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String tradeNo = params.get("trade_no");

            if (paymentNo == null || paymentNo.isEmpty()) {
                logger.warn("支付宝回调中缺少支付单号");
                return false;
            }

            // 处理支付结果
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 支付成功
                return paymentCallbackService.handlePaymentSuccess(paymentNo, "alipay", tradeNo);
            } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                // 交易关闭（支付失败或超时）
                return paymentCallbackService.handlePaymentFailure(paymentNo);
            }

            logger.warn("未知的支付状态: {}", tradeStatus);
            return false;
        } catch (Exception e) {
            logger.error("处理支付宝回调异常", e);
            return false;
        }
    }

    /**
     * 解析回调数据
     */
    private Map<String, String> parseCallbackData(String callbackData) {
        Map<String, String> params = new HashMap<>();
        if (callbackData == null || callbackData.isEmpty()) {
            return params;
        }

        String[] pairs = callbackData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    @Override
    public String queryPaymentStatus(String orderNo) {
        if (!enabled) {
            return "UNKNOWN";
        }

        try {
            AlipayClient alipayClient = getAlipayClient();
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

            com.alipay.api.domain.AlipayTradeQueryModel model = new com.alipay.api.domain.AlipayTradeQueryModel();
            model.setOutTradeNo(orderNo);
            request.setBizModel(model);

            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                // TRADE_SUCCESS: 交易成功
                // WAIT_BUYER_PAY: 交易创建，等待买家付款
                // TRADE_CLOSED: 未付款交易超时关闭，或支付完成后全额退款
                return tradeStatus;
            } else {
                logger.error("查询支付状态失败: {}", response.getSubMsg());
                return "UNKNOWN";
            }
        } catch (AlipayApiException e) {
            logger.error("查询支付状态异常", e);
            return "UNKNOWN";
        }
    }

    @Override
    public boolean refund(String orderNo, BigDecimal refundAmount, String refundReason) {
        if (!enabled) {
            return false;
        }

        try {
            AlipayClient alipayClient = getAlipayClient();
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

            com.alipay.api.domain.AlipayTradeRefundModel model = new com.alipay.api.domain.AlipayTradeRefundModel();
            model.setOutTradeNo(orderNo);
            model.setRefundAmount(refundAmount.toString());
            model.setRefundReason(refundReason);

            request.setBizModel(model);

            AlipayTradeRefundResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                logger.info("退款成功: 订单号={}, 退款金额={}", orderNo, refundAmount);
                return true;
            } else {
                logger.error("退款失败: {}", response.getSubMsg());
                return false;
            }
        } catch (AlipayApiException e) {
            logger.error("退款异常", e);
            return false;
        }
    }

    static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static String sanitizeSubject(String subject) {
        String normalized = sanitizeText(subject, 128);
        return normalized.isBlank() ? "优惠券订单" : normalized;
    }

    static String sanitizeBody(String body) {
        String normalized = sanitizeText(body, 256);
        return normalized.isBlank() ? "优惠券购买" : normalized;
    }

    static String encodePassbackParams(String passbackParams) {
        if (passbackParams == null || passbackParams.isBlank()) {
            return null;
        }
        return URLEncoder.encode(passbackParams, StandardCharsets.UTF_8);
    }

    private static String sanitizeText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        text.trim().codePoints().forEach(codePoint -> {
            if (builder.length() >= maxLength) {
                return;
            }
            if (Character.isISOControl(codePoint) || Character.isSurrogate((char) codePoint)) {
                return;
            }
            if (!Character.isValidCodePoint(codePoint) || Character.charCount(codePoint) > 1) {
                return;
            }
            builder.append((char) codePoint);
        });
        return builder.toString().replaceAll("\\s+", " ").trim();
    }
}
