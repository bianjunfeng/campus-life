package com.campus.campus_life_backend.service;

import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.voucher.entity.SeckillVoucher;
import com.campus.campus_life_backend.modules.voucher.entity.ShoppingCartItem;
import com.campus.campus_life_backend.modules.voucher.mapper.SeckillVoucherMapper;
import com.campus.campus_life_backend.modules.voucher.mapper.ShoppingCartMapper;
import com.campus.campus_life_backend.modules.voucher.service.ShoppingCartCheckoutItemService;
import com.campus.campus_life_backend.modules.voucher.service.VoucherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingCartCheckoutItemServiceTest {

    @Mock
    private ShoppingCartMapper shoppingCartMapper;

    @Mock
    private SeckillVoucherMapper seckillVoucherMapper;

    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private ShoppingCartCheckoutItemService checkoutItemService;

    @Test
    void shouldCreateOrderAndDeleteCartItem() {
        ShoppingCartItem item = cartItem();
        VoucherOrder order = new VoucherOrder();
        order.setOrderNo("VO123");
        order.setPayAmount(new BigDecimal("9.90"));

        when(seckillVoucherMapper.findByVoucherId(5L)).thenReturn(null);
        when(voucherService.claimVoucher(5L, 1L)).thenReturn(order);
        when(shoppingCartMapper.deleteById(9L, 1L)).thenReturn(1);

        Map<String, Object> result = checkoutItemService.checkoutOne(1L, item);

        assertEquals(9L, result.get("cartItemId"));
        assertEquals(5L, result.get("voucherId"));
        assertEquals("VO123", result.get("orderNo"));
        assertEquals(new BigDecimal("9.90"), result.get("payAmount"));
    }

    @Test
    void shouldRejectSeckillVoucherBeforeCreatingOrder() {
        ShoppingCartItem item = cartItem();
        when(seckillVoucherMapper.findByVoucherId(5L)).thenReturn(new SeckillVoucher());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> checkoutItemService.checkoutOne(1L, item));

        assertEquals("秒杀券不支持购物车结算，请直接抢购下单", ex.getMessage());
        verify(voucherService, never()).claimVoucher(5L, 1L);
        verify(shoppingCartMapper, never()).deleteById(9L, 1L);
    }

    @Test
    void shouldFailWhenCartItemCannotBeDeletedAfterOrderCreated() {
        ShoppingCartItem item = cartItem();
        VoucherOrder order = new VoucherOrder();
        order.setOrderNo("VO123");
        order.setPayAmount(new BigDecimal("9.90"));

        when(seckillVoucherMapper.findByVoucherId(5L)).thenReturn(null);
        when(voucherService.claimVoucher(5L, 1L)).thenReturn(order);
        when(shoppingCartMapper.deleteById(9L, 1L)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> checkoutItemService.checkoutOne(1L, item));

        assertEquals("购物车项不存在", ex.getMessage());
    }

    private ShoppingCartItem cartItem() {
        ShoppingCartItem item = new ShoppingCartItem();
        item.setId(9L);
        item.setVoucherId(5L);
        return item;
    }
}
