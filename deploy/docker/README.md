# Campus Life Docker 一键部署

这套配置独立于 `deploy/demo` 的 systemd 部署方案，目标是在云服务器上用 Docker Compose 一次性启动：

- 前端 Nginx：用户端、商家端、管理端静态资源和 `/api` 反向代理
- `campus-life-gateway`
- `campus-life-backend`
- `campus-life-ai`
- MySQL、Redis、RabbitMQ

默认关闭 Kafka、Elasticsearch、Milvus、微信支付和支付宝支付，适合先把公网演示环境跑起来。需要展示支付宝沙箱或 AI 大模型时，再在 `.env` 中打开对应配置。

## 1. 服务器准备

推荐配置：

```text
最低：2 vCPU / 4 GB RAM
推荐：4 vCPU / 8 GB RAM
系统：Ubuntu 22.04 或 24.04
```

安装 Docker：

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable --now docker
```

安全组至少开放：

```text
22
80
443 如果后续接 HTTPS
```

## 2. 配置域名

把三个域名解析到服务器公网 IP：

```text
demo.example.com
merchant.demo.example.com
admin.demo.example.com
```

如果只是本地测试，可以把 `HTTP_PORT` 改成 `8088`，然后访问 `http://localhost:8088`。

## 3. 准备环境变量

在项目根目录执行：

```bash
cd deploy/docker
cp .env.example .env
nano .env
```

至少替换这些值：

```text
CONSUMER_SERVER_NAME
MERCHANT_SERVER_NAME
ADMIN_SERVER_NAME
DEMO_PUBLIC_BASE_URL
MYSQL_ROOT_PASSWORD
MYSQL_PASSWORD
REDIS_PASSWORD
RABBITMQ_DEFAULT_PASS
JWT_SECRET
AI_MYSQL_PASSWORD
AI_API_KEY_ENCRYPTION_SECRET
```

`JWT_SECRET` 必须在后端、AI、网关中保持一致。本 Compose 会把同一个变量注入三个服务。

如果本地无法访问 Docker Hub，可以先在 Docker Desktop 里配置代理或镜像加速；也可以把 `.env` 里的基础镜像变量替换成你自己的镜像仓库地址：

```text
MAVEN_IMAGE=maven:3.9-eclipse-temurin-17
JRE_IMAGE=eclipse-temurin:17-jre
NODE_IMAGE=node:20-alpine
NGINX_IMAGE=nginx:1.27-alpine
MYSQL_IMAGE=mysql:8.0
REDIS_IMAGE=redis:7-alpine
RABBITMQ_IMAGE=rabbitmq:3-management
```

例如你已经把这些镜像同步到了私有仓库，就把对应变量改成私有仓库完整镜像名。

支付宝沙箱需要公网 HTTPS 后再开启：

```text
ALIPAY_ENABLED=true
ALIPAY_APP_ID=你的沙箱 APP_ID
ALIPAY_PRIVATE_KEY=单行应用私钥，不带 BEGIN/END
ALIPAY_PUBLIC_KEY=支付宝沙箱公钥，单行
ALIPAY_NOTIFY_URL=https://demo.example.com/api/payment/alipay/notify
ALIPAY_RETURN_URL=https://demo.example.com/api/payment/alipay/return
```

AI 大模型需要配置：

```text
AI_OPENAI_COMPATIBLE_ENABLED=true
AI_OPENAI_COMPATIBLE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_OPENAI_COMPATIBLE_API_KEY=你的 API Key
AI_OPENAI_COMPATIBLE_MODEL=qwen-plus
```

## 4. 一键构建并启动

在 `deploy/docker` 目录执行：

```bash
docker compose up -d --build
```

第一次启动会做几件事：

1. 构建三个 Java 服务镜像。
2. 构建前端静态资源镜像。
3. 初始化 MySQL 主业务库。
4. 初始化 AI 独立库并导入 `campus-life-ai` 的 `V1` 到 `V8` migration。
5. 启动 Redis、RabbitMQ、后端、AI、网关和前端 Nginx。

查看状态：

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f ai
docker compose logs -f gateway
```

## 5. 验收

容器内服务健康检查：

```bash
docker compose exec frontend wget -qO- http://gateway:8090/actuator/health
docker compose exec frontend wget -qO- http://backend:8080/actuator/health
docker compose exec frontend wget -qO- http://ai:8083/actuator/health
```

公网检查：

```bash
curl -I http://demo.example.com
curl -I http://merchant.demo.example.com
curl -I http://admin.demo.example.com
curl -I http://demo.example.com/api/voucher-service/health
```

浏览器访问：

```text
http://demo.example.com
http://merchant.demo.example.com
http://admin.demo.example.com
```

## 6. 数据库初始化说明

MySQL 官方镜像只会在数据卷为空时执行 `/docker-entrypoint-initdb.d`。因此：

- 首次 `docker compose up` 会自动建表。
- 已经存在 `mysql-data` 卷时，不会重复执行 SQL。
- 如果你要重新初始化演示环境，需要先确认数据可删除，再执行：

```bash
docker compose down -v
docker compose up -d --build
```

`docker compose down -v` 会删除 MySQL、Redis、RabbitMQ、上传文件和 AI 知识库数据卷，生产环境不要随意执行。

## 7. 更新部署

代码更新后：

```bash
cd deploy/docker
docker compose up -d --build
```

只看某个服务日志：

```bash
docker compose logs -f backend
docker compose logs -f ai
docker compose logs -f gateway
docker compose logs -f frontend
```

重启服务：

```bash
docker compose restart backend ai gateway frontend
```

## 8. HTTPS 建议

这套 Compose 默认只暴露 HTTP `80`，方便一键启动。公网正式展示建议二选一：

1. 云厂商负载均衡或宝塔面板做 HTTPS，转发到服务器 `80`。
2. 在服务器宿主机安装 Caddy/Nginx 作为 HTTPS 入口，再反代到 `127.0.0.1:HTTP_PORT`。

如果使用支付宝沙箱异步回调，公网地址必须是可访问的 HTTPS，并且 `.env` 里的 `ALIPAY_NOTIFY_URL`、`ALIPAY_RETURN_URL` 要和真实域名一致。

## 9. 常见问题

- 前端能打开但接口 502：检查 `gateway`、`backend`、`ai` 容器日志。
- 登录后接口 401：确认三个 Java 服务使用同一个 `JWT_SECRET`。
- 秒杀/优惠券失败：检查 `rabbitmq` 是否 healthy，`.env` 中 RabbitMQ 密码是否变更后重建了卷。
- AI 对话失败：确认 `AI_OPENAI_COMPATIBLE_ENABLED=true` 且 API Key、Base URL、模型名正确。
- 支付宝回调失败：确认公网 HTTPS、Nginx `/api/` 代理、支付宝沙箱公钥、通知地址完全一致。
- 修改 MySQL 密码后容器仍旧用旧密码：已有 `mysql-data` 卷不会重新初始化，需要迁移数据或重建卷。
