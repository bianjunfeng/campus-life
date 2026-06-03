package com.campus.campus_life_backend.modules.voucher.mapper;

import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VoucherMapper {

    int insertVoucher(Voucher voucher);

    Voucher findById(@Param("id") Long id);

    List<Voucher> findByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * 鏌ヨ鍟嗗鎵€鏈変紭鎯犲埜锛堝寘鎷笅鏋剁殑锛?
     */
    List<Voucher> findAllByMerchantId(@Param("merchantId") Long merchantId);

    List<Voucher> findAvailable();

    // 管理后台：查询所有优惠券（包含下架/无库存）
    List<Voucher> findAll();

    int updateVoucher(Voucher voucher);

    int markExpiredAsOffline(@Param("limit") int limit);

    int updateStock(@Param("id") Long id, @Param("stock") Integer stock);

    int decrementStock(@Param("id") Long id);

    int decrementStockIfAvailable(@Param("id") Long id);

    int incrementStock(@Param("id") Long id);

    int incrementSoldCount(@Param("id") Long id);

    long countAllForAdmin();

    long countByStatusForAdmin(@Param("status") Integer status);

    long countVouchersSince(@Param("since") java.time.LocalDateTime since);

    List<java.util.Map<String, Object>> countByMerchantTypeForAdmin();

    List<java.util.Map<String, Object>> countContributionByMonthWeek(@Param("startTime") java.time.LocalDateTime startTime);
}
