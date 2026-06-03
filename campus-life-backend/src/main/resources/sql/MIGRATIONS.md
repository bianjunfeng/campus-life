# 数据库迁移清单

主库结构以 `docs/db-schema.sql` 为基线，增量脚本位于本目录。Docker 冷启动顺序见 `deploy/docker/mysql/init/10-init-main-db.sh`。

| 文件 | Docker init | 已并入 db-schema | 说明 |
|------|:-----------:|:----------------:|------|
| migrate-payment-domain-v1.sql | 是 | 部分 | 支付域 |
| migrate-payment-reconciliation-v1.sql | 是 | 部分 | 对账 |
| migrate-payment-refund-review-v1.sql | 是 | 部分 | 退款审核 |
| migrate-payment-wallet-backfill-v1.sql | 是 | 部分 | 钱包回填 |
| migrate-system-notification.sql | 是 | 部分 | 系统通知 |
| migrate-merchant-location-v1.sql | 是 | 部分 | 商家位置 |
| migrate-merchant-profile-v1.sql | 是 | 是 | 幂等 ALTER，兼容旧库 |
| migrate-voucher-wallet-v2.sql | 是 | 部分 | 券包 |
| migrate-voucher-order-v3.sql | 是 | 部分 | 订单 |
| migrate-voucher-order-idempotency-v4.sql | 是 | 部分 | 幂等 |
| migrate-voucher-order-use-deadline-v5.sql | 是 | 部分 | 使用截止 |
| migrate-voucher-order-order-no-unique.sql | 是 | 部分 | 订单号唯一 |
| migrate-event-reliability-v1.sql | 是 | 部分 | 事件可靠性 |
| migrate-performance-indexes-v1.sql | 是 | 部分 | 性能索引 |
| migrate-admin-operation-log-v1.sql | 是 | 是 | 管理端操作日志 |
| migrate-shopping-cart-v1.sql | **否** | 是 | schema 已含购物车，重复执行会冲突 |
| migrate-auth-session-audit-v1.sql | **否** | 是 | schema 已含 auth_session / login_audit_log |

已有数据卷升级：在 `deploy/docker` 目录执行 `./scripts/migrate-existing.sh`。
