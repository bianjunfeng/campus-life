-- 管理员操作日志表（管理端 /api/admin/operation-logs）
CREATE TABLE IF NOT EXISTS `admin_operation_log` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `admin_id`          BIGINT UNSIGNED NOT NULL COMMENT '操作管理员ID',
    `operation_type`    VARCHAR(50) NOT NULL COMMENT '操作类型：post_manage, merchant_manage, voucher_manage, comment_manage',
    `target_type`       VARCHAR(50) NOT NULL COMMENT '目标类型：post, merchant, voucher, comment',
    `target_id`         BIGINT UNSIGNED NOT NULL COMMENT '目标ID',
    `action`            VARCHAR(50) NOT NULL COMMENT '操作动作：approve, reject, delete, ban, unpin, pin, set_hot, remove_hot, freeze, unfreeze, etc.',
    `old_status`        TINYINT DEFAULT NULL COMMENT '操作前状态',
    `new_status`        TINYINT DEFAULT NULL COMMENT '操作后状态',
    `reason`            VARCHAR(500) DEFAULT NULL COMMENT '操作原因/备注',
    `ip_address`        VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
    `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_admin_id` (`admin_id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_admin_operation_log_admin` FOREIGN KEY (`admin_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志表';
