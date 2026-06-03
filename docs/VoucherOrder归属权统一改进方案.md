# VoucherOrder 归属权统一改进方案

## 一、问题分析

### 1.1 当前问题

```
┌─────────────────────────────────────────────────────────────────┐
│                     当前 VoucherOrder 操作分布                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   voucher 模块 (归属)           payment 模块 (跨域操作)            │
│   ├── VoucherService           ├── PaymentCallbackService       │
│   │   ├── 创建订单              │   ├── markOrderPaid()          │
│   │   ├── 取消订单              │   ├── markOrderExpired()      │
│   │   └── 查询订单              │   └── markOrderRefunded()     │
│   ├── VoucherOrderMapper      ├── PaymentRefundDomainService   │
│   │   └── 所有 SQL             │   │   └── markRefundSuccess()  │
│   └── SeckillOrderProcess     ├── PaymentAutoRefundService     │
│       └── 创建秒杀订单          │   │   └── autoRefundExpired()  │
│                               └── PaymentRefundWorkflowService  │
│                                   └── 查询订单                   │
│                                                                  │
│   order 模块 (间接引用)                                           │
│   └── OrderFacadeService                                         │
│       └── 查询/绑定幂等键                                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 核心问题

| 问题 | 描述 | 影响 |
|------|------|------|
| **跨模块写操作** | payment 模块直接调用 `VoucherOrderMapper` 更新订单状态 | 违反单一职责，耦合严重 |
| **状态更新分散** | `markOrderPaid`、`markOrderRefunded` 等在 payment 模块 | 订单状态逻辑分散 |
| **事务边界模糊** | 支付回调时跨模块操作，无法保证原子性 | 数据一致性风险 |
| **归属权不清晰** | voucher 是实体归属，但 payment 有完整的状态更新能力 | 职责不清 |

---

## 二、改进方案设计

### 2.1 方案选型

| 方案 | 描述 | 优点 | 缺点 | 推荐度 |
|------|------|------|------|--------|
| **A. VoucherOrder 迁移到 order 模块** | 将实体和 Mapper 移到 order 模块 | 职责清晰，解耦彻底 | 改动量大 | ⭐⭐⭐⭐ |
| **B. payment 调用 voucher 服务** | payment 不直接操作 Mapper，通过 voucher 服务间接操作 | 改动量中等 | 仍是同步调用 | ⭐⭐⭐ |
| **C. 事件驱动解耦** | 支付成功发布事件，voucher 模块订阅后更新状态 | 最解耦 | 事务最终一致 | ⭐⭐⭐⭐⭐ |

**推荐方案 C + B 混合**：事件驱动解耦 + 服务调用

### 2.2 推荐方案架构

```
                    ┌─────────────────────────────────────────┐
                    │                 改进后架构               │
                    └─────────────────────────────────────────┘

  payment 模块                    order 模块                    voucher 模块
  ──────────                    ──────────                    ──────────
        │                           │                              │
        │  ① 发布支付成功事件         │                              │
        │──────────────────────────►│                              │
        │                           │  ② 同步更新本地缓存            │
        │                           │─────────────────────────────►│
        │                           │                              │
        │                           │  ③ (可选) 确认消息            │
        │                           │◄─────────────────────────────│
        │                           │                              │
        │  ④ 发布退款事件             │                              │
        │──────────────────────────►│                              │
        │                           │                              │
        │                           │  ⑤ 查询订单信息               │
        │                           │◄─────────────────────────────│
        │                           │                              │
        │                           │  ⑥ 调用 VoucherOrderService   │
        │                           │   更新订单状态                │
        │                           │──────────────────────────────►│
        │                           │                              │
```

---

## 三、涉及文件目录清单

### 3.1 第一阶段：VoucherOrder 实体与 Mapper 迁移

#### 3.1.1 需要移动的文件

```
源位置 (voucher 模块):
├── modules/voucher/entity/VoucherOrder.java
├── modules/voucher/mapper/VoucherOrderMapper.java
└── resources/mapper/voucher/VoucherOrderMapper.xml

目标位置 (order 模块):
├── modules/order/entity/VoucherOrder.java
├── modules/order/mapper/VoucherOrderMapper.java
└── resources/mapper/order/VoucherOrderMapper.xml
```

#### 3.1.2 需要修改 import 的文件 (18个 Java 文件)

| 文件 | 修改内容 |
|------|---------|
| `modules/voucher/service/VoucherService.java` | import 路径变更 |
| `modules/voucher/service/SeckillOrderProcessService.java` | import 路径变更 |
| `modules/voucher/service/VoucherSeckillService.java` | import 路径变更 |
| `modules/voucher/service/WelfareQueryService.java` | import 路径变更 |
| `modules/voucher/service/ShoppingCartService.java` | import 路径变更 |
| `modules/voucher/service/AdminTradeQueryService.java` | import 路径变更 |
| `modules/voucher/controller/VoucherController.java` | import 路径变更 |
| `modules/voucher/controller/VoucherServiceGatewayController.java` | import 路径变更 |
| `modules/payment/service/PaymentCallbackService.java` | import 路径变更 |
| `modules/payment/service/PaymentRefundDomainService.java` | import 路径变更 |
| `modules/payment/service/PaymentAutoRefundService.java` | import 路径变更 |
| `modules/payment/service/PaymentRefundWorkflowService.java` | import 路径变更 |
| `modules/payment/service/PaymentOrderDomainService.java` | import 路径变更 |
| `modules/payment/service/PaymentReconciliationService.java` | import 路径变更 |
| `modules/order/service/OrderFacadeService.java` | import 路径变更 |
| `modules/admin/service/AdminService.java` | import 路径变更 |
| `common/security/ownership/VoucherOrderOwnerResolver.java` | import 路径变更 |
| `modules/user/service/WalletService.java` | import 路径变更 |

#### 3.1.3 MyBatis 配置变更

```java
// MybatisConfig.java - 修改 @MapperScan
@MapperScan({
    // 删除 voucher.mapper.VoucherOrderMapper
    "com.campus.campus_life_backend.modules.admin.mapper",
    "com.campus.campus_life_backend.modules.forum.mapper",
    "com.campus.campus_life_backend.modules.merchant.mapper",
    "com.campus.campus_life_backend.modules.message.mapper",
    "com.campus.campus_life_backend.modules.payment.mapper",
    "com.campus.campus_life_backend.modules.user.mapper",
    "com.campus.campus_life_backend.modules.voucher.mapper",  // 删除 VoucherOrderMapper

    // 新增 order.mapper
    "com.campus.campus_life_backend.modules.order.mapper"
})
```

---

### 3.2 第二阶段：创建 VoucherOrderService (订单领域服务)

#### 3.2.1 新增文件

```
modules/order/service/VoucherOrderService.java    # 订单领域服务
modules/order/service/impl/VoucherOrderServiceImpl.java  # 实现
```

#### 3.2.2 VoucherOrderService 接口设计

```java
// VoucherOrderService.java
public interface VoucherOrderService {

    // === 订单创建 ===
    Long createOrder(Long userId, Long voucherId, BigDecimal payAmount,
                     String orderSource, LocalDateTime payDeadline);

    // === 订单查询 ===
    VoucherOrder findById(Long id);
    VoucherOrder findByOrderNo(String orderNo);
    List<VoucherOrder> findByUserId(Long userId);

    // === 状态变更 ===
    void markOrderPaid(Long orderId, String paymentMethod, LocalDateTime payTime);
    void markOrderPaymentFailed(Long orderId);
    void markOrderExpired(Long orderId);
    void markOrderRefunded(Long orderId, String reason);
    void cancelOrder(Long orderId, Long userId, String reason);

    // === 订单关闭 ===
    void closeExpiredPendingOrders(int limit);
    void closeExpiredPendingOrdersForUser(Long userId, int limit);
}
```

---

## 四、推进路线图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         VoucherOrder 归属权统一路线图                         │
└─────────────────────────────────────────────────────────────────────────────┘

阶段一：基础设施准备 (第 1-2 周)
├── 1.1 创建 order 模块目录结构
├── 1.2 复制 VoucherOrder 实体到 order/entity
├── 1.3 复制 VoucherOrderMapper 到 order/mapper
├── 1.4 复制 VoucherOrderMapper.xml 到 resources/mapper/order/
├── 1.5 修改 MyBatisConfig.java 添加 @MapperScan
└── 1.6 验证基础 CRUD 功能正常

阶段二：批量修改 Import (第 3 周)
├── 2.1 编写脚本批量替换 import 语句
│   └── voucher.entity.VoucherOrder → order.entity.VoucherOrder
│   └── voucher.mapper.VoucherOrderMapper → order.mapper.VoucherOrderMapper
├── 2.2 逐个文件验证编译通过
└── 2.3 手动检查遗漏的 import

阶段三：创建 VoucherOrderService (第 4-5 周)
├── 3.1 设计 VoucherOrderService 接口
├── 3.2 实现状态变更方法 (markPaid, markRefunded 等)
├── 3.3 将 voucher 模块中的订单操作逻辑迁移到 service
├── 3.4 实现订单创建方法
└── 3.5 验证订单状态机正确

阶段四：解耦 payment 模块 (第 6-7 周)
├── 4.1 payment 模块引入事件发布
│   ├── PaymentCallbackService 发布 OrderPaidEvent
│   └── PaymentRefundDomainService 发布 OrderRefundedEvent
├── 4.2 order 模块实现事件监听
│   └── VoucherOrderEventListener 处理事件
├── 4.3 配置 Kafka Topic
└── 4.4 编写事件消费测试

阶段五：清理与验证 (第 8 周)
├── 5.1 删除 payment 模块对 VoucherOrderMapper 的直接调用
├── 5.2 删除 voucher 模块中重复的状态变更逻辑
├── 5.3 全面回归测试
├── 5.4 性能测试
└── 5.5 文档更新
```

---

## 五、详细实施流程

### 5.1 阶段一：基础设施准备

#### Step 1.1: 创建目标目录

```bash
# 在 order 模块下创建 entity 和 mapper 目录
mkdir -p modules/order/entity
mkdir -p modules/order/mapper
mkdir -p modules/order/service/impl
mkdir -p modules/order/event
mkdir -p resources/mapper/order
```

#### Step 1.2: 复制实体文件

```bash
# 复制 VoucherOrder.java
cp modules/voucher/entity/VoucherOrder.java modules/order/entity/VoucherOrder.java

# 修改 package 声明
# from: package com.campus.campus_life_backend.modules.voucher.entity;
# to:   package com.campus.campus_life_backend.modules.order.entity;
```

#### Step 1.3: 复制 Mapper 接口

```bash
# 复制 VoucherOrderMapper.java
cp modules/voucher/mapper/VoucherOrderMapper.java modules/order/mapper/VoucherOrderMapper.java

# 修改 package 声明
# from: package com.campus.campus_life_backend.modules.voucher.mapper;
# to:   package com.campus.campus_life_backend.modules.order.mapper;
```

#### Step 1.4: 复制 XML 映射文件

```bash
# 复制 VoucherOrderMapper.xml
cp resources/mapper/voucher/VoucherOrderMapper.xml resources/mapper/order/VoucherOrderMapper.xml

# 修改 namespace
# from: com.campus.campus_life_backend.modules.voucher.mapper.VoucherOrderMapper
# to:   com.campus.campus_life_backend.modules.order.mapper.VoucherOrderMapper
```

#### Step 1.5: 更新 MyBatis 配置

```java
// MybatisConfig.java
@MapperScan({
    // ... 其他 mapper

    // 新增
    "com.campus.campus_life_backend.modules.order.mapper"
})
```

#### Step 1.6: 验证编译

```bash
mvn compile -pl campus-life-backend -am
```

---

### 5.2 阶段二：批量修改 Import

#### 5.2.1 编写替换脚本 (PowerShell)

```powershell
# replace_imports.ps1

$oldPackage = "com.campus.campus_life_backend.modules.voucher.entity.VoucherOrder"
$newPackage = "com.campus.campus_life_backend.modules.order.entity.VoucherOrder"

$oldMapper = "com.campus.campus_life_backend.modules.voucher.mapper.VoucherOrderMapper"
$newMapper = "com.campus.campus_life_backend.modules.order.mapper.VoucherOrderMapper"

$files = Get-ChildItem -Path "src" -Recurse -Include "*.java"

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw

    if ($content -match $oldPackage) {
        $content = $content -replace [regex]::Escape($oldPackage), $newPackage
        Set-Content -Path $file.FullName -Value $content
        Write-Host "Updated entity import in: $($file.FullName)"
    }

    if ($content -match $oldMapper) {
        $content = $content -replace [regex]::Escape($oldMapper), $newMapper
        Set-Content -Path $file.FullName -Value $content
        Write-Host "Updated mapper import in: $($file.FullName)"
    }
}
```

#### 5.2.2 执行替换

```bash
.\replace_imports.ps1
```

#### 5.2.3 验证所有 import 已更新

```bash
# 检查是否还有 voucher 模块对 VoucherOrder 的引用
grep -r "modules.voucher.entity.VoucherOrder" src/
grep -r "modules.voucher.mapper.VoucherOrderMapper" src/

# 应该返回空结果
```

---

### 5.3 阶段三：创建 VoucherOrderService

#### 5.3.1 定义服务接口

```java
// modules/order/service/VoucherOrderService.java
package com.campus.campus_life_backend.modules.order.service;

public interface VoucherOrderService {

    /**
     * 创建优惠券订单
     */
    Long createOrder(Long userId, Long voucherId, BigDecimal payAmount,
                     String orderSource, LocalDateTime payDeadline);

    /**
     * 查询订单
     */
    VoucherOrder findById(Long id);
    VoucherOrder findByOrderNo(String orderNo);
    List<VoucherOrder> findByUserId(Long userId);

    /**
     * 标记订单已支付
     */
    void markOrderPaid(Long orderId, String paymentMethod, LocalDateTime payTime);

    /**
     * 标记订单支付失败
     */
    void markOrderPaymentFailed(Long orderId);

    /**
     * 标记订单已过期
     */
    void markOrderExpired(Long orderId);

    /**
     * 标记订单已退款
     */
    void markOrderRefunded(Long orderId, String reason);

    /**
     * 取消订单
     */
    void cancelOrder(Long orderId, Long userId, String reason);

    /**
     * 关闭过期待支付订单
     */
    void closeExpiredPendingOrders(int limit);
    void closeExpiredPendingOrdersForUser(Long userId, int limit);
}
```

#### 5.3.2 实现服务类

```java
// modules/order/service/impl/VoucherOrderServiceImpl.java
@Service
public class VoucherOrderServiceImpl implements VoucherOrderService {

    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private VoucherMapper voucherMapper;  // 需要扣减库存

    @Autowired
    private DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public Long createOrder(Long userId, Long voucherId, BigDecimal payAmount,
                           String orderSource, LocalDateTime payDeadline) {
        // 实现创建逻辑
    }

    @Override
    @Transactional
    public void markOrderPaid(Long orderId, String paymentMethod, LocalDateTime payTime) {
        VoucherOrder order = voucherOrderMapper.findById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        voucherOrderMapper.markOrderPaidWithVersion(
            orderId, paymentMethod, payTime, order.getUpdateTime()
        );

        // 发布领域事件
        eventPublisher.publishEvent(new OrderPaidEvent(...));
    }

    // ... 其他方法实现
}
```

---

### 5.4 阶段四：事件驱动解耦

#### 5.4.1 定义事件类

```java
// modules/order/event/OrderPaidEvent.java
public class OrderPaidEvent extends ApplicationEvent {
    private final Long orderId;
    private final String orderNo;
    private final Long userId;
    private final BigDecimal payAmount;
    private final String paymentMethod;
    private final LocalDateTime payTime;

    // constructors, getters
}
```

#### 5.4.2 Payment 模块发布事件

```java
// PaymentCallbackService.java - 修改前
voucherOrderMapper.markOrderPaidWithVersion(orderId, paymentMethod, payTime, version);

// PaymentCallbackService.java - 修改后
eventPublisher.publishEvent(new OrderPaidEvent(
    orderId, orderNo, userId, payAmount, paymentMethod, payTime
));
```

#### 5.4.3 Order 模块订阅事件

```java
// VoucherOrderEventListener.java
@Component
public class VoucherOrderEventListener {

    @Autowired
    private VoucherOrderService voucherOrderService;

    @EventListener
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        voucherOrderService.markOrderPaid(
            event.getOrderId(),
            event.getPaymentMethod(),
            event.getPayTime()
        );
    }
}
```

---

### 5.5 阶段五：清理与验证

#### 5.5.1 删除旧代码

```bash
# 删除 voucher 模块中的 VoucherOrder 相关文件
rm modules/voucher/entity/VoucherOrder.java
rm modules/voucher/mapper/VoucherOrderMapper.java
rm resources/mapper/voucher/VoucherOrderMapper.xml
```

#### 5.5.2 验证测试

```bash
# 运行单元测试
mvn test -pl campus-life-backend

# 运行集成测试
mvn verify -pl campus-life-backend
```

---

## 六、文件变更汇总表

### 6.1 新增文件 (5个)

| 文件 | 说明 |
|------|------|
| `modules/order/entity/VoucherOrder.java` | 迁移自 voucher |
| `modules/order/mapper/VoucherOrderMapper.java` | 迁移自 voucher |
| `modules/order/service/VoucherOrderService.java` | 新增接口 |
| `modules/order/service/impl/VoucherOrderServiceImpl.java` | 新增实现 |
| `modules/order/event/*.java` | 新增事件类 |

### 6.2 删除文件 (3个)

| 文件 | 说明 |
|------|------|
| `modules/voucher/entity/VoucherOrder.java` | 已迁移 |
| `modules/voucher/mapper/VoucherOrderMapper.java` | 已迁移 |
| `resources/mapper/voucher/VoucherOrderMapper.xml` | 已迁移 |

### 6.3 修改文件 (18 + 8 个)

#### 后端 Java 文件 (18个)

| 序号 | 文件路径 | 修改内容 |
|-----|---------|---------|
| 1 | `modules/voucher/service/VoucherService.java` | import 路径 |
| 2 | `modules/voucher/service/SeckillOrderProcessService.java` | import 路径 |
| 3 | `modules/voucher/service/VoucherSeckillService.java` | import 路径 |
| 4 | `modules/voucher/service/WelfareQueryService.java` | import 路径 |
| 5 | `modules/voucher/service/ShoppingCartService.java` | import 路径 |
| 6 | `modules/voucher/service/AdminTradeQueryService.java` | import 路径 |
| 7 | `modules/voucher/controller/VoucherController.java` | import 路径 |
| 8 | `modules/voucher/controller/VoucherServiceGatewayController.java` | import 路径 |
| 9 | `modules/payment/service/PaymentCallbackService.java` | import 路径 + 事件发布 |
| 10 | `modules/payment/service/PaymentRefundDomainService.java` | import 路径 + 事件发布 |
| 11 | `modules/payment/service/PaymentAutoRefundService.java` | import 路径 + 事件发布 |
| 12 | `modules/payment/service/PaymentRefundWorkflowService.java` | import 路径 |
| 13 | `modules/payment/service/PaymentOrderDomainService.java` | import 路径 |
| 14 | `modules/payment/service/PaymentReconciliationService.java` | import 路径 |
| 15 | `modules/order/service/OrderFacadeService.java` | import 路径 |
| 16 | `modules/admin/service/AdminService.java` | import 路径 |
| 17 | `common/security/ownership/VoucherOrderOwnerResolver.java` | import 路径 |
| 18 | `modules/user/service/WalletService.java` | import 路径 |

#### 配置文件 (8个)

| 序号 | 文件路径 | 修改内容 |
|-----|---------|---------|
| 1 | `common/config/MybatisConfig.java` | @MapperScan 配置 |
| 2 | `modules/order/config/OrderDomainConfig.java` | 新增配置类 |
| 3 | `modules/order/event/OrderPaidEvent.java` | 新增事件 |
| 4 | `modules/order/event/OrderRefundedEvent.java` | 新增事件 |
| 5 | `modules/order/event/OrderCancelledEvent.java` | 新增事件 |
| 6 | `modules/order/listener/VoucherOrderEventListener.java` | 新增监听器 |
| 7 | `modules/voucher/task/VoucherOrderAutoCloseTask.java` | 调用变更 |
| 8 | `modules/voucher/task/VoucherOrderAutoRefundTask.java` | 调用变更 |

---

## 七、风险与回滚方案

### 7.1 主要风险

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| import 替换遗漏 | 中 | 分阶段验证编译 |
| 事务边界变化 | 高 | 充分测试事务场景 |
| 事件消费顺序 | 中 | 添加消息去重 |
| 性能下降 | 低 | Kafka 异步处理 |

### 7.2 回滚方案

```
┌─────────────────────────────────────────────────────────────┐
│                        回滚流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. 保留 Git 分支                                            │
│     git checkout -b voucher-order-refactor                   │
│                                                              │
│  2. 切换回主分支                                             │
│     git checkout main                                        │
│                                                              │
│  3. 恢复被删除的文件                                         │
│     git checkout voucher-order-refactor -- \                 │
│       modules/voucher/entity/VoucherOrder.java \             │
│       modules/voucher/mapper/VoucherOrderMapper.java         │
│       resources/mapper/voucher/VoucherOrderMapper.xml        │
│                                                              │
│  4. 还原 import 变更                                         │
│     git checkout voucher-order-refactor -- src/              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 八、验收标准

| 验收项 | 标准 |
|-------|------|
| **编译通过** | `mvn compile` 无错误 |
| **单元测试** | 所有现有测试通过 |
| **API 兼容性** | 原有 API 接口行为不变 |
| **订单状态机** | 0→1→2/3/4/5 状态流转正确 |
| **支付回调** | 支付成功能正确更新订单状态 |
| **事件消费** | 事件能正确被消费和处理 |
| **无跨模块 Mapper 调用** | payment 模块不直接调用 VoucherOrderMapper |

---

## 九、后续优化建议

完成 VoucherOrder 归属权统一后，可进一步：

1. **聚合根完善**：为 VoucherOrder 添加领域方法
2. **值对象引入**：OrderNo、Money 等值对象
3. **仓储模式**：引入 Repository 接口
4. **事件溯源**：考虑对核心订单采用 Event Sourcing
