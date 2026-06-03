package com.campus.campus_life_backend.modules.voucher.task;

import com.campus.campus_life_backend.modules.payment.service.PaymentAutoRefundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VoucherOrderAutoRefundTask {

    private static final Logger log = LoggerFactory.getLogger(VoucherOrderAutoRefundTask.class);

    private final PaymentAutoRefundService paymentAutoRefundService;

    @Value("${voucher.order.auto-refund-expired.batch-size:50}")
    private Integer batchSize;

    public VoucherOrderAutoRefundTask(PaymentAutoRefundService paymentAutoRefundService) {
        this.paymentAutoRefundService = paymentAutoRefundService;
    }

    @Scheduled(fixedDelayString = "${voucher.order.auto-refund-expired.fixed-delay-ms:60000}")
    public void refundExpiredPaidOrders() {
        try {
            int safeBatchSize = batchSize == null || batchSize <= 0 ? 50 : batchSize;
            int refunded = paymentAutoRefundService.autoRefundExpiredVoucherOrders(safeBatchSize);
            if (refunded > 0) {
                log.info("到期券自动退款完成，退款数量={}", refunded);
            }
        } catch (Exception e) {
            log.error("到期券自动退款任务执行失败", e);
        }
    }
}
