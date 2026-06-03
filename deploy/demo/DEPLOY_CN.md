# 校园生活公网演示部署执行清单

目标：部署原有项目代码，但使用 `demo` 运行环境配置，公网演示支付宝沙箱支付闭环。

## 一、已经在仓库中准备好的文件

- `deploy/demo/.env.example`：服务器环境变量模板，不包含真实密钥。
- `deploy/demo/backend/application-demo.yml`：后端 demo profile 配置，启用支付宝沙箱。
- `deploy/demo/gateway/application.yml`：网关公网演示配置。
- `deploy/demo/ai/application-demo.yml`：AI 微服务 demo 配置。
- `deploy/demo/ai/create-ai-database.sql`：AI 独立库和账号初始化模板。
- `deploy/demo/nginx/campus-life.conf`：学生端、商家端、管理端三个域名的 Nginx 配置。
- `deploy/demo/systemd/campus-life-backend.service`：后端 systemd 服务，使用 `--spring.profiles.active=demo`。
- `deploy/demo/systemd/campus-life-ai.service`：AI 微服务 systemd 服务。
- `deploy/demo/systemd/campus-life-gateway.service`：网关 systemd 服务。
- `deploy/demo/README.md`：完整部署说明。

## 二、本地构建

在本机执行：

```bash
cd D:/code1/code/campus-life-frontend
npm run build

cd D:/code1/code/campus-life-backend
mvn -Dmaven.repo.local=D:/code1/code/campus-life-backend/target/.m2repo -DskipTests package

cd D:/code1/code/campus-life-gateway
mvn -Dmaven.repo.local=D:/code1/code/campus-life-gateway/target/.m2repo -DskipTests package

cd D:/code1/code/campus-life-ai
mvn -Dmaven.repo.local=D:/code1/code/campus-life-ai/target/.m2repo -DskipTests package
```

当前前端实际输出目录是：

```text
D:/code1/dist/consumer-web
D:/code1/dist/merchant-web
D:/code1/dist/admin-web
```

后端和网关 jar：

```text
D:/code1/code/campus-life-backend/target/campus-life-backend-0.0.1-SNAPSHOT.jar
D:/code1/code/campus-life-gateway/target/campus-life-gateway-0.0.1-SNAPSHOT.jar
D:/code1/code/campus-life-ai/target/campus-life-ai-0.0.1-SNAPSHOT.jar
```

## 三、服务器目录

在服务器执行：

```bash
sudo apt update
sudo apt install -y nginx docker.io docker-compose-plugin openjdk-17-jre mysql-client
sudo systemctl enable --now docker nginx
sudo useradd -r -s /usr/sbin/nologin campus || true
sudo mkdir -p /opt/campus-life/{frontend/consumer-web,frontend/merchant-web,frontend/admin-web,backend/config,gateway/config,ai/config,ai/migration,data/uploads,data/ai-knowledge,mysql/data,redis/data,rabbitmq/data}
sudo chown -R campus:campus /opt/campus-life
```

## 四、上传映射

```text
D:/code1/dist/consumer-web                       -> /opt/campus-life/frontend/consumer-web
D:/code1/dist/merchant-web                       -> /opt/campus-life/frontend/merchant-web
D:/code1/dist/admin-web                          -> /opt/campus-life/frontend/admin-web
campus-life-backend-0.0.1-SNAPSHOT.jar           -> /opt/campus-life/backend/campus-life-backend.jar
campus-life-gateway-0.0.1-SNAPSHOT.jar           -> /opt/campus-life/gateway/campus-life-gateway.jar
campus-life-ai-0.0.1-SNAPSHOT.jar                -> /opt/campus-life/ai/campus-life-ai.jar
deploy/demo/docker-compose.yml                   -> /opt/campus-life/docker-compose.yml
deploy/demo/.env.example                         -> /opt/campus-life/.env
deploy/demo/backend/application-demo.yml         -> /opt/campus-life/backend/config/application-demo.yml
deploy/demo/gateway/application.yml              -> /opt/campus-life/gateway/config/application.yml
deploy/demo/ai/application-demo.yml              -> /opt/campus-life/ai/config/application-demo.yml
deploy/demo/ai/create-ai-database.sql            -> /opt/campus-life/ai/create-ai-database.sql
campus-life-ai/src/main/resources/db/migration/*.sql -> /opt/campus-life/ai/migration/
deploy/demo/nginx/campus-life.conf               -> /etc/nginx/sites-available/campus-life.conf
deploy/demo/systemd/campus-life-backend.service  -> /etc/systemd/system/campus-life-backend.service
deploy/demo/systemd/campus-life-ai.service       -> /etc/systemd/system/campus-life-ai.service
deploy/demo/systemd/campus-life-gateway.service  -> /etc/systemd/system/campus-life-gateway.service
```

## 五、服务器环境变量

编辑：

```bash
sudo nano /opt/campus-life/.env
sudo chmod 600 /opt/campus-life/.env
```

必须替换：

```text
MYSQL_ROOT_PASSWORD
MYSQL_PASSWORD
REDIS_PASSWORD
RABBITMQ_DEFAULT_PASS
JWT_SECRET
DEMO_PUBLIC_BASE_URL
ALIPAY_APP_ID
ALIPAY_PRIVATE_KEY
ALIPAY_PUBLIC_KEY
ALIPAY_NOTIFY_URL
ALIPAY_RETURN_URL
AI_MYSQL_PASSWORD
AI_API_KEY_ENCRYPTION_SECRET
AI_OPENAI_COMPATIBLE_API_KEY
```

支付宝沙箱建议保持：

```text
ALIPAY_ENABLED=true
ALIPAY_SKIP_NOTIFY_SIGN_VERIFY=false
ALIPAY_GATEWAY_URL=https://openapi-sandbox.dl.alipaydev.com/gateway.do
WECHAT_ENABLED=false
AI_OPENAI_COMPATIBLE_ENABLED=true
AI_KNOWLEDGE_VECTOR_ENABLED=false
```

默认关闭 Milvus 向量库，AI 知识库用 MySQL 分片和关键词回退，适合简历公网演示。要展示向量检索时再加 Milvus。

## 六、初始化 AI 独立数据库

先启动 MySQL 容器：

```bash
cd /opt/campus-life
sudo docker compose --env-file .env up -d
```

编辑 AI 建库 SQL，把 `CHANGE_ME_AI_MYSQL_PASSWORD` 改成 `.env` 里的 `AI_MYSQL_PASSWORD`：

```bash
sudo nano /opt/campus-life/ai/create-ai-database.sql
set -a
. /opt/campus-life/.env
set +a
sudo docker exec -i campus-demo-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /opt/campus-life/ai/create-ai-database.sql
```

导入 AI 表结构，按顺序只执行一次：

```bash
set -a
. /opt/campus-life/.env
set +a
sudo docker exec -i campus-demo-mysql mysql -ucampus_life_ai_user -p"$AI_MYSQL_PASSWORD" campus_life_ai_demo < /opt/campus-life/ai/migration/V1__init_ai_schema.sql
sudo docker exec -i campus-demo-mysql mysql -ucampus_life_ai_user -p"$AI_MYSQL_PASSWORD" campus_life_ai_demo < /opt/campus-life/ai/migration/V2__conversation_runtime_route.sql
sudo docker exec -i campus-demo-mysql mysql -ucampus_life_ai_user -p"$AI_MYSQL_PASSWORD" campus_life_ai_demo < /opt/campus-life/ai/migration/V3__knowledge_base.sql
sudo docker exec -i campus-demo-mysql mysql -ucampus_life_ai_user -p"$AI_MYSQL_PASSWORD" campus_life_ai_demo < /opt/campus-life/ai/migration/V4__enable_role_agent_scenes.sql
```

注意：`V2__conversation_runtime_route.sql` 不能重复执行，因为里面是普通 `ALTER TABLE`。

## 七、启动顺序

```bash
cd /opt/campus-life
sudo docker compose --env-file .env up -d

sudo systemctl daemon-reload
sudo systemctl enable --now campus-life-backend
sudo systemctl enable --now campus-life-ai
sudo systemctl enable --now campus-life-gateway

sudo ln -sf /etc/nginx/sites-available/campus-life.conf /etc/nginx/sites-enabled/campus-life.conf
sudo nginx -t
sudo systemctl reload nginx
```

HTTP 正常后申请 HTTPS：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d demo.example.com -d merchant.demo.example.com -d admin.demo.example.com
```

## 八、验收顺序

```bash
curl http://127.0.0.1:8080/actuator/health
curl http://127.0.0.1:8083/actuator/health
curl http://127.0.0.1:8090/actuator/health
curl -I https://demo.example.com
curl -I https://demo.example.com/api/voucher-service/health
```

浏览器验证：

```text
https://demo.example.com
https://merchant.demo.example.com
https://admin.demo.example.com
```

支付演示链路：

```text
学生登录 -> 福利/优惠券 -> 创建订单 -> 选择支付宝 -> 跳转沙箱支付
-> 支付宝异步 notify -> 后端验签和验金额 -> 订单支付成功
-> 管理端查看订单/退款/支付对账
```

AI 演示链路：

```text
登录学生/商家/管理员 -> 打开 AI 助手 -> 发起校园生活问题
-> 网关 /api/agent/** 路由到 campus-life-ai
-> AI 服务调用 OpenAI-compatible Provider
-> 返回并落库会话、消息、调用日志
```

知识库演示链路：

```text
创建知识库 -> 上传 txt/md -> AI 服务解析并写入 MySQL 分片
-> 提问时使用关键词回退检索 -> 模型结合检索内容回答
```

## 八、简历展示说明

推荐写法：

```text
在线演示：https://demo.example.com
演示账号：学生 / 商家 / 管理员
支付说明：演示环境接入支付宝沙箱，支持支付单创建、异步回调验签、金额校验、订单状态流转、退款审核和支付对账。
AI说明：演示环境接入 OpenAI-compatible 模型服务，支持 AI 助手、会话落库、知识库上传与检索增强问答。
```
