<template>
  <div class="refund-apply-page">
    <header class="page-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <h1>申请退款</h1>
      <div class="header-placeholder"></div>
    </header>

    <div v-if="loading" class="state-card">订单信息加载中...</div>
    <div v-else-if="error" class="state-card error">{{ error }}</div>

    <template v-else-if="order">
      <section class="hero-card">
        <div>
          <p class="hero-label">当前订单</p>
          <h2>{{ order.voucherTitle || '优惠券订单' }}</h2>
          <p class="hero-desc">{{ order.voucherSubTitle || '未核销前支持退款' }}</p>
        </div>
        <div class="amount-block">
          <span>实付金额</span>
          <strong>¥{{ maxRefundAmount.toFixed(2) }}</strong>
        </div>
      </section>

      <section class="section-card">
        <h3>退款说明</h3>
        <ul class="tips-list">
          <li>仅支持已支付且未核销订单申请退款。</li>
          <li>提交后将先进入商家审核，必要时由平台管理员继续处理。</li>
          <li>审核通过后，退款将按原支付方式返回。</li>
          <li>请核对退款金额，提交后以平台处理结果为准。</li>
        </ul>
      </section>

      <section class="section-card" v-if="refundAllowed">
        <h3>退款金额</h3>
        <div class="amount-input-wrap">
          <span class="currency">¥</span>
          <input
            v-model="refundAmountInput"
            type="number"
            min="0.01"
            step="0.01"
            :max="maxRefundAmount.toFixed(2)"
            placeholder="请输入退款金额"
            @blur="normalizeRefundAmountInput"
          />
        </div>
        <p class="helper-text">最多可退 ¥{{ maxRefundAmount.toFixed(2) }}</p>
        <div class="quick-actions">
          <button class="quick-btn" @click="fillRefundAmount(maxRefundAmount)">全额退款</button>
          <button class="quick-btn" @click="fillRefundAmount(halfRefundAmount)">退一半</button>
        </div>
      </section>

      <section class="section-card" v-if="refundAllowed">
        <h3>退款原因</h3>
        <div class="reason-list">
          <button
            v-for="item in reasons"
            :key="item"
            :class="['reason-chip', { active: selectedReason === item }]"
            @click="selectedReason = item"
          >
            {{ item }}
          </button>
        </div>
        <textarea
          v-model.trim="customReason"
          class="reason-input"
          maxlength="60"
          placeholder="补充说明（选填）"
        ></textarea>
      </section>

      <section class="section-card blocked-card" v-else>
        <h3>当前不可退款</h3>
        <p>{{ refundBlockedMessage }}</p>
      </section>

      <section class="section-card">
        <h3>订单信息</h3>
        <p>订单号：{{ order.orderNo }}</p>
        <p>支付方式：{{ paymentMethodText(order.paymentMethod) }}</p>
        <p v-if="order.payTime">支付时间：{{ formatTime(order.payTime) }}</p>
      </section>
    </template>

    <footer class="submit-footer" v-if="order">
      <div class="footer-amount">
        <span>退款金额</span>
        <strong>¥{{ previewAmount.toFixed(2) }}</strong>
      </div>
      <button class="submit-btn" :disabled="submitting || !refundAllowed" @click="handleSubmit">
        {{ submitting ? '提交中...' : '提交退款申请' }}
      </button>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { refundPayment } from '@/api/payment'
import { getMyOrderDetailByOrderNo, type OrderItem } from '@/api/order'
import { showAlert, showConfirm } from '@/utils/dialog'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)
const order = ref<OrderItem | null>(null)
const refundAmountInput = ref('')
const selectedReason = ref('买错了，不想用了')
const customReason = ref('')
const returnTo = ref('')
const orderTab = ref('')

const reasons = ['买错了，不想用了', '行程变动，无法到店', '商家信息不符合预期', '其他原因']

const maxRefundAmount = computed(() => Number(order.value?.payAmount || 0))
const halfRefundAmount = computed(() => {
  const half = Math.floor((maxRefundAmount.value / 2) * 100) / 100
  return half >= 0.01 ? half : maxRefundAmount.value
})

const previewAmount = computed(() => {
  const value = Number(refundAmountInput.value || 0)
  if (!Number.isFinite(value) || value <= 0) return maxRefundAmount.value
  return Math.min(value, maxRefundAmount.value)
})

const refundAllowed = computed(() => {
  if (!order.value) return false
  return order.value.status === 1 && !order.value.useTime && maxRefundAmount.value > 0
})

const refundBlockedMessage = computed(() => {
  if (!order.value) return '订单信息不存在'
  if (order.value.status === 4) return '该订单已退款，无需重复申请。'
  if (order.value.status === 2) return '订单已使用完成，当前不支持退款。'
  if (order.value.status === 5) return '订单已取消，无需再次申请退款。'
  if (order.value.useTime) return '订单已经核销，当前不支持退款。'
  if (order.value.status === 0) return '订单尚未支付，请先完成支付或取消订单。'
  return '当前订单状态暂不支持退款。'
})

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

const fillRefundAmount = (value: number) => {
  refundAmountInput.value = value.toFixed(2)
}

const normalizeRefundAmountInput = () => {
  const amount = Number(refundAmountInput.value)
  if (!Number.isFinite(amount) || amount <= 0) {
    refundAmountInput.value = maxRefundAmount.value > 0 ? maxRefundAmount.value.toFixed(2) : ''
    return
  }
  refundAmountInput.value = Math.min(amount, maxRefundAmount.value).toFixed(2)
}

const loadOrder = async () => {
  const orderNo = route.query.orderNo as string
  if (!orderNo) {
    error.value = '缺少订单号'
    return
  }

  loading.value = true
  error.value = null
  try {
    const detail = await getMyOrderDetailByOrderNo(orderNo)
    order.value = detail
    refundAmountInput.value = Number(detail.payAmount || 0).toFixed(2)
    normalizeRefundAmountInput()
  } catch (e: any) {
    error.value = e?.message || '加载订单失败'
  } finally {
    loading.value = false
  }
}

const buildReason = () => {
  const extra = customReason.value.trim()
  if (selectedReason.value === '其他原因') {
    return extra || '其他原因'
  }
  return extra ? `${selectedReason.value}：${extra}` : selectedReason.value
}

const handleSubmit = async () => {
  if (!order.value) {
    await showAlert('订单信息不存在')
    return
  }
  if (!refundAllowed.value) {
    await showAlert(refundBlockedMessage.value)
    return
  }

  const amount = Number(refundAmountInput.value)
  if (!Number.isFinite(amount) || amount <= 0) {
    await showAlert('请输入正确的退款金额')
    return
  }
  if (amount > maxRefundAmount.value) {
    await showAlert(`退款金额不能超过 ¥${maxRefundAmount.value.toFixed(2)}`)
    return
  }
  if (!(await showConfirm(`确认提交退款申请 ¥${amount.toFixed(2)} 吗？`, '提交退款申请'))) {
    return
  }

  submitting.value = true
  try {
    const result = await refundPayment({
      orderNo: order.value.orderNo,
      refundAmount: amount,
      reason: buildReason()
    })
    router.replace({
      path: '/payment/refund/result',
      query: {
        orderNo: order.value.orderNo,
        refundNo: result.refundNo,
        refundAmount: String(result.refundAmount),
        status: result.status,
        ...(returnTo.value ? { returnTo: returnTo.value } : {}),
        ...(orderTab.value ? { orderTab: orderTab.value } : {})
      }
    })
  } catch (e: any) {
    await showAlert(e?.message || '申请退款失败')
  } finally {
    submitting.value = false
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
  const orderNo = route.query.orderNo as string
  if (orderNo) {
    router.replace({ path: '/me/orders/0', query: { orderNo } })
    return
  }
  router.replace('/me/orders')
}

onMounted(() => {
  returnTo.value = (route.query.returnTo as string) || ''
  orderTab.value = (route.query.orderTab as string) || ''
  void loadOrder()
})
</script>

<style scoped>
.refund-apply-page {
  min-height: 100vh;
  background: #f6f7fb;
  padding-bottom: 88px;
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

.state-card,
.section-card {
  margin: 12px;
  background: #fff;
  border-radius: 14px;
  padding: 14px;
}

.state-card.error {
  color: #b91c1c;
}

.hero-card {
  margin: 12px;
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(145deg, #1f2937, #374151);
  color: #fff;
}

.hero-label {
  margin: 0 0 8px;
  font-size: 12px;
  opacity: 0.75;
}

.hero-card h2 {
  margin: 0;
  font-size: 22px;
}

.hero-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.78);
}

.amount-block {
  margin-top: 16px;
  display: flex;
  align-items: end;
  justify-content: space-between;
}

.amount-block span {
  font-size: 13px;
  opacity: 0.78;
}

.amount-block strong {
  font-size: 30px;
}

.section-card h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #4b5563;
  line-height: 1.7;
  font-size: 13px;
}

.amount-input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px 14px;
}

.currency {
  color: #ef4444;
  font-size: 24px;
  font-weight: 700;
}

.amount-input-wrap input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 24px;
  color: #111827;
  background: transparent;
}

.helper-text {
  margin: 10px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.quick-actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.quick-btn {
  border: none;
  border-radius: 999px;
  padding: 8px 14px;
  background: #eef2ff;
  color: #4338ca;
  font-size: 13px;
}

.reason-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.reason-chip {
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  padding: 8px 12px;
  background: #fff;
  color: #374151;
  font-size: 13px;
}

.reason-chip.active {
  border-color: #ef4444;
  background: #fff1f2;
  color: #be123c;
}

.reason-input {
  width: 100%;
  margin-top: 12px;
  min-height: 92px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
  font-size: 14px;
  box-sizing: border-box;
  resize: vertical;
}

.blocked-card {
  border: 1px solid #fecaca;
  background: #fff7f7;
  color: #991b1b;
}

.section-card p {
  margin: 8px 0 0;
  color: #4b5563;
  font-size: 13px;
}

.submit-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid #e5e7eb;
  padding: 12px;
}

.footer-amount {
  display: flex;
  flex-direction: column;
}

.footer-amount span {
  font-size: 12px;
  color: #6b7280;
}

.footer-amount strong {
  margin-top: 4px;
  font-size: 24px;
  color: #dc2626;
}

.submit-btn {
  min-width: 132px;
  height: 46px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #ef4444, #f97316);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}

.submit-btn:disabled {
  background: #cbd5e1;
}
</style>
