package com.campus.campus_life_backend.modules.admin.service;

import com.campus.campus_life_backend.modules.admin.entity.AdminOperationLog;
import com.campus.campus_life_backend.modules.admin.mapper.AdminOperationLogMapper;
import com.campus.campus_life_backend.modules.forum.service.AdminCommunityQueryService;
import com.campus.campus_life_backend.modules.forum.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ReportAdminOrchestrationService {

    private final AdminCommunityQueryService communityQueryService;
    private final ReportService reportService;
    private final AdminOperationLogMapper operationLogMapper;

    public ReportAdminOrchestrationService(
            AdminCommunityQueryService communityQueryService,
            ReportService reportService,
            AdminOperationLogMapper operationLogMapper) {
        this.communityQueryService = communityQueryService;
        this.reportService = reportService;
        this.operationLogMapper = operationLogMapper;
    }

    public Map<String, Object> getReportList(Integer page, Integer size, Integer status, Integer targetType, String keyword) {
        return communityQueryService.getReportList(page, size, status, targetType, keyword);
    }

    @Transactional
    public void processReport(Long adminId, Long reportId, Integer status, String handleResult, String action, String ipAddress) {
        ReportService.AdminReportProcessResult result = reportService.adminProcessReport(reportId, status, adminId, handleResult, action);
        logOperation(adminId, "report_manage", "report", reportId, "process", result.oldStatus(), result.newStatus(),
                "[action=" + result.action() + "] " + result.handleResult(), ipAddress);
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
