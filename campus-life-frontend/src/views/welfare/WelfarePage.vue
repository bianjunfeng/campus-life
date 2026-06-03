<template>
  <div class="welfare-page">
    <header class="page-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <h1>福利中心</h1>
      <div class="header-placeholder"></div>
    </header>

    <section class="search-section">
      <div class="search-wrap">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8" />
          <path d="M21 21l-4.35-4.35" />
        </svg>
        <input v-model.trim="keyword" type="text" placeholder="搜索福利券/商家" />
      </div>
      <button class="search-btn" @click="reloadData">搜索</button>
    </section>

    <section v-if="seckillCoupons.length > 0" class="seckill-carousel-section">
      <div class="section-title-row">
        <h2>秒杀专区</h2>
        <span class="hint">限时抢购</span>
      </div>
      <div class="seckill-carousel">
        <article v-for="item in seckillCoupons" :key="item.id" class="seckill-item" @click="viewDetail(item)">
          <img class="seckill-cover" :src="item.image || fallbackImage" :alt="item.title" />
          <div class="seckill-info">
            <h3>{{ item.title }}</h3>
            <p>剩余 {{ item.stock }} 张</p>
            <p>倒计时 {{ countdownText(item) }}</p>
            <div class="seckill-bottom">
              <strong>¥{{ money(item.price) }}</strong>
              <button class="grab-mini-btn" :disabled="item.stock <= 0 || isSeckillGrabbed(item.id)" @click.stop="viewDetail(item)">
                {{ isSeckillGrabbed(item.id) ? '已抢购' : (item.stock <= 0 ? '已抢光' : '抢购') }}
              </button>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section class="category-row">
      <button
        v-for="item in categoryTabs"
        :key="String(item.id)"
        class="category-item"
        :class="{ active: selectedCategory === String(item.id) }"
        @click="selectedCategory = String(item.id)"
      >
        {{ item.name }}
      </button>
    </section>

    <section class="quick-filters">
      <button class="chip" :class="{ active: sortMode === 'smart' }" @click="sortMode = 'smart'">智能排序</button>
      <button class="chip" :class="{ active: sortMode === 'latest' }" @click="sortMode = 'latest'">最新上架</button>
      <button class="chip" :class="{ active: sortMode === 'sales' }" @click="sortMode = 'sales'">销量优先</button>
    </section>

    <section class="nearby-section">
      <div class="section-title-row">
        <h2>附近商家</h2>
        <span class="hint">{{ nearbyStatusText }}</span>
      </div>
      <div v-if="nearbyMerchants.length > 0" class="nearby-list">
        <article v-for="shop in nearbyMerchants" :key="shop.id" class="nearby-card">
          <p class="nearby-name">{{ shop.name }}</p>
          <p class="nearby-addr">{{ shop.address || '校内商圈' }}</p>
          <p class="nearby-dist">{{ distanceText(shop.distance) }}</p>
        </article>
      </div>
      <div v-else class="nearby-empty">暂无可展示的附近商家</div>
    </section>

    <div v-if="loading" class="state">福利数据加载中...</div>
    <div v-else-if="error" class="state error">{{ error }}</div>

    <section v-else class="coupon-list">
      <article
        v-for="coupon in displayCoupons"
        :key="coupon.id"
        class="coupon-card"
        @click="viewDetail(coupon)"
      >
        <img class="cover" :src="coupon.image || fallbackImage" :alt="coupon.title" />

        <div class="info">
          <h3 class="title">{{ coupon.title }}</h3>
          <p class="sub">{{ coupon.shopName }} · {{ coupon.shopAddressFull || coupon.shopAddress || coupon.shopLocation || '校园商圈' }}</p>

          <div class="meta-row">
            <span>销量 {{ coupon.salesCount || 0 }}</span>
            <span>{{ (coupon.rating || 4.6).toFixed(1) }}分</span>
            <span>{{ distanceText(calcDistanceMeters(coupon)) }}</span>
          </div>

          <div class="price-row">
            <span class="price">¥{{ money(coupon.price) }}</span>
            <span class="discount" v-if="discountText(coupon)">{{ discountText(coupon) }}折</span>
            <span class="origin">¥{{ money(coupon.originalPrice) }}</span>
          </div>

          <div class="tags-row">
            <span class="tag normal">{{ coupon.validPeriod || '可随时使用' }}</span>
          </div>
        </div>

        <div class="action">
          <button
            class="grab-btn"
            :disabled="coupon.stock <= 0 || isSeckillGrabbed(coupon.id)"
            @click.stop="viewDetail(coupon)"
          >
            {{ isSeckillGrabbed(coupon.id) ? '已抢购' : (coupon.stock <= 0 ? '已抢光' : '抢购') }}
          </button>
        </div>
      </article>

      <div v-if="displayCoupons.length === 0" class="state">当前条件下暂无普通券</div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createVoucherOrder, getWelfareHome, grabCoupon as apiGrabCoupon, queryGrabResult, type Countdown, type CouponItem } from '../../api/welfareApi'
import { getMerchantCategories, type MerchantCategory } from '../../api/categoryApi'
import { getNearbyMerchants, type NearbyMerchant } from '../../api/merchant'
import { getMyOrders } from '../../api/order'
import { notify } from '@/utils/notify'

type WelfareCoupon = CouponItem & {
  isSeckill: boolean
  countdownSeconds: number
}

const router = useRouter()
const fallbackImage = 'https://picsum.photos/200/140?blur=1'

const loading = ref(false)
const error = ref('')
const keyword = ref('')
const sortMode = ref<'smart' | 'latest' | 'sales'>('smart')
const selectedCategory = ref('all')
const categoryTabs = ref<Array<{ id: string | number; name: string; code?: string }>>([
  { id: 'all', name: '推荐', code: 'recommend' }
])

const coupons = ref<WelfareCoupon[]>([])
const userPos = ref<{ lng: number; lat: number } | null>(null)
const nearbyMerchants = ref<NearbyMerchant[]>([])
const nearbyStatusText = ref('定位中')
const submittingIds = ref<Set<number>>(new Set())
const grabbedSeckillVoucherIds = ref<Set<number>>(new Set())
let timer: number | null = null

const toSeconds = (c?: Countdown) => {
  if (!c) return 0
  return Math.max((c.hours || 0) * 3600 + (c.minutes || 0) * 60 + (c.seconds || 0), 0)
}

const money = (v: unknown) => Number(v || 0).toFixed(1)

const discountText = (coupon: WelfareCoupon) => {
  if (coupon.discount && coupon.discount > 0) return Number(coupon.discount).toFixed(1)
  const p = Number(coupon.price || 0)
  const o = Number(coupon.originalPrice || 0)
  if (o <= 0 || p <= 0) return ''
  return ((p / o) * 10).toFixed(1)
}

const countdownText = (coupon: WelfareCoupon) => {
  const sec = Math.max(coupon.countdownSeconds || 0, 0)
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  const s = sec % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const distanceText = (d?: number) => {
  const n = Number(d || 0)
  if (!n) return '附近'
  return n >= 1000 ? `${(n / 1000).toFixed(1)}km` : `${Math.round(n)}m`
}

const calcDistanceMeters = (item: WelfareCoupon) => {
  if (item.distance && Number(item.distance) > 0) return Number(item.distance)
  if (!userPos.value) return 0
  const lng2 = Number(item.shopLongitude || 0)
  const lat2 = Number(item.shopLatitude || 0)
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

const normalize = (item: CouponItem, seckill: boolean): WelfareCoupon => ({
  ...item,
  isSeckill: seckill,
  countdownSeconds: seckill ? toSeconds(item.countdown) : 0
})

const seckillCoupons = computed(() => {
  const rank = (item: WelfareCoupon) => {
    if (isSeckillGrabbed(item.id)) return 2
    if (item.stock <= 0) return 3
    if (item.countdownSeconds <= 0) return 4
    return 1
  }
  return [...coupons.value.filter((item) => item.isSeckill)].sort((a, b) => {
    const ra = rank(a)
    const rb = rank(b)
    if (ra !== rb) return ra - rb
    if (ra === 1) {
      if (a.countdownSeconds !== b.countdownSeconds) return a.countdownSeconds - b.countdownSeconds
      if (Number(a.stock || 0) !== Number(b.stock || 0)) return Number(a.stock || 0) - Number(b.stock || 0)
      return Number(b.salesCount || 0) - Number(a.salesCount || 0)
    }
    return Number(b.id || 0) - Number(a.id || 0)
  })
})
const normalCoupons = computed(() => coupons.value.filter((item) => !item.isSeckill))
const isSeckillGrabbed = (voucherId: number) => grabbedSeckillVoucherIds.value.has(voucherId)

const displayCoupons = computed(() => {
  let list = normalCoupons.value.filter((item) => {
    const inCategory = selectedCategory.value === 'all' || String(item.categoryId) === selectedCategory.value
    if (!inCategory) return false
    const kw = keyword.value.toLowerCase()
    if (!kw) return true
    return [item.title, item.shopName, item.description].some((v) => String(v || '').toLowerCase().includes(kw))
  })

  if (sortMode.value === 'sales') {
    list = [...list].sort((a, b) => Number(b.salesCount || 0) - Number(a.salesCount || 0))
  } else if (sortMode.value === 'latest') {
    list = [...list].sort((a, b) => Number(b.id) - Number(a.id))
  } else {
    list = [...list].sort((a, b) => {
      const sa = Number(a.salesCount || 0) + Number(a.rating || 0) * 10
      const sb = Number(b.salesCount || 0) + Number(b.rating || 0) * 10
      return sb - sa
    })
  }

  return [...list].sort((a, b) => {
    const sa = Number(a.stock || 0) > 0 ? 0 : 1
    const sb = Number(b.stock || 0) > 0 ? 0 : 1
    if (sa !== sb) return sa - sb
    return 0
  })
})

const startTimer = () => {
  if (timer) window.clearInterval(timer)
  timer = window.setInterval(() => {
    coupons.value = coupons.value.map((item) => {
      if (!item.isSeckill) return item
      return { ...item, countdownSeconds: Math.max(item.countdownSeconds - 1, 0) }
    })
  }, 1000)
}

const loadCategories = async () => {
  try {
    const list = await getMerchantCategories()
    const fromApi = (list || []).map((c: MerchantCategory) => ({ id: c.id, name: c.name, code: c.code }))
    categoryTabs.value = [{ id: 'all', name: '推荐', code: 'recommend' }, ...fromApi]
  } catch {
    categoryTabs.value = [{ id: 'all', name: '推荐', code: 'recommend' }]
  }
}

const getCurrentPosition = () =>
  new Promise<{ lng: number; lat: number }>((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('当前设备不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          lng: Number(position.coords.longitude),
          lat: Number(position.coords.latitude)
        })
      },
      (error) => reject(error),
      {
        enableHighAccuracy: false,
        timeout: 4500,
        maximumAge: 60 * 1000
      }
    )
  })

const loadNearbyShops = async () => {
  nearbyStatusText.value = '定位中'
  try {
    let coord: { lng: number; lat: number } | null = null
    try {
      coord = await getCurrentPosition()
      userPos.value = coord
      localStorage.setItem('welfare:lastPos', JSON.stringify(coord))
    } catch {
      const cached = localStorage.getItem('welfare:lastPos')
      if (cached) {
        const parsed = JSON.parse(cached)
        coord = {
          lng: Number(parsed?.lng),
          lat: Number(parsed?.lat)
        }
        userPos.value = coord
      }
    }
    if (!coord || Number.isNaN(coord.lng) || Number.isNaN(coord.lat)) {
      nearbyMerchants.value = []
      nearbyStatusText.value = '未获取定位'
      return
    }
    const data = await getNearbyMerchants({
      lng: coord.lng,
      lat: coord.lat,
      radiusMeters: 3000,
      size: 8,
      page: 1
    })
    nearbyMerchants.value = data.list || []
    nearbyStatusText.value = nearbyMerchants.value.length > 0 ? '3km内' : '附近无商家'
  } catch {
    nearbyMerchants.value = []
    nearbyStatusText.value = '定位失败'
  }
}

const reloadData = async () => {
  loading.value = true
  error.value = ''
  try {
    const home = await getWelfareHome()
    const flashMap = new Map((home.flashSales || []).map((i) => [i.id, normalize(i, true)]))
    const normal = (home.coupons || [])
      .filter((i) => !flashMap.has(i.id))
      .map((i) => normalize(i, false))
    coupons.value = [...flashMap.values(), ...normal]
    startTimer()
  } catch (e: any) {
    coupons.value = []
    error.value = e?.message || '福利数据加载失败'
  } finally {
    loading.value = false
  }
}

const loadGrabbedSeckillState = async () => {
  try {
    const orderData = await getMyOrders({ page: 1, size: 200 })
    const ids = new Set<number>()
    ;(orderData.list || []).forEach((order: any) => {
      const source = String(order.orderSource || '')
      const status = Number(order.status ?? -1)
      const voucherId = Number(order.voucherId || 0)
      if (source === 'seckill' && voucherId > 0 && status !== 5) {
        ids.add(voucherId)
      }
    })
    grabbedSeckillVoucherIds.value = ids
  } catch {
    grabbedSeckillVoucherIds.value = new Set()
  }
}

const couponCacheKey = (couponId: number | string) => `currentCoupon:${couponId}`

const viewDetail = (coupon: WelfareCoupon) => {
  sessionStorage.setItem('currentCoupon', JSON.stringify(coupon))
  sessionStorage.setItem('currentCouponId', String(coupon.id))
  sessionStorage.setItem(couponCacheKey(coupon.id), JSON.stringify(coupon))
  router.push(`/group-buy/${coupon.id}`)
}

const withSubmitting = async (id: number, fn: () => Promise<void>) => {
  const next = new Set(submittingIds.value)
  next.add(id)
  submittingIds.value = next
  try {
    await fn()
  } finally {
    const rollback = new Set(submittingIds.value)
    rollback.delete(id)
    submittingIds.value = rollback
  }
}

const handleGrab = async (coupon: WelfareCoupon) => {
  if (coupon.stock <= 0 || submittingIds.value.has(coupon.id)) return

  await withSubmitting(coupon.id, async () => {
    if (!coupon.isSeckill) {
      const order = await createVoucherOrder(coupon.id)
      router.push({
        path: '/payment',
        query: {
          orderNo: order.orderNo,
          amount: String(order.payAmount),
          returnTo: 'welfare'
        }
      })
      return
    }

    const submit = await apiGrabCoupon(coupon.id)
    if (!submit.success || !submit.orderNo) {
      throw new Error(submit.message || '抢购失败')
    }

    for (let i = 0; i < 15; i++) {
      await new Promise((resolve) => setTimeout(resolve, 300))
      const result = await queryGrabResult(submit.orderNo)
      if (result.status === 'SUCCESS') {
        coupons.value = coupons.value.map((item) => item.id === coupon.id ? { ...item, stock: Math.max(item.stock - 1, 0) } : item)
        const next = new Set(grabbedSeckillVoucherIds.value)
        next.add(coupon.id)
        grabbedSeckillVoucherIds.value = next
        notify('抢购成功，正在前往支付页')
        router.push({
          path: '/payment',
          query: {
            orderNo: submit.orderNo,
            returnTo: 'welfare'
          }
        })
        return
      }
      if (result.status === 'FAIL' || result.status === 'NOT_FOUND') {
        throw new Error(result.message || '抢购失败')
      }
    }

    throw new Error('排队中，请稍后在订单页查看结果')
  }).catch((e: any) => {
    error.value = e?.message || '抢购失败'
  })
}

const goBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  router.replace('/home')
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadGrabbedSeckillState(), reloadData(), loadNearbyShops()])
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.welfare-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 16px;
}

.page-header {
  height: 52px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
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
  color: #222;
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.search-section {
  display: grid;
  grid-template-columns: 1fr 72px;
  gap: 8px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.search-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 10px;
  border: 1px solid #e7e7e7;
  border-radius: 19px;
  background: #f8f8f8;
}

.search-wrap input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  background: transparent;
}

.search-icon {
  width: 16px;
  height: 16px;
  color: #8b8b8b;
}

.search-btn {
  height: 38px;
  border: none;
  border-radius: 19px;
  background: #1677ff;
  color: #fff;
  font-weight: 600;
}

.seckill-carousel-section {
  margin: 10px 12px;
  border-radius: 12px;
  background: #fff;
  padding: 10px;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.section-title-row h2 {
  margin: 0;
  font-size: 15px;
}

.hint {
  font-size: 12px;
  color: #d9480f;
}

.seckill-carousel {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding-bottom: 2px;
}

.seckill-item {
  min-width: 220px;
  max-width: 220px;
  border-radius: 10px;
  border: 1px solid #f2e6dd;
  background: #fffaf7;
  overflow: hidden;
  scroll-snap-align: start;
}

.seckill-cover {
  width: 100%;
  height: 110px;
  object-fit: cover;
  background: #f2f2f2;
}

.seckill-info {
  padding: 8px;
}

.seckill-info h3 {
  margin: 0;
  font-size: 14px;
  line-height: 1.3;
  height: 36px;
  overflow: hidden;
}

.seckill-info p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #666;
}

.seckill-bottom {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.seckill-bottom strong {
  color: #e03131;
  font-size: 20px;
}

.grab-mini-btn {
  height: 30px;
  min-width: 58px;
  border: none;
  border-radius: 15px;
  background: linear-gradient(135deg, #ff8f1f, #ff5f05);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.grab-mini-btn:disabled {
  background: #c9c9c9;
}

.category-row,
.quick-filters {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 8px 12px;
  background: #fff;
}

.quick-filters {
  border-top: 1px solid #f2f2f2;
  border-bottom: 1px solid #f2f2f2;
}

.nearby-section {
  margin: 10px 12px 0;
  border-radius: 12px;
  background: #fff;
  padding: 10px;
}

.nearby-list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.nearby-card {
  min-width: 140px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 8px;
  background: #fafafa;
}

.nearby-name {
  margin: 0;
  font-size: 13px;
  color: #1f1f1f;
  font-weight: 700;
}

.nearby-addr {
  margin: 4px 0 0;
  font-size: 12px;
  color: #7d7d7d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nearby-dist {
  margin: 6px 0 0;
  font-size: 12px;
  color: #1677ff;
}

.nearby-empty {
  font-size: 12px;
  color: #9a9a9a;
}

.category-item,
.chip {
  height: 32px;
  padding: 0 12px;
  border: none;
  border-radius: 16px;
  background: #f3f3f3;
  color: #666;
  white-space: nowrap;
}

.category-item.active,
.chip.active {
  background: #e8f2ff;
  color: #1677ff;
  font-weight: 600;
}

.coupon-list {
  padding: 10px 10px 0;
}

.coupon-card {
  display: grid;
  grid-template-columns: 118px 1fr 72px;
  gap: 10px;
  margin-bottom: 10px;
  padding: 10px;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.cover {
  width: 118px;
  height: 98px;
  border-radius: 10px;
  object-fit: cover;
  background: #f1f1f1;
}

.info {
  min-width: 0;
}

.title {
  margin: 0;
  font-size: 17px;
  line-height: 1.25;
  color: #1f1f1f;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.sub {
  margin: 4px 0 0;
  font-size: 12px;
  color: #888;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-row {
  margin-top: 6px;
  display: flex;
  gap: 8px;
  color: #7f7f7f;
  font-size: 12px;
}

.price-row {
  margin-top: 6px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.price {
  color: #e03131;
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
}

.discount {
  color: #d9480f;
  font-size: 13px;
  font-weight: 700;
}

.origin {
  color: #9e9e9e;
  font-size: 14px;
  text-decoration: line-through;
}

.tags-row {
  margin-top: 4px;
  display: flex;
  gap: 6px;
}

.tag.normal {
  color: #666;
  background: #f1f3f5;
  border-radius: 10px;
  font-size: 11px;
  padding: 2px 7px;
}

.action {
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.grab-btn {
  width: 64px;
  height: 42px;
  border: none;
  border-radius: 21px;
  background: linear-gradient(135deg, #ff8f1f, #ff5f05);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
}

.grab-btn:disabled {
  background: #c9c9c9;
}

.state {
  margin: 16px 12px;
  padding: 14px;
  border-radius: 12px;
  text-align: center;
  color: #666;
  background: #fff;
}

.state.error {
  color: #d9480f;
}
</style>
