package com.campus.campus_life_backend.common.security.ownership;

import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentOrderMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PaymentOrderOwnerResolver implements ResourceOwnerResolver {

    private final PaymentOrderMapper paymentOrderMapper;

    public PaymentOrderOwnerResolver(PaymentOrderMapper paymentOrderMapper) {
        this.paymentOrderMapper = paymentOrderMapper;
    }

    @Override
    public boolean supports(ResourceTypeCode resourceType) {
        return resourceType == ResourceTypeCode.PAYMENT_ORDER;
    }

    @Override
    @Nullable
    public Long resolveOwnerId(Long resourceId) {
        PaymentOrder paymentOrder = paymentOrderMapper.findById(resourceId);
        return paymentOrder != null ? paymentOrder.getUserId() : null;
    }
}
