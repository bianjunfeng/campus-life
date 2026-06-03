# AI 能力平台与大模型对话接口完善方案

## 1. 文档说明

- 文档目标：基于当前 `campus-life` 项目的真实代码现状，设计一套可上线、可扩展、可运营的 AI 能力平台。
- 适用范围：用户端智能体助手、AI 内容生成、智能推荐、内容审核、智能客服、AI 数据分析、后台 AI 管理、后端大模型调用与能力编排。
- 输出内容：现状诊断、产品需求文档、技术实施方案、分阶段交付计划、验收标准。

## 2. 当前项目现状

### 2.1 已有能力

- 后端已存在 AI 模块目录：`ai/controller`、`ai/service`、`ai/dto`、`ai/tools`。
- 前端已存在 AI 页面：`src/views/agent/AgentListPage.vue`、`src/views/agent/AgentChatPage.vue`。
- 权限体系已预留 `ai:use`，且 `STUDENT`、`MERCHANT`、`ADMIN` 已拥有该权限。
- 后端已有 `UserTool`、`MerchantTool`，说明项目已经考虑过 AI 模块与业务模块的防腐层隔离。
- 项目已有消息模块、敏感词过滤、搜索模块、Redis、Elasticsearch、后台管理入口，这些都可以复用。

### 2.2 现有问题

#### 2.2.1 接口契约不一致

- 后端 `ChatResponse` 字段为：
  - `userMessage`
  - `agentMessage`
  - `sessionId`
  - `timestamp`
  - `error`
- 前端 `src/api/agent.ts` 期望字段为：
  - `response`
  - `sessionId`
  - `finished`
  - `error`
- 结果：当前前后端无法稳定正确对接，存在天然兼容性问题。

#### 2.2.2 会话数据只保存在内存中

- `AgentServiceImpl` 使用 `ConcurrentHashMap<Long, Map<String, List<ChatResponse>>>` 保存历史。
- 服务重启即丢失会话。
- 无法跨实例共享。
- 无法支持分页、检索、归档、统计、审计。

#### 2.2.3 会话列表能力缺失

- 后端只有：
  - `POST /api/agent/chat`
  - `GET /api/agent/history`
  - `DELETE /api/agent/history`
- 没有真正的“会话列表”接口。
- 前端 `AgentListPage.vue` 通过 `localStorage` 伪造会话列表，和服务端状态不一致。

#### 2.2.4 未真正接入大模型

- 当前实现仅回显：`已收到你的消息：xxx`。
- 未接入任何模型供应商。
- 未实现 system prompt、上下文拼装、参数控制、超时重试、限流、token 统计、错误分类。

#### 2.2.5 工具层未被编排使用

- `UserTool`、`MerchantTool` 已存在，但没有被实际聊天流程调用。
- 当前 AI 能力无法读取平台内业务数据，无法形成“校园生活助手”的真实价值。

#### 2.2.6 管理后台只有静态展示页

- 前端已有 `AdminAI.vue` 路由与页面，但目前只是规划说明，不具备：
  - 模型配置
  - 供应商切换
  - API Key 管理
  - 提示词管理
  - 调用统计
  - 限流与灰度开关

### 2.3 可以直接复用的基础设施

- 权限体系：`RolePermissionRegistry`、`PermissionCode.AI_USE`
- 消息模块设计范式：会话、消息、分页、未读数、权限校验
- 敏感词服务：`SensitiveFilterService`
- 搜索能力：`PostSearchService`、`UserSearchService`、Elasticsearch
- 缓存与分布式能力：Redis
- 管理后台入口：`/admin/ai`
- 统一返回结构：`ApiResponse`

## 3. 产品定位

### 3.1 产品名称

校园生活智能助手

### 3.2 产品目标

为平台用户提供统一的 AI 助手入口，围绕校园生活、商家服务、平台使用、内容创作与问题咨询，提供高可用、可追溯、可运营的大模型对话能力。

### 3.3 核心价值

- 对学生：校园问答、活动建议、帖子润色、生活服务指引。
- 对商家：商家入驻指引、券活动文案辅助、运营建议、常见后台操作问答。
- 对平台：提升留存、增强内容生产效率、沉淀用户需求数据。

### 3.4 AI 能力平台范围

本项目后续 AI 建设不应只围绕“聊天”单点展开，而应定义为统一 AI 平台，下挂六类能力域：

| 能力域 | 当前状态 | 后续目标 |
| --- | --- | --- |
| 大模型对话接口 | 已有原型 | 多模型、多轮对话、上下文记忆、历史管理、调用统计 |
| AI 内容生成 | 未落地 | 标题生成、摘要提取、推荐文案、多语言、风格定制 |
| 智能推荐系统 | 未落地 | 个性化推荐、协同过滤、实时更新、效果分析、A/B 测试 |
| AI 内容审核 | 有敏感词基础 | 文本审核、图片审核、自动标记、规则配置、审核日志 |
| 智能客服助手 | 未落地 | FAQ 问答、多轮对话、知识库、人工转接、满意度评价 |
| AI 数据分析 | 未落地 | 智能报表、趋势预测、异常检测、用户画像、可视化洞察 |

结论：

- 本期优先建设“大模型对话接口”。
- 但接口与技术骨架必须一次性为全部能力域留好扩展位。
- 后续各能力域应复用统一的模型接入、配置中心、日志中心、风控与指标体系，而不是各做一套。

## 4. 产品需求文档

### 4.1 用户角色

| 角色 | 使用权限 | 主要诉求 |
| --- | --- | --- |
| 学生用户 | `ai:use` | 校园问答、内容创作、生活建议 |
| 商家用户 | `ai:use` | 商家运营问答、营销辅助、流程指引 |
| 管理员 | `ai:use` + 管理后台权限 | 配置模型、查看调用、控制成本、排查问题 |

### 4.2 核心场景

#### 场景 A：普通对话

- 用户进入“智能体助手”列表页。
- 点击“新建对话”进入聊天页。
- 输入问题后，系统返回 AI 回复。
- 用户刷新页面或重新进入后，能继续看到历史对话。

#### 场景 B：多轮连续对话

- 用户在同一会话中连续追问。
- 系统保留最近 N 轮上下文。
- 会话标题可自动生成，也可手动重命名。

#### 场景 C：校园业务问答

- 用户咨询“怎么申请商家认证”“优惠券怎么退款”“如何查看我的订单”等平台内问题。
- 系统可通过业务工具和检索结果给出准确回答，而不是纯泛化生成。

#### 场景 D：后台配置与运营

- 管理员可选择启用的模型供应商。
- 管理员可配置默认模型、温度、超时时间、单用户限额、提示词模板。
- 管理员可查看调用次数、成功率、平均耗时、token 消耗、失败原因。

### 4.3 功能范围

#### 4.3.1 用户端

1. 会话列表
2. 新建会话
3. 历史会话查看
4. 发送消息
5. AI 回复展示
6. 会话删除
7. 会话重命名
8. 自动会话标题生成
9. 多轮上下文记忆
10. 消息失败重试
11. 加载中状态
12. 后续阶段支持流式输出

#### 4.3.2 AI 内容生成

1. 智能标题生成
2. 内容摘要提取
3. 推荐文案生成
4. 多语言改写
5. 风格定制输出
6. 后续扩展到帖子、商家券、活动介绍等业务场景

#### 4.3.3 智能推荐系统

1. 个性化内容推荐
2. 个性化优惠券推荐
3. 个性化商家推荐
4. 推荐召回与排序策略切换
5. 推荐效果分析
6. A/B 测试支持
7. 实时推荐刷新

#### 4.3.4 AI 内容审核

1. 文本敏感词检测
2. 模型辅助违规判定
3. 图片内容识别接口预留
4. 违规内容自动标记
5. 审核规则配置
6. 审核日志记录

#### 4.3.5 智能客服助手

1. 常见问题自动回答
2. 多轮上下文客服对话
3. 知识库问答
4. 人工转接接口预留
5. 会话评价与满意度记录

#### 4.3.6 AI 数据分析

1. 智能报表生成
2. 趋势预测分析
3. 异常检测告警
4. 用户画像分析
5. 数据可视化结果输出

#### 4.3.7 后台管理

1. AI 总开关
2. 供应商配置管理
3. 默认模型配置
4. 系统提示词模板管理
5. 场景提示词管理
6. 限流与额度配置
7. 调用日志查看
8. 失败日志查看
9. 成本与 token 统计
10. 模型联调测试

#### 4.3.8 平台能力

1. 大模型请求编排
2. 会话与消息持久化
3. 内容安全校验
4. 超时、重试、降级
5. 调用审计
6. 工具调用
7. 检索增强
8. 知识库接入预留
9. 推荐特征服务接入预留
10. 审核多模态能力接入预留

### 4.4 非功能性要求

| 维度 | 要求 |
| --- | --- |
| 可用性 | AI 服务异常时不影响主站其他业务 |
| 可恢复性 | 单次调用失败可重试，历史记录不丢失 |
| 性能 | 同步模式首包目标小于 3 秒，普通回答完成目标小于 15 秒 |
| 安全性 | API Key 不明文落库，敏感词和违规内容要拦截 |
| 可观测性 | 具备请求日志、耗时、token、错误码、供应商维度统计 |
| 可扩展性 | 支持增加多供应商、多模型、多助手场景 |

### 4.5 业务规则

1. 仅登录且具备 `ai:use` 权限的用户可使用对话功能。
2. 单条用户输入建议限制 2000 字符。
3. 默认仅保留最近 20 轮上下文进入模型，完整消息历史仍持久化保存。
4. 用户可删除自己的会话，但后台日志可保留摘要与调用元数据。
5. 平台内业务问答优先走工具/检索增强，避免模型胡编。
6. 命中敏感词或安全策略时，直接拦截或返回安全提示。

### 4.6 MVP 边界

#### 本期必须上线

- 真正可用的对话接口
- MySQL 持久化会话和消息
- 统一前后端协议
- 单模型供应商接入
- 同步非流式应答
- 会话列表与历史消息分页
- AI 管理后台基础配置页

#### 本期不强制上线

- 多模态图片理解
- 复杂 function calling 自动规划
- 向量数据库
- Agent 工作流编排
- 语音输入输出

### 4.7 接口保留与扩展原则

用户明确要求后续要实现“大模型对话接口、AI 内容生成、智能推荐、AI 内容审核、智能客服助手、AI 数据分析”全部能力，因此本项目接口设计必须遵循以下原则：

1. 统一保留 `/api/ai/*` 或 `/api/admin/ai/*` 作为平台级命名空间，避免后续能力四散到多个不相关前缀。
2. 聊天、生成、审核、推荐、分析都复用统一的 `providerCode`、`modelCode`、`sceneCode`、`requestId`、`usage` 结构。
3. 本期未启用的接口可以先返回“未开通/未启用”，但 URI、请求体、响应体结构应尽量固定。
4. 所有能力域都通过统一“能力注册表 + 场景配置 + 供应商适配器”扩展，不允许每个模块私自直连外部模型。
5. 对前端公开的接口只做向后兼容升级，不做破坏式字段替换。

### 4.8 建议预留的能力域接口

#### 用户侧接口

| 方法 | 路径 | 当前是否实现 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/ai/chat/conversations` | 本期实现 | 创建聊天会话 |
| GET | `/api/ai/chat/conversations` | 本期实现 | 获取聊天会话列表 |
| POST | `/api/ai/chat/conversations/{sessionId}/messages` | 本期实现 | 发送聊天消息 |
| POST | `/api/ai/generation/tasks` | 预留 | 发起内容生成任务 |
| POST | `/api/ai/recommendations/query` | 预留 | 查询个性化推荐结果 |
| POST | `/api/ai/moderation/check` | 预留 | 文本或内容审核 |
| POST | `/api/ai/customer-service/sessions` | 预留 | 发起客服会话 |
| POST | `/api/ai/analytics/query` | 预留 | 发起分析查询 |

#### 管理侧接口

| 方法 | 路径 | 当前是否实现 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/admin/ai/providers` | 本期实现 | 模型供应商配置 |
| GET | `/api/admin/ai/scenes` | 预留 | 场景模板与参数配置 |
| GET | `/api/admin/ai/capabilities` | 预留 | 能力开关配置 |
| GET | `/api/admin/ai/logs` | 本期实现 | 调用日志 |
| GET | `/api/admin/ai/stats/overview` | 本期实现 | 统计概览 |
| GET | `/api/admin/ai/recommendation/experiments` | 预留 | A/B 测试管理 |
| GET | `/api/admin/ai/moderation/rules` | 预留 | 审核规则配置 |
| GET | `/api/admin/ai/customer-service/knowledge-bases` | 预留 | 知识库管理 |

## 5. 目标产品设计

### 5.1 用户端信息架构

#### 页面一：智能体会话列表页

- 顶部：标题、搜索、创建会话按钮
- 中部：会话列表
  - 会话标题
  - 最近一条消息预览
  - 最近更新时间
  - 助手类型标签
- 操作：进入会话、删除、重命名、置顶（可选）
- 空状态：推荐快捷问题

#### 页面二：聊天页

- 顶部：返回、会话标题、更多操作
- 中部：消息流
  - 用户消息
  - AI 消息
  - 错误消息
  - 重试入口
- 底部：输入框、发送按钮、停止生成按钮（流式阶段）

### 5.2 助手场景定义

建议保留统一入口，但在系统内部支持 `assistantType`：

| assistantType | 面向人群 | 说明 |
| --- | --- | --- |
| `general` | 全部用户 | 通用校园生活问答 |
| `campus_qa` | 学生 | 校园平台与生活问答 |
| `merchant_ops` | 商家 | 商家功能指引与运营建议 |
| `content_helper` | 全部用户 | 帖子润色、摘要、标题建议 |

说明：

- 列表页中现在的“智能对话 / 知识问答 / 快速开始”可映射为 `assistantType`。
- 初期前端不必暴露复杂模型选择，只暴露助手场景。

## 6. 技术实施方案

### 6.1 总体架构

采用“控制器 + 会话服务 + 模型编排服务 + 供应商适配器 + 工具层 + 持久化层 + 运维配置层”的分层结构。

```text
Frontend Agent UI
    -> /api/agent/conversations
    -> AgentController / AgentConversationController
    -> AgentConversationService
    -> AgentChatOrchestrator
       -> PromptAssembler
       -> ContextWindowManager
       -> ToolExecutor
       -> LlmProvider
       -> UsageRecorder
    -> MyBatis Mapper
    -> MySQL / Redis / Elasticsearch
```

为了满足你后续全部 AI 模块都要接入且接口要长期保留，建议把总体架构再抽象成“AI 平台内核 + 能力域适配层”的两层结构：

```text
AI Platform Core
    -> ProviderSelector
    -> SceneRegistry
    -> PromptTemplateService
    -> SafetyGuard
    -> UsageRecorder
    -> QuotaService
    -> CapabilityRouter

Capability Domains
    -> ChatCapability
    -> ContentGenerationCapability
    -> RecommendationCapability
    -> ModerationCapability
    -> CustomerServiceCapability
    -> AnalyticsCapability
```

这样做的目的不是把所有东西一次写完，而是先把扩展边界固定下来，后续各能力域接入时不需要重做底座。

### 6.2 AI 平台统一抽象

建议引入三类统一抽象，后续所有能力域共享：

#### 6.2.1 `sceneCode`

用来描述调用场景，而不是把逻辑散落在不同接口内部。示例：

- `chat.general`
- `chat.campus_qa`
- `chat.merchant_ops`
- `generation.post_title`
- `generation.post_summary`
- `generation.marketing_copy`
- `moderation.text_post`
- `moderation.image_post`
- `customer_service.faq`
- `analytics.operation_report`
- `recommend.feed`
- `recommend.voucher`

#### 6.2.2 `capabilityCode`

用来描述能力域：

- `chat`
- `generation`
- `recommendation`
- `moderation`
- `customer_service`
- `analytics`

#### 6.2.3 统一调用命令 `AiInvokeCommand`

建议所有能力域在服务内部都转换为统一命令，再进入平台内核：

```java
public class AiInvokeCommand {
    private String capabilityCode;
    private String sceneCode;
    private Long userId;
    private String providerCode;
    private String modelCode;
    private Map<String, Object> input;
    private Map<String, Object> options;
}
```

价值：

- 后续新增能力域不需要复制一套模型调用基础设施。
- 调用日志、风控、限流、供应商切换都可以统一处理。
- 管理后台可以按 `capabilityCode` 和 `sceneCode` 配置。

### 6.3 推荐落地原则

1. 保持现有 `/api/agent` 命名空间，不大改前端路由。
2. 先做“稳定同步模式”，再扩展“流式输出”。
3. 不直接引入重量级 Agent 框架，先走可控的自研编排层。
4. 优先复用现有消息模块、搜索模块、敏感词模块与后台管理入口。
5. 外部大模型供应商采用抽象接口，避免写死到某一家。

补充约束：

6. 用户端聊天可继续保留 `/api/agent/*` 兼容入口，但平台级新能力统一落到 `/api/ai/*`。
7. 本期先实现聊天域，其他能力域只保留稳定 DTO 和 Controller 壳层。
8. 推荐系统和 AI 分析未必全部依赖大模型，但也要接入统一能力注册和日志体系。

### 6.4 后端模块设计

#### 6.4.1 新增或重构的核心类

建议在 `com.campus.campus_life_backend.ai` 下新增如下分层：

- `controller`
  - `AgentConversationController`
  - `AiCapabilityController`
  - `AdminAiController`
- `dto`
  - `CreateConversationRequest`
  - `ConversationDTO`
  - `ConversationDetailDTO`
  - `AgentMessageDTO`
  - `SendMessageRequest`
  - `SendMessageResponse`
  - `RenameConversationRequest`
  - `ProviderConfigDTO`
- `entity`
  - `AiConversation`
  - `AiMessage`
  - `AiProviderConfig`
  - `AiCallLog`
- `mapper`
  - `AiConversationMapper`
  - `AiMessageMapper`
  - `AiProviderConfigMapper`
  - `AiCallLogMapper`
- `service`
  - `AgentConversationService`
  - `AgentChatOrchestrator`
  - `AiCapabilityRegistry`
  - `AiInvokeService`
  - `PromptTemplateService`
  - `UsageStatService`
  - `AiSafetyService`
  - `AiQuotaService`
- `capability`
  - `ChatCapability`
  - `GenerationCapability`
  - `RecommendationCapability`
  - `ModerationCapability`
  - `CustomerServiceCapability`
  - `AnalyticsCapability`
- `provider`
  - `LlmProvider`
  - `OpenAiCompatibleProvider`
  - `ProviderSelector`
- `tool`
  - `ToolExecutor`
  - 现有 `UserTool`
  - 现有 `MerchantTool`

其中：

- `AiCapabilityRegistry` 负责根据 `capabilityCode` 或 `sceneCode` 路由到具体能力域。
- `AiInvokeService` 负责执行统一调用链：权限校验、限流、提示词组装、安全校验、供应商调用、日志记录。
- 各 `Capability` 负责本领域输入输出适配，不直接处理底层模型细节。

#### 6.4.2 供应商抽象

定义统一接口：

```java
public interface LlmProvider {
    ChatCompletionResult chat(ChatCompletionCommand command);
    StreamingHandle stream(ChatCompletionCommand command);
    HealthCheckResult healthCheck();
    String providerCode();
}
```

说明：

- `OpenAiCompatibleProvider` 可兼容多种 OpenAI 风格接口供应商。
- 这样能覆盖大部分主流商用模型接口，而不影响业务层代码。

#### 6.4.3 为什么不建议本期直接引入 Spring AI

当前项目已经是明确的 Spring Boot + MyBatis + 手工模块化结构，且已有自定义权限、日志、后台配置体系。本期目标是尽快把对话接口从“演示版”升级为“可上线版”，因此更适合：

- 使用 `RestClient` 或 `WebClient` 直接接入供应商；
- 保持 DTO、日志、审计、错误处理完全可控；
- 避免在首期引入额外抽象层，增加学习和调试成本。

后续如果业务扩展到复杂多模型、多代理、多模态，再评估是否迁移到统一 AI 框架。

### 6.5 数据模型设计

#### 6.5.1 会话表 `ai_conversation`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| session_id | varchar(64) | 对外会话 ID，兼容当前前端 sessionId 概念 |
| user_id | bigint | 所属用户 |
| assistant_type | varchar(32) | 助手类型 |
| title | varchar(128) | 会话标题 |
| status | tinyint | 0-正常 1-归档 2-删除 |
| last_message_preview | varchar(500) | 最近消息摘要 |
| last_message_at | datetime | 最近消息时间 |
| message_count | int | 消息总数 |
| pinned | tinyint | 是否置顶 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

#### 6.5.2 消息表 `ai_message`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| session_id | varchar(64) | 会话 ID |
| user_id | bigint | 所属用户 |
| role | varchar(16) | `user` / `assistant` / `system` / `tool` |
| content_type | varchar(16) | `text` / `markdown` / `json` |
| content | longtext | 消息内容 |
| message_status | tinyint | 0-成功 1-失败 2-生成中 3-取消 |
| provider_code | varchar(32) | 供应商 |
| model_code | varchar(64) | 模型标识 |
| prompt_tokens | int | 输入 token |
| completion_tokens | int | 输出 token |
| total_tokens | int | 总 token |
| latency_ms | int | 模型耗时 |
| finish_reason | varchar(32) | 停止原因 |
| error_code | varchar(64) | 错误码 |
| error_message | varchar(512) | 错误信息 |
| reply_to_message_id | bigint | 回复的用户消息 ID |
| created_at | datetime | 创建时间 |

#### 6.5.3 供应商配置表 `ai_provider_config`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| provider_code | varchar(32) | 供应商编码 |
| provider_name | varchar(64) | 供应商名称 |
| base_url | varchar(255) | 接口地址 |
| api_key_cipher | varchar(512) | 加密后的密钥 |
| default_model_code | varchar(64) | 默认模型 |
| enabled | tinyint | 是否启用 |
| timeout_ms | int | 超时时间 |
| max_context_rounds | int | 最大上下文轮数 |
| temperature | decimal(4,2) | 温度 |
| top_p | decimal(4,2) | top_p |
| max_output_tokens | int | 最大输出 token |
| system_prompt_template | text | 默认系统提示词 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

#### 6.5.4 调用日志表 `ai_call_log`

用于审计、监控、运营统计，不建议直接依赖业务消息表统计。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| request_id | varchar(64) | 请求 ID |
| session_id | varchar(64) | 会话 ID |
| user_id | bigint | 用户 ID |
| provider_code | varchar(32) | 供应商 |
| model_code | varchar(64) | 模型 |
| success | tinyint | 是否成功 |
| latency_ms | int | 耗时 |
| total_tokens | int | token 消耗 |
| error_code | varchar(64) | 错误码 |
| created_at | datetime | 创建时间 |

#### 6.5.5 场景配置表 `ai_scene_config`

这个表是“保留全部接口、维持良好扩展性”的关键。后续新增能力域时，尽量只新增场景配置，而不是修改底层平台结构。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| capability_code | varchar(32) | 能力域编码 |
| scene_code | varchar(64) | 场景编码 |
| scene_name | varchar(128) | 场景名称 |
| provider_code | varchar(32) | 默认供应商 |
| model_code | varchar(64) | 默认模型 |
| enabled | tinyint | 是否启用 |
| system_prompt_template | text | 场景系统提示词 |
| input_schema_json | json | 输入结构定义 |
| output_schema_json | json | 输出结构定义 |
| safety_level | varchar(16) | 安全等级 |
| timeout_ms | int | 超时控制 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

#### 6.5.6 能力开关表 `ai_capability_config`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| capability_code | varchar(32) | 能力域 |
| capability_name | varchar(64) | 能力名称 |
| enabled | tinyint | 是否启用 |
| gray_enabled | tinyint | 是否灰度 |
| gray_rule_json | json | 灰度规则 |
| rate_limit_json | json | 限流配置 |
| quota_rule_json | json | 配额配置 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 6.6 接口设计

### 6.6.1 建议保留与废弃策略

- 保留当前 `/api/agent` 前缀。
- 将现有：
  - `POST /api/agent/chat`
  - `GET /api/agent/history`
  - `DELETE /api/agent/history`
  标记为兼容接口。
- 新前端改为调用资源化接口。
- 老接口保留一个迭代周期，内部转调新服务。

### 6.6.2 新接口清单

#### 用户端接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/agent/conversations` | 创建会话 |
| GET | `/api/agent/conversations` | 获取会话列表 |
| GET | `/api/agent/conversations/{sessionId}` | 获取会话详情 |
| PATCH | `/api/agent/conversations/{sessionId}` | 重命名会话 |
| DELETE | `/api/agent/conversations/{sessionId}` | 删除会话 |
| GET | `/api/agent/conversations/{sessionId}/messages` | 获取消息列表 |
| POST | `/api/agent/conversations/{sessionId}/messages` | 发送消息并获取回复 |
| POST | `/api/agent/conversations/{sessionId}/messages/stream` | 流式发送消息 |
| POST | `/api/agent/messages/{messageId}/retry` | 重试生成 |

#### 平台级预留接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/ai/generation/tasks` | 创建内容生成任务 |
| GET | `/api/ai/generation/tasks/{taskId}` | 查询生成结果 |
| POST | `/api/ai/generation/tasks/{taskId}/retry` | 重试生成任务 |
| POST | `/api/ai/recommendations/query` | 查询推荐结果 |
| POST | `/api/ai/recommendations/feedback` | 上报推荐反馈 |
| POST | `/api/ai/moderation/check` | 文本审核 |
| POST | `/api/ai/moderation/image-check` | 图片审核接口预留 |
| POST | `/api/ai/customer-service/sessions` | 创建客服会话 |
| POST | `/api/ai/customer-service/sessions/{sessionId}/messages` | 客服多轮问答 |
| POST | `/api/ai/customer-service/sessions/{sessionId}/handoff` | 人工转接 |
| POST | `/api/ai/customer-service/sessions/{sessionId}/rating` | 满意度评价 |
| POST | `/api/ai/analytics/query` | 发起 AI 分析任务 |
| GET | `/api/ai/analytics/query/{taskId}` | 查询分析结果 |

#### 管理端接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/ai/providers` | 获取供应商配置 |
| POST | `/api/admin/ai/providers` | 新增供应商 |
| PUT | `/api/admin/ai/providers/{id}` | 更新供应商 |
| POST | `/api/admin/ai/providers/{id}/enable` | 启用配置 |
| POST | `/api/admin/ai/providers/{id}/disable` | 停用配置 |
| POST | `/api/admin/ai/providers/{id}/test` | 联调测试 |
| GET | `/api/admin/ai/stats/overview` | 获取调用统计 |
| GET | `/api/admin/ai/logs` | 获取调用日志 |

### 6.6.3 核心响应结构建议

#### 发送消息响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "ses_01JXYZ",
    "conversationTitle": "商家认证怎么申请",
    "userMessage": {
      "id": 101,
      "role": "user",
      "content": "商家认证怎么申请？",
      "createdAt": "2026-04-20T14:00:00"
    },
    "assistantMessage": {
      "id": 102,
      "role": "assistant",
      "content": "你可以在“我的-认证中心”中发起商家认证申请……",
      "status": "SUCCESS",
      "providerCode": "openai_compatible",
      "modelCode": "deepseek-chat",
      "createdAt": "2026-04-20T14:00:02"
    },
    "usage": {
      "promptTokens": 520,
      "completionTokens": 182,
      "totalTokens": 702,
      "latencyMs": 2140
    }
  }
}
```

### 6.7 上下文与提示词策略

#### 6.7.1 系统提示词建议

系统提示词不直接写死在代码中，而是放到配置中心或数据库中，至少支持：

- 通用默认提示词
- 学生场景提示词
- 商家场景提示词
- 内容创作提示词

#### 6.7.2 上下文窗口控制

- 进入模型上下文的默认策略：
  - 最近 20 条消息
  - 或最近 8 轮对话
  - 超长时按 token 截断
- 若会话过长，可生成摘要消息作为长期记忆，占位替代旧消息。

#### 6.7.3 平台内信息增强策略

优先级建议：

1. 系统提示词
2. 用户最新问题
3. 业务工具返回结果
4. 平台搜索检索结果
5. 最近多轮上下文

### 6.8 工具调用与检索增强

#### 6.8.1 首期建议接入的工具

| 工具 | 来源 | 用途 |
| --- | --- | --- |
| `UserTool` | 已存在 | 读取当前用户信息、认证状态、统计信息 |
| `MerchantTool` | 已存在 | 查询商家信息、商家类型、商家状态 |
| `SearchTool` | 新增封装 | 复用 `PostSearchService`、`UserSearchService` 做检索增强 |

#### 6.8.2 工具调用策略

- 不建议首期放开模型自由 function calling。
- 建议先做“规则触发式工具调用”：
  - 问“商家认证”“店铺信息”“商家状态”时调用 `MerchantTool`
  - 问“我的资料”“是否认证”“我的统计”时调用 `UserTool`
  - 问“平台帖子”“用户信息”“搜索内容”时调用 `SearchTool`

优点：

- 行为可控
- 便于排错
- 降低模型误调用概率
- 适合当前项目阶段

### 6.9 安全与风控设计

#### 6.9.1 输入安全

- 输入长度限制
- 敏感词过滤，复用 `SensitiveFilterService`
- 频率限制：
  - 单用户每分钟请求数
  - 单用户每日请求总量

#### 6.9.2 输出安全

- 模型响应落库前做敏感词检查
- 命中高风险词时返回兜底文案
- 必要时记录安全审计日志

#### 6.9.3 密钥安全

- API Key 不明文写入代码或配置文件
- 后台录入后加密存储
- 返回前端时只展示掩码

### 6.10 缓存与限流设计

#### Redis 建议用途

- 用户请求频率计数
- 模型配置缓存
- 会话短期摘要缓存
- 幂等请求锁

#### 关键键设计示例

```text
ai:quota:user:{userId}:minute
ai:quota:user:{userId}:day
ai:provider:default
ai:session:{sessionId}:summary
ai:message:retry-lock:{messageId}
```

### 6.11 可观测性设计

建议至少埋点以下指标：

- 请求总量
- 成功率
- 平均耗时
- P95 耗时
- 每供应商调用量
- 每模型调用量
- token 消耗
- 每日活跃 AI 用户数
- 失败原因分布

日志字段建议统一包含：

- `requestId`
- `sessionId`
- `userId`
- `providerCode`
- `modelCode`
- `latencyMs`
- `success`
- `errorCode`

## 7. 前端改造方案

### 7.1 `src/api/agent.ts` 改造

当前问题：

- 类型定义与后端不一致。
- 接口能力不足。

目标改造：

- 新增会话列表、会话详情、消息列表、消息发送、重试、重命名接口。
- 统一类型：
  - `AgentConversation`
  - `AgentMessage`
  - `SendAgentMessageResponse`

### 7.2 `AgentListPage.vue` 改造

现状问题：

- 会话来自 `localStorage`
- 会话标题和最后消息不可信

目标：

- 改为调用 `/api/agent/conversations`
- 支持分页、删除、重命名
- 使用服务端返回的真实 `lastMessagePreview` 和 `lastMessageAt`

### 7.3 `AgentChatPage.vue` 改造

现状问题：

- 只展示 `response`
- 历史记录加载逻辑错误
- 会话与消息结构过于简化

目标：

- 首次进入根据 `sessionId` 拉取消息列表
- 发送消息后同时插入用户消息和助手消息
- 使用真实消息时间
- 失败消息支持重试
- 后续支持流式渲染

### 7.4 流式输出的前端实现建议

由于当前项目前端认证是基于 token header，原生 `EventSource` 不适合直接携带鉴权头，建议采用：

- `fetch + ReadableStream` 读取 `text/event-stream`
- 或者 `fetch` 读取分块 JSON 行

这样可以继续使用当前鉴权体系，无需额外改造登录态。

## 8. 后台管理设计

### 8.1 `AdminAI.vue` 从静态页面升级为配置页

页面建议分为四块：

1. 运行概览
   - 总调用量
   - 今日活跃用户
   - 成功率
   - 平均耗时
2. 供应商管理
   - 供应商名称
   - baseUrl
   - 默认模型
   - 状态
   - 测试连接
3. 场景配置
   - 默认系统提示词
   - 助手类型提示词
   - 上下文轮数
   - 输出长度
4. 调用日志
   - 时间
   - 用户
   - 模型
   - 耗时
   - token
   - 结果

### 8.2 后台权限建议

新增权限更合理：

- `admin:ai:manage`
- `admin:ai:view`

如果本期不想扩展权限体系，也可暂时复用 `ADMIN_ACCESS`，但长期不建议。

## 9. 分阶段实施计划

### 阶段 P0：接口修正与持久化基础

目标：把“演示功能”变成“可稳定使用的基础能力”。

交付内容：

- 新建 `ai_conversation`、`ai_message` 表
- 新增会话列表与消息列表接口
- 重构 `src/api/agent.ts`
- 修复前后端字段不一致
- 前端移除 `localStorage` 会话伪造逻辑
- 兼容旧 `/chat`、`/history` 接口

### 阶段 P1：真实模型接入

目标：完成单供应商可用版本。

交付内容：

- 新增 `LlmProvider` 抽象
- 接入一个 OpenAI 兼容供应商
- 增加系统提示词、超时、重试、限流
- 增加调用日志与 token 统计
- 后台新增基础配置页

### 阶段 P2：业务增强

目标：让聊天回答和平台业务真正相关，并把 AI 平台公共底座补齐。

交付内容：

- 接入 `UserTool`
- 接入 `MerchantTool`
- 新增 `SearchTool`
- 基于规则的工具调用
- 会话自动标题生成
- 内容安全输出过滤
- 新增 `ai_scene_config`
- 新增 `ai_capability_config`
- 预留 `/api/ai/generation/*`、`/api/ai/moderation/*`、`/api/ai/customer-service/*`、`/api/ai/analytics/*` 控制器壳层

### 阶段 P3：体验升级

目标：提升交互体验和运维能力。

交付内容：

- 流式输出
- 停止生成
- 重试生成
- 会话重命名
- 后台详细统计与失败分析
- 灰度发布开关

### 阶段 P4：AI 内容生成

目标：落地内容生产辅助能力。

交付内容：

- 帖子标题生成
- 帖子摘要提取
- 推荐文案生成
- 多语言改写
- 风格定制输出

### 阶段 P5：AI 内容审核与客服

目标：提高平台内容治理与服务效率。

交付内容：

- 文本审核
- 图片审核接口接入
- 自动标记违规内容
- FAQ 客服问答
- 人工转接与满意度评价

### 阶段 P6：推荐与分析

目标：把 AI 平台从“问答工具”升级为“运营与增长能力”。

交付内容：

- 推荐召回与排序接口
- 推荐反馈回流
- A/B 测试支持
- 智能报表生成
- 趋势预测、异常检测、画像分析

## 10. 验收标准

### 10.1 功能验收

1. 用户可创建、查看、删除会话。
2. 用户刷新页面后历史消息仍存在。
3. 前端发送消息后可收到真实模型回复。
4. 平台异常时，用户能收到明确错误提示，不会卡死。
5. 管理员可在后台查看和修改模型配置。

### 10.2 技术验收

1. 前后端类型字段完全一致。
2. 不再依赖内存保存历史会话。
3. 日志中可按 `requestId` 串联一次完整调用。
4. 敏感词输入和输出能被拦截。
5. 限流策略生效。

### 10.3 运营验收

1. 能统计每日 AI 调用次数。
2. 能统计各模型 token 消耗。
3. 能定位失败调用的主要原因。

## 11. 推荐实施顺序

### 后端优先级

1. 数据表与 Mapper
2. 新会话与消息接口
3. 前后端字段统一
4. 模型供应商接入
5. 配置后台
6. 工具增强

### 前端优先级

1. `src/api/agent.ts`
2. `AgentListPage.vue`
3. `AgentChatPage.vue`
4. `AdminAI.vue`
5. 流式输出体验

## 12. 结论

当前项目的 AI 模块已经具备入口、权限、页面和工具雏形，但本质上仍处于“原型演示”状态。要把它升级为真正可上线的大模型对话能力，关键不是简单替换回显逻辑，而是一次性补齐以下四个基础层：

1. 资源化接口与统一协议
2. 持久化会话与消息模型
3. 供应商抽象与调用编排
4. 后台配置、日志、风控与运营能力

从当前项目基础看，最佳落地路径不是另起炉灶，而是复用已有的消息模块范式、权限体系、敏感词服务、搜索能力和后台管理入口，逐步完成 AI 能力产品化。
