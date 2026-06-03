<template>
  <div class="merchant-home-page">
    <div class="page-header">
      <div v-if="homeData">
        <p class="eyebrow">Merchant Console</p>
        <h1>{{ homeData.managementHome.title }}</h1>
        <p class="subtitle">{{ homeData.managementHome.description }}</p>
      </div>
      <button type="button" class="ghost-button" @click="loadHome" :disabled="loading">
        {{ loading ? '加载中...' : '刷新数据' }}
      </button>
    </div>

    <div v-if="loading && !homeData" class="page-state">正在加载商家首页...</div>
    <div v-else-if="errorMessage" class="page-state error">
      <p>{{ errorMessage }}</p>
      <button type="button" class="retry-button" @click="loadHome">重新加载</button>
    </div>
    <template v-else-if="homeData">
      <section class="hero-panel">
        <div class="merchant-summary">
          <span class="summary-badge">{{ homeData.merchant.statusLabel }}</span>
          <h2>{{ homeData.merchant.name }}</h2>
          <p>
            {{ homeData.merchant.typeName || '未配置分类' }}
            <span v-if="locationText"> · {{ locationText }}</span>
          </p>
        </div>
        <dl class="merchant-meta">
          <div>
            <dt>联系人</dt>
            <dd>{{ homeData.merchant.contactName || '-' }}</dd>
          </div>
          <div>
            <dt>联系电话</dt>
            <dd>{{ homeData.merchant.contactPhone || '-' }}</dd>
          </div>
          <div>
            <dt>定位状态</dt>
            <dd>{{ homeData.merchant.locationStatusLabel || '-' }}</dd>
          </div>
          <div>
            <dt>入驻时间</dt>
            <dd>{{ formatDateTime(homeData.merchant.createTime) }}</dd>
          </div>
        </dl>
      </section>

      <section class="content-section">
        <div class="section-heading">
          <div>
            <p class="section-kicker">Console</p>
            <h3>{{ homeData.managementHome.title }}</h3>
          </div>
          <RouterLink class="section-link" :to="homeData.managementHome.path">当前所在入口</RouterLink>
        </div>

        <div class="stats-grid">
          <article class="stat-card accent">
            <span>优惠券总数</span>
            <strong>{{ homeData.managementHome.stats.totalVouchers }}</strong>
            <p>{{ homeData.managementHome.stats.onlineVouchers }} 个上架中</p>
          </article>
          <article class="stat-card">
            <span>待审核退款</span>
            <strong>{{ homeData.managementHome.stats.pendingRefunds }}</strong>
            <p>{{ homeData.managementHome.stats.escalatedRefunds }} 个已转平台</p>
          </article>
          <article class="stat-card">
            <span>累计售出</span>
            <strong>{{ homeData.managementHome.stats.totalSoldCount }}</strong>
            <p>{{ homeData.managementHome.stats.successRefunds }} 笔退款已完成</p>
          </article>
          <article class="stat-card">
            <span>运营中任务</span>
            <strong>{{ homeData.managementHome.stats.processingRefunds }}</strong>
            <p>{{ homeData.managementHome.stats.offlineVouchers }} 个券当前下架</p>
          </article>
        </div>

        <div class="action-grid">
          <RouterLink
            v-for="action in visibleQuickActions"
            :key="action.key"
            class="action-card"
            :to="action.path"
          >
            <strong>{{ action.label }}</strong>
            <p>{{ action.description }}</p>
          </RouterLink>
        </div>
      </section>

      <section class="content-columns">
        <div class="list-panel">
          <div class="panel-heading">
            <h3>最近更新的券</h3>
            <RouterLink to="/merchant/vouchers">查看全部</RouterLink>
          </div>
          <div v-if="homeData.recentVouchers.length" class="item-list">
            <article v-for="voucher in homeData.recentVouchers" :key="voucher.id" class="list-item">
              <div>
                <strong>{{ voucher.title }}</strong>
                <p>{{ voucher.statusLabel }} · 库存 {{ voucher.stock ?? 0 }} · 已售 {{ voucher.soldCount ?? 0 }}</p>
              </div>
              <time>{{ formatDateTime(voucher.updateTime) }}</time>
            </article>
          </div>
          <div v-else class="empty-state">还没有优惠券数据。</div>
        </div>

        <div class="list-panel">
          <div class="panel-heading">
            <h3>最近退款单</h3>
            <RouterLink to="/merchant/refunds">查看全部</RouterLink>
          </div>
          <div v-if="homeData.recentRefunds.length" class="item-list">
            <article v-for="refund in homeData.recentRefunds" :key="refund.refundNo" class="list-item">
              <div>
                <strong>{{ refund.voucherTitle || refund.refundNo }}</strong>
                <p>{{ refund.statusLabel }} · 申请金额 {{ formatCurrency(refund.requestedAmount) }}</p>
              </div>
              <time>{{ formatDateTime(refund.createTime) }}</time>
            </article>
          </div>
          <div v-else class="empty-state">还没有退款单数据。</div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getMerchantHome, type MerchantHomeSnapshot } from '../../api/merchant'

const homeData = ref<MerchantHomeSnapshot | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const loadHome = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    homeData.value = await getMerchantHome()
  } catch (error: any) {
    console.error('加载商家首页失败:', error)
    errorMessage.value = error?.response?.data?.message || '加载商家首页失败'
  } finally {
    loading.value = false
  }
}

const locationText = computed(() => {
  const merchant = homeData.value?.merchant
  if (!merchant) return ''
  const parts = [merchant.city, merchant.district, merchant.address].filter(Boolean)
  return parts.join(' ')
})

const visibleQuickActions = computed(() => {
  return (homeData.value?.managementHome.quickActions || []).filter(action => {
    return !['publish', 'discover', 'message'].includes(action.key)
      && !action.path.startsWith('/posts')
      && action.path !== '/home'
      && action.path !== '/message'
  })
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

const formatCurrency = (value?: number) => {
  const amount = Number(value ?? 0)
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : '-'
}

onMounted(() => {
  void loadHome()
})
</script>

<style scoped>
.merchant-home-page {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef6ff 100%);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.page-header h1 {
  margin: 0;
  font-size: 30px;
  color: #0f172a;
}

.subtitle {
  margin: 10px 0 0;
  color: #475569;
  font-size: 14px;
}

.ghost-button,
.retry-button,
.section-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
}

.ghost-button:disabled {
  opacity: 0.7;
  cursor: wait;
}

.page-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
}

.page-state.error {
  flex-direction: column;
  gap: 12px;
  color: #b91c1c;
}

.hero-panel,
.content-section,
.list-panel {
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.05);
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 20px;
  padding: 24px;
  margin-bottom: 20px;
}

.summary-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #dcfce7;
  color: #166534;
  font-size: 12px;
  font-weight: 700;
}

.merchant-summary h2 {
  margin: 14px 0 8px;
  font-size: 28px;
  color: #0f172a;
}

.merchant-summary p {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

.merchant-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.merchant-meta div {
  padding: 16px;
  border-radius: 8px;
  background: #f8fafc;
}

.merchant-meta dt {
  margin: 0 0 8px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.merchant-meta dd {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
}

.content-section {
  padding: 20px;
  margin-bottom: 20px;
}

.section-heading,
.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-heading {
  margin-bottom: 18px;
}

.section-kicker {
  margin: 0 0 4px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.section-heading h3,
.panel-heading h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.stats-grid,
.action-grid,
.content-columns {
  display: grid;
  gap: 16px;
}

.stats-grid,
.content-columns {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.action-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 16px;
}

.stat-card,
.action-card,
.list-item {
  border: 1px solid #dbe4f0;
  border-radius: 8px;
}

.stat-card {
  padding: 18px;
  background: #f8fafc;
}

.stat-card.accent {
  background: linear-gradient(145deg, #0f766e, #0f172a);
  border-color: transparent;
  color: #fff;
}

.stat-card span {
  display: block;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 700;
}

.stat-card strong {
  display: block;
  font-size: 28px;
}

.stat-card p {
  margin: 10px 0 0;
  color: inherit;
  opacity: 0.82;
}

.action-card {
  display: block;
  padding: 18px;
  background: #fff;
  color: #0f172a;
  text-decoration: none;
}

.action-card strong {
  display: block;
  font-size: 17px;
}

.action-card p,
.list-item p,
.empty-state {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.6;
}

.list-panel {
  padding: 20px;
}

.panel-heading {
  margin-bottom: 14px;
}

.panel-heading a {
  color: #0f766e;
  text-decoration: none;
  font-weight: 700;
}

.item-list {
  display: grid;
  gap: 12px;
}

.list-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
}

.list-item strong {
  color: #0f172a;
}

.list-item time {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.empty-state {
  padding: 18px;
  border-radius: 8px;
  background: #f8fafc;
}

@media (max-width: 768px) {
  .merchant-home-page {
    padding: 12px;
  }

  .page-header,
  .hero-panel,
  .stats-grid,
  .action-grid,
  .content-columns,
  .merchant-meta {
    grid-template-columns: 1fr;
  }

  .page-header,
  .section-heading,
  .panel-heading,
  .list-item {
    flex-direction: column;
    align-items: stretch;
  }

  .section-link,
  .ghost-button,
  .retry-button {
    width: 100%;
  }

  .list-item time {
    white-space: normal;
  }

  .stats-grid,
  .action-grid,
  .content-columns {
    grid-template-columns: 1fr;
  }
}
</style>
