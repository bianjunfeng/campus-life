package com.campus.campus_life_backend.modules.voucher.mapper;

import com.campus.campus_life_backend.modules.voucher.entity.ShoppingCartItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ShoppingCartMapper {

    ShoppingCartItem findByUserAndVoucher(
            @Param("userId") Long userId,
            @Param("voucherId") Long voucherId
    );

    int insert(ShoppingCartItem item);

    int updateQuantityAndSelectedById(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("quantity") Integer quantity,
            @Param("selected") Integer selected
    );

    int updateSelectedById(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("selected") Integer selected
    );

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    int deleteByIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    int clearByUser(@Param("userId") Long userId);

    List<Map<String, Object>> findViewsByUser(@Param("userId") Long userId);

    List<ShoppingCartItem> findByIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
