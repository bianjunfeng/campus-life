package com.campus.campus_life_backend.modules.voucher.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.modules.voucher.service.VoucherSeckillService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Profile("perf")
@RestController
@RequestMapping("/api/perf/seckill")
public class SeckillPerfController {

    private final VoucherSeckillService voucherSeckillService;

    public SeckillPerfController(VoucherSeckillService voucherSeckillService) {
        this.voucherSeckillService = voucherSeckillService;
    }

    @PostMapping("/{voucherId}/grab")
    public ApiResponse<Map<String, Object>> grab(@PathVariable Long voucherId,
                                                 @RequestParam("userId") Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "userId must be positive");
        }
        return ApiResponse.success(voucherSeckillService.submitSeckillPerfHotPath(voucherId, userId));
    }
}
