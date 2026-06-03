# Admin 模块瘦身实施方案

## 一、背景与目标

### 1.1 现状问题

| 问题 | 描述 | 影响 |
|------|------|------|
| AdminService 过于庞大 | 410行代码，40+ 方法 | 难以维护和扩展 |
| Controller 直接依赖业务服务 | 违反分层架构原则 | 职责不清 |
| Dashboard 性能问题 | `findAll*ForAdmin()` 全量加载 | OOM 风险 |
| 分页逻辑错位 | 分页在 Service 而非 Mapper | 职责混淆 |
| 部分 Controller 未统一编排 | 架构不一致 | 维护困难 |

### 1.2 瘦身后目标

| 指标 | 瘦身前 | 瘦身后 |
|------|--------|--------|
| AdminService 行数 | 410 行 | 拆分后各 ~80-150 行 |
| AdminController 行数 | 500+ 行 | 各 Controller ~100 行 |
| Controller 数量 | 1 个 | 10 个 |
| Service 数量 | 4 个 | 11 个 |
| 单个 Service 依赖数 | 14+ 个 | 3-5 个 |
| Dashboard 内存占用 | O(n) 全量加载 | O(1) COUNT 聚合 |

---

## 二、目标架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    瘦身后 Admin 模块架构                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  controller/                                                     │
│  ├── AdminPostController.java           ← 帖子管理              │
│  ├── AdminMerchantController.java        ← 商家管理              │
│  ├── AdminVoucherController.java        ← 优惠券管理             │
│  ├── AdminUserController.java          ← 用户管理               │
│  ├── AdminCommentController.java        ← 评论管理               │
│  ├── AdminReportController.java         ← 举报管理               │
│  ├── AdminRefundController.java         ← 退款审核 (保留)        │
│  ├── AdminReconciliationController.java ← 对账 (保留)           │
│  ├── AdminSensitiveWordController.java  ← 敏感词 (保留)         │
│  └── AdminDashboardController.java      ← 仪表盘               │
│                                                                  │
│  service/                                                        │
│  ├── PostAdminOrchestrationService.java     ← 帖子编排           │
│  ├── MerchantAdminOrchestrationService.java ← 商家编排           │
│  ├── VoucherAdminOrchestrationService.java ← 券编排             │
│  ├── UserAdminOrchestrationService.java    ← 用户编排            │
│  ├── CommentAdminOrchestrationService.java ← 评论编排            │
│  ├── ReportAdminOrchestrationService.java  ← 举报编排           │
│  ├── AdminOperationLogQueryService.java   ← 日志查询            │
│  ├── AdminDashboardQueryService.java      ← 仪表盘 (优化)        │
│  └── AdminApiCatalogService.java         ← API目录 (保留)       │
│                                                                  │
│  mapper/                                                         │
│  └── AdminOperationLogMapper.java                                 │
│                                                                  │
│  entity/                                                         │
│  └── AdminOperationLog.java                                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 三、文件变更清单

### 3.1 新增文件 (19个)

| 序号 | 文件路径 | 说明 |
|-----|---------|------|
| 1 | `modules/admin/controller/AdminPostController.java` | 帖子管理控制器 |
| 2 | `modules/admin/controller/AdminMerchantController.java` | 商家管理控制器 |
| 3 | `modules/admin/controller/AdminVoucherController.java` | 优惠券管理控制器 |
| 4 | `modules/admin/controller/AdminUserController.java` | 用户管理控制器 |
| 5 | `modules/admin/controller/AdminCommentController.java` | 评论管理控制器 |
| 6 | `modules/admin/controller/AdminReportController.java` | 举报管理控制器 |
| 7 | `modules/admin/controller/AdminDashboardController.java` | 仪表盘控制器 |
| 8 | `modules/admin/service/PostAdminOrchestrationService.java` | 帖子编排服务 |
| 9 | `modules/admin/service/MerchantAdminOrchestrationService.java` | 商家编排服务 |
| 10 | `modules/admin/service/VoucherAdminOrchestrationService.java` | 券编排服务 |
| 11 | `modules/admin/service/UserAdminOrchestrationService.java` | 用户编排服务 |
| 12 | `modules/admin/service/CommentAdminOrchestrationService.java` | 评论编排服务 |
| 13 | `modules/admin/service/ReportAdminOrchestrationService.java` | 举报编排服务 |
| 14 | `modules/admin/service/AdminOperationLogQueryService.java` | 日志查询服务 |
| 15 | `modules/admin/vo/DashboardStatsVO.java` | 仪表盘总览 VO |
| 16 | `modules/admin/vo/TodayStatsVO.java` | 今日统计 VO |
| 17 | `modules/user/vo/UserTrendVO.java` | 用户趋势 VO |
| 18 | `modules/forum/vo/PostTrendVO.java` | 帖子趋势 VO |
| 19 | `modules/voucher/vo/OrderTrendVO.java` | 订单趋势 VO |

### 3.2 删除文件 (2个)

| 序号 | 文件路径 | 说明 |
|-----|---------|------|
| 1 | `modules/admin/controller/AdminController.java` | 已拆分 |
| 2 | `modules/admin/service/AdminService.java` | 已拆分 |

### 3.3 修改文件 (10个)

| 序号 | 文件路径 | 修改内容 |
|-----|---------|---------|
| 1 | `modules/admin/service/AdminDashboardQueryService.java` | 优化 Dashboard 查询 |
| 2 | `modules/admin/service/AdminApiCatalogService.java` | 权限注解更新 |
| 3 | `modules/admin/controller/AdminRefundController.java` | 权限注解更新 |
| 4 | `modules/admin/controller/AdminReconciliationController.java` | 权限注解更新 |
| 5 | `modules/admin/controller/AdminSensitiveWordController.java` | 权限注解更新 |
| 6 | `modules/user/service/AdminUserQueryService.java` | 新增 COUNT 聚合方法 |
| 7 | `modules/forum/service/AdminCommunityQueryService.java` | 新增 COUNT 聚合方法 |
| 8 | `modules/merchant/service/MerchantQueryService.java` | 新增 COUNT 聚合方法 |
| 9 | `modules/voucher/service/AdminTradeQueryService.java` | 新增 COUNT 聚合方法 |
| 10 | `resources/mapper/admin/AdminOperationLogMapper.xml` | 新增分页查询 |

---

## 四、实施计划

### 4.1 总体时间表

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Admin 模块瘦身体实施工周期                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  第1周          第2周          第3周          第4周          第5周           │
│  ──────        ──────        ──────        ──────        ──────           │
│                                                                            │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐      │
│  │阶段一   │   │阶段二   │   │阶段三   │   │阶段四   │   │阶段五   │      │
│  │Controller│   │Orchestr-│   │Dashboard│   │拆分     │   │Mapper   │      │
│  │拆分     │   │ation    │   │优化     │   │Controller│   │分页     │      │
│  │        │   │Services │   │        │   │        │   │        │      │
│  │        │   │创建     │   │        │   │        │   │        │      │
│  └─────────┘   └─────────┘   └─────────┘   └─────────┘   └─────────┘      │
│                                                                            │
│  ┌─────────────────────────────────────────────────────────────────┐      │
│  │                        集成测试与回归测试                          │      │
│  │                              第6周                                │      │
│  └─────────────────────────────────────────────────────────────────┘      │
│                                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 五、详细实施流程

### 5.1 阶段一：Controller 拆分 (第1周)

#### Step 1.1: 创建 AdminPostController

**目标文件**: `modules/admin/controller/AdminPostController.java`

**职责**: 帖子管理相关 API

**API 端点**:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/posts` | 查询帖子列表 |
| POST | `/api/admin/posts/{postId}/delete` | 删除帖子 |
| POST | `/api/admin/posts/{postId}/ban` | 禁言帖子 |
| POST | `/api/admin/posts/{postId}/unban` | 解除禁言 |
| POST | `/api/admin/posts/{postId}/pin` | 置顶帖子 |
| POST | `/api/admin/posts/{postId}/unpin` | 取消置顶 |
| POST | `/api/admin/posts/{postId}/set-hot` | 设置热门 |
| POST | `/api/admin/posts/{postId}/remove-hot` | 取消热门 |

#### Step 1.2: 创建 AdminMerchantController

**目标文件**: `modules/admin/controller/AdminMerchantController.java`

**职责**: 商家管理相关 API

**API 端点**:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/merchants` | 查询商家列表 |
| POST | `/api/admin/merchants` | 创建商家 |
| PUT | `/api/admin/merchants/{merchantId}` | 更新商家 |
| POST | `/api/admin/merchants/{merchantId}/approve` | 审核通过 |
| POST | `/api/admin/merchants/{merchantId}/reject` | 审核拒绝 |
| POST | `/api/admin/merchants/{merchantId}/freeze` | 冻结商家 |
| POST | `/api/admin/merchants/{merchantId}/unfreeze` | 解冻商家 |

#### Step 1.3: 创建 AdminVoucherController

**目标文件**: `modules/admin/controller/AdminVoucherController.java`

**职责**: 优惠券管理相关 API

**API 端点**:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/vouchers` | 查询优惠券列表 |
| POST | `/api/admin/vouchers` | 创建优惠券 |
| PUT | `/api/admin/vouchers/{voucherId}` | 更新优惠券 |
| POST | `/api/admin/vouchers/{voucherId}/online` | 上架 |
| POST | `/api/admin/vouchers/{voucherId}/offline` | 下架 |
| POST | `/api/admin/vouchers/{voucherId}/violation-offline` | 违规下架 |
| POST | `/api/admin/vouchers/{voucherId}/preheat` | 预热秒杀 |
| GET | `/api/admin/vouchers/{voucherId}/monitor` | 秒杀监控 |

#### Step 1.4: 创建 AdminUserController

**目标文件**: `modules/admin/controller/AdminUserController.java`

**职责**: 用户管理相关 API

**API 端点**:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users` | 查询用户列表 |
| GET | `/api/admin/members` | 会员分页列表 |
| POST | `/api/admin/users/{userId}/status` | 更新用户状态 |
| DELETE | `/api/admin/system/users/{userId}` | 删除用户 |
| GET | `/api/admin/student-auth/requests` | 学生认证列表 |
| POST | `/api/admin/student-auth/{requestId}/approve` | 审核通过 |
| POST | `/api/admin/student-auth/{requestId}/reject` | 审核拒绝 |

#### Step 1.5: 创建 AdminCommentController

**目标文件**: `modules/admin/controller/AdminCommentController.java`

**职责**: 评论管理相关 API

**API 端点**:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/comments` | 查询评论列表 |
| POST | `/api/admin/comments/{commentId}/delete` | 删除评论 |
| POST | `/api/admin/comments/{commentId}/ban` | 禁言评论 |
| POST | `/api/admin/comments/{commentId}/unban` | 解除禁言 |

#### Step 1.6: 创建 AdminReportController

**目标文件**: `modules/admin/controller/AdminReportController.java`

**职责**: 举报管理相关 API

**API 端点**:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/reports` | 查询举报列表 |
| POST | `/api/admin/reports/{reportId}/process` | 处理举报 |

#### Step 1.7: 创建 AdminDashboardController

**目标文件**: `modules/admin/controller/AdminDashboardController.java`

**职责**: 仪表盘相关 API

**API 端点**:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/stats` | 统计数据 |
| GET | `/api/admin/dashboard/stats` | 仪表盘数据 |
| GET | `/api/admin/dashboard/today` | 今日数据 |
| GET | `/api/admin/operation-logs` | 操作日志 |

---

### 5.2 阶段二：创建 Orchestration Services (第2周)

#### Step 2.1: 创建 PostAdminOrchestrationService

**目标文件**: `modules/admin/service/PostAdminOrchestrationService.java`

**职责**: 帖子管理编排

**依赖注入**:
```java
@Autowired
private PostService postService;

@Autowired
private AdminOperationLogMapper operationLogMapper;
```

**核心方法**:
| 方法 | 说明 |
|------|------|
| `getPostList()` | 查询帖子列表 |
| `deletePost()` | 删除帖子 + 记录日志 |
| `banPost()` | 禁言帖子 + 记录日志 |
| `unbanPost()` | 解除禁言 + 记录日志 |
| `pinPost()` | 置顶帖子 + 记录日志 |
| `unpinPost()` | 取消置顶 + 记录日志 |
| `setHotPost()` | 设置热门 + 记录日志 |
| `removeHotPost()` | 取消热门 + 记录日志 |

#### Step 2.2: 创建 MerchantAdminOrchestrationService

**目标文件**: `modules/admin/service/MerchantAdminOrchestrationService.java`

**职责**: 商家管理编排

**依赖注入**:
```java
@Autowired
private MerchantManagementService merchantManagementService;

@Autowired
private MerchantAuthService merchantAuthService;

@Autowired
private MerchantQueryService merchantQueryService;

@Autowired
private AdminOperationLogMapper operationLogMapper;
```

**核心方法**:
| 方法 | 说明 |
|------|------|
| `getMerchantList()` | 查询商家列表 |
| `createMerchant()` | 创建商家 + 记录日志 |
| `updateMerchant()` | 更新商家 + 记录日志 |
| `approveMerchant()` | 审核通过 + 记录日志 |
| `rejectMerchant()` | 审核拒绝 + 记录日志 |
| `freezeMerchant()` | 冻结商家 + 记录日志 |
| `unfreezeMerchant()` | 解冻商家 + 记录日志 |

#### Step 2.3: 创建 VoucherAdminOrchestrationService

**目标文件**: `modules/admin/service/VoucherAdminOrchestrationService.java`

**职责**: 优惠券管理编排

**依赖注入**:
```java
@Autowired
private VoucherService voucherService;

@Autowired
private VoucherSeckillService seckillService;

@Autowired
private AdminTradeQueryService tradeQueryService;

@Autowired
private AdminOperationLogMapper operationLogMapper;
```

**核心方法**:
| 方法 | 说明 |
|------|------|
| `getVoucherList()` | 查询优惠券列表 |
| `createVoucher()` | 创建优惠券 + 记录日志 |
| `updateVoucher()` | 更新优惠券 + 记录日志 |
| `onlineVoucher()` | 上架 + 记录日志 |
| `offlineVoucher()` | 下架 + 记录日志 |
| `violationOfflineVoucher()` | 违规下架 + 记录日志 |
| `preheatSeckillVoucher()` | 预热秒杀 |
| `getSeckillMonitor()` | 获取秒杀监控 |

#### Step 2.4: 创建 UserAdminOrchestrationService

**目标文件**: `modules/admin/service/UserAdminOrchestrationService.java`

**职责**: 用户管理编排

**依赖注入**:
```java
@Autowired
private UserService userService;

@Autowired
private AdminUserQueryService userQueryService;

@Autowired
private AuthRequestService authRequestService;

@Autowired
private AdminOperationLogMapper operationLogMapper;
```

**核心方法**:
| 方法 | 说明 |
|------|------|
| `getUserList()` | 查询用户列表 |
| `getMemberListPage()` | 会员分页列表 |
| `updateUserStatus()` | 更新用户状态 + 记录日志 |
| `deleteUser()` | 删除用户 + 记录日志 |
| `getStudentAuthList()` | 学生认证列表 |
| `approveStudentAuth()` | 审核通过 + 记录日志 |
| `rejectStudentAuth()` | 审核拒绝 + 记录日志 |

#### Step 2.5: 创建 AdminOperationLogQueryService

**目标文件**: `modules/admin/service/AdminOperationLogQueryService.java`

**职责**: 操作日志查询（分页下沉）

**核心方法**:
| 方法 | 说明 |
|------|------|
| `getOperationLogList()` | 分页查询日志 |

---

### 5.3 阶段三：Dashboard 优化 (第3周)

#### Step 3.1: 优化 AdminDashboardQueryService

**目标文件**: `modules/admin/service/AdminDashboardQueryService.java`

**优化内容**:

| 优化项 | 瘦身前 | 瘦身后 |
|--------|--------|--------|
| 用户统计 | `findAllUsersForAdmin()` | `countUsers()` |
| 帖子统计 | `findAllPostsForAdmin()` | `countPosts()` |
| 商家统计 | `findAllMerchantsForAdmin()` | `countMerchants()` |
| 优惠券统计 | `findAllVouchersForAdmin()` | `countVouchers()` |

**新增 VO**:
```java
public class DashboardStatsVO {
    private long totalUsers;
    private long totalPosts;
    private long totalMerchants;
    private long totalVouchers;
    private long pendingStudentAuth;
    private long pendingMerchantAuth;
    private long pendingReports;
    private long pendingRefunds;
}

public class TodayStatsVO {
    private long newUsersToday;
    private long newPostsToday;
    private long newOrdersToday;
    private long newMerchantsToday;
}
```

#### Step 3.2: 新增 QueryService 聚合方法

**AdminUserQueryService.java 新增**:
```java
long countUsers();
long countPendingStudentAuth();
List<UserTrendVO> countUsersTrend(int days);
long countUsersSince(LocalDateTime since);
```

**MerchantQueryService.java 新增**:
```java
long countMerchants();
long countPendingMerchantAuth();
long countMerchantsSince(LocalDateTime since);
```

**AdminCommunityQueryService.java 新增**:
```java
long countPosts();
long countPendingReports();
List<PostTrendVO> countPostsTrend(int days);
long countPostsSince(LocalDateTime since);
```

**AdminTradeQueryService.java 新增**:
```java
long countVouchers();
List<OrderTrendVO> countOrdersTrend(int days);
long countOrdersSince(LocalDateTime since);
```

**PaymentRefundWorkflowService.java 新增**:
```java
long countPendingAdminRefunds();
```

---

### 5.4 阶段四：删除旧文件 (第4周)

#### Step 4.1: 删除旧 AdminController

```bash
# 删除文件
rm modules/admin/controller/AdminController.java
```

#### Step 4.2: 删除旧 AdminService

```bash
# 删除文件
rm modules/admin/service/AdminService.java
```

#### Step 4.3: 验证编译

```bash
mvn compile -pl campus-life-backend -am
```

---

### 5.5 阶段五：Mapper 分页优化 (第5周)

#### Step 5.1: 更新 AdminOperationLogMapper.xml

**目标文件**: `resources/mapper/admin/AdminOperationLogMapper.xml`

**新增 SQL**:
```xml
<select id="selectByConditions" resultType="AdminOperationLog">
    SELECT id, target_type, target_id, action, detail,
           admin_id, create_time, update_time
    FROM admin_operation_log
    WHERE 1=1
    <if test="adminId != null">
        AND admin_id = #{adminId}
    </if>
    <if test="targetType != null">
        AND target_type = #{targetType}
    </if>
    <if test="action != null">
        AND action = #{action}
    </if>
    <if test="beginTime != null">
        AND create_time >= #{beginTime}
    </if>
    <if test="endTime != null">
        AND create_time <= #{endTime}
    </if>
    ORDER BY create_time DESC
    LIMIT #{offset}, #{limit}
</select>

<select id="countByConditions" resultType="long">
    SELECT COUNT(*)
    FROM admin_operation_log
    WHERE 1=1
    <if test="adminId != null">
        AND admin_id = #{adminId}
    </if>
    <if test="targetType != null">
        AND target_type = #{targetType}
    </if>
    <if test="action != null">
        AND action = #{action}
    </if>
    <if test="beginTime != null">
        AND create_time >= #{beginTime}
    </if>
    <if test="endTime != null">
        AND create_time <= #{endTime}
    </if>
</select>
```

---

### 5.6 阶段六：集成测试 (第6周)

#### Step 6.1: 编译验证

```bash
mvn compile -pl campus-life-backend -am
```

#### Step 6.2: 单元测试

```bash
mvn test -pl campus-life-backend
```

#### Step 6.3: API 兼容性验证

```bash
# 验证所有原有 API 端点可访问
# 帖子管理
curl -X GET http://localhost:8080/api/admin/posts

# 商家管理
curl -X GET http://localhost:8080/api/admin/merchants

# 用户管理
curl -X GET http://localhost:8080/api/admin/users

# 仪表盘
curl -X GET http://localhost:8080/api/admin/stats
```

---

## 六、API 端点对照表

| 瘦身前路径 | 瘦身后路径 | 控制器 |
|-----------|-----------|--------|
| `/api/admin/posts` | `/api/admin/posts` | AdminPostController |
| `/api/admin/posts/{id}/delete` | `/api/admin/posts/{id}/delete` | AdminPostController |
| `/api/admin/merchants` | `/api/admin/merchants` | AdminMerchantController |
| `/api/admin/merchants/{id}/approve` | `/api/admin/merchants/{id}/approve` | AdminMerchantController |
| `/api/admin/vouchers` | `/api/admin/vouchers` | AdminVoucherController |
| `/api/admin/users` | `/api/admin/users` | AdminUserController |
| `/api/admin/members` | `/api/admin/members` | AdminUserController |
| `/api/admin/comments` | `/api/admin/comments` | AdminCommentController |
| `/api/admin/reports` | `/api/admin/reports` | AdminReportController |
| `/api/admin/stats` | `/api/admin/stats` | AdminDashboardController |
| `/api/admin/refunds` | `/api/admin/refunds` | (保持不变) |

**结论**: 所有 API 路径保持不变。

---

## 七、风险与缓解措施

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| API 路径变化 | 高 | 保持原有 RequestMapping 路径不变 |
| 权限控制失效 | 高 | 按项目现有 RBAC 体系统一使用 `@RequireRole(RoleCode.ADMIN)` / `@RequirePermission` 注解 |
| 事务边界变化 | 中 | 确保每个编排方法有 @Transactional |
| 日志记录遗漏 | 中 | 逐个方法添加日志记录 |
| Dashboard 性能回退 | 低 | 编写性能测试验证 |

---

## 八、回滚方案

```bash
# 1. 保留 Git 分支
git checkout -b admin-refactor

# 2. 提交当前代码
git add -A
git commit -m "feat: Admin module refactoring - Phase 1"

# 3. 如需回滚
git checkout main
git checkout admin-refactor -- modules/admin/
```

---

## 九、验收标准

| 验收项 | 标准 |
|-------|------|
| **编译通过** | `mvn compile` 无错误 |
| **单元测试** | 所有现有测试通过 |
| **API 兼容性** | 所有原 API 端点路径保持不变 |
| **功能一致性** | 原有业务逻辑不变 |
| **权限控制** | 所有端点正确要求管理员权限 |
| **Dashboard 性能** | 使用 COUNT 聚合查询，无 OOM |
| **日志记录** | 所有操作正确记录审计日志 |
| **代码行数** | AdminService: 410行 → 各~150行 |

---

## 十、实施检查清单

### 阶段一：Controller 拆分
- [x] AdminPostController 创建完成
- [x] AdminMerchantController 创建完成
- [x] AdminVoucherController 创建完成
- [x] AdminUserController 创建完成
- [x] AdminCommentController 创建完成
- [x] AdminReportController 创建完成
- [x] AdminDashboardController 创建完成

### 阶段二：Orchestration Services
- [x] PostAdminOrchestrationService 创建完成
- [x] MerchantAdminOrchestrationService 创建完成
- [x] VoucherAdminOrchestrationService 创建完成
- [x] UserAdminOrchestrationService 创建完成
- [x] CommentAdminOrchestrationService 创建完成
- [x] ReportAdminOrchestrationService 创建完成
- [x] AdminOperationLogQueryService 创建完成

### 阶段三：Dashboard 优化
- [x] AdminDashboardQueryService 优化完成
- [x] DashboardStatsVO / TodayStatsVO 新增完成
- [x] UserTrendVO / PostTrendVO / OrderTrendVO 新增完成
- [x] AdminUserQueryService 新增方法
- [x] MerchantQueryService 新增方法
- [x] AdminCommunityQueryService 新增方法
- [x] AdminTradeQueryService 新增方法

### 阶段四：清理旧代码
- [x] AdminController.java 删除
- [x] AdminService.java 删除
- [x] 编译验证通过

### 阶段五：Mapper 优化
- [x] AdminOperationLogMapper.xml 更新
- [x] 分页逻辑验证通过

### 阶段六：集成测试
- [x] 单元测试全部通过
- [x] API 兼容性验证通过
- [x] 日志记录验证通过

> 实施说明：权限注解未引入新的 `@RequiresAdmin`，而是沿用当前代码库已落地的 `@RequireRole(RoleCode.ADMIN)` 与细粒度 `@RequirePermission`。

---

## 十一、后续优化建议

1. **引入 CQRS**: Dashboard 查询与业务写入分离
2. **缓存优化**: 统计结果缓存5分钟
3. **异步任务**: 操作日志异步写入
4. **权限细化**: 基于角色的权限控制 (RBAC)
5. **操作审计**: 增加操作前后的数据快照

---

## 十二、文档更新

实施完成后需更新以下文档：

| 文档 | 更新内容 |
|------|---------|
| 技术架构文档 | 更新模块依赖图 |
| API 文档 | 更新 Controller 归属 |
| 数据库文档 | 无需更新 |

---

*方案制定日期: 2026-04-30*
*版本: v1.0*
