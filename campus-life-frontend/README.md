# 校园生活前端项目

## 项目技术栈

- Vue 3
- Vue Router
- Axios
- Vite

## 运行步骤

### 1. 安装 Node.js

首先需要安装 Node.js 环境，建议安装 LTS 版本（长期支持版）。

#### 下载地址
- 国内地址：[Node.js 中文网](https://nodejs.cn/download/)
- 官方地址：[Node.js 官网](https://nodejs.org/zh-cn/download/)

#### 安装步骤
1. 下载适合 Windows 系统的 .msi 安装包（64位）
2. 双击安装包进行安装
3. 按照安装向导的提示完成安装（建议保持默认设置）
4. 安装完成后，打开命令行工具（cmd 或 PowerShell），输入以下命令验证安装是否成功：
   ```bash
   node -v
   npm -v
   ```
   如果能够显示版本号，则说明安装成功。

### 2. 安装项目依赖

在项目根目录下运行以下命令安装依赖：

```bash
npm install
```

### 3. 运行开发服务器

可选：先复制环境变量文件并按需修改后端地址（默认 `http://localhost:8080`）：

```bash
cp .env.example .env.development
```

如果你的后端跑在其他端口（例如 `8081`），把 `.env.development` 中的 `VITE_API_PROXY_TARGET` 改成对应地址。

依赖安装完成后，按前端入口启动开发服务器：

```bash
npm run dev:consumer
npm run dev:admin
npm run dev:merchant
```

开发端口固定如下，端口被占用时 Vite 会直接报错，不会自动切换到其他端口：

| 前端入口 | 启动命令 | 访问地址 |
| --- | --- | --- |
| 用户端 | `npm run dev` 或 `npm run dev:consumer` | `http://localhost:5173/` |
| 管理端 | `npm run dev:admin` | `http://localhost:5174/` |
| 商家端 | `npm run dev:merchant` | `http://localhost:5175/` |

### 4. 构建生产版本

如果需要构建生产版本，可以运行以下命令：

```bash
npm run build
```

构建完成后，会在上级工作区的 `dist/` 目录下生成三个入口的生产代码：

- `dist/consumer-web`
- `dist/merchant-web`
- `dist/admin-web`

## 项目结构

```
├── src/
│   ├── api/          # API 接口
│   ├── assets/       # 静态资源
│   ├── components/   # 公共组件
│   ├── router/       # 路由配置
│   ├── types/        # 类型定义
│   ├── utils/        # 工具函数
│   ├── views/        # 页面组件
│   ├── App.vue       # 根组件
│   └── main.js       # 入口文件
├── index.html        # HTML 模板
├── package.json      # 项目配置
├── vite.config.js    # Vite 配置
└── README.md         # 项目说明
```
