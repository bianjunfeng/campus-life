package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.modules.admin.service.CommentAdminOrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRole(RoleCode.ADMIN)
public class AdminCommentController {

    private final CommentAdminOrchestrationService commentAdminService;
    private final AdminAuthUtil adminAuthUtil;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminCommentController(CommentAdminOrchestrationService commentAdminService,
                                  AdminAuthUtil adminAuthUtil,
                                  CurrentUserAccessor currentUserAccessor) {
        this.commentAdminService = commentAdminService;
        this.adminAuthUtil = adminAuthUtil;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/comments")
    public ApiResponse<Map<String, Object>> getCommentList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "postId", required = false) Long postId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(commentAdminService.getCommentList(page, size, status, postId, userId, keyword));
    }

    @PostMapping("/comments/{commentId}/delete")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        commentAdminService.deleteComment(currentUserAccessor.requireUserId(), commentId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/comments/{commentId}/ban")
    public ApiResponse<Void> banComment(@PathVariable Long commentId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        commentAdminService.banComment(currentUserAccessor.requireUserId(), commentId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/comments/{commentId}/unban")
    public ApiResponse<Void> unbanComment(@PathVariable Long commentId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        commentAdminService.unbanComment(currentUserAccessor.requireUserId(), commentId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }
}
