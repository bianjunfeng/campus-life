<template>
  <div class="order-list-page">
    <header class="page-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 18-6-6 6-6"/>
        </svg>
      </button>
      <h1 class="page-title">我的订单</h1>
      <div class="header-placeholder"></div>
    </header>

    <div class="order-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab-btn', { active: activeTab === tab.value }]"
        @click="changeTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="state-card">订单加载中...</div>
    <div v-else-if="error" class="state-card error">{{ error }}</div>
    <div v-else-if="!orders.length" class="state-card">暂无订单</div>

    <div class="order-list" v-else>
      <div class="order-item" v-for="order in orders" :key="order.id" @click="goToOrderDetail(order.id)">
        <div class="order-header">
          <div class="order-shop">{{ order.shopName || '商家' }}</div>
          <div class="order-status" :class="`status-${order.statusClass || 'unknown'}`">{{ order.statusText || '-' }}</div>
        </div>

        <div class="order-content">
          <img :src="order.voucherImage || fallbackImage" alt="商品图片" class="product-image" />
          <div class="product-details">
            <div class="coupon-head">
              <span class="coupon-tag">优惠券</span>
              <h3 class="product-title">{{ order.voucherTitle || '优惠券订单' }}</h3>
            </div>
            <p class="product-desc">{{ order.voucherSubTitle || '到店核销使用' }}</p>
            <div class="product-price">¥{{ Number(order.payAmount || 0).toFixed(2) }}</div>
            <p v-if="order.status === 0" class="countdown-text">
              {{ isTimeout(order) ? '即将自动取消' : `剩余支付时间 ${remainingText(order)}` }}
            </p>
          </div>
        </div>

        <div class="order-footer">
          <div class="order-time">{{ formatTime(order.createTime) }}</div>
          <div class="order-actions">
            <button
              v-if="order.status === 0"
              class="action-btn pay-btn"
              :disabled="isTimeout(order)"
              @click.stop="goPay(order.orderNo)"
            >
              去支付
            </button>
            <button
              v-if="order.status === 0"
              class="action-btn cancel-btn"
              @click.stop="cancelOrder(order.id)"
            >
              取消订单
            </button>
            <button
              v-if="order.status !== 0"
              class="action-btn detail-btn"
              @click.stop="goToOrderDetail(order.id)"
            >
              查看详情
            </button>
            <button
              v-if="order.status === 1 && !order.useTime"
              class="action-btn refund-btn"
              @click.stop="goRefund(order.orderNo)"
            >
              申请退款
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cancelMyOrder, getMyOrders, type OrderItem } from '@/api/order'
import { showAlert, showConfirm } from '@/utils/dialog'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref<string | null>(null)
const orders = ref<OrderItem[]>([])
const activeTab = ref('all')
const fallbackImage = 'https://picsum.photos/seed/order-cover/180/180'
const nowTs = ref(Date.now())
let timer: number | null = null

const tabs = [
  { label: '全部', value: 'all', status: undefined },
  { label: '待支付', value: 'pending_payment', status: 0 },
  { label: '待使用', value: 'pending_usage', status: 1 },
  { label: '已退款', value: 'refunded', status: 4 },
  { label: '已完成', value: 'completed', status: 2 },
  { label: '已取消', value: 'cancelled', status: 5 }
]

const resolveTabValue = (rawTab?: string, rawStatus?: string) => {
  if (rawTab && tabs.some(tab => tab.value === rawTab)) {
    return rawTab
  }
  const normalizedStatus = Number(rawStatus)
  if (!Number.isNaN(normalizedStatus)) {
    const matchedTab = tabs.find(tab => tab.status === normalizedStatus)
    if (matchedTab) {
      return matchedTab.value
    }
  }
  return 'all'
}

const syncTabFromRoute = () => {
  const nextTab = resolveTabValue(route.query.tab as string | undefined, route.query.status as string | undefined)
  if (activeTab.value !== nextTab) {
    activeTab.value = nextTab
  }
}

const normalizeOrders = (list: OrderItem[]) => {
  const loadedAt = Date.now()
  return (list || []).map((item) => {
    if (item.status !== 0 || item.payDeadline || !item.remainingSeconds) {
      return item
    }
    return {
      ...item,
      payDeadline: new Date(loadedAt + Number(item.remainingSeconds) * 1000).toISOString()
    }
  })
}

const loadOrders = async () => {
  loading.value = true
  error.value = null
  try {
    const currentTab = tabs.find(tab => tab.value === activeTab.value)
    const result = await getMyOrders({ status: currentTab?.status, page: 1, size: 30 })
    orders.value = normalizeOrders(result.list || [])
  } catch (e: any) {
    error.value = e?.message || '订单加载失败'
  } finally {
    loading.value = false
  }
}

const changeTab = async (value: string) => {
  if (activeTab.value === value) return
  activeTab.value = value
  const nextQuery = value === 'all' ? {} : { tab: value }
  await router.replace({ path: '/me/orders', query: nextQuery })
  await loadOrders()
}

const goBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  router.replace('/me')
}

const goToOrderDetail = (orderId: number) => {
  router.push(`/me/orders/${orderId}`)
}

const goPay = (orderNo: string) => {
  router.push({
    path: '/payment',
    query: {
      orderNo,
      returnTo: 'orders',
      ...(activeTab.value !== 'all' ? { orderTab: activeTab.value } : {})
    }
  })
}

const goRefund = (orderNo: string) => {
  router.push({
    path: '/payment/refund',
    query: {
      orderNo,
      returnTo: 'orders',
      ...(activeTab.value !== 'all' ? { orderTab: activeTab.value } : {})
    }
  })
}

const cancelOrder = async (id: number) => {
  if (!(await showConfirm('确认取消该订单吗？'))) return
  try {
    await cancelMyOrder(id)
    await loadOrders()
  } catch (e: any) {
    await showAlert(e?.message || '取消订单失败')
  }
}

const remainingSeconds = (order: OrderItem) => {
  if (order.status !== 0) return 0
  if (order.payDeadline) {
    const end = new Date(order.payDeadline).getTime()
    return Math.max(Math.floor((end - nowTs.value) / 1000), 0)
  }
  return Math.max(Number(order.remainingSeconds || 0), 0)
}

const isTimeout = (order: OrderItem) => remainingSeconds(order) <= 0

const remainingText = (order: OrderItem) => {
  const sec = remainingSeconds(order)
  const m = Math.floor(sec / 60).toString().padStart(2, '0')
  const s = Math.floor(sec % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

const hasTimeoutOrder = computed(() => orders.value.some(item => item.status === 0 && isTimeout(item)))

onMounted(async () => {
  syncTabFromRoute()
  await loadOrders()
  timer = window.setInterval(async () => {
    nowTs.value = Date.now()
    if (hasTimeoutOrder.value && !loading.value) {
      await loadOrders()
    }
  }, 1000)
})

watch(
  () => [route.query.tab, route.query.status],
  async () => {
    const previousTab = activeTab.value
    syncTabFromRoute()
    if (activeTab.value !== previousTab && !loading.value) {
      await loadOrders()
    }
  }
)

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.order-list-page { background: #f6f7fb; min-height: 100vh; }
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
.order-tabs { display: flex; gap: 8px; overflow-x: auto; background: #fff; padding: 10px 12px; border-bottom: 1px solid #eee; }
.tab-btn { border: 1px solid #ddd; background: #fff; border-radius: 999px; padding: 6px 12px; font-size: 13px; white-space: nowrap; }
.tab-btn.active { border-color: #1677ff; color: #1677ff; }
.state-card { margin: 12px; background: #fff; border-radius: 8px; padding: 14px; color: #666; }
.state-card.error { color: #c0392b; }
.order-list { padding: 12px; display: grid; gap: 10px; }
.order-item { background: #fff; border-radius: 10px; padding: 12px; border: 1px solid #edf0f5; }
.order-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.order-shop { font-size: 14px; color: #222; font-weight: 600; }
.order-status { font-size: 12px; }
.status-pending { color: #d97706; }
.status-ongoing { color: #2563eb; }
.status-success { color: #16a34a; }
.status-cancelled, .status-expired { color: #777; }
.status-refund { color: #a21caf; }
.order-content { display: flex; gap: 10px; }
.product-image { width: 72px; height: 72px; border-radius: 8px; object-fit: cover; }
.product-details { flex: 1; }
.coupon-head { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.coupon-tag { font-size: 11px; color: #fff; background: linear-gradient(135deg, #ef4444, #f97316); padding: 2px 6px; border-radius: 999px; }
.product-title { margin: 0; font-size: 14px; }
.product-desc { margin: 0 0 8px; font-size: 12px; color: #666; }
.product-price { color: #e53935; font-size: 16px; font-weight: 700; }
.countdown-text { margin: 6px 0 0; font-size: 12px; color: #d97706; }
.order-footer { margin-top: 10px; display: flex; align-items: center; justify-content: space-between; }
.order-time { font-size: 12px; color: #777; }
.action-btn { border: none; border-radius: 999px; padding: 6px 12px; font-size: 12px; }
.pay-btn { background: #1677ff; color: #fff; }
.pay-btn:disabled { background: #94a3b8; }
.cancel-btn { background: #fff1f2; color: #be123c; margin-right: 6px; }
.detail-btn { background: #f3f4f6; color: #333; }
.refund-btn { background: #fff7ed; color: #c2410c; margin-left: 6px; }
</style>
