# 校园生活 AI 服务

校园生活 AI 服务是平台的独立 AI 微服务，负责大模型对话、AI 供应商配置、模型路由、配额控制、内容审核、知识库文件存储和 AI 运维分析等能力。

## 技术栈

- Java 17
- Spring Boot 3.4
- Spring AI
- MyBatis
- MySQL
- 兼容 Milvus 的向量存储
- OpenAI 兼容模型供应商
- Maven

## 主要能力

- 智能体对话接口
- OpenAI 兼容供应商接入
- 大模型网关统一调用门面
- 模型路由规则和供应商健康状态跟踪
- 限流、配额、成本和安全策略
- 供应商、模型、路由、配额、安全规则和探测请求的后台管理接口
- 用户知识库上传和分块持久化
- Prometheus 和 CLS 兼容日志的运维分析接入

## 环境要求

- JDK 17
- Maven 3.9+
- MySQL 8+
- 可选：Milvus 或兼容向量数据库
- 至少一个 OpenAI 兼容模型供应商 API Key

## 配置说明

不要提交真实密钥。所有密钥和部署环境相关地址都应通过环境变量注入。

常用环境变量：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `AI_API_KEY_ENCRYPTION_SECRET`
- `AI_OPENAI_COMPATIBLE_BASE_URL`
- `AI_OPENAI_COMPATIBLE_API_KEY`
- `AI_OPENAI_COMPATIBLE_MODEL`
- `AI_KNOWLEDGE_STORAGE_PATH`
- `MILVUS_URI`
- `MILVUS_TOKEN`
- `PROMETHEUS_BASE_URL`
- `CLS_SECRET_ID`
- `CLS_SECRET_KEY`

## 数据库

数据库迁移脚本位于 `src/main/resources/db/migration`。

更多文档：

- `docs/README.md`
- `docs/environment-checklist.md`
- `docs/llm-gateway-framework.md`
- `docs/llm-gateway-fullstack-design.md`
- `docs/llm-gateway-rollout-summary.md`

## 本地运行

```bash
mvn spring-boot:run
```

默认本地环境监听端口为 `8083`。

## 测试

```bash
mvn test
```

## 仓库清理规则

`.gitignore` 已忽略 `target/`、日志、用户上传文件和本地环境变量文件。`uploads/` 下的知识库文件属于运行时数据，不应提交到仓库。
