package com.campus.campus_life_backend.modules.payment.mapper;

import com.campus.campus_life_backend.modules.payment.entity.PaymentRefundOrder;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PaymentRefundOrderMapper {

    int insert(PaymentRefundOrder refundOrder);

    PaymentRefundOrder findById(@Param("id") Long id);

    PaymentRefundOrder findByRefundNo(@Param("refundNo") String refundNo);

    PaymentRefundOrder findByRequestIdempotencyKey(@Param("requestIdempotencyKey") String requestIdempotencyKey);

    PaymentRefundOrder findLatestByBizOrderNo(@Param("bizOrderNo") String bizOrderNo);

    List<PaymentRefundOrder> findByPaymentNo(@Param("paymentNo") String paymentNo);

    List<Map<String, Object>> findUserRefundViews(@Param("userId") Long userId);

    List<Map<String, Object>> findMerchantRefundViews(
            @Param("merchantId") Long merchantId,
            @Param("status") String status
    );

    List<Map<String, Object>> findAdminRefundViews(@Param("status") String status);

    List<PaymentRefundOrder> findOverdueMerchantReviewOrders(
            @Param("now") LocalDateTime now,
            @Param("limit") Integer limit
    );

    BigDecimal sumSuccessfulRefundAmountByPaymentNo(@Param("paymentNo") String paymentNo);

    int markSuccess(
            @Param("refundNo") String refundNo,
            @Param("channelRefundNo") String channelRefundNo,
            @Param("successTime") LocalDateTime successTime,
            @Param("expectedStatus") String expectedStatus
    );

    int markFailed(
            @Param("refundNo") String refundNo,
            @Param("expectedStatus") String expectedStatus
    );

    int markProcessingFromFailed(
            @Param("refundNo") String refundNo,
            @Param("expectedStatus") String expectedStatus
    );

    int markMerchantApproved(
            @Param("refundNo") String refundNo,
            @Param("approvedAmount") BigDecimal approvedAmount,
            @Param("merchantReviewReason") String merchantReviewReason,
            @Param("merchantReviewTime") LocalDateTime merchantReviewTime,
            @Param("merchantReviewerId") Long merchantReviewerId,
            @Param("expectedStatus") String expectedStatus
    );

    int escalateToAdminReview(
            @Param("refundNo") String refundNo,
            @Param("merchantReviewReason") String merchantReviewReason,
            @Param("merchantReviewTime") LocalDateTime merchantReviewTime,
            @Param("merchantReviewerId") Long merchantReviewerId,
            @Param("reviewDeadline") LocalDateTime reviewDeadline,
            @Param("expectedStatus") String expectedStatus
    );

    int markAdminApproved(
            @Param("refundNo") String refundNo,
            @Param("approvedAmount") BigDecimal approvedAmount,
            @Param("adminReviewReason") String adminReviewReason,
            @Param("adminReviewTime") LocalDateTime adminReviewTime,
            @Param("adminReviewerId") Long adminReviewerId,
            @Param("expectedStatus") String expectedStatus
    );

    int markAdminRejected(
            @Param("refundNo") String refundNo,
            @Param("adminReviewReason") String adminReviewReason,
            @Param("adminReviewTime") LocalDateTime adminReviewTime,
            @Param("adminReviewerId") Long adminReviewerId,
            @Param("expectedStatus") String expectedStatus
    );

    int escalateTimeoutToAdminReview(
            @Param("refundNo") String refundNo,
            @Param("merchantReviewReason") String merchantReviewReason,
            @Param("merchantReviewTime") LocalDateTime merchantReviewTime,
            @Param("reviewDeadline") LocalDateTime reviewDeadline,
            @Param("expectedStatus") String expectedStatus
    );

    long countByStatus(@Param("status") String status);
}
