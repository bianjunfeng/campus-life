import http from './http'

// ==================== 商家类型 ====================

export interface MerchantType {
  id: number
  code: string
  name: string
  description?: string
}

export interface NearbyMerchant {
  id: number
  userId?: number
  name: string
  address?: string
  province?: string
  city?: string
  district?: string
  typeId?: number
  status?: number
  longitude?: number
  latitude?: number
  distance?: number
}

export interface MerchantHomeAction {
  key: string
  label: string
  path: string
  description?: string
}

export interface MerchantHomeMerchantInfo {
  id: number
  name: string
  status: number
  statusLabel: string
  typeId?: number
  typeName?: string
  contactName?: string
  contactPhone?: string
  address?: string
  businessHours?: string
  province?: string
  city?: string
  district?: string
  locationStatus?: number
  locationStatusLabel?: string
  locationUpdatedTime?: string
  createTime?: string
}

export interface MerchantHomeManagementSection {
  path: string
  title: string
  description?: string
  stats: {
    totalVouchers: number
    onlineVouchers: number
    offlineVouchers: number
    totalSoldCount: number
    pendingRefunds: number
    escalatedRefunds: number
    processingRefunds: number
    successRefunds: number
  }
  quickActions: MerchantHomeAction[]
}

export interface MerchantHomeSnapshot {
  merchant: MerchantHomeMerchantInfo
  managementHome: MerchantHomeManagementSection
  recentVouchers: Array<{
    id: number
    title: string
    status: number
    statusLabel: string
    stock?: number
    soldCount?: number
    updateTime?: string
  }>
  recentRefunds: Array<{
    refundNo: string
    voucherTitle?: string
    status: string
    statusLabel: string
    requestedAmount?: number
    createTime?: string
  }>
  serverTime?: string
}

export interface MerchantAiAgentEntry {
  code: string
  name: string
  description: string
  frontendPath: string
  backendPath: string
  sceneCode: string
}

/**
 * 获取商家类型列表
 */
export async function getMerchantTypes(): Promise<MerchantType[]> {
  const { data } = await http.get('/shops/types')
  return data.data || []
}

export async function getNearbyMerchants(params: {
  lng: number
  lat: number
  radiusMeters?: number
  typeId?: number
  page?: number
  size?: number
}): Promise<{ list: NearbyMerchant[]; page: number; size: number; total: number }> {
  const { data } = await http.get('/shops/nearby', { params })
  return data.data || { list: [], page: 1, size: 10, total: 0 }
}

export async function updateMerchantLocation(
  merchantId: number,
  payload: {
    longitude: number
    latitude: number
    province?: string
    city?: string
    district?: string
    address?: string
    geoHash?: string
  }
): Promise<{ success: boolean }> {
  const { data } = await http.post(`/shops/${merchantId}/location`, payload)
  return data.data || { success: false }
}

export async function getMerchantHome(): Promise<MerchantHomeSnapshot> {
  const { data } = await http.get('/merchant/home')
  return data.data
}

export async function getMerchantAiAgents(): Promise<MerchantAiAgentEntry[]> {
  const { data } = await http.get('/merchant/ai/agents')
  return data.data || []
}

export async function updateMerchantProfile(payload: {
  contactName?: string
  contactPhone?: string
  address?: string
  businessHours?: string
}): Promise<MerchantHomeMerchantInfo> {
  const { data } = await http.put('/merchant/profile', payload)
  if (data.code === 200 && data.data) {
    return data.data as MerchantHomeMerchantInfo
  }
  throw new Error(data.message || '更新商家资料失败')
}

// ==================== 商家优惠券管理 ====================

export interface MerchantVoucher {
  id: number
  merchantId: number
  title: string
  subTitle?: string
  voucherType?: 'group' | 'seckill'
  stock: number
  seckillStock?: number
  amount: number
  payValue?: number
  status: number  // 0-下架;1-上架
  beginTime?: string
  endTime?: string
  seckillStartTime?: string
  seckillEndTime?: string
  createTime: string
  updateTime?: string
}

export interface MerchantVoucherListResponse {
  list: MerchantVoucher[]
  page: number
  size: number
  total: number
}

export interface MerchantRefundRecord {
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
  reviewDeadline?: string
  merchantReviewReason?: string
  merchantReviewTime?: string
  adminReviewReason?: string
  adminReviewTime?: string
  createTime?: string
  successTime?: string
  logs?: Array<{
    id: number
    action: string
    comment?: string
    operatorRole?: string
    operatorId?: number
    fromStatus?: string
    toStatus?: string
    createTime?: string
  }>
}

/**
 * 获取商家优惠券列表
 */
export async function getMerchantVoucherList(params: {
  page?: number
  size?: number
  status?: number
}): Promise<MerchantVoucherListResponse> {
  const { data } = await http.get('/merchant/vouchers', { params })
  return data.data
}

/**
 * 获取优惠券详情
 */
export async function getMerchantVoucherDetail(voucherId: number): Promise<MerchantVoucher> {
  const { data } = await http.get(`/merchant/vouchers/${voucherId}`)
  return data.data
}

/**
 * 创建优惠券
 */
export async function createMerchantVoucher(voucher: {
  voucherType?: 'group' | 'seckill'
  title: string
  subTitle?: string
  stock: number
  seckillStock?: number
  amount: number
  payValue?: number
  beginTime?: string
  endTime?: string
  seckillStartTime?: string
  seckillEndTime?: string
}): Promise<MerchantVoucher> {
  const { data } = await http.post('/merchant/vouchers', voucher)
  return data.data
}

/**
 * 更新优惠券
 */
export async function updateMerchantVoucher(
  voucherId: number,
  voucher: {
    voucherType?: 'group' | 'seckill'
    title?: string
    subTitle?: string
    stock?: number
    seckillStock?: number
    amount?: number
    payValue?: number
    beginTime?: string
    endTime?: string
    seckillStartTime?: string
    seckillEndTime?: string
  }
): Promise<MerchantVoucher> {
  const { data } = await http.put(`/merchant/vouchers/${voucherId}`, voucher)
  return data.data
}

/**
 * 上架优惠券
 */
export async function onlineMerchantVoucher(voucherId: number): Promise<MerchantVoucher> {
  const { data } = await http.post(`/merchant/vouchers/${voucherId}/online`)
  return data.data
}

/**
 * 下架优惠券
 */
export async function offlineMerchantVoucher(voucherId: number): Promise<MerchantVoucher> {
  const { data } = await http.post(`/merchant/vouchers/${voucherId}/offline`)
  return data.data
}

/**
 * 删除优惠券
 */
export async function deleteMerchantVoucher(voucherId: number): Promise<void> {
  const { data } = await http.delete(`/merchant/vouchers/${voucherId}`)
  return data.data
}

export async function getMerchantRefunds(params?: {
  status?: string
}): Promise<MerchantRefundRecord[]> {
  const { data } = await http.get('/merchant/refunds', { params })
  return data.data || []
}

export async function getMerchantRefundDetail(refundNo: string): Promise<MerchantRefundRecord> {
  const { data } = await http.get(`/merchant/refunds/${refundNo}`)
  return data.data
}

export async function approveMerchantRefund(refundNo: string, payload?: {
  approvedAmount?: number
  reason?: string
}) {
  const { data } = await http.post(`/merchant/refunds/${refundNo}/approve`, payload || {})
  return data.data
}

export async function rejectMerchantRefund(refundNo: string, payload?: {
  reason?: string
}) {
  const { data } = await http.post(`/merchant/refunds/${refundNo}/reject`, payload || {})
  return data.data
}
