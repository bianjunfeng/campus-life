package com.campus.campus_life_backend.modules.voucher.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.voucher.config.SeckillSentinelConfig;
import com.campus.campus_life_backend.modules.voucher.service.VoucherSeckillService;
import com.campus.campus_life_backend.modules.voucher.service.WelfareQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CouponController {

    private final CurrentUserAccessor currentUserAccessor;
    private final WelfareQueryService welfareQueryService;
    private final VoucherSeckillService voucherSeckillService;

    public CouponController(
            CurrentUserAccessor currentUserAccessor,
            WelfareQueryService welfareQueryService,
            VoucherSeckillService voucherSeckillService
    ) {
        this.currentUserAccessor = currentUserAccessor;
        this.welfareQueryService = welfareQueryService;
        this.voucherSeckillService = voucherSeckillService;
    }

    @GetMapping("/welfare/home")
    public ApiResponse<Map<String, Object>> getWelfareHome() {
        return ApiResponse.success(welfareQueryService.getWelfareHome());
    }

    @GetMapping("/coupons")
    public ApiResponse<List<Map<String, Object>>> getCoupons(@RequestParam(required = false) Integer categoryId) {
        return ApiResponse.success(welfareQueryService.getCoupons(categoryId));
    }

    @GetMapping("/flash-sales")
    public ApiResponse<List<Map<String, Object>>> getFlashSales() {
        return ApiResponse.success(welfareQueryService.getFlashSales());
    }

    @GetMapping("/coupons/{couponId}")
    public ApiResponse<Map<String, Object>> getCouponDetail(@PathVariable Long couponId) {
        return ApiResponse.success(welfareQueryService.getCouponDetail(couponId));
    }

    @PostMapping("/coupons/{couponId}/grab")
    @RequirePermission(anyOf = {"voucher:order:self"})
    public ApiResponse<Map<String, Object>> grabCoupon(@PathVariable Long couponId) {
        Long userId = currentUserAccessor.requireUserId();

        Entry entry = null;
        try {
            entry = SphU.entry(SeckillSentinelConfig.SECKILL_RESOURCE);
            return ApiResponse.success(voucherSeckillService.submitSeckill(couponId, userId));
        } catch (BlockException e) {
            throw new BusinessException(BusinessErrorCode.TOO_MANY_REQUESTS, "请求太频繁，请稍后再试", e);
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    @GetMapping("/coupons/grab/result")
    @RequirePermission(anyOf = {"voucher:order:self"})
    public ApiResponse<Map<String, Object>> queryGrabResult(@RequestParam("orderNo") String orderNo) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(voucherSeckillService.querySeckillResult(orderNo, userId));
    }
}
