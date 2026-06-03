package com.campus.campus_life_backend.modules.forum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 帖子图片实体类
 */
@Data
public class PostImage {

    private Long id;

    private Long postId;

    private String url;

    private Integer width;

    private Integer height;

    private Integer sortOrder;

    private LocalDateTime createTime;
}


