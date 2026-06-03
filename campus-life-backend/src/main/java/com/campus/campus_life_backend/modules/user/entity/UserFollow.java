package com.campus.campus_life_backend.modules.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户关注实体类
 */
@Data
public class UserFollow {

    private Long id;

    private Long followerId;

    private Long followeeId;

    private LocalDateTime createTime;
}


