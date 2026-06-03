package com.campus.campus_life_backend.modules.payment.mapper;

import com.campus.campus_life_backend.modules.payment.entity.PaymentRefundReviewLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PaymentRefundReviewLogMapper {

    int insert(PaymentRefundReviewLog reviewLog);

    List<PaymentRefundReviewLog> findByRefundNo(@Param("refundNo") String refundNo);
}
