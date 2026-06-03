package com.campus.campus_life_backend.modules.payment.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.order.service.OrderFacadeService;
import com.campus.campus_life_backend.modules.payment.dto.PaymentCallbackResult;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRefundRequest;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRefundResponse;
import com.campus.campus_life_backend.modules.payment.dto.PaymentResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.service.PaymentCreateOrchestrator;
import com.campus.campus_life_backend.modules.payment.service.PaymentIdempotencyService;
import com.campus.campus_life_backend.modules.payment.service.PaymentIdempotencyService.IdempotencyLock;
import com.campus.campus_life_backend.modules.payment.service.PaymentOrderDomainService;
import com.campus.campus_life_backend.modules.payment.service.PaymentService;
import com.campus.campus_life_backend.modules.payment.service.PaymentRefundWorkflowService;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final CurrentUserAccessor currentUserAccessor;
    private final PaymentService paymentService;
    private final PaymentCreateOrchestrator paymentCreateOrchestrator;
    private final PaymentIdempotencyService paymentIdempotencyService;
    private final PaymentOrderDomainService paymentOrderDomainService;
    private final PaymentRefundWorkflowService paymentRefundWorkflowService;
    private final OrderFacadeService orderFacadeService;

    @Value("${payment.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public PaymentController(
            CurrentUserAccessor currentUserAccessor,
            PaymentService paymentService,
            PaymentCreateOrchestrator paymentCreateOrchestrator,
            PaymentIdempotencyService paymentIdempotencyService,
            PaymentOrderDomainService paymentOrderDomainService,
            PaymentRefundWorkflowService paymentRefundWorkflowService,
            OrderFacadeService orderFacadeService
    ) {
        this.currentUserAccessor = currentUserAccessor;
        this.paymentService = paymentService;
        this.paymentCreateOrchestrator = paymentCreateOrchestrator;
        this.paymentIdempotencyService = paymentIdempotencyService;
        this.paymentOrderDomainService = paymentOrderDomainService;
        this.paymentRefundWorkflowService = paymentRefundWorkflowService;
        this.orderFacadeService = orderFacadeService;
    }

    @PostMapping("/create")
    @RequireLogin
    public ApiResponse<PaymentResponse> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody PaymentRequest request
    ) {
        Long userId = currentUserAccessor.requireUserId();

        VoucherOrder order = orderFacadeService.findOwnedOrderByOrderNo(request.getOrderNo(), userId);
        if (order == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        if (!isOrderPayable(order)) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "订单当前状态不可支付");
        }

        Map<String, Object> paymentOrderInfo = orderFacadeService.buildPaymentOrderInfo(order);
        if (request.getSubject() == null || request.getSubject().isEmpty()) {
            request.setSubject(String.valueOf(paymentOrderInfo.getOrDefault("subject", "优惠券订单")));
        }
        if (request.getDescription() == null || request.getDescription().isEmpty()) {
            request.setDescription(String.valueOf(paymentOrderInfo.getOrDefault("description", "优惠券购买")));
        }

        BigDecimal orderAmount = normalizeAmount(order.getPayAmount());
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(BusinessErrorCode.ORDER_AMOUNT_INVALID);
        }
        request.setAmount(orderAmount);

        String createKey = paymentIdempotencyService.resolveCreateKey(
                idempotencyKey,
                order.getOrderNo(),
                userId,
                request.getPaymentMethod()
        );
        Optional<IdempotencyLock> idempotencyLock = paymentIdempotencyService.tryAcquireCreate(
                createKey,
                PaymentIdempotencyService.CREATE_PROCESSING_TTL
        );
        if (idempotencyLock.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "支付请求处理中，请勿重复提交");
        }
        orderFacadeService.bindPaymentIdempotencyKeyIfPending(order.getId(), createKey);

        try {
            PaymentResponse response = paymentCreateOrchestrator.create(order, userId, request, createKey);
            paymentIdempotencyService.completeCreate(idempotencyLock.get());
            return ApiResponse.success(response);
        } catch (BusinessException e) {
            paymentIdempotencyService.releaseCreate(idempotencyLock.get());
            throw e;
        } catch (Exception e) {
            paymentIdempotencyService.releaseCreate(idempotencyLock.get());
            logger.error("创建支付订单失败", e);
            throw new BusinessException(BusinessErrorCode.PAYMENT_CREATE_FAILED, "创建支付订单失败，请稍后重试", e);
        }
    }

    @GetMapping("/status/{orderNo}")
    @RequireLogin
    public ApiResponse<Map<String, Object>> queryPaymentStatus(@PathVariable String orderNo) {
        Long userId = currentUserAccessor.requireUserId();

        VoucherOrder order = orderFacadeService.findOwnedOrderByOrderNo(orderNo, userId);
        if (order == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }

        try {
            String status;
            PaymentOrder paymentOrder = paymentOrderDomainService.findLatestVoucherPaymentByBizOrderNo(orderNo);
            if (paymentOrder != null) {
                status = mapPaymentStatus(paymentOrder.getStatus());
                if ("PENDING".equals(status)) {
                    String channelStatus = paymentService.queryPaymentStatus(paymentOrder.getPaymentNo());
                    if (!"UNKNOWN".equalsIgnoreCase(channelStatus)) {
                        status = normalizeChannelPaymentStatus(channelStatus);
                    }
                }
            } else {
                status = normalizeOrderPaymentStatus(order.getPaymentStatus());
            }
            status = reconcilePaymentStatus(status, order.getStatus(), order.getPaymentStatus());

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", orderNo);
            result.put("paymentStatus", status);
            result.put("orderStatus", order.getStatus());
            return ApiResponse.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("查询支付状态失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "查询支付状态失败，请稍后重试", e);
        }
    }

    @GetMapping("/order/{orderNo}")
    @RequireLogin
    public ApiResponse<Map<String, Object>> getPaymentOrderInfo(@PathVariable String orderNo) {
        Long userId = currentUserAccessor.requireUserId();

        VoucherOrder order = orderFacadeService.findOwnedOrderByOrderNo(orderNo, userId);
        if (order == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        return ApiResponse.success(orderFacadeService.buildPaymentOrderInfo(order));
    }

    @PostMapping("/refund")
    @RequireLogin
    public ApiResponse<PaymentRefundResponse> refundPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody PaymentRefundRequest request
    ) {
        Long userId = currentUserAccessor.requireUserId();
        if (request == null || request.getOrderNo() == null || request.getOrderNo().isBlank()) {
            throw new BusinessException(BusinessErrorCode.ORDER_NO_REQUIRED);
        }
        if (request.getRefundAmount() == null || request.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "退款金额必须大于 0");
        }

        VoucherOrder order = orderFacadeService.findOwnedOrderByOrderNo(request.getOrderNo(), userId);
        if (order == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "仅未核销订单支持退款");
        }

        PaymentOrder paymentOrder = paymentOrderDomainService.findLatestVoucherPaymentByBizOrderNo(order.getOrderNo());
        if (paymentOrder == null || paymentOrder.getPaymentNo() == null) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_RECORD_NOT_FOUND, "未找到可退款支付单");
        }
        if (!"SUCCESS".equalsIgnoreCase(paymentOrder.getStatus())
                && !"PARTIAL_REFUNDED".equalsIgnoreCase(paymentOrder.getStatus())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_STATUS_CHANGED, "当前支付状态不可退款");
        }

        String refundKey = paymentIdempotencyService.resolveRefundKey(idempotencyKey, paymentOrder.getPaymentNo(), userId);
        Optional<IdempotencyLock> idempotencyLock = paymentIdempotencyService.tryAcquireRefund(
                refundKey,
                PaymentIdempotencyService.REFUND_PROCESSING_TTL
        );
        if (idempotencyLock.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.CONFLICT, "退款请求处理中，请勿重复提交");
        }

        try {
            PaymentRefundResponse refundResponse = paymentRefundWorkflowService.submitVoucherRefundApplication(
                    paymentOrder,
                    order,
                    userId,
                    request.getRefundAmount().stripTrailingZeros(),
                    request.getReason(),
                    refundKey
            );
            paymentIdempotencyService.completeRefund(idempotencyLock.get());
            return ApiResponse.success(refundResponse);
        } catch (BusinessException e) {
            paymentIdempotencyService.releaseRefund(idempotencyLock.get());
            throw e;
        } catch (IllegalArgumentException e) {
            paymentIdempotencyService.releaseRefund(idempotencyLock.get());
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        } catch (Exception e) {
            paymentIdempotencyService.releaseRefund(idempotencyLock.get());
            logger.error("提交退款申请失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "提交退款申请失败，请稍后重试", e);
        }
    }

    @GetMapping("/refunds")
    @RequireLogin
    public ApiResponse<?> listMyRefunds() {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(paymentRefundWorkflowService.listUserRefunds(userId));
    }

    @GetMapping("/refunds/{refundNo}")
    @RequireLogin
    public ApiResponse<?> getMyRefundDetail(@PathVariable String refundNo) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(paymentRefundWorkflowService.getUserRefundDetail(userId, refundNo));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    @PostMapping("/alipay/notify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        logger.info("收到支付宝回调: {}", params);

        String paymentNo = params.get("out_trade_no");
        String bizOrderNo = paymentOrderDomainService.getBizOrderNoByPaymentNo(paymentNo);
        String callbackData = buildCallbackData(params);

        try {
            PaymentCallbackResult result = paymentService.handlePaymentCallback("alipay", callbackData);
            saveAlipayCallbackLog(
                    paymentNo,
                    bizOrderNo,
                    callbackData,
                    result.isSignatureVerified(),
                    result.isAmountVerified(),
                    result.isSuccess() ? "PROCESSED" : "FAILED",
                    result.getErrorCode()
            );
            return result.isSuccess() ? "success" : "fail";
        } catch (Exception e) {
            logger.error("处理支付宝回调失败", e);
            saveAlipayCallbackLog(paymentNo, bizOrderNo, callbackData, true, true, "FAILED", e.getMessage());
            return "fail";
        }
    }

    @GetMapping("/alipay/return")
    public ResponseEntity<Void> alipayReturn(@RequestParam Map<String, String> params) {
        logger.info("支付宝支付返回: {}", params);
        String paymentNo = params.getOrDefault("out_trade_no", "");
        String orderNo = paymentOrderDomainService.getBizOrderNoByPaymentNo(paymentNo);
        StringBuilder target = new StringBuilder(frontendBaseUrl)
                .append("/payment/success?orderNo=").append(orderNo == null ? paymentNo : orderNo);
        appendPassbackQuery(target, params.get("passback_params"));
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(target.toString()).toString())
                .build();
    }

    @PostMapping("/wechat/notify")
    public String wechatNotify(@RequestBody String body) {
        logger.info("收到微信支付回调: {}", body);

        try {
            PaymentCallbackResult result = paymentService.handlePaymentCallback("wechat", body);
            return result.isSuccess() ? "success" : "fail";
        } catch (Exception e) {
            logger.error("处理微信支付回调失败", e);
            return "fail";
        }
    }

    private String buildCallbackData(Map<String, String> params) {
        StringBuilder callbackData = new StringBuilder();
        params.forEach((key, value) -> {
            if (callbackData.length() > 0) {
                callbackData.append("&");
            }
            callbackData.append(key).append("=").append(value);
        });
        return callbackData.toString();
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? null : amount.stripTrailingZeros();
    }

    private void saveAlipayCallbackLog(
            String paymentNo,
            String bizOrderNo,
            String rawBody,
            boolean signatureVerified,
            boolean amountVerified,
            String processStatus,
            String errorMessage
    ) {
        paymentOrderDomainService.saveCallbackLog(
                "alipay",
                "PAY_NOTIFY",
                paymentNo,
                bizOrderNo,
                rawBody,
                signatureVerified,
                amountVerified,
                processStatus,
                errorMessage
        );
    }

    private void appendPassbackQuery(StringBuilder target, String rawPassbackParams) {
        if (rawPassbackParams == null || rawPassbackParams.isBlank()) {
            return;
        }
        String decoded = URLDecoder.decode(rawPassbackParams, StandardCharsets.UTF_8);
        for (String pair : decoded.split("&")) {
            if (pair == null || pair.isBlank() || !pair.contains("=")) {
                continue;
            }
            String[] kv = pair.split("=", 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            if ("returnTo".equals(key)
                    || "couponId".equals(key)
                    || "orderTab".equals(key)
                    || "paymentMethod".equals(key)) {
                target.append("&").append(key).append("=").append(value);
            }
        }
    }

    private boolean isOrderPayable(VoucherOrder order) {
        if (order == null) {
            return false;
        }
        Integer status = order.getStatus();
        Integer paymentStatus = order.getPaymentStatus();
        if (status == null || paymentStatus == null) {
            return false;
        }
        if (status != 0 || paymentStatus != 0) {
            return false;
        }
        LocalDateTime payDeadline = order.getPayDeadline();
        return payDeadline == null || payDeadline.isAfter(LocalDateTime.now());
    }

    private String normalizeOrderPaymentStatus(Integer paymentStatus) {
        if (paymentStatus == null) {
            return "PENDING";
        }
        if (paymentStatus == 1) {
            return "SUCCESS";
        }
        if (paymentStatus == 3) {
            return "FULL_REFUNDED";
        }
        if (paymentStatus == 4) {
            return "CLOSED";
        }
        if (paymentStatus == 2) {
            return "FAILED";
        }
        return "PENDING";
    }

    private String reconcilePaymentStatus(String status, Integer orderStatus, Integer paymentStatus) {
        if (orderStatus != null) {
            if (orderStatus == 4) {
                return "FULL_REFUNDED";
            }
            if (orderStatus == 1 || orderStatus == 2) {
                return "SUCCESS";
            }
            if ((orderStatus == 3 || orderStatus == 5) && !"SUCCESS".equalsIgnoreCase(status)) {
                return "CLOSED";
            }
        }
        if (paymentStatus != null) {
            if (paymentStatus == 3) {
                return "FULL_REFUNDED";
            }
            if (paymentStatus == 1) {
                return "SUCCESS";
            }
            if (paymentStatus == 2) {
                return "FAILED";
            }
            if (paymentStatus == 4) {
                return "CLOSED";
            }
        }
        return status;
    }

    private String mapPaymentStatus(String paymentOrderStatus) {
        if (paymentOrderStatus == null || paymentOrderStatus.isBlank()) {
            return "PENDING";
        }
        if ("SUCCESS".equalsIgnoreCase(paymentOrderStatus)
                || "PARTIAL_REFUNDED".equalsIgnoreCase(paymentOrderStatus)
                || "FULL_REFUNDED".equalsIgnoreCase(paymentOrderStatus)) {
            return "SUCCESS";
        }
        if ("FAILED".equalsIgnoreCase(paymentOrderStatus)
                || "CLOSED".equalsIgnoreCase(paymentOrderStatus)) {
            return "FAILED";
        }
        return "PENDING";
    }

    private String normalizeChannelPaymentStatus(String channelStatus) {
        if (channelStatus == null || channelStatus.isBlank()) {
            return "PENDING";
        }
        String status = channelStatus.toUpperCase();
        if ("TRADE_SUCCESS".equals(status)
                || "TRADE_FINISHED".equals(status)
                || "SUCCESS".equals(status)) {
            return "SUCCESS";
        }
        if ("TRADE_CLOSED".equals(status) || "FAILED".equals(status)) {
            return "FAILED";
        }
        return "PENDING";
    }
}
