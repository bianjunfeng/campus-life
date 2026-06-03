<template>
  <div class="payment-success-page">
    <header class="page-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <h1>{{ pageTitle }}</h1>
      <div class="header-placeholder"></div>
    </header>

    <div class="success-content">
      <div class="success-icon" :class="iconClass">{{ iconText }}</div>
      <h1 class="success-title">{{ titleText }}</h1>
      <p class="success-desc">{{ descText }}</p>
      <div class="order-info" v-if="orderNo">
        <p>订单号：{{ orderNo }}</p>
      </div>
      <div class="action-buttons">
        <button class="btn-primary" @click="handlePrimaryAction">{{ primaryText }}</button>
        <button class="btn-secondary" @click="goHome">返回首页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { queryPaymentStatus } from '@/api/payment'
import { resolvePaymentDisplayState } from '@/utils/paymentStatus'

const router = useRouter()
const route = useRoute()
const orderNo = ref<string>('')
const paymentMethod = ref<string>('')
const returnTo = ref<string>('')
const couponId = ref<string>('')
const orderTab = ref<string>('')
const status = ref<'loading' | 'success' | 'pending' | 'failed' | 'refunded'>('loading')

const pageTitle = ref('支付结果')
const iconText = ref('…')
const titleText = ref('支付状态校验中')
const descText = ref('正在核验订单支付状态，请稍候')
const primaryText = ref('返回支付页')
const iconClass = ref('is-loading')
let isPageActive = true

onMounted(() => {
  orderNo.value = (route.query.orderNo as string) || ''
  paymentMethod.value = (route.query.paymentMethod as string) || ''
  returnTo.value = (route.query.returnTo as string) || ''
  couponId.value = (route.query.couponId as string) || ''
  orderTab.value = (route.query.orderTab as string) || ''
  verifyPaymentStatus()
})

onBeforeUnmount(() => {
  isPageActive = false
})

const goToOrders = () => {
  if (returnTo.value === 'orders') {
    router.replace({
      path: '/me/orders',
      query: orderTab.value && orderTab.value !== 'all' ? { tab: orderTab.value } : {}
    })
    return
  }
  if (orderNo.value) {
    router.replace({
      path: '/me/orders/0',
      query: { orderNo: orderNo.value }
    })
    return
  }
  router.replace('/me/orders')
}

const goHome = () => {
  router.push('/')
}

const goToPayment = () => {
  if (orderNo.value) {
    router.replace({
      path: '/payment',
      query: {
        orderNo: orderNo.value,
        ...(paymentMethod.value ? { paymentMethod: paymentMethod.value } : {}),
        ...(returnTo.value ? { returnTo: returnTo.value } : {}),
        ...(couponId.value ? { couponId: couponId.value } : {}),
        ...(orderTab.value ? { orderTab: orderTab.value } : {})
      }
    })
    return
  }
  router.replace('/welfare')
}

const applyStatus = (next: 'loading' | 'success' | 'pending' | 'failed' | 'refunded') => {
  if (!isPageActive) return
  status.value = next
  if (next === 'loading') {
    pageTitle.value = '支付结果'
    iconText.value = '…'
    titleText.value = '支付状态校验中'
    descText.value = '正在核验订单支付状态，请稍候'
    primaryText.value = '返回支付页'
    iconClass.value = 'is-loading'
    return
  }
  if (next === 'success') {
    pageTitle.value = '支付成功'
    iconText.value = '✓'
    titleText.value = '支付成功'
    descText.value = '您的订单已支付成功，请前往订单页面查看'
    primaryText.value = '查看订单'
    iconClass.value = 'is-success'
    return
  }
  if (next === 'pending') {
    pageTitle.value = '支付未完成'
    iconText.value = '!'
    titleText.value = '支付尚未完成'
    descText.value = '订单状态仍在处理中，可稍后重试或返回支付页继续支付'
    primaryText.value = '返回支付页'
    iconClass.value = 'is-pending'
    return
  }
  if (next === 'refunded') {
    pageTitle.value = '订单已退款'
    iconText.value = '¥'
    titleText.value = '订单已退款'
    descText.value = '当前订单已完成退款处理，可前往订单页或退款记录查看最新结果'
    primaryText.value = '查看订单'
    iconClass.value = 'is-refunded'
    return
  }
  pageTitle.value = '支付失败'
  iconText.value = '×'
  titleText.value = '支付失败'
  descText.value = '订单未支付成功，请返回支付页重试'
  primaryText.value = '返回支付页'
  iconClass.value = 'is-failed'
}

const verifyPaymentStatus = async () => {
  if (!orderNo.value) {
    applyStatus('failed')
    if (isPageActive) {
      descText.value = '缺少订单号，无法校验支付状态'
    }
    return
  }
  applyStatus('loading')
  const maxAttempts = 4
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      const result = await queryPaymentStatus(orderNo.value)
      const nextState = resolvePaymentDisplayState(result.paymentStatus, result.orderStatus)
      if (nextState === 'success' || nextState === 'refunded' || nextState === 'failed') {
        applyStatus(nextState)
        if (nextState === 'failed' && isPageActive && (result.orderStatus === 3 || result.orderStatus === 5)) {
          descText.value = '订单已关闭或已取消，请返回订单页查看最新状态'
        }
        return
      }
      if (attempt < maxAttempts) {
        await new Promise(resolve => setTimeout(resolve, 1200))
        if (!isPageActive) return
        continue
      }
      applyStatus('pending')
      return
    } catch {
      if (attempt < maxAttempts) {
        await new Promise(resolve => setTimeout(resolve, 1200))
        if (!isPageActive) return
        continue
      }
      applyStatus('failed')
      if (isPageActive) {
        descText.value = '支付状态校验失败，请稍后重试'
      }
      return
    }
  }
}

const handlePrimaryAction = () => {
  if (status.value === 'success' || status.value === 'refunded') {
    goToOrders()
    return
  }
  goToPayment()
}

const goBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  if (returnTo.value === 'detail' && couponId.value) {
    router.replace(`/group-buy/${couponId.value}`)
    return
  }
  if (returnTo.value === 'cart') {
    router.replace('/cart')
    return
  }
  if (returnTo.value === 'welfare') {
    router.replace('/welfare')
    return
  }
  if (returnTo.value === 'orders') {
    goToOrders()
    return
  }
  if (orderNo.value) {
    goToOrders()
    return
  }
  router.replace('/welfare')
}
</script>

<style scoped>
.payment-success-page {
  min-height: 100vh;
  background-color: #f8f8f8;
  padding-bottom: 20px;
}

.page-header {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid #eee;
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
  background: #f4f4f4;
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.header-placeholder {
  width: 40px;
  height: 40px;
}

.success-content {
  margin: 20px;
  background-color: #fff;
  border-radius: 12px;
  padding: 40px 30px;
  text-align: center;
  max-width: 400px;
  width: 100%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.success-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  color: #fff;
  font-size: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.success-icon.is-loading {
  background-color: #8c8c8c;
}

.success-icon.is-success {
  background-color: #52c41a;
}

.success-icon.is-pending {
  background-color: #faad14;
}

.success-icon.is-refunded {
  background-color: #7c3aed;
}

.success-icon.is-failed {
  background-color: #ff4d4f;
}

.success-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
}

.success-desc {
  font-size: 14px;
  color: #666;
  margin: 0 0 20px 0;
}

.order-info {
  background-color: #f5f5f5;
  border-radius: 8px;
  padding: 12px;
  margin: 20px 0;
}

.order-info p {
  font-size: 14px;
  color: #333;
  margin: 0;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 30px;
}

.btn-primary,
.btn-secondary {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.3s;
}

.btn-primary {
  background-color: #ff4d4f;
  color: #fff;
}

.btn-primary:hover {
  background-color: #ff7875;
}

.btn-secondary {
  background-color: #f5f5f5;
  color: #333;
}

.btn-secondary:hover {
  background-color: #e8e8e8;
}
</style>
