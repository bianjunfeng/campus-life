<template>
  <div class="payment-failed-page">
    <div class="failed-content">
      <div class="failed-icon">✗</div>
      <h1 class="failed-title">支付失败</h1>
      <p class="failed-desc">{{ errorMessage || '支付过程中出现错误，请重试' }}</p>
      <div class="order-info" v-if="orderNo">
        <p>订单号：{{ orderNo }}</p>
      </div>
      <div class="action-buttons">
        <button class="btn-primary" @click="retryPayment">重新支付</button>
        <button class="btn-secondary" @click="goToOrders">查看订单</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const orderNo = ref<string>('')
const errorMessage = ref<string>('')
const paymentMethod = ref<string>('')
const returnTo = ref<string>('')
const couponId = ref<string>('')
const orderTab = ref<string>('')

onMounted(() => {
  orderNo.value = (route.query.orderNo as string) || ''
  errorMessage.value = (route.query.error as string) || ''
  paymentMethod.value = (route.query.paymentMethod as string) || ''
  returnTo.value = (route.query.returnTo as string) || ''
  couponId.value = (route.query.couponId as string) || ''
  orderTab.value = (route.query.orderTab as string) || ''
})

const goToSource = () => {
  if (returnTo.value === 'detail' && couponId.value) {
    router.push(`/group-buy/${couponId.value}`)
    return true
  }
  if (returnTo.value === 'cart') {
    router.push('/cart')
    return true
  }
  if (returnTo.value === 'welfare') {
    router.push('/welfare')
    return true
  }
  if (returnTo.value === 'orders') {
    router.push({
      path: '/me/orders',
      query: orderTab.value && orderTab.value !== 'all' ? { tab: orderTab.value } : {}
    })
    return true
  }
  return false
}

const retryPayment = () => {
  if (orderNo.value) {
    router.push({
      path: '/payment',
      query: {
        orderNo: orderNo.value,
        ...(paymentMethod.value ? { paymentMethod: paymentMethod.value } : {}),
        ...(returnTo.value ? { returnTo: returnTo.value } : {}),
        ...(couponId.value ? { couponId: couponId.value } : {}),
        ...(orderTab.value ? { orderTab: orderTab.value } : {})
      }
    })
  } else {
    if (!goToSource()) {
      router.push('/')
    }
  }
}

const goToOrders = () => {
  if (returnTo.value === 'orders' && goToSource()) {
    return
  }
  if (orderNo.value) {
    router.push({
      path: '/me/orders/0',
      query: { orderNo: orderNo.value }
    })
    return
  }
  if (!goToSource()) {
    router.push('/me/orders')
  }
}
</script>

<style scoped>
.payment-failed-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f8f8;
  padding: 20px;
}

.failed-content {
  background-color: #fff;
  border-radius: 12px;
  padding: 40px 30px;
  text-align: center;
  max-width: 400px;
  width: 100%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.failed-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-color: #ff4d4f;
  color: #fff;
  font-size: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.failed-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
}

.failed-desc {
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

