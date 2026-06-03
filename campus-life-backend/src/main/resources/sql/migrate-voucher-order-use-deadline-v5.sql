SET NAMES utf8mb4;

SET @exists_col := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'voucher_order'
      AND COLUMN_NAME = 'use_deadline'
);
SET @sql_col := IF(
    @exists_col = 0,
    'ALTER TABLE voucher_order ADD COLUMN use_deadline DATETIME NULL COMMENT ''使用截止时间快照'' AFTER pay_deadline',
    'SELECT 1'
);
PREPARE stmt_col FROM @sql_col;
EXECUTE stmt_col;
DEALLOCATE PREPARE stmt_col;

SET @exists_idx := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'voucher_order'
      AND INDEX_NAME = 'idx_voucher_order_use_deadline_refund'
);
SET @sql_idx := IF(
    @exists_idx = 0,
    'CREATE INDEX idx_voucher_order_use_deadline_refund ON voucher_order (status, payment_status, use_deadline)',
    'SELECT 1'
);
PREPARE stmt_idx FROM @sql_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

UPDATE `voucher_order` vo
LEFT JOIN `voucher` v ON v.id = vo.voucher_id
SET vo.use_deadline = v.end_time
WHERE vo.use_deadline IS NULL
  AND v.end_time IS NOT NULL;
