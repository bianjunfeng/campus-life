package com.campus.campus_life_backend.modules.voucher.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.voucher.entity.ShoppingCartItem;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.voucher.entity.SeckillVoucher;
import com.campus.campus_life_backend.modules.voucher.mapper.SeckillVoucherMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.ShoppingCartMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShoppingCartService {

    private final ShoppingCartMapper shoppingCartMapper;
    private final VoucherMapper voucherMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final ShoppingCartCheckoutItemService checkoutItemService;

    public ShoppingCartService(
            ShoppingCartMapper shoppingCartMapper,
            VoucherMapper voucherMapper,
            SeckillVoucherMapper seckillVoucherMapper,
            ShoppingCartCheckoutItemService checkoutItemService
    ) {
        this.shoppingCartMapper = shoppingCartMapper;
        this.voucherMapper = voucherMapper;
        this.seckillVoucherMapper = seckillVoucherMapper;
        this.checkoutItemService = checkoutItemService;
    }

    @Transactional
    public void addToCart(Long userId, Long voucherId, Integer quantity) {
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.LOGIN_REQUIRED);
        }
        if (voucherId == null || voucherId <= 0) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_PARAM_INVALID);
        }
        Voucher voucher = voucherMapper.findById(voucherId);
        if (voucher == null || voucher.getStatus() == null || voucher.getStatus() != 1) {
            throw new BusinessException(BusinessErrorCode.VOUCHER_NOT_AVAILABLE);
        }
        if (isSeckillVoucher(voucherId)) {
            throw new BusinessException(BusinessErrorCode.CART_SECKILL_NOT_ALLOWED);
        }
        int safeQuantity = normalizeQuantity(quantity);

        ShoppingCartItem existing = shoppingCartMapper.findByUserAndVoucher(userId, voucherId);
        if (existing != null) {
            shoppingCartMapper.updateQuantityAndSelectedById(existing.getId(), userId, safeQuantity, 1);
            return;
        }

        ShoppingCartItem item = new ShoppingCartItem();
        item.setUserId(userId);
        item.setVoucherId(voucherId);
        item.setQuantity(safeQuantity);
        item.setSelected(1);
        shoppingCartMapper.insert(item);
    }

    public List<Map<String, Object>> getCartViews(Long userId) {
        List<Map<String, Object>> list = shoppingCartMapper.findViewsByUser(userId);
        list.forEach(this::appendComputedFields);
        return list;
    }

    @Transactional
    public void updateCartItem(Long userId, Long id, Integer quantity, Integer selected) {
        if (id == null || id <= 0) {
            throw new BusinessException(BusinessErrorCode.CART_ITEM_INVALID);
        }
        int safeQuantity = normalizeQuantity(quantity);
        int safeSelected = selected != null && selected == 0 ? 0 : 1;
        int updated = shoppingCartMapper.updateQuantityAndSelectedById(id, userId, safeQuantity, safeSelected);
        if (updated <= 0) {
            throw new BusinessException(BusinessErrorCode.CART_ITEM_NOT_FOUND);
        }
    }

    @Transactional
    public void updateCartItemSelected(Long userId, Long id, Integer selected) {
        if (id == null || id <= 0) {
            throw new BusinessException(BusinessErrorCode.CART_ITEM_INVALID);
        }
        int safeSelected = selected != null && selected == 0 ? 0 : 1;
        int updated = shoppingCartMapper.updateSelectedById(id, userId, safeSelected);
        if (updated <= 0) {
            throw new BusinessException(BusinessErrorCode.CART_ITEM_NOT_FOUND);
        }
    }

    @Transactional
    public void removeCartItem(Long userId, Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(BusinessErrorCode.CART_ITEM_INVALID);
        }
        shoppingCartMapper.deleteById(id, userId);
    }

    @Transactional
    public void clearCart(Long userId) {
        shoppingCartMapper.clearByUser(userId);
    }

    public Map<String, Object> checkout(Long userId, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.CART_EMPTY);
        }
        List<ShoppingCartItem> items = shoppingCartMapper.findByIds(userId, itemIds);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.CART_ITEM_NOT_FOUND);
        }

        List<Map<String, Object>> successOrders = new ArrayList<>();
        List<Map<String, Object>> failedItems = new ArrayList<>();

        for (ShoppingCartItem item : items) {
            try {
                successOrders.add(checkoutItemService.checkoutOne(userId, item));
            } catch (BusinessException e) {
                failedItems.add(failItem(item, e.getMessage()));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successOrders", successOrders);
        result.put("failedItems", failedItems);
        result.put("successCount", successOrders.size());
        result.put("failedCount", failedItems.size());
        return result;
    }

    private Map<String, Object> failItem(ShoppingCartItem item, String reason) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("cartItemId", item == null ? null : item.getId());
        fail.put("voucherId", item == null ? null : item.getVoucherId());
        fail.put("reason", reason == null || reason.isBlank() ? "结算失败" : reason);
        return fail;
    }

    private int normalizeQuantity(Integer quantity) {
        return 1;
    }

    private boolean isSeckillVoucher(Long voucherId) {
        if (voucherId == null || voucherId <= 0) {
            return false;
        }
        SeckillVoucher seckillVoucher = seckillVoucherMapper.findByVoucherId(voucherId);
        return seckillVoucher != null;
    }

    private void appendComputedFields(Map<String, Object> item) {
        item.put("quantity", 1);
        Number quantityNum = asNumber(item.get("quantity"));
        Number priceNum = asNumber(item.get("price"));
        int quantity = quantityNum == null ? 1 : quantityNum.intValue();
        double unitPrice = priceNum == null ? 0D : priceNum.doubleValue();
        item.put("subtotal", unitPrice * quantity);

        Object endTimeObj = item.get("seckillEndTime");
        if (endTimeObj != null) {
            LocalDateTime endTime = parseTime(endTimeObj);
            if (endTime != null) {
                long seconds = Math.max(Duration.between(LocalDateTime.now(), endTime).getSeconds(), 0);
                item.put("countdownSeconds", seconds);
            }
        }
    }

    private Number asNumber(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        try {
            return value == null ? null : Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value).replace(" ", "T"));
        } catch (Exception ignored) {
            return null;
        }
    }
}
