<template>
  <div class="admin-refund-manage">
    <section class="page-header">
      <div class="header-left">
        <button class="back-button" type="button" @click="goBack" aria-label="返回">
          <span class="back-icon">‹</span>
        </button>
        <div>
          <p class="eyebrow">Admin Console</p>
          <h1>退款审核</h1>
        </div>
      </div>
    </section>

    <section class="filter-section">
      <select v-model="statusFilter" @change="loadRefunds">
        <option value="">全部状态</option>
        <option value="WAIT_ADMIN_REVIEW">待平台处理</option>
        <option value="WAIT_MERCHANT_REVIEW">待商家处理</option>
        <option value="SUCCESS">退款成功</option>
        <option value="FAILED">退款失败</option>
        <option value="REJECTED">已驳回</option>
      </select>
      <button @click="loadRefunds">刷新</button>
    </section>

    <section class="stats-section">
      <div class="stat-card">
        <h3>退款单总数</h3>
        <p class="stat-value">{{ refunds.length }}</p>
      </div>
      <div class="stat-card">
        <h3>待平台处理</h3>
        <p class="stat-value">{{ pendingAdminCount }}</p>
      </div>
      <div class="stat-card">
        <h3>退款成功</h3>
        <p class="stat-value">{{ successCount }}</p>
      </div>
    </section>

    <section class="content-shell">
      <div class="list-panel">
        <div v-if="loading" class="state-text">退款单加载中...</div>
        <div v-else-if="error" class="state-text error">{{ error }}</div>
        <div v-else-if="!refunds.length" class="state-text">暂无退款数据</div>
        <table v-else class="refund-table">
          <thead>
            <tr>
              <th>退款单号</th>
              <th>订单号</th>
              <th>商家</th>
              <th>申请金额</th>
              <th>状态</th>
              <th>提交时间</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in refunds"
              :key="item.refundNo"
              :class="{ active: selectedRefundNo === item.refundNo }"
              @click="selectRefund(item.refundNo)"
            >
              <td>{{ item.refundNo }}</td>
              <td>{{ item.bizOrderNo }}</td>
              <td>{{ item.shopName || '-' }}</td>
              <td>¥{{ amountText(item.requestedAmount ?? item.refundAmount) }}</td>
              <td><span class="status-chip">{{ statusText(item.status) }}</span></td>
              <td>{{ formatTime(item.createTime) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="detail-panel">
        <div v-if="detailLoading" class="state-text">详情加载中...</div>
        <div v-else-if="detailError" class="state-text error">{{ detailError }}</div>
        <div v-else-if="!detail" class="state-text">请选择一条退款单</div>

        <template v-else>
          <div class="detail-hero">
            <div>
              <p class="hero-label">平台审核</p>
              <h2>{{ statusText(detail.status) }}</h2>
              <p class="hero-desc">{{ detail.reason || '无补充说明' }}</p>
            </div>
            <strong>¥{{ amountText(detail.requestedAmount ?? detail.refundAmount) }}</strong>
          </div>

          <div class="detail-card">
            <h3>退款信息</h3>
            <p>退款单号：{{ detail.refundNo }}</p>
            <p>订单号：{{ detail.bizOrderNo }}</p>
            <p>商家：{{ detail.shopName || '-' }}</p>
            <p>用户ID：{{ detail.userId || '-' }}</p>
            <p>申请金额：¥{{ amountText(detail.requestedAmount ?? detail.refundAmount) }}</p>
            <p v-if="detail.merchantReviewReason">商家备注：{{ detail.merchantReviewReason }}</p>
            <p v-if="detail.adminReviewReason">平台备注：{{ detail.adminReviewReason }}</p>
          </div>

          <div class="detail-card" v-if="detail.status === 'WAIT_ADMIN_REVIEW'">
            <h3>平台处理</h3>
            <label class="field-label">通过金额</label>
            <input v-model="approveAmount" type="number" step="0.01" min="0.01" class="text-input" />
            <label class="field-label">处理备注</label>
            <textarea v-model.trim="reviewReason" class="text-area" maxlength="120" placeholder="请输入平台审核说明"></textarea>
            <div class="action-row">
              <button class="primary-btn" :disabled="acting" @click="handleApprove">通过并退款</button>
              <button class="secondary-btn" :disabled="acting" @click="handleReject">驳回申请</button>
            </div>
          </div>

          <div class="detail-card">
            <h3>审核时间线</h3>
            <div v-if="detail.logs?.length" class="timeline">
              <div v-for="log in detail.logs" :key="log.id" class="timeline-item">
                <div class="timeline-head">
                  <strong>{{ actionText(log.action) }}</strong>
                  <span>{{ formatTime(log.createTime) }}</span>
                </div>
                <p v-if="log.comment">{{ log.comment }}</p>
              </div>
            </div>
            <p v-else class="empty-text">暂无处理记录</p>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { approveAdminRefund, getAdminRefundDetail, getAdminRefundList, rejectAdminRefund, type AdminRefundRecord } from '@/api/admin'
import { showAlert, showConfirm } from '@/utils/dialog'

const router = useRouter()
const loading = ref(false)
const detailLoading = ref(false)
const acting = ref(false)
const error = ref<string | null>(null)
const detailError = ref<string | null>(null)
const statusFilter = ref('')
const refunds = ref<AdminRefundRecord[]>([])
const detail = ref<AdminRefundRecord | null>(null)
const selectedRefundNo = ref('')
const approveAmount = ref('')
const reviewReason = ref('')

const pendingAdminCount = computed(() => refunds.value.filter(item => item.status === 'WAIT_ADMIN_REVIEW').length)
const successCount = computed(() => refunds.value.filter(item => item.status === 'SUCCESS').length)

const amountText = (value?: number) => Number(value || 0).toFixed(2)
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'

const statusText = (status?: string) => {
  switch ((status || '').toUpperCase()) {
    case 'WAIT_MERCHANT_REVIEW': return '待商家审核'
    case 'WAIT_ADMIN_REVIEW': return '待平台处理'
    case 'PROCESSING': return '退款处理中'
    case 'SUCCESS': return '退款成功'
    case 'FAILED': return '退款失败'
    case 'REJECTED': return '已驳回'
    default: return status || '处理中'
  }
}

const actionText = (action?: string) => {
  switch ((action || '').toUpperCase()) {
    case 'SUBMIT': return '用户提交申请'
    case 'MERCHANT_APPROVE': return '商家通过'
    case 'MERCHANT_REJECT': return '商家转交平台'
    case 'ADMIN_APPROVE': return '平台通过'
    case 'ADMIN_REJECT': return '平台驳回'
    case 'SYSTEM_SUCCESS': return '系统退款成功'
    case 'SYSTEM_FAILED': return '系统退款失败'
    case 'SYSTEM_ESCALATE': return '系统自动升级'
    default: return action || '状态变更'
  }
}

const syncForm = () => {
  approveAmount.value = detail.value ? amountText(detail.value.requestedAmount ?? detail.value.refundAmount) : ''
  reviewReason.value = ''
}

const loadRefunds = async () => {
  loading.value = true
  error.value = null
  try {
    refunds.value = await getAdminRefundList(statusFilter.value ? { status: statusFilter.value } : undefined)
    if (!refunds.value.length) {
      selectedRefundNo.value = ''
      detail.value = null
      return
    }
    if (!selectedRefundNo.value || !refunds.value.some(item => item.refundNo === selectedRefundNo.value)) {
      selectedRefundNo.value = refunds.value[0].refundNo
    }
  } catch (e: any) {
    error.value = e?.message || '加载退款单失败'
  } finally {
    loading.value = false
  }
}

const loadDetail = async () => {
  if (!selectedRefundNo.value) return
  detailLoading.value = true
  detailError.value = null
  try {
    detail.value = await getAdminRefundDetail(selectedRefundNo.value)
    syncForm()
  } catch (e: any) {
    detailError.value = e?.message || '加载退款详情失败'
  } finally {
    detailLoading.value = false
  }
}

const selectRefund = (refundNo: string) => {
  selectedRefundNo.value = refundNo
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  void router.push('/admin/dashboard')
}

const handleApprove = async () => {
  if (!detail.value) return
  const amount = Number(approveAmount.value)
  if (!Number.isFinite(amount) || amount <= 0) {
    await showAlert('请输入正确的通过金额')
    return
  }
  if (!(await showConfirm(`确认平台通过并退款 ¥${amount.toFixed(2)} 吗？`, '平台审核通过'))) {
    return
  }
  acting.value = true
  try {
    await approveAdminRefund(detail.value.refundNo, { approvedAmount: amount, reason: reviewReason.value })
    await showAlert('退款已处理完成')
    await loadRefunds()
    await loadDetail()
  } catch (e: any) {
    await showAlert(e?.message || '处理失败')
  } finally {
    acting.value = false
  }
}

const handleReject = async () => {
  if (!detail.value) return
  if (!(await showConfirm('确认驳回这笔退款申请吗？', '平台驳回'))) {
    return
  }
  acting.value = true
  try {
    await rejectAdminRefund(detail.value.refundNo, { reason: reviewReason.value || '平台审核驳回' })
    await showAlert('已驳回该退款申请')
    await loadRefunds()
    await loadDetail()
  } catch (e: any) {
    await showAlert(e?.message || '处理失败')
  } finally {
    acting.value = false
  }
}

watch(selectedRefundNo, () => {
  void loadDetail()
})

onMounted(async () => {
  await loadRefunds()
  await loadDetail()
})
</script>

<style scoped>
.admin-refund-manage { width: 100%; }
.page-header { margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.eyebrow { margin: 0; font-size: 12px; letter-spacing: .08em; color: #64748b; text-transform: uppercase; }
.page-header h1 { margin: 4px 0 0; font-size: 28px; color: #0f172a; }
.back-button { width: 40px; height: 40px; border: none; border-radius: 999px; background: #e2e8f0; color: #0f172a; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: background .2s; }
.back-button:hover { background: #cbd5e1; }
.back-icon { font-size: 24px; line-height: 1; }
.filter-section { display: flex; gap: 10px; margin-bottom: 16px; padding: 16px; border: 1px solid #e3ebf5; border-radius: 12px; background: #fff; box-shadow: 0 2px 10px rgba(0,0,0,.05); }
.filter-section select,.filter-section button,.text-input,.text-area { border: 1px solid #dbe4f0; border-radius: 10px; background: #fff; }
.filter-section select,.filter-section button { height: 40px; padding: 0 14px; }
.filter-section button { background: #1677ff; color: #fff; cursor: pointer; border-color: #1677ff; }
.stats-section { display: grid; grid-template-columns: repeat(auto-fit,minmax(200px,1fr)); gap: 12px; margin-bottom: 16px; }
.stat-card { background: #fff; padding: 16px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,.05); }
.stat-card h3 { margin: 0 0 8px; font-size: 14px; color: #64748b; }
.stat-value { margin: 0; font-size: 22px; font-weight: 700; color: #0f172a; }
.content-shell { display: grid; grid-template-columns: minmax(0,1.15fr) minmax(360px,.85fr); gap: 16px; align-items: start; }
.list-panel,.detail-panel { background: #fff; border: 1px solid #e3ebf5; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,.05); padding: 14px; }
.state-text { color: #64748b; padding: 16px 0; }
.state-text.error { color: #b91c1c; }
.refund-table { width: 100%; border-collapse: collapse; }
.refund-table th,.refund-table td { padding: 12px; text-align: left; border-bottom: 1px solid #eef2f7; font-size: 13px; }
.refund-table th { color: #475569; background: #f8fafc; }
.refund-table tbody tr { cursor: pointer; }
.refund-table tbody tr.active { background: #f0fdf4; }
.status-chip { border-radius: 999px; padding: 5px 10px; background: #eef2ff; color: #3730a3; font-size: 12px; font-weight: 700; }
.detail-hero { border-radius: 16px; padding: 16px; background: linear-gradient(145deg, #0f172a, #334155); color: #fff; display: flex; justify-content: space-between; gap: 12px; }
.hero-label { margin: 0; font-size: 12px; opacity: .75; text-transform: uppercase; letter-spacing: .08em; }
.detail-hero h2 { margin: 6px 0 0; font-size: 24px; }
.hero-desc { margin: 8px 0 0; color: rgba(255,255,255,.82); font-size: 13px; }
.detail-hero strong { align-self: flex-end; font-size: 28px; }
.detail-card { margin-top: 14px; border: 1px solid #e2e8f0; border-radius: 12px; padding: 14px; }
.detail-card h3 { margin: 0 0 10px; font-size: 15px; color: #0f172a; }
.detail-card p { margin: 8px 0 0; color: #475569; font-size: 13px; line-height: 1.6; }
.field-label { display: block; margin: 10px 0 6px; color: #334155; font-size: 13px; font-weight: 600; }
.text-input { width: 100%; height: 42px; padding: 0 12px; box-sizing: border-box; }
.text-area { width: 100%; min-height: 96px; padding: 12px; box-sizing: border-box; resize: vertical; }
.action-row { display: flex; gap: 10px; margin-top: 12px; }
.primary-btn,.secondary-btn { height: 42px; border: none; border-radius: 10px; padding: 0 16px; cursor: pointer; font-weight: 700; }
.primary-btn { background: linear-gradient(135deg, #1677ff, #0ea5e9); color: #fff; }
.secondary-btn { background: #fff1f2; color: #be123c; }
.timeline { display: grid; gap: 10px; }
.timeline-item { border: 1px solid #e2e8f0; border-radius: 12px; padding: 10px 12px; background: #fbfdff; }
.timeline-head { display: flex; justify-content: space-between; gap: 10px; color: #0f172a; font-size: 13px; }
.timeline-item p { margin: 6px 0 0; font-size: 12px; color: #475569; }
.empty-text { color: #64748b; font-size: 13px; }
@media (max-width: 1100px) { .content-shell { grid-template-columns: 1fr; } .filter-section { flex-wrap: wrap; } }
@media (max-width: 768px) { .header-left { width: 100%; } }
</style>
