-- 优惠券与钱包数据结构升级脚本（兼容 MySQL 5.7/8.0）
-- 建议在 campus_life 数据库执行

SET NAMES utf8mb4;

-- voucher.sold_count
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'voucher'
              AND column_name = 'sold_count'
        ),
        'SELECT 1',
        'ALTER TABLE `voucher` ADD COLUMN `sold_count` INT NOT NULL DEFAULT 0 COMMENT ''已售数量（支付成功）'' AFTER `stock`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- voucher_order.payment_method
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'voucher_order'
              AND column_name = 'payment_method'
        ),
        'SELECT 1',
        'ALTER TABLE `voucher_order` ADD COLUMN `payment_method` VARCHAR(20) NULL COMMENT ''支付方式：alipay/wechat'' AFTER `pay_amount`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- voucher_order.payment_status
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'voucher_order'
              AND column_name = 'payment_status'
        ),
        'SELECT 1',
        'ALTER TABLE `voucher_order` ADD COLUMN `payment_status` TINYINT NOT NULL DEFAULT 0 COMMENT ''0-待支付;1-支付成功;2-支付失败;3-已退款'' AFTER `payment_method`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- voucher_order.pay_time
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'voucher_order'
              AND column_name = 'pay_time'
        ),
        'SELECT 1',
        'ALTER TABLE `voucher_order` ADD COLUMN `pay_time` DATETIME NULL COMMENT ''支付时间'' AFTER `payment_status`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- voucher_order.update_time
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'voucher_order'
              AND column_name = 'update_time'
        ),
        'SELECT 1',
        'ALTER TABLE `voucher_order` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `use_time`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 回填 payment_status
UPDATE `voucher_order`
SET payment_status = CASE
    WHEN status >= 1 THEN 1
    ELSE 0
END
WHERE payment_status IS NULL OR payment_status NOT IN (0, 1, 2, 3);

-- 回填 sold_count
UPDATE `voucher` v
LEFT JOIN (
    SELECT voucher_id, COUNT(1) AS sold_cnt
    FROM `voucher_order`
    WHERE status >= 1
    GROUP BY voucher_id
) o ON v.id = o.voucher_id
SET v.sold_count = COALESCE(o.sold_cnt, 0);

-- 钱包主表
CREATE TABLE IF NOT EXISTS `user_wallet` (
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `balance` DECIMAL(12,2) NOT NULL DEFAULT 1000.00 COMMENT '可用余额',
    `frozen_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
    `total_recharge` DECIMAL(12,2) NOT NULL DEFAULT 1000.00 COMMENT '累计入账',
    `total_spent` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-正常;0-冻结',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_user_wallet_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户钱包表';

-- 钱包流水表
CREATE TABLE IF NOT EXISTS `wallet_transaction` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `biz_type` TINYINT NOT NULL COMMENT '1-充值;2-消费;3-退款;4-调整',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '变动金额（正数）',
    `balance_after` DECIMAL(12,2) NOT NULL COMMENT '变动后余额',
    `order_no` VARCHAR(64) NULL COMMENT '关联订单号',
    `remark` VARCHAR(255) NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_wallet_tx_user_time` (`user_id`, `create_time`),
    KEY `idx_wallet_tx_order_no` (`order_no`),
    CONSTRAINT `fk_wallet_transaction_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包交易流水';

-- 初始化钱包
INSERT INTO `user_wallet` (`user_id`, `balance`, `frozen_amount`, `total_recharge`, `total_spent`, `status`)
SELECT
    u.id,
    GREATEST(1000.00 - COALESCE(s.spent, 0), 0),
    0.00,
    1000.00,
    COALESCE(s.spent, 0),
    1
FROM `user` u
LEFT JOIN (
    SELECT user_id, COALESCE(SUM(pay_amount), 0) AS spent
    FROM `voucher_order`
    WHERE status >= 1
    GROUP BY user_id
) s ON u.id = s.user_id
ON DUPLICATE KEY UPDATE
    total_spent = VALUES(total_spent),
    balance = GREATEST(total_recharge - VALUES(total_spent), 0),
    status = 1;

