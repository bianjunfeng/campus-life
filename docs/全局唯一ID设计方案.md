# Campus-Life 全局唯一 ID 设计方案

状态：设计稿  
日期：2026-06-08  
适用范围：`campus-life-backend`、`campus-life-ai`、`campus-life-gateway`、前端 API 契约、部署配置

## 1. 背景

当前项目的实体主键主要依赖 MySQL `BIGINT AUTO_INCREMENT`，插入时通过 MyBatis `useGeneratedKeys="true" keyProperty="id"` 回填。订单号、支付单号、退款单号、会话 ID、事件 ID、请求 ID 等则由各业务模块分别使用 UUID、时间戳加随机数或本地规则生成。

这种方式在单库单体阶段可用，但在微服务拆分、数据合并、异步事件、跨库关联、离线分析和高并发写入场景下会暴露以下问题：

- 自增主键只保证单表或单库唯一，不能作为跨服务、跨库、跨表的统一标识。
- 应用层拿到主键依赖数据库插入完成，不利于先落事件、先写缓存、先组装聚合对象。
- 不同业务单号规则分散，唯一性依赖各自实现和数据库唯一索引兜底。
- 后续分库分表、读写分离、多实例部署时，自增主键需要额外协调。
- 64 位 ID 一旦对前端按 JSON number 传输，会超过 JavaScript 安全整数范围并产生精度丢失。

## 2. 目标

全局唯一 ID 体系需要满足：

- 全局唯一：在同一生产环境内跨服务、跨库、跨表唯一。
- 趋势递增：按时间大体有序，适合数据库主键聚簇索引和分页游标。
- 本地生成：正常请求路径不依赖数据库或 Redis，避免中心化瓶颈。
- 高吞吐：单实例每毫秒至少支持 4096 个 ID。
- 兼容现状：可落到 MySQL `BIGINT`，Java 内部使用 `Long`。
- 可解析：可从 ID 解析生成时间、worker 信息和序列号，便于排障。
- 可渐进迁移：允许历史自增 ID 与新 ID 共存。
- 前端安全：所有对外 64 位 ID 按字符串传输。

## 3. 非目标

- 不用全局 ID 解决权限、鉴权或防枚举问题。
- 不替代第三方支付平台生成的交易号。
- 不强制一次性重写全部历史数据。
- 不把所有外部公开标识都暴露为可解析的 Snowflake ID；高隐私场景仍可使用独立 `public_id`。

## 4. 总体方案

采用 Snowflake 风格的 64 位正整数 ID：

```
0 | 41-bit timestamp | 10-bit worker_id | 12-bit sequence
```

含义：

| 字段 | 位数 | 说明 |
| --- | ---: | --- |
| sign | 1 | 固定为 0，保证 Java `long` 为正数 |
| timestamp | 41 | 当前毫秒时间减去项目 epoch |
| worker_id | 10 | 环境、服务、实例组合编号 |
| sequence | 12 | 同一毫秒内的递增序号 |

核心参数：

| 参数 | 值 |
| --- | --- |
| epoch | `2026-01-01T00:00:00Z` |
| 可用年限 | 约 69 年 |
| worker 数量 | 1024 |
| 单 worker 峰值 | 4096 ID/ms，约 409 万 ID/s |
| ID 类型 | Java `long` / `Long`，MySQL `BIGINT` 或 `BIGINT UNSIGNED` |

ID 示例用途：

- 实体主键：`user.id`、`post.id`、`voucher_order.id`、`payment_order.id`
- 领域事件：`event_id`
- 消息幂等：`dedupe_id`
- 业务单号：使用前缀加全局 ID，例如 `VO123...`、`P123...`、`R123...`
- 请求追踪：需要跨系统日志串联时可使用 `request_id`

## 5. Worker ID 分配

`worker_id` 使用 10 位，拆为：

```
2-bit env_id | 4-bit service_id | 4-bit instance_id
```

计算方式：

```text
workerId = (envId << 8) | (serviceId << 4) | instanceId
```

### 5.1 环境编号

| env_id | 环境 |
| ---: | --- |
| 0 | local / dev |
| 1 | test |
| 2 | staging |
| 3 | prod |

说明：

- 生产环境必须使用 `env_id=3`。
- 如果测试、预发、生产数据会进入同一个数据仓库或日志平台，必须按环境分配不同 `env_id`。
- 本地开发默认 `env_id=0`，允许不同开发者生成相同 worker，但本地数据不得与生产数据合并。

### 5.2 服务编号

| service_id | 服务 |
| ---: | --- |
| 1 | `campus-life-backend` |
| 2 | `campus-life-ai` |
| 3 | `campus-life-gateway` |
| 4 | 后台批处理 / 数据修复任务 |
| 5 | 搜索重建 / 索引任务 |
| 6 | 压测或导入工具 |
| 7-15 | 预留 |

说明：

- 网关通常不生成数据库实体 ID，但可生成请求追踪 ID。
- 同一个服务在同一环境下最多支持 16 个实例。如果未来单服务实例数超过 16，需要切换为动态 worker 分配模式，或重新划分 worker 位段。

### 5.3 实例编号

实例编号范围为 `0-15`。

部署约定：

- Docker Compose：通过环境变量显式配置 `CAMPUS_ID_INSTANCE_ID`。
- systemd：每个服务实例的 unit 文件显式配置不同实例号。
- Kubernetes：优先使用 StatefulSet ordinal 作为 `instance_id`；Deployment 模式需要通过启动脚本或 Redis 租约分配。

生产环境禁止多个运行实例使用相同 `(env_id, service_id, instance_id)`。

## 6. Worker 分配模式

### 6.1 静态分配（默认推荐）

每个服务实例通过环境变量声明：

```yaml
campus:
  id:
    epoch: 2026-01-01T00:00:00Z
    env-id: ${CAMPUS_ID_ENV_ID:0}
    service-id: ${CAMPUS_ID_SERVICE_ID:1}
    instance-id: ${CAMPUS_ID_INSTANCE_ID:0}
```

优点：

- 运行期无中心依赖。
- 可审计、可预期。
- 适合当前 Docker Compose、systemd 和少量实例部署形态。

缺点：

- 扩容时需要运维正确分配实例号。
- 人为配置错误会导致 ID 冲突。

### 6.2 Redis 租约分配（可选增强）

如果未来进入弹性扩缩容环境，可以引入 Redis 租约：

```text
SET campus:id:worker:{env}:{service}:{instance} {nodeIdentity} NX PX 30000
```

规则：

- 服务启动时申请 worker。
- 租约每 10 秒续期。
- 续期失败或发现 owner 变化时，实例必须停止生成 ID 并触发自杀式退出。
- Redis 只参与 worker 领取，不参与每个 ID 的生成。

该模式适合 Kubernetes Deployment，但必须有完善的续租、退出和告警机制。

## 7. 生成算法

伪代码：

```java
public synchronized long nextId() {
    long now = clock.millis();

    if (now < lastTimestamp) {
        long offset = lastTimestamp - now;
        if (offset <= 5) {
            sleep(offset);
            now = clock.millis();
            if (now < lastTimestamp) {
                throw new ClockBackwardsException(offset);
            }
        } else {
            throw new ClockBackwardsException(offset);
        }
    }

    if (now == lastTimestamp) {
        sequence = (sequence + 1) & 0xFFF;
        if (sequence == 0) {
            now = waitNextMillis(lastTimestamp);
        }
    } else {
        sequence = 0;
    }

    lastTimestamp = now;

    return ((now - epochMillis) << 22)
            | (workerId << 12)
            | sequence;
}
```

关键策略：

- 同一 worker 内单调递增。
- 同一毫秒序列号耗尽时等待下一毫秒。
- 小于等于 5ms 的时钟回拨允许短暂等待。
- 大于 5ms 的时钟回拨直接失败，不冒险生成重复 ID。
- 生成失败必须记录错误日志和指标，由上层返回可重试错误。

## 8. Java 组件设计

建议新增独立轻量模块 `campus-life-id`，不依赖 Spring Boot，只依赖 Java 17。`campus-life-backend`、`campus-life-ai`、`campus-life-gateway` 均引用同一实现，避免复制后规则漂移。

包结构建议：

```text
com.campus.common.id
├── IdGenerator.java
├── SnowflakeIdGenerator.java
├── SnowflakeIdProperties.java
├── SnowflakeIdParser.java
├── WorkerId.java
├── WorkerIdProvider.java
├── StaticWorkerIdProvider.java
├── RedisLeaseWorkerIdProvider.java
├── BusinessNoGenerator.java
├── BusinessNoType.java
└── ClockBackwardsException.java
```

核心接口：

```java
public interface IdGenerator {
    long nextId();
}
```

业务单号接口：

```java
public interface BusinessNoGenerator {
    String nextNo(BusinessNoType type);
}
```

业务单号枚举：

```java
public enum BusinessNoType {
    VOUCHER_ORDER("VO"),
    PAYMENT("P"),
    REFUND("R"),
    EVENT("E"),
    REQUEST("REQ");

    private final String prefix;
}
```

解析结果：

```java
public record ParsedSnowflakeId(
        long id,
        Instant timestamp,
        int envId,
        int serviceId,
        int instanceId,
        int workerId,
        int sequence
) {
}
```

Spring Boot 自动配置建议放在各服务的 `common/config` 中，或后续封装为 `campus-life-id-spring-boot-starter`。

## 9. 数据库规范

### 9.1 新表规范

新表主键不再使用 `AUTO_INCREMENT`：

```sql
CREATE TABLE example_entity (
  id BIGINT UNSIGNED NOT NULL COMMENT '全局唯一ID',
  name VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

MyBatis 插入必须显式写入 `id`：

```xml
<insert id="insert" parameterType="ExampleEntity">
  INSERT INTO example_entity (id, name, created_at, updated_at)
  VALUES (#{id}, #{name}, NOW(), NOW())
</insert>
```

### 9.2 现有表迁移规范

现有 `AUTO_INCREMENT` 表允许分阶段迁移：

阶段内可以保留 `AUTO_INCREMENT`，应用开始显式传入新 ID。MySQL 接收显式 ID 后不会冲突，且新 Snowflake ID 远大于当前自增值。

稳定后再移除 `AUTO_INCREMENT`：

```sql
ALTER TABLE example_entity
  MODIFY id BIGINT UNSIGNED NOT NULL COMMENT '全局唯一ID';
```

迁移约束：

- 不重写历史主键。
- 不改变外键字段类型。
- 新旧 ID 共存时，业务逻辑不得依赖 ID 位数、连续性或最大值。
- 如果表中存在 Redis bitmap 按 ID 作为 offset 的用法，不能直接切换为 Snowflake ID，需要先改为稠密映射或其他统计结构。

### 9.3 索引规范

主键使用全局 ID 后仍保持趋势递增，适合 InnoDB 聚簇索引。

分页建议：

```sql
SELECT *
FROM post
WHERE id < #{cursorId}
ORDER BY id DESC
LIMIT #{pageSize};
```

需要严格按业务时间排序的场景仍保留 `created_at`，并使用 `(created_at, id)` 复合游标。

## 10. API 与前端契约

### 10.1 ID 对外必须是字符串

Snowflake ID 会超过 JavaScript `Number.MAX_SAFE_INTEGER`。因此：

- 后端内部实体、Mapper、领域服务继续使用 `Long`。
- REST API 响应中的 `id`、`userId`、`postId`、`voucherId`、`merchantId`、`orderId` 等 64 位 ID 必须序列化为字符串。
- REST API 请求路径和请求体中的 64 位 ID 可以接收字符串，服务端统一解析为 `Long`。
- 前端 TypeScript 统一定义：

```ts
export type ID = string
```

前端 API 类型示例：

```ts
export interface Post {
  id: ID
  authorId: ID
  title: string
}
```

### 10.2 序列化策略

推荐目标形态：

- 不直接向前端返回数据库实体。
- Controller 返回 DTO/VO。
- DTO/VO 中 ID 字段定义为 `String`，由 assembler 转换。

过渡方案：

- 可以临时在 Jackson 中将 `Long` 序列化为字符串。
- 使用全局 Long 转字符串前，需要检查统计接口中是否有 `Long` 类型计数值；如果有，应改为 `Integer`、`BigDecimal` 或显式 DTO 字段。

示例：

```java
@Bean
Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
    return builder -> {
        builder.serializerByType(Long.class, ToStringSerializer.instance);
        builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
    };
}
```

该过渡方案简单，但影响范围大，最终仍建议回到显式 DTO 契约。

## 11. 业务单号规范

业务单号使用前缀加全局 ID 的十进制字符串：

```text
{prefix}{snowflakeId}
```

| 类型 | 前缀 | 示例 | 当前替代对象 |
| --- | --- | --- | --- |
| 优惠券订单号 | `VO` | `VO51234567890123456` | `voucher_order.order_no` |
| 支付单号 | `P` | `P51234567890123456` | `payment_order.payment_no` |
| 退款单号 | `R` | `R51234567890123456` | `payment_refund_order.refund_no` |
| 事件 ID | `E` | `E51234567890123456` | `event_id` |
| 请求 ID | `REQ` | `REQ51234567890123456` | `requestId` |

规则：

- 业务单号只用于外部交互、幂等、查询和日志串联。
- 数据库实体主键仍使用纯数字全局 ID。
- 业务单号必须继续保留唯一索引。
- 业务代码不得从业务单号中推导业务状态，只允许解析 ID 生成时间和来源。
- 支付渠道若限制字符集，优先使用大写字母加数字，长度控制在 64 字符以内。

## 12. 项目落点

### 12.1 第一批接入对象

优先接入无需改动大量外键的编号字段：

- `voucher_order.order_no`
- `payment_order.payment_no`
- `payment_refund_order.refund_no`
- `event_outbox.event_id`
- `system_notification.event_id`
- AI `request_id`
- AI `session_id`

这些字段已有唯一索引或天然需要全局唯一，切换风险低。

### 12.2 第二批接入对象

新增数据的主键改为应用生成：

- `post.id`
- `comment.id`
- `message.id`
- `voucher.id`
- `voucher_order.id`
- `payment_order.id`
- `payment_refund_order.id`
- `ai_conversation.id`
- `ai_message.id`
- `ai_knowledge_base.id`
- `ai_knowledge_document.id`

迁移时保持历史 ID 不变。

### 12.3 第三批接入对象

用户、商家、钱包等核心身份类主键：

- `user.id`
- `merchant.id`
- `user_wallet.user_id`
- `student_profile.user_id`
- 各类认证申请表外键

该批次影响范围最大，需要先完成前端 ID 字符串化和权限/缓存/消息链路验证。

## 13. 迁移计划

### 阶段 0：设计与契约冻结

- 确认 bit 位布局、epoch 和 worker 分配表。
- 在部署文档中登记每个环境、服务、实例的 worker 参数。
- 明确前端 ID 类型改为 `string`。

产出：

- 本设计文档。
- Worker 分配清单。
- API ID 字符串化改造清单。

### 阶段 1：基础组件引入

- 新增 `campus-life-id` 模块。
- 后端、AI、网关引入同一 ID 组件。
- 增加 ID 解析工具和单元测试。
- 增加 Actuator 信息或诊断接口。

验收：

- 单实例连续生成 1000 万 ID 无重复。
- 多 worker 并发生成无重复。
- 时钟回拨测试符合预期。

### 阶段 2：业务单号统一

替换以下生成逻辑：

- 券订单号从 UUID 改为 `VO + snowflakeId`。
- 支付单号从时间戳加随机数改为 `P + snowflakeId`。
- 退款单号从时间戳加随机数改为 `R + snowflakeId`。
- 事件 ID 从 UUID 改为 `E + snowflakeId` 或纯 Snowflake 字符串。
- AI requestId/sessionId 改为统一生成规则。

保留数据库唯一索引兜底。

### 阶段 3：API ID 字符串化

- 前端新增统一 `ID = string` 类型。
- 修改 API 类型定义中的 `id: number`、`userId: number`、`postId: number` 等。
- 后端和 AI 的响应 DTO 确保 ID 字段为字符串。
- 路由参数、查询参数按字符串传递。

验收：

- 浏览器端不再对 64 位 ID 做数字运算。
- URL、缓存 key、集合 key 使用字符串 ID。
- 所有 ID 比较使用字符串相等，不使用大小比较；需要游标分页时由后端比较。

### 阶段 4：新增主键应用生成

选择低风险表先改：

- `message`
- `system_notification`
- `ai_call_log`
- `ai_message`

改造方式：

- insert 前设置 `entity.setId(idGenerator.nextId())`。
- MyBatis insert 增加 `id` 字段。
- 移除对应 mapper 的 `useGeneratedKeys`。
- 数据库暂时保留 `AUTO_INCREMENT` 作为回滚保护。

### 阶段 5：核心表主键切换

逐步切换：

- forum：`post`、`comment`、`report`
- voucher/order/payment：`voucher`、`voucher_order`、`payment_order`、`payment_refund_order`
- user/merchant：`user`、`merchant`、认证申请表
- AI：知识库、文档、分片、会话、消息

每批次上线后观察：

- 主键重复错误。
- 前端 ID 精度问题。
- SQL 慢查询和索引体积。
- 消息消费幂等。
- Redis key 命中情况。

### 阶段 6：清理历史自增依赖

稳定至少两个发布周期后：

- 移除表结构中的 `AUTO_INCREMENT`。
- 删除 `useGeneratedKeys`。
- 禁止新增 mapper 使用数据库回填主键。
- 更新开发规范和代码模板。

## 14. 运维配置

生产配置示例：

```env
CAMPUS_ID_ENV_ID=3
CAMPUS_ID_SERVICE_ID=1
CAMPUS_ID_INSTANCE_ID=0
```

AI 服务示例：

```env
CAMPUS_ID_ENV_ID=3
CAMPUS_ID_SERVICE_ID=2
CAMPUS_ID_INSTANCE_ID=0
```

网关示例：

```env
CAMPUS_ID_ENV_ID=3
CAMPUS_ID_SERVICE_ID=3
CAMPUS_ID_INSTANCE_ID=0
```

多实例示例：

| 服务 | 实例 | env_id | service_id | instance_id | worker_id |
| --- | --- | ---: | ---: | ---: | ---: |
| backend | backend-0 | 3 | 1 | 0 | 784 |
| backend | backend-1 | 3 | 1 | 1 | 785 |
| ai | ai-0 | 3 | 2 | 0 | 800 |
| gateway | gateway-0 | 3 | 3 | 0 | 816 |

## 15. 监控与告警

必须暴露以下指标：

| 指标 | 类型 | 说明 |
| --- | --- | --- |
| `campus_id_generated_total` | counter | 已生成 ID 数 |
| `campus_id_wait_next_millis_total` | counter | 序列号耗尽等待次数 |
| `campus_id_clock_backwards_total` | counter | 时钟回拨次数 |
| `campus_id_clock_backwards_millis` | gauge/histogram | 回拨毫秒数 |
| `campus_id_worker_id` | gauge | 当前 worker ID |
| `campus_id_last_timestamp` | gauge | 最近生成时间 |

告警规则：

- 任意生产实例出现 `clock_backwards_total > 0` 需要告警。
- 同一服务发现重复 worker 配置需要阻断启动。
- `wait_next_millis_total` 持续增长说明单实例 ID 生成接近瓶颈，需要扩容或分流。

诊断接口建议：

```http
GET /actuator/campus-id
```

响应：

```json
{
  "epoch": "2026-01-01T00:00:00Z",
  "envId": 3,
  "serviceId": 1,
  "instanceId": 0,
  "workerId": 784,
  "lastTimestamp": "2026-06-08T06:00:00Z"
}
```

## 16. 测试策略

单元测试：

- 同一 worker 单线程生成 ID 不重复。
- 同一 worker 多线程生成 ID 不重复。
- 不同 worker 同时生成 ID 不重复。
- 同一毫秒内 sequence 从 0 递增。
- sequence 溢出时等待下一毫秒。
- 时钟小回拨时等待。
- 时钟大回拨时抛异常。
- 解析器能还原 timestamp、env、service、instance、sequence。

集成测试：

- MyBatis 显式插入 ID 成功。
- 新旧 ID 共存查询正常。
- 业务单号唯一索引正常。
- JSON 响应 ID 为字符串。
- 前端路由和 API 使用字符串 ID 正常。

压测：

- 单实例 1000 万 ID 生成无重复。
- 多实例模拟 16 worker 并发生成无重复。
- 高并发下平均生成耗时低于 1 微秒级或满足业务 SLA。

## 17. 风险与约束

### 17.1 时钟回拨

Snowflake 依赖本机时间。生产机器必须使用稳定 NTP，并避免大幅度手动校时。

处理策略：

- 小回拨等待。
- 大回拨失败并告警。
- 不引入“借用未来时间”策略，避免 ID 时间长期偏移。

### 17.2 Worker 冲突

同一毫秒、同一 worker、同一 sequence 会产生重复 ID。生产环境必须通过配置审计或 Redis 租约保证 worker 唯一。

### 17.3 前端精度

这是上线前置条件。只要有 64 位 ID 以 JSON number 到达浏览器，就可能产生不可逆精度丢失。

### 17.4 ID 可推断

Snowflake ID 可推断生成时间和大致业务量。对不适合暴露的资源，新增 `public_id`：

```sql
public_id VARCHAR(32) NOT NULL UNIQUE COMMENT '对外不透明ID'
```

内部仍使用 Snowflake 主键。

### 17.5 Redis bitmap

不能将 Snowflake ID 直接作为 bitmap offset。若要统计用户签到、活跃等，需要使用稠密用户序号、Hash、Set、HyperLogLog 或分桶方案。

## 18. 开发规范

新增实体：

- `id` 类型使用 `Long`。
- 插入前必须由应用设置 `id`。
- Mapper 不使用 `useGeneratedKeys`。
- 新表不使用 `AUTO_INCREMENT`。

新增 API：

- DTO 中 ID 字段使用 `String`。
- 前端类型使用 `ID = string`。
- URL path 中的 ID 按字符串传递，后端解析校验。

新增业务单号：

- 使用 `BusinessNoGenerator`。
- 必须有唯一索引。
- 不允许手写时间戳加随机数。
- 不允许各模块自行调用 `UUID.randomUUID()` 生成业务唯一号，除非该值只用于安全随机 token。

允许继续使用 UUID 的场景：

- OAuth state。
- WebSocket 临时 ticket。
- 幂等锁 value。
- 文件名随机后缀。
- 安全 token 或 nonce。

## 19. 验收清单

- [ ] `campus-life-id` 模块存在并被 backend、AI、gateway 复用。
- [ ] 生产部署配置中每个实例 worker 唯一。
- [ ] 业务单号统一由 `BusinessNoGenerator` 生成。
- [ ] 新增表不再使用 `AUTO_INCREMENT`。
- [ ] 新增 mapper 不再使用 `useGeneratedKeys`。
- [ ] API 响应中的 64 位 ID 均为字符串。
- [ ] 前端 ID 类型从 `number` 迁移为 `string`。
- [ ] ID 生成、解析、并发、回拨测试覆盖。
- [ ] 监控指标和告警上线。
- [ ] 文档中维护 worker 分配表和服务编号表。

## 20. 推荐实施优先级

推荐按以下顺序实施：

1. 引入 ID 组件和测试。
2. 统一业务单号：`order_no`、`payment_no`、`refund_no`、`event_id`、`request_id`。
3. 完成后端/AI API ID 字符串化。
4. 完成前端 `ID = string` 类型迁移。
5. 新表和低风险日志/消息表主键改为应用生成。
6. 订单、支付、帖子、评论等核心表主键逐批迁移。
7. 用户、商家、钱包等身份类主键最后迁移。

这个顺序能先消除分散业务单号的问题，同时把 JavaScript 精度风险前置处理，避免核心主键切换后再发现前端不可兼容。
