package com.campus.campus_life_backend.modules.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员操作日志实体类
 */
@Data
public class AdminOperationLog {
    
    private Long id;
    
    private Long adminId;  // 操作管理员ID
    
    private String operationType;  // 操作类型：post_manage, merchant_manage, voucher_manage, comment_manage
    
    private String targetType;  // 目标类型：post, merchant, voucher, comment
    
    private Long targetId;  // 目标ID
    
    private String action;  // 操作动作：approve, reject, delete, ban, unpin, pin, set_hot, remove_hot, freeze, unfreeze, etc.
    
    private Integer oldStatus;  // 操作前状态
    
    private Integer newStatus;  // 操作后状态
    
    private String reason;  // 操作原因/备注
    
    private String ipAddress;  // 操作IP地址
    
    private LocalDateTime createTime;
}


