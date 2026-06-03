<template>
  <div class="refund-detail-page">
    <header class="page-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <h1>退款详情</h1>
      <button class="refresh-btn" @click="loadDetail">刷新</button>
    </header>

    <div v-if="loading" class="state-card">退款详情加载中...</div>
    <div v-else-if="error" class="state-card error">{{ error }}</div>

    <template v-else-if="detail">
      <section class="hero-card">
        <div>
          <p class="hero-label">退款进度</p>
          <h2>{{ statusText(detail.status) }}</h2>
          <p class="hero-desc">{{ statusDesc(detail.status) }}</p>
        </div>
        <div class="hero-amount">
          <span>申请金额</span>
          <strong>¥{{ amountText(detail.requestedAmount ?? detail.refundAmount) }}</strong>
        </div>
      </section>

      <section class="section-card">
        <h3>退款单信息</h3>
        <div class="info-grid">
          <div class="info-row"><span>退款单号</span><strong>{{ detail.refundNo }}</strong></div>
          <div class="info-row"><span>订单号</span><strong>{{ detail.bizOrderNo }}</strong></div>
          <div class="info-row"><span>商家</span><strong>{{ detail.shopName || '-' }}</strong></div>
          <div class="info-row"><span>支付方式</span><strong>{{ paymentMethodText(detail.paymentMethod) }}</strong></div>
          <div class="info-row"><span>当前处理方</span><strong>{{ reviewerText(detail.currentReviewerRole) }}</strong></div>
          <div class="info-row"><span>提交时间</span><strong>{{ formatTime(detail.createTime) }}</strong></div>
          <div class="info-row" v-if="detail.reviewDeadline"><span>审核截止</span><strong>{{ formatTime(detail.reviewDeadline) }}</strong></div>
          <div class="info-row" v-if="detail.approvedAmount != null"><span>通过金额</span><strong>¥{{ amountText(detail.approvedAmount) }}</strong></div>
          <div class="info-row" v-if="detail.successTime"><span>退款完成</span><strong>{{ formatTime(detail.successTime) }}</strong></div>
        </div>
      </section>

      <section class="section-card">
        <h3>申请内容</h3>
        <p>商品：{{ detail.voucherTitle || '优惠券订单' }}</p>
        <p>说明：{{ detail.reason || '未填写退款原因' }}</p>
      </section>

      <section class="section-card" v-if="detail.merchantReviewReason || detail.adminReviewReason">
        <h3>审核备注</h3>
        <p v-if="detail.merchantReviewReason">商家备注：{{ detail.merchantReviewReason }}</p>
        <p v-if="detail.merchantReviewTime">商家处理时间：{{ formatTime(detail.merchantReviewTime) }}</p>
        <p v-if="detail.adminReviewReason">平台备注：{{ detail.adminReviewReason }}</p>
        <p v-if="detail.adminReviewTime">平台处理时间：{{ formatTime(detail.adminReviewTime) }}</p>
      </section>

      <section class="section-card">
        <h3>处理时间线</h3>
        <div v-if="detail.logs?.length" class="timeline">
          <article v-for="log in detail.logs" :key="log.id" class="timeline-item">
            <div class="timeline-head">
              <strong>{{ actionText(log.action) }}</strong>
              <span>{{ formatTime(log.createTime) }}</span>
            </div>
            <p class="timeline-meta">角色：{{ reviewerText(log.operatorRole) }}</p>
            <p class="timeline-meta" v-if="log.comment">备注：{{ log.comment }}</p>
            <p class="timeline-meta" v-if="log.fromStatus || log.toStatus">
              状态：{{ statusText(log.fromStatus) }} → {{ statusText(log.toStatus) }}
            </p>
          </article>
        </div>
        <p v-else class="empty-text">暂无处理记录</p>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMyRefundDetail, type RefundRecordItem } from '@/api/payment'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const detail = ref<RefundRecordItem | null>(null)
const returnTo = ref('')
const orderTab = ref('')
const orderNo = ref('')

const amountText = (value?: number) => Number(value || 0).toFixed(2)

const paymentMethodText = (method?: string) => {
  if (method === 'alipay') return '支付宝'
  if (method === 'wechat') return '微信支付'
  if (method === 'wallet') return '钱包支付'
  return '未支付'
}

const reviewerText = (role?: string) => {
  if (role === 'MERCHANT') return '商家'
  if (role === 'ADMIN') return '平台管理员'
  if (role === 'SYSTEM') return '系统执行中'
  if (role === 'USER') return '用户'
  return '暂无'
}

const statusText = (status?: string) => {
  if (!status) return '-'
  switch ((status || '').toUpperCase()) {
    case 'WAIT_MERCHANT_REVIEW': return '待商家审核'
    case 'WAIT_ADMIN_REVIEW': return '待平台处理'
    case 'PROCESSING': return '退款处理中'
    case 'SUCCESS': return '退款成功'
    case 'FAILED': return '退款失败'
    case 'REJECTED': return '已驳回'
    case 'CLOSED': return '退款已关闭'
    default: return status || '处理中'
  }
}

const statusDesc = (status?: string) => {
  switch ((status || '').toUpperCase()) {
    case 'WAIT_MERCHANT_REVIEW': return '商家正在审核你的退款申请。'
    case 'WAIT_ADMIN_REVIEW': return '平台管理员正在处理这笔退款。'
    case 'PROCESSING': return '审核通过，退款正在原路退回。'
    case 'SUCCESS': return '退款已完成，请留意原支付渠道到账。'
    case 'FAILED': return '退款执行失败，平台会继续处理。'
    case 'REJECTED': return '这笔退款申请未通过，请查看审核备注。'
    case 'CLOSED': return '这笔退款申请已关闭，如有疑问请联系平台处理。'
    default: return '退款状态更新中。'
  }
}

const actionText = (action?: string) => {
  switch ((action || '').toUpperCase()) {
    case 'SUBMIT': return '提交申请'
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

const formatTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

const loadDetail = async () => {
  const refundNo = route.params.refundNo as string
  if (!refundNo) {
    error.value = '缺少退款单号'
    return
  }
  loading.value = true
  error.value = null
  try {
    detail.value = await getMyRefundDetail(refundNo)
  } catch (e: any) {
    error.value = e?.message || '加载退款详情失败'
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  if (returnTo.value === 'orders') {
    router.replace({
      path: '/me/orders',
      query: orderTab.value && orderTab.value !== 'all' ? { tab: orderTab.value } : {}
    })
    return
  }
  if (returnTo.value === 'refunds') {
    router.replace('/me/refunds')
    return
  }
  if (orderNo.value || detail.value?.bizOrderNo) {
    router.replace({
      path: '/me/orders/0',
      query: { orderNo: orderNo.value || detail.value?.bizOrderNo || '' }
    })
    return
  }
  router.replace('/me/refunds')
}

onMounted(() => {
  returnTo.value = (route.query.returnTo as string) || ''
  orderTab.value = (route.query.orderTab as string) || ''
  orderNo.value = (route.query.orderNo as string) || ''
  void loadDetail()
})
</script>

<style scoped>
.refund-detail-page { min-height: 100vh; background: #f8fafc; padding-bottom: 24px; }
.page-header { height: 52px; display: flex; align-items: center; justify-content: space-between; padding: 0 12px; background: rgba(255,255,255,.92); border-bottom: 1px solid #e5e7eb; }
.page-header h1 { margin: 0; font-size: 16px; }
.back-btn,.refresh-btn { height: 36px; border: none; border-radius: 18px; background: #fff; padding: 0 14px; }
.back-btn { width: 40px; padding: 0; }
.back-btn svg { width: 20px; height: 20px; }
.state-card,.section-card { margin: 12px; background: #fff; border-radius: 16px; padding: 14px; }
.state-card.error { color: #b91c1c; }
.hero-card { margin: 12px; padding: 18px; border-radius: 20px; color: #fff; background: linear-gradient(145deg, #0f172a, #334155); display: flex; justify-content: space-between; gap: 12px; }
.hero-label { margin: 0 0 6px; font-size: 12px; opacity: .75; }
.hero-card h2 { margin: 0; font-size: 24px; }
.hero-desc { margin: 8px 0 0; font-size: 13px; color: rgba(255,255,255,.8); }
.hero-amount { text-align: right; }
.hero-amount span { display: block; font-size: 12px; opacity: .75; }
.hero-amount strong { display: block; margin-top: 8px; font-size: 28px; }
.section-card h3 { margin: 0 0 12px; font-size: 15px; color: #0f172a; }
.section-card p { margin: 8px 0 0; color: #475569; font-size: 13px; line-height: 1.6; }
.info-grid { display: grid; gap: 10px; }
.info-row { display: flex; justify-content: space-between; gap: 10px; background: #f8fafc; border-radius: 12px; padding: 10px 12px; }
.info-row span { color: #64748b; font-size: 13px; }
.info-row strong { color: #0f172a; font-size: 13px; text-align: right; word-break: break-all; }
.timeline { display: grid; gap: 10px; }
.timeline-item { border: 1px solid #e2e8f0; border-radius: 14px; padding: 12px; background: #fbfdff; }
.timeline-head { display: flex; justify-content: space-between; gap: 10px; color: #0f172a; font-size: 13px; }
.timeline-meta { margin: 6px 0 0; color: #475569; font-size: 12px; }
.empty-text { color: #64748b; font-size: 13px; }
</style>
