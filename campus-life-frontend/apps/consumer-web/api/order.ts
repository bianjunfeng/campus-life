import http from './http'

export interface OrderItem {
  id: number
  orderNo: string
  voucherId: number
  status: number
  statusText: string
  statusClass: string
  payAmount: number
  paymentMethod?: string
  paymentStatus?: number
  createTime: string
  payDeadline?: string
  remainingSeconds?: number
  cancelReason?: string
  payTime?: string
  useTime?: string
  voucherTitle?: string
  voucherSubTitle?: string
  voucherImage?: string
  shopName?: string
}

export interface OrderListResponse {
  list: OrderItem[]
  page: number
  size: number
  total: number
}

export async function getMyOrders(params?: {
  status?: number
  page?: number
  size?: number
}): Promise<OrderListResponse> {
  const response = await http.get('/vouchers/my-orders', { params })
  return response.data?.data || { list: [], page: 1, size: 10, total: 0 }
}

export async function getMyOrderDetailById(id: number): Promise<OrderItem> {
  const response = await http.get(`/vouchers/my-orders/${id}`)
  return response.data?.data
}

export async function getMyOrderDetailByOrderNo(orderNo: string): Promise<OrderItem> {
  const response = await http.get(`/vouchers/my-orders/order-no/${orderNo}`)
  return response.data?.data
}

export async function cancelMyOrder(id: number): Promise<void> {
  const response = await http.post(`/vouchers/my-orders/${id}/cancel`)
  const data = response.data
  if (data.code !== 200) {
    throw new Error(data.message || '取消订单失败')
  }
}
