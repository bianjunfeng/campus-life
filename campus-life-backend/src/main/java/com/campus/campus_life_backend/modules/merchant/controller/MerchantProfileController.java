package com.campus.campus_life_backend.modules.merchant.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
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
    private final MerchantMapper merchantMapper;

    public MerchantProfileController(CurrentUserAccessor currentUserAccessor, MerchantMapper merchantMapper) {
        this.currentUserAccessor = currentUserAccessor;
        this.merchantMapper = merchantMapper;
    }

    @PutMapping
    public ApiResponse<Merchant> updateMerchantProfile(@RequestBody Map<String, Object> request) {
        Long userId = currentUserAccessor.requireUserId();
        Merchant merchant = merchantMapper.findByUserId(userId);
        if (merchant == null) {
            throw new BusinessException(BusinessErrorCode.MERCHANT_NOT_FOUND);
        }

        String contactName = trimToNull(request.get("contactName"));
        String contactPhone = trimToNull(request.get("contactPhone"));
        String address = trimToNull(request.get("address"));
        String businessHours = trimToNull(request.get("businessHours"));

        if (contactName != null) {
            merchant.setContactName(contactName);
        }
        if (contactPhone != null) {
            if (!contactPhone.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(BusinessErrorCode.CONTACT_PHONE_INVALID);
            }
            merchant.setContactPhone(contactPhone);
        }
        if (address != null) {
            merchant.setAddress(address);
        }
        if (request.containsKey("businessHours")) {
            if (businessHours != null && businessHours.length() > 80) {
                throw new IllegalArgumentException("营业时间不能超过80个字符");
            }
            merchant.setBusinessHours(businessHours);
        }

        merchantMapper.updateMerchant(merchant);
        return ApiResponse.success(merchantMapper.findByUserId(userId));
    }

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? null : str;
    }
}
