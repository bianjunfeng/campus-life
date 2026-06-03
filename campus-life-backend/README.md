# 校园生活后端服务

校园生活后端服务是平台的主业务服务，基于 Spring Boot 构建，负责用户、认证、论坛、商家、优惠券、订单、支付、消息、搜索、文件和后台管理等核心业务。

## 技术栈

- Java 17
- Spring Boot 3.4
- MyBatis
- MySQL
- Redis
- RabbitMQ
- Kafka
- Elasticsearch
- Maven

## 主要模块

- `admin`：后台管理接口和操作日志
- `auth`：登录、令牌刷新、第三方登录、验证码
- `forum`：帖子、评论、点赞、收藏、举报、敏感词过滤
- `merchant`：商家资料和商家认证审核
- `voucher`：优惠券、秒杀券、购物车
- `order`：优惠券订单流程
- `payment`：支付、退款、对账
- `message`：聊天和通知
- `search`：MySQL 与 Elasticsearch 搜索集成
- `user`：用户资料、关注关系、学生认证

## 环境要求

- JDK 17
- Maven 3.9+
- MySQL 8+
- Redis 6+
- 可选：RabbitMQ、Kafka、Elasticsearch

## 配置说明

不要提交真实密钥、密码或私钥。开发、测试、生产环境的敏感配置应通过环境变量注入。

常用环境变量：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_REDIS_HOST`
- `SPRING_REDIS_PORT`
- `SPRING_REDIS_PASSWORD`
- `SPRING_RABBITMQ_PASSWORD`
- `JWT_SECRET`
- `OSS_ACCESS_KEY_ID`
- `OSS_ACCESS_KEY_SECRET`
- `ALIPAY_APP_ID`
- `ALIPAY_PRIVATE_KEY`
- `ALIPAY_PUBLIC_KEY`
- `KAFKA_BOOTSTRAP_SERVERS`
- `ES_URIS`

## 本地运行

```bash
mvn spring-boot:run
```

默认开发环境监听端口为 `8080`。

## 测试

```bash
mvn test
```

## 仓库清理规则

`.gitignore` 已忽略 `target/`、日志、本地上传文件和本地环境变量文件。默认敏感词字典请使用 `src/main/resources/sensitive-words.txt`，不要提交运行时生成的上传数据。
