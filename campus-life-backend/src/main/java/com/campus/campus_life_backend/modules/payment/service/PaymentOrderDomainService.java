package com.campus.campus_life_backend.modules.payment.service;

import com.campus.campus_life_backend.modules.payment.entity.PaymentCallbackLog;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentBizType;
import com.campus.campus_life_backend.modules.payment.enums.PaymentOrderStatus;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentCallbackLogMapper;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentOrderMapper;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentOrderDomainService {

    private static final DateTimeFormatter PAYMENT_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentCallbackLogMapper paymentCallbackLogMapper;

    public PaymentOrderDomainService(
            PaymentOrderMapper paymentOrderMapper,
            PaymentCallbackLogMapper paymentCallbackLogMapper
    ) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentCallbackLogMapper = paymentCallbackLogMapper;
    }

    public PaymentOrder createVoucherPaymentOrder(
            VoucherOrder order,
            Long userId,
            String channel,
            String title,
            String description,
            String idempotencyKey
    ) {
        PaymentOrder existing = paymentOrderMapper.findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing;
        }

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo(generatePaymentNo());
        paymentOrder.setBizType(PaymentBizType.VOUCHER.getCode());
        paymentOrder.setBizOrderNo(order.getOrderNo());
        paymentOrder.setUserId(userId);
        paymentOrder.setChannel(channel);
        paymentOrder.setTitle(title);
        paymentOrder.setDescription(description);
        paymentOrder.setAmount(order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount());
        paymentOrder.setRefundedAmount(BigDecimal.ZERO);
        paymentOrder.setStatus(PaymentOrderStatus.INIT.getCode());
        paymentOrder.setIdempotencyKey(idempotencyKey);
        paymentOrder.setExpireTime(order.getPayDeadline());
        paymentOrder.setVersion(0);
        paymentOrderMapper.insert(paymentOrder);
        return paymentOrder;
    }

    public boolean markWaitingForPay(PaymentOrder paymentOrder) {
        int updated = paymentOrderMapper.markWaitingForPay(
                paymentOrder.getId(),
                paymentOrder.getIdempotencyKey(),
                paymentOrder.getVersion()
        );
        if (updated > 0) {
            paymentOrder.setStatus(PaymentOrderStatus.WAIT_PAY.getCode());
            paymentOrder.setVersion(paymentOrder.getVersion() + 1);
            return true;
        }
        return false;
    }

    public PaymentOrder findLatestVoucherPaymentByBizOrderNo(String bizOrderNo) {
        return paymentOrderMapper.findLatestByBizOrderNo(PaymentBizType.VOUCHER.getCode(), bizOrderNo);
    }

    public PaymentOrder findByPaymentNo(String paymentNo) {
        return paymentOrderMapper.findByPaymentNo(paymentNo);
    }

    /**
     * 按支付单号或业务订单号解析用于渠道路由的支付单（先 paymentNo，再最新券订单支付单）。
     */
    public PaymentOrder resolveForChannelRouting(String orderOrPaymentNo) {
        if (orderOrPaymentNo == null || orderOrPaymentNo.isBlank()) {
            return null;
        }
        PaymentOrder byPaymentNo = findByPaymentNo(orderOrPaymentNo);
        if (byPaymentNo != null) {
            return byPaymentNo;
        }
        return findLatestVoucherPaymentByBizOrderNo(orderOrPaymentNo);
    }

    public BigDecimal getExpectedAmountByPaymentNo(String paymentNo) {
        PaymentOrder paymentOrder = findByPaymentNo(paymentNo);
        return paymentOrder == null ? null : paymentOrder.getAmount();
    }

    public String getBizOrderNoByPaymentNo(String paymentNo) {
        PaymentOrder paymentOrder = findByPaymentNo(paymentNo);
        return paymentOrder == null ? null : paymentOrder.getBizOrderNo();
    }

    public void saveCallbackLog(
            String channel,
            String callbackType,
            String paymentNo,
            String bizOrderNo,
            String rawBody,
            boolean signatureVerified,
            boolean amountVerified,
            String processStatus,
            String errorMessage
    ) {
        PaymentCallbackLog callbackLog = new PaymentCallbackLog();
        callbackLog.setChannel(channel);
        callbackLog.setCallbackType(callbackType);
        callbackLog.setPaymentNo(paymentNo);
        callbackLog.setBizOrderNo(bizOrderNo);
        callbackLog.setRawBody(rawBody);
        callbackLog.setSignatureVerified(signatureVerified ? 1 : 0);
        callbackLog.setAmountVerified(amountVerified ? 1 : 0);
        callbackLog.setProcessStatus(processStatus);
        callbackLog.setErrorMessage(errorMessage);
        paymentCallbackLogMapper.insert(callbackLog);
    }

    private String generatePaymentNo() {
        return "P" + LocalDateTime.now().format(PAYMENT_NO_FORMATTER)
                + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
