package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRefundReviewRequest;
import com.campus.campus_life_backend.modules.payment.service.PaymentRefundWorkflowService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/refunds")
@RequirePermission(anyOf = {"admin:refund:review"})
public class AdminRefundController {

    private final CurrentUserAccessor currentUserAccessor;
    private final PaymentRefundWorkflowService paymentRefundWorkflowService;

    public AdminRefundController(
            CurrentUserAccessor currentUserAccessor,
            PaymentRefundWorkflowService paymentRefundWorkflowService
    ) {
        this.currentUserAccessor = currentUserAccessor;
        this.paymentRefundWorkflowService = paymentRefundWorkflowService;
    }

    @GetMapping
    public ApiResponse<?> listAdminRefunds(@RequestParam(value = "status", required = false) String status) {
        return ApiResponse.success(paymentRefundWorkflowService.listAdminRefunds(status));
    }

    @GetMapping("/{refundNo}")
    public ApiResponse<?> getAdminRefundDetail(@PathVariable String refundNo) {
        try {
            return ApiResponse.success(paymentRefundWorkflowService.getAdminRefundDetail(refundNo));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    @PostMapping("/{refundNo}/approve")
    public ApiResponse<?> approveRefund(
            @PathVariable String refundNo,
            @RequestBody(required = false) PaymentRefundReviewRequest request
    ) {
        try {
            Long adminId = currentUserAccessor.requireUserId();
            return ApiResponse.success(paymentRefundWorkflowService.adminApprove(
                    refundNo,
                    adminId,
                    request == null ? null : request.getApprovedAmount(),
                    request == null ? null : request.getReason()
            ));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    @PostMapping("/{refundNo}/reject")
    public ApiResponse<?> rejectRefund(
            @PathVariable String refundNo,
            @RequestBody(required = false) PaymentRefundReviewRequest request
    ) {
        try {
            Long adminId = currentUserAccessor.requireUserId();
            return ApiResponse.success(paymentRefundWorkflowService.adminReject(
                    refundNo,
                    adminId,
                    request == null ? null : request.getReason()
            ));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }
}
