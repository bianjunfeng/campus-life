package com.campus.campus_life_backend.modules.voucher.mapper;

import com.campus.campus_life_backend.modules.voucher.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SeckillVoucherMapper {

    int insertSeckillVoucher(SeckillVoucher seckillVoucher);

    SeckillVoucher findById(@Param("id") Long id);

    SeckillVoucher findByVoucherId(@Param("voucherId") Long voucherId);

    List<SeckillVoucher> findActive();

    int updateSeckillVoucher(SeckillVoucher seckillVoucher);

    int decrementStock(@Param("id") Long id);

    int decrementStockByVoucherId(@Param("voucherId") Long voucherId);

    int incrementStockByVoucherId(@Param("voucherId") Long voucherId);

    int deleteByVoucherId(@Param("voucherId") Long voucherId);
}
