# 部署目录说明

| 目录 | 档位 | 说明 |
|------|------|------|
| [`database/`](database/) | **L1/L2 数据库快照** | 从运行库导出的 `schema.sql` / `full.sql` |
| [`docker/`](docker/) | **L1 / L2** | 全栈 Docker Compose |
| [`middleware/`](middleware/) | 本地开发 | 仅中间件，配合本机启动 Java/Vue |
| [`demo/`](demo/) | 裸机演示 | systemd + Nginx，非 Docker 全栈 |

**快速选择**

- 云服务器公网演示 → `deploy/docker`（L1 默认，L2 加 `--profile full`）
- 本机写代码调试 → `deploy/middleware` 或 IDE 直连已有库
- 已有 Linux + systemd 习惯 → `deploy/demo`
