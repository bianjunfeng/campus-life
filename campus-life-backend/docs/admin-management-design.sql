-- ============================================
-- 管理员管理系统设计
-- 包含：帖子管理、商铺管理、券管理、评论管理
-- ============================================

-- ========================
-- 1. 管理操作日志表（记录所有管理操作）
-- ========================
DROP TABLE IF EXISTS `admin_operation_log`;
CREATE TABLE `admin_operation_log` (
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

-- ========================
-- 2. 扩展现有表的 status 字段说明
-- ========================

-- 2.1 post 表 status 字段扩展说明
-- 当前：0-正常;1-仅自己可见;2-已删除;3-屏蔽
-- 建议保持不变，管理员可以通过设置 status = 3 来屏蔽帖子

-- 2.2 merchant 表 status 字段扩展说明
-- 当前：0-待审核;1-正常;2-冻结;3-关闭
-- 建议保持不变，管理员可以：
--   - 审核：0 -> 1 (通过) 或 0 -> 3 (拒绝)
--   - 冻结：1 -> 2 (冻结商家)
--   - 解冻：2 -> 1 (恢复正常)
--   - 关闭：1 -> 3 (关闭商家)

-- 2.3 voucher 表 status 字段扩展说明
-- 当前：1-上架;0-下架
-- 建议扩展为：0-下架;1-上架;2-已下架(管理员);3-违规下架
-- 需要修改表结构

-- 2.4 comment 表 status 字段扩展说明
-- 当前：0-正常;1-已删除;2-屏蔽
-- 建议保持不变，管理员可以通过设置 status = 2 来屏蔽评论

-- ========================
-- 3. 修改 voucher 表 status 字段（扩展状态）
-- ========================
-- 注意：如果表中已有数据，需要先迁移数据
-- ALTER TABLE `voucher` 
-- MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 1 
-- COMMENT '0-下架;1-上架;2-已下架(管理员);3-违规下架';

-- ========================
-- 4. 管理审核队列表（可选，用于待审核内容）
-- ========================
-- 如果内容需要审核后才能发布，可以创建此表
-- 当前设计：内容发布后，管理员可以管理（删除、屏蔽等）
-- 如果需要先审核后发布，可以添加此表

DROP TABLE IF EXISTS `content_audit_queue`;
CREATE TABLE `content_audit_queue` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `content_type`      VARCHAR(50) NOT NULL COMMENT '内容类型：post, comment',
    `content_id`        BIGINT UNSIGNED NOT NULL COMMENT '内容ID',
    `submit_user_id`   BIGINT UNSIGNED NOT NULL COMMENT '提交用户ID',
    `status`           TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核;1-已通过;2-已拒绝',
    `auditor_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '审核管理员ID',
    `audit_time`       DATETIME DEFAULT NULL COMMENT '审核时间',
    `audit_reason`     VARCHAR(500) DEFAULT NULL COMMENT '审核原因/备注',
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_content` (`content_type`, `content_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_audit_queue_user` FOREIGN KEY (`submit_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核队列表（可选）';

-- ========================
-- 5. 管理统计视图（可选，用于快速查询）
-- ========================
-- 创建视图方便管理员查看统计数据

-- 帖子管理统计视图
CREATE OR REPLACE VIEW `v_admin_post_stats` AS
SELECT 
    COUNT(*) AS total_posts,
    SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS normal_posts,
    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS banned_posts,
    SUM(CASE WHEN is_pinned = 1 THEN 1 ELSE 0 END) AS pinned_posts,
    SUM(CASE WHEN is_hot = 1 THEN 1 ELSE 0 END) AS hot_posts,
    DATE(create_time) AS post_date
FROM post
GROUP BY DATE(create_time);

-- 商家管理统计视图
CREATE OR REPLACE VIEW `v_admin_merchant_stats` AS
SELECT 
    COUNT(*) AS total_merchants,
    SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS pending_merchants,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS normal_merchants,
    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS frozen_merchants,
    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS closed_merchants
FROM merchant;

-- 券管理统计视图
CREATE OR REPLACE VIEW `v_admin_voucher_stats` AS
SELECT 
    COUNT(*) AS total_vouchers,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS online_vouchers,
    SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS offline_vouchers,
    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS admin_offline_vouchers,
    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS violation_vouchers
FROM voucher;

-- 评论管理统计视图
CREATE OR REPLACE VIEW `v_admin_comment_stats` AS
SELECT 
    COUNT(*) AS total_comments,
    SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS normal_comments,
    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS banned_comments,
    DATE(create_time) AS comment_date
FROM comment
GROUP BY DATE(create_time);

-- ========================
-- 6. 索引优化（为管理查询添加索引）
-- ========================

-- post 表：为管理查询添加索引
ALTER TABLE `post` ADD INDEX `idx_status_create_time` (`status`, `create_time`);

-- merchant 表：为管理查询添加索引
ALTER TABLE `merchant` ADD INDEX `idx_status_create_time` (`status`, `create_time`);

-- voucher 表：为管理查询添加索引
ALTER TABLE `voucher` ADD INDEX `idx_status_create_time` (`status`, `create_time`);

-- comment 表：为管理查询添加索引
ALTER TABLE `comment` ADD INDEX `idx_status_create_time` (`status`, `create_time`);

-- ========================
-- 7. 管理操作类型说明
-- ========================
-- 帖子管理操作：
--   - delete: 删除帖子 (status: 0 -> 2)
--   - ban: 屏蔽帖子 (status: 0 -> 3)
--   - unban: 解除屏蔽 (status: 3 -> 0)
--   - pin: 置顶 (is_pinned: 0 -> 1)
--   - unpin: 取消置顶 (is_pinned: 1 -> 0)
--   - set_hot: 设为热门 (is_hot: 0 -> 1)
--   - remove_hot: 取消热门 (is_hot: 1 -> 0)

-- 商家管理操作：
--   - approve: 审核通过 (status: 0 -> 1)
--   - reject: 审核拒绝 (status: 0 -> 3)
--   - freeze: 冻结商家 (status: 1 -> 2)
--   - unfreeze: 解冻商家 (status: 2 -> 1)
--   - close: 关闭商家 (status: 1 -> 3)

-- 券管理操作：
--   - approve: 审核通过 (status: 0 -> 1)
--   - reject: 审核拒绝 (status: 0 -> 3)
--   - offline: 下架 (status: 1 -> 0)
--   - online: 上架 (status: 0 -> 1)
--   - admin_offline: 管理员下架 (status: 1 -> 2)
--   - violation_offline: 违规下架 (status: 1 -> 3)

-- 评论管理操作：
--   - delete: 删除评论 (status: 0 -> 1)
--   - ban: 屏蔽评论 (status: 0 -> 2)
--   - unban: 解除屏蔽 (status: 2 -> 0)

