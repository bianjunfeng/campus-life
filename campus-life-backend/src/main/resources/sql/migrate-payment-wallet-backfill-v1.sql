-- 钱包支付历史单回填到 payment_order
-- 用途：
-- 1. 为历史 wallet 支付订单补齐统一支付单
-- 2. 让钱包支付订单可走统一退款链路

SET NAMES utf8mb4;

INSERT INTO `payment_order` (
    `payment_no`,
    `biz_type`,
    `biz_order_no`,
    `user_id`,
    `channel`,
    `title`,
    `description`,
    `amount`,
    `refunded_amount`,
    `status`,
    `channel_trade_no`,
    `idempotency_key`,
    `expire_time`,
    `success_time`,
    `last_callback_time`,
    `version`,
    `create_time`,
    `update_time`
)
SELECT
    CONCAT('PW', vo.order_no) AS payment_no,
    'VOUCHER' AS biz_type,
    vo.order_no AS biz_order_no,
    vo.user_id,
    'wallet' AS channel,
    COALESCE(v.title, '优惠券订单') AS title,
    COALESCE(v.sub_title, '钱包支付历史订单回填') AS description,
    COALESCE(vo.pay_amount, 0.00) AS amount,
    CASE
        WHEN vo.status = 4 OR vo.payment_status = 3 THEN COALESCE(vo.pay_amount, 0.00)
        ELSE 0.00
    END AS refunded_amount,
    CASE
        WHEN vo.status = 4 OR vo.payment_status = 3 THEN 'FULL_REFUNDED'
        ELSE 'SUCCESS'
    END AS status,
    CONCAT('WALLET_', vo.order_no) AS channel_trade_no,
    vo.payment_idempotency_key AS idempotency_key,
    vo.pay_deadline AS expire_time,
    vo.pay_time AS success_time,
    COALESCE(vo.pay_time, vo.update_time, vo.create_time) AS last_callback_time,
    1 AS version,
    COALESCE(vo.pay_time, vo.create_time, NOW()) AS create_time,
    COALESCE(vo.update_time, vo.pay_time, vo.create_time, NOW()) AS update_time
FROM `voucher_order` vo
LEFT JOIN `voucher` v ON v.id = vo.voucher_id
LEFT JOIN `payment_order` po
       ON po.biz_type = 'VOUCHER'
      AND po.biz_order_no = vo.order_no
WHERE vo.payment_method = 'wallet'
  AND vo.payment_status IN (1, 3)
  AND po.id IS NULL;
