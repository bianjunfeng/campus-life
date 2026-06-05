# 本地中间件 Compose

仅启动 **MySQL、Redis、RabbitMQ**，用于本机 `mvn` / `npm run dev` 开发。

```bash
cd deploy/middleware
docker compose up -d
```

| 服务 | 宿主机端口 | 默认 |
|------|-----------|------|
| MySQL | 3307 | 库 `campus_life` / `campus_life_ai`，root / `root123456` |
| Redis | 6379 | 无密码 |
| RabbitMQ | 5672、15672 | guest / guest |

**数据库初始化**来自 `deploy/database/` 快照（不再使用 `deploy/mysql/init/*.sql`）：

| 变量 | 默认 | 说明 |
|------|------|------|
| `DB_INIT_MODE` | `full` | `schema` 空表 / `full` 含数据 |
| `AI_DB_INIT_MODE` | `full` | AI 库同上 |

重新导出当前库：`powershell -File ../database/scripts/export-from-running.ps1`

全栈部署见 [`../docker/README.md`](../docker/README.md)。
