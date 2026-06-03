package com.campus.campus_life_backend.modules.user.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface WalletTransactionMapper {

    int insertExpenseTransaction(
            @Param("userId") Long userId,
            @Param("amount") BigDecimal amount,
            @Param("balanceAfter") BigDecimal balanceAfter,
            @Param("orderNo") String orderNo,
            @Param("remark") String remark
    );

    int insertRefundTransaction(
            @Param("userId") Long userId,
            @Param("amount") BigDecimal amount,
            @Param("balanceAfter") BigDecimal balanceAfter,
            @Param("orderNo") String orderNo,
            @Param("remark") String remark
    );
}

