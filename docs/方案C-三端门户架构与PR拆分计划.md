# 方案 C：三端门户架构与 PR 拆分计划

> 文档版本：v1.0  
> 更新日期：2026-06-08  
> 适用范围：`campus-life-frontend` 三端（用户端 / 商家端 / 管理端）架构演进  
> 状态：**PR-1 已合并至 `dev` 分支**，PR-2 ~ PR-8 待实施

---

## 1. 背景与动机

### 1.1 当前架构（改造前）

前端采用 **三独立应用 + 共享代码** 模式：

| 角色 | 应用目录 | 开发命令 | 端口 |
|------|----------|----------|------|
| 学生/用户端 | `apps/consumer-web` | `npm run dev:consumer` | 5173 |
| 管理端 | `apps/admin-web` | `npm run dev:admin` | 5174 |
| 商家端 | `apps/merchant-web` | `npm run dev:merchant` | 5175 |

- 页面组件复用 `src/views/`、`src/components/`
- 路由守卫统一在 `src/shared/router/auth.ts` 的 `createAuthGuard`
- 鉴权信息存 `sessionStorage`，三子域/三端口间 **不共享登录态**
- 遗留单体入口 `src/router/index.ts`（约 583 行）与 `apps/*` 双轨并存

### 1.2 主要痛点

| 类别 | 问题 |
|------|------|
| 权限 | 用户端未绑定 `STUDENT` 门户角色，管理员/商家可浏览部分用户页 |
| 跨端 | `ProfilePage` 用 `router.push('/admin')`，在 consumer SPA 内无效 |
| 登录 | `LoginPage` 用 `hasRoute('AdminDashboard')` 猜测门户，脆弱 |
| OAuth | 回调写死 `router.push('/home')`，商家/管理端错误 |
| 部署 | 子域名与单域路径两种 Nginx 方案并存，URL 前缀冗余（`admin.xxx.com/admin/...`） |
| 维护 | 三套路由 + 遗留 `src/router` 易漂移；Agent 等共享页靠 `startsWith('/admin/...')` 硬编码 |

### 1.3 改造目标

| 编号 | 目标 |
|------|------|
| G1 | 三子域独立入口，`life.*` / `merchant.*` / `admin.*` |
| G2 | 共享 `@campus/runtime`，鉴权/路由/HTTP/跨端导航统一 |
| G3 | 子域下 URL 去前缀：商家 `/home`，管理 `/dashboard` |
| G4 | 父域 Cookie SSO，同浏览器三端免重复登录 |
| G5 | 删除 `src/router/index.ts` 等双轨遗留 |
| G6 | 构建分离：`static/` 共享 + `portals/` 薄壳 + `modules/` 业务包 |

---

## 2. 方案对比

| 方案 | 思路 | 优点 | 缺点 | 适用 |
|------|------|------|------|------|
| **A. Portal Kernel** | 保留三构建，抽共享内核 | 改动小、可渐进 | 仍三份 bundle | 快速止血 |
| **B. 单 SPA + 懒加载** | 一个应用按门户拆 chunk | 开发最轻、跨端跳转自然 | 用户端包体变大 | 人手紧、接受大包 |
| **C. 子域薄入口 + 共享 Runtime**（**选定**） | 三薄 HTML + runtime + module | SSO 自然、URL 干净、bundle 隔离 | 构建与运维复杂 | **正式多门户生产** |

**结论：** 物理上三子域、三薄入口；逻辑上单一 `@campus/runtime`；鉴权上父域 HttpOnly Cookie SSO。

---

## 3. 方案 C 总体架构

### 3.1 三层模型

```
┌─────────────────────────────────────────────────────────┐
│  Layer 1: Portal Shell（薄入口，每子域一份 index.html）   │
│  只负责：读环境、加载 runtime、mount 对应 portal module    │
├─────────────────────────────────────────────────────────┤
│  Layer 2: @campus/runtime（共享运行时）                   │
│  createApp / router factory / auth / http / usePortal   │
├─────────────────────────────────────────────────────────┤
│  Layer 3: Portal Modules（门户业务包，按需动态 import）    │
│  @campus/module-student | module-merchant | module-admin│
└─────────────────────────────────────────────────────────┘
```

### 3.2 子域与 URL 规范

#### 生产环境（推荐）

| 门户 | 子域 | 路由前缀 | 默认首页 | OAuth 回调示例 |
|------|------|----------|----------|----------------|
| 用户端 | `life.example.com` | `/` | `/home` | `https://life.example.com/oauth/wechat/callback` |
| 商家端 | `merchant.example.com` | `/` | `/home` | `https://merchant.example.com/oauth/wechat/callback` |
| 管理端 | `admin.example.com` | `/` | `/dashboard` | `https://admin.example.com/oauth/wechat/callback` |

门户身份由 **Host** 决定，不再在 path 中编码 `/admin`、`/merchant`。

#### 开发环境

```
life.campus.local:5173       → 用户端
merchant.campus.local:5175   → 商家端（与 vite.merchant.config.js 一致）
admin.campus.local:5174      → 管理端
```

需在 hosts 或 dnsmasq 中配置 `*.campus.local`。

#### 路由迁移对照（Scheme C）

**商家端：**

| 原路径 | 新路径 |
|--------|--------|
| `/merchant/home` | `/home` |
| `/merchant/vouchers` | `/vouchers` |
| `/merchant/refunds` | `/refunds` |
| `/merchant/ai/*` | `/ai/*` |
| `/merchant/profile/*` | `/profile/*` |

**管理端：**

| 原路径 | 新路径 |
|--------|--------|
| `/admin/dashboard` | `/dashboard` |
| `/admin/posts` | `/posts` |
| `/admin/ai/agents` | `/ai/agents` |
| 其余 `/admin/*` | 去掉 `/admin` 前缀 |

**用户端：** `/home`、`/me`、`/agent` 等 **保持不变**。

### 3.3 门户注册表（`PORTALS`）

已在 PR-1 落地于 `campus-life-frontend/packages/runtime/src/portal/registry.ts`：

| PortalId | requiredRole | devHost | defaultRoute | routes.agent | routes.account | gates |
|----------|--------------|---------|--------------|--------------|----------------|-------|
| STUDENT | STUDENT | life.campus.local:5173 | /home | /agent | /me | — |
| MERCHANT | MERCHANT | merchant.campus.local:5175 | /home | /ai | /profile/info | merchantVerified |
| ADMIN | ADMIN | admin.campus.local:5174 | /dashboard | /ai/agents | /profile/info | — |

### 3.4 目标目录结构（终态）

```
campus-life-frontend/
├── packages/
│   ├── runtime/                 # @campus/runtime（PR-1 已建骨架）
│   ├── module-student/
│   ├── module-merchant/
│   └── module-admin/
├── portals/
│   ├── consumer/                # index.html + bootstrap.ts
│   ├── merchant/
│   └── admin/
├── shared/                        # 跨模块 views/components/styles
├── vite.runtime.config.ts
├── vite.modules.config.ts
├── vite.portals.config.ts
└── package.json                 # workspaces: ["packages/*"]

删除（PR-8）：
├── apps/
├── src/router/index.ts
├── src/main.js
├── vite.config.js
└── vite.consumer/admin/merchant.config.js
```

### 3.5 鉴权：父域 Cookie SSO

| Cookie | 用途 | 属性 |
|--------|------|------|
| `cl_access` | 短期 access JWT | `HttpOnly; Secure; SameSite=Lax; Domain=.example.com; Path=/api` |
| `cl_refresh` | refresh token | 同上，`Path=/api/auth/refresh` |

- 前端 `axios` 使用 `withCredentials: true`，不再从 `sessionStorage` 写 `Authorization`
- Gateway `JwtAuthGlobalFilter` 优先读 Cookie，**兼容** `Authorization: Bearer`（双轨过渡期）
- 会话来源统一为 `GET /api/users/me`
- 角色不匹配 → `navigatePortal()` 跳转正确子域，**不** `clearAuthState()`

### 3.6 构建产物（终态）

```
dist/
├── static/                    # runtime + vendor，长缓存
├── portals/consumer|merchant|admin/
└── modules/student|merchant|admin.[hash].js
```

---

## 4. 分阶段实施（P0 ~ P4）

| 阶段 | 周期 | 核心交付 |
|------|------|----------|
| **P0** 基建 | 第 1~2 周 | runtime 骨架、薄入口、构建拆包 |
| **P1** 鉴权 | 第 3 周 | 后端 Cookie SSO、前端 withCredentials |
| **P2** 路由 | 第 4~5 周 | 三 module 迁移、去前缀、usePortal |
| **P3** 部署 | 第 5~6 周 | Nginx 三子域、OAuth/WS Origin |
| **P4** 收尾 | 第 6~8 周 | 删遗留、全量回归、文档 |

每阶段可独立上线；PR-4 删除遗留前建议 **1 周双轨并行观察**。

---

## 5. PR 拆分（共 8 个）

| PR | 阶段 | 内容 | 可合并条件 | 状态 |
|----|------|------|------------|------|
| **PR-1** | P0 | `packages/runtime` 骨架 + 类型 | 单元测试通过 | ✅ 已完成 |
| **PR-2** | P0 | 构建配置 + portal shell 可启动 | 本地三端口可打开 | ⏳ 待做 |
| **PR-3** | P1 | 后端 Cookie SSO + Gateway | 接口测试通过 | ⏳ 待做 |
| **PR-4** | P1 | 前端鉴权改造 + LoginPage | 登录流程通 | ⏳ 待做 |
| **PR-5** | P2 | module-student 迁移 | 用户端全路由通 | ⏳ 待做 |
| **PR-6** | P2 | module-merchant/admin 迁移 + 去前缀 | 商家/管理端通 | ⏳ 待做 |
| **PR-7** | P2 | 共享页 usePortal + 跨端跳转 | 跨端 smoke 通过 | ⏳ 待做 |
| **PR-8** | P3+P4 | Nginx/Docker + 删遗留 + 文档 | 全量回归通过 | ⏳ 待做 |

---

### PR-1：`packages/runtime` 骨架 + 类型 ✅

**范围：**

- 新建 `packages/runtime/`（types、registry、navigate、guards/http/session 等 stub）
- `package.json` 增加 `workspaces`、`test:runtime`
- devDependencies：`vitest`、`typescript`

**不包含：** 不改 `apps/*`、不改 Vite、不接业务。

**验收：**

```bash
cd campus-life-frontend
npm install
npm run test:runtime   # 11 tests passed
```

**对现网影响：** 无（runtime 未被业务 import）。

---

### PR-2：构建配置 + portal shell 可启动

**新建：**

- `portals/{consumer,merchant,admin}/index.html` + `bootstrap.ts`
- `vite.runtime.config.ts`、`vite.modules.config.ts`、`vite.portals.config.ts`
- 实现 stub：`mountPortal`、`createPortalRouter`、`createHttpClient`

**修改：**

- 根 `package.json` 增加 `dev:portal:*`、`build:runtime` 等脚本
- `echarts` + `zrender` 合并为 `charts-vendor`（避免已知 chunk 问题）

**验收：**

- 三 portal shell 本地可打开空白 layout
- 旧 `apps/*` 仍可并行运行（双轨）

---

### PR-3：后端 Cookie SSO + Gateway

**修改：**

| 文件 | 改动 |
|------|------|
| `AuthController.java` | 登录/刷新/登出 `Set-Cookie` |
| `AuthService.java` | Cookie 模式（body 可保留 token 兼容移动端） |
| `JwtAuthGlobalFilter.java` | 优先 `Cookie: cl_access`，兼容 Bearer |
| `MessageWebSocketConfig.java` | `allowed-origin-patterns` → `https://*.example.com` |
| 相关测试类 | 同步更新 |

**验收：**

- 登录响应带 Set-Cookie
- Gateway 用 Cookie 通过鉴权
- 现有 Bearer 客户端仍可用

---

### PR-4：前端鉴权改造 + LoginPage

**修改：**

- `packages/runtime` 实现 `getSession`、`portalGuard`、`createHttpClient`（`withCredentials`）
- `LoginPage.vue`：改用 `usePortal()`，去掉 `hasRoute` 猜测
- `OAuthCallback.vue`：跳转 `portal.defaultRoute`
- `authStorage.ts`：token 逻辑标记 deprecated

**验收：**

- 三端登录/登出/刷新流程通
- OAuth 回调落到正确门户首页

---

### PR-5：module-student 迁移

**迁移：**

- `apps/consumer-web/router` → `packages/module-student/src/routes.ts`
- `apps/consumer-web/api/*` → `packages/module-student/src/api/`
- 守卫：`requiredRole: 'STUDENT'`

**验收：**

- 用户端全路由 smoke（首页、帖子、订单、支付、消息等）
- 非学生角色被重定向到正确门户

---

### PR-6：module-merchant/admin 迁移 + 去前缀

**迁移：**

- `apps/merchant-web` → `packages/module-merchant`
- `apps/admin-web` → `packages/module-admin`
- 路由按 §3.2 对照表去前缀

**重点改动文件：**

- `AdminLayout.vue`（约 50~120 处链接）
- `MerchantLayout.vue`、`MerchantConsoleNav.vue`
- 三套路由表

**验收：**

- `merchant.*` / `admin.*` 全路由可导航
- 商家 `merchantVerified` 门禁正常

---

### PR-7：共享页 usePortal + 跨端跳转

**修改：**

| 文件 | 改法 |
|------|------|
| `AgentListPage.vue` 等 Agent 页 | `usePortal().routes.agent` |
| `AccountCenter.vue`、`AccountUserMenu.vue` | `usePortal().routes.account` |
| `ProfilePage.vue` | `navigatePortal('ADMIN'|'MERCHANT')` 替代 `router.push` |

**验收：**

- 用户端 Profile → 管理/商家后台跳转正确子域
- Agent 三端内部链接正确

---

### PR-8：Nginx/Docker + 删遗留 + 文档

**部署：**

- `deploy/docker/frontend/default.conf.template`：三子域简化，共享 `static/`
- `Dockerfile`：COPY `dist/static` + `dist/portals`

**删除：**

- `apps/`、`src/router/index.ts`、`src/main.js`、旧 `vite.*.config.js`

**文档：**

- 更新 `campus-life-frontend/README.md`、`deploy/docker/README.md`

**全量回归清单：**

- [ ] 三端登录/登出/SSO
- [ ] 三端全路由 smoke
- [ ] OAuth 三回调
- [ ] WebSocket 消息
- [ ] 学生发帖/下单/支付
- [ ] 商家发券/退款审核
- [ ] 管理审核/统计
- [ ] `npm run build` 产物完整

---

## 6. 修改量估算

| 维度 | 数量 |
|------|------|
| 新建文件 | 45 ~ 70 |
| 修改文件 | 50 ~ 80 |
| 删除文件 | 15 ~ 25 |
| 新增代码 | ~6,000 ~ 9,000 行 |
| 净新增逻辑 | ~3,000 ~ 5,000 行 |
| 基本不动业务页 | ~65 个（约 68%） |

### 人力与排期

| 角色 | 投入 |
|------|------|
| 前端 | 28 ~ 35 人日 |
| 后端 | 6 ~ 8 人日 |
| 运维 | 3 ~ 5 人日 |
| QA | 5 ~ 8 人日 |
| **合计** | **35 ~ 48 人日（约 6~8 周）** |

---

## 7. 风险与回滚

| 风险 | 缓解 | 回滚 |
|------|------|------|
| Cookie SSO 破坏现有客户端 | Gateway 双轨 Bearer + Cookie | 关 Cookie 开关 |
| 路由去前缀书签失效 | Nginx 301 旧路径（保留 1 版本） | 恢复 `/admin` 前缀 |
| chunk 加载失败 | `asset-manifest` + echarts 合并 | 回旧 vite 配置 |
| 双轨期冲突 | P0~P3 保持 `apps/*` 可运行 | 保留 `apps/*` 分支 |

---

## 8. 与现有文档的关系

| 文档 | 关系 |
|------|------|
| `campus-life-backend/docs/admin-architecture-design-analysis.md` | 曾推荐「单 SPA + 角色权限」；方案 C 在**物理上**保留三构建，**逻辑上**采纳统一守卫与角色模型 |
| `deploy/docker/L1_DEPLOYMENT_TROUBLESHOOTING_CN.md` | 三端 Nginx、echarts chunk 等问题，PR-2/PR-8 需对齐 |
| `campus-life-ai/docs/llm-gateway-fullstack-design.md` | AI 三端 API 路径不变，前端仅改门户路由前缀 |
| `docs/技术架构文档.md` | 全局技术栈；本文档专注前端三端门户演进 |

---

## 9. 实施进度记录

| 日期 | PR | 说明 |
|------|-----|------|
| 2026-06-08 | PR-1 | `@campus/runtime` 骨架合并至 `dev`；`npm run test:runtime` 11 tests passed；未接业务，不影响现有三端运行 |

**下一步：** PR-2 — 构建配置 + portal shell 本地可启动。

---

## 10. 附录：PR-1 已交付文件清单

```
campus-life-frontend/
├── package.json              # workspaces + test:runtime
├── package-lock.json
└── packages/runtime/
    ├── package.json
    ├── tsconfig.json
    ├── vitest.config.ts
    └── src/
        ├── index.ts
        ├── createCampusApp.ts
        ├── portal/
        │   ├── types.ts
        │   ├── registry.ts
        │   ├── navigate.ts
        │   ├── usePortal.ts
        │   ├── registry.test.ts
        │   └── navigate.test.ts
        ├── router/
        │   ├── createPortalRouter.ts
        │   └── guards.ts
        ├── auth/session.ts
        └── http/client.ts
```

---

*本文档随 PR-2 ~ PR-8 推进持续更新「§9 实施进度记录」。*
