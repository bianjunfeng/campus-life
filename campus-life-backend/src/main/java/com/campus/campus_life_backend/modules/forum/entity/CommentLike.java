package com.campus.campus_life_backend.modules.forum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论点赞实体类
 */
@Data
public class CommentLike {

    private Long id;

    private Long commentId;

    private Long userId;

    private LocalDateTime createTime;
}


