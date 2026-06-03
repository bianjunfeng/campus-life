<template>
  <div class="group-buy-order">
    <header class="page-header">
      <button class="back-btn" @click="goBack">返回</button>
      <h1>确认订单</h1>
      <div class="placeholder"></div>
    </header>

    <section v-if="!product" class="state-card">商品信息已失效，请返回详情页重新下单</section>

    <section class="card" v-else>
      <img :src="product.imageUrl" :alt="product.title" class="product-image" />
      <div class="product-content">
        <h2>{{ product.title }}</h2>
        <p class="shop">{{ product.shopName || '商家' }}</p>
        <div class="price-row">
          <span class="price">¥{{ product.price }}</span>
          <span class="origin">¥{{ product.originalPrice }}</span>
        </div>
      </div>
    </section>

    <section class="card">
      <h3>支付方式</h3>
      <div class="pay-options">
        <button :class="['pay-option', { active: paymentMethod === 'alipay' }]" @click="paymentMethod = 'alipay'">支付宝</button>
        <button :class="['pay-option', { active: paymentMethod === 'wechat' }]" @click="paymentMethod = 'wechat'">微信支付</button>
        <button :class="['pay-option', { active: paymentMethod === 'wallet' }]" @click="paymentMethod = 'wallet'">钱包支付</button>
      </div>
    </section>

    <footer class="bottom-bar">
      <div class="amount">应付：¥{{ product?.price || 0 }}</div>
      <button class="submit-btn" :disabled="submitting || !product" @click="submitOrder">
        {{ submitting ? '提交中...' : '提交订单去支付' }}
      </button>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createVoucherOrder } from '@/api/welfareApi'
import { notify } from '@/utils/notify'

interface ProductSnapshot {
  id: number
  title: string
  imageUrl: string
  price: number
  originalPrice: number
  shopName?: string
}

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const paymentMethod = ref<'alipay' | 'wechat' | 'wallet'>('alipay')
const couponCacheKey = (couponId: number | string) => `currentCoupon:${couponId}`

const product = computed<ProductSnapshot | null>(() => {
  const couponId = (route.query.couponId as string) || sessionStorage.getItem('currentCouponId') || ''
  const raw = (couponId ? sessionStorage.getItem(couponCacheKey(couponId)) : null)
    || sessionStorage.getItem('currentCoupon')
    || sessionStorage.getItem('selectedProduct')
  if (!raw) return null
  try {
    const data = JSON.parse(raw)
    if (couponId && Number(data.id || 0) !== Number(couponId)) {
      return null
    }
    return {
      id: Number(data.id),
      title: data.title,
      imageUrl: data.imageUrl || data.image || '',
      price: Number(data.price || 0),
      originalPrice: Number(data.originalPrice || 0),
      shopName: data.shopName
    }
  } catch {
    return null
  }
})

const goBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  if (product.value?.id) {
    router.replace(`/group-buy/${product.value.id}`)
    return
  }
  router.replace('/welfare')
}

const submitOrder = async () => {
  if (!product.value || submitting.value) return
  submitting.value = true
  try {
    const order = await createVoucherOrder(product.value.id)
    router.push({
      path: '/payment',
      query: {
        orderNo: order.orderNo,
        paymentMethod: paymentMethod.value,
        returnTo: 'detail',
        couponId: String(product.value.id)
      }
    })
  } catch (e: any) {
    notify(e?.message || '下单失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.group-buy-order {
  min-height: 100vh;
  background: #f6f7fb;
  padding-bottom: 84px;
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

.back-btn, .placeholder {
  width: 52px;
}

.back-btn {
  border: none;
  background: transparent;
  color: #1677ff;
}

.card {
  margin: 12px;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
}

.state-card {
  margin: 12px;
  border-radius: 8px;
  background: #fff;
  color: #666;
  text-align: center;
  padding: 20px 12px;
}

.product-image {
  width: 100%;
  height: 180px;
  border-radius: 8px;
  object-fit: cover;
}

.product-content h2 {
  margin: 10px 0 4px;
  font-size: 18px;
}

.shop {
  margin: 0 0 8px;
  color: #666;
  font-size: 13px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price {
  color: #e53935;
  font-size: 24px;
  font-weight: 700;
}

.origin {
  color: #999;
  text-decoration: line-through;
}

.pay-options {
  display: flex;
  gap: 8px;
}

.pay-option {
  flex: 1;
  height: 38px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: #fff;
}

.pay-option.active {
  border-color: #1677ff;
  color: #1677ff;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 64px;
  background: #fff;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
}

.amount {
  font-size: 18px;
  color: #e53935;
  font-weight: 700;
}

.submit-btn {
  border: none;
  background: #ff4d4f;
  color: #fff;
  padding: 10px 16px;
  border-radius: 999px;
}

.submit-btn:disabled {
  background: #c7c7c7;
}
</style>
