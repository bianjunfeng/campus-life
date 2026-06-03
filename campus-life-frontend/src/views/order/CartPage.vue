<template>
  <div class="cart-page">
    <header class="page-header">
      <button class="back-btn" @click="goBack" aria-label="返回">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <h1>购物车</h1>
      <button class="clear-btn" @click="clearAll" :disabled="loading || items.length === 0">清空</button>
    </header>

    <section v-if="loading" class="state">购物车加载中...</section>
    <section v-else-if="error" class="state error">{{ error }}</section>
    <section v-else-if="items.length === 0" class="state">购物车空空如也，去福利页看看吧</section>

    <section v-else class="cart-list">
      <article v-for="item in items" :key="item.id" class="cart-item">
        <label class="selector">
          <input type="checkbox" :checked="isSelected(item)" @change="toggleSelected(item)" />
        </label>
        <img class="cover" :src="item.image || fallbackImage" :alt="item.title || '商品'" />
        <div class="content">
          <h3>{{ item.title }}</h3>
          <p class="shop">{{ item.shopName || '校园商家' }}</p>
          <p v-if="item.isSeckill" class="seckill">秒杀券 · 剩余 {{ countdownText(item.countdownSeconds) }}</p>
          <p class="price">
            <strong>¥{{ money(item.price) }}</strong>
            <span v-if="Number(item.originalPrice || 0) > 0">¥{{ money(item.originalPrice) }}</span>
          </p>
          <div class="actions">
            <div class="quantity-tag">每个账号限购 1 张</div>
            <button class="remove" @click="remove(item.id)" :disabled="updating">删除</button>
          </div>
        </div>
      </article>
    </section>

    <footer v-if="items.length > 0" class="bottom-bar">
      <div class="summary">
        <p>合计</p>
        <strong>¥{{ totalAmount.toFixed(2) }}</strong>
      </div>
      <button class="checkout-btn" :disabled="updating || selectedIds.length === 0" @click="checkout">
        去结算({{ selectedIds.length }})
      </button>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { checkoutCart, clearCartItems, getCartItems, removeCartItem, updateCartItem } from '@/api/cart'
import { notify } from '@/utils/notify'

type CartViewItem = {
  id: number
  voucherId: number
  quantity: number
  selected: number
  title?: string
  shopName?: string
  image?: string
  price?: number
  originalPrice?: number
  isSeckill?: number | boolean
  countdownSeconds?: number
}

const router = useRouter()
const loading = ref(false)
const updating = ref(false)
const items = ref<CartViewItem[]>([])
const error = ref('')
const fallbackImage = 'https://picsum.photos/300/200?grayscale=1'

const selectedIds = computed(() =>
  items.value.filter((item) => Number(item.selected || 0) === 1).map((item) => item.id)
)

const totalAmount = computed(() =>
  items.value.reduce((sum, item) => {
    if (Number(item.selected || 0) !== 1) return sum
    const price = Number(item.price || 0)
    return sum + price
  }, 0)
)

const money = (v: unknown) => Number(v || 0).toFixed(2)

const countdownText = (seconds?: number) => {
  const sec = Math.max(Number(seconds || 0), 0)
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  const s = sec % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const isSelected = (item: CartViewItem) => Number(item.selected || 0) === 1

const goBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  router.replace('/welfare')
}

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const list = await getCartItems()
    items.value = (list || []).map((item) => ({
      ...item,
      quantity: 1
    }))
  } catch (e: any) {
    items.value = []
    error.value = e?.message || '购物车加载失败'
  } finally {
    loading.value = false
  }
}

const toggleSelected = async (item: CartViewItem) => {
  if (updating.value) return
  updating.value = true
  try {
    await updateCartItem(item.id, { quantity: item.quantity || 1, selected: isSelected(item) ? 0 : 1 })
    await load()
  } catch (e: any) {
    notify(e?.message || '更新失败')
  } finally {
    updating.value = false
  }
}

const remove = async (id: number) => {
  if (updating.value) return
  updating.value = true
  try {
    await removeCartItem(id)
    await load()
  } catch (e: any) {
    notify(e?.message || '删除失败')
  } finally {
    updating.value = false
  }
}

const clearAll = async () => {
  if (updating.value || items.value.length === 0) return
  updating.value = true
  try {
    await clearCartItems()
    await load()
    notify('购物车已清空')
  } catch (e: any) {
    notify(e?.message || '清空失败')
  } finally {
    updating.value = false
  }
}

const checkout = async () => {
  if (updating.value || selectedIds.value.length === 0) return
  updating.value = true
  try {
    const result = await checkoutCart(selectedIds.value)
    if ((result.successOrders || []).length > 0) {
      const first = result.successOrders[0]
      if ((result.successOrders || []).length > 1) {
        notify(`已创建${result.successOrders.length}个订单，先支付第一个`)
      } else {
        notify('订单创建成功，前往支付')
      }
      router.push({
        path: '/payment',
        query: {
          orderNo: first.orderNo,
          returnTo: 'cart'
        }
      })
      return
    }
    const firstFail = result.failedItems?.[0]
    notify(firstFail?.reason || '结算失败')
  } catch (e: any) {
    notify(e?.message || '结算失败')
  } finally {
    updating.value = false
    await load()
  }
}

onMounted(load)
</script>

<style scoped>
.cart-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 90px;
}

.page-header {
  height: 52px;
  display: grid;
  grid-template-columns: 40px 1fr auto;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.page-header h1 {
  margin: 0;
  text-align: center;
  font-size: 17px;
  font-weight: 700;
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

.clear-btn {
  border: none;
  background: transparent;
  color: #666;
  font-size: 14px;
  padding: 6px 0;
}

.state {
  margin: 12px;
  border-radius: 12px;
  background: #fff;
  color: #666;
  text-align: center;
  padding: 30px 12px;
}

.state.error {
  color: #d9480f;
}

.cart-list {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cart-item {
  background: #fff;
  border-radius: 12px;
  padding: 10px;
  display: grid;
  grid-template-columns: 24px 88px 1fr;
  gap: 10px;
  align-items: start;
}

.selector {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88px;
}

.cover {
  width: 88px;
  height: 88px;
  border-radius: 10px;
  object-fit: cover;
  background: #f2f2f2;
}

.content h3 {
  margin: 0;
  font-size: 15px;
  line-height: 1.35;
}

.shop {
  margin: 4px 0;
  font-size: 12px;
  color: #8a8a8a;
}

.seckill {
  margin: 0 0 4px;
  font-size: 12px;
  color: #e03131;
}

.price {
  display: flex;
  gap: 8px;
  align-items: baseline;
  margin: 0;
}

.price strong {
  color: #e03131;
  font-size: 20px;
}

.price span {
  color: #999;
  font-size: 12px;
  text-decoration: line-through;
}

.actions {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.quantity-tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #fff3bf;
  color: #8c6d1f;
  font-size: 12px;
}

.remove {
  border: none;
  background: transparent;
  color: #888;
  font-size: 13px;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 72px;
  background: #fff;
  border-top: 1px solid #eee;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary p {
  margin: 0;
  color: #666;
  font-size: 12px;
}

.summary strong {
  color: #e03131;
  font-size: 24px;
  line-height: 1.1;
}

.checkout-btn {
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff4d4f 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  padding: 0 18px;
  height: 44px;
}

.checkout-btn:disabled {
  background: #c8c8c8;
}
</style>
