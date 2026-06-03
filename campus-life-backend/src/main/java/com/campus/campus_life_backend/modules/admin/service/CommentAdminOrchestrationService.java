package com.campus.campus_life_backend.modules.admin.service;

import com.campus.campus_life_backend.modules.admin.entity.AdminOperationLog;
import com.campus.campus_life_backend.modules.admin.mapper.AdminOperationLogMapper;
import com.campus.campus_life_backend.modules.forum.service.AdminCommunityQueryService;
import com.campus.campus_life_backend.modules.forum.service.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CommentAdminOrchestrationService {

    private final AdminCommunityQueryService communityQueryService;
    private final CommentService commentService;
    private final AdminOperationLogMapper operationLogMapper;

    public CommentAdminOrchestrationService(
            AdminCommunityQueryService communityQueryService,
            CommentService commentService,
            AdminOperationLogMapper operationLogMapper) {
        this.communityQueryService = communityQueryService;
        this.commentService = commentService;
        this.operationLogMapper = operationLogMapper;
    }

    public Map<String, Object> getCommentList(Integer page, Integer size, Integer status, Long postId, Long userId, String keyword) {
        return communityQueryService.getCommentList(page, size, status, postId, userId, keyword);
    }

    @Transactional
    public void deleteComment(Long adminId, Long commentId, String reason, String ipAddress) {
        CommentService.AdminCommentStatusChange change = commentService.adminUpdateCommentStatus(commentId, 1);
        logOperation(adminId, "comment_manage", "comment", commentId, "delete", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void banComment(Long adminId, Long commentId, String reason, String ipAddress) {
        CommentService.AdminCommentStatusChange change = commentService.adminUpdateCommentStatus(commentId, 2);
        logOperation(adminId, "comment_manage", "comment", commentId, "ban", change.oldStatus(), change.newStatus(), reason, ipAddress);
    }

    @Transactional
    public void unbanComment(Long adminId, Long commentId, String reason, String ipAddress) {
        CommentService.AdminCommentStatusChange change = commentService.adminUpdateCommentStatus(commentId, 0);
        logOperation(adminId, "comment_manage", "comment", commentId, "unban", change.oldStatus(), change.newStatus(), reason, ipAddress);
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
