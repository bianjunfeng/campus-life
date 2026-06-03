<template>
  <div class="refund-list-page">
    <header class="page-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <h1>退款记录</h1>
      <button class="refresh-btn" @click="loadRefundOrders">刷新</button>
    </header>

    <section class="summary-card">
      <div class="summary-item">
        <span>退款订单</span>
        <strong>{{ refundOrders.length }}</strong>
      </div>
      <div class="summary-item">
        <span>累计退款</span>
        <strong>¥{{ totalRefundAmount.toFixed(2) }}</strong>
      </div>
    </section>

    <div v-if="loading" class="state-card">退款记录加载中...</div>
    <div v-else-if="error" class="state-card error">{{ error }}</div>
    <div v-else-if="!refundOrders.length" class="state-card">暂无退款记录</div>

    <div class="refund-list" v-else>
      <article
        v-for="refund in refundOrders"
        :key="refund.refundNo"
        class="refund-card"
        @click="goRefundDetail(refund.refundNo)"
      >
        <div class="card-top">
          <div>
            <p class="shop-name">{{ refund.shopName || '商家' }}</p>
            <h3>{{ refund.voucherTitle || '优惠券订单' }}</h3>
          </div>
          <span class="status-badge" :class="statusClass(refund.status)">{{ refundStatusText(refund.status) }}</span>
        </div>

        <div class="card-body">
          <img :src="refund.voucherImage || fallbackImage" alt="voucher" class="voucher-image" />
          <div class="card-main">
            <p class="desc">{{ refund.voucherSubTitle || '退款原路返回' }}</p>
            <p class="meta">订单号：{{ refund.bizOrderNo }}</p>
            <p class="meta">退款单号：{{ refund.refundNo }}</p>
            <p class="meta">支付方式：{{ paymentMethodText(refund.paymentMethod) }}</p>
          </div>
        </div>

        <div class="card-footer">
          <div>
            <span class="label">退款金额</span>
            <strong class="amount">¥{{ refundAmountText(refund) }}</strong>
          </div>
          <div class="time-block">
            <span class="label">{{ refund.status === 'SUCCESS' ? '完成时间' : '提交时间' }}</span>
            <span class="time">{{ formatTime(refund.successTime || refund.createTime) }}</span>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyRefunds, type RefundRecordItem } from '@/api/payment'

const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const refundOrders = ref<RefundRecordItem[]>([])
const fallbackImage = 'https://picsum.photos/seed/refund-cover/180/180'

const totalRefundAmount = computed(() =>
  refundOrders.value
    .filter(item => String(item.status || '').toUpperCase() === 'SUCCESS')
    .reduce((sum, item) => sum + Number(item.approvedAmount ?? item.refundAmount ?? 0), 0)
)

const loadRefundOrders = async () => {
  loading.value = true
  error.value = null
  try {
    refundOrders.value = await getMyRefunds()
  } catch (e: any) {
    error.value = e?.message || '退款记录加载失败'
  } finally {
    loading.value = false
  }
}

const paymentMethodText = (method?: string) => {
  if (method === 'alipay') return '支付宝'
  if (method === 'wechat') return '微信支付'
  if (method === 'wallet') return '钱包支付'
  return '未支付'
}

const refundStatusText = (status?: string) => {
  switch ((status || '').toUpperCase()) {
    case 'WAIT_MERCHANT_REVIEW':
      return '待商家审核'
    case 'WAIT_ADMIN_REVIEW':
      return '待平台处理'
    case 'PROCESSING':
      return '退款处理中'
    case 'SUCCESS':
      return '退款成功'
    case 'FAILED':
      return '退款失败'
    case 'REJECTED':
      return '已驳回'
    case 'CLOSED':
      return '退款已关闭'
    default:
      return status || '处理中'
  }
}

const refundAmountText = (refund: RefundRecordItem) => {
  const amount = Number(refund.approvedAmount ?? refund.requestedAmount ?? refund.refundAmount ?? 0)
  return amount.toFixed(2)
}

const statusClass = (status?: string) => {
  const normalized = (status || '').toLowerCase()
  return normalized ? `status-${normalized}` : 'status-unknown'
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

const goRefundDetail = (refundNo: string) => {
  router.push({
    path: `/payment/refunds/${refundNo}`,
    query: {
      returnTo: 'refunds'
    }
  })
}

const goBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  router.replace('/me')
}

onMounted(() => {
  void loadRefundOrders()
})
</script>

<style scoped>
.refund-list-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fef7ed 0%, #f8fafc 26%, #eef2ff 100%);
}

.page-header {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(229, 231, 235, 0.9);
}

.page-header h1 {
  margin: 0;
  font-size: 16px;
}

.back-btn,
.refresh-btn {
  height: 36px;
  border: none;
  border-radius: 18px;
  background: #fff;
  padding: 0 14px;
  color: #111827;
}

.back-btn {
  width: 40px;
  padding: 0;
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.summary-card {
  margin: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 18px;
  padding: 16px;
}

.summary-item span {
  display: block;
  color: #6b7280;
  font-size: 13px;
}

.summary-item strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #111827;
}

.state-card {
  margin: 12px;
  background: #fff;
  border-radius: 14px;
  padding: 14px;
  color: #4b5563;
}

.state-card.error {
  color: #b91c1c;
}

.refund-list {
  padding: 0 12px 18px;
  display: grid;
  gap: 12px;
}

.refund-card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  padding: 14px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.shop-name {
  margin: 0;
  color: #6b7280;
  font-size: 12px;
}

.card-top h3 {
  margin: 4px 0 0;
  color: #111827;
  font-size: 16px;
}

.status-badge {
  white-space: nowrap;
  border-radius: 999px;
  padding: 6px 10px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 12px;
  font-weight: 700;
}

.card-body {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}

.voucher-image {
  width: 72px;
  height: 72px;
  border-radius: 14px;
  object-fit: cover;
}

.card-main {
  flex: 1;
  min-width: 0;
}

.desc,
.meta {
  margin: 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.6;
}

.meta {
  margin-top: 6px;
  word-break: break-all;
}

.card-footer {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.label {
  display: block;
  color: #94a3b8;
  font-size: 12px;
}

.amount {
  display: block;
  margin-top: 5px;
  color: #dc2626;
  font-size: 24px;
}

.time-block {
  text-align: right;
}

.time {
  display: block;
  margin-top: 5px;
  color: #334155;
  font-size: 12px;
}
</style>
