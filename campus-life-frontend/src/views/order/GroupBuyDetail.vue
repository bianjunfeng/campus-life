<template>
  <div class="group-buy-detail">
    <header class="page-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <div class="tab-switch">
        <span class="active">商品</span>
        <span>测评</span>
      </div>
      <div class="header-actions">
        <button class="icon-btn">☆</button>
        <button class="icon-btn">⋯</button>
      </div>
    </header>

    <div v-if="loading" class="state">详情加载中...</div>
    <div v-else-if="error" class="state error">{{ error }}</div>

    <template v-else>
      <section class="gallery-card">
        <img class="main-image" :src="detail.image || fallbackImage" :alt="detail.title" />
        <div class="side-images">
          <img :src="detail.image || fallbackImage" alt="图1" />
          <img :src="detail.image || fallbackImage" alt="图2" />
        </div>
        <div class="album-tag">图集 1/3</div>
      </section>

      <section class="sku-strip">
        <div class="sku-item active">
          <img :src="detail.image || fallbackImage" alt="sku" />
          <span>默认规格</span>
        </div>
        <div class="sku-item">
          <img :src="detail.image || fallbackImage" alt="sku2" />
          <span>大份组合</span>
        </div>
      </section>

      <section class="price-panel" :class="{ seckill: detail.isSeckill }">
        <div class="price-main">¥{{ money(detail.price) }}</div>
        <div class="price-meta">
          <span>{{ discountText }}折</span>
          <span class="origin">¥{{ money(detail.originalPrice) }}</span>
        </div>
        <div class="sold">已售{{ detail.salesCount || 0 }}+</div>
        <div v-if="detail.isSeckill" class="seckill-box">
          <div class="left">京东秒杀</div>
          <div class="right">还剩 {{ countdownText }}</div>
        </div>
      </section>

      <section class="card quick-benefits">
        <div class="row"><span>可再享</span><strong>优惠换购</strong></div>
        <div class="row"><span>开通享</span><strong>先享后付，享0元下单</strong></div>
      </section>

      <section class="card info-card">
        <h2>{{ detail.title }}</h2>
        <div class="tags">
          <span>买贵双倍赔</span>
          <span>24小时热度飙升</span>
          <span>近1周300+人加购</span>
        </div>
        <p class="merchant">{{ detail.shopName }} · {{ detail.shopAddressFull || detail.shopAddress || detail.shopLocation || '校园商圈' }}</p>
        <p class="merchant-location">
          {{ detailDistanceText }}
          <span v-if="detail.shopLongitude && detail.shopLatitude"> · {{ Number(detail.shopLongitude).toFixed(6) }}, {{ Number(detail.shopLatitude).toFixed(6) }}</span>
        </p>
        <p class="desc">{{ detail.couponInfo || detail.description || '到店核销使用，具体规则以商家公告为准。' }}</p>
      </section>

      <section class="card rule-card">
        <h3>使用规则</h3>
        <ul>
          <li>每个账号限购 1 张，购买后在订单页查看。</li>
          <li>普通券下单后进入支付页，秒杀券需排队抢购。</li>
          <li>有效期：{{ detail.validPeriod || '以门店公告为准' }}。</li>
          <li>营业时间：{{ detail.businessHours || '10:00-22:00' }}。</li>
        </ul>
      </section>
    </template>

    <footer v-if="!loading && !error" class="bottom-bar">
      <div class="bar-icons">
        <button>店铺</button>
        <button>客服</button>
        <button @click="goCart">购物车</button>
      </div>
      <button v-if="canAddToCart" class="add-cart-btn" :disabled="submitting || detail.stock <= 0" @click="addToCart">
        加入购物车
      </button>
      <button class="buy-btn" :disabled="submitting || detail.stock <= 0 || (detail.isSeckill && grabbedSeckill)" @click="buyNow">
        <span class="price">到手价¥{{ money(detail.price) }}</span>
        <span class="sub">{{ actionText }}</span>
      </button>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createVoucherOrder, getCouponDetail, grabCoupon, queryGrabResult, type Countdown, type CouponItem } from '../../api/welfareApi'
import { getMyOrders } from '../../api/order'
import { addCartItem } from '../../api/cart'
import { notify } from '@/utils/notify'

type DetailCoupon = CouponItem & {
  image?: string
  isSeckill: boolean
  countdownSeconds: number
}

const route = useRoute()
const router = useRouter()
const fallbackImage = 'https://picsum.photos/800/400?blur=1'

const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const grabbedSeckill = ref(false)
const detail = ref<DetailCoupon>({
  id: 0,
  title: '',
  description: '',
  price: 0,
  originalPrice: 0,
  categoryId: 0,
  stock: 0,
  image: '',
  salesCount: 0,
  rating: 4.6,
  shopName: '',
  shopAddress: '',
  validPeriod: '',
  businessHours: '',
  couponInfo: '',
  isSeckill: false,
  countdownSeconds: 0
})

let countdownTimer: number | null = null
let realtimeTimer: number | null = null
const userPos = ref<{ lng: number; lat: number } | null>(null)
const couponCacheKey = (couponId: number | string) => `currentCoupon:${couponId}`

const toSeconds = (c?: Countdown) => {
  if (!c) return 0
  return Math.max((c.hours || 0) * 3600 + (c.minutes || 0) * 60 + (c.seconds || 0), 0)
}

const normalize = (item: CouponItem): DetailCoupon => ({
  ...item,
  image: item.image || '',
  isSeckill: !!item.isSeckill,
  countdownSeconds: item.isSeckill ? toSeconds(item.countdown) : 0
})

const countdownText = computed(() => {
  const sec = Math.max(detail.value.countdownSeconds || 0, 0)
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  const s = sec % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const money = (v: unknown) => Number(v || 0).toFixed(1)

const discountText = computed(() => {
  if (detail.value.discount && detail.value.discount > 0) return Number(detail.value.discount).toFixed(1)
  const p = Number(detail.value.price || 0)
  const o = Number(detail.value.originalPrice || 0)
  if (o <= 0 || p <= 0) return '0.0'
  return ((p / o) * 10).toFixed(1)
})

const calcDistanceMeters = () => {
  if (detail.value.distance && Number(detail.value.distance) > 0) return Number(detail.value.distance)
  if (!userPos.value) return 0
  const lng2 = Number(detail.value.shopLongitude || 0)
  const lat2 = Number(detail.value.shopLatitude || 0)
  if (!lng2 || !lat2) return 0
  const toRad = (v: number) => (v * Math.PI) / 180
  const earthRadius = 6371000
  const dLat = toRad(lat2 - userPos.value.lat)
  const dLng = toRad(lng2 - userPos.value.lng)
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(toRad(userPos.value.lat)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return Math.round(earthRadius * c)
}

const detailDistanceText = computed(() => {
  const d = calcDistanceMeters()
  if (!d) return '附近'
  return d >= 1000 ? `${(d / 1000).toFixed(1)}km` : `${d}m`
})

const actionText = computed(() => {
  if (detail.value.isSeckill && grabbedSeckill.value) return '已抢购'
  if (detail.value.stock <= 0) return '已售罄'
  if (submitting.value) return '处理中...'
  if (detail.value.isSeckill) return `还剩 ${countdownText.value} 立即秒杀`
  return '立即购买'
})

const canAddToCart = computed(() => !detail.value.isSeckill)

const startTimer = () => {
  if (countdownTimer) window.clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    if (!detail.value.isSeckill) return
    detail.value.countdownSeconds = Math.max(detail.value.countdownSeconds - 1, 0)
  }, 1000)
}

const loadDetail = async (silent = false) => {
  if (!silent) {
    loading.value = true
    error.value = ''
  }
  try {
    const id = Number(route.params.id)
    if (!id) throw new Error('无效优惠券ID')

    const remote = await getCouponDetail(id)
    detail.value = normalize(remote)
    await loadGrabbedSeckillState(detail.value.id)
    sessionStorage.setItem('currentCoupon', JSON.stringify(remote))
    sessionStorage.setItem('currentCouponId', String(remote.id))
    sessionStorage.setItem(couponCacheKey(remote.id), JSON.stringify(remote))
    startTimer()
    if (detail.value.isSeckill && !realtimeTimer) {
      realtimeTimer = window.setInterval(() => {
        loadDetail(true).catch(() => {})
      }, 5000)
    }
    if (!detail.value.isSeckill && realtimeTimer) {
      window.clearInterval(realtimeTimer)
      realtimeTimer = null
    }
  } catch (e: any) {
    if (silent) {
      return
    }
    const couponId = Number(route.params.id)
    const cache = sessionStorage.getItem(couponCacheKey(couponId))
      || sessionStorage.getItem('currentCoupon')
    if (!cache) {
      error.value = e?.message || '详情加载失败'
      return
    }
    try {
      const parsed = JSON.parse(cache)
      if (Number(parsed?.id || 0) !== couponId) {
        throw new Error('优惠券缓存不匹配')
      }
      detail.value = normalize(parsed)
      startTimer()
    } catch {
      error.value = e?.message || '详情加载失败'
    }
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

const loadGrabbedSeckillState = async (voucherId: number) => {
  if (!detail.value.isSeckill) {
    grabbedSeckill.value = false
    return
  }
  try {
    const orderData = await getMyOrders({ page: 1, size: 200 })
    grabbedSeckill.value = (orderData.list || []).some((order: any) => {
      return String(order.orderSource || '') === 'seckill'
        && Number(order.voucherId || 0) === Number(voucherId || detail.value.id)
        && Number(order.status ?? -1) !== 5
    })
  } catch {
    grabbedSeckill.value = false
  }
}

const buyNow = async () => {
  if (submitting.value || detail.value.stock <= 0 || (detail.value.isSeckill && grabbedSeckill.value)) return
  submitting.value = true
  error.value = ''
  try {
    if (!detail.value.isSeckill) {
      const order = await createVoucherOrder(detail.value.id)
      router.push({
        path: '/payment',
        query: {
          orderNo: order.orderNo,
          amount: String(order.payAmount),
          returnTo: 'detail',
          couponId: String(detail.value.id)
        }
      })
      return
    }

    const submit = await grabCoupon(detail.value.id)
    if (!submit.success || !submit.orderNo) {
      throw new Error(submit.message || '抢购失败')
    }

    for (let i = 0; i < 15; i++) {
      await new Promise((resolve) => setTimeout(resolve, 300))
      const result = await queryGrabResult(submit.orderNo)
      if (result.status === 'SUCCESS') {
        detail.value.stock = Math.max(detail.value.stock - 1, 0)
        grabbedSeckill.value = true
        notify('秒杀成功，正在前往支付页')
        router.push({
          path: '/payment',
          query: {
            orderNo: submit.orderNo,
            returnTo: 'detail',
            couponId: String(detail.value.id)
          }
        })
        return
      }
      if (result.status === 'FAIL' || result.status === 'NOT_FOUND') {
        throw new Error(result.message || '抢购失败')
      }
    }

    throw new Error('排队中，请稍后在订单页查看结果')
  } catch (e: any) {
    error.value = e?.message || '抢购失败'
  } finally {
    submitting.value = false
  }
}

const addToCart = async () => {
  if (submitting.value || detail.value.stock <= 0 || (detail.value.isSeckill && grabbedSeckill.value)) return
  submitting.value = true
  error.value = ''
  try {
    await addCartItem(detail.value.id, 1)
    notify('已加入购物车')
  } catch (e: any) {
    error.value = e?.message || '加入购物车失败'
  } finally {
    submitting.value = false
  }
}

const goCart = () => router.push('/cart')

const goBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  router.replace('/welfare')
}

onMounted(() => {
  const cachedPos = localStorage.getItem('welfare:lastPos')
  if (cachedPos) {
    try {
      const parsed = JSON.parse(cachedPos)
      const lng = Number(parsed?.lng)
      const lat = Number(parsed?.lat)
      if (!Number.isNaN(lng) && !Number.isNaN(lat)) {
        userPos.value = { lng, lat }
      }
    } catch {
      userPos.value = null
    }
  }
  loadDetail()
})
onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
  if (realtimeTimer) window.clearInterval(realtimeTimer)
})
</script>

<style scoped>
.group-buy-detail {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 92px;
}

.page-header {
  height: 52px;
  background: #fff;
  display: grid;
  grid-template-columns: 40px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  border-bottom: 1px solid #eee;
}

.back-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 20px;
  background: #f4f4f4;
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.tab-switch {
  display: inline-flex;
  align-items: center;
  background: #eceff3;
  border-radius: 8px;
  padding: 2px;
  width: fit-content;
}

.tab-switch span {
  padding: 6px 14px;
  font-size: 14px;
  color: #666;
}

.tab-switch .active {
  background: #fff;
  border-radius: 6px;
  color: #111;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 6px;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 16px;
  background: #f4f4f4;
  color: #666;
}

.gallery-card {
  margin: 10px 12px;
  border-radius: 14px;
  overflow: hidden;
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 8px;
  position: relative;
}

.main-image {
  width: 100%;
  height: 280px;
  object-fit: cover;
  border-radius: 14px;
  background: #efefef;
}

.side-images {
  display: grid;
  gap: 8px;
}

.side-images img {
  width: 100%;
  height: 136px;
  object-fit: cover;
  border-radius: 14px;
  background: #efefef;
}

.album-tag {
  position: absolute;
  right: 10px;
  bottom: 10px;
  background: rgba(0, 0, 0, 0.58);
  color: #fff;
  border-radius: 8px;
  font-size: 13px;
  padding: 4px 10px;
}

.sku-strip {
  margin: 8px 12px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.sku-item {
  min-width: 140px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-radius: 10px;
  padding: 6px;
  background: #f2f2f2;
  border: 1px solid transparent;
}

.sku-item.active {
  border-color: #ff3b30;
  background: #fff;
}

.sku-item img {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  object-fit: cover;
}

.sku-item span {
  font-size: 13px;
  color: #333;
  white-space: nowrap;
}

.price-panel {
  margin: 10px 12px;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
}

.price-panel.seckill {
  background: linear-gradient(180deg, #ff2f5f 0%, #ff4d4f 100%);
  color: #fff;
}

.price-main {
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
}

.price-meta {
  margin-top: 4px;
  display: flex;
  gap: 8px;
  align-items: baseline;
}

.price-meta .origin {
  text-decoration: line-through;
  opacity: 0.88;
}

.sold {
  margin-top: 6px;
  font-size: 14px;
}

.seckill-box {
  margin-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.35);
  padding-top: 8px;
  display: flex;
  justify-content: space-between;
}

.card {
  margin: 10px 12px;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
}

.quick-benefits .row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f3f3f3;
}

.quick-benefits .row:last-child {
  border-bottom: none;
}

.quick-benefits strong {
  color: #d9480f;
}

.info-card h2 {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.tags {
  margin-top: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tags span {
  font-size: 12px;
  background: #f2f4f7;
  color: #666;
  border-radius: 8px;
  padding: 3px 8px;
}

.merchant {
  margin: 10px 0 6px;
  color: #666;
  font-size: 13px;
}

.merchant-location {
  margin: 0 0 8px;
  color: #8b8b8b;
  font-size: 12px;
}

.desc {
  margin: 0;
  color: #555;
  font-size: 14px;
  line-height: 1.6;
}

.rule-card h3 {
  margin: 0 0 6px;
  font-size: 15px;
}

.rule-card ul {
  margin: 0;
  padding-left: 18px;
  color: #555;
  line-height: 1.7;
  font-size: 14px;
}

.state {
  margin: 14px 12px;
  border-radius: 10px;
  padding: 12px;
  text-align: center;
  background: #fff;
  color: #666;
}

.state.error {
  color: #d9480f;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 78px;
  background: #fff;
  border-top: 1px solid #eee;
  display: grid;
  grid-template-columns: auto auto 1fr;
  gap: 8px;
  align-items: center;
  padding: 0 10px calc(env(safe-area-inset-bottom) + 6px);
}

.bar-icons {
  display: flex;
  gap: 4px;
}

.bar-icons button {
  width: 54px;
  height: 44px;
  border: none;
  border-radius: 10px;
  background: #f5f6f8;
  color: #444;
  font-size: 12px;
}

.add-cart-btn {
  height: 44px;
  border: 1px solid #ff9f7a;
  border-radius: 10px;
  background: #fff7f1;
  color: #ff6b35;
  font-size: 13px;
  font-weight: 700;
  padding: 0 12px;
}

.buy-btn {
  height: 50px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff2f5f, #ff3b30);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.buy-btn .price {
  font-size: 20px;
  font-weight: 800;
  line-height: 1;
}

.buy-btn .sub {
  font-size: 12px;
  margin-top: 3px;
}

.buy-btn:disabled {
  background: #c9c9c9;
}
</style>
