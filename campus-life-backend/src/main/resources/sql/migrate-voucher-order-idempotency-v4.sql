SET @exists_col := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'voucher_order'
      AND COLUMN_NAME = 'payment_idempotency_key'
);
SET @sql_col := IF(
    @exists_col = 0,
    'ALTER TABLE voucher_order ADD COLUMN payment_idempotency_key VARCHAR(80) NULL COMMENT ''支付请求幂等键'' AFTER payment_method',
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
      AND INDEX_NAME = 'uk_voucher_order_payment_idempotency_key'
);
SET @sql_idx := IF(
    @exists_idx = 0,
    'CREATE UNIQUE INDEX uk_voucher_order_payment_idempotency_key ON voucher_order (payment_idempotency_key)',
    'SELECT 1'
);
PREPARE stmt_idx FROM @sql_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;
