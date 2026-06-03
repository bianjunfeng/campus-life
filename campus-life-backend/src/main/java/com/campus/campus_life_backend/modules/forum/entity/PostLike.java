package com.campus.campus_life_backend.modules.forum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 帖子点赞实体类
 */
@Data
public class PostLike {

    private Long id;

    private Long postId;

    private Long userId;

    private LocalDateTime createTime;
}


