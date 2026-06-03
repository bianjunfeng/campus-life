-- 商家定位字段迁移（v1）
SET NAMES utf8mb4;

-- longitude
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'merchant'
              AND column_name = 'longitude'
        ),
        'SELECT 1',
        'ALTER TABLE `merchant` ADD COLUMN `longitude` DECIMAL(10,6) NULL COMMENT ''经度'' AFTER `address`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- latitude
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'merchant'
              AND column_name = 'latitude'
        ),
        'SELECT 1',
        'ALTER TABLE `merchant` ADD COLUMN `latitude` DECIMAL(10,6) NULL COMMENT ''纬度'' AFTER `longitude`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- geo_hash
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'merchant'
              AND column_name = 'geo_hash'
        ),
        'SELECT 1',
        'ALTER TABLE `merchant` ADD COLUMN `geo_hash` VARCHAR(32) NULL COMMENT ''GeoHash编码'' AFTER `latitude`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- province
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'merchant'
              AND column_name = 'province'
        ),
        'SELECT 1',
        'ALTER TABLE `merchant` ADD COLUMN `province` VARCHAR(50) NULL COMMENT ''省'' AFTER `geo_hash`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- city
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'merchant'
              AND column_name = 'city'
        ),
        'SELECT 1',
        'ALTER TABLE `merchant` ADD COLUMN `city` VARCHAR(50) NULL COMMENT ''市'' AFTER `province`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- district
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'merchant'
              AND column_name = 'district'
        ),
        'SELECT 1',
        'ALTER TABLE `merchant` ADD COLUMN `district` VARCHAR(50) NULL COMMENT ''区县'' AFTER `city`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- location_status
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'merchant'
              AND column_name = 'location_status'
        ),
        'SELECT 1',
        'ALTER TABLE `merchant` ADD COLUMN `location_status` TINYINT NOT NULL DEFAULT 0 COMMENT ''定位状态：0未设置 1已设置'' AFTER `district`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- location_updated_time
SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'merchant'
              AND column_name = 'location_updated_time'
        ),
        'SELECT 1',
        'ALTER TABLE `merchant` ADD COLUMN `location_updated_time` DATETIME NULL COMMENT ''定位更新时间'' AFTER `location_status`'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 索引 idx_status_type
SET @idx_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'merchant'
      AND index_name = 'idx_status_type'
);
SET @sql = IF(@idx_exists > 0, 'SELECT 1', 'CREATE INDEX idx_status_type ON merchant (status, type_id)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 索引 idx_lng_lat
SET @idx_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'merchant'
      AND index_name = 'idx_lng_lat'
);
SET @sql = IF(@idx_exists > 0, 'SELECT 1', 'CREATE INDEX idx_lng_lat ON merchant (longitude, latitude)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

