package com.campus.campus_life_backend.modules.merchant.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.merchant.service.MerchantVoucherService;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant/vouchers")
@RequirePermission(anyOf = {"merchant:voucher:manage:self"})
public class MerchantVoucherController {

    private final CurrentUserAccessor currentUserAccessor;
    private final MerchantVoucherService merchantVoucherService;

    public MerchantVoucherController(CurrentUserAccessor currentUserAccessor,
                                     MerchantVoucherService merchantVoucherService) {
        this.currentUserAccessor = currentUserAccessor;
        this.merchantVoucherService = merchantVoucherService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getMerchantVouchers(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(merchantVoucherService.getMerchantVouchers(userId, status, page, size));
        } catch (MerchantVoucherService.ForbiddenException e) {
            throw e;
        }
    }

    @GetMapping("/{id}")
    @RequireOwnerOrPermission(resource = ResourceTypeCode.VOUCHER, idParam = "id")
    public ApiResponse<Map<String, Object>> getVoucherDetail(@PathVariable Long id) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(merchantVoucherService.getVoucherDetail(userId, id));
        } catch (MerchantVoucherService.ForbiddenException e) {
            throw e;
        } catch (MerchantVoucherService.NotFoundException e) {
            throw e;
        }
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createVoucher(@RequestBody Map<String, Object> request) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(merchantVoucherService.createVoucher(userId, request));
        } catch (MerchantVoucherService.ForbiddenException e) {
            throw e;
        } catch (MerchantVoucherService.ValidationException e) {
            throw e;
        }
    }

    @PutMapping("/{id}")
    @RequireOwnerOrPermission(resource = ResourceTypeCode.VOUCHER, idParam = "id")
    public ApiResponse<Map<String, Object>> updateVoucher(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(merchantVoucherService.updateVoucher(userId, id, request));
        } catch (MerchantVoucherService.ForbiddenException e) {
            throw e;
        } catch (MerchantVoucherService.NotFoundException e) {
            throw e;
        } catch (MerchantVoucherService.ValidationException e) {
            throw e;
        }
    }

    @PostMapping("/{id}/online")
    @RequireOwnerOrPermission(resource = ResourceTypeCode.VOUCHER, idParam = "id")
    public ApiResponse<Voucher> onlineVoucher(@PathVariable Long id) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(merchantVoucherService.updateStatus(userId, id, 1));
        } catch (MerchantVoucherService.ForbiddenException e) {
            throw e;
        } catch (MerchantVoucherService.NotFoundException e) {
            throw e;
        }
    }

    @PostMapping("/{id}/offline")
    @RequireOwnerOrPermission(resource = ResourceTypeCode.VOUCHER, idParam = "id")
    public ApiResponse<Voucher> offlineVoucher(@PathVariable Long id) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(merchantVoucherService.updateStatus(userId, id, 0));
        } catch (MerchantVoucherService.ForbiddenException e) {
            throw e;
        } catch (MerchantVoucherService.NotFoundException e) {
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    @RequireOwnerOrPermission(resource = ResourceTypeCode.VOUCHER, idParam = "id")
    public ApiResponse<Void> deleteVoucher(@PathVariable Long id) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            merchantVoucherService.deleteVoucher(userId, id);
            return ApiResponse.success(null);
        } catch (MerchantVoucherService.ForbiddenException e) {
            throw e;
        } catch (MerchantVoucherService.NotFoundException e) {
            throw e;
        }
    }
}
