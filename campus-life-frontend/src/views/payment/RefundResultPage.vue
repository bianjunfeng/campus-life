<template>
  <div class="refund-result-page">
    <header class="page-header">
      <button class="back-btn" @click="goOrders" aria-label="返回">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <h1>退款结果</h1>
      <div class="header-placeholder"></div>
    </header>

    <main class="result-shell">
      <div class="result-card">
        <div :class="['result-icon', iconClass]">{{ iconText }}</div>
        <h2>{{ titleText }}</h2>
        <p class="desc">{{ descText }}</p>

        <div class="info-grid">
          <div class="info-row">
            <span>订单号</span>
            <strong>{{ orderNo || '-' }}</strong>
          </div>
          <div class="info-row">
            <span>退款单号</span>
            <strong>{{ refundNo || '-' }}</strong>
          </div>
          <div class="info-row">
            <span>退款金额</span>
            <strong>¥{{ refundAmountText }}</strong>
          </div>
          <div class="info-row">
            <span>处理状态</span>
            <strong>{{ statusText }}</strong>
          </div>
        </div>

        <div class="actions">
          <button class="primary-btn" @click="goRefundDetail">查看退款详情</button>
          <button class="secondary-btn" @click="goRefundList">退款记录</button>
          <button class="secondary-btn" @click="goOrders">返回订单列表</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const orderNo = computed(() => (route.query.orderNo as string) || '')
const refundNo = computed(() => (route.query.refundNo as string) || '')
const returnTo = computed(() => (route.query.returnTo as string) || '')
const orderTab = computed(() => (route.query.orderTab as string) || '')
const refundAmount = computed(() => {
  const amount = Number(route.query.refundAmount || 0)
  return Number.isFinite(amount) ? amount : 0
})
const status = computed(() => String(route.query.status || '').toUpperCase())
const isSuccessStatus = computed(() => ['FULL_REFUNDED', 'PARTIAL_REFUNDED', 'SUCCESS'].includes(status.value))

const refundAmountText = computed(() => refundAmount.value.toFixed(2))

const statusText = computed(() => {
  if (status.value === 'WAIT_MERCHANT_REVIEW') return '待商家审核'
  if (status.value === 'WAIT_ADMIN_REVIEW') return '待平台处理'
  if (status.value === 'REJECTED') return '退款申请被驳回'
  if (status.value === 'CLOSED') return '退款已关闭'
  if (status.value === 'FULL_REFUNDED') return '全额退款成功'
  if (status.value === 'PARTIAL_REFUNDED') return '部分退款成功'
  if (status.value === 'SUCCESS') return '退款成功'
  if (status.value === 'PROCESSING') return '退款处理中'
  if (status.value === 'FAILED') return '退款失败'
  return status.value || '状态待确认'
})

const titleText = computed(() => {
  if (status.value === 'WAIT_MERCHANT_REVIEW' || status.value === 'WAIT_ADMIN_REVIEW') return '退款申请已提交'
  if (status.value === 'REJECTED') return '退款申请未通过'
  if (status.value === 'CLOSED') return '退款申请已关闭'
  if (status.value === 'FAILED') return '退款申请失败'
  if (status.value === 'PROCESSING') return '退款申请已提交'
  if (isSuccessStatus.value) return '退款申请成功'
  return '退款状态待确认'
})

const descText = computed(() => {
  if (status.value === 'WAIT_MERCHANT_REVIEW') return '退款申请已提交，商家正在审核，请稍后回到退款记录页查看进度。'
  if (status.value === 'WAIT_ADMIN_REVIEW') return '商家已转交平台处理，管理员审核后会继续推进退款。'
  if (status.value === 'REJECTED') return '本次退款申请未通过，你可以查看退款记录中的审核说明。'
  if (status.value === 'CLOSED') return '当前退款申请已关闭，请返回退款详情页查看具体状态。'
  if (status.value === 'FAILED') return '当前退款未成功，请返回订单页稍后重试或联系平台处理。'
  if (status.value === 'PROCESSING') return '退款已提交，渠道处理中，请稍后回到订单页确认最终状态。'
  if (isSuccessStatus.value) return '退款结果已记录，资金将按原支付方式退回。'
  return '退款状态已记录，请前往退款详情或退款记录页查看最新进度。'
})

const iconText = computed(() => {
  if (status.value === 'WAIT_MERCHANT_REVIEW' || status.value === 'WAIT_ADMIN_REVIEW') return '审'
  if (status.value === 'REJECTED') return '!'
  if (status.value === 'CLOSED') return '×'
  if (status.value === 'FAILED') return '×'
  if (status.value === 'PROCESSING') return '…'
  if (isSuccessStatus.value) return '¥'
  return '?'
})

const iconClass = computed(() => {
  if (status.value === 'WAIT_MERCHANT_REVIEW' || status.value === 'WAIT_ADMIN_REVIEW') return 'is-processing'
  if (status.value === 'REJECTED') return 'is-failed'
  if (status.value === 'CLOSED') return 'is-failed'
  if (status.value === 'FAILED') return 'is-failed'
  if (status.value === 'PROCESSING') return 'is-processing'
  if (isSuccessStatus.value) return 'is-success'
  return 'is-processing'
})

const goRefundDetail = () => {
  if (refundNo.value) {
    router.replace({
      path: `/payment/refunds/${refundNo.value}`,
      query: {
        ...(returnTo.value ? { returnTo: returnTo.value } : {}),
        ...(orderTab.value ? { orderTab: orderTab.value } : {}),
        ...(orderNo.value ? { orderNo: orderNo.value } : {})
      }
    })
    return
  }
  goOrders()
}

const goOrders = () => {
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
  if (orderNo.value) {
    router.replace({ path: '/me/orders/0', query: { orderNo: orderNo.value } })
    return
  }
  router.replace('/me/orders')
}

const goRefundList = () => {
  router.replace('/me/refunds')
}
</script>

<style scoped>
.refund-result-page {
  min-height: 100vh;
  background: radial-gradient(circle at top, #fff7ed 0%, #f8fafc 48%, #eef2ff 100%);
}

.page-header {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(229, 231, 235, 0.8);
}

.page-header h1 {
  margin: 0;
  font-size: 16px;
}

.back-btn,
.header-placeholder {
  width: 40px;
  height: 40px;
}

.back-btn {
  border: none;
  border-radius: 20px;
  background: #fff;
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.result-shell {
  padding: 24px 16px;
}

.result-card {
  max-width: 420px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.1);
  border-radius: 24px;
  padding: 28px 20px 22px;
  text-align: center;
}

.result-icon {
  width: 88px;
  height: 88px;
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 18px;
  font-size: 42px;
  color: #fff;
}

.result-icon.is-success {
  background: linear-gradient(135deg, #ea580c, #f97316);
}

.result-icon.is-processing {
  background: linear-gradient(135deg, #475569, #64748b);
}

.result-icon.is-failed {
  background: linear-gradient(135deg, #b91c1c, #ef4444);
}

.result-card h2 {
  margin: 0;
  font-size: 24px;
  color: #111827;
}

.desc {
  margin: 12px 0 0;
  color: #4b5563;
  line-height: 1.6;
  font-size: 14px;
}

.info-grid {
  margin-top: 22px;
  display: grid;
  gap: 10px;
  text-align: left;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #f8fafc;
  border-radius: 14px;
  padding: 12px 14px;
}

.info-row span {
  color: #64748b;
  font-size: 13px;
}

.info-row strong {
  color: #0f172a;
  font-size: 13px;
  text-align: right;
  word-break: break-all;
}

.actions {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}

.primary-btn,
.secondary-btn {
  width: 100%;
  height: 46px;
  border-radius: 14px;
  border: none;
  font-size: 15px;
  font-weight: 700;
}

.primary-btn {
  background: linear-gradient(135deg, #111827, #374151);
  color: #fff;
}

.secondary-btn {
  background: #eef2ff;
  color: #3730a3;
}
</style>
