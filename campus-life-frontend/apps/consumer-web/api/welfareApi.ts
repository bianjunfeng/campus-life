import http from './http'

export interface Countdown {
  hours: number
  minutes: number
  seconds: number
}

export interface CouponItem {
  id: number
  title: string
  description: string
  price: number
  originalPrice: number
  categoryId: number
  validPeriod?: string
  stock: number
  grabbed?: boolean
  image?: string
  totalStock?: number
  countdown?: Countdown
  shopName?: string
  shopAddress?: string
  shopAddressFull?: string
  shopLocation?: string
  shopProvince?: string
  shopCity?: string
  shopDistrict?: string
  shopLongitude?: number
  shopLatitude?: number
  salesCount?: number
  rating?: number
  discount?: number
  businessHours?: string
  couponInfo?: string
  distance?: number
  isSeckill?: boolean
}

export interface WelfareHomeData {
  coupons: CouponItem[]
  flashSales: CouponItem[]
}

export async function getWelfareHome(): Promise<WelfareHomeData> {
  const response = await http.get('/welfare/home')
  return response.data.data || { coupons: [], flashSales: [] }
}

export async function getCoupons(params?: any): Promise<CouponItem[]> {
  const response = await http.get('/coupons', { params })
  return response.data.data || []
}

export async function getFlashSales(params?: any): Promise<CouponItem[]> {
  const response = await http.get('/flash-sales', { params })
  return response.data.data || []
}

export async function grabCoupon(couponId: number): Promise<{ success: boolean; message: string; orderNo?: string }> {
  const response = await http.post(`/coupons/${couponId}/grab`)
  return response.data.data || { success: false, message: '抢购失败' }
}

export async function queryGrabResult(orderNo: string): Promise<{ status: string; message: string }> {
  const response = await http.get('/coupons/grab/result', { params: { orderNo } })
  return response.data.data || { status: 'NOT_FOUND', message: '订单不存在' }
}

export async function getCouponDetail(couponId: number): Promise<CouponItem> {
  const response = await http.get(`/coupons/${couponId}`)
  return response.data.data
}

export async function createVoucherOrder(voucherId: number): Promise<{ orderNo: string; payAmount: number }> {
  const response = await http.post(`/vouchers/${voucherId}/claims`)
  const data = response.data?.data
  if (!data) {
    throw new Error(response.data?.message || '创建订单失败')
  }
  return {
    orderNo: data.orderNo,
    payAmount: Number(data.payAmount || 0)
  }
}
