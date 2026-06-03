package com.campus.campus_life_backend.modules.voucher.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.voucher.dto.SeckillOrderMessage;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.voucher.mapper.SeckillVoucherMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SeckillOrderProcessService {

    public enum ProcessStatus {
        SUCCESS,
        SKIPPED
    }

    public static class SeckillNonRetryableException extends BusinessException {
        public SeckillNonRetryableException(String message) {
            super(BusinessErrorCode.STOCK_NOT_ENOUGH, message);
        }
    }

    private final VoucherOrderService voucherOrderService;
    private final VoucherMapper voucherMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final WelfareQueryService welfareQueryService;

    public SeckillOrderProcessService(
            VoucherOrderService voucherOrderService,
            VoucherMapper voucherMapper,
            SeckillVoucherMapper seckillVoucherMapper,
            WelfareQueryService welfareQueryService
    ) {
        this.voucherOrderService = voucherOrderService;
        this.voucherMapper = voucherMapper;
        this.seckillVoucherMapper = seckillVoucherMapper;
        this.welfareQueryService = welfareQueryService;
    }

    @Transactional
    public ProcessStatus processOrder(SeckillOrderMessage message) {
        int seckillUpdated = seckillVoucherMapper.decrementStockByVoucherId(message.getVoucherId());
        int voucherUpdated = voucherMapper.decrementStockIfAvailable(message.getVoucherId());
        if (seckillUpdated <= 0 || voucherUpdated <= 0) {
            throw new SeckillNonRetryableException("库存不足");
        }

        Voucher voucher = voucherMapper.findById(message.getVoucherId());
        voucherOrderService.createOrder(
                message.getUserId(),
                message.getVoucherId(),
                voucher != null ? voucher.getPayValue() : null,
                "seckill",
                LocalDateTime.now().plusMinutes(30),
                voucher != null ? voucher.getEndTime() : null,
                message.getOrderNo()
        );

        welfareQueryService.evictWelfareCache();
        return ProcessStatus.SUCCESS;
    }
}
