-- 支付域基础建模
-- 首期覆盖：支付单、退款单、支付回调日志

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `payment_order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `payment_no` VARCHAR(64) NOT NULL COMMENT '支付单号',
    `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型: VOUCHER/GOODS/MEMBER/RECHARGE',
    `biz_order_no` VARCHAR(64) NOT NULL COMMENT '业务订单号',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `channel` VARCHAR(20) NOT NULL COMMENT '支付渠道: alipay/wechat/wallet',
    `title` VARCHAR(128) NULL COMMENT '支付标题',
    `description` VARCHAR(255) NULL COMMENT '支付描述',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    `refunded_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计退款金额',
    `status` VARCHAR(24) NOT NULL COMMENT '支付状态: INIT/WAIT_PAY/SUCCESS/FAILED/CLOSED/PARTIAL_REFUNDED/FULL_REFUNDED',
    `channel_trade_no` VARCHAR(128) NULL COMMENT '渠道交易号',
    `idempotency_key` VARCHAR(80) NULL COMMENT '创建支付幂等键',
    `expire_time` DATETIME NULL COMMENT '支付过期时间',
    `success_time` DATETIME NULL COMMENT '支付成功时间',
    `last_callback_time` DATETIME NULL COMMENT '最近一次回调时间',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_order_payment_no` (`payment_no`),
    UNIQUE KEY `uk_payment_order_idempotency_key` (`idempotency_key`),
    KEY `idx_payment_order_biz` (`biz_type`, `biz_order_no`),
    KEY `idx_payment_order_user_time` (`user_id`, `create_time`),
    KEY `idx_payment_order_channel_status` (`channel`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单';

CREATE TABLE IF NOT EXISTS `payment_refund_order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `refund_no` VARCHAR(64) NOT NULL COMMENT '退款单号',
    `payment_no` VARCHAR(64) NOT NULL COMMENT '支付单号',
    `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
    `biz_order_no` VARCHAR(64) NOT NULL COMMENT '业务订单号',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `channel` VARCHAR(20) NOT NULL COMMENT '退款渠道',
    `refund_amount` DECIMAL(12,2) NOT NULL COMMENT '退款金额',
    `reason` VARCHAR(255) NULL COMMENT '退款原因',
    `status` VARCHAR(24) NOT NULL COMMENT '退款状态: INIT/SUBMITTED/SUCCESS/FAILED/CLOSED',
    `channel_refund_no` VARCHAR(128) NULL COMMENT '渠道退款单号',
    `request_idempotency_key` VARCHAR(80) NULL COMMENT '退款请求幂等键',
    `success_time` DATETIME NULL COMMENT '退款成功时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_refund_order_refund_no` (`refund_no`),
    UNIQUE KEY `uk_payment_refund_order_request_idempotency_key` (`request_idempotency_key`),
    KEY `idx_payment_refund_payment_no` (`payment_no`),
    KEY `idx_payment_refund_biz` (`biz_type`, `biz_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付退款单';

CREATE TABLE IF NOT EXISTS `payment_callback_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `channel` VARCHAR(20) NOT NULL COMMENT '渠道',
    `callback_type` VARCHAR(32) NOT NULL COMMENT '回调类型: PAY_NOTIFY/REFUND_NOTIFY',
    `payment_no` VARCHAR(64) NULL COMMENT '支付单号',
    `biz_order_no` VARCHAR(64) NULL COMMENT '业务订单号',
    `raw_body` TEXT NOT NULL COMMENT '原始回调内容',
    `signature_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '是否验签通过: 0否 1是',
    `amount_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '是否金额校验通过: 0否 1是',
    `process_status` VARCHAR(24) NOT NULL COMMENT '处理状态: RECEIVED/PROCESSED/FAILED/IGNORED',
    `error_message` VARCHAR(255) NULL COMMENT '错误信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_payment_callback_payment_no` (`payment_no`),
    KEY `idx_payment_callback_channel_time` (`channel`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调日志';
