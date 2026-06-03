package com.campus.campus_life_backend.modules.voucher.task;

import com.campus.campus_life_backend.modules.voucher.service.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VoucherAutoOfflineTask {

    private static final Logger log = LoggerFactory.getLogger(VoucherAutoOfflineTask.class);

    private final VoucherService voucherService;

    @Value("${voucher.auto-offline-expired.batch-size:100}")
    private Integer batchSize;

    public VoucherAutoOfflineTask(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @Scheduled(fixedDelayString = "${voucher.auto-offline-expired.fixed-delay-ms:15000}")
    public void offlineExpiredVouchers() {
        try {
            int safeBatchSize = batchSize == null || batchSize <= 0 ? 100 : batchSize;
            int offlineCount = voucherService.autoOfflineExpiredVouchers(safeBatchSize);
            if (offlineCount > 0) {
                log.info("到期优惠券自动下架完成，下架数量={}", offlineCount);
            }
        } catch (Exception e) {
            log.error("到期优惠券自动下架任务执行失败", e);
        }
    }
}
