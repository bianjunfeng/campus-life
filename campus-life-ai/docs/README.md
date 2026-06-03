# campus-life-ai

独立 AI 微服务，承接以下能力域：

- 大模型对话 `chat`
- 内容生成 `generation`
- 智能推荐 `recommendation`
- 内容审核 `moderation`
- 智能客服 `customer_service`
- 数据分析 `analytics`

当前已落地：

- 聊天会话与消息持久化
- 独立数据库方案与初始化脚本自动执行
- OpenAI 兼容模型接入抽象
- 大模型网关骨架、模型路由、fallback、限流、配额、成本和安全治理
- 管理端 Provider、模型、路由规则、安全规则、配额、测试台和健康检查
- 文本内容审核接口 `/api/ai/moderation/check`
- `/api/agent/**` 兼容接口
- `/api/ai/**` 平台级预留接口
- `/api/admin/ai/**` 管理端配置 CRUD 接口

当前未落地但已预留：

- 内容生成任务执行
- 推荐召回与排序
- 审核多模态能力和图片审核
- 客服知识库与人工转接
- AI 分析任务执行

数据库表结构见：

- [ai-microservice-schema.sql](D:/code1/code/campus-life-ai/docs/ai-microservice-schema.sql)
- [ai-database-bootstrap.sql](D:/code1/code/campus-life-ai/docs/ai-database-bootstrap.sql)
- [environment-checklist.md](D:/code1/code/campus-life-ai/docs/environment-checklist.md)
- [llm-gateway-rollout-summary.md](D:/code1/code/campus-life-ai/docs/llm-gateway-rollout-summary.md)
- [llm-gateway-framework.md](D:/code1/code/campus-life-ai/docs/llm-gateway-framework.md)
- [llm-gateway-fullstack-design.md](D:/code1/code/campus-life-ai/docs/llm-gateway-fullstack-design.md)

运行方式：

- 默认使用独立库 `campus_life_ai`
- 默认使用 `local` 环境配置，启动时会自动执行 `classpath:db/migration/V1__init_ai_schema.sql`
- 生产环境使用 `SPRING_PROFILES_ACTIVE=prod`
- 初始化脚本使用 `CREATE TABLE IF NOT EXISTS` 和幂等种子数据，适合首次创建独立 AI 数据库后直接初始化
- 推荐做法是在同一个 MySQL 实例中新建独立库 `campus_life_ai`，并为 AI 微服务分配独立账号 `campus_life_ai_user`

关键环境变量：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `AI_GATEWAY_ENABLED`
- `AI_GATEWAY_MAX_INPUT_CHARS`
- `AI_GATEWAY_HEALTH_FAILURE_THRESHOLD`
- `AI_GATEWAY_HEALTH_COOLDOWN_MS`
- `AI_DEFAULT_PROVIDER_CODE`
- `AI_DEFAULT_MODEL_CODE`
- `AI_OPENAI_COMPATIBLE_ENABLED`
- `AI_OPENAI_COMPATIBLE_BASE_URL`
- `AI_OPENAI_COMPATIBLE_API_KEY`
- `AI_OPENAI_COMPATIBLE_MODEL`

推荐落库顺序：

1. 先执行 [ai-database-bootstrap.sql](D:/code1/code/campus-life-ai/docs/ai-database-bootstrap.sql) 创建独立库和账号
2. 再启动 `campus-life-ai`，由应用自动执行表结构初始化脚本
3. 按 [environment-checklist.md](D:/code1/code/campus-life-ai/docs/environment-checklist.md) 补齐环境变量
