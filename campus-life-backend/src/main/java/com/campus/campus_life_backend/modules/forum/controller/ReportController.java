package com.campus.campus_life_backend.modules.forum.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.forum.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/forum/reports")
public class ReportController {

    private final CurrentUserAccessor currentUserAccessor;
    private final ReportService reportService;

    public ReportController(CurrentUserAccessor currentUserAccessor,
                            ReportService reportService) {
        this.currentUserAccessor = currentUserAccessor;
        this.reportService = reportService;
    }

    @PostMapping
    @RequirePermission(anyOf = {"report:create"})
    public ApiResponse<Map<String, Object>> createReport(@RequestBody Map<String, Object> request) {
        Long reporterId = currentUserAccessor.requireUserId();

        Integer targetType = parseInt(request.get("targetType"));
        Long targetId = parseLong(request.get("targetId"));
        String reason = request.get("reason") == null ? "" : String.valueOf(request.get("reason")).trim();

        return ApiResponse.success(reportService.createReport(reporterId, targetType, targetId, reason));
    }

    private Integer parseInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
