package com.campus.campus_life_backend.modules.voucher.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission;
import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.voucher.config.SeckillSentinelConfig;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.voucher.service.ShoppingCartService;
import com.campus.campus_life_backend.modules.voucher.service.VoucherSeckillService;
import com.campus.campus_life_backend.modules.voucher.service.VoucherService;
import com.campus.campus_life_backend.modules.voucher.service.WelfareQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voucher-service")
public class VoucherServiceGatewayController {

    private final CurrentUserAccessor currentUserAccessor;
    private final WelfareQueryService welfareQueryService;
    private final VoucherSeckillService voucherSeckillService;
    private final VoucherService voucherService;
    private final ShoppingCartService shoppingCartService;

    public VoucherServiceGatewayController(
            CurrentUserAccessor currentUserAccessor,
            WelfareQueryService welfareQueryService,
            VoucherSeckillService voucherSeckillService,
            VoucherService voucherService,
            ShoppingCartService shoppingCartService
    ) {
        this.currentUserAccessor = currentUserAccessor;
        this.welfareQueryService = welfareQueryService;
        this.voucherSeckillService = voucherSeckillService;
        this.voucherService = voucherService;
        this.shoppingCartService = shoppingCartService;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of("service", "voucher-service", "status", "UP"));
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
    @RequireLogin
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
    @RequireLogin
    public ApiResponse<Map<String, Object>> queryGrabResult(@RequestParam("orderNo") String orderNo) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(voucherSeckillService.querySeckillResult(orderNo, userId));
    }

    @PostMapping("/vouchers/{voucherId}/claims")
    @RequireLogin
    public ApiResponse<VoucherOrder> claimVoucher(@PathVariable Long voucherId) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(voucherService.claimVoucher(voucherId, userId));
    }

    @GetMapping("/orders/my")
    @RequireLogin
    public ApiResponse<Map<String, Object>> getMyOrders(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(voucherService.getMyOrderViews(userId, status, page, size));
    }

    @PostMapping("/orders/my/{id}/cancel")
    @RequireLogin
    @RequireOwnerOrPermission(resource = ResourceTypeCode.VOUCHER_ORDER, idParam = "id")
    public ApiResponse<Map<String, Object>> cancelMyOrder(@PathVariable("id") Long id) {
        Long userId = currentUserAccessor.requireUserId();
        voucherService.cancelOrder(userId, id);
        return ApiResponse.success(Map.of("success", true));
    }

    @GetMapping("/cart/items")
    @RequireLogin
    public ApiResponse<List<Map<String, Object>>> getCartItems() {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(shoppingCartService.getCartViews(userId));
    }

    @PostMapping("/cart/items")
    @RequireLogin
    public ApiResponse<Map<String, Object>> addCartItem(@RequestBody Map<String, Object> request) {
        Long userId = currentUserAccessor.requireUserId();
        shoppingCartService.addToCart(userId, toLong(request.get("voucherId")), toInteger(request.get("quantity")));
        return ApiResponse.success(Collections.singletonMap("success", true));
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(String.valueOf(value));
    }
}
