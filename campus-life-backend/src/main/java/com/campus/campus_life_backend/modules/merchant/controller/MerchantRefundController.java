package com.campus.campus_life_backend.modules.merchant.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRefundReviewRequest;
import com.campus.campus_life_backend.modules.payment.service.PaymentRefundWorkflowService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant/refunds")
@RequirePermission(anyOf = {"merchant:refund:review:self"})
public class MerchantRefundController {

    private final CurrentUserAccessor currentUserAccessor;
    private final PaymentRefundWorkflowService paymentRefundWorkflowService;

    public MerchantRefundController(CurrentUserAccessor currentUserAccessor,
                                    PaymentRefundWorkflowService paymentRefundWorkflowService) {
        this.currentUserAccessor = currentUserAccessor;
        this.paymentRefundWorkflowService = paymentRefundWorkflowService;
    }

    @GetMapping
    public ApiResponse<?> listMerchantRefunds(@RequestParam(value = "status", required = false) String status) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(paymentRefundWorkflowService.listMerchantRefunds(userId, status));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, e.getMessage(), e);
        }
    }

    @GetMapping("/{refundNo}")
    public ApiResponse<?> getMerchantRefundDetail(@PathVariable String refundNo) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(paymentRefundWorkflowService.getMerchantRefundDetail(userId, refundNo));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    @PostMapping("/{refundNo}/approve")
    public ApiResponse<?> approveRefund(
            @PathVariable String refundNo,
            @RequestBody(required = false) PaymentRefundReviewRequest request
    ) {
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(paymentRefundWorkflowService.merchantApprove(
                    refundNo,
                    userId,
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
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(paymentRefundWorkflowService.merchantRejectToAdmin(
                    refundNo,
                    userId,
                    request == null ? null : request.getReason()
            ));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }
}
