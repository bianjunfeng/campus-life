# campus-life-ai 环境配置清单

## 数据库

- 数据库实例：沿用现有 MySQL 实例即可
- 独立数据库名：`campus_life_ai`
- 独立数据库账号：`campus_life_ai_user`
- 权限范围：仅授予 `campus_life_ai.*`

## 应用配置

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/campus_life_ai?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8`
- `SPRING_DATASOURCE_USERNAME=campus_life_ai_user`
- `SPRING_DATASOURCE_PASSWORD=<你的数据库密码>`
- `JWT_SECRET=<与网关/主业务服务一致的 JWT 密钥>`

## AI Provider

- `AI_DEFAULT_PROVIDER_CODE=openai-compatible`
- `AI_DEFAULT_MODEL_CODE=gpt-4o-mini`
- `AI_OPENAI_COMPATIBLE_ENABLED=true`
- `AI_OPENAI_COMPATIBLE_BASE_URL=<供应商网关地址>`
- `AI_OPENAI_COMPATIBLE_API_KEY=<供应商 API Key>`
- `AI_OPENAI_COMPATIBLE_MODEL=<实际使用的模型编码>`

## 上线前检查

- 已执行 [ai-database-bootstrap.sql](D:/code1/code/campus-life-ai/docs/ai-database-bootstrap.sql)
- `campus_life_ai` 库已创建
- `campus_life_ai_user` 已创建并可连接数据库
- `campus-life-ai` 首次启动后已自动创建 AI 相关表
- `/actuator/health` 返回正常
- 网关路由仍指向 `campus-life-ai`
- `JWT_SECRET` 与发 token 的服务保持一致

## 约束建议

- AI 微服务不要跨库直连主业务库
- 与主业务系统仅通过接口或消息传递 `userId`、`merchantId`、`contentId` 等业务标识
- 不在数据库层建立跨库外键
