package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.modules.admin.service.PostAdminOrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRole(RoleCode.ADMIN)
public class AdminPostController {

    private final PostAdminOrchestrationService postAdminService;
    private final AdminAuthUtil adminAuthUtil;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminPostController(PostAdminOrchestrationService postAdminService,
                               AdminAuthUtil adminAuthUtil,
                               CurrentUserAccessor currentUserAccessor) {
        this.postAdminService = postAdminService;
        this.adminAuthUtil = adminAuthUtil;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/posts")
    public ApiResponse<Map<String, Object>> getPostList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "userId", required = false) Long userId) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(postAdminService.getPostList(page, size, status, keyword, userId));
    }

    @PostMapping("/posts/{postId}/delete")
    public ApiResponse<Void> deletePost(@PathVariable Long postId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        postAdminService.deletePost(currentUserAccessor.requireUserId(), postId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/posts/{postId}/ban")
    public ApiResponse<Void> banPost(@PathVariable Long postId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        postAdminService.banPost(currentUserAccessor.requireUserId(), postId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/posts/{postId}/unban")
    public ApiResponse<Void> unbanPost(@PathVariable Long postId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        postAdminService.unbanPost(currentUserAccessor.requireUserId(), postId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/posts/{postId}/pin")
    public ApiResponse<Void> pinPost(@PathVariable Long postId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        postAdminService.pinPost(currentUserAccessor.requireUserId(), postId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/posts/{postId}/unpin")
    public ApiResponse<Void> unpinPost(@PathVariable Long postId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        postAdminService.unpinPost(currentUserAccessor.requireUserId(), postId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/posts/{postId}/set-hot")
    public ApiResponse<Void> setHotPost(@PathVariable Long postId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        postAdminService.setHotPost(currentUserAccessor.requireUserId(), postId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @PostMapping("/posts/{postId}/remove-hot")
    public ApiResponse<Void> removeHotPost(@PathVariable Long postId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        postAdminService.removeHotPost(currentUserAccessor.requireUserId(), postId, request.get("reason"), adminAuthUtil.getClientIp(httpRequest));
        return ApiResponse.success(null);
    }
}
