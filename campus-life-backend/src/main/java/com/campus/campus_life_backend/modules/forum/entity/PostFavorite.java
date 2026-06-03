package com.campus.campus_life_backend.modules.forum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 帖子收藏实体类
 */
@Data
public class PostFavorite {

    private Long id;

    private Long postId;

    private Long userId;

    private LocalDateTime createTime;
}


