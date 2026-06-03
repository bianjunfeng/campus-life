package com.campus.campus_life_backend.modules.admin.service;

import com.campus.campus_life_backend.modules.admin.entity.AdminOperationLog;
import com.campus.campus_life_backend.modules.admin.mapper.AdminOperationLogMapper;
import com.campus.campus_life_backend.modules.merchant.service.MerchantAuthService;
import com.campus.campus_life_backend.modules.merchant.service.MerchantManagementService;
import com.campus.campus_life_backend.modules.merchant.service.MerchantQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class MerchantAdminOrchestrationService {

    private final MerchantQueryService merchantQueryService;
    private final MerchantAuthService merchantAuthService;
    private final MerchantManagementService merchantManagementService;
    private final AdminOperationLogMapper operationLogMapper;

    public MerchantAdminOrchestrationService(
            MerchantQueryService merchantQueryService,
            MerchantAuthService merchantAuthService,
            MerchantManagementService merchantManagementService,
            AdminOperationLogMapper operationLogMapper) {
        this.merchantQueryService = merchantQueryService;
        this.merchantAuthService = merchantAuthService;
        this.merchantManagementService = merchantManagementService;
        this.operationLogMapper = operationLogMapper;
    }

    public Map<String, Object> getMerchantList(Integer page, Integer size, Integer status, String keyword) {
        return merchantQueryService.getAdminMerchantList(page, size, status, keyword);
    }

    @Transactional
    public Map<String, Object> createMerchant(Long adminId, Map<String, Object> request, String ipAddress) {
        MerchantManagementService.AdminMerchantCreateResult created = merchantManagementService.adminCreateMerchant(request);
        logOperation(adminId, "merchant_manage", "merchant", created.id(), "create", null, created.status(), "管理员创建商家", ipAddress);
        Map<String, Object> result = new HashMap<>();
        result.put("id", created.id());
        result.put("name", created.name());
        return result;
    }

    @Transactional
    public void updateMerchant(Long adminId, Long merchantId, Map<String, Object> request, String ipAddress) {
        MerchantManagementService.AdminMerchantUpdateResult change = merchantManagementService.adminUpdateMerchant(merchantId, request);
        logOperation(adminId, "merchant_manage", "merchant", merchantId, "update", change.oldStatus(), change.newStatus(), "管理员更新商家", ipAddress);
    }

    @Transactional
    public void approveMerchant(Long adminId, Long merchantId, String reason, String ipAddress) {
        MerchantAuthService.AdminMerchantReviewResult change = merchantAuthService.adminApproveMerchant(merchantId, adminId, reason);
        logOperation(adminId, "merchant_manage", "merchant", merchantId, "approve", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void rejectMerchant(Long adminId, Long merchantId, String reason, String ipAddress) {
        MerchantAuthService.AdminMerchantReviewResult change = merchantAuthService.adminRejectMerchant(merchantId, adminId, reason);
        logOperation(adminId, "merchant_manage", "merchant", merchantId, "reject", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void freezeMerchant(Long adminId, Long merchantId, String reason, String ipAddress) {
        MerchantManagementService.AdminMerchantUpdateResult change = merchantManagementService.adminUpdateStatus(merchantId, 2);
        logOperation(adminId, "merchant_manage", "merchant", merchantId, "freeze", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void unfreezeMerchant(Long adminId, Long merchantId, String reason, String ipAddress) {
        MerchantManagementService.AdminMerchantUpdateResult change = merchantManagementService.adminUpdateStatus(merchantId, 1);
        logOperation(adminId, "merchant_manage", "merchant", merchantId, "unfreeze", change.oldStatus(), change.newStatus(), reason, ipAddress);
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
