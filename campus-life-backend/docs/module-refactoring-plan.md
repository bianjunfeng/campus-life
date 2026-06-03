# 后端代码模块化重构计划

## 目标
将当前按技术层次组织的代码（controller/service/mapper/entity）重构为按业务模块组织的代码结构。

## 新的目录结构

```
campus_life_backend/
├── CampusLifeBackendApplication.java
│
├── common/                    # 公共模块（保持不变）
│   ├── ApiResponse.java
│   ├── GlobalExceptionHandler.java
│   ├── JwtUtil.java
│   └── AdminAuthUtil.java
│
├── config/                    # 配置模块（保持不变）
│   ├── CorsConfig.java
│   ├── MyBatisConfig.java
│   ├── RedisConfig.java
│   └── SecurityConfig.java
│
├── user/                      # 用户模块
│   ├── controller/
│   │   └── UserController.java
│   ├── service/
│   │   ├── UserService.java
│   │   └── UserFollowService.java
│   ├── mapper/
│   │   ├── UserMapper.java
│   │   ├── UserFollowMapper.java
│   │   └── StudentProfileMapper.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── UserFollow.java
│   │   ├── StudentProfile.java
│   │   └── StudentAuthRequest.java
│   └── dto/
│       └── UserDTO.java
│
├── auth/                      # 认证模块
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── TokenService.java
│   │   ├── VerificationService.java
│   │   ├── OAuthService.java
│   │   └── impl/
│   │       ├── VerificationServiceImpl.java
│   │       ├── WechatOAuthService.java
│   │       └── QQOAuthService.java
│   ├── entity/
│   │   └── LoginToken.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── RegisterRequest.java
│       └── SendVerificationCodeRequest.java
│
├── forum/                     # 论坛模块
│   ├── controller/
│   │   ├── PostController.java
│   │   ├── CommentController.java
│   │   └── CategoryController.java
│   ├── service/
│   │   ├── PostService.java
│   │   └── CommentService.java
│   ├── mapper/
│   │   ├── PostMapper.java
│   │   ├── CommentMapper.java
│   │   ├── PostCategoryMapper.java
│   │   ├── PostLikeMapper.java
│   │   ├── PostFavoriteMapper.java
│   │   └── PostImageMapper.java
│   ├── entity/
│   │   ├── Post.java
│   │   ├── Comment.java
│   │   ├── PostCategory.java
│   │   ├── PostLike.java
│   │   ├── PostFavorite.java
│   │   ├── PostImage.java
│   │   ├── CommentLike.java
│   │   ├── BrowseHistory.java
│   │   └── Report.java
│   └── dto/
│       └── PostDTO.java
│
├── merchant/                  # 商家模块
│   ├── controller/
│   │   ├── MerchantController.java
│   │   └── MerchantVoucherController.java
│   ├── mapper/
│   │   ├── MerchantMapper.java
│   │   └── MerchantTypeMapper.java
│   └── entity/
│       ├── Merchant.java
│       ├── MerchantType.java
│       └── MerchantAuthRequest.java
│
├── voucher/                   # 优惠券模块
│   ├── controller/
│   │   ├── VoucherController.java
│   │   ├── CouponController.java
│   │   └── ProductController.java
│   ├── service/
│   │   └── VoucherService.java
│   ├── mapper/
│   │   ├── VoucherMapper.java
│   │   ├── SeckillVoucherMapper.java
│   │   └── VoucherOrderMapper.java
│   └── entity/
│       ├── Voucher.java
│       ├── SeckillVoucher.java
│       └── VoucherOrder.java
│
├── admin/                     # 管理模块
│   ├── controller/
│   │   └── AdminController.java
│   ├── service/
│   │   └── AdminService.java
│   ├── mapper/
│   │   └── AdminOperationLogMapper.java
│   └── entity/
│       └── AdminOperationLog.java
│
├── file/                      # 文件模块
│   └── controller/
│       └── FileUploadController.java
│
├── health/                    # 健康检查（保持不变）
│   └── HealthController.java
│
└── util/                      # 工具类（保持不变）
    └── PasswordHashGenerator.java
```

## 文件移动映射表

### User模块
- controller/UserController.java → user/controller/UserController.java
- service/UserService.java → user/service/UserService.java
- service/UserFollowService.java → user/service/UserFollowService.java
- mapper/UserMapper.java → user/mapper/UserMapper.java
- mapper/UserFollowMapper.java → user/mapper/UserFollowMapper.java
- mapper/StudentProfileMapper.java → user/mapper/StudentProfileMapper.java
- entity/User.java → user/entity/User.java
- entity/UserFollow.java → user/entity/UserFollow.java
- entity/StudentProfile.java → user/entity/StudentProfile.java
- entity/StudentAuthRequest.java → user/entity/StudentAuthRequest.java
- dto/UserDTO.java → user/dto/UserDTO.java

### Auth模块
- controller/AuthController.java → auth/controller/AuthController.java
- service/AuthService.java → auth/service/AuthService.java
- service/TokenService.java → auth/service/TokenService.java
- service/VerificationService.java → auth/service/VerificationService.java
- service/OAuthService.java → auth/service/OAuthService.java
- service/impl/VerificationServiceImpl.java → auth/service/impl/VerificationServiceImpl.java
- service/impl/WechatOAuthService.java → auth/service/impl/WechatOAuthService.java
- service/impl/QQOAuthService.java → auth/service/impl/QQOAuthService.java
- entity/LoginToken.java → auth/entity/LoginToken.java
- dto/auth/* → auth/dto/*

### Forum模块
- controller/PostController.java → forum/controller/PostController.java
- controller/CommentController.java → forum/controller/CommentController.java
- controller/CategoryController.java → forum/controller/CategoryController.java
- service/PostService.java → forum/service/PostService.java
- service/CommentService.java → forum/service/CommentService.java
- mapper/PostMapper.java → forum/mapper/PostMapper.java
- mapper/CommentMapper.java → forum/mapper/CommentMapper.java
- mapper/PostCategoryMapper.java → forum/mapper/PostCategoryMapper.java
- mapper/PostLikeMapper.java → forum/mapper/PostLikeMapper.java
- mapper/PostFavoriteMapper.java → forum/mapper/PostFavoriteMapper.java
- mapper/PostImageMapper.java → forum/mapper/PostImageMapper.java
- entity/Post.java → forum/entity/Post.java
- entity/Comment.java → forum/entity/Comment.java
- entity/PostCategory.java → forum/entity/PostCategory.java
- entity/PostLike.java → forum/entity/PostLike.java
- entity/PostFavorite.java → forum/entity/PostFavorite.java
- entity/PostImage.java → forum/entity/PostImage.java
- entity/CommentLike.java → forum/entity/CommentLike.java
- entity/BrowseHistory.java → forum/entity/BrowseHistory.java
- entity/Report.java → forum/entity/Report.java
- dto/PostDTO.java → forum/dto/PostDTO.java

### Merchant模块
- controller/MerchantController.java → merchant/controller/MerchantController.java
- controller/MerchantVoucherController.java → merchant/controller/MerchantVoucherController.java
- mapper/MerchantMapper.java → merchant/mapper/MerchantMapper.java
- mapper/MerchantTypeMapper.java → merchant/mapper/MerchantTypeMapper.java
- entity/Merchant.java → merchant/entity/Merchant.java
- entity/MerchantType.java → merchant/entity/MerchantType.java
- entity/MerchantAuthRequest.java → merchant/entity/MerchantAuthRequest.java

### Voucher模块
- controller/VoucherController.java → voucher/controller/VoucherController.java
- controller/CouponController.java → voucher/controller/CouponController.java
- controller/ProductController.java → voucher/controller/ProductController.java
- service/VoucherService.java → voucher/service/VoucherService.java
- mapper/VoucherMapper.java → voucher/mapper/VoucherMapper.java
- mapper/SeckillVoucherMapper.java → voucher/mapper/SeckillVoucherMapper.java
- mapper/VoucherOrderMapper.java → voucher/mapper/VoucherOrderMapper.java
- entity/Voucher.java → voucher/entity/Voucher.java
- entity/SeckillVoucher.java → voucher/entity/SeckillVoucher.java
- entity/VoucherOrder.java → voucher/entity/VoucherOrder.java

### Admin模块
- controller/AdminController.java → admin/controller/AdminController.java
- service/AdminService.java → admin/service/AdminService.java
- mapper/AdminOperationLogMapper.java → admin/mapper/AdminOperationLogMapper.java
- entity/AdminOperationLog.java → admin/entity/AdminOperationLog.java

### File模块
- controller/FileUploadController.java → file/controller/FileUploadController.java

## 注意事项

1. **BaseController处理**：BaseController被多个Controller继承，可以：
   - 保留在common模块
   - 或者移动到auth模块（因为主要提供认证相关功能）

2. **Message实体**：如果Message是消息相关，可以创建message模块，或者暂时保留在common中

3. **Package声明更新**：所有移动的文件都需要更新package声明

4. **Import语句更新**：所有引用移动文件的地方都需要更新import语句

5. **Mapper XML文件**：需要同步移动mapper XML文件到对应模块的resources目录

## 执行步骤

1. 创建新的目录结构
2. 移动文件并更新package声明
3. 更新所有import语句
4. 移动Mapper XML文件
5. 编译测试
6. 修复错误

