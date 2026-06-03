-- 商家账户中心扩展字段迁移（v1）
SET NAMES utf8mb4;

-- business_hours
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'merchant'
              AND column_name = 'business_hours'
        ),
        'SELECT 1',
        'ALTER TABLE `merchant` ADD COLUMN `business_hours` VARCHAR(80) NULL COMMENT ''营业时间'' AFTER `address`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
