package com.campus.campus_life_backend.modules.payment.mapper;

import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentOrderMapper {

    int insert(PaymentOrder paymentOrder);

    PaymentOrder findById(@Param("id") Long id);

    PaymentOrder findByPaymentNo(@Param("paymentNo") String paymentNo);

    PaymentOrder findLatestByBizOrderNo(@Param("bizType") String bizType, @Param("bizOrderNo") String bizOrderNo);

    PaymentOrder findLatestByBizOrderNoAndChannel(
            @Param("bizType") String bizType,
            @Param("bizOrderNo") String bizOrderNo,
            @Param("channel") String channel
    );

    PaymentOrder findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    List<PaymentOrder> findExpiredWaitingOrders(@Param("now") LocalDateTime now, @Param("limit") Integer limit);

    int markWaitingForPay(
            @Param("id") Long id,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("expectedVersion") Integer expectedVersion
    );

    int markSuccessByPaymentNo(
            @Param("paymentNo") String paymentNo,
            @Param("channelTradeNo") String channelTradeNo,
            @Param("successTime") LocalDateTime successTime,
            @Param("lastCallbackTime") LocalDateTime lastCallbackTime,
            @Param("expectedVersion") Integer expectedVersion
    );

    int markFailedByPaymentNo(
            @Param("paymentNo") String paymentNo,
            @Param("lastCallbackTime") LocalDateTime lastCallbackTime,
            @Param("expectedVersion") Integer expectedVersion
    );

    int closeWaitingOrder(
            @Param("paymentNo") String paymentNo,
            @Param("expectedVersion") Integer expectedVersion
    );

    int markRefunded(
            @Param("paymentNo") String paymentNo,
            @Param("refundedAmount") BigDecimal refundedAmount,
            @Param("status") String status
    );
}
