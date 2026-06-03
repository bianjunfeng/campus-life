package com.campus.campus_life_backend.modules.user.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Map;

public interface UserWalletMapper {

    int initWalletIfAbsent(@Param("userId") Long userId, @Param("initialBalance") BigDecimal initialBalance);

    Map<String, Object> findByUserId(@Param("userId") Long userId);

    int addSpent(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int deductBalanceAndAddSpent(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int addBalanceAndReduceSpent(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
