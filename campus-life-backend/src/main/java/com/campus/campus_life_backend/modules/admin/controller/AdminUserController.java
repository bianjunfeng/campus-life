package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.modules.admin.service.UserAdminOrchestrationService;
import com.campus.campus_life_backend.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRole(RoleCode.ADMIN)
public class AdminUserController {

    private final UserAdminOrchestrationService userAdminService;
    private final AdminAuthUtil adminAuthUtil;
    private final CurrentUserAccessor currentUserAccessor;
    private final AuthService authService;

    public AdminUserController(UserAdminOrchestrationService userAdminService,
                               AdminAuthUtil adminAuthUtil,
                               CurrentUserAccessor currentUserAccessor,
                               AuthService authService) {
        this.userAdminService = userAdminService;
        this.adminAuthUtil = adminAuthUtil;
        this.currentUserAccessor = currentUserAccessor;
        this.authService = authService;
    }

    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> getUserList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "role", required = false) Integer role,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(userAdminService.getUserList(page, size, role, status, keyword));
    }

    @GetMapping("/system/users")
    public ApiResponse<Map<String, Object>> getSystemUserList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "role", required = false) Integer role,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(userAdminService.getUserListAdvanced(page, size, keyword, username, phone, email, role, status, startDate, endDate));
    }

    @GetMapping("/members")
    public ApiResponse<Map<String, Object>> getMemberList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "role", required = false) Integer role,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(userAdminService.getMemberListPage(page, size, keyword, username, phone, email, role, status, startDate, endDate));
    }

    @GetMapping("/student-auth/requests")
    public ApiResponse<Map<String, Object>> getStudentAuthRequests(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(userAdminService.getStudentAuthList(page, size, status, keyword));
    }

    @PostMapping("/student-auth/{requestId}/approve")
    public ApiResponse<Void> approveStudentAuth(@PathVariable Long requestId,
                                                @RequestBody(required = false) Map<String, String> request,
                                                HttpServletRequest httpRequest) {
        userAdminService.approveStudentAuth(currentUserAccessor.requireUserId(), requestId,
                request == null ? null : request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/student-auth/{requestId}/reject")
    public ApiResponse<Void> rejectStudentAuth(@PathVariable Long requestId,
                                               @RequestBody(required = false) Map<String, String> request,
                                               HttpServletRequest httpRequest) {
        userAdminService.rejectStudentAuth(currentUserAccessor.requireUserId(), requestId,
                request == null ? null : request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/users/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long userId, @RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        userAdminService.updateUserStatus(currentUserAccessor.requireUserId(), userId,
                (Integer) request.get("status"), (String) request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/users/{userId}/force-logout")
    public ApiResponse<Map<String, Object>> forceLogoutUser(@PathVariable Long userId) {
        currentUserAccessor.requireUserId();
        authService.forceLogoutUser(userId);
        return ApiResponse.success(Map.of("userId", userId, "forced", true));
    }

    @PutMapping("/system/users/{userId}/status")
    public ApiResponse<Void> updateSystemUserStatus(@PathVariable Long userId, @RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        userAdminService.updateUserStatus(currentUserAccessor.requireUserId(), userId,
                (Integer) request.get("status"), (String) request.getOrDefault("reason", "管理员更新用户状态"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/system/users/{userId}")
    public ApiResponse<Void> deleteSystemUser(@PathVariable Long userId, HttpServletRequest httpRequest) {
        userAdminService.deleteUser(currentUserAccessor.requireUserId(), userId, adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @GetMapping("/system/users/stats")
    public ApiResponse<Map<String, Object>> getSystemUserStats() {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(userAdminService.getUserStats());
    }
}
