package com.campus.campus_life_backend.common.security.ownership;

import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MerchantOwnerResolver implements ResourceOwnerResolver {

    private final MerchantMapper merchantMapper;

    public MerchantOwnerResolver(MerchantMapper merchantMapper) {
        this.merchantMapper = merchantMapper;
    }

    @Override
    public boolean supports(ResourceTypeCode resourceType) {
        return resourceType == ResourceTypeCode.MERCHANT;
    }

    @Override
    @Nullable
    public Long resolveOwnerId(Long resourceId) {
        Merchant merchant = merchantMapper.findById(resourceId);
        return merchant != null ? merchant.getUserId() : null;
    }
}
