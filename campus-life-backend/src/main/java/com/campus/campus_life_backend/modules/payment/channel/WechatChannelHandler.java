package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.payment.dto.PaymentCallbackResult;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import com.campus.campus_life_backend.modules.payment.service.PaymentCallbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 微信支付渠道（未接入官方 SDK，按配置显式降级）。
 *
 * <p>能力矩阵（{@code payment.wechat.enabled}）：
 * <pre>
 * | 能力                    | enabled=false     | enabled=true（当前）      |
 * |-------------------------|-------------------|---------------------------|
 * | createChannelPayment    | WECHAT_DISABLED   | NOT_INTEGRATED 抛业务异常 |
 * | handlePaymentCallback   | 拒绝 + 日志       | 验签未实现，拒绝 + 503604 |
 * | queryPaymentStatus      | WECHAT_DISABLED   | NOT_INTEGRATED 抛业务异常 |
 * | refund                  | WECHAT_DISABLED   | NOT_INTEGRATED 抛业务异常 |
 * </pre>
 */
@Component
public class WechatChannelHandler implements PaymentChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(WechatChannelHandler.class);

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
    public WechatChannelHandler(PaymentCallbackService paymentCallbackService) {
        this.paymentCallbackService = paymentCallbackService;
    }

    @Override
    public String channelCode() {
        return PaymentMethod.WECHAT.getCode();
    }

    @Override
    public PaymentResponse createChannelPayment(PaymentRequest request) {
        ensureWechatEnabled();
        throw notIntegratedException();
    }

    @Override
    public PaymentCallbackResult handlePaymentCallback(String paymentMethod, String callbackData) {
        logger.info("处理微信支付回调: paymentMethod={}", paymentMethod);

        if (!enabled) {
            logger.warn(
                    "微信支付未启用，拒绝处理回调: errorCode={} ({})",
                    BusinessErrorCode.WECHAT_PAYMENT_DISABLED.getCode(),
                    BusinessErrorCode.WECHAT_PAYMENT_DISABLED.getMessage()
            );
            return PaymentCallbackResult.channelRejected(
                    String.valueOf(BusinessErrorCode.WECHAT_PAYMENT_DISABLED.getCode())
            );
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) objectMapper.readValue(callbackData, Map.class);
            Object outTradeNo = data.get("out_trade_no");
            logger.warn(
                    "微信支付回调验签未实现，拒绝处理: errorCode={} ({}), out_trade_no={}",
                    BusinessErrorCode.WECHAT_PAYMENT_CALLBACK_VERIFY_NOT_IMPLEMENTED.getCode(),
                    BusinessErrorCode.WECHAT_PAYMENT_CALLBACK_VERIFY_NOT_IMPLEMENTED.getMessage(),
                    outTradeNo
            );
            return PaymentCallbackResult.channelRejected(
                    String.valueOf(BusinessErrorCode.WECHAT_PAYMENT_CALLBACK_VERIFY_NOT_IMPLEMENTED.getCode())
            );
        } catch (Exception e) {
            logger.error(
                    "微信支付回调解析失败，拒绝处理: errorCode={}",
                    BusinessErrorCode.WECHAT_PAYMENT_CALLBACK_VERIFY_NOT_IMPLEMENTED.getCode(),
                    e
            );
            return PaymentCallbackResult.channelRejected(
                    String.valueOf(BusinessErrorCode.WECHAT_PAYMENT_CALLBACK_VERIFY_NOT_IMPLEMENTED.getCode())
            );
        }
    }

    @Override
    public String queryPaymentStatus(String orderNo) {
        ensureWechatEnabled();
        logger.info("查询微信支付状态（未接入）: 订单号={}", orderNo);
        throw notIntegratedException();
    }

    @Override
    public boolean refund(String orderNo, BigDecimal refundAmount, String refundReason) {
        ensureWechatEnabled();
        logger.info(
                "申请微信支付退款（未接入）: 订单号={}, 退款金额={}, 原因={}",
                orderNo,
                refundAmount,
                refundReason
        );
        throw notIntegratedException();
    }

    private void ensureWechatEnabled() {
        if (!enabled) {
            throw new BusinessException(BusinessErrorCode.WECHAT_PAYMENT_DISABLED);
        }
    }

    private static BusinessException notIntegratedException() {
        return new BusinessException(BusinessErrorCode.WECHAT_PAYMENT_NOT_INTEGRATED);
    }
}
