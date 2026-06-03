package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.modules.payment.dto.PaymentReconciliationResolveRequest;
import com.campus.campus_life_backend.modules.payment.service.PaymentReconciliationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payment-reconciliation")
@RequirePermission(anyOf = {"admin:payment:reconcile"})
public class AdminReconciliationController {

    private final CurrentUserAccessor currentUserAccessor;
    private final PaymentReconciliationService paymentReconciliationService;

    public AdminReconciliationController(
            CurrentUserAccessor currentUserAccessor,
            PaymentReconciliationService paymentReconciliationService
    ) {
        this.currentUserAccessor = currentUserAccessor;
        this.paymentReconciliationService = paymentReconciliationService;
    }

    @GetMapping("/issues")
    public ApiResponse<?> listIssues(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "issueType", required = false) String issueType
    ) {
        return ApiResponse.success(paymentReconciliationService.listIssues(status, issueType));
    }

    @PostMapping("/scan")
    public ApiResponse<?> scanIssues(
            @RequestParam(value = "days", required = false, defaultValue = "7") Integer days,
            @RequestParam(value = "limit", required = false, defaultValue = "500") Integer limit
    ) {
        try {
            return ApiResponse.success(paymentReconciliationService.scanRecentIssues(days, limit));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    @PostMapping("/issues/{issueId}/resolve")
    public ApiResponse<?> resolveIssue(
            @PathVariable Long issueId,
            @RequestBody(required = false) PaymentReconciliationResolveRequest request
    ) {
        try {
            Long adminId = currentUserAccessor.requireUserId();
            paymentReconciliationService.resolveIssue(issueId, adminId, request == null ? null : request.getNote());
            return ApiResponse.success(true);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }
}
