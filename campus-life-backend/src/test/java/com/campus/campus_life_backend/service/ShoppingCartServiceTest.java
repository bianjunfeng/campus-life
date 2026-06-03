package com.campus.campus_life_backend.service;

import com.campus.campus_life_backend.modules.voucher.entity.ShoppingCartItem;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.voucher.entity.SeckillVoucher;
import com.campus.campus_life_backend.modules.voucher.mapper.SeckillVoucherMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.ShoppingCartMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import com.campus.campus_life_backend.modules.voucher.service.ShoppingCartService;
import com.campus.campus_life_backend.modules.voucher.service.VoucherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {

    @Mock
    private ShoppingCartMapper shoppingCartMapper;

    @Mock
    private VoucherMapper voucherMapper;

    @Mock
    private SeckillVoucherMapper seckillVoucherMapper;

    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private ShoppingCartService shoppingCartService;

    @Test
    void shouldKeepSingleQuantityWhenAddingExistingVoucherAgain() {
        Voucher voucher = new Voucher();
        voucher.setId(5L);
        voucher.setStatus(1);

        ShoppingCartItem existing = new ShoppingCartItem();
        existing.setId(11L);
        existing.setQuantity(3);

        when(voucherMapper.findById(5L)).thenReturn(voucher);
        when(seckillVoucherMapper.findByVoucherId(5L)).thenReturn(null);
        when(shoppingCartMapper.findByUserAndVoucher(1L, 5L)).thenReturn(existing);

        shoppingCartService.addToCart(1L, 5L, 4);

        verify(shoppingCartMapper).updateQuantityAndSelectedById(11L, 1L, 1, 1);
    }

    @Test
    void shouldIgnoreRequestedQuantityWhenUpdatingCartItem() {
        when(shoppingCartMapper.updateQuantityAndSelectedById(9L, 1L, 1, 0)).thenReturn(1);

        shoppingCartService.updateCartItem(1L, 9L, 7, 0);

        verify(shoppingCartMapper).updateQuantityAndSelectedById(9L, 1L, 1, 0);
    }

    @Test
    void shouldNormalizeLegacyQuantityInCartViews() {
        Map<String, Object> raw = new java.util.HashMap<>();
        raw.put("id", 1L);
        raw.put("quantity", 4);
        raw.put("price", new BigDecimal("19.90"));

        when(shoppingCartMapper.findViewsByUser(1L)).thenReturn(List.of(raw));

        List<Map<String, Object>> views = shoppingCartService.getCartViews(1L);

        assertEquals(1, ((Number) views.get(0).get("quantity")).intValue());
        assertEquals(19.9D, ((Number) views.get(0).get("subtotal")).doubleValue());
    }

    @Test
    void shouldRejectSeckillVoucherWhenAddingToCart() {
        Voucher voucher = new Voucher();
        voucher.setId(5L);
        voucher.setStatus(1);

        when(voucherMapper.findById(5L)).thenReturn(voucher);
        when(seckillVoucherMapper.findByVoucherId(5L)).thenReturn(new SeckillVoucher());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> shoppingCartService.addToCart(1L, 5L, 1));

        assertEquals("秒杀券不支持加入购物车，请直接抢购下单", ex.getMessage());
        verify(shoppingCartMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectSeckillVoucherWhenCheckingOutCart() {
        ShoppingCartItem item = new ShoppingCartItem();
        item.setId(9L);
        item.setVoucherId(5L);

        when(shoppingCartMapper.findByIds(1L, List.of(9L))).thenReturn(List.of(item));
        when(seckillVoucherMapper.findByVoucherId(5L)).thenReturn(new SeckillVoucher());

        Map<String, Object> result = shoppingCartService.checkout(1L, List.of(9L));

        assertEquals(0, ((List<?>) result.get("successOrders")).size());
        assertEquals(1, ((List<?>) result.get("failedItems")).size());
        @SuppressWarnings("unchecked")
        Map<String, Object> failedItem = (Map<String, Object>) ((List<?>) result.get("failedItems")).get(0);
        assertEquals("秒杀券不支持购物车结算，请直接抢购下单", failedItem.get("reason"));
        verify(voucherService, never()).claimVoucher(5L, 1L);
    }
}
