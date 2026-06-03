package com.campus.campus_life_backend.modules.merchant.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.merchant.service.MerchantHomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant/home")
@RequirePermission(anyOf = {
        "merchant:profile:manage:self",
        "merchant:voucher:manage:self",
        "merchant:refund:review:self"
})
public class MerchantHomeController {

    private final CurrentUserAccessor currentUserAccessor;
    private final MerchantHomeService merchantHomeService;

    public MerchantHomeController(
            CurrentUserAccessor currentUserAccessor,
            MerchantHomeService merchantHomeService
    ) {
        this.currentUserAccessor = currentUserAccessor;
        this.merchantHomeService = merchantHomeService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getMerchantHome() {
        Long userId = currentUserAccessor.requireUserId();
        try {
            return ApiResponse.success(merchantHomeService.getMerchantHome(userId));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, e.getMessage(), e);
        }
    }
}
