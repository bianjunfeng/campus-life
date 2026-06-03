# 阶段3灰度前变更说明（管理员登录安全）

更新日期：2026-03-09

## 1. 变更目标
- 修复管理员弱口令可登录风险。
- 增加登录失败限流，降低暴力破解风险。
- 明确关键接口状态码语义，保证前后端联调一致。

## 2. 后端变更
- 管理员弱口令拦截：管理员使用弱口令登录时返回 403。
- 登录失败限流：同一手机号连续输错密码达到阈值后返回 429，并在锁定窗口内拒绝登录。
- 新增管理员改密接口：`POST /api/auth/admin/change-password`
  - 未登录/无效 token：HTTP 401
  - 改密成功：HTTP 200
  - 原密码错误/新密码强度不足：HTTP 400
- 改密后安全处理：吊销该管理员已签发 token（强制重新登录）。
- 新增配置项：
  - `auth.login.max-failures`
  - `auth.login.lock-minutes`
  - `auth.login.failure-window-minutes`
  - `auth.admin.block-weak-password-login`

## 3. 前端变更
- 移除登录页明文演示账号/密码展示，避免口令泄露放大风险。

## 4. 核心验证结果
已通过测试：
- `GlobalExceptionHandlerTest`
- `AuthServiceTest`
- `LoginSecurityServiceTest`
- `AuthControllerTest`

执行命令：
```powershell
mvn "-Dmaven.repo.local=D:/code1/code/campus-life-backend/target/.m2repo" test "-Dtest=GlobalExceptionHandlerTest,AuthServiceTest,LoginSecurityServiceTest,AuthControllerTest"
```

结果：`Tests run: 16, Failures: 0, Errors: 0`

## 5. 灰度前执行清单
1. 用管理员强口令替换现有弱口令（必须先执行）。
2. 确认生产配置 `auth.admin.block-weak-password-login=true`。
3. 执行灰度验证脚本：
```powershell
pwsh ./scripts/admin-gray-smoke.ps1 \
  -BaseUrl "http://127.0.0.1:8080" \
  -AdminPhone "管理员手机号" \
  -AdminPassword "管理员密码" \
  -UserPhone "普通用户手机号" \
  -UserPassword "普通用户密码"
```
4. 若需要实测 429（可选）：
```powershell
pwsh ./scripts/admin-gray-smoke.ps1 \
  -BaseUrl "http://127.0.0.1:8080" \
  -AdminPhone "管理员手机号" \
  -AdminPassword "管理员密码" \
  -RunRateLimitCheck \
  -RateLimitPhone "测试手机号" \
  -WrongPassword "WrongPass@123"
```

## 6. 状态码验收口径
- 管理员访问后台只读接口：200
- 普通用户访问后台接口：403
- 管理员改密（无 token / 无效 token）：401
- 密码错误触发限流后登录：429

## 7. 回滚策略
- 临时关闭弱口令拦截：`auth.admin.block-weak-password-login=false`（仅应急，需记录审批）。
- 提高限流阈值：调整 `auth.login.*` 配置后重启服务。
- 代码回滚：回退本次安全变更提交并重新发布。
