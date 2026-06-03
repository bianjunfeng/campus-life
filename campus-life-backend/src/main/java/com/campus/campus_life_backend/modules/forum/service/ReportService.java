package com.campus.campus_life_backend.modules.forum.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.forum.entity.Report;
import com.campus.campus_life_backend.modules.forum.entity.Post;
import com.campus.campus_life_backend.modules.forum.entity.Comment;
import com.campus.campus_life_backend.modules.forum.mapper.CommentMapper;
import com.campus.campus_life_backend.modules.forum.mapper.PostMapper;
import com.campus.campus_life_backend.modules.forum.mapper.ReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

@Service
public class ReportService {

    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final PostService postService;
    private final CommentService commentService;

    public record AdminReportProcessResult(Integer oldStatus, Integer newStatus, String action, String handleResult) {
    }

    public ReportService(ReportMapper reportMapper, PostMapper postMapper, CommentMapper commentMapper,
                         PostService postService, CommentService commentService) {
        this.reportMapper = reportMapper;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.postService = postService;
        this.commentService = commentService;
    }

    @Transactional
    public Map<String, Object> createReport(Long reporterId, Integer targetType, Long targetId, String reason) {
        validateReportCreateRequest(reporterId, targetType, targetId, reason);
        Report pending = reportMapper.findPendingByReporterAndTarget(reporterId, targetType, targetId);
        if (pending != null) {
            throw new BusinessException(BusinessErrorCode.REPORT_ALREADY_HANDLED, "该内容已举报，正在处理中");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason.trim());
        report.setStatus(0);
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.insert(report);

        Map<String, Object> data = new HashMap<>();
        data.put("id", report.getId());
        data.put("status", report.getStatus());
        return data;
    }

    @Transactional
    public AdminReportProcessResult adminProcessReport(Long reportId, Integer status, Long handlerId,
                                                       String handleResult, String action) {
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(BusinessErrorCode.REPORT_STATUS_INVALID);
        }
        Report report = reportMapper.findById(reportId);
        if (report == null) {
            throw new BusinessException(BusinessErrorCode.REPORT_NOT_FOUND);
        }
        Integer oldStatus = report.getStatus();
        if (oldStatus != null && oldStatus != 0) {
            throw new BusinessException(BusinessErrorCode.REPORT_ALREADY_HANDLED);
        }

        String safeAction = action == null ? "NONE" : action.trim().toUpperCase(Locale.ROOT);
        String safeResult = handleResult == null ? "" : handleResult.trim();

        if (status == 1) {
            if ("NONE".equals(safeAction) || safeAction.isEmpty()) {
                safeAction = report.getTargetType() != null && report.getTargetType() == 1
                        ? "BAN_POST"
                        : "BAN_COMMENT";
            }
            executeReportAction(report, safeAction, safeResult);
        } else {
            safeAction = "IGNORE";
        }

        int updated = reportMapper.processReport(reportId, status, handlerId, safeResult);
        if (updated <= 0) {
            throw new BusinessException(BusinessErrorCode.REPORT_PROCESS_FAILED);
        }
        return new AdminReportProcessResult(oldStatus, status, safeAction, safeResult);
    }

    private void executeReportAction(Report report, String action, String handleResult) {
        if ("NONE".equals(action) || action.isEmpty()) {
            return;
        }
        Integer targetType = report.getTargetType();
        Long targetId = report.getTargetId();
        if (targetType == null || targetId == null) {
            throw new BusinessException(BusinessErrorCode.REPORT_TARGET_INCOMPLETE);
        }
        String reason = isBlank(handleResult) ? "举报处理触发处置" : handleResult;
        switch (action) {
            case "DELETE_POST" -> {
                if (targetType != 1) {
                    throw new BusinessException(BusinessErrorCode.REPORT_TARGET_TYPE_INVALID, "该举报不是帖子类型，不能执行删除帖子");
                }
                postService.adminUpdatePostStatus(targetId, 2, reason);
            }
            case "BAN_POST" -> {
                if (targetType != 1) {
                    throw new BusinessException(BusinessErrorCode.REPORT_TARGET_TYPE_INVALID, "该举报不是帖子类型，不能执行屏蔽帖子");
                }
                postService.adminUpdatePostStatus(targetId, 3, reason);
            }
            case "DELETE_COMMENT" -> {
                if (targetType != 2) {
                    throw new BusinessException(BusinessErrorCode.REPORT_TARGET_TYPE_INVALID, "该举报不是评论类型，不能执行删除评论");
                }
                commentService.adminUpdateCommentStatus(targetId, 1);
            }
            case "BAN_COMMENT" -> {
                if (targetType != 2) {
                    throw new BusinessException(BusinessErrorCode.REPORT_TARGET_TYPE_INVALID, "该举报不是评论类型，不能执行屏蔽评论");
                }
                commentService.adminUpdateCommentStatus(targetId, 2);
            }
            default -> throw new BusinessException(BusinessErrorCode.REPORT_ACTION_UNSUPPORTED,
                    "不支持的处置动作: " + action);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void validateReportCreateRequest(Long reporterId, Integer targetType, Long targetId, String reason) {
        if (targetType == null || (targetType != 1 && targetType != 2)) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "targetType 仅支持 1-帖子 或 2-评论");
        }
        if (targetId == null || targetId <= 0) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "targetId 不合法");
        }
        if (isBlank(reason)) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "举报原因不能为空");
        }
        if (reason.trim().length() > 255) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "举报原因不能超过255字符");
        }

        if (targetType == 1) {
            Post post = postMapper.findById(targetId);
            if (post == null) {
                throw new ReportTargetNotFoundException(BusinessErrorCode.FORUM_POST_NOT_FOUND);
            }
            if (post.getUserId() != null && post.getUserId().equals(reporterId)) {
                throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "不能举报自己的帖子");
            }
            return;
        }

        Comment comment = commentMapper.findById(targetId);
        if (comment == null) {
            throw new ReportTargetNotFoundException(BusinessErrorCode.FORUM_COMMENT_NOT_FOUND);
        }
        if (comment.getUserId() != null && comment.getUserId().equals(reporterId)) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "不能举报自己的评论");
        }
    }

    public static class ReportTargetNotFoundException extends BusinessException {
        public ReportTargetNotFoundException(BusinessErrorCode errorCode) {
            super(errorCode);
        }
    }
}
