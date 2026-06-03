# 微服务模块拆分映射表

## 一、后端模块到微服务的映射

### 1. 用户服务 (user-service)

| 原模块路径 | 目标服务 | API路径 | 说明 |
|-----------|---------|---------|------|
| `controller/AuthController.java` | user-service | `/api/auth/**` | 认证相关 |
| `controller/UserController.java` | user-service | `/api/users/**` | 用户管理 |
| `service/AuthService.java` | user-service | - | 认证服务 |
| `service/UserService.java` | user-service | - | 用户服务 |
| `service/UserFollowService.java` | user-service | - | 关注服务 |
| `service/TokenService.java` | user-service | - | Token管理 |
| `service/OAuthService.java` | user-service | - | OAuth服务 |
| `entity/User.java` | user-service | - | 用户实体 |
| `entity/UserFollow.java` | user-service | - | 关注实体 |
| `entity/LoginToken.java` | user-service | - | Token实体 |
| `entity/StudentProfile.java` | user-service | - | 学生资料 |
| `entity/StudentAuthRequest.java` | user-service | - | 学生认证 |
| `mapper/UserMapper.java` | user-service | - | 用户Mapper |
| `mapper/UserFollowMapper.java` | user-service | - | 关注Mapper |
| `mapper/StudentProfileMapper.java` | user-service | - | 学生Mapper |

**数据库：** `user_db`
- `user` 表
- `user_follow` 表
- `login_token` 表
- `student_profile` 表
- `student_auth_request` 表

---

### 2. 论坛服务 (forum-service)

| 原模块路径 | 目标服务 | API路径 | 说明 |
|-----------|---------|---------|------|
| `controller/PostController.java` | forum-service | `/api/forum/posts/**` | 帖子管理 |
| `controller/CommentController.java` | forum-service | `/api/forum/posts/{id}/comments/**` | 评论管理 |
| `controller/CategoryController.java` | forum-service | `/api/categories/**` | 分类管理 |
| `service/PostService.java` | forum-service | - | 帖子服务 |
| `service/CommentService.java` | forum-service | - | 评论服务 |
| `entity/Post.java` | forum-service | - | 帖子实体 |
| `entity/Comment.java` | forum-service | - | 评论实体 |
| `entity/PostCategory.java` | forum-service | - | 分类实体 |
| `entity/PostLike.java` | forum-service | - | 点赞实体 |
| `entity/PostFavorite.java` | forum-service | - | 收藏实体 |
| `entity/PostImage.java` | forum-service | - | 帖子图片 |
| `entity/CommentLike.java` | forum-service | - | 评论点赞 |
| `entity/BrowseHistory.java` | forum-service | - | 浏览历史 |
| `entity/Report.java` | forum-service | - | 举报实体 |
| `mapper/PostMapper.java` | forum-service | - | 帖子Mapper |
| `mapper/CommentMapper.java` | forum-service | - | 评论Mapper |
| `mapper/PostCategoryMapper.java` | forum-service | - | 分类Mapper |
| `mapper/PostLikeMapper.java` | forum-service | - | 点赞Mapper |
| `mapper/PostFavoriteMapper.java` | forum-service | - | 收藏Mapper |
| `mapper/PostImageMapper.java` | forum-service | - | 图片Mapper |

**数据库：** `forum_db`
- `post` 表
- `comment` 表
- `post_category` 表
- `post_like` 表
- `post_favorite` 表
- `post_image` 表
- `comment_like` 表
- `browse_history` 表
- `report` 表

**依赖服务：**
- 需要调用 `user-service` 获取用户信息

---

### 3. 商家服务 (merchant-service)

| 原模块路径 | 目标服务 | API路径 | 说明 |
|-----------|---------|---------|------|
| `controller/MerchantController.java` | merchant-service | `/api/shops/**` | 商家信息 |
| `controller/MerchantVoucherController.java` | merchant-service | `/api/merchant/vouchers/**` | 商家优惠券 |
| `entity/Merchant.java` | merchant-service | - | 商家实体 |
| `entity/MerchantType.java` | merchant-service | - | 商家类型 |
| `entity/MerchantAuthRequest.java` | merchant-service | - | 商家认证 |
| `mapper/MerchantMapper.java` | merchant-service | - | 商家Mapper |
| `mapper/MerchantTypeMapper.java` | merchant-service | - | 类型Mapper |

**数据库：** `merchant_db`
- `merchant` 表
- `merchant_type` 表
- `merchant_auth_request` 表

**依赖服务：**
- 需要调用 `user-service` 获取用户信息
- 需要调用 `voucher-service` 管理优惠券（可选，或合并到本服务）

---

### 4. 优惠券服务 (voucher-service)

| 原模块路径 | 目标服务 | API路径 | 说明 |
|-----------|---------|---------|------|
| `controller/VoucherController.java` | voucher-service | `/api/vouchers/**` | 优惠券 |
| `controller/CouponController.java` | voucher-service | `/api/coupons/**` | 福利券 |
| `controller/ProductController.java` | voucher-service | `/api/products/**` | 商品详情 |
| `service/VoucherService.java` | voucher-service | - | 优惠券服务 |
| `entity/Voucher.java` | voucher-service | - | 优惠券实体 |
| `entity/SeckillVoucher.java` | voucher-service | - | 秒杀券实体 |
| `entity/VoucherOrder.java` | voucher-service | - | 订单实体 |
| `mapper/VoucherMapper.java` | voucher-service | - | 优惠券Mapper |
| `mapper/SeckillVoucherMapper.java` | voucher-service | - | 秒杀Mapper |
| `mapper/VoucherOrderMapper.java` | voucher-service | - | 订单Mapper |

**数据库：** `voucher_db`
- `voucher` 表
- `seckill_voucher` 表
- `voucher_order` 表

**依赖服务：**
- 需要调用 `user-service` 获取用户信息
- 需要调用 `merchant-service` 获取商家信息

---

### 5. 管理服务 (admin-service)

| 原模块路径 | 目标服务 | API路径 | 说明 |
|-----------|---------|---------|------|
| `controller/AdminController.java` | admin-service | `/api/admin/**` | 管理后台 |
| `service/AdminService.java` | admin-service | - | 管理服务 |
| `entity/AdminOperationLog.java` | admin-service | - | 操作日志 |
| `mapper/AdminOperationLogMapper.java` | admin-service | - | 日志Mapper |
| `common/AdminAuthUtil.java` | admin-service | - | 管理认证工具 |

**数据库：** `admin_db`
- `admin_operation_log` 表
- 其他管理相关表

**依赖服务：**
- 需要调用所有其他服务进行数据聚合和操作

---

### 6. 文件服务 (file-service)

| 原模块路径 | 目标服务 | API路径 | 说明 |
|-----------|---------|---------|------|
| `controller/FileUploadController.java` | file-service | `/api/files/**`, `/api/upload/**` | 文件上传 |

**数据库：** `file_db`（可选，或使用对象存储）
- 文件元数据表（如需要）

**存储方案：**
- 本地存储（开发环境）
- OSS对象存储（生产环境）

---

### 7. 公共模块 (common-module)

| 原模块路径 | 目标模块 | 说明 |
|-----------|---------|------|
| `common/ApiResponse.java` | common-core | 统一响应格式 |
| `common/GlobalExceptionHandler.java` | common-core | 全局异常处理 |
| `common/JwtUtil.java` | common-core | JWT工具类 |
| `config/RedisConfig.java` | common-core | Redis配置 |
| `config/MyBatisConfig.java` | common-core | MyBatis配置 |
| `config/CorsConfig.java` | common-core | 跨域配置 |
| `config/SecurityConfig.java` | common-core | 安全配置（部分） |
| `entity/*` | common-entity | 共享实体（如需要） |

**说明：**
- 公共模块被所有服务依赖
- 版本管理需要统一

---

## 二、前端API调用映射（保持不变）

### 前端文件 → 后端服务路由

| 前端文件 | API调用 | 路由到服务 | 说明 |
|---------|---------|-----------|------|
| `api/auth.ts` | `/api/auth/**` | user-service | 认证相关 |
| `api/auth.ts` | `/api/users/**` | user-service | 用户信息 |
| `api/forum.ts` | `/api/forum/**` | forum-service | 论坛相关 |
| `api/forum.ts` | `/api/posts/**` | forum-service | 帖子相关 |
| `api/forum.ts` | `/api/comments/**` | forum-service | 评论相关 |
| `api/categoryApi.ts` | `/api/categories/**` | forum-service | 分类相关 |
| `api/merchant.ts` | `/api/shops/**` | merchant-service | 商家信息 |
| `api/merchant.ts` | `/api/merchant/**` | merchant-service | 商家管理 |
| `api/welfareApi.ts` | `/api/coupons/**` | voucher-service | 福利券 |
| `api/welfareApi.ts` | `/api/flash-sales/**` | voucher-service | 秒杀活动 |
| `api/admin.ts` | `/api/admin/**` | admin-service | 管理后台 |

**结论：前端代码无需修改！** ✅

---

## 三、服务依赖关系图

```
┌─────────────┐
│ API Gateway │
└──────┬──────┘
       │
       ├──→ user-service (独立)
       │
       ├──→ forum-service ──→ user-service
       │
       ├──→ merchant-service ──→ user-service
       │
       ├──→ voucher-service ──→ user-service
       │                      └──→ merchant-service
       │
       ├──→ admin-service ──→ user-service
       │                  ├──→ forum-service
       │                  ├──→ merchant-service
       │                  └──→ voucher-service
       │
       └──→ file-service (独立)
```

---

## 四、数据库拆分方案

### 4.1 数据库分配

| 服务 | 数据库名 | 包含表 |
|------|---------|--------|
| user-service | `user_db` | user, user_follow, login_token, student_profile, student_auth_request |
| forum-service | `forum_db` | post, comment, post_category, post_like, post_favorite, post_image, comment_like, browse_history, report |
| merchant-service | `merchant_db` | merchant, merchant_type, merchant_auth_request |
| voucher-service | `voucher_db` | voucher, seckill_voucher, voucher_order |
| admin-service | `admin_db` | admin_operation_log, admin_user (如需要) |
| file-service | `file_db` (可选) | file_metadata (如需要) |

### 4.2 跨服务数据访问原则

1. **禁止直接跨库访问**
   - ❌ 论坛服务不能直接查询 `user_db.user` 表
   - ✅ 论坛服务通过调用 `user-service` API 获取用户信息

2. **数据同步策略**
   - **实时查询**：通过服务调用（推荐）
   - **缓存策略**：Redis缓存用户信息，减少服务调用
   - **数据冗余**：必要时在本地服务冗余部分用户信息（如用户名、头像）

---

## 五、迁移优先级建议

### 阶段1：基础设施（1周）
1. ✅ 搭建API网关
2. ✅ 搭建服务注册中心（Nacos/Eureka）
3. ✅ 创建公共模块
4. ✅ 配置统一日志和监控

### 阶段2：独立服务（2周）
1. **文件服务**（最简单，无依赖）
2. **用户服务**（基础服务，其他服务依赖）

### 阶段3：业务服务（3-4周）
3. **论坛服务**（依赖用户服务）
4. **商家服务**（依赖用户服务）
5. **优惠券服务**（依赖用户服务和商家服务）

### 阶段4：管理服务（1-2周）
6. **管理服务**（依赖所有服务）

---

## 六、检查清单

### 6.1 服务拆分检查
- [ ] 确定服务边界和职责
- [ ] 识别服务间依赖关系
- [ ] 设计服务间通信方式
- [ ] 规划数据库拆分方案

### 6.2 代码迁移检查
- [ ] 创建服务项目骨架
- [ ] 迁移Controller层
- [ ] 迁移Service层
- [ ] 迁移Mapper层
- [ ] 迁移Entity层
- [ ] 配置数据库连接

### 6.3 网关配置检查
- [ ] 配置路由规则
- [ ] 配置负载均衡
- [ ] 配置统一认证
- [ ] 配置跨域处理
- [ ] 配置限流熔断

### 6.4 测试检查
- [ ] 单元测试
- [ ] 集成测试
- [ ] 前端回归测试
- [ ] 性能测试
- [ ] 压力测试

### 6.5 部署检查
- [ ] Docker镜像构建
- [ ] 容器编排配置（如使用K8s）
- [ ] 环境变量配置
- [ ] 监控告警配置
- [ ] 日志收集配置

---

**文档版本：** v1.0  
**创建日期：** 2025-01-XX

