# 微服务架构设计方案

## 一、模块拆分方案

### 1.1 服务模块划分

根据当前项目结构，建议按以下模块拆分为独立的微服务：

#### 1. 用户服务 (user-service)
**职责：** 用户管理、认证授权、用户资料
- **API路径：** `/api/users`, `/api/auth`
- **包含功能：**
  - 用户注册/登录
  - OAuth登录（微信、QQ）
  - 用户信息管理
  - 用户关注/粉丝
  - 学生认证
  - Token管理

#### 2. 论坛服务 (forum-service)
**职责：** 帖子、评论、分类管理
- **API路径：** `/api/forum`, `/api/posts`, `/api/comments`, `/api/categories`
- **包含功能：**
  - 帖子CRUD
  - 评论管理
  - 帖子分类
  - 点赞/收藏
  - 浏览历史

#### 3. 商家服务 (merchant-service)
**职责：** 商家管理、商家认证
- **API路径：** `/api/shops`, `/api/merchant`
- **包含功能：**
  - 商家注册/认证
  - 商家信息管理
  - 商家类型管理
  - 商家优惠券管理

#### 4. 优惠券服务 (voucher-service)
**职责：** 优惠券、秒杀活动
- **API路径：** `/api/vouchers`, `/api/coupons`, `/api/flash-sales`
- **包含功能：**
  - 优惠券管理
  - 秒杀活动
  - 优惠券订单
  - 库存管理

#### 5. 管理服务 (admin-service)
**职责：** 后台管理功能
- **API路径：** `/api/admin`
- **包含功能：**
  - 内容审核
  - 用户管理
  - 商家审核
  - 数据统计
  - 操作日志
  - 系统监控

#### 6. 文件服务 (file-service)
**职责：** 文件上传、存储
- **API路径：** `/api/files`, `/api/upload`
- **包含功能：**
  - 图片上传
  - 文件存储
  - 文件访问

### 1.2 共享组件

#### 公共模块 (common-module)
- **职责：** 所有服务共享的基础组件
- **包含：**
  - 统一响应格式 (`ApiResponse`)
  - 异常处理 (`GlobalExceptionHandler`)
  - JWT工具类 (`JwtUtil`)
  - 数据库实体（部分共享）
  - 配置类（Redis、MyBatis等）

## 二、对前端的影响分析

### 2.1 影响评估：**几乎无影响** ✅

**原因：** 通过API网关统一入口，前端调用方式保持不变。

### 2.2 前端调用方式对比

#### 当前方式（单体应用）
```typescript
// http.ts
const http = axios.create({
  baseURL: '/api',  // 统一入口
  timeout: 10000
})

// 前端调用示例
http.get('/forum/posts')      // → 后端: /api/forum/posts
http.get('/users/me')         // → 后端: /api/users/me
http.get('/merchant/vouchers') // → 后端: /api/merchant/vouchers
```

#### 微服务化后（通过API网关）
```typescript
// http.ts - 无需修改！
const http = axios.create({
  baseURL: '/api',  // 仍然统一入口，由网关路由
  timeout: 10000
})

// 前端调用 - 完全不变！
http.get('/forum/posts')      // → 网关 → forum-service: /api/forum/posts
http.get('/users/me')         // → 网关 → user-service: /api/users/me
http.get('/merchant/vouchers') // → 网关 → merchant-service: /api/merchant/vouchers
```

### 2.3 需要的前端调整（可选优化）

#### 方案A：完全透明（推荐）
- **无需任何修改**，所有请求通过API网关路由
- 前端代码零改动

#### 方案B：服务发现优化（可选）
如果未来需要直接调用特定服务（不推荐），可以配置：
```typescript
// 可选：为不同服务配置不同的超时时间
const forumHttp = axios.create({
  baseURL: '/api/forum',
  timeout: 5000  // 论坛服务响应快
})

const voucherHttp = axios.create({
  baseURL: '/api/vouchers',
  timeout: 15000  // 优惠券服务可能较慢
})
```

## 三、技术架构设计

### 3.1 API网关方案

#### 方案选择：Spring Cloud Gateway 或 Nginx

**推荐：Spring Cloud Gateway**
- 与Spring Boot生态完美集成
- 支持服务发现、负载均衡
- 统一认证、限流、熔断

#### 网关路由配置示例
```yaml
spring:
  cloud:
    gateway:
      routes:
        # 用户服务
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**, /api/auth/**
          
        # 论坛服务
        - id: forum-service
          uri: lb://forum-service
          predicates:
            - Path=/api/forum/**, /api/posts/**, /api/comments/**, /api/categories/**
          
        # 商家服务
        - id: merchant-service
          uri: lb://merchant-service
          predicates:
            - Path=/api/shops/**, /api/merchant/**
          
        # 优惠券服务
        - id: voucher-service
          uri: lb://voucher-service
          predicates:
            - Path=/api/vouchers/**, /api/coupons/**, /api/flash-sales/**
          
        # 管理服务
        - id: admin-service
          uri: lb://admin-service
          predicates:
            - Path=/api/admin/**
          
        # 文件服务
        - id: file-service
          uri: lb://file-service
          predicates:
            - Path=/api/files/**, /api/upload/**
```

### 3.2 服务间通信

#### 同步调用：OpenFeign
```java
@FeignClient(name = "user-service", path = "/api/users")
public interface UserServiceClient {
    @GetMapping("/{userId}")
    ApiResponse<UserDTO> getUserById(@PathVariable Long userId);
}
```

#### 异步通信：消息队列（可选）
- RabbitMQ / RocketMQ
- 用于：订单通知、审核结果通知等

### 3.3 数据管理

#### 数据库拆分策略
1. **垂直拆分**：每个服务独立数据库
   - `user_db`: 用户、认证相关表
   - `forum_db`: 帖子、评论相关表
   - `merchant_db`: 商家相关表
   - `voucher_db`: 优惠券、订单相关表
   - `admin_db`: 管理后台相关表

2. **共享数据**：通过服务调用获取
   - 用户信息：其他服务通过 `user-service` 获取
   - 避免跨服务直接访问数据库

#### 分布式事务（如需要）
- **Seata**：处理跨服务事务
- **最终一致性**：优先使用最终一致性方案

### 3.4 服务注册与发现

#### 使用 Nacos 或 Eureka
```yaml
# application.yml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: campus-life
```

## 四、迁移步骤

### 阶段一：准备阶段（1-2周）
1. ✅ 创建公共模块 (`common-module`)
2. ✅ 搭建API网关项目
3. ✅ 配置服务注册中心（Nacos/Eureka）
4. ✅ 设计数据库拆分方案

### 阶段二：服务拆分（2-4周）
1. **用户服务**（优先级最高）
   - 迁移认证、用户管理功能
   - 保持API路径不变

2. **论坛服务**
   - 迁移帖子、评论功能
   - 测试与用户服务的交互

3. **商家服务**
   - 迁移商家管理功能

4. **优惠券服务**
   - 迁移优惠券、订单功能

5. **管理服务**
   - 迁移后台管理功能

6. **文件服务**
   - 迁移文件上传功能

### 阶段三：测试与优化（1-2周）
1. 集成测试
2. 性能测试
3. 前端回归测试（确保无影响）
4. 监控与日志完善

### 阶段四：上线（1周）
1. 灰度发布
2. 全量切换
3. 监控告警

## 五、项目结构示例

```
campus-life-microservices/
├── gateway/                    # API网关
│   └── src/main/java/com/campus/gateway/
├── common/                     # 公共模块
│   ├── common-core/           # 核心工具类
│   ├── common-entity/         # 共享实体
│   └── common-feign/          # Feign客户端
├── user-service/              # 用户服务
│   ├── src/main/java/com/campus/user/
│   └── pom.xml
├── forum-service/             # 论坛服务
│   ├── src/main/java/com/campus/forum/
│   └── pom.xml
├── merchant-service/          # 商家服务
│   ├── src/main/java/com/campus/merchant/
│   └── pom.xml
├── voucher-service/           # 优惠券服务
│   ├── src/main/java/com/campus/voucher/
│   └── pom.xml
├── admin-service/             # 管理服务
│   ├── src/main/java/com/campus/admin/
│   └── pom.xml
├── file-service/              # 文件服务
│   ├── src/main/java/com/campus/file/
│   └── pom.xml
└── pom.xml                    # 父POM
```

## 六、优势与挑战

### 6.1 优势
1. ✅ **独立部署**：各服务可独立发布，互不影响
2. ✅ **技术栈灵活**：不同服务可使用不同技术栈
3. ✅ **扩展性强**：可按需扩展高并发服务
4. ✅ **团队协作**：不同团队负责不同服务
5. ✅ **故障隔离**：单个服务故障不影响整体

### 6.2 挑战与解决方案
1. **服务间调用复杂度**
   - 解决：使用Feign简化调用，统一异常处理

2. **分布式事务**
   - 解决：优先使用最终一致性，必要时使用Seata

3. **数据一致性**
   - 解决：通过服务调用获取数据，避免跨服务直接访问数据库

4. **运维复杂度**
   - 解决：使用容器化（Docker/K8s），统一监控（Prometheus + Grafana）

## 七、前端调用示例（保持不变）

### 当前前端代码
```typescript
// src/api/http.ts - 无需修改
import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// src/api/auth.ts - 无需修改
export async function loginApi(payload: LoginPayload) {
  const { data } = await http.post('/auth/tokens', payload)
  return data
}

// src/api/forum.ts - 无需修改
export async function fetchPosts(categoryId?: string) {
  const { data } = await http.get('/forum/posts', { params: { categoryId } })
  return data.data
}

// src/api/merchant.ts - 无需修改
export async function getMerchantVoucherList(params) {
  const { data } = await http.get('/merchant/vouchers', { params })
  return data.data
}
```

**结论：前端代码完全不需要修改！** ✅

## 八、总结

### 8.1 关键点
1. ✅ **前端零影响**：通过API网关统一入口，前端调用方式完全不变
2. ✅ **渐进式迁移**：可以逐个服务拆分，不影响现有功能
3. ✅ **向后兼容**：保持API路径不变，确保平滑过渡

### 8.2 建议
1. **先搭建网关**：确保网关正常工作后再拆分服务
2. **优先拆分独立模块**：如文件服务、论坛服务
3. **保持API兼容**：拆分过程中保持API路径和响应格式不变
4. **充分测试**：每个服务拆分后都要进行完整测试

### 8.3 下一步行动
1. 创建微服务项目骨架
2. 搭建API网关
3. 迁移第一个服务（建议从文件服务或论坛服务开始）
4. 逐步迁移其他服务

---

**文档版本：** v1.0  
**创建日期：** 2025-01-XX  
**最后更新：** 2025-01-XX

