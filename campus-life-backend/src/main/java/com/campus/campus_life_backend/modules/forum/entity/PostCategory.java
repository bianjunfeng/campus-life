package com.campus.campus_life_backend.modules.forum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 帖子分类实体类
 */
@Data
public class PostCategory {

    private Long id;

    private String name;

    private String code;

    private String description;

    private Integer sortOrder;

    private Integer status;  // 1-启用;0-停用

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}


