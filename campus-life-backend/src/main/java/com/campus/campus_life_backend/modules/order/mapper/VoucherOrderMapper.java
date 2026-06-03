package com.campus.campus_life_backend.modules.order.mapper;

import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VoucherOrderMapper {

    int insertVoucherOrder(VoucherOrder voucherOrder);

    VoucherOrder findById(@Param("id") Long id);

    VoucherOrder findByOrderNo(@Param("orderNo") String orderNo);

    List<VoucherOrder> findByUserId(@Param("userId") Long userId);

    List<VoucherOrder> findByVoucherId(@Param("voucherId") Long voucherId);

    int countByUserAndVoucher(@Param("userId") Long userId, @Param("voucherId") Long voucherId);

    int countActiveByUserAndVoucher(@Param("userId") Long userId, @Param("voucherId") Long voucherId);

    BigDecimal sumPaidAmountByUserId(@Param("userId") Long userId);

    Integer countPendingPaymentByUserId(@Param("userId") Long userId);

    Integer countAvailableCouponByUserId(@Param("userId") Long userId);

    List<Map<String, Object>> findRecentTransactionsByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    List<Map<String, Object>> findOrderViewsByUserId(
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );

    Integer countOrderViewsByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    Map<String, Object> findOrderViewByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Map<String, Object> findOrderViewByOrderNoAndUserId(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    int cancelPendingOrderByUserAndId(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("cancelReason") String cancelReason
    );

    int cancelPendingOrderByUserAndIdWithVersion(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("cancelReason") String cancelReason,
            @Param("expectedUpdateTime") LocalDateTime expectedUpdateTime
    );

    int cancelPendingOrderById(
            @Param("id") Long id,
            @Param("cancelReason") String cancelReason
    );

    int cancelPendingOrderByIdWithVersion(
            @Param("id") Long id,
            @Param("cancelReason") String cancelReason,
            @Param("expectedUpdateTime") LocalDateTime expectedUpdateTime
    );

    List<VoucherOrder> findExpiredPendingOrders(@Param("limit") Integer limit);

    List<VoucherOrder> findExpiredPendingOrdersByUser(
            @Param("userId") Long userId,
            @Param("limit") Integer limit
    );

    List<VoucherOrder> findExpiredPaidOrdersForAutoRefund(
            @Param("now") LocalDateTime now,
            @Param("limit") Integer limit
    );

    int expirePendingOrdersByUser(@Param("userId") Long userId);

    int markOrderExpiredWithVersion(
            @Param("id") Long id,
            @Param("cancelReason") String cancelReason,
            @Param("now") LocalDateTime now,
            @Param("expectedUpdateTime") LocalDateTime expectedUpdateTime
    );

    int markOrderPaidWithVersion(
            @Param("id") Long id,
            @Param("paymentMethod") String paymentMethod,
            @Param("payTime") LocalDateTime payTime,
            @Param("expectedUpdateTime") LocalDateTime expectedUpdateTime
    );

    int bindPaymentIdempotencyKeyIfPending(
            @Param("id") Long id,
            @Param("idempotencyKey") String idempotencyKey
    );

    int markOrderPaymentFailedWithVersion(
            @Param("id") Long id,
            @Param("expectedUpdateTime") LocalDateTime expectedUpdateTime
    );

    int markOrderRefundedWithVersion(
            @Param("id") Long id,
            @Param("cancelReason") String cancelReason,
            @Param("expectedUpdateTime") LocalDateTime expectedUpdateTime
    );

    List<Map<String, Object>> findAdminOrderViews(
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            @Param("voucherId") Long voucherId,
            @Param("status") Integer status,
            @Param("paymentStatus") Integer paymentStatus,
            @Param("orderSource") String orderSource,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );

    Integer countAdminOrderViews(
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            @Param("voucherId") Long voucherId,
            @Param("status") Integer status,
            @Param("paymentStatus") Integer paymentStatus,
            @Param("orderSource") String orderSource,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    Map<String, Object> sumAdminOrderStats(
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            @Param("voucherId") Long voucherId,
            @Param("status") Integer status,
            @Param("paymentStatus") Integer paymentStatus,
            @Param("orderSource") String orderSource,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    List<Map<String, Object>> findAdminOrderTrend(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    List<Map<String, Object>> findAdminVoucherUsage(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") Integer limit
    );

    int updateVoucherOrder(VoucherOrder voucherOrder);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    long countOrdersSince(@Param("since") LocalDateTime since);

    List<Map<String, Object>> countOrdersTrend(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
