package com.campus.campus_life_backend.modules.forum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论实体类
 */
@Data
public class Comment {

    private Long id;

    private Long postId;

    private Long userId;

    private Long parentId;

    private Long replyToUserId;

    private String content;

    private Integer status;  // 0-正常;1-已删除;2-屏蔽

    private Integer likeCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // 评论作者展示信息（联表查询返回）
    private String authorName;

    private String authorAvatar;

    // 当前登录用户是否已点赞此评论
    private Boolean isLiked;
}

