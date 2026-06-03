package com.campus.campus_life_backend.modules.voucher.task;

import com.campus.campus_life_backend.modules.voucher.service.VoucherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherAutoOfflineTaskTest {

    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private VoucherAutoOfflineTask voucherAutoOfflineTask;

    @Test
    void shouldUseConfiguredBatchSizeWhenOfflineExpiredVouchers() {
        ReflectionTestUtils.setField(voucherAutoOfflineTask, "batchSize", 30);
        when(voucherService.autoOfflineExpiredVouchers(30)).thenReturn(1);

        voucherAutoOfflineTask.offlineExpiredVouchers();

        verify(voucherService).autoOfflineExpiredVouchers(30);
    }
}
