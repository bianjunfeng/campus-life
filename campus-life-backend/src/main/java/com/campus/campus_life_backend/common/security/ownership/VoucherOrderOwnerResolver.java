package com.campus.campus_life_backend.common.security.ownership;

import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.order.mapper.VoucherOrderMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class VoucherOrderOwnerResolver implements ResourceOwnerResolver {

    private final VoucherOrderMapper voucherOrderMapper;

    public VoucherOrderOwnerResolver(VoucherOrderMapper voucherOrderMapper) {
        this.voucherOrderMapper = voucherOrderMapper;
    }

    @Override
    public boolean supports(ResourceTypeCode resourceType) {
        return resourceType == ResourceTypeCode.VOUCHER_ORDER;
    }

    @Override
    @Nullable
    public Long resolveOwnerId(Long resourceId) {
        VoucherOrder voucherOrder = voucherOrderMapper.findById(resourceId);
        return voucherOrder != null ? voucherOrder.getUserId() : null;
    }
}
