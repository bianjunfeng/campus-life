<template>
  <div class="merchant-refund-page">
    <div class="page-header">
      <div class="header-left">
        <div>
        <p class="eyebrow">Merchant Console</p>
        <h1>退款审核</h1>
        </div>
      </div>
      <div class="header-actions">
        <select v-model="statusFilter" @change="loadRefunds">
          <option value="">全部状态</option>
          <option value="WAIT_MERCHANT_REVIEW">待我处理</option>
          <option value="WAIT_ADMIN_REVIEW">已转平台</option>
          <option value="SUCCESS">已退款</option>
          <option value="FAILED">退款失败</option>
          <option value="REJECTED">已驳回</option>
        </select>
        <button class="ghost-btn" @click="loadRefunds">刷新</button>
      </div>
    </div>

    <div class="shell">
      <section class="list-panel">
        <div v-if="loading" class="state-card">退款单加载中...</div>
        <div v-else-if="error" class="state-card error">{{ error }}</div>
        <div v-else-if="!refunds.length" class="state-card">暂无退款单</div>

        <article
          v-for="item in refunds"
          :key="item.refundNo"
          :class="['refund-item', { active: selectedRefundNo === item.refundNo }]"
          @click="selectRefund(item.refundNo)"
        >
          <div class="item-head">
            <div>
              <p class="shop-name">{{ item.shopName || '商家' }}</p>
              <h3>{{ item.voucherTitle || '优惠券订单' }}</h3>
            </div>
            <span class="status-chip">{{ statusText(item.status) }}</span>
          </div>
          <p class="item-meta">订单号：{{ item.bizOrderNo }}</p>
          <p class="item-meta">申请金额：¥{{ amountText(item.requestedAmount ?? item.refundAmount) }}</p>
          <p class="item-meta">提交时间：{{ formatTime(item.createTime) }}</p>
        </article>
      </section>

      <section class="detail-panel">
        <div v-if="detailLoading" class="state-card">详情加载中...</div>
        <div v-else-if="detailError" class="state-card error">{{ detailError }}</div>
        <div v-else-if="!detail" class="state-card">请选择左侧退款单</div>

        <template v-else>
          <div class="detail-hero">
            <div>
              <p class="eyebrow">退款申请</p>
              <h2>{{ statusText(detail.status) }}</h2>
              <p class="detail-desc">{{ detail.reason || '用户未填写补充说明' }}</p>
            </div>
            <strong>¥{{ amountText(detail.requestedAmount ?? detail.refundAmount) }}</strong>
          </div>

          <div class="detail-card">
            <h3>订单信息</h3>
            <p>退款单号：{{ detail.refundNo }}</p>
            <p>订单号：{{ detail.bizOrderNo }}</p>
            <p>商品：{{ detail.voucherTitle || '优惠券订单' }}</p>
            <p>支付方式：{{ paymentMethodText(detail.paymentMethod) }}</p>
            <p>审核截止：{{ formatTime(detail.reviewDeadline) }}</p>
          </div>

          <div class="detail-card" v-if="detail.status === 'WAIT_MERCHANT_REVIEW'">
            <h3>审核操作</h3>
            <label class="field-label">通过金额</label>
            <input v-model="approveAmount" type="number" step="0.01" min="0.01" class="text-input" />
            <label class="field-label">处理备注</label>
            <textarea v-model.trim="reviewReason" class="text-area" maxlength="120" placeholder="填写给用户或平台看的处理说明"></textarea>
            <div class="action-row">
              <button class="primary-btn" :disabled="acting" @click="handleApprove">同意退款</button>
              <button class="secondary-btn" :disabled="acting" @click="handleReject">转交平台</button>
            </div>
          </div>

          <div class="detail-card" v-if="detail.merchantReviewReason || detail.adminReviewReason">
            <h3>审核备注</h3>
            <p v-if="detail.merchantReviewReason">商家备注：{{ detail.merchantReviewReason }}</p>
            <p v-if="detail.adminReviewReason">平台备注：{{ detail.adminReviewReason }}</p>
          </div>

          <div class="detail-card">
            <h3>处理时间线</h3>
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
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { approveMerchantRefund, getMerchantRefundDetail, getMerchantRefunds, rejectMerchantRefund, type MerchantRefundRecord } from '@/api/merchant'
import { showAlert, showConfirm } from '@/utils/dialog'

const loading = ref(false)
const detailLoading = ref(false)
const acting = ref(false)
const error = ref<string | null>(null)
const detailError = ref<string | null>(null)
const statusFilter = ref('')
const refunds = ref<MerchantRefundRecord[]>([])
const detail = ref<MerchantRefundRecord | null>(null)
const selectedRefundNo = ref('')
const approveAmount = ref('')
const reviewReason = ref('')

const amountText = (value?: number) => Number(value || 0).toFixed(2)
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'

const paymentMethodText = (method?: string) => {
  if (method === 'alipay') return '支付宝'
  if (method === 'wechat') return '微信支付'
  if (method === 'wallet') return '钱包支付'
  return '未支付'
}

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

const syncReviewForm = () => {
  approveAmount.value = detail.value ? amountText(detail.value.requestedAmount ?? detail.value.refundAmount) : ''
  reviewReason.value = ''
}

const loadRefunds = async () => {
  loading.value = true
  error.value = null
  try {
    refunds.value = await getMerchantRefunds(statusFilter.value ? { status: statusFilter.value } : undefined)
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
    detail.value = await getMerchantRefundDetail(selectedRefundNo.value)
    syncReviewForm()
  } catch (e: any) {
    detailError.value = e?.message || '加载退款详情失败'
  } finally {
    detailLoading.value = false
  }
}

const selectRefund = (refundNo: string) => {
  selectedRefundNo.value = refundNo
}

const handleApprove = async () => {
  if (!detail.value) return
  const amount = Number(approveAmount.value)
  if (!Number.isFinite(amount) || amount <= 0) {
    await showAlert('请输入正确的通过金额')
    return
  }
  if (!(await showConfirm(`确认同意退款 ¥${amount.toFixed(2)} 吗？`, '商家审核通过'))) {
    return
  }
  acting.value = true
  try {
    await approveMerchantRefund(detail.value.refundNo, {
      approvedAmount: amount,
      reason: reviewReason.value
    })
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
  if (!(await showConfirm('确认将该退款申请转交平台管理员处理吗？', '转交平台'))) {
    return
  }
  acting.value = true
  try {
    await rejectMerchantRefund(detail.value.refundNo, { reason: reviewReason.value || '商家转交平台处理' })
    await showAlert('已转交平台管理员处理')
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
.merchant-refund-page { min-height: 100vh; background: linear-gradient(180deg, #f7fbff 0%, #f8fafc 100%); padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.page-header h1 { margin: 4px 0 0; font-size: 28px; color: #0f172a; }
.eyebrow { margin: 0; font-size: 12px; letter-spacing: .08em; color: #64748b; text-transform: uppercase; }
.back-button { width: 40px; height: 40px; border: none; border-radius: 999px; background: #e2e8f0; color: #0f172a; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: background .2s; }
.back-button:hover { background: #cbd5e1; }
.back-icon { font-size: 24px; line-height: 1; }
.header-actions { display: flex; gap: 10px; align-items: center; }
.header-actions select,.ghost-btn,.text-input,.text-area { border: 1px solid #dbe4f0; border-radius: 12px; background: #fff; }
.header-actions select,.ghost-btn { height: 40px; padding: 0 14px; }
.ghost-btn { cursor: pointer; }
.shell { display: grid; grid-template-columns: 380px minmax(0,1fr); gap: 16px; align-items: start; }
.list-panel,.detail-panel { background: rgba(255,255,255,.92); border: 1px solid #e5edf6; border-radius: 20px; padding: 14px; box-shadow: 0 18px 36px rgba(15,23,42,.06); }
.state-card { background: #fff; border-radius: 14px; padding: 16px; color: #475569; }
.state-card.error { color: #b91c1c; }
.refund-item { border: 1px solid #e5edf6; border-radius: 16px; padding: 14px; cursor: pointer; background: #fff; }
.refund-item + .refund-item { margin-top: 12px; }
.refund-item.active { border-color: #0f766e; box-shadow: 0 0 0 3px rgba(20,184,166,.12); }
.item-head { display: flex; justify-content: space-between; gap: 10px; }
.shop-name { margin: 0; color: #64748b; font-size: 12px; }
.item-head h3 { margin: 4px 0 0; font-size: 16px; color: #0f172a; }
.status-chip { border-radius: 999px; padding: 6px 10px; background: #ecfeff; color: #155e75; font-size: 12px; font-weight: 700; white-space: nowrap; }
.item-meta { margin: 8px 0 0; color: #475569; font-size: 13px; }
.detail-hero { border-radius: 18px; padding: 18px; background: linear-gradient(145deg, #0f766e, #0f172a); color: #fff; display: flex; justify-content: space-between; gap: 12px; }
.detail-hero h2 { margin: 4px 0 0; font-size: 24px; }
.detail-hero strong { font-size: 30px; align-self: flex-end; }
.detail-desc { margin: 8px 0 0; color: rgba(255,255,255,.8); font-size: 13px; }
.detail-card { margin-top: 14px; border: 1px solid #e5edf6; border-radius: 16px; padding: 14px; background: #fff; }
.detail-card h3 { margin: 0 0 12px; font-size: 15px; color: #0f172a; }
.detail-card p { margin: 8px 0 0; color: #475569; font-size: 13px; line-height: 1.6; }
.field-label { display: block; margin: 10px 0 6px; color: #334155; font-size: 13px; font-weight: 600; }
.text-input { width: 100%; height: 42px; padding: 0 12px; box-sizing: border-box; }
.text-area { width: 100%; min-height: 96px; padding: 12px; box-sizing: border-box; resize: vertical; }
.action-row { display: flex; gap: 10px; margin-top: 12px; }
.primary-btn,.secondary-btn { height: 42px; border: none; border-radius: 12px; padding: 0 16px; cursor: pointer; font-weight: 700; }
.primary-btn { background: linear-gradient(135deg, #0f766e, #14b8a6); color: #fff; }
.secondary-btn { background: #fff7ed; color: #c2410c; }
.timeline { display: grid; gap: 10px; }
.timeline-item { border: 1px solid #e2e8f0; border-radius: 12px; padding: 10px 12px; background: #fbfdff; }
.timeline-head { display: flex; justify-content: space-between; gap: 10px; color: #0f172a; font-size: 13px; }
.timeline-item p { margin: 6px 0 0; font-size: 12px; }
.empty-text { color: #64748b; font-size: 13px; }
@media (max-width: 1100px) { .shell { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .page-header { flex-direction: column; align-items: flex-start; } .header-left { width: 100%; } }
</style>
