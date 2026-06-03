package com.campus.campus_life_backend.modules.admin.service;

import com.campus.campus_life_backend.modules.admin.entity.AdminOperationLog;
import com.campus.campus_life_backend.modules.admin.mapper.AdminOperationLogMapper;
import com.campus.campus_life_backend.modules.voucher.service.AdminTradeQueryService;
import com.campus.campus_life_backend.modules.voucher.service.VoucherSeckillService;
import com.campus.campus_life_backend.modules.voucher.service.VoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class VoucherAdminOrchestrationService {

    private final AdminTradeQueryService tradeQueryService;
    private final VoucherService voucherService;
    private final VoucherSeckillService voucherSeckillService;
    private final AdminOperationLogMapper operationLogMapper;

    public VoucherAdminOrchestrationService(
            AdminTradeQueryService tradeQueryService,
            VoucherService voucherService,
            VoucherSeckillService voucherSeckillService,
            AdminOperationLogMapper operationLogMapper) {
        this.tradeQueryService = tradeQueryService;
        this.voucherService = voucherService;
        this.voucherSeckillService = voucherSeckillService;
        this.operationLogMapper = operationLogMapper;
    }

    public Map<String, Object> getVoucherList(Integer page, Integer size, Integer status, Long merchantId, String keyword) {
        return tradeQueryService.getVoucherList(page, size, status, merchantId, keyword);
    }

    public Map<String, Object> getVoucherOrderList(Integer page, Integer size, String keyword, Long userId, Long voucherId,
                                                   Integer status, Integer paymentStatus, String orderSource,
                                                   String timeRange, String startDate, String endDate) {
        return tradeQueryService.getVoucherOrderList(page, size, keyword, userId, voucherId, status, paymentStatus, orderSource, timeRange, startDate, endDate);
    }

    public Map<String, Object> getVoucherOrderStats(String keyword, Long userId, Long voucherId, Integer status,
                                                    Integer paymentStatus, String orderSource, String timeRange,
                                                    String startDate, String endDate) {
        return tradeQueryService.getVoucherOrderStats(keyword, userId, voucherId, status, paymentStatus, orderSource, timeRange, startDate, endDate);
    }

    @Transactional
    public Map<String, Object> createVoucher(Long adminId, Map<String, Object> request, String ipAddress) {
        VoucherService.AdminVoucherCreateResult created = voucherService.adminCreateVoucher(request);
        logOperation(adminId, "voucher_manage", "voucher", created.id(), "create", null, created.status(), "管理员创建优惠券", ipAddress);
        Map<String, Object> result = new HashMap<>();
        result.put("id", created.id());
        result.put("title", created.title());
        return result;
    }

    @Transactional
    public void updateVoucher(Long adminId, Long voucherId, Map<String, Object> request, String ipAddress) {
        VoucherService.AdminVoucherUpdateResult change = voucherService.adminUpdateVoucher(voucherId, request);
        logOperation(adminId, "voucher_manage", "voucher", voucherId, "update", change.oldStatus(), change.newStatus(), "管理员更新优惠券", ipAddress);
    }

    @Transactional
    public void offlineVoucher(Long adminId, Long voucherId, String reason, String ipAddress) {
        VoucherService.AdminVoucherStatusChange change = voucherService.adminUpdateVoucherStatus(voucherId, 0);
        logOperation(adminId, "voucher_manage", "voucher", voucherId, "offline", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void violationOfflineVoucher(Long adminId, Long voucherId, String reason, String ipAddress) {
        VoucherService.AdminVoucherStatusChange change = voucherService.adminUpdateVoucherStatus(voucherId, 0);
        logOperation(adminId, "voucher_manage", "voucher", voucherId, "violation_offline", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void onlineVoucher(Long adminId, Long voucherId, String ipAddress) {
        VoucherService.AdminVoucherStatusChange change = voucherService.adminUpdateVoucherStatus(voucherId, 1);
        logOperation(adminId, "voucher_manage", "voucher", voucherId, "online", change.oldStatus(), change.newStatus(), "管理员上架优惠券", ipAddress);
    }

    public void preheatSeckillVoucher(Long voucherId) {
        voucherSeckillService.preheatSeckillVoucher(voucherId);
    }

    public Map<String, Object> getSeckillMonitor(Long voucherId) {
        return voucherSeckillService.getSeckillMonitor(voucherId);
    }

    private void logOperation(Long adminId, String operationType, String targetType, Long targetId, String action,
                              Integer oldStatus, Integer newStatus, String reason, String ipAddress) {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(adminId);
        log.setOperationType(operationType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAction(action);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setReason(reason);
        log.setIpAddress(ipAddress);
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
