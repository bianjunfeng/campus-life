package com.campus.campus_life_backend.modules.merchant.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.service.MerchantProfileService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant/profile")
@RequirePermission(anyOf = {"merchant:profile:manage:self"})
public class MerchantProfileController {

    private final CurrentUserAccessor currentUserAccessor;
    private final MerchantProfileService merchantProfileService;

    public MerchantProfileController(
            CurrentUserAccessor currentUserAccessor,
            MerchantProfileService merchantProfileService
    ) {
        this.currentUserAccessor = currentUserAccessor;
        this.merchantProfileService = merchantProfileService;
    }

    @PutMapping
    public ApiResponse<Merchant> updateMerchantProfile(@RequestBody Map<String, Object> request) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(merchantProfileService.updateMerchantProfile(userId, request));
    }
}
