package com.campus.campus_life_backend.modules.voucher.service;

import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.order.mapper.VoucherOrderMapper;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceExpiredOfflineTest {

    @Mock
    private VoucherMapper voucherMapper;

    @Mock
    private VoucherOrderMapper voucherOrderMapper;

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private VoucherOrderService voucherOrderService;

    @Mock
    private WelfareQueryService welfareQueryService;

    private VoucherService voucherService;

    @BeforeEach
    void setUp() {
        voucherService = new VoucherService(
                voucherMapper,
                voucherOrderMapper,
                merchantMapper,
                voucherOrderService,
                welfareQueryService
        );
    }

    @Test
    void shouldOfflineExpiredVouchersAndEvictWelfareCache() {
        when(voucherMapper.markExpiredAsOffline(50)).thenReturn(2);

        int offlineCount = voucherService.autoOfflineExpiredVouchers(50);

        assertEquals(2, offlineCount);
        verify(voucherMapper).markExpiredAsOffline(50);
        verify(welfareQueryService).evictWelfareCache();
    }

    @Test
    void shouldNotEvictWelfareCacheWhenNoExpiredVoucherChanged() {
        when(voucherMapper.markExpiredAsOffline(100)).thenReturn(0);

        int offlineCount = voucherService.autoOfflineExpiredVouchers(0);

        assertEquals(0, offlineCount);
        verify(voucherMapper).markExpiredAsOffline(100);
        verify(welfareQueryService, never()).evictWelfareCache();
    }
}
