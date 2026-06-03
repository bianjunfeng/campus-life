package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.modules.admin.service.VoucherAdminOrchestrationService;
import com.campus.campus_life_backend.modules.voucher.service.SeckillOpsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRole(RoleCode.ADMIN)
public class AdminVoucherController {

    private final VoucherAdminOrchestrationService voucherAdminService;
    private final SeckillOpsService seckillOpsService;
    private final AdminAuthUtil adminAuthUtil;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminVoucherController(VoucherAdminOrchestrationService voucherAdminService,
                                  SeckillOpsService seckillOpsService,
                                  AdminAuthUtil adminAuthUtil,
                                  CurrentUserAccessor currentUserAccessor) {
        this.voucherAdminService = voucherAdminService;
        this.seckillOpsService = seckillOpsService;
        this.adminAuthUtil = adminAuthUtil;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/vouchers")
    public ApiResponse<Map<String, Object>> getVoucherList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "merchantId", required = false) Long merchantId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(voucherAdminService.getVoucherList(page, size, status, merchantId, keyword));
    }

    @GetMapping("/voucher-orders")
    public ApiResponse<Map<String, Object>> getVoucherOrderList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "voucherId", required = false) Long voucherId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "paymentStatus", required = false) Integer paymentStatus,
            @RequestParam(value = "orderSource", required = false) String orderSource,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(voucherAdminService.getVoucherOrderList(page, size, keyword, userId, voucherId, status, paymentStatus, orderSource, timeRange, startDate, endDate));
    }

    @GetMapping("/voucher-orders/stats")
    public ApiResponse<Map<String, Object>> getVoucherOrderStats(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "voucherId", required = false) Long voucherId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "paymentStatus", required = false) Integer paymentStatus,
            @RequestParam(value = "orderSource", required = false) String orderSource,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(voucherAdminService.getVoucherOrderStats(keyword, userId, voucherId, status, paymentStatus, orderSource, timeRange, startDate, endDate));
    }

    @PostMapping("/vouchers")
    public ApiResponse<Map<String, Object>> createVoucher(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        return ApiResponse.success(voucherAdminService.createVoucher(currentUserAccessor.requireUserId(), request, adminAuthUtil.getClientIp(httpRequest)));
    }

    @PutMapping("/vouchers/{voucherId}")
    public ApiResponse<Void> updateVoucher(@PathVariable Long voucherId, @RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        voucherAdminService.updateVoucher(currentUserAccessor.requireUserId(), voucherId, request, adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/vouchers/{voucherId}/offline")
    public ApiResponse<Void> offlineVoucher(@PathVariable Long voucherId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        voucherAdminService.offlineVoucher(currentUserAccessor.requireUserId(), voucherId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/vouchers/{voucherId}/violation-offline")
    public ApiResponse<Void> violationOfflineVoucher(@PathVariable Long voucherId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        voucherAdminService.violationOfflineVoucher(currentUserAccessor.requireUserId(), voucherId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/vouchers/{voucherId}/online")
    public ApiResponse<Void> onlineVoucher(@PathVariable Long voucherId, HttpServletRequest httpRequest) {
        voucherAdminService.onlineVoucher(currentUserAccessor.requireUserId(), voucherId, adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/vouchers/{voucherId}/preheat")
    public ApiResponse<Void> preheatVoucher(@PathVariable Long voucherId) {
        currentUserAccessor.requireUserId();
        voucherAdminService.preheatSeckillVoucher(voucherId);
        return ApiResponse.success(null);
    }

    @GetMapping("/vouchers/{voucherId}/monitor")
    public ApiResponse<Map<String, Object>> monitorVoucher(@PathVariable Long voucherId) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(voucherAdminService.getSeckillMonitor(voucherId));
    }

    @GetMapping("/seckill/health")
    public ApiResponse<Map<String, Object>> seckillHealth(@RequestParam(value = "voucherId", required = false) Long voucherId) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(seckillOpsService.getHealth(voucherId));
    }

    @PostMapping("/seckill/metrics/reset")
    public ApiResponse<Map<String, Object>> resetSeckillMetrics() {
        currentUserAccessor.requireUserId();
        seckillOpsService.resetMetrics();
        return ApiResponse.success(Map.of("success", true));
    }
}
