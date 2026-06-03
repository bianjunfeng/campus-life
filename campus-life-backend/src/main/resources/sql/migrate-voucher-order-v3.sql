-- 订单表结构增强（v3）
SET NAMES utf8mb4;

-- order_source
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'voucher_order'
              AND column_name = 'order_source'
        ),
        'SELECT 1',
        'ALTER TABLE `voucher_order` ADD COLUMN `order_source` VARCHAR(20) NOT NULL DEFAULT ''app'' COMMENT ''订单来源：app/web/admin/seckill'' AFTER `payment_status`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- pay_deadline
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'voucher_order'
              AND column_name = 'pay_deadline'
        ),
        'SELECT 1',
        'ALTER TABLE `voucher_order` ADD COLUMN `pay_deadline` DATETIME NULL COMMENT ''支付截止时间'' AFTER `order_source`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cancel_reason
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'voucher_order'
              AND column_name = 'cancel_reason'
        ),
        'SELECT 1',
        'ALTER TABLE `voucher_order` ADD COLUMN `cancel_reason` VARCHAR(255) NULL COMMENT ''取消原因'' AFTER `pay_deadline`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 复合索引 user_id + status + create_time
SET @idx_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'voucher_order'
      AND index_name = 'idx_user_status_time'
);
SET @sql = IF(@idx_exists > 0, 'SELECT 1', 'CREATE INDEX idx_user_status_time ON voucher_order (user_id, status, create_time)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 历史订单来源回填
UPDATE `voucher_order`
SET order_source = CASE
    WHEN EXISTS (
        SELECT 1 FROM `seckill_voucher` sv WHERE sv.voucher_id = voucher_order.voucher_id
    ) THEN 'seckill'
    ELSE 'app'
END
WHERE order_source IS NULL OR order_source = '';
