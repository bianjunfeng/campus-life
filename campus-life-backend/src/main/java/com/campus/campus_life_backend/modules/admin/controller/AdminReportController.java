package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.modules.admin.service.ReportAdminOrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRole(RoleCode.ADMIN)
public class AdminReportController {

    private final ReportAdminOrchestrationService reportAdminService;
    private final AdminAuthUtil adminAuthUtil;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminReportController(ReportAdminOrchestrationService reportAdminService,
                                 AdminAuthUtil adminAuthUtil,
                                 CurrentUserAccessor currentUserAccessor) {
        this.reportAdminService = reportAdminService;
        this.adminAuthUtil = adminAuthUtil;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/reports")
    public ApiResponse<Map<String, Object>> getReportList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "targetType", required = false) Integer targetType,
            @RequestParam(value = "keyword", required = false) String keyword) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(reportAdminService.getReportList(page, size, status, targetType, keyword));
    }

    @PostMapping("/reports/{reportId}/process")
    public ApiResponse<Void> processReport(@PathVariable Long reportId, @RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Integer status = request.get("status") instanceof Number ? ((Number) request.get("status")).intValue() : null;
        String handleResult = request.get("handleResult") == null ? "" : String.valueOf(request.get("handleResult"));
        String action = request.get("action") == null ? "NONE" : String.valueOf(request.get("action"));
        reportAdminService.processReport(currentUserAccessor.requireUserId(), reportId, status, handleResult, action, adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }
}
