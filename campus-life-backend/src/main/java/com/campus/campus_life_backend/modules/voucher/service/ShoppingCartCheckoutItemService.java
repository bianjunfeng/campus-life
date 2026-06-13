package com.campus.campus_life_backend.modules.voucher.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.voucher.entity.ShoppingCartItem;
import com.campus.campus_life_backend.modules.voucher.mapper.SeckillVoucherMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.ShoppingCartMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class ShoppingCartCheckoutItemService {

    private final ShoppingCartMapper shoppingCartMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final VoucherService voucherService;

    public ShoppingCartCheckoutItemService(
            ShoppingCartMapper shoppingCartMapper,
            SeckillVoucherMapper seckillVoucherMapper,
            VoucherService voucherService
    ) {
        this.shoppingCartMapper = shoppingCartMapper;
        this.seckillVoucherMapper = seckillVoucherMapper;
        this.voucherService = voucherService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> checkoutOne(Long userId, ShoppingCartItem item) {
        if (item == null || item.getId() == null || item.getVoucherId() == null) {
            throw new BusinessException(BusinessErrorCode.CART_ITEM_INVALID);
        }
        if (isSeckillVoucher(item.getVoucherId())) {
            throw new BusinessException(BusinessErrorCode.CART_SECKILL_CHECKOUT_NOT_ALLOWED);
        }

        VoucherOrder order = voucherService.claimVoucher(item.getVoucherId(), userId);
        int deleted = shoppingCartMapper.deleteById(item.getId(), userId);
        if (deleted <= 0) {
            throw new BusinessException(BusinessErrorCode.CART_ITEM_NOT_FOUND);
        }

        Map<String, Object> success = new HashMap<>();
        success.put("cartItemId", item.getId());
        success.put("voucherId", item.getVoucherId());
        success.put("orderNo", order.getOrderNo());
        success.put("payAmount", order.getPayAmount());
        return success;
    }

    private boolean isSeckillVoucher(Long voucherId) {
        if (voucherId == null || voucherId <= 0) {
            return false;
        }
        return seckillVoucherMapper.findByVoucherId(voucherId) != null;
    }
}
