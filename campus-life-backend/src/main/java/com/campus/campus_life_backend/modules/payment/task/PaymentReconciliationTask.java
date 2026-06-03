package com.campus.campus_life_backend.modules.payment.task;

import com.campus.campus_life_backend.modules.payment.service.PaymentReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "payment.reconciliation.task", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentReconciliationTask {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationTask.class);

    private final PaymentReconciliationService paymentReconciliationService;

    @Value("${payment.reconciliation.days:7}")
    private Integer reconciliationDays;

    @Value("${payment.reconciliation.batch-size:500}")
    private Integer batchSize;

    public PaymentReconciliationTask(PaymentReconciliationService paymentReconciliationService) {
        this.paymentReconciliationService = paymentReconciliationService;
    }

    @Scheduled(fixedDelayString = "${payment.reconciliation.fixed-delay-ms:1800000}")
    public void scanIssues() {
        try {
            Map<String, Object> result = paymentReconciliationService.scanRecentIssuesForTask(
                    reconciliationDays == null ? 7 : reconciliationDays,
                    batchSize == null ? 500 : batchSize
            );
            log.info("支付对账扫描完成: {}", result);
        } catch (Exception e) {
            log.error("支付对账扫描任务执行失败", e);
        }
    }
}
