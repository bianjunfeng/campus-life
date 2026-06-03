<template>
  <div class="admin-ai">
    <div class="page-header">
      <div>
        <h1 class="page-title">AI 管理</h1>
        <p class="page-description">维护模型提供者、能力开关、场景配置，并查看 AI 调用统计与最近日志。</p>
      </div>
      <div class="header-actions">
        <RouterLink class="btn btn-secondary" to="/admin/ai/agents">
          Agent 中心
        </RouterLink>
        <button class="btn btn-secondary" :disabled="loading" @click="loadAllData">
          {{ loading ? '刷新中...' : '刷新数据' }}
        </button>
      </div>
    </div>

    <div class="overview-grid">
      <div class="overview-card">
        <span class="overview-label">总调用量</span>
        <strong class="overview-value">{{ overview.totalCalls }}</strong>
      </div>
      <div class="overview-card success">
        <span class="overview-label">成功调用</span>
        <strong class="overview-value">{{ overview.successCalls }}</strong>
      </div>
      <div class="overview-card danger">
        <span class="overview-label">失败调用</span>
        <strong class="overview-value">{{ overview.failedCalls }}</strong>
      </div>
      <div class="overview-card">
        <span class="overview-label">累计 Tokens</span>
        <strong class="overview-value">{{ formatNumber(overview.totalTokens) }}</strong>
      </div>
      <div class="overview-card">
        <span class="overview-label">累计成本</span>
        <strong class="overview-value">{{ formatMoney(overview.totalCostAmount) }}</strong>
      </div>
      <div class="overview-card">
        <span class="overview-label">平均耗时</span>
        <strong class="overview-value">{{ overview.avgLatencyMs }} ms</strong>
      </div>
      <div class="overview-card">
        <span class="overview-label">当前配置概况</span>
        <strong class="overview-value">{{ providers.length }}/{{ models.length }}/{{ routeRules.length }}/{{ quotas.length }}</strong>
        <span class="overview-footnote">提供者 / 模型 / 路由 / 配额</span>
      </div>
    </div>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-button"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <section v-if="activeTab === 'overview'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">运行总览</h2>
          <p class="panel-description">这里展示当前 AI 管理配置和最近调用情况。</p>
        </div>
      </div>

      <div class="summary-grid">
        <article class="summary-card">
          <h3>提供者状态</h3>
          <p class="summary-main">{{ enabledProvidersCount }} / {{ providers.length }}</p>
          <p class="summary-sub">已启用提供者</p>
        </article>
        <article class="summary-card">
          <h3>能力状态</h3>
          <p class="summary-main">{{ enabledCapabilitiesCount }} / {{ capabilities.length }}</p>
          <p class="summary-sub">已启用能力</p>
        </article>
        <article class="summary-card">
          <h3>模型状态</h3>
          <p class="summary-main">{{ enabledModelsCount }} / {{ models.length }}</p>
          <p class="summary-sub">已启用模型</p>
        </article>
        <article class="summary-card">
          <h3>路由状态</h3>
          <p class="summary-main">{{ enabledRouteRulesCount }} / {{ routeRules.length }}</p>
          <p class="summary-sub">已启用路由规则</p>
        </article>
        <article class="summary-card">
          <h3>配额状态</h3>
          <p class="summary-main">{{ enabledQuotasCount }} / {{ quotas.length }}</p>
          <p class="summary-sub">已启用限额策略</p>
        </article>
        <article class="summary-card">
          <h3>场景状态</h3>
          <p class="summary-main">{{ enabledScenesCount }} / {{ scenes.length }}</p>
          <p class="summary-sub">已启用场景</p>
        </article>
      </div>

      <div class="split-grid">
        <div class="card-panel">
          <div class="section-head">
            <h3>已配置提供者</h3>
            <button class="link-button" @click="activeTab = 'providers'">前往管理</button>
          </div>
          <div v-if="providers.length" class="tag-list">
            <span
              v-for="provider in providers"
              :key="provider.id"
              class="status-tag"
              :class="{ enabled: provider.enabled, disabled: !provider.enabled }"
            >
              {{ provider.providerName }} / {{ provider.defaultModelCode }}
            </span>
          </div>
          <div v-else class="empty-state">暂无 Provider 配置。</div>
        </div>

        <div class="card-panel">
          <div class="section-head">
            <h3>最近调用日志</h3>
            <button class="link-button" @click="activeTab = 'logs'">查看全部</button>
          </div>
          <div v-if="logs.length" class="mini-log-list">
            <div v-for="log in logs.slice(0, 6)" :key="log.id" class="mini-log-item">
              <span class="mini-log-time">{{ formatTime(log.createdAt) }}</span>
              <span class="mini-log-scene">{{ log.sceneCode }}</span>
              <span class="mini-log-result" :class="{ success: log.success === 1, failed: log.success !== 1 }">
                {{ log.success === 1 ? '成功' : '失败' }}
              </span>
            </div>
          </div>
          <div v-else class="empty-state">暂无调用日志。</div>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === 'providers'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">Provider 管理</h2>
          <p class="panel-description">运行时聊天会优先读取这里的配置，再回退到环境变量。</p>
        </div>
        <button class="btn" @click="openCreateProvider">新增 Provider</button>
      </div>

      <div class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>编码</th>
              <th>名称</th>
              <th>默认模型</th>
              <th>地址</th>
              <th>上下文</th>
              <th>温度</th>
              <th>API Key</th>
              <th>状态</th>
              <th>健康</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="provider in providers" :key="provider.id">
              <td class="mono">{{ provider.providerCode }}</td>
              <td>{{ provider.providerName }}</td>
              <td class="mono">{{ provider.defaultModelCode }}</td>
              <td class="truncate-cell" :title="provider.baseUrl">{{ provider.baseUrl }}</td>
              <td>{{ provider.maxContextMessages ?? '-' }}</td>
              <td>{{ provider.temperature ?? '-' }}</td>
              <td>{{ provider.hasApiKey ? '已配置' : '未配置' }}</td>
              <td>
                <span class="status-pill" :class="{ active: provider.enabled, inactive: !provider.enabled }">
                  {{ provider.enabled ? '启用' : '停用' }}
                </span>
              </td>
              <td>
                <span
                  v-if="providerHealthChecks[provider.id]"
                  class="status-pill"
                  :class="{
                    active: providerHealthChecks[provider.id].available,
                    inactive: !providerHealthChecks[provider.id].available
                  }"
                  :title="providerHealthChecks[provider.id].message"
                >
                  {{ providerHealthChecks[provider.id].available ? '可用' : '不可用' }}
                </span>
                <span v-else>-</span>
              </td>
              <td>{{ formatTime(provider.updatedAt) }}</td>
              <td class="action-cell">
                <button
                  class="text-button"
                  :disabled="providerCheckingId === provider.id"
                  @click="checkProviderHealth(provider)"
                >
                  {{ providerCheckingId === provider.id ? '检查中' : '检查' }}
                </button>
                <button class="text-button" @click="openEditProvider(provider)">编辑</button>
                <button class="text-button danger" @click="removeProvider(provider)">删除</button>
              </td>
            </tr>
            <tr v-if="!providers.length">
              <td colspan="11" class="empty-row">暂无 Provider 配置</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else-if="activeTab === 'testBench'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">网关测试台</h2>
          <p class="panel-description">按当前路由、限流、配额、日志和成本链路发起一次管理端测试请求。</p>
        </div>
        <button class="btn" :disabled="probeLoading" @click="runGatewayProbe">
          {{ probeLoading ? '测试中...' : '运行测试' }}
        </button>
      </div>

      <div class="probe-layout">
        <div class="card-panel">
          <div class="form-grid">
            <label class="form-item span-2 textarea-item">
              <span>测试内容</span>
              <textarea v-model="probeForm.content" rows="5" maxlength="2000" />
            </label>
            <label class="form-item">
              <span>能力</span>
              <select v-model="probeForm.capabilityCode">
                <option value="">默认</option>
                <option v-for="capability in capabilities" :key="capability.id" :value="capability.capabilityCode">
                  {{ capability.capabilityCode }}
                </option>
              </select>
            </label>
            <label class="form-item">
              <span>场景</span>
              <select v-model="probeForm.sceneCode">
                <option value="">默认</option>
                <option v-for="scene in scenes" :key="scene.id" :value="scene.sceneCode">
                  {{ scene.sceneCode }}
                </option>
              </select>
            </label>
            <label class="form-item">
              <span>Provider</span>
              <select v-model="probeForm.providerCode">
                <option value="">按路由选择</option>
                <option v-for="provider in providers" :key="provider.id" :value="provider.providerCode">
                  {{ provider.providerCode }}
                </option>
              </select>
            </label>
            <label class="form-item">
              <span>模型</span>
              <select v-model="probeForm.modelCode">
                <option value="">按路由选择</option>
                <option v-for="model in models" :key="model.id" :value="model.modelCode">
                  {{ model.providerCode }} / {{ model.modelCode }}
                </option>
              </select>
            </label>
            <label class="form-item">
              <span>Temperature</span>
              <input v-model.number="probeForm.temperature" type="number" min="0" max="2" step="0.1" />
            </label>
            <label class="form-item">
              <span>最大输出 Tokens</span>
              <input v-model.number="probeForm.maxOutputTokens" type="number" min="1" step="1" />
            </label>
          </div>
        </div>

        <div class="card-panel probe-result-panel">
          <div class="section-head compact">
            <h3>测试结果</h3>
            <span
              v-if="probeResult"
              class="status-pill"
              :class="{ active: probeResult.success, inactive: !probeResult.success }"
            >
              {{ probeResult.success ? '成功' : '失败' }}
            </span>
          </div>
          <div v-if="probeResult" class="probe-result">
            <div class="probe-meta-grid">
              <span>Request ID</span>
              <strong class="mono">{{ probeResult.requestId }}</strong>
              <span>Provider</span>
              <strong class="mono">{{ probeResult.providerCode || '-' }}</strong>
              <span>模型</span>
              <strong class="mono">{{ probeResult.modelCode || '-' }}</strong>
              <span>Tokens</span>
              <strong>{{ formatNumber(probeResult.totalTokens) }}</strong>
              <span>成本</span>
              <strong>{{ formatMoney(probeResult.costAmount) }}</strong>
              <span>耗时</span>
              <strong>{{ probeResult.latencyMs ? `${probeResult.latencyMs} ms` : '-' }}</strong>
              <span>Fallback</span>
              <strong>{{ probeResult.fallbackLevel ?? '-' }}</strong>
            </div>
            <pre v-if="probeResult.success" class="probe-output">{{ probeResult.content || '-' }}</pre>
            <div v-else class="probe-error">
              <strong>{{ probeResult.errorCode || 'PROBE_FAILED' }}</strong>
              <span>{{ probeResult.errorType || '-' }}</span>
              <p>{{ probeResult.errorMessage || '网关测试请求失败' }}</p>
            </div>
          </div>
          <div v-else class="empty-state">暂无测试结果</div>
        </div>
      </div>

      <div class="probe-layout moderation-layout">
        <div class="card-panel">
          <div class="section-head compact">
            <h3>文本审核</h3>
            <button class="btn" :disabled="moderationLoading" @click="runModerationCheck">
              {{ moderationLoading ? '审核中...' : '运行审核' }}
            </button>
          </div>
          <div class="form-grid">
            <label class="form-item span-2 textarea-item">
              <span>待审核内容</span>
              <textarea v-model="moderationForm.content" rows="5" maxlength="5000" />
            </label>
            <label class="form-item">
              <span>内容类型</span>
              <select v-model="moderationForm.targetType">
                <option value="post">帖子</option>
                <option value="comment">评论</option>
                <option value="profile">资料</option>
                <option value="text">文本</option>
              </select>
            </label>
            <label class="form-item">
              <span>场景</span>
              <select v-model="moderationForm.sceneCode">
                <option value="">默认审核场景</option>
                <option
                  v-for="scene in moderationScenes"
                  :key="scene.id"
                  :value="scene.sceneCode"
                >
                  {{ scene.sceneCode }}
                </option>
              </select>
            </label>
            <label class="form-item">
              <span>Provider</span>
              <select v-model="moderationForm.providerCode">
                <option value="">按场景选择</option>
                <option v-for="provider in providers" :key="provider.id" :value="provider.providerCode">
                  {{ provider.providerCode }}
                </option>
              </select>
            </label>
            <label class="form-item">
              <span>模型</span>
              <select v-model="moderationForm.modelCode">
                <option value="">按场景选择</option>
                <option v-for="model in models" :key="`moderation-${model.id}`" :value="model.modelCode">
                  {{ model.providerCode }} / {{ model.modelCode }}
                </option>
              </select>
            </label>
          </div>
        </div>

        <div class="card-panel probe-result-panel">
          <div class="section-head compact">
            <h3>审核结果</h3>
            <span
              v-if="moderationResult"
              class="status-pill"
              :class="{
                active: moderationResult.result === 'PASS',
                inactive: moderationResult.result === 'REVIEW',
                danger: moderationResult.result === 'REJECT'
              }"
            >
              {{ moderationResult.result }}
            </span>
          </div>
          <div v-if="moderationResult" class="probe-result">
            <div class="probe-meta-grid">
              <span>Request ID</span>
              <strong class="mono">{{ moderationResult.requestId }}</strong>
              <span>风险分</span>
              <strong>{{ formatModerationScore(moderationResult.score) }}</strong>
              <span>Provider</span>
              <strong class="mono">{{ moderationResult.providerCode || '-' }}</strong>
              <span>模型</span>
              <strong class="mono">{{ moderationResult.modelCode || '-' }}</strong>
              <span>场景</span>
              <strong class="mono">{{ moderationResult.sceneCode || '-' }}</strong>
              <span>耗时</span>
              <strong>{{ moderationResult.latencyMs ? `${moderationResult.latencyMs} ms` : '-' }}</strong>
            </div>
            <div class="moderation-category-list">
              <span
                v-for="category in moderationResult.categories"
                :key="category"
                class="moderation-category"
              >
                {{ category }}
              </span>
            </div>
            <div class="probe-error">
              <strong>原因</strong>
              <p>{{ moderationResult.reason || '-' }}</p>
            </div>
            <pre v-if="moderationResult.rawResponse" class="probe-output">{{ moderationResult.rawResponse }}</pre>
          </div>
          <div v-else class="empty-state">暂无审核结果</div>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === 'models'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">Model 管理</h2>
          <p class="panel-description">维护可参与网关路由的模型、能力和成本信息。</p>
        </div>
        <button class="btn" @click="openCreateModel">新增 Model</button>
      </div>

      <div class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>Provider</th>
              <th>模型编码</th>
              <th>名称</th>
              <th>窗口</th>
              <th>输出</th>
              <th>价格 / 1K</th>
              <th>优先级</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="model in models" :key="model.id">
              <td class="mono">{{ model.providerCode }}</td>
              <td class="mono">{{ model.modelCode }}</td>
              <td>{{ model.modelName }}</td>
              <td>{{ model.contextWindow ?? '-' }}</td>
              <td>{{ model.maxOutputTokens ?? '-' }}</td>
              <td>{{ formatModelPrice(model) }}</td>
              <td>{{ model.priority ?? '-' }}</td>
              <td>
                <span class="status-pill" :class="{ active: model.enabled, inactive: !model.enabled }">
                  {{ model.enabled ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatTime(model.updatedAt) }}</td>
              <td class="action-cell">
                <button class="text-button" @click="openEditModel(model)">编辑</button>
                <button class="text-button danger" @click="removeModel(model)">删除</button>
              </td>
            </tr>
            <tr v-if="!models.length">
              <td colspan="10" class="empty-row">暂无 Model 配置</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else-if="activeTab === 'routeRules'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">Route Rule 管理</h2>
          <p class="panel-description">按能力和场景配置网关候选模型路由。</p>
        </div>
        <button class="btn" @click="openCreateRouteRule">新增 Route Rule</button>
      </div>

      <div class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>规则</th>
              <th>能力</th>
              <th>场景</th>
              <th>路由规则</th>
              <th>优先级</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in routeRules" :key="rule.id">
              <td>{{ rule.ruleName }}</td>
              <td class="mono">{{ rule.capabilityCode }}</td>
              <td class="mono">{{ rule.sceneCode || '-' }}</td>
              <td class="truncate-cell" :title="rule.routeRuleJson">{{ previewText(rule.routeRuleJson) }}</td>
              <td>{{ rule.priority ?? '-' }}</td>
              <td>
                <span class="status-pill" :class="{ active: rule.enabled, inactive: !rule.enabled }">
                  {{ rule.enabled ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatTime(rule.updatedAt) }}</td>
              <td class="action-cell">
                <button class="text-button" @click="openEditRouteRule(rule)">编辑</button>
                <button class="text-button danger" @click="removeRouteRule(rule)">删除</button>
              </td>
            </tr>
            <tr v-if="!routeRules.length">
              <td colspan="8" class="empty-row">暂无 Route Rule 配置</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else-if="activeTab === 'safetyRules'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">安全治理</h2>
          <p class="panel-description">按能力、场景和输入/输出方向配置网关安全拦截规则。</p>
        </div>
        <button class="btn" @click="openCreateSafetyRule">新增安全规则</button>
      </div>

      <div class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>规则</th>
              <th>范围</th>
              <th>方向 / 动作</th>
              <th>匹配</th>
              <th>分类</th>
              <th>等级</th>
              <th>优先级</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in safetyRules" :key="rule.id">
              <td>{{ rule.ruleName }}</td>
              <td>
                <div class="mono">{{ rule.capabilityCode || 'ALL' }}</div>
                <div class="minor-text">{{ rule.sceneCode || 'ALL' }}</div>
              </td>
              <td>
                <div class="mono">{{ rule.direction }}</div>
                <div class="minor-text">{{ rule.action }}</div>
              </td>
              <td>
                <div class="mono">{{ rule.matchType }}</div>
                <div class="minor-text truncate-cell" :title="rule.patternText">{{ rule.patternText }}</div>
              </td>
              <td>{{ rule.category }}</td>
              <td>{{ rule.severity }}</td>
              <td>{{ rule.priority ?? '-' }}</td>
              <td>
                <span class="status-pill" :class="{ active: rule.enabled, inactive: !rule.enabled }">
                  {{ rule.enabled ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatTime(rule.updatedAt) }}</td>
              <td class="action-cell">
                <button class="text-button" @click="openEditSafetyRule(rule)">编辑</button>
                <button class="text-button danger" @click="removeSafetyRule(rule)">删除</button>
              </td>
            </tr>
            <tr v-if="!safetyRules.length">
              <td colspan="10" class="empty-row">暂无安全规则配置</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else-if="activeTab === 'quotas'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">限流配额</h2>
          <p class="panel-description">按全局、用户、角色或商家维度控制每日/月调用量、Token 和成本上限。</p>
        </div>
        <button class="btn" @click="openCreateQuota">新增配额</button>
      </div>

      <div class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>主体</th>
              <th>能力 / 场景</th>
              <th>周期</th>
              <th>调用上限</th>
              <th>Token 上限</th>
              <th>成本上限</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="quota in quotas" :key="quota.id">
              <td>
                <div class="mono">{{ quota.subjectType }}</div>
                <div class="minor-text">{{ quota.subjectId || '-' }}</div>
              </td>
              <td>
                <div class="mono">{{ quota.capabilityCode || 'ALL' }}</div>
                <div class="minor-text">{{ quota.sceneCode || 'ALL' }}</div>
              </td>
              <td>{{ quota.quotaPeriod === 'MONTH' ? '月' : '日' }}</td>
              <td>{{ quota.maxCalls ?? '-' }}</td>
              <td>{{ quota.maxTokens ?? '-' }}</td>
              <td>{{ formatMoney(quota.maxCost) }}</td>
              <td>
                <span class="status-pill" :class="{ active: quota.enabled, inactive: !quota.enabled }">
                  {{ quota.enabled ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatTime(quota.updatedAt) }}</td>
              <td class="action-cell">
                <button class="text-button" @click="openEditQuota(quota)">编辑</button>
                <button class="text-button danger" @click="removeQuota(quota)">删除</button>
              </td>
            </tr>
            <tr v-if="!quotas.length">
              <td colspan="9" class="empty-row">暂无限流配额配置</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else-if="activeTab === 'usageCost'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">用量成本</h2>
          <p class="panel-description">按日期和模型聚合调用量、Token、成本和平均耗时。</p>
        </div>
        <button class="btn btn-secondary" @click="loadUsageCost">刷新用量</button>
      </div>

      <div class="split-grid">
        <div class="card-panel">
          <div class="section-head">
            <h3>近 14 日趋势</h3>
          </div>
          <div class="table-shell">
            <table class="data-table compact-table">
              <thead>
                <tr>
                  <th>日期</th>
                  <th>调用</th>
                  <th>成功</th>
                  <th>Tokens</th>
                  <th>成本</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in usageTrend" :key="item.bucket">
                  <td>{{ item.bucket }}</td>
                  <td>{{ item.totalCalls }}</td>
                  <td>{{ item.successCalls }}</td>
                  <td>{{ formatNumber(item.totalTokens) }}</td>
                  <td>{{ formatMoney(item.totalCostAmount) }}</td>
                </tr>
                <tr v-if="!usageTrend.length">
                  <td colspan="5" class="empty-row">暂无趋势数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card-panel">
          <div class="section-head">
            <h3>模型排行</h3>
          </div>
          <div class="table-shell">
            <table class="data-table compact-table">
              <thead>
                <tr>
                  <th>Provider / 模型</th>
                  <th>调用</th>
                  <th>Tokens</th>
                  <th>成本</th>
                  <th>均耗时</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in modelRanking" :key="`${item.providerCode}-${item.modelCode}`">
                  <td>
                    <div>{{ item.providerCode }}</div>
                    <div class="minor-text mono">{{ item.modelCode || '-' }}</div>
                  </td>
                  <td>{{ item.totalCalls }}</td>
                  <td>{{ formatNumber(item.totalTokens) }}</td>
                  <td>{{ formatMoney(item.totalCostAmount) }}</td>
                  <td>{{ item.avgLatencyMs ? `${Math.round(item.avgLatencyMs)} ms` : '-' }}</td>
                </tr>
                <tr v-if="!modelRanking.length">
                  <td colspan="5" class="empty-row">暂无模型排行数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === 'capabilities'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">Capability 管理</h2>
          <p class="panel-description">统一控制能力开关、灰度标记和限流/配额规则。</p>
        </div>
        <button class="btn" @click="openCreateCapability">新增 Capability</button>
      </div>

      <div class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>编码</th>
              <th>名称</th>
              <th>状态</th>
              <th>灰度</th>
              <th>灰度规则</th>
              <th>限流规则</th>
              <th>配额规则</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="capability in capabilities" :key="capability.id">
              <td class="mono">{{ capability.capabilityCode }}</td>
              <td>{{ capability.capabilityName }}</td>
              <td>
                <span class="status-pill" :class="{ active: capability.enabled, inactive: !capability.enabled }">
                  {{ capability.status }}
                </span>
              </td>
              <td>{{ capability.grayEnabled ? '开启' : '关闭' }}</td>
              <td class="truncate-cell" :title="capability.grayRuleJson || ''">{{ previewText(capability.grayRuleJson) }}</td>
              <td class="truncate-cell" :title="capability.rateLimitJson || ''">{{ previewText(capability.rateLimitJson) }}</td>
              <td class="truncate-cell" :title="capability.quotaRuleJson || ''">{{ previewText(capability.quotaRuleJson) }}</td>
              <td>{{ formatTime(capability.updatedAt) }}</td>
              <td class="action-cell">
                <button class="text-button" @click="openEditCapability(capability)">编辑</button>
                <button class="text-button danger" @click="removeCapability(capability)">删除</button>
              </td>
            </tr>
            <tr v-if="!capabilities.length">
              <td colspan="9" class="empty-row">暂无 Capability 配置</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else-if="activeTab === 'scenes'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">Scene 管理</h2>
          <p class="panel-description">按场景控制模型、提示词、安全等级和超时。</p>
        </div>
        <button class="btn" @click="openCreateScene">新增 Scene</button>
      </div>

      <div class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>场景编码</th>
              <th>名称</th>
              <th>能力</th>
              <th>Provider</th>
              <th>模型</th>
              <th>安全等级</th>
              <th>超时</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="scene in scenes" :key="scene.id">
              <td class="mono">{{ scene.sceneCode }}</td>
              <td>{{ scene.sceneName }}</td>
              <td class="mono">{{ scene.capabilityCode }}</td>
              <td class="mono">{{ scene.providerCode || '-' }}</td>
              <td class="mono">{{ scene.modelCode || '-' }}</td>
              <td>{{ scene.safetyLevel || '-' }}</td>
              <td>{{ scene.timeoutMs ? `${scene.timeoutMs} ms` : '-' }}</td>
              <td>
                <span class="status-pill" :class="{ active: scene.enabled, inactive: !scene.enabled }">
                  {{ scene.enabled ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatTime(scene.updatedAt) }}</td>
              <td class="action-cell">
                <button class="text-button" @click="openEditScene(scene)">编辑</button>
                <button class="text-button danger" @click="removeScene(scene)">删除</button>
              </td>
            </tr>
            <tr v-if="!scenes.length">
              <td colspan="10" class="empty-row">暂无 Scene 配置</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else-if="activeTab === 'ops'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">运维 Agent</h2>
          <p class="panel-description">聚合服务健康、告警、日志和 AI 调用状态，生成可执行的运维诊断报告。</p>
        </div>
        <div class="inline-actions">
          <button class="btn btn-secondary" :disabled="opsLoading" @click="loadOpsSnapshot">
            {{ opsLoading ? '刷新中...' : '刷新快照' }}
          </button>
          <button class="btn" :disabled="opsAnalyzing" @click="runOpsAnalysis">
            {{ opsAnalyzing ? '分析中...' : '生成报告' }}
          </button>
        </div>
      </div>

      <div class="ops-layout">
        <div class="ops-left">
          <label class="form-item ops-question">
            <span>分析任务</span>
            <textarea v-model.trim="opsQuestion" rows="4" placeholder="例如：分析当前告警和最近错误日志，给出处理步骤"></textarea>
          </label>

          <div class="ops-options">
            <label><input v-model="opsOptions.includeAlerts" type="checkbox" /> 告警</label>
            <label><input v-model="opsOptions.includeLogs" type="checkbox" /> 日志</label>
            <label><input v-model="opsOptions.includeAiLogs" type="checkbox" /> AI 调用日志</label>
            <select v-model.number="opsOptions.logLimit">
              <option :value="8">8 条</option>
              <option :value="12">12 条</option>
              <option :value="20">20 条</option>
            </select>
          </div>

          <div class="ops-section">
            <div class="section-head compact">
              <h3>服务健康</h3>
              <span class="minor-text">{{ formatTime(opsSnapshot?.generatedAt) }}</span>
            </div>
            <div v-if="opsSnapshot?.services?.length" class="ops-service-list">
              <div v-for="service in opsSnapshot.services" :key="service.code" class="ops-service-row">
                <div>
                  <strong>{{ service.name }}</strong>
                  <div class="minor-text mono">{{ service.healthUrl }}</div>
                </div>
                <span class="status-pill" :class="{ active: service.status === 'UP', inactive: service.status !== 'UP' }">
                  {{ service.status }}
                </span>
              </div>
            </div>
            <div v-else class="empty-state">暂无服务健康快照</div>
          </div>

          <div class="ops-section">
            <div class="section-head compact">
              <h3>活跃告警</h3>
              <span class="minor-text">{{ opsSnapshot?.alerts?.length || 0 }} 条</span>
            </div>
            <div v-if="opsSnapshot?.alerts?.length" class="ops-alert-list">
              <div v-for="alert in opsSnapshot.alerts" :key="`${alert.alertName}-${alert.service}`" class="ops-alert-item">
                <div class="ops-alert-title">
                  <strong>{{ alert.alertName }}</strong>
                  <span class="status-pill inactive">{{ alert.severity || alert.state }}</span>
                </div>
                <p>{{ alert.description }}</p>
                <div class="minor-text">{{ alert.service || '-' }} / {{ alert.duration }} / {{ alert.source || 'prometheus' }}</div>
              </div>
            </div>
            <div v-else class="empty-state">暂无告警</div>
          </div>
        </div>

        <div class="ops-right">
          <div class="ops-section">
            <div class="section-head compact">
              <h3>Provider 状态</h3>
            </div>
            <div v-if="opsSnapshot?.provider" class="ops-provider-grid">
              <span>Provider</span><strong>{{ opsSnapshot.provider.providerName }}</strong>
              <span>模型</span><strong class="mono">{{ opsSnapshot.provider.defaultModelCode }}</strong>
              <span>状态</span>
              <strong :class="opsSnapshot.provider.available ? 'success-text' : 'danger-text'">
                {{ opsSnapshot.provider.available ? '可用' : '不可用' }}
              </strong>
              <span>API Key</span><strong>{{ opsSnapshot.provider.apiKeyConfigured ? '已注入' : '缺失' }}</strong>
            </div>
            <div v-else class="empty-state">暂无 Provider 快照</div>
          </div>

          <div class="ops-section">
            <div class="section-head compact">
              <h3>数据源状态</h3>
            </div>
            <div v-if="opsSnapshot?.dataSources?.length" class="ops-data-source-list">
              <div v-for="source in opsSnapshot.dataSources" :key="source.code" class="ops-data-source-row">
                <div>
                  <strong>{{ source.name }}</strong>
                  <div class="minor-text">{{ source.message || source.code }}</div>
                </div>
                <span
                  class="status-pill"
                  :class="{ active: source.status === 'UP', inactive: source.status !== 'UP' }"
                >
                  {{ source.status }}
                </span>
              </div>
            </div>
            <div v-else class="empty-state">暂无数据源状态</div>
          </div>

          <div class="ops-section">
            <div class="section-head compact">
              <h3>最近日志</h3>
            </div>
            <div v-if="opsSnapshot?.recentLogs?.length" class="ops-log-list">
              <div v-for="(entry, index) in opsSnapshot.recentLogs.slice(0, 6)" :key="`${entry.timestamp}-${index}`" class="ops-log-item">
                <span class="status-tag" :class="{ disabled: entry.level !== 'ERROR', enabled: entry.level === 'ERROR' }">{{ entry.level }}</span>
                <span class="mono">{{ entry.service }}</span>
                <p>{{ entry.message }}</p>
              </div>
            </div>
            <div v-else class="empty-state">暂无日志样本</div>
          </div>

          <div class="ops-section report-section">
            <div class="section-head compact">
              <h3>诊断报告</h3>
              <span v-if="opsAnalysis" class="minor-text">{{ opsAnalysis.modelCode }} / {{ opsAnalysis.latencyMs || '-' }} ms</span>
            </div>
            <pre v-if="opsReport" class="ops-report">{{ opsReport }}</pre>
            <div v-else class="empty-state">点击“生成报告”后显示运维 Agent 分析结果。</div>
          </div>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === 'logs'" class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">调用日志</h2>
          <p class="panel-description">展示最近 {{ logLimit }} 条 AI 调用记录。</p>
        </div>
        <div class="inline-actions">
          <select v-model.number="logLimit" @change="loadLogs">
            <option :value="20">最近 20 条</option>
            <option :value="50">最近 50 条</option>
            <option :value="100">最近 100 条</option>
          </select>
          <button class="btn btn-secondary" @click="loadLogs">刷新日志</button>
        </div>
      </div>

      <div class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>请求 ID</th>
              <th>用户</th>
              <th>能力 / 场景</th>
              <th>Provider / 模型</th>
              <th>结果</th>
              <th>Tokens</th>
              <th>成本</th>
              <th>Fallback</th>
              <th>耗时</th>
              <th>错误</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id">
              <td>{{ formatTime(log.createdAt) }}</td>
              <td class="mono">{{ shrink(log.requestId, 18) }}</td>
              <td>{{ log.userId }}</td>
              <td>
                <div>{{ log.capabilityCode }}</div>
                <div class="minor-text">{{ log.sceneCode }}</div>
              </td>
              <td>
                <div>{{ log.providerCode }}</div>
                <div class="minor-text mono">{{ log.modelCode || '-' }}</div>
              </td>
              <td>
                <span class="status-pill" :class="{ active: log.success === 1, inactive: log.success !== 1 }">
                  {{ log.success === 1 ? '成功' : '失败' }}
                </span>
              </td>
              <td>{{ log.totalTokens ?? '-' }}</td>
              <td>{{ formatMoney(log.costAmount) }}</td>
              <td>{{ log.fallbackLevel ?? '-' }}</td>
              <td>{{ log.latencyMs ? `${log.latencyMs} ms` : '-' }}</td>
              <td class="truncate-cell" :title="log.errorMessage || ''">
                {{ log.errorType || previewText(log.errorMessage) }}
              </td>
            </tr>
            <tr v-if="!logs.length">
              <td colspan="11" class="empty-row">暂无调用日志</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="dialogType" class="dialog-overlay" @click="closeDialog">
      <div class="dialog" @click.stop>
        <div class="dialog-header">
          <h3>{{ dialogTitle }}</h3>
          <button class="close-button" @click="closeDialog">×</button>
        </div>

        <form v-if="dialogType === 'provider'" class="dialog-form" @submit.prevent="submitProvider">
          <div class="form-grid">
            <label class="form-item">
              <span>Provider 编码</span>
              <input v-model.trim="providerForm.providerCode" :disabled="dialogMode === 'edit'" required maxlength="32" />
            </label>
            <label class="form-item">
              <span>Provider 名称</span>
              <input v-model.trim="providerForm.providerName" required maxlength="64" />
            </label>
            <label class="form-item span-2">
              <span>Base URL</span>
              <input v-model.trim="providerForm.baseUrl" required maxlength="255" />
            </label>
            <label class="form-item">
              <span>默认模型</span>
              <input v-model.trim="providerForm.defaultModelCode" required maxlength="64" />
            </label>
            <label class="form-item">
              <span>超时（ms）</span>
              <input v-model.number="providerForm.timeoutMs" type="number" min="1000" />
            </label>
            <label class="form-item">
              <span>上下文消息数</span>
              <input v-model.number="providerForm.maxContextMessages" type="number" min="1" />
            </label>
            <label class="form-item">
              <span>Temperature</span>
              <input v-model.number="providerForm.temperature" type="number" min="0" max="2" step="0.1" />
            </label>
            <label class="form-item">
              <span>Top P</span>
              <input v-model.number="providerForm.topP" type="number" min="0" max="1" step="0.1" />
            </label>
            <label class="form-item">
              <span>最大输出 Tokens</span>
              <input v-model.number="providerForm.maxOutputTokens" type="number" min="1" />
            </label>
            <label class="form-item span-2">
              <span>{{ dialogMode === 'edit' ? 'API Key（留空则保持不变）' : 'API Key' }}</span>
              <input v-model.trim="providerForm.apiKeyCipher" type="password" maxlength="512" />
            </label>
            <label class="form-item span-2 textarea-item">
              <span>系统提示词模板</span>
              <textarea v-model.trim="providerForm.systemPromptTemplate" rows="4" maxlength="4000" />
            </label>
            <label class="form-checkbox span-2">
              <input v-model="providerForm.enabled" type="checkbox" />
              <span>启用该 Provider</span>
            </label>
          </div>

          <div class="dialog-actions">
            <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
            <button type="submit" class="btn" :disabled="submitting">{{ submitting ? '保存中...' : '保存' }}</button>
          </div>
        </form>

        <form v-else-if="dialogType === 'model'" class="dialog-form" @submit.prevent="submitModel">
          <div class="form-grid">
            <label class="form-item">
              <span>Provider 编码</span>
              <input v-model.trim="modelForm.providerCode" required maxlength="32" />
            </label>
            <label class="form-item">
              <span>模型编码</span>
              <input v-model.trim="modelForm.modelCode" required maxlength="64" />
            </label>
            <label class="form-item span-2">
              <span>模型名称</span>
              <input v-model.trim="modelForm.modelName" required maxlength="128" />
            </label>
            <label class="form-item">
              <span>上下文窗口</span>
              <input v-model.number="modelForm.contextWindow" type="number" min="1" />
            </label>
            <label class="form-item">
              <span>最大输出 Tokens</span>
              <input v-model.number="modelForm.maxOutputTokens" type="number" min="1" />
            </label>
            <label class="form-item">
              <span>输入价格 / 1K</span>
              <input v-model.number="modelForm.inputPricePer1k" type="number" min="0" step="0.000001" />
            </label>
            <label class="form-item">
              <span>输出价格 / 1K</span>
              <input v-model.number="modelForm.outputPricePer1k" type="number" min="0" step="0.000001" />
            </label>
            <label class="form-item">
              <span>优先级</span>
              <input v-model.number="modelForm.priority" type="number" min="0" />
            </label>
            <label class="form-checkbox">
              <input v-model="modelForm.enabled" type="checkbox" />
              <span>启用模型</span>
            </label>
            <label class="form-item span-2 textarea-item">
              <span>能力 JSON</span>
              <textarea v-model.trim="modelForm.capabilitiesJson" rows="4" maxlength="4000" />
            </label>
          </div>

          <div class="dialog-actions">
            <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
            <button type="submit" class="btn" :disabled="submitting">{{ submitting ? '保存中...' : '保存' }}</button>
          </div>
        </form>

        <form v-else-if="dialogType === 'routeRule'" class="dialog-form" @submit.prevent="submitRouteRule">
          <div class="form-grid">
            <label class="form-item span-2">
              <span>规则名称</span>
              <input v-model.trim="routeRuleForm.ruleName" required maxlength="128" />
            </label>
            <label class="form-item">
              <span>能力编码</span>
              <input v-model.trim="routeRuleForm.capabilityCode" required maxlength="32" />
            </label>
            <label class="form-item">
              <span>场景编码</span>
              <input v-model.trim="routeRuleForm.sceneCode" maxlength="64" />
            </label>
            <label class="form-item">
              <span>优先级</span>
              <input v-model.number="routeRuleForm.priority" type="number" min="0" />
            </label>
            <label class="form-checkbox">
              <input v-model="routeRuleForm.enabled" type="checkbox" />
              <span>启用规则</span>
            </label>
            <label class="form-item span-2 textarea-item">
              <span>匹配规则 JSON</span>
              <textarea v-model.trim="routeRuleForm.matchRuleJson" rows="3" maxlength="4000" />
            </label>
            <label class="form-item span-2 textarea-item">
              <span>路由规则 JSON</span>
              <textarea v-model.trim="routeRuleForm.routeRuleJson" rows="7" maxlength="8000" required />
            </label>
          </div>

          <div class="dialog-actions">
            <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
            <button type="submit" class="btn" :disabled="submitting">{{ submitting ? '保存中...' : '保存' }}</button>
          </div>
        </form>

        <form v-else-if="dialogType === 'safetyRule'" class="dialog-form" @submit.prevent="submitSafetyRule">
          <div class="form-grid">
            <label class="form-item span-2">
              <span>规则名称</span>
              <input v-model.trim="safetyRuleForm.ruleName" required maxlength="128" />
            </label>
            <label class="form-item">
              <span>能力编码</span>
              <input v-model.trim="safetyRuleForm.capabilityCode" maxlength="32" placeholder="留空表示全部能力" />
            </label>
            <label class="form-item">
              <span>场景编码</span>
              <input v-model.trim="safetyRuleForm.sceneCode" maxlength="64" placeholder="留空表示全部场景" />
            </label>
            <label class="form-item">
              <span>方向</span>
              <select v-model="safetyRuleForm.direction">
                <option value="INPUT">INPUT</option>
                <option value="OUTPUT">OUTPUT</option>
                <option value="BOTH">BOTH</option>
              </select>
            </label>
            <label class="form-item">
              <span>动作</span>
              <select v-model="safetyRuleForm.action">
                <option value="BLOCK">BLOCK</option>
                <option value="AUDIT">AUDIT</option>
              </select>
            </label>
            <label class="form-item">
              <span>匹配类型</span>
              <select v-model="safetyRuleForm.matchType">
                <option value="KEYWORD">KEYWORD</option>
                <option value="REGEX">REGEX</option>
              </select>
            </label>
            <label class="form-item">
              <span>风险等级</span>
              <select v-model="safetyRuleForm.severity">
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="CRITICAL">CRITICAL</option>
              </select>
            </label>
            <label class="form-item">
              <span>分类</span>
              <input v-model.trim="safetyRuleForm.category" required maxlength="64" />
            </label>
            <label class="form-item">
              <span>优先级</span>
              <input v-model.number="safetyRuleForm.priority" type="number" min="0" />
            </label>
            <label class="form-checkbox">
              <input v-model="safetyRuleForm.enabled" type="checkbox" />
              <span>启用规则</span>
            </label>
            <label class="form-item span-2 textarea-item">
              <span>匹配内容</span>
              <textarea v-model.trim="safetyRuleForm.patternText" rows="4" maxlength="512" required />
            </label>
          </div>

          <div class="dialog-actions">
            <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
            <button type="submit" class="btn" :disabled="submitting">{{ submitting ? '保存中...' : '保存' }}</button>
          </div>
        </form>

        <form v-else-if="dialogType === 'quota'" class="dialog-form" @submit.prevent="submitQuota">
          <div class="form-grid">
            <label class="form-item">
              <span>主体类型</span>
              <select v-model="quotaForm.subjectType">
                <option value="GLOBAL">GLOBAL</option>
                <option value="USER">USER</option>
                <option value="ROLE">ROLE</option>
                <option value="MERCHANT">MERCHANT</option>
              </select>
            </label>
            <label class="form-item">
              <span>主体 ID</span>
              <input v-model.trim="quotaForm.subjectId" :disabled="quotaForm.subjectType === 'GLOBAL'" maxlength="64" />
            </label>
            <label class="form-item">
              <span>能力编码</span>
              <input v-model.trim="quotaForm.capabilityCode" maxlength="32" placeholder="留空表示全部能力" />
            </label>
            <label class="form-item">
              <span>场景编码</span>
              <input v-model.trim="quotaForm.sceneCode" maxlength="64" placeholder="留空表示全部场景" />
            </label>
            <label class="form-item">
              <span>周期</span>
              <select v-model="quotaForm.quotaPeriod">
                <option value="DAY">日</option>
                <option value="MONTH">月</option>
              </select>
            </label>
            <label class="form-checkbox">
              <input v-model="quotaForm.enabled" type="checkbox" />
              <span>启用配额</span>
            </label>
            <label class="form-item">
              <span>最大调用数</span>
              <input v-model.number="quotaForm.maxCalls" type="number" min="1" />
            </label>
            <label class="form-item">
              <span>最大 Tokens</span>
              <input v-model.number="quotaForm.maxTokens" type="number" min="1" />
            </label>
            <label class="form-item">
              <span>最大成本</span>
              <input v-model.number="quotaForm.maxCost" type="number" min="0" step="0.0001" />
            </label>
          </div>

          <div class="dialog-actions">
            <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
            <button type="submit" class="btn" :disabled="submitting">{{ submitting ? '保存中...' : '保存' }}</button>
          </div>
        </form>

        <form v-else-if="dialogType === 'capability'" class="dialog-form" @submit.prevent="submitCapability">
          <div class="form-grid">
            <label class="form-item">
              <span>能力编码</span>
              <input v-model.trim="capabilityForm.capabilityCode" :disabled="dialogMode === 'edit'" required maxlength="32" />
            </label>
            <label class="form-item">
              <span>能力名称</span>
              <input v-model.trim="capabilityForm.capabilityName" required maxlength="64" />
            </label>
            <label class="form-checkbox">
              <input v-model="capabilityForm.enabled" type="checkbox" />
              <span>启用能力</span>
            </label>
            <label class="form-checkbox">
              <input v-model="capabilityForm.grayEnabled" type="checkbox" />
              <span>开启灰度</span>
            </label>
            <label class="form-item span-2 textarea-item">
              <span>灰度规则 JSON</span>
              <textarea v-model.trim="capabilityForm.grayRuleJson" rows="4" maxlength="4000" />
            </label>
            <label class="form-item span-2 textarea-item">
              <span>限流规则 JSON</span>
              <textarea v-model.trim="capabilityForm.rateLimitJson" rows="4" maxlength="4000" />
            </label>
            <label class="form-item span-2 textarea-item">
              <span>配额规则 JSON</span>
              <textarea v-model.trim="capabilityForm.quotaRuleJson" rows="4" maxlength="4000" />
            </label>
          </div>

          <div class="dialog-actions">
            <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
            <button type="submit" class="btn" :disabled="submitting">{{ submitting ? '保存中...' : '保存' }}</button>
          </div>
        </form>

        <form v-else-if="dialogType === 'scene'" class="dialog-form" @submit.prevent="submitScene">
          <div class="form-grid">
            <label class="form-item">
              <span>场景编码</span>
              <input v-model.trim="sceneForm.sceneCode" :disabled="dialogMode === 'edit'" required maxlength="64" />
            </label>
            <label class="form-item">
              <span>场景名称</span>
              <input v-model.trim="sceneForm.sceneName" required maxlength="128" />
            </label>
            <label class="form-item">
              <span>能力编码</span>
              <input v-model.trim="sceneForm.capabilityCode" required maxlength="32" />
            </label>
            <label class="form-item">
              <span>Provider 编码</span>
              <input v-model.trim="sceneForm.providerCode" maxlength="32" />
            </label>
            <label class="form-item">
              <span>模型编码</span>
              <input v-model.trim="sceneForm.modelCode" maxlength="64" />
            </label>
            <label class="form-item">
              <span>安全等级</span>
              <input v-model.trim="sceneForm.safetyLevel" maxlength="16" />
            </label>
            <label class="form-item">
              <span>超时（ms）</span>
              <input v-model.number="sceneForm.timeoutMs" type="number" min="1000" />
            </label>
            <label class="form-checkbox">
              <input v-model="sceneForm.enabled" type="checkbox" />
              <span>启用场景</span>
            </label>
            <label class="form-item span-2 textarea-item">
              <span>系统提示词模板</span>
              <textarea v-model.trim="sceneForm.systemPromptTemplate" rows="4" maxlength="4000" />
            </label>
            <label class="form-item span-2 textarea-item">
              <span>输入 Schema JSON</span>
              <textarea v-model.trim="sceneForm.inputSchemaJson" rows="4" maxlength="4000" />
            </label>
            <label class="form-item span-2 textarea-item">
              <span>输出 Schema JSON</span>
              <textarea v-model.trim="sceneForm.outputSchemaJson" rows="4" maxlength="4000" />
            </label>
          </div>

          <div class="dialog-actions">
            <button type="button" class="btn btn-secondary" @click="closeDialog">取消</button>
            <button type="submit" class="btn" :disabled="submitting">{{ submitting ? '保存中...' : '保存' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  checkAiModeration,
  checkAdminAiProviderHealth,
  createAdminAiCapability,
  createAdminAiGatewayRouteRule,
  createAdminAiGatewaySafetyRule,
  createAdminAiModel,
  createAdminAiProvider,
  createAdminAiScene,
  createAdminAiUsageQuota,
  deleteAdminAiCapability,
  deleteAdminAiGatewayRouteRule,
  deleteAdminAiGatewaySafetyRule,
  deleteAdminAiModel,
  deleteAdminAiProvider,
  deleteAdminAiScene,
  deleteAdminAiUsageQuota,
  analyzeAdminAiOps,
  getAdminAiCapabilities,
  getAdminAiGatewayRouteRules,
  getAdminAiGatewaySafetyRules,
  getAdminAiLogs,
  getAdminAiModelRanking,
  getAdminAiModels,
  getAdminAiOverview,
  getAdminAiOpsSnapshot,
  getAdminAiProviders,
  getAdminAiScenes,
  getAdminAiUsageQuotas,
  getAdminAiUsageTrend,
  probeAdminAiGateway,
  updateAdminAiCapability,
  updateAdminAiGatewayRouteRule,
  updateAdminAiGatewaySafetyRule,
  updateAdminAiModel,
  updateAdminAiProvider,
  updateAdminAiScene,
  updateAdminAiUsageQuota,
  type AdminAiCallLog,
  type AdminAiCapabilityConfig,
  type AdminAiCapabilityUpsertPayload,
  type AdminAiGatewayProbeResponse,
  type AdminAiGatewayRouteRule,
  type AdminAiGatewayRouteRuleUpsertPayload,
  type AdminAiGatewaySafetyRule,
  type AdminAiGatewaySafetyRuleUpsertPayload,
  type AdminAiModelConfig,
  type AdminAiModelUpsertPayload,
  type AdminAiOverview,
  type AdminAiOpsAnalysisResponse,
  type AdminAiOpsSnapshot,
  type AdminAiProviderConfig,
  type AdminAiProviderHealthCheck,
  type AdminAiProviderUpsertPayload,
  type AdminAiSceneConfig,
  type AdminAiSceneUpsertPayload,
  type AdminAiUsageQuota,
  type AdminAiUsageQuotaUpsertPayload,
  type AdminAiUsageTrendItem,
  type AdminAiModelRankingItem,
  type AiModerationCheckResponse
} from '../../api/admin'
import { showAlert, showConfirm } from '../../utils/dialog'

type TabKey = 'overview' | 'providers' | 'testBench' | 'models' | 'routeRules' | 'safetyRules' | 'quotas' | 'usageCost' | 'capabilities' | 'scenes' | 'ops' | 'logs'
type DialogType = 'provider' | 'model' | 'routeRule' | 'safetyRule' | 'quota' | 'capability' | 'scene' | null
type DialogMode = 'create' | 'edit'

const tabs: Array<{ key: TabKey; label: string }> = [
  { key: 'overview', label: '运行总览' },
  { key: 'providers', label: 'Providers' },
  { key: 'testBench', label: '测试台' },
  { key: 'models', label: 'Models' },
  { key: 'routeRules', label: 'Route Rules' },
  { key: 'safetyRules', label: '安全治理' },
  { key: 'quotas', label: '限流配额' },
  { key: 'usageCost', label: '用量成本' },
  { key: 'capabilities', label: 'Capabilities' },
  { key: 'scenes', label: 'Scenes' },
  { key: 'ops', label: '运维 Agent' },
  { key: 'logs', label: '调用日志' }
]

const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const activeTab = ref<TabKey>('overview')
const dialogType = ref<DialogType>(null)
const dialogMode = ref<DialogMode>('create')
const currentId = ref<number | null>(null)
const logLimit = ref(20)

const overview = reactive<AdminAiOverview>({
  totalCalls: 0,
  successCalls: 0,
  failedCalls: 0,
  totalTokens: 0,
  avgLatencyMs: 0,
  totalCostAmount: 0
})

const providers = ref<AdminAiProviderConfig[]>([])
const models = ref<AdminAiModelConfig[]>([])
const routeRules = ref<AdminAiGatewayRouteRule[]>([])
const safetyRules = ref<AdminAiGatewaySafetyRule[]>([])
const quotas = ref<AdminAiUsageQuota[]>([])
const usageTrend = ref<AdminAiUsageTrendItem[]>([])
const modelRanking = ref<AdminAiModelRankingItem[]>([])
const capabilities = ref<AdminAiCapabilityConfig[]>([])
const scenes = ref<AdminAiSceneConfig[]>([])
const logs = ref<AdminAiCallLog[]>([])
const opsSnapshot = ref<AdminAiOpsSnapshot | null>(null)
const opsAnalysis = ref<AdminAiOpsAnalysisResponse | null>(null)
const opsReport = ref('')
const opsLoading = ref(false)
const opsAnalyzing = ref(false)
const opsQuestion = ref('请基于当前服务健康、告警、最近错误日志和 AI 调用情况，生成校园生活平台运维巡检报告。')
const opsOptions = reactive({
  includeAlerts: true,
  includeLogs: true,
  includeAiLogs: true,
  logLimit: 12
})
const providerHealthChecks = ref<Record<number, AdminAiProviderHealthCheck>>({})
const providerCheckingId = ref<number | null>(null)
const probeLoading = ref(false)
const probeResult = ref<AdminAiGatewayProbeResponse | null>(null)
const probeForm = reactive({
  content: '请用一句话回复：网关测试成功。',
  capabilityCode: 'chat',
  sceneCode: 'chat.general',
  providerCode: '',
  modelCode: '',
  temperature: 0.2 as number | undefined,
  maxOutputTokens: 256 as number | undefined
})
const moderationLoading = ref(false)
const moderationResult = ref<AiModerationCheckResponse | null>(null)
const moderationForm = reactive({
  content: '这是一条校园互助帖子，约同学一起自习。',
  targetType: 'post',
  sceneCode: 'moderation.text_post',
  providerCode: '',
  modelCode: ''
})

const providerForm = reactive({
  providerCode: '',
  providerName: '',
  baseUrl: '',
  apiKeyCipher: '',
  defaultModelCode: '',
  enabled: false,
  timeoutMs: 30000,
  maxContextMessages: 20,
  temperature: 0.7,
  topP: undefined as number | undefined,
  maxOutputTokens: 1024,
  systemPromptTemplate: ''
})

const modelForm = reactive({
  providerCode: '',
  modelCode: '',
  modelName: '',
  capabilitiesJson: '',
  contextWindow: undefined as number | undefined,
  maxOutputTokens: undefined as number | undefined,
  inputPricePer1k: undefined as number | undefined,
  outputPricePer1k: undefined as number | undefined,
  enabled: true,
  priority: 100
})

const routeRuleForm = reactive({
  ruleName: '',
  capabilityCode: '',
  sceneCode: '',
  matchRuleJson: '',
  routeRuleJson: '{\n  "candidates": []\n}',
  enabled: true,
  priority: 100
})

const safetyRuleForm = reactive({
  ruleName: '',
  capabilityCode: '',
  sceneCode: '',
  direction: 'INPUT' as AdminAiGatewaySafetyRule['direction'],
  action: 'BLOCK' as AdminAiGatewaySafetyRule['action'],
  matchType: 'KEYWORD' as AdminAiGatewaySafetyRule['matchType'],
  patternText: '',
  category: 'policy',
  severity: 'MEDIUM' as AdminAiGatewaySafetyRule['severity'],
  enabled: true,
  priority: 100
})

const quotaForm = reactive({
  subjectType: 'GLOBAL' as AdminAiUsageQuota['subjectType'],
  subjectId: '',
  capabilityCode: '',
  sceneCode: '',
  quotaPeriod: 'DAY' as AdminAiUsageQuota['quotaPeriod'],
  maxCalls: undefined as number | undefined,
  maxTokens: undefined as number | undefined,
  maxCost: undefined as number | undefined,
  enabled: true
})

const capabilityForm = reactive({
  capabilityCode: '',
  capabilityName: '',
  enabled: false,
  grayEnabled: false,
  grayRuleJson: '',
  rateLimitJson: '',
  quotaRuleJson: ''
})

const sceneForm = reactive({
  capabilityCode: '',
  sceneCode: '',
  sceneName: '',
  providerCode: '',
  modelCode: '',
  enabled: false,
  systemPromptTemplate: '',
  inputSchemaJson: '',
  outputSchemaJson: '',
  safetyLevel: '',
  timeoutMs: 30000
})

const dialogTitle = computed(() => {
  const action = dialogMode.value === 'create' ? '新增' : '编辑'
  if (dialogType.value === 'provider') return `${action} Provider`
  if (dialogType.value === 'model') return `${action} Model`
  if (dialogType.value === 'routeRule') return `${action} Route Rule`
  if (dialogType.value === 'safetyRule') return `${action} 安全规则`
  if (dialogType.value === 'quota') return `${action} 配额`
  if (dialogType.value === 'capability') return `${action} Capability`
  if (dialogType.value === 'scene') return `${action} Scene`
  return ''
})

const enabledProvidersCount = computed(() => providers.value.filter((item) => item.enabled).length)
const enabledModelsCount = computed(() => models.value.filter((item) => item.enabled).length)
const enabledRouteRulesCount = computed(() => routeRules.value.filter((item) => item.enabled).length)
const enabledQuotasCount = computed(() => quotas.value.filter((item) => item.enabled).length)
const enabledCapabilitiesCount = computed(() => capabilities.value.filter((item) => item.enabled).length)
const enabledScenesCount = computed(() => scenes.value.filter((item) => item.enabled).length)
const moderationScenes = computed(() => scenes.value.filter((item) => item.capabilityCode === 'moderation'))

const applyTabFromQuery = (tab: unknown) => {
  if (typeof tab !== 'string') return
  if (tabs.some((item) => item.key === tab)) {
    activeTab.value = tab as TabKey
  }
}

const loadAllData = async () => {
  loading.value = true
  try {
    const [
      overviewData,
      providerData,
      modelData,
      routeRuleData,
      safetyRuleData,
      quotaData,
      usageTrendData,
      modelRankingData,
      capabilityData,
      sceneData,
      logData
    ] = await Promise.all([
      getAdminAiOverview(),
      getAdminAiProviders(),
      getAdminAiModels(),
      getAdminAiGatewayRouteRules(),
      getAdminAiGatewaySafetyRules(),
      getAdminAiUsageQuotas(),
      getAdminAiUsageTrend(14),
      getAdminAiModelRanking(10),
      getAdminAiCapabilities(),
      getAdminAiScenes(),
      getAdminAiLogs(logLimit.value)
    ])

    Object.assign(overview, {
      totalCalls: overviewData?.totalCalls || 0,
      successCalls: overviewData?.successCalls || 0,
      failedCalls: overviewData?.failedCalls || 0,
      totalTokens: overviewData?.totalTokens || 0,
      avgLatencyMs: overviewData?.avgLatencyMs || 0,
      totalCostAmount: overviewData?.totalCostAmount || 0
    })
    providers.value = providerData || []
    models.value = modelData || []
    routeRules.value = routeRuleData || []
    safetyRules.value = safetyRuleData || []
    quotas.value = quotaData || []
    usageTrend.value = usageTrendData || []
    modelRanking.value = modelRankingData || []
    capabilities.value = capabilityData || []
    scenes.value = sceneData || []
    logs.value = logData || []
  } catch (error: any) {
    console.error('加载 AI 管理数据失败:', error)
    await showAlert(error?.message || '加载 AI 管理数据失败')
  } finally {
    loading.value = false
  }
}

const loadUsageCost = async () => {
  try {
    const [overviewData, usageTrendData, modelRankingData] = await Promise.all([
      getAdminAiOverview(),
      getAdminAiUsageTrend(14),
      getAdminAiModelRanking(10)
    ])
    Object.assign(overview, {
      totalCalls: overviewData?.totalCalls || 0,
      successCalls: overviewData?.successCalls || 0,
      failedCalls: overviewData?.failedCalls || 0,
      totalTokens: overviewData?.totalTokens || 0,
      avgLatencyMs: overviewData?.avgLatencyMs || 0,
      totalCostAmount: overviewData?.totalCostAmount || 0
    })
    usageTrend.value = usageTrendData || []
    modelRanking.value = modelRankingData || []
  } catch (error: any) {
    console.error('加载用量成本失败:', error)
    await showAlert(error?.message || '加载用量成本失败')
  }
}

const loadLogs = async () => {
  try {
    logs.value = await getAdminAiLogs(logLimit.value)
  } catch (error: any) {
    console.error('加载 AI 日志失败:', error)
    await showAlert(error?.message || '加载 AI 日志失败')
  }
}

const checkProviderHealth = async (provider: AdminAiProviderConfig) => {
  providerCheckingId.value = provider.id
  try {
    const result = await checkAdminAiProviderHealth(provider.id)
    providerHealthChecks.value = {
      ...providerHealthChecks.value,
      [provider.id]: result
    }
    await showAlert(result.message || (result.available ? 'Provider 配置可用' : 'Provider 不可用'))
  } catch (error: any) {
    console.error('Provider 健康检查失败:', error)
    await showAlert(error?.message || 'Provider 健康检查失败')
  } finally {
    providerCheckingId.value = null
  }
}

const runGatewayProbe = async () => {
  const content = probeForm.content.trim()
  if (!content) {
    await showAlert('请填写测试内容')
    return
  }

  probeLoading.value = true
  probeResult.value = null
  try {
    probeResult.value = await probeAdminAiGateway({
      content,
      capabilityCode: normalizeOptional(probeForm.capabilityCode),
      sceneCode: normalizeOptional(probeForm.sceneCode),
      providerCode: normalizeOptional(probeForm.providerCode),
      modelCode: normalizeOptional(probeForm.modelCode),
      temperature: normalizeOptionalNumber(probeForm.temperature),
      maxOutputTokens: normalizeOptionalNumber(probeForm.maxOutputTokens)
    })
    await Promise.all([loadUsageCost(), loadLogs()])
  } catch (error: any) {
    console.error('网关测试失败:', error)
    await showAlert(error?.message || '网关测试失败')
  } finally {
    probeLoading.value = false
  }
}

const runModerationCheck = async () => {
  const content = moderationForm.content.trim()
  if (!content) {
    await showAlert('请填写待审核内容')
    return
  }

  moderationLoading.value = true
  moderationResult.value = null
  try {
    moderationResult.value = await checkAiModeration({
      content,
      targetType: normalizeOptional(moderationForm.targetType),
      sceneCode: normalizeOptional(moderationForm.sceneCode),
      providerCode: normalizeOptional(moderationForm.providerCode),
      modelCode: normalizeOptional(moderationForm.modelCode)
    })
    await Promise.all([loadUsageCost(), loadLogs()])
  } catch (error: any) {
    console.error('文本审核失败:', error)
    await showAlert(error?.message || '文本审核失败')
  } finally {
    moderationLoading.value = false
  }
}

const loadOpsSnapshot = async () => {
  opsLoading.value = true
  try {
    opsSnapshot.value = await getAdminAiOpsSnapshot()
  } catch (error: any) {
    console.error('加载运维快照失败:', error)
    await showAlert(error?.message || '加载运维快照失败')
  } finally {
    opsLoading.value = false
  }
}

const runOpsAnalysis = async () => {
  opsAnalyzing.value = true
  opsReport.value = ''
  try {
    const response = await analyzeAdminAiOps({
      question: opsQuestion.value,
      includeAlerts: opsOptions.includeAlerts,
      includeLogs: opsOptions.includeLogs,
      includeAiLogs: opsOptions.includeAiLogs,
      logLimit: opsOptions.logLimit
    })
    opsAnalysis.value = response
    opsSnapshot.value = response.snapshot
    opsReport.value = response.report || ''
  } catch (error: any) {
    console.error('运维 Agent 分析失败:', error)
    await showAlert(error?.message || '运维 Agent 分析失败')
  } finally {
    opsAnalyzing.value = false
  }
}

const resetProviderForm = () => {
  Object.assign(providerForm, {
    providerCode: '',
    providerName: '',
    baseUrl: '',
    apiKeyCipher: '',
    defaultModelCode: '',
    enabled: false,
    timeoutMs: 30000,
    maxContextMessages: 20,
    temperature: 0.7,
    topP: undefined,
    maxOutputTokens: 1024,
    systemPromptTemplate: ''
  })
}

const resetModelForm = () => {
  Object.assign(modelForm, {
    providerCode: '',
    modelCode: '',
    modelName: '',
    capabilitiesJson: '',
    contextWindow: undefined,
    maxOutputTokens: undefined,
    inputPricePer1k: undefined,
    outputPricePer1k: undefined,
    enabled: true,
    priority: 100
  })
}

const resetRouteRuleForm = () => {
  Object.assign(routeRuleForm, {
    ruleName: '',
    capabilityCode: '',
    sceneCode: '',
    matchRuleJson: '',
    routeRuleJson: '{\n  "candidates": []\n}',
    enabled: true,
    priority: 100
  })
}

const resetSafetyRuleForm = () => {
  Object.assign(safetyRuleForm, {
    ruleName: '',
    capabilityCode: '',
    sceneCode: '',
    direction: 'INPUT',
    action: 'BLOCK',
    matchType: 'KEYWORD',
    patternText: '',
    category: 'policy',
    severity: 'MEDIUM',
    enabled: true,
    priority: 100
  })
}

const resetQuotaForm = () => {
  Object.assign(quotaForm, {
    subjectType: 'GLOBAL',
    subjectId: '',
    capabilityCode: '',
    sceneCode: '',
    quotaPeriod: 'DAY',
    maxCalls: undefined,
    maxTokens: undefined,
    maxCost: undefined,
    enabled: true
  })
}

const resetCapabilityForm = () => {
  Object.assign(capabilityForm, {
    capabilityCode: '',
    capabilityName: '',
    enabled: false,
    grayEnabled: false,
    grayRuleJson: '',
    rateLimitJson: '',
    quotaRuleJson: ''
  })
}

const resetSceneForm = () => {
  Object.assign(sceneForm, {
    capabilityCode: '',
    sceneCode: '',
    sceneName: '',
    providerCode: '',
    modelCode: '',
    enabled: false,
    systemPromptTemplate: '',
    inputSchemaJson: '',
    outputSchemaJson: '',
    safetyLevel: '',
    timeoutMs: 30000
  })
}

const closeDialog = () => {
  dialogType.value = null
  currentId.value = null
  submitting.value = false
}

const openCreateProvider = () => {
  dialogMode.value = 'create'
  dialogType.value = 'provider'
  currentId.value = null
  resetProviderForm()
}

const openEditProvider = (provider: AdminAiProviderConfig) => {
  dialogMode.value = 'edit'
  dialogType.value = 'provider'
  currentId.value = provider.id
  Object.assign(providerForm, {
    providerCode: provider.providerCode,
    providerName: provider.providerName,
    baseUrl: provider.baseUrl,
    apiKeyCipher: '',
    defaultModelCode: provider.defaultModelCode,
    enabled: provider.enabled,
    timeoutMs: provider.timeoutMs || 30000,
    maxContextMessages: provider.maxContextMessages || 20,
    temperature: provider.temperature ?? 0.7,
    topP: provider.topP,
    maxOutputTokens: provider.maxOutputTokens || 1024,
    systemPromptTemplate: provider.systemPromptTemplate || ''
  })
}

const openCreateModel = () => {
  dialogMode.value = 'create'
  dialogType.value = 'model'
  currentId.value = null
  resetModelForm()
}

const openEditModel = (model: AdminAiModelConfig) => {
  dialogMode.value = 'edit'
  dialogType.value = 'model'
  currentId.value = model.id
  Object.assign(modelForm, {
    providerCode: model.providerCode,
    modelCode: model.modelCode,
    modelName: model.modelName,
    capabilitiesJson: model.capabilitiesJson || '',
    contextWindow: model.contextWindow,
    maxOutputTokens: model.maxOutputTokens,
    inputPricePer1k: model.inputPricePer1k,
    outputPricePer1k: model.outputPricePer1k,
    enabled: model.enabled,
    priority: model.priority ?? 100
  })
}

const openCreateRouteRule = () => {
  dialogMode.value = 'create'
  dialogType.value = 'routeRule'
  currentId.value = null
  resetRouteRuleForm()
}

const openEditRouteRule = (rule: AdminAiGatewayRouteRule) => {
  dialogMode.value = 'edit'
  dialogType.value = 'routeRule'
  currentId.value = rule.id
  Object.assign(routeRuleForm, {
    ruleName: rule.ruleName,
    capabilityCode: rule.capabilityCode,
    sceneCode: rule.sceneCode || '',
    matchRuleJson: rule.matchRuleJson || '',
    routeRuleJson: rule.routeRuleJson,
    enabled: rule.enabled,
    priority: rule.priority ?? 100
  })
}

const openCreateSafetyRule = () => {
  dialogMode.value = 'create'
  dialogType.value = 'safetyRule'
  currentId.value = null
  resetSafetyRuleForm()
}

const openEditSafetyRule = (rule: AdminAiGatewaySafetyRule) => {
  dialogMode.value = 'edit'
  dialogType.value = 'safetyRule'
  currentId.value = rule.id
  Object.assign(safetyRuleForm, {
    ruleName: rule.ruleName,
    capabilityCode: rule.capabilityCode || '',
    sceneCode: rule.sceneCode || '',
    direction: rule.direction,
    action: rule.action,
    matchType: rule.matchType,
    patternText: rule.patternText,
    category: rule.category,
    severity: rule.severity,
    enabled: rule.enabled,
    priority: rule.priority ?? 100
  })
}

const openCreateQuota = () => {
  dialogMode.value = 'create'
  dialogType.value = 'quota'
  currentId.value = null
  resetQuotaForm()
}

const openEditQuota = (quota: AdminAiUsageQuota) => {
  dialogMode.value = 'edit'
  dialogType.value = 'quota'
  currentId.value = quota.id
  Object.assign(quotaForm, {
    subjectType: quota.subjectType,
    subjectId: quota.subjectId || '',
    capabilityCode: quota.capabilityCode || '',
    sceneCode: quota.sceneCode || '',
    quotaPeriod: quota.quotaPeriod,
    maxCalls: quota.maxCalls,
    maxTokens: quota.maxTokens,
    maxCost: quota.maxCost,
    enabled: quota.enabled
  })
}

const openCreateCapability = () => {
  dialogMode.value = 'create'
  dialogType.value = 'capability'
  currentId.value = null
  resetCapabilityForm()
}

const openEditCapability = (capability: AdminAiCapabilityConfig) => {
  dialogMode.value = 'edit'
  dialogType.value = 'capability'
  currentId.value = capability.id
  Object.assign(capabilityForm, {
    capabilityCode: capability.capabilityCode,
    capabilityName: capability.capabilityName,
    enabled: capability.enabled,
    grayEnabled: capability.grayEnabled,
    grayRuleJson: capability.grayRuleJson || '',
    rateLimitJson: capability.rateLimitJson || '',
    quotaRuleJson: capability.quotaRuleJson || ''
  })
}

const openCreateScene = () => {
  dialogMode.value = 'create'
  dialogType.value = 'scene'
  currentId.value = null
  resetSceneForm()
}

const openEditScene = (scene: AdminAiSceneConfig) => {
  dialogMode.value = 'edit'
  dialogType.value = 'scene'
  currentId.value = scene.id
  Object.assign(sceneForm, {
    capabilityCode: scene.capabilityCode,
    sceneCode: scene.sceneCode,
    sceneName: scene.sceneName,
    providerCode: scene.providerCode || '',
    modelCode: scene.modelCode || '',
    enabled: scene.enabled,
    systemPromptTemplate: scene.systemPromptTemplate || '',
    inputSchemaJson: scene.inputSchemaJson || '',
    outputSchemaJson: scene.outputSchemaJson || '',
    safetyLevel: scene.safetyLevel || '',
    timeoutMs: scene.timeoutMs || 30000
  })
}

const normalizeOptional = (value?: string) => {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

const normalizeOptionalNumber = (value?: number) => {
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined
}

const validateOptionalJson = (value: string, label: string) => {
  const trimmed = value.trim()
  if (!trimmed) return true
  try {
    JSON.parse(trimmed)
    return true
  } catch (error) {
    void showAlert(`${label} 不是合法 JSON`)
    return false
  }
}

const submitProvider = async () => {
  const payload: AdminAiProviderUpsertPayload = {
    providerCode: providerForm.providerCode.trim(),
    providerName: providerForm.providerName.trim(),
    baseUrl: providerForm.baseUrl.trim(),
    defaultModelCode: providerForm.defaultModelCode.trim(),
    enabled: providerForm.enabled,
    timeoutMs: providerForm.timeoutMs,
    maxContextMessages: providerForm.maxContextMessages,
    temperature: providerForm.temperature,
    topP: providerForm.topP,
    maxOutputTokens: providerForm.maxOutputTokens,
    systemPromptTemplate: normalizeOptional(providerForm.systemPromptTemplate)
  }

  if (!payload.providerCode || !payload.providerName || !payload.baseUrl || !payload.defaultModelCode) {
    await showAlert('请完整填写 Provider 必填项')
    return
  }
  if (providerForm.apiKeyCipher.trim()) {
    payload.apiKeyCipher = providerForm.apiKeyCipher.trim()
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createAdminAiProvider(payload)
      await showAlert('Provider 创建成功')
    } else if (currentId.value) {
      await updateAdminAiProvider(currentId.value, payload)
      await showAlert('Provider 更新成功')
    }
    closeDialog()
    await loadAllData()
  } catch (error: any) {
    console.error('保存 Provider 失败:', error)
    await showAlert(error?.message || '保存 Provider 失败')
  } finally {
    submitting.value = false
  }
}

const submitModel = async () => {
  if (!validateOptionalJson(modelForm.capabilitiesJson, '模型能力')) {
    return
  }

  const payload: AdminAiModelUpsertPayload = {
    providerCode: modelForm.providerCode.trim(),
    modelCode: modelForm.modelCode.trim(),
    modelName: modelForm.modelName.trim(),
    capabilitiesJson: normalizeOptional(modelForm.capabilitiesJson),
    contextWindow: normalizeOptionalNumber(modelForm.contextWindow),
    maxOutputTokens: normalizeOptionalNumber(modelForm.maxOutputTokens),
    inputPricePer1k: normalizeOptionalNumber(modelForm.inputPricePer1k),
    outputPricePer1k: normalizeOptionalNumber(modelForm.outputPricePer1k),
    enabled: modelForm.enabled,
    priority: modelForm.priority
  }

  if (!payload.providerCode || !payload.modelCode || !payload.modelName) {
    await showAlert('请完整填写 Model 必填项')
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createAdminAiModel(payload)
      await showAlert('Model 创建成功')
    } else if (currentId.value) {
      await updateAdminAiModel(currentId.value, payload)
      await showAlert('Model 更新成功')
    }
    closeDialog()
    await loadAllData()
  } catch (error: any) {
    console.error('保存 Model 失败:', error)
    await showAlert(error?.message || '保存 Model 失败')
  } finally {
    submitting.value = false
  }
}

const submitRouteRule = async () => {
  if (
    !validateOptionalJson(routeRuleForm.matchRuleJson, '匹配规则') ||
    !validateOptionalJson(routeRuleForm.routeRuleJson, '路由规则')
  ) {
    return
  }

  const payload: AdminAiGatewayRouteRuleUpsertPayload = {
    ruleName: routeRuleForm.ruleName.trim(),
    capabilityCode: routeRuleForm.capabilityCode.trim(),
    sceneCode: normalizeOptional(routeRuleForm.sceneCode),
    matchRuleJson: normalizeOptional(routeRuleForm.matchRuleJson),
    routeRuleJson: routeRuleForm.routeRuleJson.trim(),
    enabled: routeRuleForm.enabled,
    priority: routeRuleForm.priority
  }

  if (!payload.ruleName || !payload.capabilityCode || !payload.routeRuleJson) {
    await showAlert('请完整填写 Route Rule 必填项')
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createAdminAiGatewayRouteRule(payload)
      await showAlert('Route Rule 创建成功')
    } else if (currentId.value) {
      await updateAdminAiGatewayRouteRule(currentId.value, payload)
      await showAlert('Route Rule 更新成功')
    }
    closeDialog()
    await loadAllData()
  } catch (error: any) {
    console.error('保存 Route Rule 失败:', error)
    await showAlert(error?.message || '保存 Route Rule 失败')
  } finally {
    submitting.value = false
  }
}

const submitSafetyRule = async () => {
  const payload: AdminAiGatewaySafetyRuleUpsertPayload = {
    ruleName: safetyRuleForm.ruleName.trim(),
    capabilityCode: normalizeOptional(safetyRuleForm.capabilityCode),
    sceneCode: normalizeOptional(safetyRuleForm.sceneCode),
    direction: safetyRuleForm.direction,
    action: safetyRuleForm.action,
    matchType: safetyRuleForm.matchType,
    patternText: safetyRuleForm.patternText.trim(),
    category: safetyRuleForm.category.trim(),
    severity: safetyRuleForm.severity,
    enabled: safetyRuleForm.enabled,
    priority: safetyRuleForm.priority
  }

  if (!payload.ruleName || !payload.patternText || !payload.category) {
    await showAlert('请完整填写安全规则必填项')
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createAdminAiGatewaySafetyRule(payload)
      await showAlert('安全规则创建成功')
    } else if (currentId.value) {
      await updateAdminAiGatewaySafetyRule(currentId.value, payload)
      await showAlert('安全规则更新成功')
    }
    closeDialog()
    await loadAllData()
  } catch (error: any) {
    console.error('保存安全规则失败:', error)
    await showAlert(error?.message || '保存安全规则失败')
  } finally {
    submitting.value = false
  }
}

const submitQuota = async () => {
  const payload: AdminAiUsageQuotaUpsertPayload = {
    subjectType: quotaForm.subjectType,
    subjectId: quotaForm.subjectType === 'GLOBAL' ? undefined : normalizeOptional(quotaForm.subjectId),
    capabilityCode: normalizeOptional(quotaForm.capabilityCode),
    sceneCode: normalizeOptional(quotaForm.sceneCode),
    quotaPeriod: quotaForm.quotaPeriod,
    maxCalls: normalizeOptionalNumber(quotaForm.maxCalls),
    maxTokens: normalizeOptionalNumber(quotaForm.maxTokens),
    maxCost: normalizeOptionalNumber(quotaForm.maxCost),
    enabled: quotaForm.enabled
  }

  if (payload.subjectType !== 'GLOBAL' && !payload.subjectId) {
    await showAlert('非全局配额必须填写主体 ID')
    return
  }
  if (!payload.maxCalls && !payload.maxTokens && !payload.maxCost) {
    await showAlert('配额至少需要填写一个上限')
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createAdminAiUsageQuota(payload)
      await showAlert('配额创建成功')
    } else if (currentId.value) {
      await updateAdminAiUsageQuota(currentId.value, payload)
      await showAlert('配额更新成功')
    }
    closeDialog()
    await loadAllData()
  } catch (error: any) {
    console.error('保存配额失败:', error)
    await showAlert(error?.message || '保存配额失败')
  } finally {
    submitting.value = false
  }
}

const submitCapability = async () => {
  if (
    !validateOptionalJson(capabilityForm.grayRuleJson, '灰度规则') ||
    !validateOptionalJson(capabilityForm.rateLimitJson, '限流规则') ||
    !validateOptionalJson(capabilityForm.quotaRuleJson, '配额规则')
  ) {
    return
  }

  const payload: AdminAiCapabilityUpsertPayload = {
    capabilityCode: capabilityForm.capabilityCode.trim(),
    capabilityName: capabilityForm.capabilityName.trim(),
    enabled: capabilityForm.enabled,
    grayEnabled: capabilityForm.grayEnabled,
    grayRuleJson: normalizeOptional(capabilityForm.grayRuleJson),
    rateLimitJson: normalizeOptional(capabilityForm.rateLimitJson),
    quotaRuleJson: normalizeOptional(capabilityForm.quotaRuleJson)
  }

  if (!payload.capabilityCode || !payload.capabilityName) {
    await showAlert('请完整填写 Capability 必填项')
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createAdminAiCapability(payload)
      await showAlert('Capability 创建成功')
    } else if (currentId.value) {
      await updateAdminAiCapability(currentId.value, payload)
      await showAlert('Capability 更新成功')
    }
    closeDialog()
    await loadAllData()
  } catch (error: any) {
    console.error('保存 Capability 失败:', error)
    await showAlert(error?.message || '保存 Capability 失败')
  } finally {
    submitting.value = false
  }
}

const submitScene = async () => {
  if (
    !validateOptionalJson(sceneForm.inputSchemaJson, '输入 Schema') ||
    !validateOptionalJson(sceneForm.outputSchemaJson, '输出 Schema')
  ) {
    return
  }

  const payload: AdminAiSceneUpsertPayload = {
    capabilityCode: sceneForm.capabilityCode.trim(),
    sceneCode: sceneForm.sceneCode.trim(),
    sceneName: sceneForm.sceneName.trim(),
    providerCode: normalizeOptional(sceneForm.providerCode),
    modelCode: normalizeOptional(sceneForm.modelCode),
    enabled: sceneForm.enabled,
    systemPromptTemplate: normalizeOptional(sceneForm.systemPromptTemplate),
    inputSchemaJson: normalizeOptional(sceneForm.inputSchemaJson),
    outputSchemaJson: normalizeOptional(sceneForm.outputSchemaJson),
    safetyLevel: normalizeOptional(sceneForm.safetyLevel),
    timeoutMs: sceneForm.timeoutMs
  }

  if (!payload.capabilityCode || !payload.sceneCode || !payload.sceneName) {
    await showAlert('请完整填写 Scene 必填项')
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createAdminAiScene(payload)
      await showAlert('Scene 创建成功')
    } else if (currentId.value) {
      await updateAdminAiScene(currentId.value, payload)
      await showAlert('Scene 更新成功')
    }
    closeDialog()
    await loadAllData()
  } catch (error: any) {
    console.error('保存 Scene 失败:', error)
    await showAlert(error?.message || '保存 Scene 失败')
  } finally {
    submitting.value = false
  }
}

const removeProvider = async (provider: AdminAiProviderConfig) => {
  const confirmed = await showConfirm(`确定删除 Provider「${provider.providerName}」吗？`)
  if (!confirmed) return
  try {
    await deleteAdminAiProvider(provider.id)
    await showAlert('Provider 删除成功')
    await loadAllData()
  } catch (error: any) {
    console.error('删除 Provider 失败:', error)
    await showAlert(error?.message || '删除 Provider 失败')
  }
}

const removeModel = async (model: AdminAiModelConfig) => {
  const confirmed = await showConfirm(`确定删除 Model「${model.modelName}」吗？`)
  if (!confirmed) return
  try {
    await deleteAdminAiModel(model.id)
    await showAlert('Model 删除成功')
    await loadAllData()
  } catch (error: any) {
    console.error('删除 Model 失败:', error)
    await showAlert(error?.message || '删除 Model 失败')
  }
}

const removeRouteRule = async (rule: AdminAiGatewayRouteRule) => {
  const confirmed = await showConfirm(`确定删除 Route Rule「${rule.ruleName}」吗？`)
  if (!confirmed) return
  try {
    await deleteAdminAiGatewayRouteRule(rule.id)
    await showAlert('Route Rule 删除成功')
    await loadAllData()
  } catch (error: any) {
    console.error('删除 Route Rule 失败:', error)
    await showAlert(error?.message || '删除 Route Rule 失败')
  }
}

const removeSafetyRule = async (rule: AdminAiGatewaySafetyRule) => {
  const confirmed = await showConfirm(`确定删除安全规则「${rule.ruleName}」吗？`)
  if (!confirmed) return
  try {
    await deleteAdminAiGatewaySafetyRule(rule.id)
    await showAlert('安全规则删除成功')
    await loadAllData()
  } catch (error: any) {
    console.error('删除安全规则失败:', error)
    await showAlert(error?.message || '删除安全规则失败')
  }
}

const removeQuota = async (quota: AdminAiUsageQuota) => {
  const confirmed = await showConfirm(`确定删除 ${quota.subjectType} 配额吗？`)
  if (!confirmed) return
  try {
    await deleteAdminAiUsageQuota(quota.id)
    await showAlert('配额删除成功')
    await loadAllData()
  } catch (error: any) {
    console.error('删除配额失败:', error)
    await showAlert(error?.message || '删除配额失败')
  }
}

const removeCapability = async (capability: AdminAiCapabilityConfig) => {
  const confirmed = await showConfirm(`确定删除 Capability「${capability.capabilityName}」吗？`)
  if (!confirmed) return
  try {
    await deleteAdminAiCapability(capability.id)
    await showAlert('Capability 删除成功')
    await loadAllData()
  } catch (error: any) {
    console.error('删除 Capability 失败:', error)
    await showAlert(error?.message || '删除 Capability 失败')
  }
}

const removeScene = async (scene: AdminAiSceneConfig) => {
  const confirmed = await showConfirm(`确定删除 Scene「${scene.sceneName}」吗？`)
  if (!confirmed) return
  try {
    await deleteAdminAiScene(scene.id)
    await showAlert('Scene 删除成功')
    await loadAllData()
  } catch (error: any) {
    console.error('删除 Scene 失败:', error)
    await showAlert(error?.message || '删除 Scene 失败')
  }
}

const previewText = (value?: string) => {
  const text = value?.trim()
  if (!text) return '-'
  return text.length > 36 ? `${text.slice(0, 36)}...` : text
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

const formatNumber = (value?: number) => new Intl.NumberFormat('zh-CN').format(value || 0)
const formatMoney = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `¥${Number(value).toFixed(4)}`
}
const formatModerationScore = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return Number(value).toFixed(2)
}
const formatModelPrice = (model: AdminAiModelConfig) => {
  if (model.inputPricePer1k === undefined && model.outputPricePer1k === undefined) return '-'
  return `${model.inputPricePer1k ?? '-'} / ${model.outputPricePer1k ?? '-'}`
}
const shrink = (value?: string, max = 20) => {
  if (!value) return '-'
  return value.length > max ? `${value.slice(0, max)}...` : value
}

onMounted(() => {
  applyTabFromQuery(route.query.tab)
  loadAllData()
  loadOpsSnapshot()
})

watch(() => route.query.tab, applyTabFromQuery)
watch(() => quotaForm.subjectType, (subjectType) => {
  if (subjectType === 'GLOBAL') {
    quotaForm.subjectId = ''
  }
})
</script>

<style scoped>
.admin-ai {
  width: 100%;
  color: #1f2937;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 700;
  color: #111827;
}

.page-description {
  margin: 0;
  color: #6b7280;
  line-height: 1.6;
}

.header-actions,
.inline-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #dbeafe;
  border-radius: 16px;
  padding: 18px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
}

.overview-card.success {
  border-color: #bbf7d0;
  background: linear-gradient(180deg, #ffffff 0%, #f2fff6 100%);
}

.overview-card.danger {
  border-color: #fecaca;
  background: linear-gradient(180deg, #ffffff 0%, #fff5f5 100%);
}

.overview-label,
.overview-footnote {
  display: block;
  font-size: 13px;
  color: #6b7280;
}

.overview-value {
  display: block;
  margin: 10px 0 4px;
  font-size: 28px;
  line-height: 1.1;
  color: #111827;
}

.tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.tab-button {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #4b5563;
  padding: 10px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-button.active {
  border-color: #1677ff;
  background: #1677ff;
  color: #fff;
}

.panel {
  background: #fff;
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

.panel-header,
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-title,
.section-head h3 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #111827;
}

.panel-description,
.summary-sub,
.minor-text {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.summary-grid,
.split-grid {
  display: grid;
  gap: 16px;
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  margin-bottom: 18px;
}

.split-grid {
  grid-template-columns: 1.2fr 1fr;
}

.summary-card,
.card-panel {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 18px;
  background: #fbfdff;
}

.summary-card h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.summary-main {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.status-tag,
.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 600;
}

.status-tag.enabled,
.status-pill.active {
  background: #dcfce7;
  color: #166534;
}

.status-tag.disabled,
.status-pill.inactive {
  background: #f3f4f6;
  color: #6b7280;
}

.status-pill.danger {
  background: #fee2e2;
  color: #991b1b;
}

.mini-log-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mini-log-item {
  display: grid;
  grid-template-columns: 150px 1fr auto;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #eef2f7;
  font-size: 13px;
}

.mini-log-item:last-child {
  border-bottom: none;
}

.mini-log-result.success {
  color: #16a34a;
}

.mini-log-result.failed {
  color: #dc2626;
}

.table-shell {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  background: #f8fafc;
  color: #475569;
  text-align: left;
  padding: 12px;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
}

.data-table td {
  padding: 12px;
  border-bottom: 1px solid #eef2f7;
  vertical-align: top;
}

.data-table tbody tr:hover {
  background: #f9fbff;
}

.mono {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

.truncate-cell {
  max-width: 220px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-cell {
  white-space: nowrap;
}

.empty-row,
.empty-state {
  text-align: center;
  color: #94a3b8;
  padding: 20px;
}

.btn,
.text-button,
.link-button,
.close-button,
select,
input,
textarea {
  font: inherit;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: #1677ff;
  color: #fff;
  border-radius: 10px;
  padding: 10px 16px;
  cursor: pointer;
  text-decoration: none;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.btn-secondary {
  background: #eef2f7;
  color: #334155;
}

.text-button,
.link-button,
.close-button {
  border: none;
  background: transparent;
  cursor: pointer;
}

.text-button {
  color: #1677ff;
  margin-right: 10px;
}

.text-button.danger {
  color: #dc2626;
}

.link-button {
  color: #1677ff;
  padding: 0;
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.48);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
  padding: 20px;
}

.dialog {
  width: min(920px, 100%);
  max-height: 88vh;
  overflow-y: auto;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.24);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 0;
}

.dialog-header h3 {
  margin: 0;
  font-size: 22px;
}

.close-button {
  font-size: 28px;
  line-height: 1;
  color: #64748b;
}

.dialog-form {
  padding: 18px 24px 24px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-item,
.form-checkbox {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item span,
.form-checkbox span {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.form-item input,
.form-item textarea,
.form-item select,
.inline-actions select {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fff;
  color: #111827;
}

.compact-table th,
.compact-table td {
  padding: 10px;
  font-size: 13px;
}

.textarea-item textarea {
  resize: vertical;
}

.form-checkbox {
  flex-direction: row;
  align-items: center;
  padding-top: 30px;
}

.form-checkbox input {
  width: 16px;
  height: 16px;
  margin: 0;
}

.span-2 {
  grid-column: span 2;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.probe-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(380px, 0.9fr);
  gap: 18px;
}

.moderation-layout {
  margin-top: 18px;
}

.probe-result-panel {
  min-height: 360px;
}

.probe-result {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.probe-meta-grid {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  gap: 10px 14px;
  font-size: 14px;
}

.probe-meta-grid span {
  color: #64748b;
}

.probe-meta-grid strong {
  min-width: 0;
  word-break: break-word;
}

.probe-output,
.probe-error {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
  padding: 14px;
}

.probe-output {
  margin: 0;
  min-height: 120px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  line-height: 1.7;
}

.probe-error {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.probe-error p {
  margin: 0;
  color: #334155;
  line-height: 1.6;
}

.moderation-category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.moderation-category {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
}

.ops-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.9fr);
  gap: 18px;
}

.ops-left,
.ops-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ops-question textarea {
  min-height: 108px;
  resize: vertical;
}

.ops-options {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}

.ops-options label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #334155;
  font-size: 14px;
}

.ops-options select {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 8px 10px;
  background: #fff;
}

.ops-section {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px;
  background: #fbfdff;
}

.section-head.compact {
  align-items: center;
  margin-bottom: 12px;
}

.section-head.compact h3 {
  margin: 0;
  font-size: 16px;
}

.ops-service-list,
.ops-alert-list,
.ops-data-source-list,
.ops-log-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ops-service-row,
.ops-data-source-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #eef2f7;
}

.ops-service-row:last-child,
.ops-data-source-row:last-child {
  border-bottom: none;
}

.ops-alert-item {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.ops-alert-title {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.ops-alert-item p,
.ops-log-item p {
  margin: 8px 0 0;
  color: #334155;
  line-height: 1.5;
}

.ops-provider-grid {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 10px 14px;
  font-size: 14px;
}

.ops-provider-grid span {
  color: #64748b;
}

.success-text {
  color: #16a34a;
}

.danger-text {
  color: #dc2626;
}

.ops-log-item {
  display: grid;
  grid-template-columns: auto 120px 1fr;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 0;
  border-bottom: 1px solid #eef2f7;
}

.ops-log-item:last-child {
  border-bottom: none;
}

.report-section {
  min-height: 260px;
}

.ops-report {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  line-height: 1.7;
  color: #111827;
}

@media (max-width: 960px) {
  .split-grid {
    grid-template-columns: 1fr;
  }

  .ops-layout {
    grid-template-columns: 1fr;
  }

  .probe-layout {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }
}

@media (max-width: 768px) {
  .page-header,
  .panel-header,
  .section-head {
    flex-direction: column;
  }

  .header-actions,
  .inline-actions {
    width: 100%;
  }

  .header-actions .btn,
  .inline-actions .btn,
  .inline-actions select {
    width: 100%;
  }

  .panel {
    padding: 18px;
  }

  .mini-log-item {
    grid-template-columns: 1fr;
  }

  .ops-log-item {
    grid-template-columns: 1fr;
  }
}
</style>
