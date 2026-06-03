# 校园生活网关服务

校园生活网关服务是平台的 Spring Cloud Gateway 入口，负责把前端 API 流量转发到主业务后端、优惠券服务、AI 服务和灰度后端服务。

## 技术栈

- Java 17
- Spring Boot 3.4
- Spring Cloud Gateway
- 响应式 Redis 限流
- JWT 鉴权
- Micrometer Prometheus 指标
- Maven

## 路由说明

默认路由目标：

- 主业务后端接口：`http://localhost:8080`
- 优惠券服务接口：`http://localhost:8081`
- 灰度后端接口：`http://localhost:8082`
- AI 服务接口：`http://localhost:8083`

网关自身监听端口为 `8090`。

## 配置说明

不要提交真实密钥。密钥和部署环境相关地址应通过环境变量注入。

常用环境变量：

- `SPRING_REDIS_HOST`
- `SPRING_REDIS_PORT`
- `SPRING_REDIS_PASSWORD`
- `JWT_SECRET`
- `GATEWAY_BACKEND_URI`
- `GATEWAY_VOUCHER_URI`
- `GATEWAY_AI_URI`
- `GATEWAY_GRAY_ENABLED`
- `GATEWAY_GRAY_BACKEND_URI`
- `GATEWAY_RATE_LIMIT_REPLENISH_RATE`
- `GATEWAY_RATE_LIMIT_BURST_CAPACITY`
- `GATEWAY_RATE_LIMIT_REQUESTED_TOKENS`

## 本地运行

```bash
mvn spring-boot:run
```

前端 API 流量应指向：

```text
http://localhost:8090
```

## 测试

```bash
mvn test
```

## 仓库清理规则

`.gitignore` 已忽略 `target/`、日志、上传文件和本地环境变量文件。构建产物应在本地重新生成，不应提交到仓库。
