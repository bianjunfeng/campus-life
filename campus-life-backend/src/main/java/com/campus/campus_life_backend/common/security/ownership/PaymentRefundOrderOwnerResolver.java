package com.campus.campus_life_backend.common.security.ownership;

import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.modules.payment.entity.PaymentRefundOrder;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentRefundOrderMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PaymentRefundOrderOwnerResolver implements ResourceOwnerResolver {

    private final PaymentRefundOrderMapper paymentRefundOrderMapper;

    public PaymentRefundOrderOwnerResolver(PaymentRefundOrderMapper paymentRefundOrderMapper) {
        this.paymentRefundOrderMapper = paymentRefundOrderMapper;
    }

    @Override
    public boolean supports(ResourceTypeCode resourceType) {
        return resourceType == ResourceTypeCode.PAYMENT_REFUND_ORDER;
    }

    @Override
    @Nullable
    public Long resolveOwnerId(Long resourceId) {
        PaymentRefundOrder refundOrder = paymentRefundOrderMapper.findById(resourceId);
        return refundOrder != null ? refundOrder.getUserId() : null;
    }
}
