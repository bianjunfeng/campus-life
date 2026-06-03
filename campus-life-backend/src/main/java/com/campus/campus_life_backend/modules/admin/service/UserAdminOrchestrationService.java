package com.campus.campus_life_backend.modules.admin.service;

import com.campus.campus_life_backend.modules.admin.entity.AdminOperationLog;
import com.campus.campus_life_backend.modules.admin.mapper.AdminOperationLogMapper;
import com.campus.campus_life_backend.modules.user.service.AdminUserQueryService;
import com.campus.campus_life_backend.modules.user.service.AuthRequestService;
import com.campus.campus_life_backend.modules.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class UserAdminOrchestrationService {

    private final AdminUserQueryService userQueryService;
    private final UserService userService;
    private final AuthRequestService authRequestService;
    private final AdminOperationLogMapper operationLogMapper;

    public UserAdminOrchestrationService(
            AdminUserQueryService userQueryService,
            UserService userService,
            AuthRequestService authRequestService,
            AdminOperationLogMapper operationLogMapper) {
        this.userQueryService = userQueryService;
        this.userService = userService;
        this.authRequestService = authRequestService;
        this.operationLogMapper = operationLogMapper;
    }

    public Map<String, Object> getUserList(Integer page, Integer size, Integer role, Integer status, String keyword) {
        return userQueryService.getUserList(page, size, role, status, keyword);
    }

    public Map<String, Object> getUserListAdvanced(Integer page, Integer size, String keyword, String username, String phone,
                                                   String email, Integer role, Integer status, String startDate, String endDate) {
        return userQueryService.getUserListAdvanced(page, size, keyword, username, phone, email, role, status, startDate, endDate);
    }

    public Map<String, Object> getMemberListPage(Integer page, Integer size, String keyword, String username, String phone,
                                                 String email, Integer role, Integer status, String startDate, String endDate) {
        return userQueryService.getMemberListPage(page, size, keyword, username, phone, email, role, status, startDate, endDate);
    }

    public Map<String, Object> getUserStats() {
        return userQueryService.getUserStats();
    }

    public Map<String, Object> getStudentAuthList(Integer page, Integer size, Integer status, String keyword) {
        return userQueryService.getStudentAuthList(page, size, status, keyword);
    }

    @Transactional
    public void updateUserStatus(Long adminId, Long userId, Integer status, String reason, String ipAddress) {
        UserService.AdminUserStatusChange change = userService.adminUpdateStatus(userId, status);
        logOperation(adminId, "user_manage", "user", userId, "update_status", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void deleteUser(Long adminId, Long userId, String ipAddress) {
        UserService.AdminUserStatusChange change = userService.adminUpdateStatus(userId, 0);
        logOperation(adminId, "user_manage", "user", userId, "delete", change.oldStatus(), change.newStatus(), "管理员删除用户（软删除）", ipAddress);
    }

    @Transactional
    public void approveStudentAuth(Long adminId, Long requestId, String reason, String ipAddress) {
        AuthRequestService.AdminStudentAuthReviewResult change = authRequestService.adminApproveStudentAuth(requestId, adminId, reason);
        logOperation(adminId, "student_auth", "student_auth_request", requestId, "approve", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void rejectStudentAuth(Long adminId, Long requestId, String reason, String ipAddress) {
        AuthRequestService.AdminStudentAuthReviewResult change = authRequestService.adminRejectStudentAuth(requestId, adminId, reason);
        logOperation(adminId, "student_auth", "student_auth_request", requestId, "reject", change.oldStatus(), change.newStatus(), reason, ipAddress);
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
