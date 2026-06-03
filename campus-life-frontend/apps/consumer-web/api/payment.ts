import http from './http'

// 支付请求类型
export interface PaymentRequest {
  orderNo: string
  paymentMethod: 'alipay' | 'wechat' | 'wallet'
  amount?: number
  subject?: string
  description?: string
  passbackParams?: string
}

// 支付响应类型
export interface PaymentResponse {
  paymentMethod: string
  paymentOrderNo: string
  payUrl?: string
  qrCode?: string
  payForm?: string
}

// 支付状态查询响应
export interface PaymentStatusResponse {
  orderNo: string
  paymentStatus: string
  orderStatus: number
}

export interface PaymentOrderInfo {
  orderNo: string
  amount: number
  subject: string
  description: string
  paymentStatus?: number
  orderStatus?: number
}

export interface PaymentRefundRequest {
  orderNo: string
  refundAmount: number
  reason?: string
}

export interface PaymentRefundResponse {
  refundNo: string
  paymentNo: string
  refundAmount: number
  requestedAmount?: number
  approvedAmount?: number
  status: string
  currentReviewerRole?: string
}

export interface RefundReviewLogItem {
  id: number
  refundNo: string
  operatorRole: string
  operatorId?: number
  action: string
  fromStatus?: string
  toStatus?: string
  comment?: string
  createTime?: string
}

export interface RefundRecordItem {
  refundNo: string
  paymentNo: string
  bizOrderNo: string
  shopName?: string
  voucherTitle?: string
  voucherSubTitle?: string
  voucherImage?: string
  paymentMethod?: string
  status: string
  requestedAmount?: number
  approvedAmount?: number
  refundAmount?: number
  reason?: string
  createTime?: string
  reviewDeadline?: string
  successTime?: string
  currentReviewerRole?: string
  merchantReviewReason?: string
  merchantReviewTime?: string
  adminReviewReason?: string
  adminReviewTime?: string
  logs?: RefundReviewLogItem[]
}

// 创建支付订单
export const createPayment = async (request: PaymentRequest): Promise<PaymentResponse> => {
  const response = await http.post('/payment/create', request)
  const data = response.data
  if (data.code === 200) {
    return data.data
  }
  throw new Error(data.message || '创建支付订单失败')
}

// 查询支付状态
export const queryPaymentStatus = async (orderNo: string): Promise<PaymentStatusResponse> => {
  const response = await http.get(`/payment/status/${orderNo}`)
  const data = response.data
  if (data.code === 200) {
    return data.data
  }
  throw new Error(data.message || '查询支付状态失败')
}

export const getPaymentOrderInfo = async (orderNo: string): Promise<PaymentOrderInfo> => {
  const response = await http.get(`/payment/order/${orderNo}`)
  const data = response.data
  if (data.code === 200) {
    return data.data
  }
  throw new Error(data.message || '获取支付订单信息失败')
}

export const refundPayment = async (request: PaymentRefundRequest): Promise<PaymentRefundResponse> => {
  const response = await http.post('/payment/refund', request)
  const data = response.data
  if (data.code === 200) {
    return data.data
  }
  throw new Error(data.message || '申请退款失败')
}

export const getMyRefunds = async (): Promise<RefundRecordItem[]> => {
  const response = await http.get('/payment/refunds')
  const data = response.data
  if (data.code === 200) {
    return data.data || []
  }
  throw new Error(data.message || '获取退款记录失败')
}

export const getMyRefundDetail = async (refundNo: string): Promise<RefundRecordItem> => {
  const response = await http.get(`/payment/refunds/${refundNo}`)
  const data = response.data
  if (data.code === 200) {
    return data.data
  }
  throw new Error(data.message || '获取退款详情失败')
}
