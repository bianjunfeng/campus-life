package com.campus.campus_life_backend.modules.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学生档案实体类
 */
@Data
public class StudentProfile {

    private Long userId;

    private String realName;

    private String school;

    private String college;

    private String major;

    private String grade;

    private String className;

    private String studentNo;

    private Integer status;  // 0-待审核;1-已通过/有效;2-已拒绝/无效

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}


