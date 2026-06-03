package com.campus.campus_life_backend.modules.admin.service;

import com.campus.campus_life_backend.modules.admin.entity.AdminOperationLog;
import com.campus.campus_life_backend.modules.admin.mapper.AdminOperationLogMapper;
import com.campus.campus_life_backend.modules.forum.service.AdminCommunityQueryService;
import com.campus.campus_life_backend.modules.forum.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PostAdminOrchestrationService {

    private final AdminCommunityQueryService communityQueryService;
    private final PostService postService;
    private final AdminOperationLogMapper operationLogMapper;

    public PostAdminOrchestrationService(
            AdminCommunityQueryService communityQueryService,
            PostService postService,
            AdminOperationLogMapper operationLogMapper) {
        this.communityQueryService = communityQueryService;
        this.postService = postService;
        this.operationLogMapper = operationLogMapper;
    }

    public Map<String, Object> getPostList(Integer page, Integer size, Integer status, String keyword, Long userId) {
        return communityQueryService.getPostList(page, size, status, keyword, userId);
    }

    @Transactional
    public void deletePost(Long adminId, Long postId, String reason, String ipAddress) {
        PostService.AdminPostStatusChange change = postService.adminUpdatePostStatus(postId, 2, "admin_delete_post");
        logOperation(adminId, "post_manage", "post", postId, "delete", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void banPost(Long adminId, Long postId, String reason, String ipAddress) {
        PostService.AdminPostStatusChange change = postService.adminUpdatePostStatus(postId, 3, "admin_ban_post");
        logOperation(adminId, "post_manage", "post", postId, "ban", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void unbanPost(Long adminId, Long postId, String reason, String ipAddress) {
        PostService.AdminPostStatusChange change = postService.adminUpdatePostStatus(postId, 0, "admin_unban_post");
        logOperation(adminId, "post_manage", "post", postId, "unban", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void pinPost(Long adminId, Long postId, String reason, String ipAddress) {
        postService.adminSetPinned(postId, true);
        logOperation(adminId, "post_manage", "post", postId, "pin", null, null, reason, ipAddress);
    }

    @Transactional
    public void unpinPost(Long adminId, Long postId, String reason, String ipAddress) {
        postService.adminSetPinned(postId, false);
        logOperation(adminId, "post_manage", "post", postId, "unpin", null, null, reason, ipAddress);
    }

    @Transactional
    public void setHotPost(Long adminId, Long postId, String reason, String ipAddress) {
        postService.adminSetHot(postId, true);
        logOperation(adminId, "post_manage", "post", postId, "set_hot", null, null, reason, ipAddress);
    }

    @Transactional
    public void removeHotPost(Long adminId, Long postId, String reason, String ipAddress) {
        postService.adminSetHot(postId, false);
        logOperation(adminId, "post_manage", "post", postId, "remove_hot", null, null, reason, ipAddress);
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
