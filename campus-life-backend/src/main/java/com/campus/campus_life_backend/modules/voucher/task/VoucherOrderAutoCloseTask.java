package com.campus.campus_life_backend.modules.voucher.task;

import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VoucherOrderAutoCloseTask {

    private static final Logger log = LoggerFactory.getLogger(VoucherOrderAutoCloseTask.class);

    private final VoucherOrderService voucherOrderService;

    @Value("${voucher.order.auto-close.batch-size:100}")
    private Integer batchSize;

    public VoucherOrderAutoCloseTask(VoucherOrderService voucherOrderService) {
        this.voucherOrderService = voucherOrderService;
    }

    @Scheduled(fixedDelayString = "${voucher.order.auto-close.fixed-delay-ms:15000}")
    public void closeExpiredPendingOrders() {
        try {
            int safeBatchSize = batchSize == null || batchSize <= 0 ? 100 : batchSize;
            int closed = voucherOrderService.closeExpiredPendingOrders(safeBatchSize);
            if (closed > 0) {
                log.info("未支付超时订单自动关闭完成，关闭数量={}", closed);
            }
        } catch (Exception e) {
            log.error("未支付超时订单自动关闭任务执行失败", e);
        }
    }
}
