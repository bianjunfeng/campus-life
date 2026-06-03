# 本地 Elasticsearch 验证方案

## 目标

在本地单节点环境验证搜索链路，而不是在资源紧张的远程服务器上强行部署 Elasticsearch。

本方案覆盖：

- 本地启动单节点 Elasticsearch
- 后端切到 ES 路径
- 重建帖子/用户索引
- 验证搜索健康状态
- 用同一套 JMeter 计划对比 MySQL fallback 和 ES 路径

## 适用前提

- Windows 开发环境
- JDK 17 已可用
- Maven 已可用
- 后端项目路径：`D:\code1\code\campus-life-backend`
- 建议把 Elasticsearch 压缩包解压到：`D:\code1\code\tools`

## 1. 准备本地 Elasticsearch

建议使用单节点、关闭安全认证、低内存运行，仅用于开发验证。

推荐目录结构：

```text
D:\code1\code\tools\elasticsearch-8.x.x
```

项目已提供启动脚本：

[`scripts/start-local-es.ps1`](/D:/code1/code/campus-life-backend/scripts/start-local-es.ps1)

示例命令：

```powershell
pwsh .\scripts\start-local-es.ps1 -HeapMb 512 -Port 9200
```

脚本行为：

- 自动查找 `D:\code1\code\tools\elasticsearch-*`
- 以单节点模式启动
- 关闭 `xpack.security`
- 将数据目录写到 `D:\code1\code\var\elasticsearch\data`
- 将 ES 自身日志写到 `D:\code1\code\var\elasticsearch\logs`
- 将启动输出写到 `D:\code1\code\logs\local-es`

启动成功后，访问：

```text
http://127.0.0.1:9200
```

## 2. 切换后端到本地 ES 路径

项目新增了本地 ES profile：

[`application-local-es.yml`](/D:/code1/code/campus-life-backend/src/main/resources/application-local-es.yml)

该 profile 会覆盖两项关键配置：

- `spring.data.elasticsearch.repositories.enabled=true`
- `search.es.enabled=true`

启动后端时使用：

```powershell
mvn "-Dmaven.repo.local=D:/code1/code/campus-life-backend/target/.m2repo" "-Dspring-boot.run.profiles=dev,local-es" spring-boot:run
```

如果本地 ES 不在 `9200`，可以覆盖：

```powershell
$env:ES_URIS="http://127.0.0.1:9201"
mvn "-Dmaven.repo.local=D:/code1/code/campus-life-backend/target/.m2repo" "-Dspring-boot.run.profiles=dev,local-es" spring-boot:run
```

## 3. 验证是否真的走到 ES 链路

先看健康接口：

```text
GET http://127.0.0.1:8080/api/search/health
```

期望至少看到：

- `esEnabled=true`
- `repositoryAvailable=true`
- `esStatus=UP` 或 `DEGRADED`

注意：

- 如果 `search.es.enabled=true` 但索引还没建，搜索仍可能退回 MySQL
- 所以还需要手动执行索引重建

## 4. 重建索引

管理员接口：

- `POST /api/admin/search/posts/reindex`
- `POST /api/admin/search/users/reindex`

推荐顺序：

1. 管理员登录获取 token
2. 调帖子重建接口
3. 调用户重建接口
4. 再次查看 `/api/search/health`

示例：

```powershell
$loginBody = @{
  phone = "13800000001"
  password = "123456"
} | ConvertTo-Json

$login = Invoke-RestMethod `
  -Uri "http://127.0.0.1:8080/api/auth/tokens" `
  -Method Post `
  -ContentType "application/json" `
  -Body $loginBody

$token = $login.data.accessToken
$headers = @{ Authorization = "Bearer $token" }

Invoke-RestMethod `
  -Uri "http://127.0.0.1:8080/api/admin/search/posts/reindex" `
  -Method Post `
  -Headers $headers

Invoke-RestMethod `
  -Uri "http://127.0.0.1:8080/api/admin/search/users/reindex" `
  -Method Post `
  -Headers $headers
```

如果重建成功，期望看到：

- `indexExists=true`
- `documentCount>0`
- `postSearchMode=elasticsearch`
- `searchMode=elasticsearch`

## 5. 压测对比

项目里已有 JMeter 测试计划：

[`search-posts-load-test.jmx`](/D:/code1/code/perf/search-posts-load-test.jmx)

建议对比同一接口、同一关键字、同一并发参数：

- 场景 A：`dev`，即 MySQL fallback
- 场景 B：`dev,local-es`，即本地 ES 路径

示例：

```powershell
& 'D:\Code\Code_App\apache-jmeter-5.6.3\bin\jmeter.bat' `
  -n `
  -t D:\code1\code\perf\search-posts-load-test.jmx `
  -Jthreads=20 `
  -Jloops=50 `
  -Jrampup=10 `
  -Jkeyword=校园 `
  -JresultsFile=D:/code1/code/perf/results/search-posts-es.jtl `
  -l D:/code1/code/perf/results/search-posts-es.jtl `
  -j D:/code1/code/perf/results/search-posts-es.log `
  -e `
  -o D:/code1/code/perf/reports/search-posts-es
```

建议至少记录这些指标：

- 样本数
- 错误率
- 平均响应时间
- P50
- P90
- P95
- P99
- 最大响应时间
- 吞吐量

## 6. 面试和简历口径

本地验证完成后，建议写成：

- 在本地单节点 Elasticsearch 环境下完成帖子/用户搜索链路验证
- 实现索引映射、自定义 analyzer、索引重建、别名切换与 MySQL 降级
- 在开发环境对比 MySQL fallback 与 ES 路径的搜索性能

不建议写成：

- 已部署生产级 ES 集群
- 已完成线上 ES 运维
- 搜索稳定控制在固定毫秒数以内

## 7. 常见问题

### 端口 9200 已被占用

改用其他端口：

```powershell
pwsh .\scripts\start-local-es.ps1 -HeapMb 512 -Port 9201
$env:ES_URIS="http://127.0.0.1:9201"
```

### 后端仍显示 `repositoryAvailable=false`

通常是后端没有带上 `local-es` profile，或者本地 ES 没有成功启动。

### 后端显示 `esEnabled=true`，但搜索仍不是 `elasticsearch`

通常是索引尚未重建，或者重建失败。

### 本地机器内存紧张

先用：

```powershell
pwsh .\scripts\start-local-es.ps1 -HeapMb 512
```

仅做开发验证一般够用；不要把本地单节点结果当成线上容量结论。
