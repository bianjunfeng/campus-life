package com.campus.campus_life_backend.modules.forum.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 举报实体类
 */
@Data
public class Report {

    private Long id;

    private Long reporterId;

    private Integer targetType;  // 1-帖子;2-评论;3-用户

    private Long targetId;

    private String reason;

    private Integer status;  // 0-待处理;1-已处理;2-忽略

    private Long handlerId;

    private String handleResult;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

