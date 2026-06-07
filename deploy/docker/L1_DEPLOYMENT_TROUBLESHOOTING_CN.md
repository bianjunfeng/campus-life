# L1 云服务器部署问题复盘

本文档记录 `campus-life` L1 版本在云服务器部署过程中实际遇到的问题、判断依据、处理方案和复查命令。适用于单机 Docker Compose 部署：

- 前端三端：用户端、商家端、管理端
- 后端服务：gateway、backend、ai
- 基础组件：MySQL、Redis、RabbitMQ
- L1 不启用 Elasticsearch、Kafka、Milvus 等完整检索/向量栈

## 一、最终推荐部署形态

L1 推荐使用 Docker Compose 承载业务和基础组件，避免继续混用历史裸机 Java 进程。

```text
DNS
  life.bianjunfeng.online      -> 49.232.59.230
  merchant.bianjunfeng.online  -> 49.232.59.230
  admin.bianjunfeng.online     -> 49.232.59.230

公网 80/443
  -> Docker frontend/nginx
  -> /api 转发到 gateway
  -> gateway 转发到 backend/ai
  -> MySQL/Redis/RabbitMQ 仅容器内或 127.0.0.1 暴露
```

HTTP 阶段 `.env` 建议：

```dotenv
HTTP_PORT=80
CONSUMER_SERVER_NAME=life.bianjunfeng.online
MERCHANT_SERVER_NAME=merchant.bianjunfeng.online
ADMIN_SERVER_NAME=admin.bianjunfeng.online
DEMO_PUBLIC_BASE_URL=http://life.bianjunfeng.online
```

启用 HTTPS 后，建议让宿主机 Caddy/Nginx 占用 80/443，Docker 前端改为内网端口：

```dotenv
HTTP_PORT=8088
DEMO_PUBLIC_BASE_URL=https://life.bianjunfeng.online
```

Caddy 示例：

```caddyfile
life.bianjunfeng.online {
  reverse_proxy 127.0.0.1:8088
}

merchant.bianjunfeng.online {
  reverse_proxy 127.0.0.1:8088
}

admin.bianjunfeng.online {
  reverse_proxy 127.0.0.1:8088
}
```

## 二、问题与解决方案总表

| 序号 | 问题现象 | 根因判断 | 解决方案 | 复查命令 |
| --- | --- | --- | --- | --- |
| 1 | 服务器上已有 `/opt/campus-life`、systemd 服务、Java 进程、旧 Nginx 配置 | 历史裸机部署和新 Docker 部署争抢端口、配置和运行目录 | 停止并禁用 `campus-life-*` systemd 服务，清理旧 Java 进程，移除旧 Nginx/Caddy 站点配置，只保留新 L1 Compose 栈 | `systemctl list-units --type=service \| grep -i campus`，`pgrep -af "/opt/campus-life"`，`ss -ltnp \| grep -E ':80\|:443'` |
| 2 | 服务器已有多个旧容器，例如旧 MySQL、Redis、Elasticsearch、RabbitMQ、其它项目容器 | 旧容器占用端口和内存，容易连接到错误实例 | 停掉无关容器；L1 只保留本项目 Compose 创建的 `campus-life-*` 容器 | `docker ps`，`docker stats --no-stream` |
| 3 | `git clone` 报 `HTTP/2 stream ... CANCEL`、`early EOF`、`GnuTLS recv error` | GitHub 到服务器网络不稳定，HTTP/2/TLS 长连接中断 | 切换 Git HTTP/1.1，浅克隆、单分支、部分克隆、稀疏检出；必要时多次重试 | `git config --global http.version HTTP/1.1`，`git clone --depth 1 --single-branch --filter=blob:none --sparse ...` |
| 4 | `wget codeload.github.com` 下载 zip 后 `End-of-central-directory signature not found` | 下载过程中连接中断，得到的是不完整 zip | 不依赖一次性 zip；优先使用浅克隆和稀疏检出；若必须 zip，下载后先校验文件完整性再解压 | `file campus-life.zip`，`unzip -t campus-life.zip` |
| 5 | `git sparse-checkout set ... .dockerignore` 报 `.dockerignore is not a directory` | 稀疏检出默认按目录规则校验，文件路径被当成目录 | `sparse-checkout set` 只放目录，或按 Git 提示使用 `--skip-checks` 处理文件 | `git sparse-checkout list` |
| 6 | `./scripts/validate-env.sh` 报 `Permission denied` | 脚本没有执行权限 | 给部署脚本加执行权限 | `chmod +x scripts/*.sh` |
| 7 | 脚本报 `/usr/bin/env: 'bash\r': No such file or directory` | 文件是 Windows CRLF 换行，Linux 识别 shebang 失败 | 转换脚本和 `.env` 为 LF | `find deploy -type f -name "*.sh" -exec sed -i 's/\r$//' {} \;`，`sed -i 's/\r$//' deploy/docker/.env` |
| 8 | Docker build 报 `short read ... unexpected EOF`，拉镜像时连接中断 | Docker Hub 或网络链路不稳定，镜像层/构建缓存不完整 | 检查磁盘和 inode；清理构建缓存；基础镜像逐个 `docker pull`；必要时配置镜像加速源 | `df -h`，`df -i`，`docker system df`，`docker builder prune` |
| 9 | `campus-life-mysql-1 is unhealthy` | MySQL 初始化脚本执行方式或初始化数据不完整导致容器健康检查失败 | 确认 init 脚本为 LF；按项目要求调整脚本权限；需要重建数据时执行 `docker compose down -v` 后重新启动 | `docker compose logs --tail=120 mysql`，`docker compose ps mysql` |
| 10 | backend 反复重启或 unhealthy | L1 不启用 Elasticsearch，但后端仍扫描/尝试 ES 仓库；也可能有非 L1 必需监听器启动 | Docker 配置中关闭 ES repository，按 L1 关闭非必要 listener 或依赖 | `docker compose logs --tail=120 backend`，`curl -s http://127.0.0.1/api/actuator/health` |
| 11 | ai health 返回 `503` 或 `{"status":"DOWN"}`，详情显示 Redis DOWN | ai 容器内未正确使用 Redis host/password，或启动早于 Redis 就绪 | 给 ai 显式注入 `SPRING_DATA_REDIS_HOST=redis`、`SPRING_DATA_REDIS_PORT=6379`、`SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}`；重启 ai | `docker compose exec ai sh -c 'curl -s http://127.0.0.1:8083/actuator/health; echo'` |
| 12 | `curl -I http://127.0.0.1` 或带 Host 访问返回 `403 Forbidden` | Nginx `try_files $uri $uri/ ...` 命中了目录，目录无索引导致 403 | 前端 Nginx 配置移除 `$uri/`，为 `/` 添加精确 fallback 到对应 `index.html` | `curl -I -H "Host: life.bianjunfeng.online" http://127.0.0.1/` |
| 13 | merchant 可打开，life 页面打不开或资源路径异常 | 三端构建目录和 Nginx alias/root 不匹配；Vite base 下的 assets 映射不完整 | 按实际产物 `/usr/share/nginx/<web>/apps/<web>/index.html` 与 `/assets` 配置三端 fallback 和静态资源 alias | `docker compose exec frontend find /usr/share/nginx -name index.html` |
| 14 | admin 登录后报 `TypeError: no is not a function`，堆栈在 `echarts-vendor`、`zrender-vendor` | Vite manualChunks 把 `echarts` 和 `zrender` 拆成两个 vendor chunk，产生循环依赖/初始化顺序问题 | 将 `echarts` 和 `zrender` 合并到同一个 `charts-vendor` chunk，重建 frontend | `docker compose exec frontend ls /usr/share/nginx/admin-web/assets \| grep -E 'echarts\|zrender\|charts'` |
| 15 | `https://merchant...` 或 `https://life...` 打不开 | 当前只配置了 HTTP，DNS 指向服务器不等于自动启用 HTTPS | HTTP 阶段使用 `http://...`；若要 HTTPS，配置证书和 80/443 反向代理 | `curl -I http://merchant.bianjunfeng.online/`，`curl -vkI https://merchant.bianjunfeng.online/` |
| 16 | `life.bianjunfeng.online 当前无法处理此请求 / HTTP ERROR 502` | 如果服务器本机 HTTP Host curl 是 200，则 502 多半来自 HTTPS、旧宿主机代理、CDN、浏览器缓存或 443 入口 | 查 80/443 监听者，确认是否还有旧 Nginx/Caddy；用 HTTP 验证；清浏览器缓存；正式启用 HTTPS 时统一反代到 Docker 前端 | `ss -ltnp \| grep -E ':80\|:443'`，`curl -I -H "Host: life.bianjunfeng.online" http://127.0.0.1/` |
| 17 | DNS 配好后仍访问异常 | 解析未生效、只配了部分子域名、浏览器缓存旧协议 | 三个子域都建 A 记录指向 `49.232.59.230`，等待 TTL，分别用 DNS 和 Host header 验证 | `dig life.bianjunfeng.online +short`，`curl -I -H "Host: admin.bianjunfeng.online" http://127.0.0.1/` |
| 18 | 修复前端后浏览器仍报旧 JS 错误 | 浏览器或边缘缓存仍加载旧 chunk | hard reload、清站点数据或无痕窗口；HTML 设置 `Cache-Control: no-store`，assets 用带 hash 文件名长期缓存 | 浏览器 DevTools Network 检查 JS 文件名 |

## 三、关键问题详细记录

### 1. 旧裸机部署痕迹

服务器上出现过如下旧进程和服务：

```text
campus-life-ai.service
campus-life-backend.service
campus-life-gateway.service
/usr/bin/java -jar /opt/campus-life/...
```

同时旧 Nginx 配置中仍引用：

```text
/opt/campus-life/frontend/consumer-web
/opt/campus-life/frontend/merchant-web
/opt/campus-life/frontend/admin-web
```

处理原则：

- 新旧部署不要混跑。
- systemd 裸机 Java 服务全部停止并禁用。
- 宿主机 Nginx/Caddy 如果未用于 HTTPS 反代，应停止或移除旧站点。
- 若后续启用 HTTPS，宿主机 Nginx/Caddy 只做反向代理，不再直接读 `/opt/campus-life/frontend` 静态文件。

常用排查命令：

```bash
systemctl list-units --type=service | grep -i campus
pgrep -af "/opt/campus-life"
sudo grep -R "campus-life" /etc/nginx /etc/caddy 2>/dev/null
sudo ss -ltnp | grep -E ':80|:443'
```

### 2. GitHub 拉取不稳定

服务器多次出现：

```text
RPC failed; curl 92 HTTP/2 stream ... was not closed cleanly
fatal: early EOF
GnuTLS recv error
```

稳定性更好的拉取方式：

```bash
cd /opt
rm -rf campus-life-l1

git config --global http.version HTTP/1.1
git config --global http.lowSpeedLimit 0
git config --global http.lowSpeedTime 999999

git clone --depth 1 --single-branch --filter=blob:none --sparse \
  https://github.com/bianjunfeng/campus-life.git campus-life-l1

cd /opt/campus-life-l1
git sparse-checkout set \
  campus-life-backend \
  campus-life-ai \
  campus-life-gateway \
  campus-life-frontend \
  deploy
```

注意：不要把 `.dockerignore` 直接放入上述目录模式中，否则可能触发：

```text
fatal: '.dockerignore' is not a directory
```

### 3. `.env` 与域名

三端建议使用独立子域名：

```dotenv
CONSUMER_SERVER_NAME=life.bianjunfeng.online
MERCHANT_SERVER_NAME=merchant.bianjunfeng.online
ADMIN_SERVER_NAME=admin.bianjunfeng.online
DEMO_PUBLIC_BASE_URL=http://life.bianjunfeng.online
```

DNS 侧需要三条 A 记录：

```text
life      A  49.232.59.230
merchant  A  49.232.59.230
admin     A  49.232.59.230
```

强密码生成示例：

```bash
openssl rand -base64 32
```

安全注意：

- `.env` 不要提交到 Git。
- 已经在聊天、截图、日志或公开环境中暴露过的密码、JWT、API secret，应视为已泄露并轮换。
- MySQL、Redis、RabbitMQ 密码改变后，如果对应 volume 已初始化过，通常需要同步更新已有实例密码，或确认可以接受数据重建后执行 `docker compose down -v` 重新初始化。

### 4. 脚本权限和换行

部署脚本先后出现：

```text
Permission denied
/usr/bin/env: 'bash\r': No such file or directory
```

修复命令：

```bash
cd /opt/campus-life-l1/deploy/docker
chmod +x scripts/*.sh

cd /opt/campus-life-l1
find deploy -type f -name "*.sh" -exec sed -i 's/\r$//' {} \;
sed -i 's/\r$//' deploy/docker/.env
```

复查：

```bash
cd /opt/campus-life-l1/deploy/docker
./scripts/validate-env.sh
```

### 5. Docker build 和镜像拉取中断

出现过：

```text
failed to compute cache key: short read
unexpected EOF
Network error: Software caused connection abort
```

先确认不是磁盘问题：

```bash
df -h
df -i
sudo docker system df
```

若磁盘充足，按网络/缓存问题处理：

```bash
sudo docker builder prune

sudo docker pull maven:3.9-eclipse-temurin-17
sudo docker pull eclipse-temurin:17-jre
sudo docker pull node:20-alpine
sudo docker pull nginx:1.27-alpine
sudo docker pull mysql:8.0
sudo docker pull redis:7-alpine
sudo docker pull rabbitmq:3-management
```

如果 Docker Hub 长期不稳定，改用云厂商镜像加速源或自建 registry mirror。

### 6. MySQL unhealthy

现象：

```text
dependency failed to start: container campus-life-mysql-1 is unhealthy
```

排查：

```bash
cd /opt/campus-life-l1/deploy/docker
sudo docker compose logs --tail=120 mysql
sudo docker compose ps mysql
```

处理要点：

- 确认 init SQL 和 shell 脚本为 LF。
- 确认初始化脚本权限符合项目当前 Docker 配置要求。
- 如果首次初始化失败且可以清空演示数据，执行：

```bash
sudo docker compose down -v
sudo docker compose up -d mysql
```

注意：`down -v` 会删除 Compose volume，生产数据不可直接执行。

### 7. Backend unhealthy

现象：

```text
dependency failed to start: container campus-life-backend-1 is unhealthy
```

L1 不部署 Elasticsearch，因此 backend 需要关闭 ES repository 或完整检索相关能力。启动参数或配置中应包含：

```properties
spring.data.elasticsearch.repositories.enabled=false
```

排查：

```bash
sudo docker compose logs --tail=160 backend
sudo docker compose exec backend sh -c 'curl -s http://127.0.0.1:8080/actuator/health; echo'
```

### 8. AI health Redis DOWN

现象：

```json
{"status":"DOWN","components":{"redis":{"status":"DOWN"}}}
```

容器内 Redis DNS 和密码验证：

```bash
sudo docker compose exec ai sh -c 'env | grep SPRING_DATA_REDIS'
sudo docker compose exec ai sh -c 'getent hosts redis'
sudo docker compose exec redis sh -c 'redis-cli -a "$REDIS_PASSWORD" ping'
```

Compose 中需要给 ai 注入：

```yaml
SPRING_DATA_REDIS_HOST: redis
SPRING_DATA_REDIS_PORT: 6379
SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
```

修复后复查：

```bash
sudo docker compose exec ai sh -c 'curl -s http://127.0.0.1:8083/actuator/health; echo'
```

期望：

```json
{"status":"UP"}
```

### 9. 前端 403

现象：

```bash
curl -I -H "Host: life.bianjunfeng.online" http://127.0.0.1
```

返回：

```text
HTTP/1.1 403 Forbidden
```

实际前端产物路径：

```text
/usr/share/nginx/admin-web/apps/admin-web/index.html
/usr/share/nginx/merchant-web/apps/merchant-web/index.html
/usr/share/nginx/consumer-web/apps/consumer-web/index.html
```

根因是 Nginx fallback 命中了目录。修复原则：

- HTML fallback 精确指向对应 `index.html`。
- `try_files` 不再使用 `$uri/`。
- 三端 assets 分别映射到实际 assets 目录。

复查：

```bash
curl -I -H "Host: life.bianjunfeng.online" http://127.0.0.1/
curl -I -H "Host: merchant.bianjunfeng.online" http://127.0.0.1/merchant/
curl -I -H "Host: admin.bianjunfeng.online" http://127.0.0.1/admin/
```

期望均为 200 或正常前端响应。

### 10. Admin 登录后 `no is not a function`

现象：

```text
Failed to load resource: the server responded with a status of 401
TypeError: no is not a function
at echarts-vendor-...
at zrender-vendor-...
登录失败: TypeError: no is not a function
```

判断：

- 登录接口本身已经进入前端流程。
- 报错堆栈集中在 `echarts-vendor` 和 `zrender-vendor`。
- Vite manualChunks 将 `echarts` 与 `zrender` 拆成两个 vendor chunk 后存在循环依赖和初始化顺序问题。

修复：

```js
if (id.includes('node_modules/echarts') || id.includes('node_modules/zrender')) {
  return 'charts-vendor'
}
```

重建：

```bash
cd /opt/campus-life-l1/deploy/docker
sudo docker compose build frontend
sudo docker compose up -d frontend
```

复查：

```bash
sudo docker compose exec frontend sh -c \
  "find /usr/share/nginx/admin-web/assets -maxdepth 1 -type f | grep -E 'echarts|zrender|charts' || true"
```

期望只看到 `charts-vendor-*.js`，不再单独出现 `echarts-vendor-*.js`、`zrender-vendor-*.js`。

浏览器侧建议 hard reload、清站点数据或使用无痕窗口，避免继续加载旧 chunk。

### 11. HTTPS 和 502

现象：

```text
https://merchant.bianjunfeng.online/ 打不开
life.bianjunfeng.online 当前无法处理此请求
HTTP ERROR 502
```

判断方式：

```bash
curl -I -H "Host: life.bianjunfeng.online" http://127.0.0.1/
curl -I http://life.bianjunfeng.online/
curl -vkI https://life.bianjunfeng.online/
sudo ss -ltnp | grep -E ':80|:443'
sudo systemctl status nginx caddy --no-pager || true
```

如果服务器本机 HTTP Host header 访问为 200，而浏览器 HTTPS 为 502，则问题通常不在 Docker frontend，而在：

- 当前未配置 HTTPS。
- 443 被旧 Nginx/Caddy 或其它服务占用。
- CDN、云解析代理或浏览器缓存仍指向旧入口。
- 访问时浏览器自动升级到 HTTPS。

处理：

- 未配置 HTTPS 前，明确使用 `http://life.bianjunfeng.online/`。
- 配置 HTTPS 时，宿主机反代监听 80/443，Docker frontend 改监听 `127.0.0.1:8088` 或宿主机 `8088`。
- 清理旧 443 配置，确保所有域名反代到同一个 Docker frontend 入口。

## 四、标准启动与验证流程

每次修改配置或代码后建议按以下顺序验证：

```bash
cd /opt/campus-life-l1/deploy/docker

./scripts/validate-env.sh
sudo docker compose config >/tmp/campus-life-compose.yml

sudo docker compose up -d --build
sudo docker compose ps
```

基础组件：

```bash
sudo docker compose exec redis sh -c 'redis-cli -a "$REDIS_PASSWORD" ping'
sudo docker compose exec mysql sh -c 'mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD"'
```

服务健康：

```bash
curl -s http://127.0.0.1/api/actuator/health
sudo docker compose exec ai sh -c 'curl -s http://127.0.0.1:8083/actuator/health; echo'
sudo docker compose exec backend sh -c 'curl -s http://127.0.0.1:8080/actuator/health; echo'
```

三端入口：

```bash
curl -I -H "Host: life.bianjunfeng.online" http://127.0.0.1/
curl -I -H "Host: merchant.bianjunfeng.online" http://127.0.0.1/
curl -I -H "Host: admin.bianjunfeng.online" http://127.0.0.1/
```

外网 DNS：

```bash
dig life.bianjunfeng.online +short
dig merchant.bianjunfeng.online +short
dig admin.bianjunfeng.online +short
```

运行日志：

```bash
sudo docker compose logs --tail=120 frontend
sudo docker compose logs --tail=120 gateway
sudo docker compose logs --tail=120 backend
sudo docker compose logs --tail=120 ai
```

## 五、后续建议

1. 先稳定 HTTP 三端访问和登录流程，再启用 HTTPS。
2. HTTPS 启用后统一改 `DEMO_PUBLIC_BASE_URL=https://life.bianjunfeng.online`，并更新支付回调 URL。
3. 对已经暴露过的 `.env` 密码、JWT secret、AI secret 做一次完整轮换。
4. 给服务器保留至少 1 GB 空闲内存；若同机还有 Elasticsearch 或其它项目，L1 服务容易被挤压。
5. 部署完成后保留 `docker compose ps`、三端 `curl -I`、backend/ai health 输出作为验收记录。
