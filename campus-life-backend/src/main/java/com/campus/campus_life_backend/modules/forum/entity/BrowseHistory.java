package com.campus.campus_life_backend.modules.forum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 浏览历史实体类
 */
@Data
public class BrowseHistory {

    private Long id;

    private Long userId;

    private Long postId;

    private LocalDateTime firstViewTime;

    private LocalDateTime lastViewTime;

    private Integer viewCount;
}


