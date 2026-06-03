package com.campus.campus_life_backend.common.security.ownership;

import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class VoucherOwnerResolver implements ResourceOwnerResolver {

    private final VoucherMapper voucherMapper;
    private final MerchantMapper merchantMapper;

    public VoucherOwnerResolver(VoucherMapper voucherMapper, MerchantMapper merchantMapper) {
        this.voucherMapper = voucherMapper;
        this.merchantMapper = merchantMapper;
    }

    @Override
    public boolean supports(ResourceTypeCode resourceType) {
        return resourceType == ResourceTypeCode.VOUCHER;
    }

    @Override
    @Nullable
    public Long resolveOwnerId(Long resourceId) {
        Voucher voucher = voucherMapper.findById(resourceId);
        if (voucher == null || voucher.getMerchantId() == null) {
            return null;
        }
        Merchant merchant = merchantMapper.findById(voucher.getMerchantId());
        return merchant != null ? merchant.getUserId() : null;
    }
}
