package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.modules.admin.service.MerchantAdminOrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRole(RoleCode.ADMIN)
public class AdminMerchantController {

    private final MerchantAdminOrchestrationService merchantAdminService;
    private final AdminAuthUtil adminAuthUtil;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminMerchantController(MerchantAdminOrchestrationService merchantAdminService,
                                   AdminAuthUtil adminAuthUtil,
                                   CurrentUserAccessor currentUserAccessor) {
        this.merchantAdminService = merchantAdminService;
        this.adminAuthUtil = adminAuthUtil;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/merchants")
    public ApiResponse<Map<String, Object>> getMerchantList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(merchantAdminService.getMerchantList(page, size, status, keyword));
    }

    @PostMapping("/merchants")
    public ApiResponse<Map<String, Object>> createMerchant(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        return ApiResponse.success(merchantAdminService.createMerchant(currentUserAccessor.requireUserId(), request, adminAuthUtil.getClientIp(httpRequest)));
    }

    @PutMapping("/merchants/{merchantId}")
    public ApiResponse<Void> updateMerchant(@PathVariable Long merchantId, @RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        merchantAdminService.updateMerchant(currentUserAccessor.requireUserId(), merchantId, request, adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/merchants/{merchantId}/approve")
    public ApiResponse<Void> approveMerchant(@PathVariable Long merchantId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        merchantAdminService.approveMerchant(currentUserAccessor.requireUserId(), merchantId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/merchants/{merchantId}/reject")
    public ApiResponse<Void> rejectMerchant(@PathVariable Long merchantId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        merchantAdminService.rejectMerchant(currentUserAccessor.requireUserId(), merchantId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/merchants/{merchantId}/freeze")
    public ApiResponse<Void> freezeMerchant(@PathVariable Long merchantId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        merchantAdminService.freezeMerchant(currentUserAccessor.requireUserId(), merchantId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/merchants/{merchantId}/unfreeze")
    public ApiResponse<Void> unfreezeMerchant(@PathVariable Long merchantId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        merchantAdminService.unfreezeMerchant(currentUserAccessor.requireUserId(), merchantId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }
}
