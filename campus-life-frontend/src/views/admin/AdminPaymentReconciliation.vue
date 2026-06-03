<template>
  <div class="admin-payment-reconciliation">
    <section class="hero">
      <div>
        <p class="eyebrow">Payment Ops</p>
        <h1>支付对账</h1>
        <p class="hero-desc">扫描本地支付单、退款单和业务订单的一致性，优先处理开放中的差错单。</p>
      </div>
      <div class="hero-actions">
        <select v-model="scanDays">
          <option :value="3">近 3 天</option>
          <option :value="7">近 7 天</option>
          <option :value="15">近 15 天</option>
          <option :value="30">近 30 天</option>
        </select>
        <button class="primary-btn" :disabled="scanning" @click="handleScan">{{ scanning ? '扫描中...' : '立即扫描' }}</button>
      </div>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <span>差错总数</span>
        <strong>{{ summary.totalIssues }}</strong>
      </article>
      <article class="stat-card">
        <span>待处理</span>
        <strong class="warn">{{ summary.openIssues }}</strong>
      </article>
      <article class="stat-card">
        <span>严重待处理</span>
        <strong class="danger">{{ summary.criticalOpenIssues }}</strong>
      </article>
      <article class="stat-card">
        <span>已解决</span>
        <strong class="ok">{{ summary.resolvedIssues }}</strong>
      </article>
    </section>

    <section class="toolbar">
      <select v-model="statusFilter" @change="loadIssues">
        <option value="">全部状态</option>
        <option value="OPEN">待处理</option>
        <option value="RESOLVED">已解决</option>
      </select>
      <select v-model="issueTypeFilter" @change="loadIssues">
        <option value="">全部类型</option>
        <option value="PAYMENT_SUCCESS_ORDER_STATE_MISMATCH">支付成功订单状态不一致</option>
        <option value="ORDER_PAID_PAYMENT_STATUS_MISMATCH">订单已支付但支付单未成功</option>
        <option value="REFUND_AMOUNT_MISMATCH">退款金额不一致</option>
        <option value="FULL_REFUND_ORDER_STATE_MISMATCH">全额退款订单状态不一致</option>
        <option value="MISSING_PAYMENT_ORDER">缺少支付单</option>
      </select>
      <button class="ghost-btn" @click="loadIssues">刷新列表</button>
    </section>

    <section class="shell">
      <div class="list-panel">
        <div v-if="loading" class="state-card">差错单加载中...</div>
        <div v-else-if="error" class="state-card error">{{ error }}</div>
        <div v-else-if="!issues.length" class="state-card">当前没有对账差错</div>

        <article
          v-for="item in issues"
          :key="item.id"
          :class="['issue-item', { active: selectedIssueId === item.id }]"
          @click="selectIssue(item.id)"
        >
          <div class="item-head">
            <div>
              <p class="item-type">{{ issueTypeText(item.issueType) }}</p>
              <h3>{{ item.bizOrderNo }}</h3>
            </div>
            <span :class="['level-chip', item.issueLevel?.toLowerCase()]">{{ item.issueLevel }}</span>
          </div>
          <p class="item-meta">支付单：{{ item.paymentNo || '缺失' }}</p>
          <p class="item-meta">{{ item.issueMessage }}</p>
          <div class="item-foot">
            <span :class="['status-chip', item.issueStatus?.toLowerCase()]">{{ issueStatusText(item.issueStatus) }}</span>
            <span>{{ formatTime(item.lastCheckedTime) }}</span>
          </div>
        </article>
      </div>

      <div class="detail-panel">
        <div v-if="!selectedIssue" class="state-card">请选择左侧差错单查看详情</div>
        <template v-else>
          <div class="detail-hero">
            <div>
              <p class="eyebrow">Issue Detail</p>
              <h2>{{ issueTypeText(selectedIssue.issueType) }}</h2>
              <p>{{ selectedIssue.issueMessage }}</p>
            </div>
            <span :class="['status-chip', selectedIssue.issueStatus?.toLowerCase()]">{{ issueStatusText(selectedIssue.issueStatus) }}</span>
          </div>

          <div class="detail-card">
            <h3>基础信息</h3>
            <p>业务订单号：{{ selectedIssue.bizOrderNo }}</p>
            <p>支付单号：{{ selectedIssue.paymentNo || '缺失' }}</p>
            <p>用户ID：{{ selectedIssue.userId || '-' }}</p>
            <p>支付渠道：{{ paymentMethodText(selectedIssue.channel) }}</p>
            <p>最后扫描：{{ formatTime(selectedIssue.lastCheckedTime) }}</p>
          </div>

          <div class="detail-card">
            <h3>状态快照</h3>
            <p>支付单状态：{{ selectedIssue.paymentOrderStatus || '-' }}</p>
            <p>订单状态：{{ voucherOrderStatusText(selectedIssue.voucherOrderStatus) }}</p>
            <p>订单支付状态：{{ voucherPaymentStatusText(selectedIssue.voucherPaymentStatus) }}</p>
            <p>支付金额：¥{{ amountText(selectedIssue.paymentAmount) }}</p>
            <p>支付单退款金额：¥{{ amountText(selectedIssue.recordedRefundedAmount) }}</p>
            <p>成功退款累计：¥{{ amountText(selectedIssue.actualRefundedAmount) }}</p>
          </div>

          <div class="detail-card" v-if="selectedIssue.issueStatus === 'OPEN'">
            <h3>处理动作</h3>
            <textarea
              v-model.trim="resolveNote"
              class="text-area"
              maxlength="120"
              placeholder="记录排查结果，例如已回填支付单、已手工修正订单状态"
            />
            <div class="action-row">
              <button class="primary-btn" :disabled="resolving" @click="handleResolve">标记已处理</button>
            </div>
          </div>

          <div class="detail-card" v-else>
            <h3>处理记录</h3>
            <p>处理时间：{{ formatTime(selectedIssue.resolvedTime) }}</p>
            <p>处理人：{{ selectedIssue.resolvedBy || '-' }}</p>
            <p>处理备注：{{ selectedIssue.resolveNote || '-' }}</p>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  getPaymentReconciliationIssues,
  resolvePaymentReconciliationIssue,
  triggerPaymentReconciliationScan,
  type PaymentReconciliationIssueRecord
} from '@/api/admin'
import { showAlert, showConfirm } from '@/utils/dialog'

const loading = ref(false)
const scanning = ref(false)
const resolving = ref(false)
const error = ref<string | null>(null)
const scanDays = ref(7)
const statusFilter = ref('')
const issueTypeFilter = ref('')
const resolveNote = ref('')
const issues = ref<PaymentReconciliationIssueRecord[]>([])
const summary = ref({
  totalIssues: 0,
  openIssues: 0,
  resolvedIssues: 0,
  criticalOpenIssues: 0
})
const selectedIssueId = ref<number | null>(null)

const selectedIssue = computed(() => issues.value.find(item => item.id === selectedIssueId.value) || null)

const amountText = (value?: number) => Number(value || 0).toFixed(2)
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'

const paymentMethodText = (value?: string) => {
  if (value === 'alipay') return '支付宝'
  if (value === 'wechat') return '微信支付'
  if (value === 'wallet') return '钱包支付'
  return value || '-'
}

const issueStatusText = (value?: string) => value === 'RESOLVED' ? '已解决' : '待处理'

const voucherOrderStatusText = (value?: number) => {
  if (value === 0) return '待支付'
  if (value === 1) return '待使用'
  if (value === 2) return '已核销'
  if (value === 3) return '已过期'
  if (value === 4) return '已退款'
  if (value === 5) return '已取消'
  return '-'
}

const voucherPaymentStatusText = (value?: number) => {
  if (value === 0) return '待支付'
  if (value === 1) return '支付成功'
  if (value === 2) return '支付失败'
  if (value === 3) return '已退款'
  return '-'
}

const issueTypeText = (value?: string) => {
  switch (value) {
    case 'PAYMENT_SUCCESS_ORDER_STATE_MISMATCH': return '支付成功订单状态不一致'
    case 'ORDER_PAID_PAYMENT_STATUS_MISMATCH': return '订单已支付但支付单未成功'
    case 'REFUND_AMOUNT_MISMATCH': return '退款金额不一致'
    case 'FULL_REFUND_ORDER_STATE_MISMATCH': return '全额退款订单状态不一致'
    case 'MISSING_PAYMENT_ORDER': return '缺少支付单'
    default: return value || '差错'
  }
}

const selectIssue = (id: number) => {
  selectedIssueId.value = id
  resolveNote.value = ''
}

const loadIssues = async () => {
  loading.value = true
  error.value = null
  try {
    const result = await getPaymentReconciliationIssues({
      status: statusFilter.value || undefined,
      issueType: issueTypeFilter.value || undefined
    })
    summary.value = result.summary
    issues.value = result.list || []
    if (!issues.value.length) {
      selectedIssueId.value = null
      return
    }
    if (!selectedIssueId.value || !issues.value.some(item => item.id === selectedIssueId.value)) {
      selectedIssueId.value = issues.value[0].id
    }
  } catch (e: any) {
    error.value = e?.message || '加载对账差错失败'
  } finally {
    loading.value = false
  }
}

const handleScan = async () => {
  if (!(await showConfirm(`确认扫描近 ${scanDays.value} 天的支付对账数据吗？`, '执行对账扫描'))) {
    return
  }
  scanning.value = true
  try {
    const result = await triggerPaymentReconciliationScan({ days: scanDays.value, limit: 500 })
    await showAlert(`扫描完成：扫描 ${result.scannedCount} 条，新增/重开 ${result.openedCount} 条，自动关闭 ${result.resolvedCount} 条`)
    await loadIssues()
  } catch (e: any) {
    await showAlert(e?.message || '对账扫描失败')
  } finally {
    scanning.value = false
  }
}

const handleResolve = async () => {
  if (!selectedIssue.value) return
  if (!(await showConfirm('确认将该差错单标记为已处理吗？后续扫描如果仍然异常，会再次打开。', '标记已处理'))) {
    return
  }
  resolving.value = true
  try {
    await resolvePaymentReconciliationIssue(selectedIssue.value.id, { note: resolveNote.value || undefined })
    await showAlert('已标记为已处理')
    await loadIssues()
  } catch (e: any) {
    await showAlert(e?.message || '处理失败')
  } finally {
    resolving.value = false
  }
}

onMounted(() => {
  void loadIssues()
})
</script>

<style scoped>
.admin-payment-reconciliation { width: 100%; }
.hero { display: flex; justify-content: space-between; gap: 16px; align-items: center; margin-bottom: 16px; padding: 18px; border-radius: 16px; background: linear-gradient(135deg, #0f172a, #1d4ed8); color: #fff; }
.eyebrow { margin: 0; font-size: 12px; text-transform: uppercase; letter-spacing: .08em; color: rgba(255,255,255,.72); }
.hero h1 { margin: 4px 0 0; font-size: 28px; }
.hero-desc { margin: 8px 0 0; color: rgba(255,255,255,.78); max-width: 680px; font-size: 13px; }
.hero-actions { display: flex; gap: 10px; align-items: center; }
.hero-actions select,.primary-btn,.ghost-btn,.text-area,.toolbar select { border-radius: 12px; border: 1px solid #dbe4f0; }
.hero-actions select,.primary-btn,.ghost-btn,.toolbar select { height: 40px; padding: 0 14px; }
.primary-btn { border: none; background: linear-gradient(135deg, #22c55e, #16a34a); color: #fff; cursor: pointer; font-weight: 700; }
.ghost-btn { background: #fff; cursor: pointer; }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 16px; }
.stat-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 16px; box-shadow: 0 2px 12px rgba(15,23,42,.05); }
.stat-card span { color: #64748b; font-size: 13px; }
.stat-card strong { display: block; margin-top: 8px; font-size: 28px; color: #0f172a; }
.stat-card strong.warn { color: #d97706; }
.stat-card strong.danger { color: #dc2626; }
.stat-card strong.ok { color: #16a34a; }
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.toolbar select { background: #fff; }
.shell { display: grid; grid-template-columns: 400px minmax(0,1fr); gap: 16px; align-items: start; }
.list-panel,.detail-panel { background: #fff; border-radius: 16px; border: 1px solid #e2e8f0; box-shadow: 0 2px 12px rgba(15,23,42,.05); padding: 14px; }
.state-card { padding: 18px; border-radius: 12px; background: #f8fafc; color: #475569; }
.state-card.error { color: #b91c1c; }
.issue-item { border: 1px solid #e2e8f0; border-radius: 14px; padding: 14px; cursor: pointer; background: #fff; }
.issue-item + .issue-item { margin-top: 12px; }
.issue-item.active { border-color: #1d4ed8; box-shadow: 0 0 0 3px rgba(29,78,216,.12); }
.item-head { display: flex; justify-content: space-between; gap: 10px; }
.item-type { margin: 0; font-size: 12px; color: #64748b; }
.item-head h3 { margin: 4px 0 0; font-size: 16px; color: #0f172a; }
.level-chip,.status-chip { border-radius: 999px; padding: 5px 10px; font-size: 12px; font-weight: 700; white-space: nowrap; }
.level-chip.info { background: #e0f2fe; color: #0369a1; }
.level-chip.warn { background: #fef3c7; color: #b45309; }
.level-chip.critical { background: #fee2e2; color: #b91c1c; }
.status-chip.open { background: #fff7ed; color: #c2410c; }
.status-chip.resolved { background: #dcfce7; color: #15803d; }
.item-meta { margin: 8px 0 0; color: #475569; font-size: 13px; }
.item-foot { margin-top: 10px; display: flex; justify-content: space-between; gap: 10px; align-items: center; color: #64748b; font-size: 12px; }
.detail-hero { border-radius: 16px; padding: 16px; background: linear-gradient(145deg, #111827, #1f2937); color: #fff; display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.detail-hero h2 { margin: 4px 0 0; font-size: 24px; }
.detail-hero p:last-child { margin: 8px 0 0; color: rgba(255,255,255,.78); font-size: 13px; }
.detail-card { margin-top: 14px; border: 1px solid #e2e8f0; border-radius: 14px; padding: 14px; }
.detail-card h3 { margin: 0 0 12px; font-size: 15px; color: #0f172a; }
.detail-card p { margin: 8px 0 0; color: #475569; font-size: 13px; line-height: 1.6; }
.text-area { width: 100%; min-height: 100px; padding: 12px; box-sizing: border-box; resize: vertical; }
.action-row { margin-top: 12px; display: flex; gap: 10px; }
@media (max-width: 1100px) { .shell { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .hero, .toolbar { flex-direction: column; align-items: stretch; } .hero-actions { width: 100%; } }
</style>
