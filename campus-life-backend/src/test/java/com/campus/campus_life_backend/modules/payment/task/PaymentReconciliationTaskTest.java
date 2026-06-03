package com.campus.campus_life_backend.modules.payment.task;

import com.campus.campus_life_backend.modules.payment.service.PaymentReconciliationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationTaskTest {

    @Mock
    private PaymentReconciliationService paymentReconciliationService;

    @InjectMocks
    private PaymentReconciliationTask paymentReconciliationTask;

    @Test
    void shouldUseTaskSpecificScanEntry() {
        ReflectionTestUtils.setField(paymentReconciliationTask, "reconciliationDays", 3);
        ReflectionTestUtils.setField(paymentReconciliationTask, "batchSize", 200);
        when(paymentReconciliationService.scanRecentIssuesForTask(3, 200))
                .thenReturn(Map.of("scannedCount", 0));

        paymentReconciliationTask.scanIssues();

        verify(paymentReconciliationService).scanRecentIssuesForTask(3, 200);
        verify(paymentReconciliationService, never()).scanRecentIssues(anyInt(), anyInt());
    }
}
