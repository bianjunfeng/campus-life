import http from './http'

export interface CartItem {
  id: number
  voucherId: number
  quantity: number
  selected: number
  title?: string
  description?: string
  image?: string
  price?: number
  originalPrice?: number
  stock?: number
  salesCount?: number
  shopName?: string
  isSeckill?: number | boolean
  countdownSeconds?: number
  subtotal?: number
}

export interface CartCheckoutResult {
  successOrders: Array<{
    cartItemId: number
    voucherId: number
    orderNo: string
    payAmount: number
  }>
  failedItems: Array<{
    cartItemId: number
    voucherId: number
    reason: string
  }>
  successCount: number
  failedCount: number
}

export async function getCartItems(): Promise<CartItem[]> {
  const response = await http.get('/cart/items')
  return response.data?.data || []
}

export async function addCartItem(voucherId: number, quantity = 1): Promise<void> {
  const response = await http.post('/cart/items', { voucherId, quantity })
  const data = response.data
  if (data.code !== 200) {
    throw new Error(data.message || '加入购物车失败')
  }
}

export async function updateCartItem(id: number, payload: { quantity?: number; selected?: number }): Promise<void> {
  const response = await http.put(`/cart/items/${id}`, payload)
  const data = response.data
  if (data.code !== 200) {
    throw new Error(data.message || '更新购物车失败')
  }
}

export async function updateCartItemSelected(id: number, selected: number): Promise<void> {
  const response = await http.patch(`/cart/items/${id}/selected`, { selected })
  const data = response.data
  if (data.code !== 200) {
    throw new Error(data.message || '更新购物车失败')
  }
}

export async function removeCartItem(id: number): Promise<void> {
  const response = await http.delete(`/cart/items/${id}`)
  const data = response.data
  if (data.code !== 200) {
    throw new Error(data.message || '删除购物车失败')
  }
}

export async function clearCartItems(): Promise<void> {
  const response = await http.delete('/cart/items')
  const data = response.data
  if (data.code !== 200) {
    throw new Error(data.message || '清空购物车失败')
  }
}

export async function checkoutCart(itemIds: number[]): Promise<CartCheckoutResult> {
  const response = await http.post('/cart/checkout', { itemIds })
  const data = response.data
  if (data.code !== 200) {
    throw new Error(data.message || '结算失败')
  }
  return data.data || { successOrders: [], failedItems: [], successCount: 0, failedCount: 0 }
}
