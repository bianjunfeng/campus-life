package com.campus.campus_life_backend.modules.payment.mapper;

import com.campus.campus_life_backend.modules.payment.entity.PaymentCallbackLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PaymentCallbackLogMapper {

    int insert(PaymentCallbackLog callbackLog);

    List<PaymentCallbackLog> findByPaymentNo(@Param("paymentNo") String paymentNo);
}
