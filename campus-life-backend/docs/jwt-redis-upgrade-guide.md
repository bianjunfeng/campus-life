# JWT + Redis 认证升级指南

## 概述

项目已从简单的 Base64 Token 认证升级为标准的 JWT + Redis 组合认证方案。

## 主要改进

### 1. 标准 JWT 格式
- ✅ 使用标准的 JWT（JSON Web Token）格式：`Header.Payload.Signature`
- ✅ 支持签名验证，防止 Token 被篡改
- ✅ 内置过期时间控制

### 2. 双 Token 机制
- **Access Token（访问令牌）**：用于 API 请求，默认有效期 7 天
- **Refresh Token（刷新令牌）**：用于刷新 Access Token，默认有效期 30 天

### 3. Redis 集成
- ✅ Token 黑名单管理（登出时使用）
- ✅ Token 刷新机制
- ✅ 支持 Redis 不可用时的降级处理（仅使用 JWT 验证）

## 新增功能

### 1. Token 刷新接口
```
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "your_refresh_token"
}

响应：
{
  "code": 200,
  "data": {
    "accessToken": "new_access_token",
    "refreshToken": "new_refresh_token"
  }
}
```

### 2. 登出接口
```
POST /api/auth/logout
Authorization: Bearer {accessToken}

响应：
{
  "code": 200,
  "data": "登出成功"
}
```

## API 变更说明

### 登录/注册接口变更

**旧版本返回：**
```json
{
  "code": 200,
  "data": {
    "token": "xxx",
    "user": {...}
  }
}
```

**新版本返回（向后兼容）：**
```json
{
  "code": 200,
  "data": {
    "token": "xxx",           // 兼容旧字段
    "accessToken": "xxx",     // 新的访问令牌
    "refreshToken": "xxx",    // 新的刷新令牌
    "user": {...}
  }
}
```

## 配置说明

### application-dev.yml

```yaml
jwt:
  secret: campus-life-secret-key-2025-please-change-in-production  # 生产环境请修改
  expiration: 604800000  # 访问令牌过期时间（毫秒），默认7天
  refresh-expiration: 2592000000  # 刷新令牌过期时间（毫秒），默认30天
```

**⚠️ 重要：生产环境请修改 `jwt.secret` 为更复杂的密钥！**

## 前端集成建议

### 1. 存储 Token

```javascript
// 登录成功后
const { accessToken, refreshToken } = response.data.data;
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);
```

### 2. 请求拦截器

```javascript
http.interceptors.request.use(config => {
  const accessToken = localStorage.getItem('accessToken');
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});
```

### 3. 响应拦截器 - Token 刷新

```javascript
http.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;
    
    // 如果是 401 错误且未重试过
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        // 使用 refreshToken 刷新 accessToken
        const refreshToken = localStorage.getItem('refreshToken');
        const { data } = await http.post('/api/auth/refresh', {
          refreshToken: refreshToken
        });
        
        const { accessToken, refreshToken: newRefreshToken } = data.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', newRefreshToken);
        
        // 重试原请求
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return http(originalRequest);
      } catch (refreshError) {
        // 刷新失败，跳转到登录页
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);
```

### 4. 登出

```javascript
async function logout() {
  try {
    await http.post('/api/auth/logout');
  } finally {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userInfo');
    window.location.href = '/login';
  }
}
```

## Redis 数据结构

### Token 存储
- Key: `token:{userId}:{accessToken}`
- Value: `{refreshToken}`
- TTL: 与 Access Token 过期时间一致

### Refresh Token 存储
- Key: `refresh_token:{userId}:{refreshToken}`
- Value: `{accessToken}`
- TTL: 30 天

### 黑名单
- Key: `blacklist:{accessToken}`
- Value: `1`
- TTL: 与 Access Token 剩余有效期一致

## 降级处理

如果 Redis 不可用，系统会自动降级：
- ✅ JWT 验证仍然有效（基于签名和过期时间）
- ⚠️ Token 黑名单功能不可用（登出后 Token 仍可使用直到过期）
- ⚠️ 刷新令牌验证仅依赖 JWT（不检查 Redis）

## 安全建议

1. **生产环境密钥**：务必修改 `jwt.secret` 为复杂的随机字符串
2. **HTTPS**：生产环境必须使用 HTTPS 传输 Token
3. **Token 存储**：前端建议使用 `httpOnly` Cookie 或安全的存储方式
4. **过期时间**：根据业务需求调整 Token 过期时间
5. **刷新策略**：建议在 Access Token 即将过期前自动刷新

## 迁移检查清单

- [x] 添加 JWT 依赖
- [x] 创建 JwtUtil 工具类
- [x] 创建 TokenService 服务
- [x] 更新 AuthService
- [x] 更新 AuthController
- [x] 添加配置
- [x] 添加刷新和登出接口
- [ ] 前端更新 Token 存储逻辑
- [ ] 前端添加 Token 刷新机制
- [ ] 测试 Token 刷新流程
- [ ] 测试登出功能
- [ ] 生产环境修改 JWT Secret

## 注意事项

1. **向后兼容**：旧版本的 `token` 字段仍然返回，但建议前端迁移到 `accessToken`
2. **Token 格式**：新 Token 是标准的 JWT 格式，可以通过 [jwt.io](https://jwt.io) 解码查看（但不包含敏感信息）
3. **Redis 依赖**：虽然支持降级，但建议生产环境使用 Redis 以获得完整的黑名单功能

## 问题排查

### Token 验证失败
1. 检查 JWT Secret 配置是否正确
2. 检查 Token 是否过期
3. 检查 Redis 连接是否正常（如果使用黑名单）

### 刷新 Token 失败
1. 检查 Refresh Token 是否过期
2. 检查 Redis 中是否存在该 Refresh Token（如果 Redis 可用）
3. 检查 Token 类型是否为 "refresh"

## 相关文件

- `JwtUtil.java` - JWT 工具类
- `TokenService.java` - Token 管理服务
- `AuthService.java` - 认证服务（已更新）
- `AuthController.java` - 认证控制器（已更新）
- `application-dev.yml` - 配置文件（已添加 JWT 配置）

