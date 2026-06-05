# 数据库初始化快照（唯一来源）

从**当前运行中的 MySQL** 导出的两个版本，不再依赖 `db-schema.sql`、`migrate-*.sql`、`V*.sql` 等历史脚本做 Docker 冷启动。

## 目录

```text
deploy/database/
├── campus_life/
│   ├── schema.sql    # 版本 A：仅表结构（空库）
│   └── full.sql      # 版本 B：表结构 + 当前全部数据
├── campus_life_ai/
│   ├── schema.sql
│   └── full.sql
├── MANIFEST.json     # 导出时间、来源库、表数量
└── scripts/
    ├── export-from-running.sh
    └── export-from-running.ps1
```

## 两个版本

| 版本 | 文件 | 环境变量 | 说明 |
|------|------|----------|------|
| **schema** | `schema.sql` | `DB_INIT_MODE=schema` | 只有 DDL，无业务数据 |
| **full** | `full.sql` | `DB_INIT_MODE=full` | DDL + 当前库全部 DML |

主库、AI 库分别用 `DB_INIT_MODE` / `AI_DB_INIT_MODE` 控制。

## 重新导出（表结构或数据有变更时）

确保 MySQL 容器在运行（默认 `campus-life-mysql`）：

**Windows**

```powershell
powershell -ExecutionPolicy Bypass -File deploy/database/scripts/export-from-running.ps1
```

**Linux / macOS**

```bash
bash deploy/database/scripts/export-from-running.sh
```

可选环境变量：`MYSQL_CONTAINER`、`MYSQL_ROOT_PASSWORD`、`MAIN_DB`、`AI_DB`。

## Docker 使用

**空库（L1 公网默认）**

```bash
cd deploy/docker
DB_INIT_MODE=schema AI_DB_INIT_MODE=schema docker compose down -v
DB_INIT_MODE=schema AI_DB_INIT_MODE=schema docker compose up -d mysql
```

**带现有数据（本地/演示）**

```bash
cd deploy/docker
DB_INIT_MODE=full AI_DB_INIT_MODE=full docker compose down -v
DB_INIT_MODE=full AI_DB_INIT_MODE=full docker compose up -d mysql
```

或在 `.env` 中设置：

```env
DB_INIT_MODE=full
AI_DB_INIT_MODE=full
```

> MySQL 官方镜像**仅在数据卷为空时**执行 init；切换版本需 `docker compose down -v` 或手动导入。

## 本地 middleware

`deploy/middleware` 默认 `DB_INIT_MODE=full`，方便本机开发直接复现当前数据。

## 历史 SQL 文件

旧的 `deploy/init_sql/` 和 `deploy/mysql/` 已删除。以下 SQL 仍保留为参考或应用内迁移用途，**不参与** Docker / middleware 冷启动：

- `campus-life-backend/docs/db-schema.sql`
- `campus-life-backend/src/main/resources/sql/migrate-*.sql`
- `campus-life-ai/src/main/resources/db/migration/V*.sql`

以后以本目录 `schema.sql` / `full.sql` 为准，变更后重新执行导出脚本即可。
