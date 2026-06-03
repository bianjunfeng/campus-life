package com.campus.campus_life_backend.modules.forum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 帖子实体类
 */
@Data
public class Post {

    private Long id;

    private Long userId;

    private Long categoryId;

    private String title;

    private String content;

    private Integer status;  // 0-正常;1-仅自己可见;2-已删除;3-屏蔽

    private Integer isPinned;  // 0-否;1-是

    private Integer isHot;  // 0-否;1-是

    private Integer likeCount;

    private Integer commentCount;

    private Integer favoriteCount;

    private Integer viewCount;

    private LocalDateTime lastCommentTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}


