package com.campus.campus_life_backend.modules.forum.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.forum.entity.Comment;
import com.campus.campus_life_backend.modules.forum.service.CommentService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum")
public class CommentController {

    private final CurrentUserAccessor currentUserAccessor;
    private final CommentService commentService;

    public CommentController(CurrentUserAccessor currentUserAccessor, CommentService commentService) {
        this.currentUserAccessor = currentUserAccessor;
        this.commentService = commentService;
    }

    @PostMapping("/posts/{postId}/comments")
    @RequirePermission(anyOf = {"comment:create"})
    public ApiResponse<Comment> createComment(
            @PathVariable Long postId,
            @RequestBody Map<String, Object> request) {
        Long userId = currentUserAccessor.requireUserId();
        String content = (String) request.get("content");
        Long parentId = request.get("parentId") != null
                ? Long.valueOf(request.get("parentId").toString())
                : null;
        Long replyToUserId = request.get("targetUserId") != null
                ? Long.valueOf(request.get("targetUserId").toString())
                : null;

        Comment comment = commentService.createComment(postId, userId, content, parentId, replyToUserId);
        return ApiResponse.success(comment);
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<Map<String, Object>> getComments(
            @PathVariable Long postId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Long currentUserId = currentUserAccessor.getCurrentUserId();
        List<Comment> list = commentService.getCommentsByPostId(postId, currentUserId);
        long total = commentService.countCommentsByPostId(postId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/comments/{commentId}")
    @RequirePermission(anyOf = {"comment:delete:self"})
    @RequireOwnerOrPermission(resource = ResourceTypeCode.COMMENT, idParam = "commentId")
    public ApiResponse<Map<String, Object>> deleteComment(@PathVariable Long commentId) {
        Comment comment = commentService.getCommentById(commentId);
        if (comment == null || (comment.getStatus() != null && comment.getStatus() == 1)) {
            throw new BusinessException(BusinessErrorCode.FORUM_COMMENT_NOT_FOUND);
        }

        commentService.deleteComment(commentId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ApiResponse.success(result);
    }

    @PostMapping("/comments/{commentId}/like")
    @RequireLogin
    public ApiResponse<Map<String, Object>> toggleLike(@PathVariable Long commentId) {
        Long userId = currentUserAccessor.requireUserId();
        Comment comment = commentService.getCommentById(commentId);
        if (comment == null || (comment.getStatus() != null && comment.getStatus() == 1)) {
            throw new BusinessException(BusinessErrorCode.FORUM_COMMENT_NOT_FOUND);
        }

        boolean liked = commentService.toggleLike(commentId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", commentService.getCommentLikeCount(commentId));
        return ApiResponse.success(result);
    }
}
