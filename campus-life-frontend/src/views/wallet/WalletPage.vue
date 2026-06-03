<template>
  <div class="wallet-page">
    <header class="page-header">
      <button @click="goBack" class="back-btn" aria-label="返回">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 18-6-6 6-6"/>
        </svg>
      </button>
      <h1>我的钱包</h1>
      <button @click="loadData" class="refresh-btn">刷新</button>
    </header>

    <div v-if="loading" class="state">钱包数据加载中...</div>
    <div v-else-if="error" class="state error">{{ error }}</div>

    <template v-else>
      <section class="balance-card">
        <p class="label">可用余额</p>
        <p class="balance">¥{{ Number(wallet.availableBalance || 0).toFixed(2) }}</p>
        <div class="stats">
          <div class="stat-item">
            <span>累计消费</span>
            <strong>¥{{ Number(wallet.totalSpent || 0).toFixed(2) }}</strong>
          </div>
          <button class="stat-item stat-btn" type="button" @click="goPendingOrders">
            <span>待支付订单</span>
            <strong>{{ wallet.pendingPaymentCount || 0 }}</strong>
          </button>
          <div class="stat-item">
            <span>可用优惠券</span>
            <strong>{{ wallet.availableCouponCount || 0 }}</strong>
          </div>
        </div>
      </section>

      <section class="tx-card">
        <h2>最近交易</h2>
        <div v-if="!wallet.recentTransactions?.length" class="empty">暂无交易记录</div>
        <div
          v-for="tx in wallet.recentTransactions || []"
          :key="tx.orderNo"
          class="tx-item"
          @click="goOrderDetail(tx)"
        >
          <img :src="tx.voucherImage || fallbackImage" class="tx-image" alt="voucher" />
          <div class="tx-content">
            <p class="tx-title">{{ tx.voucherTitle || '优惠券订单' }}</p>
            <p class="tx-desc">{{ tx.voucherSubTitle || '到店核销使用' }}</p>
            <p class="tx-time">{{ formatTime(tx.payTime || tx.createTime) }}</p>
            <p class="tx-status">{{ tx.statusText || '-' }}</p>
          </div>
          <div class="tx-amount">¥{{ Number(tx.amount || 0).toFixed(2) }}</div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyWalletOverview, type WalletOverview } from '@/api/wallet'

const router = useRouter()
const loading = ref(false)
const error = ref<string | null>(null)
const fallbackImage = 'https://picsum.photos/seed/wallet-tx/120/120'

const wallet = ref<WalletOverview>({
  availableBalance: 0,
  totalSpent: 0,
  pendingPaymentCount: 0,
  availableCouponCount: 0,
  recentTransactions: []
})

const loadData = async () => {
  loading.value = true
  error.value = null
  try {
    wallet.value = await getMyWalletOverview()
  } catch (e: any) {
    error.value = e?.message || '加载钱包失败'
  } finally {
    loading.value = false
  }
}

const formatTime = (time?: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', { hour12: false })
}

const goBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  router.replace('/me')
}

const goOrderDetail = (tx: any) => {
  if (tx?.orderId) {
    router.push(`/me/orders/${tx.orderId}`)
    return
  }
  if (tx?.orderNo) {
    router.push({
      path: '/me/orders/0',
      query: { orderNo: tx.orderNo }
    })
  }
}

const goPendingOrders = () => {
  router.push({
    path: '/me/orders',
    query: { tab: 'pending_payment' }
  })
}

onMounted(loadData)
</script>

<style scoped>
.wallet-page {
  background: #f5f7fb;
  min-height: 100vh;
  padding: 12px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

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

.page-header h1 {
  margin: 0;
  font-size: 20px;
}

.refresh-btn {
  border: none;
  background: #1677ff;
  color: #fff;
  border-radius: 8px;
  padding: 6px 12px;
}

.state {
  background: #fff;
  border-radius: 8px;
  padding: 14px;
}

.state.error {
  color: #c0392b;
}

.balance-card, .tx-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 12px;
}

.label {
  margin: 0;
  color: #666;
  font-size: 13px;
}

.balance {
  margin: 4px 0 12px;
  font-size: 32px;
  color: #0d47a1;
  font-weight: 700;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.stat-item {
  background: #f7f9ff;
  border-radius: 8px;
  padding: 8px;
  font-size: 12px;
  color: #666;
}

.stat-btn {
  border: none;
  text-align: left;
  cursor: pointer;
}

.stat-item strong {
  display: block;
  margin-top: 4px;
  font-size: 15px;
  color: #222;
}

.tx-card h2 {
  margin: 0 0 10px;
  font-size: 16px;
}

.empty {
  color: #888;
  font-size: 13px;
}

.tx-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.tx-item:last-child {
  border-bottom: none;
}

.tx-image {
  width: 46px;
  height: 46px;
  border-radius: 6px;
  object-fit: cover;
}

.tx-content {
  flex: 1;
}

.tx-title {
  margin: 0;
  font-size: 14px;
}

.tx-desc {
  margin: 2px 0 0;
  font-size: 12px;
  color: #666;
}

.tx-time, .tx-status {
  margin: 2px 0 0;
  font-size: 12px;
  color: #777;
}

.tx-amount {
  color: #e53935;
  font-weight: 700;
}
</style>
