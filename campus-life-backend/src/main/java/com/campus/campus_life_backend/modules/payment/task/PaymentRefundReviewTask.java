package com.campus.campus_life_backend.modules.payment.task;

import com.campus.campus_life_backend.modules.payment.service.PaymentRefundWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentRefundReviewTask {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundReviewTask.class);

    private final PaymentRefundWorkflowService paymentRefundWorkflowService;

    @Value("${payment.refund.review-timeout.batch-size:100}")
    private Integer batchSize;

    public PaymentRefundReviewTask(PaymentRefundWorkflowService paymentRefundWorkflowService) {
        this.paymentRefundWorkflowService = paymentRefundWorkflowService;
    }

    @Scheduled(fixedDelayString = "${payment.refund.review-timeout.fixed-delay-ms:60000}")
    public void escalateTimeoutMerchantReviews() {
        try {
            int safeBatchSize = batchSize == null || batchSize <= 0 ? 100 : batchSize;
            int escalated = paymentRefundWorkflowService.autoEscalateTimeoutMerchantReviews(safeBatchSize);
            if (escalated > 0) {
                log.info("退款审核超时自动升级完成，升级数量={}", escalated);
            }
        } catch (Exception e) {
            log.error("退款审核超时自动升级任务执行失败", e);
        }
    }
}
