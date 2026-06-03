package com.campus.campus_life_backend.modules.voucher.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission;
import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.voucher.service.VoucherService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    private final CurrentUserAccessor currentUserAccessor;
    private final VoucherService voucherService;

    public VoucherController(CurrentUserAccessor currentUserAccessor, VoucherService voucherService) {
        this.currentUserAccessor = currentUserAccessor;
        this.voucherService = voucherService;
    }

    @GetMapping("/available")
    public ApiResponse<List<Voucher>> getAvailableVouchers(
            @RequestParam(value = "type", required = false) Integer type) {
        List<Voucher> vouchers = voucherService.getAvailableVouchers(type);
        return ApiResponse.success(vouchers);
    }

    @GetMapping("/my-orders")
    @RequireLogin
    public ApiResponse<Map<String, Object>> getMyVouchers(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(voucherService.getMyOrderViews(userId, status, page, size));
    }

    @GetMapping("/my-orders/{id}")
    @RequireLogin
    @RequireOwnerOrPermission(resource = ResourceTypeCode.VOUCHER_ORDER, idParam = "id")
    public ApiResponse<Map<String, Object>> getMyOrderDetail(@PathVariable("id") Long id) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(voucherService.getOrderDetailById(userId, id));
    }

    @GetMapping("/my-orders/order-no/{orderNo}")
    @RequireLogin
    public ApiResponse<Map<String, Object>> getMyOrderDetailByOrderNo(@PathVariable("orderNo") String orderNo) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(voucherService.getOrderDetailByOrderNo(userId, orderNo));
    }

    @PostMapping("/{voucherId}/claims")
    @RequireLogin
    public ApiResponse<VoucherOrder> claimVoucher(@PathVariable Long voucherId) {
        Long userId = currentUserAccessor.requireUserId();
        VoucherOrder order = voucherService.claimVoucher(voucherId, userId);
        return ApiResponse.success(order);
    }

    @PostMapping("/my-orders/{id}/cancel")
    @RequireLogin
    @RequireOwnerOrPermission(resource = ResourceTypeCode.VOUCHER_ORDER, idParam = "id")
    public ApiResponse<Map<String, Object>> cancelMyOrder(@PathVariable("id") Long id) {
        Long userId = currentUserAccessor.requireUserId();
        voucherService.cancelOrder(userId, id);
        return ApiResponse.success(Map.of("success", true));
    }
}
