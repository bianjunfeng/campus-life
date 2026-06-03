<template>
  <div class="payment-page">
    <header class="page-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <h1>确认支付</h1>
      <div class="header-placeholder"></div>
    </header>

    <section class="order-hero" v-if="orderInfo">
      <div class="order-main">
        <p class="label">待支付金额</p>
        <h2>¥{{ Number(orderInfo.amount || 0).toFixed(2) }}</h2>
        <p class="subject">{{ orderInfo.subject || '优惠券订单' }}</p>
      </div>
      <div class="order-extra">
        <span>订单号</span>
        <strong>{{ orderInfo.orderNo }}</strong>
      </div>
    </section>

    <section class="method-card">
      <h3>选择支付方式</h3>
      <div class="method-list">
        <button class="method-item" :class="{ active: selectedMethod === 'wallet' }" @click="selectedMethod = 'wallet'">
          <div class="left">
            <span class="icon">👛</span>
            <div>
              <p class="name">钱包支付</p>
              <p class="desc">优先使用钱包余额</p>
            </div>
          </div>
          <span class="check">{{ selectedMethod === 'wallet' ? '●' : '○' }}</span>
        </button>

        <button class="method-item" :class="{ active: selectedMethod === 'alipay' }" @click="selectedMethod = 'alipay'">
          <div class="left">
            <span class="icon">💰</span>
            <div>
              <p class="name">支付宝</p>
              <p class="desc">官方安全支付</p>
            </div>
          </div>
          <span class="check">{{ selectedMethod === 'alipay' ? '●' : '○' }}</span>
        </button>

        <button class="method-item" :class="{ active: selectedMethod === 'wechat' }" @click="selectedMethod = 'wechat'">
          <div class="left">
            <span class="icon">💳</span>
            <div>
              <p class="name">微信支付</p>
              <p class="desc">微信快捷支付</p>
            </div>
          </div>
          <span class="check">{{ selectedMethod === 'wechat' ? '●' : '○' }}</span>
        </button>
      </div>
    </section>

    <section class="tips-card">
      <p>支付说明</p>
      <ul>
        <li>未支付订单将自动取消，请及时完成支付。</li>
        <li>支付成功后可在“我的订单”中查看并使用。</li>
      </ul>
    </section>

    <section v-if="wechatPayPayload" class="wechat-card">
      <p class="wechat-title">微信支付</p>
      <p class="wechat-tip">
        {{ isWechatDeepLink ? '已尝试调起微信支付，若未跳转可点击下方按钮重试。' : '请使用微信扫码或复制下方内容在支持环境中完成支付。' }}
      </p>
      <img v-if="isWechatQrImage" :src="wechatPayPayload" alt="微信支付二维码" class="wechat-qr-image" />
      <code v-else class="wechat-code">{{ wechatPayPayload }}</code>
      <div class="wechat-actions">
        <button v-if="isWechatDeepLink" class="wechat-action-btn" @click="openWechatPay">重新调起</button>
        <button v-else class="wechat-action-btn secondary" @click="copyWechatPayload">复制支付内容</button>
      </div>
    </section>

    <footer class="pay-footer">
      <div class="left">
        <span>实付</span>
        <strong>¥{{ Number(orderInfo?.amount || 0).toFixed(2) }}</strong>
      </div>
      <button class="pay-btn" :disabled="loading || !selectedMethod || !orderInfo" @click="handlePay">
        {{ loading ? '处理中...' : '立即支付' }}
      </button>
    </footer>

    <div v-if="payForm" v-html="payForm" style="display: none"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createPayment, getPaymentOrderInfo, queryPaymentStatus, type PaymentRequest, type PaymentResponse } from '@/api/payment'
import { notify as toast } from '@/utils/notify'
import { resolvePaymentDisplayState } from '@/utils/paymentStatus'

const router = useRouter()
const route = useRoute()

const orderNo = ref('')
const selectedMethod = ref<'alipay' | 'wechat' | 'wallet'>('wallet')
const loading = ref(false)
const payForm = ref('')
const orderInfo = ref<{ orderNo: string; amount: number; subject?: string } | null>(null)
const returnTo = ref('')
const couponId = ref('')
const orderTab = ref('')
const wechatPayPayload = ref('')
let paymentStatusTimer: number | null = null
let paymentStatusTimeout: number | null = null

const showNotify = (type: 'success' | 'error' | 'info', message: string) => {
  if (type === 'error') console.error(message)
  else console.log(message)
  toast(message)
}

const isDeepLinkValue = (value: string) => /^(weixin|wechat):\/\//i.test(value)
const isImageValue = (value: string) => /^data:image\//i.test(value) || /^https?:\/\//i.test(value)
const isWechatDeepLink = computed(() => isDeepLinkValue(wechatPayPayload.value))
const isWechatQrImage = computed(() => !isWechatDeepLink.value && isImageValue(wechatPayPayload.value))

onMounted(() => {
  const orderNoParam = (route.params.orderNo as string) || (route.query.orderNo as string)
  if (orderNoParam) {
    orderNo.value = orderNoParam
    loadOrderInfo(orderNoParam)
  }
  const method = route.query.paymentMethod as string
  if (method === 'alipay' || method === 'wechat' || method === 'wallet') {
    selectedMethod.value = method
  }
  returnTo.value = (route.query.returnTo as string) || ''
  couponId.value = (route.query.couponId as string) || ''
  orderTab.value = (route.query.orderTab as string) || ''
})

onUnmounted(() => {
  stopPollingPaymentStatus()
})

const loadOrderInfo = async (currentOrderNo: string) => {
  try {
    const info = await getPaymentOrderInfo(currentOrderNo)
    orderInfo.value = {
      orderNo: info.orderNo,
      amount: Number(info.amount || 0),
      subject: info.subject
    }
  } catch (e: any) {
    showNotify('error', e?.message || '加载订单信息失败')
  }
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

const openWechatPay = () => {
  if (!wechatPayPayload.value || !isWechatDeepLink.value) return
  window.location.href = wechatPayPayload.value
}

const copyWechatPayload = async () => {
  if (!wechatPayPayload.value) return
  try {
    await navigator.clipboard.writeText(wechatPayPayload.value)
    showNotify('success', '支付内容已复制')
  } catch (e) {
    console.error('复制支付内容失败', e)
    showNotify('error', '复制失败，请手动重试')
  }
}

const handlePay = async () => {
  if (!orderNo.value || !selectedMethod.value || !orderInfo.value) {
    showNotify('error', '订单信息不完整')
    return
  }

  loading.value = true
  try {
    stopPollingPaymentStatus()
    wechatPayPayload.value = ''
    const request: PaymentRequest = {
      orderNo: orderNo.value,
      paymentMethod: selectedMethod.value,
      amount: orderInfo.value.amount,
      subject: orderInfo.value.subject
    }
    if (returnTo.value || couponId.value || orderTab.value) {
      const params: string[] = []
      if (returnTo.value) params.push(`returnTo=${encodeURIComponent(returnTo.value)}`)
      if (couponId.value) params.push(`couponId=${encodeURIComponent(couponId.value)}`)
      if (orderTab.value) params.push(`orderTab=${encodeURIComponent(orderTab.value)}`)
      params.push(`paymentMethod=${encodeURIComponent(selectedMethod.value)}`)
      request.passbackParams = params.join('&')
    }

    const response: PaymentResponse = await createPayment(request)

    if (response.paymentMethod === 'wallet') {
      stopPollingPaymentStatus()
      showNotify('success', '钱包支付成功')
      router.push({
        path: '/payment/success',
        query: {
          orderNo: orderNo.value,
          paymentMethod: selectedMethod.value,
          ...(returnTo.value ? { returnTo: returnTo.value } : {}),
          ...(couponId.value ? { couponId: couponId.value } : {}),
          ...(orderTab.value ? { orderTab: orderTab.value } : {})
        }
      })
      return
    }

    if (response.payForm) {
      stopPollingPaymentStatus()
      payForm.value = response.payForm
      setTimeout(() => {
        const form = document.querySelector('form[action*="alipay"]') as HTMLFormElement
        if (form) form.submit()
      }, 100)
      return
    }

    if (response.payUrl) {
      stopPollingPaymentStatus()
      window.location.href = response.payUrl
      return
    }

    if (response.qrCode) {
      wechatPayPayload.value = response.qrCode
      if (isDeepLinkValue(response.qrCode)) {
        showNotify('info', '正在尝试调起微信支付')
        openWechatPay()
      } else {
        showNotify('info', '请使用微信扫码支付')
      }
      startPollingPaymentStatus()
      return
    }

    showNotify('error', '支付方式暂不支持')
  } catch (e: any) {
    showNotify('error', e?.message || '创建支付订单失败')
  } finally {
    loading.value = false
  }
}

const stopPollingPaymentStatus = () => {
  if (paymentStatusTimer) {
    clearInterval(paymentStatusTimer)
    paymentStatusTimer = null
  }
  if (paymentStatusTimeout) {
    clearTimeout(paymentStatusTimeout)
    paymentStatusTimeout = null
  }
}

const startPollingPaymentStatus = () => {
  stopPollingPaymentStatus()
  paymentStatusTimer = window.setInterval(async () => {
    try {
      const status = await queryPaymentStatus(orderNo.value)
      const nextState = resolvePaymentDisplayState(status.paymentStatus, status.orderStatus)
      if (nextState === 'success' || nextState === 'refunded') {
        stopPollingPaymentStatus()
        showNotify('success', '支付成功')
        router.push({
          path: '/payment/success',
          query: {
            orderNo: orderNo.value,
            paymentMethod: selectedMethod.value,
            ...(returnTo.value ? { returnTo: returnTo.value } : {}),
            ...(couponId.value ? { couponId: couponId.value } : {}),
            ...(orderTab.value ? { orderTab: orderTab.value } : {})
          }
        })
      } else if (nextState === 'failed') {
        stopPollingPaymentStatus()
        showNotify('error', '支付失败')
        router.push({
          path: '/payment/failed',
          query: {
            orderNo: orderNo.value,
            paymentMethod: selectedMethod.value,
            ...(returnTo.value ? { returnTo: returnTo.value } : {}),
            ...(couponId.value ? { couponId: couponId.value } : {}),
            ...(orderTab.value ? { orderTab: orderTab.value } : {})
          }
        })
      }
    } catch (err) {
      console.error('查询支付状态失败', err)
    }
  }, 3000)

  paymentStatusTimeout = window.setTimeout(() => {
    stopPollingPaymentStatus()
  }, 30000)
}
</script>

<style scoped>
.payment-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 86px;
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

.order-hero {
  margin: 10px 12px;
  border-radius: 14px;
  padding: 14px;
  background: linear-gradient(135deg, #ff3b5c, #ff5a3f);
  color: #fff;
}

.order-main .label {
  margin: 0;
  font-size: 13px;
  opacity: 0.9;
}

.order-main h2 {
  margin: 6px 0 2px;
  font-size: 34px;
  line-height: 1;
}

.order-main .subject {
  margin: 0;
  font-size: 13px;
  opacity: 0.95;
}

.order-extra {
  margin-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.28);
  padding-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.method-card,
.tips-card {
  margin: 10px 12px;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
}

.method-card h3 {
  margin: 0 0 8px;
  font-size: 15px;
}

.method-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.method-item {
  width: 100%;
  border: 1px solid #ececec;
  border-radius: 10px;
  background: #fff;
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  text-align: left;
}

.method-item.active {
  border-color: #ff4d4f;
  background: #fff5f5;
}

.method-item .left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.method-item .icon {
  width: 28px;
  text-align: center;
  font-size: 22px;
}

.method-item .name {
  margin: 0;
  font-size: 14px;
  color: #222;
}

.method-item .desc {
  margin: 2px 0 0;
  font-size: 12px;
  color: #888;
}

.method-item .check {
  color: #ff4d4f;
}

.tips-card p {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
}

.tips-card ul {
  margin: 0;
  padding-left: 18px;
  color: #666;
  line-height: 1.6;
  font-size: 13px;
}

.wechat-card {
  margin: 10px 12px;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
}

.wechat-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
}

.wechat-tip {
  margin: 8px 0 0;
  color: #666;
  font-size: 13px;
  line-height: 1.5;
}

.wechat-qr-image {
  display: block;
  width: min(220px, 100%);
  margin: 12px auto 0;
  border-radius: 12px;
  background: #f5f5f5;
}

.wechat-code {
  display: block;
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f7f7f7;
  color: #333;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-all;
}

.wechat-actions {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.wechat-action-btn {
  border: none;
  border-radius: 999px;
  background: #07c160;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  padding: 0 18px;
  height: 38px;
}

.wechat-action-btn.secondary {
  background: #1f2937;
}

.pay-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 74px;
  background: #fff;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px calc(env(safe-area-inset-bottom) + 6px);
}

.pay-footer .left {
  display: flex;
  flex-direction: column;
}

.pay-footer .left span {
  font-size: 12px;
  color: #777;
}

.pay-footer .left strong {
  margin-top: 2px;
  font-size: 24px;
  line-height: 1;
  color: #e03131;
}

.pay-btn {
  min-width: 132px;
  height: 46px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff2f5f, #ff3b30);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.pay-btn:disabled {
  background: #c9c9c9;
}
</style>
