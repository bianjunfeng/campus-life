SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64),
    IN alter_statement TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND column_name = target_column
    ) THEN
        SET @ddl = alter_statement;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_missing('payment_refund_order', 'merchant_id',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `merchant_id` BIGINT UNSIGNED NULL COMMENT ''所属商家ID'' AFTER `user_id`');
CALL add_column_if_missing('payment_refund_order', 'requested_amount',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `requested_amount` DECIMAL(12,2) NULL COMMENT ''申请退款金额'' AFTER `refund_amount`');
CALL add_column_if_missing('payment_refund_order', 'approved_amount',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `approved_amount` DECIMAL(12,2) NULL COMMENT ''审核通过金额'' AFTER `requested_amount`');
CALL add_column_if_missing('payment_refund_order', 'current_reviewer_role',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `current_reviewer_role` VARCHAR(20) NULL COMMENT ''当前审核角色: MERCHANT/ADMIN/SYSTEM'' AFTER `status`');
CALL add_column_if_missing('payment_refund_order', 'current_reviewer_id',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `current_reviewer_id` BIGINT UNSIGNED NULL COMMENT ''当前审核人ID'' AFTER `current_reviewer_role`');
CALL add_column_if_missing('payment_refund_order', 'review_deadline',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `review_deadline` DATETIME NULL COMMENT ''当前审核截止时间'' AFTER `current_reviewer_id`');
CALL add_column_if_missing('payment_refund_order', 'merchant_review_reason',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `merchant_review_reason` VARCHAR(255) NULL COMMENT ''商家审核说明'' AFTER `review_deadline`');
CALL add_column_if_missing('payment_refund_order', 'merchant_review_time',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `merchant_review_time` DATETIME NULL COMMENT ''商家审核时间'' AFTER `merchant_review_reason`');
CALL add_column_if_missing('payment_refund_order', 'merchant_reviewer_id',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `merchant_reviewer_id` BIGINT UNSIGNED NULL COMMENT ''商家审核人ID'' AFTER `merchant_review_time`');
CALL add_column_if_missing('payment_refund_order', 'admin_review_reason',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `admin_review_reason` VARCHAR(255) NULL COMMENT ''管理员审核说明'' AFTER `merchant_reviewer_id`');
CALL add_column_if_missing('payment_refund_order', 'admin_review_time',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `admin_review_time` DATETIME NULL COMMENT ''管理员审核时间'' AFTER `admin_review_reason`');
CALL add_column_if_missing('payment_refund_order', 'admin_reviewer_id',
    'ALTER TABLE `payment_refund_order` ADD COLUMN `admin_reviewer_id` BIGINT UNSIGNED NULL COMMENT ''管理员审核人ID'' AFTER `admin_review_time`');

DROP PROCEDURE IF EXISTS add_column_if_missing;

SET @idx_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_refund_order'
      AND index_name = 'idx_payment_refund_merchant_status'
);
SET @sql = IF(@idx_exists > 0, 'SELECT 1', 'CREATE INDEX idx_payment_refund_merchant_status ON payment_refund_order (merchant_id, status)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_refund_order'
      AND index_name = 'idx_payment_refund_user_status'
);
SET @sql = IF(@idx_exists > 0, 'SELECT 1', 'CREATE INDEX idx_payment_refund_user_status ON payment_refund_order (user_id, status)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `payment_refund_order`
SET
    `requested_amount` = COALESCE(`requested_amount`, `refund_amount`),
    `approved_amount` = CASE
        WHEN `status` = 'SUCCESS' THEN COALESCE(`approved_amount`, `refund_amount`)
        ELSE `approved_amount`
    END,
    `status` = CASE
        WHEN `status` = 'INIT' THEN 'WAIT_MERCHANT_REVIEW'
        WHEN `status` = 'SUBMITTED' THEN 'WAIT_MERCHANT_REVIEW'
        ELSE `status`
    END,
    `current_reviewer_role` = CASE
        WHEN `status` IN ('WAIT_MERCHANT_REVIEW', 'INIT', 'SUBMITTED') THEN COALESCE(`current_reviewer_role`, 'MERCHANT')
        ELSE `current_reviewer_role`
    END;

UPDATE `payment_refund_order` r
JOIN `voucher_order` vo ON vo.order_no = r.biz_order_no
JOIN `voucher` v ON v.id = vo.voucher_id
SET r.merchant_id = v.merchant_id
WHERE r.merchant_id IS NULL
  AND r.biz_type = 'VOUCHER';

CREATE TABLE IF NOT EXISTS `payment_refund_review_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `refund_no` VARCHAR(64) NOT NULL COMMENT '退款单号',
    `operator_role` VARCHAR(20) NOT NULL COMMENT '操作角色: USER/MERCHANT/ADMIN/SYSTEM',
    `operator_id` BIGINT UNSIGNED NULL COMMENT '操作人ID',
    `action` VARCHAR(32) NOT NULL COMMENT '动作: SUBMIT/MERCHANT_APPROVE/MERCHANT_REJECT/ADMIN_APPROVE/ADMIN_REJECT/SYSTEM_SUCCESS/SYSTEM_FAILED',
    `from_status` VARCHAR(32) NULL COMMENT '原状态',
    `to_status` VARCHAR(32) NULL COMMENT '目标状态',
    `comment` VARCHAR(255) NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_payment_refund_review_log_refund_no` (`refund_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款审核日志';
