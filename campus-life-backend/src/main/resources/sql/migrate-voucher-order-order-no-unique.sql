-- voucher_order.order_no 唯一索引，保障秒杀消息重复投递时的持久化幂等
SET NAMES utf8mb4;

SET @idx_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'voucher_order'
      AND index_name = 'uk_voucher_order_order_no'
);

SET @sql = IF(
    @idx_exists > 0,
    'SELECT 1',
    'CREATE UNIQUE INDEX uk_voucher_order_order_no ON voucher_order (order_no)'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
