package com.campus.campus_life_backend.modules.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学生认证申请实体类
 */
@Data
public class StudentAuthRequest {

    private Long id;

    private Long userId;

    private String realName;

    private String school;

    private String college;

    private String major;

    private String grade;

    private String className;

    private String studentNo;

    private String studentCardImg;

    private Integer status;  // 0-待审核;1-通过;2-驳回

    private String reason;

    private Long reviewerId;

    private LocalDateTime createTime;

    private LocalDateTime reviewTime;
}


