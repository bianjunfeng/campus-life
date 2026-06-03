# 阶段3灰度提交落地指南

更新时间：2026-03-09

## 建议提交拆分
- 提交1（backend）：管理员登录安全加固 + 灰度脚本与文档
- 提交2（frontend）：登录页移除演示账号/密码展示

## 注意事项
- `logs/*.log` 为运行时文件，不建议纳入提交。
- 当前工作区包含日志变更，请先恢复日志文件再提交。

## 提交1（backend）建议命令
```powershell
cd D:/code1/code/campus-life-backend

git restore logs/campus-life-backend.log logs/campus-life-backend-error.log

git add \
  src/main/java/com/campus/campus_life_backend/modules/auth/service/AuthService.java \
  src/main/java/com/campus/campus_life_backend/modules/auth/service/LoginSecurityService.java \
  src/main/java/com/campus/campus_life_backend/modules/auth/controller/AuthController.java \
  src/main/java/com/campus/campus_life_backend/modules/auth/dto/AdminChangePasswordRequest.java \
  src/main/java/com/campus/campus_life_backend/common/exception/GlobalExceptionHandler.java \
  src/main/java/com/campus/campus_life_backend/modules/user/mapper/UserMapper.java \
  src/main/resources/mapper/user/UserMapper.xml \
  src/main/resources/application-dev.yml \
  src/main/resources/application-prod.yml \
  src/main/java/com/campus/campus_life_backend/common/util/PasswordHashGenerator.java \
  src/test/java/com/campus/campus_life_backend/common/exception/GlobalExceptionHandlerTest.java \
  src/test/java/com/campus/campus_life_backend/service/AuthServiceTest.java \
  src/test/java/com/campus/campus_life_backend/service/LoginSecurityServiceTest.java \
  src/test/java/com/campus/campus_life_backend/modules/auth/controller/AuthControllerTest.java \
  scripts/admin-gray-smoke.ps1 \
  docs/stage3-gray-release-checklist.md \
  docs/stage3-commit-guide.md

git commit -m "feat(auth): harden admin login and add gray-release checks"
```

## 提交2（frontend）建议命令
```powershell
cd D:/code1/code/campus-life-frontend

git add src/views/auth/LoginPage.vue

git commit -m "fix(auth-ui): remove demo credentials from login page"
```

## 回归命令（backend）
```powershell
cd D:/code1/code/campus-life-backend
mvn "-Dmaven.repo.local=D:/code1/code/campus-life-backend/target/.m2repo" test "-Dtest=GlobalExceptionHandlerTest,AuthServiceTest,LoginSecurityServiceTest,AuthControllerTest"
```

## 当前已验证结果
- Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
- BUILD SUCCESS
