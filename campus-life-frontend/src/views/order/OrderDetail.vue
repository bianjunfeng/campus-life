<template>
  <div class="order-detail-page">
    <header class="page-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 18-6-6 6-6"/>
        </svg>
      </button>
      <h1 class="page-title">订单详情</h1>
      <div class="header-placeholder"></div>
    </header>

    <div v-if="loading" class="state-card">订单详情加载中...</div>
    <div v-else-if="error" class="state-card error">{{ error }}</div>

    <template v-else-if="order">
      <div class="status-card" :class="`status-${order.statusClass || 'unknown'}`">
        <h2>{{ order.statusText }}</h2>
        <p>{{ statusMessage }}</p>
      </div>

      <section class="section-card">
        <h3>商品信息</h3>
        <div class="product-row">
          <img :src="order.voucherImage || fallbackImage" class="product-image" alt="voucher" />
          <div class="product-main">
            <p class="title">{{ order.voucherTitle || '优惠券订单' }}</p>
            <p class="desc">{{ order.voucherSubTitle || '到店核销使用' }}</p>
            <p class="price">¥{{ Number(order.payAmount || 0).toFixed(2) }}</p>
          </div>
        </div>
        <p class="shop">商家：{{ order.shopName || '商家' }}</p>
      </section>

      <section class="section-card">
        <h3>订单信息</h3>
        <p>订单编号：{{ order.orderNo }}</p>
        <p>创建时间：{{ formatTime(order.createTime) }}</p>
        <p v-if="order.status === 0 && order.payDeadline">支付截止：{{ formatTime(order.payDeadline) }}</p>
        <p v-if="order.payTime">支付时间：{{ formatTime(order.payTime) }}</p>
        <p v-if="order.useTime">使用时间：{{ formatTime(order.useTime) }}</p>
        <p>支付方式：{{ paymentMethodText(order.paymentMethod) }}</p>
        <p v-if="canRefund" class="refund-hint">当前订单未核销，可申请退款</p>
      </section>

      <div class="footer-actions" v-if="order.status === 0 || canRefund">
        <button v-if="order.status === 0" class="pay-btn" @click="goPay">去支付</button>
        <button v-if="canRefund" class="refund-btn" @click="goRefund">申请退款</button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMyOrderDetailById, getMyOrderDetailByOrderNo, type OrderItem } from '@/api/order'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const error = ref<string | null>(null)
const order = ref<OrderItem | null>(null)
const fallbackImage = 'https://picsum.photos/seed/order-detail/240/240'
const nowTs = ref(Date.now())
let timer: number | null = null
const canRefund = computed(() => order.value?.status === 1 && !order.value?.useTime)
const remainingPaymentText = computed(() => {
  const currentOrder = order.value
  if (!currentOrder || currentOrder.status !== 0) return ''
  const directRemaining = Number(currentOrder.remainingSeconds || 0)
  const remaining = directRemaining > 0
    ? directRemaining
    : currentOrder.payDeadline
      ? Math.max(Math.floor((new Date(currentOrder.payDeadline).getTime() - nowTs.value) / 1000), 0)
      : 0
  if (remaining <= 0) return ''
  const minutes = Math.floor(remaining / 60).toString().padStart(2, '0')
  const seconds = Math.floor(remaining % 60).toString().padStart(2, '0')
  return `${minutes}:${seconds}`
})

const statusMessage = computed(() => {
  const status = order.value?.status
  if (status === 0) {
    if (remainingPaymentText.value) {
      return `请在 ${remainingPaymentText.value} 内完成支付，超时将自动取消`
    }
    return '请尽快完成支付'
  }
  if (status === 1) return canRefund.value ? '支付成功，未核销前可申请退款' : '支付成功，请到店核销'
  if (status === 2) return '订单已完成，感谢使用'
  if (status === 3) return '订单已过期'
  if (status === 4) return '该订单已退款'
  if (status === 5) return '订单已取消'
  return '状态未知'
})

const loadDetail = async () => {
  loading.value = true
  error.value = null
  try {
    const orderNo = route.query.orderNo as string | undefined
    if (orderNo) {
      order.value = await getMyOrderDetailByOrderNo(orderNo)
      return
    }

    const id = Number(route.params.id)
    if (!id) throw new Error('无效订单ID')
    order.value = await getMyOrderDetailById(id)
  } catch (e: any) {
    error.value = e?.message || '订单详情加载失败'
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
  router.replace('/me/orders')
}

const goPay = () => {
  if (!order.value?.orderNo) return
  router.push({ path: '/payment', query: { orderNo: order.value.orderNo } })
}

const goRefund = () => {
  if (!order.value?.orderNo) return
  router.push({ path: '/payment/refund', query: { orderNo: order.value.orderNo } })
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

const paymentMethodText = (method?: string) => {
  if (method === 'alipay') return '支付宝'
  if (method === 'wechat') return '微信支付'
  if (method === 'wallet') return '钱包支付'
  return '未支付'
}

onMounted(async () => {
  await loadDetail()
  timer = window.setInterval(() => {
    nowTs.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.order-detail-page { min-height: 100vh; background: #f6f7fb; padding-bottom: 80px; }
.page-header { height: 52px; display: flex; align-items: center; justify-content: space-between; padding: 0 12px; background: #fff; border-bottom: 1px solid #eee; }
.back-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  color: #333;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.back-btn svg {
  width: 20px;
  height: 20px;
}
.back-btn:hover {
  background: #f5f5f5;
}
.header-placeholder {
  width: 40px;
}
.page-title { margin: 0; font-size: 16px; }
.state-card { margin: 12px; padding: 14px; border-radius: 8px; background: #fff; color: #666; }
.state-card.error { color: #c0392b; }
.status-card { margin: 12px; border-radius: 10px; padding: 14px; color: #fff; }
.status-card h2 { margin: 0 0 6px; font-size: 18px; }
.status-card p { margin: 0; font-size: 13px; }
.status-pending { background: linear-gradient(135deg, #f59e0b, #d97706); }
.status-ongoing { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.status-success { background: linear-gradient(135deg, #22c55e, #16a34a); }
.status-expired, .status-cancelled { background: linear-gradient(135deg, #9ca3af, #6b7280); }
.status-refund { background: linear-gradient(135deg, #c084fc, #a855f7); }
.section-card { margin: 12px; padding: 14px; background: #fff; border-radius: 10px; }
.section-card h3 { margin: 0 0 12px; font-size: 15px; }
.product-row { display: flex; gap: 10px; }
.product-image { width: 84px; height: 84px; border-radius: 8px; object-fit: cover; }
.product-main { flex: 1; }
.title { margin: 0 0 6px; font-size: 15px; }
.desc { margin: 0 0 8px; font-size: 12px; color: #666; }
.price { margin: 0; color: #e53935; font-size: 18px; font-weight: 700; }
.shop { margin: 10px 0 0; color: #555; font-size: 13px; }
.section-card p { margin: 6px 0; color: #444; font-size: 13px; }
.refund-hint { color: #c2410c !important; font-weight: 600; }
.footer-actions { position: fixed; left: 0; right: 0; bottom: 0; height: 64px; background: #fff; border-top: 1px solid #eee; display: flex; align-items: center; justify-content: flex-end; gap: 10px; padding: 0 12px; }
.pay-btn,
.refund-btn { border: none; border-radius: 999px; padding: 10px 18px; }
.pay-btn { background: #ff4d4f; color: #fff; }
.refund-btn { background: #fff7ed; color: #c2410c; }
</style>
