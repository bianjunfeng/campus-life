package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.admin.dto.AdminApiInfoDTO;
import com.campus.campus_life_backend.modules.admin.service.AdminApiCatalogService;
import com.campus.campus_life_backend.modules.admin.service.AdminDashboardQueryService;
import com.campus.campus_life_backend.modules.admin.service.AdminOperationLogQueryService;
import com.campus.campus_life_backend.modules.auth.service.LoginAuditLogService;
import com.campus.campus_life_backend.modules.auth.service.TokenService;
import com.campus.campus_life_backend.modules.presence.service.PresenceService;
import com.campus.campus_life_backend.modules.search.service.PostSearchService;
import com.campus.campus_life_backend.modules.search.service.SearchOpsService;
import com.campus.campus_life_backend.modules.search.service.UserSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRole(RoleCode.ADMIN)
public class AdminDashboardController {

    private final AdminDashboardQueryService dashboardQueryService;
    private final AdminOperationLogQueryService operationLogQueryService;
    private final AdminApiCatalogService adminApiCatalogService;
    private final PostSearchService postSearchService;
    private final UserSearchService userSearchService;
    private final SearchOpsService searchOpsService;
    private final CurrentUserAccessor currentUserAccessor;
    private final TokenService tokenService;
    private final LoginAuditLogService loginAuditLogService;
    private final PresenceService presenceService;

    public AdminDashboardController(AdminDashboardQueryService dashboardQueryService,
                                    AdminOperationLogQueryService operationLogQueryService,
                                    AdminApiCatalogService adminApiCatalogService,
                                    PostSearchService postSearchService,
                                    UserSearchService userSearchService,
                                    SearchOpsService searchOpsService,
                                    CurrentUserAccessor currentUserAccessor,
                                    TokenService tokenService,
                                    LoginAuditLogService loginAuditLogService,
                                    PresenceService presenceService) {
        this.dashboardQueryService = dashboardQueryService;
        this.operationLogQueryService = operationLogQueryService;
        this.adminApiCatalogService = adminApiCatalogService;
        this.postSearchService = postSearchService;
        this.userSearchService = userSearchService;
        this.searchOpsService = searchOpsService;
        this.currentUserAccessor = currentUserAccessor;
        this.tokenService = tokenService;
        this.loginAuditLogService = loginAuditLogService;
        this.presenceService = presenceService;
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(dashboardQueryService.getStats());
    }

    @GetMapping("/dashboard/stats")
    public ApiResponse<Map<String, Object>> getDashboardStats() {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(dashboardQueryService.getDashboardStats());
    }

    @GetMapping("/dashboard/today")
    public ApiResponse<Map<String, Object>> getTodayStats() {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(dashboardQueryService.getTodayStats());
    }

    @GetMapping("/operation-logs")
    public ApiResponse<Map<String, Object>> getOperationLogList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "adminId", required = false) Long adminId,
            @RequestParam(value = "operationType", required = false) String operationType,
            @RequestParam(value = "targetType", required = false) String targetType,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(operationLogQueryService.getOperationLogList(page, size, adminId, operationType, targetType, action, startTime, endTime));
    }

    @GetMapping("/tools/apis")
    public ApiResponse<Map<String, Object>> getToolsApis() {
        currentUserAccessor.requireUserId();
        List<AdminApiInfoDTO> list = adminApiCatalogService.listApis();
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return ApiResponse.success(result);
    }

    @PostMapping("/search/posts/reindex")
    public ApiResponse<Map<String, Object>> rebuildPostSearchIndex() {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(postSearchService.rebuildPostIndex());
    }

    @PostMapping("/search/users/reindex")
    public ApiResponse<Map<String, Object>> rebuildUserSearchIndex() {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(userSearchService.rebuildUserIndex());
    }

    @PostMapping("/search/posts/{postId}/sync")
    public ApiResponse<Map<String, Object>> syncPostSearchIndex(@PathVariable Long postId) {
        currentUserAccessor.requireUserId();
        postSearchService.syncPostToEs(postId);
        return ApiResponse.success(Map.of("postId", postId, "status", "SYNCED"));
    }

    @PostMapping("/search/users/{userId}/sync")
    public ApiResponse<Map<String, Object>> syncUserSearchIndex(@PathVariable Long userId) {
        currentUserAccessor.requireUserId();
        userSearchService.syncUserToEs(userId);
        return ApiResponse.success(Map.of("userId", userId, "status", "SYNCED"));
    }

    @GetMapping("/search/ops")
    public ApiResponse<Map<String, Object>> searchOps() {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(searchOpsService.getHealthStatus());
    }

    @GetMapping("/search/failures")
    public ApiResponse<Map<String, Object>> searchFailures(
            @RequestParam(value = "channel", required = false) String channel,
            @RequestParam(value = "startMillis", required = false) Long startMillis,
            @RequestParam(value = "endMillis", required = false) Long endMillis,
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(searchOpsService.listFailures(channel, startMillis, endMillis, limit));
    }

    @PostMapping("/search/failures/{recordId}/replay")
    public ApiResponse<Map<String, Object>> replaySearchFailure(@PathVariable String recordId) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(searchOpsService.replayFailure(recordId));
    }

    @GetMapping("/kafka/health")
    public ApiResponse<Map<String, Object>> kafkaHealth() {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(searchOpsService.getHealthStatus());
    }

    @GetMapping("/kafka/failures")
    public ApiResponse<Map<String, Object>> kafkaFailures(
            @RequestParam(value = "channel", required = false) String channel,
            @RequestParam(value = "startMillis", required = false) Long startMillis,
            @RequestParam(value = "endMillis", required = false) Long endMillis,
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(searchOpsService.listFailures(channel, startMillis, endMillis, limit));
    }

    @PostMapping("/kafka/failures/{recordId}/replay")
    public ApiResponse<Map<String, Object>> replayKafkaFailure(@PathVariable String recordId) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(searchOpsService.replayFailure(recordId));
    }

    @PostMapping("/kafka/failures/replay")
    public ApiResponse<Map<String, Object>> replayKafkaFailures(@RequestBody Map<String, Object> request) {
        currentUserAccessor.requireUserId();
        Object rawIds = request == null ? null : request.get("recordIds");
        List<String> recordIds = rawIds instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        return ApiResponse.success(searchOpsService.replayFailures(recordIds));
    }

    @PostMapping("/kafka/failures/{recordId}/ignore")
    public ApiResponse<Map<String, Object>> ignoreKafkaFailure(
            @PathVariable String recordId,
            @RequestBody(required = false) Map<String, String> request) {
        currentUserAccessor.requireUserId();
        String reason = request == null ? null : request.get("reason");
        return ApiResponse.success(searchOpsService.ignoreFailure(recordId, reason));
    }

    @GetMapping("/logs/member-login")
    public ApiResponse<Map<String, Object>> getMemberLoginLogList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                                  @RequestParam(value = "size", defaultValue = "20") Integer size) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(loginAuditLogService.getMemberLoginLogs(page, size));
    }

    @GetMapping("/logs/admin-login")
    public ApiResponse<Map<String, Object>> getAdminLoginLogList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                                 @RequestParam(value = "size", defaultValue = "20") Integer size) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(loginAuditLogService.getAdminLoginLogs(page, size));
    }

    @GetMapping("/logs/audit")
    public ApiResponse<Map<String, Object>> getAuditLogList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(emptyPage(page, size));
    }

    @GetMapping("/monitor/online-users")
    public ApiResponse<Map<String, Object>> getOnlineUsers(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                           @RequestParam(value = "size", defaultValue = "20") Integer size,
                                                           @RequestParam(value = "username", required = false) String username,
                                                           @RequestParam(value = "ip", required = false) String ip) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(presenceService.getOnlineUsers(page, size, username, ip));
    }

    @PostMapping("/monitor/online-users/{userId}/force-logout")
    public ApiResponse<Map<String, Object>> forceLogoutUser(@PathVariable Long userId) {
        currentUserAccessor.requireUserId();
        tokenService.revokeAllUserTokens(userId);
        presenceService.markUserOffline(userId);
        return ApiResponse.success(Map.of("userId", userId, "forced", true));
    }

    @GetMapping("/monitor/cache")
    public ApiResponse<Map<String, Object>> getCacheList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                         @RequestParam(value = "size", defaultValue = "20") Integer size) {
        currentUserAccessor.requireUserId();
        Map<String, Object> result = emptyPage(page, size);
        result.put("totalCache", 0);
        result.put("memoryUsage", "0 MB");
        return ApiResponse.success(result);
    }

    @DeleteMapping("/monitor/cache/{key}")
    public ApiResponse<Void> deleteCacheItem(@PathVariable String key) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(null);
    }

    @PostMapping("/monitor/cache/clear")
    public ApiResponse<Map<String, Object>> clearAllCache() {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(Map.of("cleared", true));
    }

    private Map<String, Object> emptyPage(Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", List.of());
        result.put("page", page);
        result.put("size", size);
        result.put("total", 0);
        return result;
    }
}
