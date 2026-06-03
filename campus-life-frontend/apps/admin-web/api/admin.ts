import http from './http'

// ==================== 帖子管理 ====================

export interface AdminPost {
  id: number
  userId: number
  title: string
  content: string
  status: number  // 0-正常;1-仅自己可见;2-已删除;3-屏蔽
  isPinned: number
  isHot: number
  likeCount: number
  commentCount: number
  viewCount: number
  createTime: string
  authorName?: string
  authorAvatar?: string
}

export interface AdminPostListResponse {
  list: AdminPost[]
  page: number
  size: number
  total: number
}

/**
 * 获取帖子列表（管理后台）
 */
export async function getAdminPostList(params: {
  page?: number
  size?: number
  status?: number
  keyword?: string
  userId?: number
}): Promise<AdminPostListResponse> {
  const { data } = await http.get('/admin/posts', { params })
  return data.data
}

/**
 * 删除帖子
 */
export async function deletePost(postId: number, reason: string) {
  const { data } = await http.post(`/admin/posts/${postId}/delete`, { reason })
  return data
}

/**
 * 屏蔽帖子
 */
export async function banPost(postId: number, reason: string) {
  const { data } = await http.post(`/admin/posts/${postId}/ban`, { reason })
  return data
}

/**
 * 解除屏蔽帖子
 */
export async function unbanPost(postId: number, reason: string) {
  const { data } = await http.post(`/admin/posts/${postId}/unban`, { reason })
  return data
}

/**
 * 置顶帖子
 */
export async function pinPost(postId: number, reason?: string) {
  const { data } = await http.post(`/admin/posts/${postId}/pin`, { reason: reason || '' })
  return data
}

/**
 * 取消置顶
 */
export async function unpinPost(postId: number, reason?: string) {
  const { data } = await http.post(`/admin/posts/${postId}/unpin`, { reason: reason || '' })
  return data
}

/**
 * 设为热门
 */
export async function setHotPost(postId: number, reason?: string) {
  const { data } = await http.post(`/admin/posts/${postId}/set-hot`, { reason: reason || '' })
  return data
}

/**
 * 取消热门
 */
export async function removeHotPost(postId: number, reason?: string) {
  const { data } = await http.post(`/admin/posts/${postId}/remove-hot`, { reason: reason || '' })
  return data
}

// ==================== 商家管理 ====================

export interface AdminMerchant {
  id: number
  userId: number
  name: string
  contactName: string
  contactPhone: string
  address: string
  typeId: number
  status: number  // 0-待审核;1-正常;2-冻结;3-关闭
  createTime: string
}

export interface AdminMerchantListResponse {
  list: AdminMerchant[]
  page: number
  size: number
  total: number
}

/**
 * 获取商家列表（管理后台）
 */
export async function getAdminMerchantList(params: {
  page?: number
  size?: number
  status?: number
  keyword?: string
}): Promise<AdminMerchantListResponse> {
  const { data } = await http.get('/admin/merchants', { params })
  return data.data
}

/**
 * 审核通过商家
 */
export async function approveMerchant(merchantId: number, reason: string) {
  const { data } = await http.post(`/admin/merchants/${merchantId}/approve`, { reason })
  return data
}

/**
 * 审核拒绝商家
 */
export async function rejectMerchant(merchantId: number, reason: string) {
  const { data } = await http.post(`/admin/merchants/${merchantId}/reject`, { reason })
  return data
}

/**
 * 冻结商家
 */
export async function freezeMerchant(merchantId: number, reason: string) {
  const { data } = await http.post(`/admin/merchants/${merchantId}/freeze`, { reason })
  return data
}

/**
 * 解冻商家
 */
export async function unfreezeMerchant(merchantId: number, reason: string) {
  const { data } = await http.post(`/admin/merchants/${merchantId}/unfreeze`, { reason })
  return data
}

// ==================== 券管理 ====================

export interface AdminVoucher {
  id: number
  merchantId: number
  title: string
  subTitle: string
  stock: number
  amount: number
  payValue: number
  status: number  // 0-下架;1-上架;2-已下架(管理员);3-违规下架
  beginTime: string
  endTime: string
  createTime: string
}

export interface AdminVoucherListResponse {
  list: AdminVoucher[]
  page: number
  size: number
  total: number
}

export interface AdminVoucherOrderItem {
  id: number
  orderNo: string
  userId: number
  userName?: string
  voucherId: number
  voucherTitle?: string
  shopName?: string
  payAmount: number
  status: number
  statusText?: string
  paymentStatus?: number
  paymentMethod?: string
  orderSource?: string
  payDeadline?: string
  payTime?: string
  useTime?: string
  cancelReason?: string
  createTime?: string
}

export interface AdminVoucherOrderListResponse {
  list: AdminVoucherOrderItem[]
  page: number
  size: number
  total: number
}

export interface AdminVoucherOrderStatsResponse {
  summary: {
    totalOrders: number
    totalAmount: number
    avgOrderAmount: number
    usedOrders: number
    pendingOrders: number
    paidOrders: number
    cancelledOrders: number
    seckillOrders: number
  }
  trend: Array<{ day: string; orderCount: number; amount: number }>
  voucherUsage: Array<{ voucherId: number; voucherTitle: string; useCount: number }>
  timeRange?: { start: string; end: string }
}

export interface AdminRefundRecord {
  refundNo: string
  paymentNo: string
  bizOrderNo: string
  userId?: number
  merchantId?: number
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

export interface PaymentReconciliationIssueRecord {
  id: number
  issueKey: string
  bizType: string
  paymentNo?: string
  bizOrderNo: string
  userId?: number
  channel?: string
  issueType: string
  issueLevel: string
  issueStatus: string
  issueMessage: string
  paymentOrderStatus?: string
  voucherOrderStatus?: number
  voucherPaymentStatus?: number
  paymentAmount?: number
  recordedRefundedAmount?: number
  actualRefundedAmount?: number
  lastCheckedTime?: string
  resolvedTime?: string
  resolvedBy?: number
  resolveNote?: string
  createTime?: string
  updateTime?: string
}

/**
 * 获取券列表（管理后台）
 */
export async function getAdminVoucherList(params: {
  page?: number
  size?: number
  status?: number
  merchantId?: number
  keyword?: string
}): Promise<AdminVoucherListResponse> {
  const { data } = await http.get('/admin/vouchers', { params })
  return data.data
}

export async function getAdminVoucherOrderList(params: {
  page?: number
  size?: number
  keyword?: string
  userId?: number
  voucherId?: number
  status?: number
  paymentStatus?: number
  orderSource?: string
  timeRange?: string
  startDate?: string
  endDate?: string
}): Promise<AdminVoucherOrderListResponse> {
  const { data } = await http.get('/admin/voucher-orders', { params })
  return data?.data || { list: [], page: 1, size: 20, total: 0 }
}

export async function getAdminVoucherOrderStats(params: {
  keyword?: string
  userId?: number
  voucherId?: number
  status?: number
  paymentStatus?: number
  orderSource?: string
  timeRange?: string
  startDate?: string
  endDate?: string
}): Promise<AdminVoucherOrderStatsResponse> {
  const { data } = await http.get('/admin/voucher-orders/stats', { params })
  return data?.data || { summary: {} as any, trend: [], voucherUsage: [] }
}

export async function getAdminRefundList(params?: {
  status?: string
}): Promise<AdminRefundRecord[]> {
  const { data } = await http.get('/admin/refunds', { params })
  return data.data || []
}

export async function getAdminRefundDetail(refundNo: string): Promise<AdminRefundRecord> {
  const { data } = await http.get(`/admin/refunds/${refundNo}`)
  return data.data
}

export async function approveAdminRefund(refundNo: string, payload?: {
  approvedAmount?: number
  reason?: string
}) {
  const { data } = await http.post(`/admin/refunds/${refundNo}/approve`, payload || {})
  return data.data
}

export async function rejectAdminRefund(refundNo: string, payload?: {
  reason?: string
}) {
  const { data } = await http.post(`/admin/refunds/${refundNo}/reject`, payload || {})
  return data.data
}

export async function getPaymentReconciliationIssues(params?: {
  status?: string
  issueType?: string
}): Promise<{
  summary: {
    totalIssues: number
    openIssues: number
    resolvedIssues: number
    criticalOpenIssues: number
  }
  list: PaymentReconciliationIssueRecord[]
}> {
  const { data } = await http.get('/admin/payment-reconciliation/issues', { params })
  return data.data || {
    summary: { totalIssues: 0, openIssues: 0, resolvedIssues: 0, criticalOpenIssues: 0 },
    list: []
  }
}

export async function triggerPaymentReconciliationScan(params?: {
  days?: number
  limit?: number
}) {
  const { data } = await http.post('/admin/payment-reconciliation/scan', null, { params })
  return data.data
}

export async function resolvePaymentReconciliationIssue(issueId: number, payload?: {
  note?: string
}) {
  const { data } = await http.post(`/admin/payment-reconciliation/issues/${issueId}/resolve`, payload || {})
  return data.data
}

/**
 * 管理员下架券
 */
export async function offlineVoucher(voucherId: number, reason: string) {
  const { data } = await http.post(`/admin/vouchers/${voucherId}/offline`, { reason })
  return data
}

/**
 * 违规下架券
 */
export async function violationOfflineVoucher(voucherId: number, reason: string) {
  const { data } = await http.post(`/admin/vouchers/${voucherId}/violation-offline`, { reason })
  return data
}

export async function onlineVoucher(voucherId: number) {
  const { data } = await http.post(`/admin/vouchers/${voucherId}/online`)
  return data
}

export async function preheatVoucher(voucherId: number) {
  const { data } = await http.post(`/admin/vouchers/${voucherId}/preheat`)
  return data
}

export async function monitorVoucher(voucherId: number) {
  const { data } = await http.get(`/admin/vouchers/${voucherId}/monitor`)
  return data.data
}

export async function seckillHealth(voucherId?: number) {
  const { data } = await http.get('/admin/seckill/health', {
    params: voucherId ? { voucherId } : {}
  })
  return data.data
}

// ==================== 评论管理 ====================

export interface AdminComment {
  id: number
  postId: number
  userId: number
  content: string
  status: number  // 0-正常;1-已删除;2-屏蔽
  likeCount: number
  createTime: string
  userName?: string
  userAvatar?: string
}

export interface AdminCommentListResponse {
  list: AdminComment[]
  page: number
  size: number
  total: number
}

/**
 * 获取评论列表（管理后台）
 */
export async function getAdminCommentList(params: {
  page?: number
  size?: number
  status?: number
  postId?: number
  userId?: number
  keyword?: string
}): Promise<AdminCommentListResponse> {
  const { data } = await http.get('/admin/comments', { params })
  return data.data
}

/**
 * 删除评论
 */
export async function deleteComment(commentId: number, reason: string) {
  const { data } = await http.post(`/admin/comments/${commentId}/delete`, { reason })
  return data
}

/**
 * 屏蔽评论
 */
export async function banComment(commentId: number, reason: string) {
  const { data } = await http.post(`/admin/comments/${commentId}/ban`, { reason })
  return data
}

/**
 * 解除屏蔽评论
 */
export async function unbanComment(commentId: number, reason: string) {
  const { data } = await http.post(`/admin/comments/${commentId}/unban`, { reason })
  return data
}

// ==================== 举报管理 ====================

export interface AdminReport {
  id: number
  reporterId: number
  reporterName: string
  targetType: number
  targetId: number
  reason: string
  status: number
  handlerId?: number
  handleResult?: string
  createTime: string
  updateTime?: string
  targetSummary?: string
}

export interface AdminReportListResponse {
  list: AdminReport[]
  page: number
  size: number
  total: number
}

export async function getAdminReportList(params: {
  page?: number
  size?: number
  status?: number
  targetType?: number
  keyword?: string
}): Promise<AdminReportListResponse> {
  const { data } = await http.get('/admin/reports', { params })
  return data.data
}

export async function processAdminReport(reportId: number, payload: {
  status: 1 | 2
  handleResult?: string
  action?: 'NONE' | 'DELETE_POST' | 'BAN_POST' | 'DELETE_COMMENT' | 'BAN_COMMENT' | 'IGNORE'
}) {
  const { data } = await http.post(`/admin/reports/${reportId}/process`, payload)
  return data
}

// ==================== 操作日志 ====================

export interface AdminOperationLog {
  id: number
  adminId: number
  operationType: string
  targetType: string
  targetId: number
  action: string
  oldStatus: number | null
  newStatus: number | null
  reason: string
  ipAddress: string
  createTime: string
}

export interface AdminOperationLogListResponse {
  list: AdminOperationLog[]
  page: number
  size: number
  total: number
}

/**
 * 获取操作日志列表
 */
export async function getAdminOperationLogList(params: {
  page?: number
  size?: number
  adminId?: number
  operationType?: string
  targetType?: string
  action?: string
  startTime?: string
  endTime?: string
}): Promise<AdminOperationLogListResponse> {
  const { data } = await http.get('/admin/operation-logs', { params })
  return data.data
}

// ==================== 统计数据 ====================

export interface AdminStats {
  studentAuth: {
    pending: number
    approved: number
    rejected: number
  }
  merchantAuth: {
    pending: number
    approved: number
    rejected: number
  }
  posts: {
    total: number
    pending: number
    banned: number
  }
  comments: {
    total: number
    pending: number
    banned: number
  }
  merchants: {
    total: number
    normal: number
    frozen: number
  }
  vouchers: {
    total: number
    online: number
    offline: number
  }
}

/**
 * 获取管理后台统计数据
 */
export async function getAdminStats(): Promise<AdminStats> {
  const { data } = await http.get('/admin/stats')
  return data.data
}

// ==================== 工作台统计数据 ====================

export interface DashboardStats {
  todayIP: number
  memberCount: number
  postCount: number
  voucherCount: number
  merchantCount: number
  contributionData?: {
    data: number[][]
    months: string[]
    weeks: string[]
  }
  contentTypeData?: Array<{
    name: string
    value: number
    percent: number
    color: string
  }>
  voucherTypeData?: Array<{
    name: string
    value: number
    percent: number
    color: string
  }>
  merchantTypeData?: Array<{
    name: string
    value: number
    percent: number
    color: string
  }>
}

/**
 * 获取工作台统计数据
 */
export async function getDashboardStats(): Promise<DashboardStats> {
  const { data } = await http.get('/admin/dashboard/stats')
  return data.data
}

// ==================== 会员管理 ====================

export interface AdminMember {
  id: number
  xiaolanshuId?: string
  avatar?: string
  name: string
  gender: number  // 1-男;2-女
  role?: number
  isStudentVerified?: number
  isMerchant?: number
  phone?: string
  source: string  // app/web/miniapp
  noteCount?: number
  productCount?: number
  loginIp?: string
  loginLocation?: string
  status: number  // 0-禁用;1-启用
  createTime: string
}

export interface AdminMemberListResponse {
  list: AdminMember[]
  page: number
  size: number
  total: number
  enabledTotal?: number
  disabledTotal?: number
}

export interface StudentAuthRequestItem {
  id: number
  userId: number
  realName: string
  school: string
  college?: string
  major?: string
  grade?: string
  className?: string
  studentNo: string
  studentCardImg?: string
  status: number // 0-待审核;1-通过;2-驳回
  reason?: string
  reviewerId?: number
  createTime?: string
  reviewTime?: string
}

export interface StudentAuthRequestListResponse {
  list: StudentAuthRequestItem[]
  page: number
  size: number
  total: number
}

const mapSystemUserToAdminMember = (item: any): AdminMember => ({
  id: Number(item?.id || 0),
  xiaolanshuId: String(item?.xiaolanshuId || item?.id || ''),
  avatar: item?.avatarUrl || '',
  name: item?.username || `用户${item?.id || ''}`,
  gender: Number(item?.gender || 0),
  role: Number(item?.role ?? 0),
  isStudentVerified: Number(item?.isStudentVerified ?? 0),
  isMerchant: Number(item?.isMerchant ?? 0),
  phone: item?.phone || '',
  source: 'app',
  noteCount: Number(item?.postCount || 0),
  productCount: Number(item?.voucherCount || 0),
  loginIp: '',
  loginLocation: item?.region || '',
  status: Number(item?.status ?? 1),
  createTime: item?.createTime || ''
})

/**
 * 获取会员列表（管理后台）
 */
export async function getMemberList(params: {
  page?: number
  size?: number
  keyword?: string
  xiaolanshuId?: string
  memberName?: string
  phone?: string
  email?: string
  role?: string | number
  status?: number
  startDate?: string
  endDate?: string
}): Promise<AdminMemberListResponse> {
  const role =
    params.role === '' || params.role === undefined || params.role === null
      ? undefined
      : Number(params.role)
  const status =
    params.status === '' || params.status === undefined || params.status === null
      ? undefined
      : Number(params.status)

  const query = {
    page: params.page,
    size: params.size,
    keyword: params.keyword || params.xiaolanshuId || undefined,
    username: params.memberName || undefined,
    phone: params.phone || undefined,
    email: params.email || undefined,
    role,
    status,
    startDate: params.startDate || undefined,
    endDate: params.endDate || undefined
  }

  try {
    const { data } = await http.get('/admin/members', { params: query })
    const payload = data?.data || {}
    return {
      list: Array.isArray(payload.list) ? payload.list.map(mapSystemUserToAdminMember) : [],
      page: Number(payload.page || query.page || 1),
      size: Number(payload.size || query.size || 10),
      total: Number(payload.total || 0),
      enabledTotal: payload.enabledTotal === undefined ? undefined : Number(payload.enabledTotal || 0),
      disabledTotal: payload.disabledTotal === undefined ? undefined : Number(payload.disabledTotal || 0)
    }
  } catch (error) {
    // 兼容旧接口，避免后端未更新时页面不可用
    const { data } = await http.get('/admin/system/users', { params: query })
    const payload = data?.data || {}
    return {
      list: Array.isArray(payload.list) ? payload.list.map(mapSystemUserToAdminMember) : [],
      page: Number(payload.page || query.page || 1),
      size: Number(payload.size || query.size || 10),
      total: Number(payload.total || 0),
      enabledTotal: payload.enabledTotal === undefined ? undefined : Number(payload.enabledTotal || 0),
      disabledTotal: payload.disabledTotal === undefined ? undefined : Number(payload.disabledTotal || 0)
    }
  }
}

/**
 * 删除会员
 */
export async function deleteMember(memberId: number) {
  const { data } = await http.delete(`/admin/system/users/${memberId}`)
  return data
}

/**
 * 更新会员状态
 */
export async function updateMemberStatus(memberId: number, status: number) {
  const { data } = await http.put(`/admin/system/users/${memberId}/status`, {
    status,
    reason: status === 1 ? '管理员启用用户' : '管理员禁用用户'
  })
  return data
}

export async function getStudentAuthRequests(params: {
  page?: number
  size?: number
  status?: number
  keyword?: string
}): Promise<StudentAuthRequestListResponse> {
  const { data } = await http.get('/admin/student-auth/requests', { params })
  return data.data
}

export async function approveStudentAuth(requestId: number, reason = '后台审核通过') {
  const { data } = await http.post(`/admin/student-auth/${requestId}/approve`, { reason })
  return data
}

export async function rejectStudentAuth(requestId: number, reason = '后台审核驳回') {
  const { data } = await http.post(`/admin/student-auth/${requestId}/reject`, { reason })
  return data
}

// ==================== 会员统计 ====================

export interface MemberStats {
  totalStudents: number
  totalMerchants?: number
  totalSchools: number
  verifiedStudents: number
  verifiedMerchants?: number
  monthlyNew: number
  genderData: Array<{
    name: string
    value: number
    percent: number
    color: string
  }>
  gradeData: Array<{
    name: string
    value: number
  }>
  schoolData: Array<{
    name: string
    value: number
    verified: number
    unverified: number
  }>
  collegeData: Array<{
    name: string
    value: number
  }>
}

/**
 * 获取会员统计数据
 */
export async function getMemberStats(): Promise<MemberStats> {
  const { data } = await http.get('/admin/system/users/stats')
  const payload = data?.data || {}
  return {
    totalStudents: Number(payload.totalStudents ?? payload.student ?? 0),
    totalMerchants: Number(payload.totalMerchants ?? payload.merchant ?? 0),
    totalSchools: Number(payload.totalSchools ?? 0),
    verifiedStudents: Number(payload.verifiedStudents ?? 0),
    verifiedMerchants: Number(payload.verifiedMerchants ?? 0),
    monthlyNew: Number(payload.monthlyNew ?? 0),
    genderData: Array.isArray(payload.genderData) ? payload.genderData : [],
    gradeData: Array.isArray(payload.gradeData) ? payload.gradeData : [],
    schoolData: Array.isArray(payload.schoolData) ? payload.schoolData : [],
    collegeData: Array.isArray(payload.collegeData) ? payload.collegeData : []
  }
}

// ==================== 会员登录日志 ====================

export interface MemberLoginLog {
  id: number
  memberId: number
  username?: string
  loginIp?: string
  loginLocation?: string
  loginTime: string
}

export interface MemberLoginLogListResponse {
  list: MemberLoginLog[]
  page: number
  size: number
  total: number
}

/**
 * 获取会员登录日志列表
 */
export async function getMemberLoginLogList(params: {
  page?: number
  size?: number
  memberId?: number
  username?: string
  startTime?: string
  endTime?: string
}): Promise<MemberLoginLogListResponse> {
  const { data } = await http.get('/admin/logs/member-login', { params })
  return data.data
}

// ==================== 管理员登录日志 ====================

export interface AdminLoginLog {
  id: number
  adminId: number
  username?: string
  loginIp?: string
  loginLocation?: string
  loginTime: string
}

export interface AdminLoginLogListResponse {
  list: AdminLoginLog[]
  page: number
  size: number
  total: number
}

/**
 * 获取管理员登录日志列表
 */
export async function getAdminLoginLogList(params: {
  page?: number
  size?: number
  adminId?: number
  username?: string
  startTime?: string
  endTime?: string
}): Promise<AdminLoginLogListResponse> {
  const { data } = await http.get('/admin/logs/admin-login', { params })
  return data.data
}

// ==================== 审核日志 ====================

export interface AuditLog {
  id: number
  contentId: number
  contentType: string  // post, comment
  result: string  // pass, reject, manual
  score: number | null
  provider?: string
  details?: string
  auditTime: string
}

export interface AuditLogListResponse {
  list: AuditLog[]
  page: number
  size: number
  total: number
}

/**
 * 获取审核日志列表
 */
export async function getAuditLogList(params: {
  page?: number
  size?: number
  contentId?: number
  contentType?: string
  provider?: string
  result?: string
  startTime?: string
  endTime?: string
}): Promise<AuditLogListResponse> {
  const { data } = await http.get('/admin/logs/audit', { params })
  return data.data
}

// ==================== 在线用户 ====================

export interface OnlineUser {
  userId: number
  username?: string
  nickname?: string
  loginIp?: string
  loginLocation?: string
  loginTime: string
  lastActiveTime: string
}

export interface OnlineUserListResponse {
  list: OnlineUser[]
  page: number
  size: number
  total: number
  totalOnline?: number
}

/**
 * 获取在线用户列表
 */
export async function getOnlineUsers(params: {
  page?: number
  size?: number
  username?: string
  ip?: string
}): Promise<OnlineUserListResponse> {
  const { data } = await http.get('/admin/monitor/online-users', { params })
  return data.data
}

/**
 * 强制用户下线
 */
export async function forceLogoutUser(userId: number) {
  const { data } = await http.post(`/admin/monitor/online-users/${userId}/force-logout`)
  return data
}

// ==================== 缓存管理 ====================

export interface CacheItem {
  key: string
  value: any
  type: string
  size: number
  expireTime?: string
}

export interface CacheListResponse {
  list: CacheItem[]
  page: number
  size: number
  total: number
  totalCache?: number
  memoryUsage?: string
}

/**
 * 获取缓存列表
 */
export async function getCacheList(params: {
  page?: number
  size?: number
  key?: string
}): Promise<CacheListResponse> {
  const { data } = await http.get('/admin/monitor/cache', { params })
  return data.data
}

/**
 * 删除缓存项
 */
export async function deleteCacheItem(key: string) {
  const { data } = await http.delete(`/admin/monitor/cache/${encodeURIComponent(key)}`)
  return data
}

/**
 * 清空所有缓存
 */
export async function clearAllCache() {
  const { data } = await http.post('/admin/monitor/cache/clear')
  return data
}

// ==================== 系统接口 ====================

export interface ApiParam {
  name: string
  type: string
  required: boolean
  description?: string
}

export interface ApiInfo {
  path: string
  method: string
  description?: string
  module?: string
  params?: ApiParam[]
  response?: any
}

export interface ApiListResponse {
  list: ApiInfo[]
  total: number
}

/**
 * 获取系统接口列表
 */
export async function getApiList(): Promise<ApiListResponse> {
  try {
    const { data } = await http.get('/admin/tools/apis')
    return data.data
  } catch (error) {
    // 如果接口不存在，返回空列表
    return { list: [], total: 0 }
  }
}

// ==================== 用户管理 ====================

export interface SystemUser {
  id: number
  username?: string
  phone?: string
  email?: string
  avatarUrl?: string
  role: number  // 0-学生;1-商家;2-管理员
  status: number  // 0-禁用;1-正常
  gender?: number  // 0-保密;1-男;2-女
  bio?: string
  region?: string
  createTime: string
}

export interface SystemUserListResponse {
  list: SystemUser[]
  page: number
  size: number
  total: number
}

export interface UserStats {
  student: number
  merchant: number
  admin: number
  total: number
}

/**
 * 获取用户列表（系统管理）
 */
export async function getUserList(params: {
  page?: number
  size?: number
  username?: string
  phone?: string
  email?: string
  role?: number
  status?: number
  startDate?: string
  endDate?: string
}): Promise<SystemUserListResponse> {
  const { data } = await http.get('/admin/system/users', { params })
  return data.data
}

/**
 * 更新用户状态
 */
export async function updateUserStatus(userId: number, status: number) {
  const { data } = await http.put(`/admin/system/users/${userId}/status`, { status })
  return data
}

/**
 * 删除用户
 */
export async function deleteUserById(userId: number) {
  const { data } = await http.delete(`/admin/system/users/${userId}`)
  return data
}

/**
 * 获取用户统计数据
 */
export async function getUserStats(): Promise<UserStats> {
  const { data } = await http.get('/admin/system/users/stats')
  return data.data
}

// ==================== AI 管理 ====================

export interface AdminAiOverview {
  totalCalls: number
  successCalls: number
  failedCalls: number
  totalTokens: number
  avgLatencyMs: number
  totalCostAmount?: number
}

export interface AdminAiCallLog {
  id: number
  requestId: string
  sessionId?: string
  userId: number
  capabilityCode: string
  sceneCode: string
  providerCode: string
  modelCode?: string
  success: number
  latencyMs?: number
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  fallbackLevel?: number
  promptChars?: number
  completionChars?: number
  costAmount?: number
  errorCode?: string
  errorType?: string
  errorMessage?: string
  createdAt: string
}

export interface AdminAiProviderConfig {
  id: number
  providerCode: string
  providerName: string
  baseUrl: string
  defaultModelCode: string
  enabled: boolean
  timeoutMs?: number
  maxContextMessages?: number
  temperature?: number
  topP?: number
  maxOutputTokens?: number
  systemPromptTemplate?: string
  hasApiKey: boolean
  createdAt?: string
  updatedAt?: string
}

export interface AdminAiProviderHealthCheck {
  providerCode: string
  providerName: string
  modelCode?: string
  enabled: boolean
  available: boolean
  apiKeyConfigured: boolean
  baseUrl?: string
  message?: string
  checkedAt?: string
}

export interface AdminAiProviderUpsertPayload {
  providerCode: string
  providerName: string
  baseUrl: string
  apiKeyCipher?: string
  defaultModelCode: string
  enabled?: boolean
  timeoutMs?: number
  maxContextMessages?: number
  temperature?: number
  topP?: number
  maxOutputTokens?: number
  systemPromptTemplate?: string
}

export interface AdminAiModelConfig {
  id: number
  providerCode: string
  modelCode: string
  modelName: string
  capabilitiesJson?: string
  contextWindow?: number
  maxOutputTokens?: number
  inputPricePer1k?: number
  outputPricePer1k?: number
  enabled: boolean
  priority?: number
  createdAt?: string
  updatedAt?: string
}

export interface AdminAiModelUpsertPayload {
  providerCode: string
  modelCode: string
  modelName: string
  capabilitiesJson?: string
  contextWindow?: number
  maxOutputTokens?: number
  inputPricePer1k?: number
  outputPricePer1k?: number
  enabled?: boolean
  priority?: number
}

export interface AdminAiGatewayRouteRule {
  id: number
  ruleName: string
  capabilityCode: string
  sceneCode?: string
  matchRuleJson?: string
  routeRuleJson: string
  enabled: boolean
  priority?: number
  createdAt?: string
  updatedAt?: string
}

export interface AdminAiGatewayRouteRuleUpsertPayload {
  ruleName: string
  capabilityCode: string
  sceneCode?: string
  matchRuleJson?: string
  routeRuleJson: string
  enabled?: boolean
  priority?: number
}

export interface AdminAiGatewaySafetyRule {
  id: number
  ruleName: string
  capabilityCode?: string
  sceneCode?: string
  direction: 'INPUT' | 'OUTPUT' | 'BOTH'
  action: 'BLOCK' | 'AUDIT'
  matchType: 'KEYWORD' | 'REGEX'
  patternText: string
  category: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  enabled: boolean
  priority?: number
  createdAt?: string
  updatedAt?: string
}

export interface AdminAiGatewaySafetyRuleUpsertPayload {
  ruleName: string
  capabilityCode?: string
  sceneCode?: string
  direction: 'INPUT' | 'OUTPUT' | 'BOTH'
  action: 'BLOCK' | 'AUDIT'
  matchType: 'KEYWORD' | 'REGEX'
  patternText: string
  category: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  enabled?: boolean
  priority?: number
}

export interface AdminAiUsageQuota {
  id: number
  subjectType: 'GLOBAL' | 'USER' | 'ROLE' | 'MERCHANT'
  subjectId?: string
  capabilityCode?: string
  sceneCode?: string
  quotaPeriod: 'DAY' | 'MONTH'
  maxCalls?: number
  maxTokens?: number
  maxCost?: number
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export interface AdminAiUsageQuotaUpsertPayload {
  subjectType: 'GLOBAL' | 'USER' | 'ROLE' | 'MERCHANT'
  subjectId?: string
  capabilityCode?: string
  sceneCode?: string
  quotaPeriod: 'DAY' | 'MONTH'
  maxCalls?: number
  maxTokens?: number
  maxCost?: number
  enabled?: boolean
}

export interface AdminAiUsageTrendItem {
  bucket: string
  totalCalls: number
  successCalls: number
  totalTokens: number
  totalCostAmount?: number
}

export interface AdminAiModelRankingItem {
  providerCode: string
  modelCode?: string
  totalCalls: number
  successCalls: number
  totalTokens: number
  totalCostAmount?: number
  avgLatencyMs?: number
}

export interface AdminAiGatewayProbePayload {
  content: string
  capabilityCode?: string
  sceneCode?: string
  providerCode?: string
  modelCode?: string
  temperature?: number
  maxOutputTokens?: number
}

export interface AdminAiGatewayProbeResponse {
  requestId: string
  content?: string
  success: boolean
  providerCode?: string
  modelCode?: string
  capabilityCode?: string
  sceneCode?: string
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  costAmount?: number
  latencyMs?: number
  fallbackLevel?: number
  finishReason?: string
  errorCode?: string
  errorType?: string
  errorMessage?: string
  createdAt?: string
}

export interface AiModerationCheckPayload {
  content: string
  targetType?: string
  sceneCode?: string
  providerCode?: string
  modelCode?: string
}

export interface AiModerationCheckResponse {
  requestId: string
  targetType?: string
  result: 'PASS' | 'REJECT' | 'REVIEW'
  score?: number
  categories: string[]
  reason?: string
  rawResponse?: string
  providerCode?: string
  modelCode?: string
  capabilityCode?: string
  sceneCode?: string
  latencyMs?: number
  fallbackLevel?: number
  createdAt?: string
}

export interface AdminAiCapabilityConfig {
  id: number
  capabilityCode: string
  capabilityName: string
  enabled: boolean
  grayEnabled: boolean
  grayRuleJson?: string
  rateLimitJson?: string
  quotaRuleJson?: string
  status: string
  createdAt?: string
  updatedAt?: string
}

export interface AdminAiCapabilityUpsertPayload {
  capabilityCode: string
  capabilityName: string
  enabled?: boolean
  grayEnabled?: boolean
  grayRuleJson?: string
  rateLimitJson?: string
  quotaRuleJson?: string
}

export interface AdminAiSceneConfig {
  id: number
  capabilityCode: string
  sceneCode: string
  sceneName: string
  providerCode?: string
  modelCode?: string
  enabled: boolean
  systemPromptTemplate?: string
  inputSchemaJson?: string
  outputSchemaJson?: string
  safetyLevel?: string
  timeoutMs?: number
  createdAt?: string
  updatedAt?: string
}

export interface AdminAiSceneUpsertPayload {
  capabilityCode: string
  sceneCode: string
  sceneName: string
  providerCode?: string
  modelCode?: string
  enabled?: boolean
  systemPromptTemplate?: string
  inputSchemaJson?: string
  outputSchemaJson?: string
  safetyLevel?: string
  timeoutMs?: number
}

export interface AdminAiOpsServiceStatus {
  code: string
  name: string
  healthUrl: string
  status: string
  httpStatus?: number
  latencyMs?: number
  message?: string
}

export interface AdminAiOpsAlert {
  alertName: string
  description: string
  state: string
  activeAt: string
  duration: string
  severity?: string
  service?: string
  source?: string
}

export interface AdminAiOpsMetricSample {
  name: string
  service?: string
  instance?: string
  value?: number
  unit?: string
  timestamp?: string
  labels?: Record<string, unknown>
  source?: string
}

export interface AdminAiOpsDataSourceStatus {
  code: string
  name: string
  status: string
  message?: string
}

export interface AdminAiOpsLogTopic {
  topicName: string
  description: string
  exampleQueries: string[]
  relatedAlerts: string[]
}

export interface AdminAiOpsLogEntry {
  timestamp: string
  level: string
  service: string
  topic: string
  message: string
  fields?: Record<string, unknown>
}

export interface AdminAiAgentEntry {
  code: string
  name: string
  description: string
  frontendPath: string
  backendPath: string
  sceneCode: string
}

export interface AdminAiOpsSnapshot {
  generatedAt: string
  runtime?: {
    javaVersion: string
    processors: number
    heapUsedBytes: number
    heapMaxBytes: number
    diskTotalBytes: number
    diskUsableBytes: number
  }
  provider?: {
    providerCode: string
    providerName: string
    baseUrl: string
    defaultModelCode: string
    enabled: boolean
    available: boolean
    apiKeyConfigured: boolean
  }
  aiOverview?: AdminAiOverview
  dataSources: AdminAiOpsDataSourceStatus[]
  services: AdminAiOpsServiceStatus[]
  alerts: AdminAiOpsAlert[]
  metrics: AdminAiOpsMetricSample[]
  logTopics: AdminAiOpsLogTopic[]
  recentLogs: AdminAiOpsLogEntry[]
  recentAiCalls: AdminAiCallLog[]
}

export interface AdminAiOpsAnalyzePayload {
  question?: string
  includeAlerts?: boolean
  includeLogs?: boolean
  includeAiLogs?: boolean
  logLimit?: number
}

export interface AdminAiOpsAnalysisResponse {
  report: string
  snapshot: AdminAiOpsSnapshot
  providerCode: string
  modelCode: string
  latencyMs?: number
  generatedAt: string
}

export async function getAdminAiOverview(): Promise<AdminAiOverview> {
  const { data } = await http.get('/admin/ai/stats/overview')
  return data.data
}

export async function getAdminAiAgents(): Promise<AdminAiAgentEntry[]> {
  const { data } = await http.get('/admin/ai/agents')
  return data.data || []
}

export async function getAdminAiLogs(limit = 20): Promise<AdminAiCallLog[]> {
  const { data } = await http.get('/admin/ai/logs', { params: { limit } })
  return data.data || []
}

export async function getAdminAiProviders(): Promise<AdminAiProviderConfig[]> {
  const { data } = await http.get('/admin/ai/providers')
  return data.data || []
}

export async function createAdminAiProvider(payload: AdminAiProviderUpsertPayload): Promise<AdminAiProviderConfig> {
  const { data } = await http.post('/admin/ai/providers', payload)
  return data.data
}

export async function updateAdminAiProvider(
  id: number,
  payload: AdminAiProviderUpsertPayload
): Promise<AdminAiProviderConfig> {
  const { data } = await http.put(`/admin/ai/providers/${id}`, payload)
  return data.data
}

export async function deleteAdminAiProvider(id: number) {
  const { data } = await http.delete(`/admin/ai/providers/${id}`)
  return data.data
}

export async function checkAdminAiProviderHealth(id: number): Promise<AdminAiProviderHealthCheck> {
  const { data } = await http.post(`/admin/ai/providers/${id}/health-check`)
  return data.data
}

export async function probeAdminAiGateway(
  payload: AdminAiGatewayProbePayload
): Promise<AdminAiGatewayProbeResponse> {
  const { data } = await http.post('/admin/ai/gateway/probe', payload, { timeout: 120000 })
  return data.data
}

export async function checkAiModeration(
  payload: AiModerationCheckPayload
): Promise<AiModerationCheckResponse> {
  const { data } = await http.post('/ai/moderation/check', payload, { timeout: 120000 })
  return data.data
}

export async function getAdminAiModels(): Promise<AdminAiModelConfig[]> {
  const { data } = await http.get('/admin/ai/models')
  return data.data || []
}

export async function createAdminAiModel(payload: AdminAiModelUpsertPayload): Promise<AdminAiModelConfig> {
  const { data } = await http.post('/admin/ai/models', payload)
  return data.data
}

export async function updateAdminAiModel(
  id: number,
  payload: AdminAiModelUpsertPayload
): Promise<AdminAiModelConfig> {
  const { data } = await http.put(`/admin/ai/models/${id}`, payload)
  return data.data
}

export async function deleteAdminAiModel(id: number) {
  const { data } = await http.delete(`/admin/ai/models/${id}`)
  return data.data
}

export async function getAdminAiGatewayRouteRules(): Promise<AdminAiGatewayRouteRule[]> {
  const { data } = await http.get('/admin/ai/route-rules')
  return data.data || []
}

export async function createAdminAiGatewayRouteRule(
  payload: AdminAiGatewayRouteRuleUpsertPayload
): Promise<AdminAiGatewayRouteRule> {
  const { data } = await http.post('/admin/ai/route-rules', payload)
  return data.data
}

export async function updateAdminAiGatewayRouteRule(
  id: number,
  payload: AdminAiGatewayRouteRuleUpsertPayload
): Promise<AdminAiGatewayRouteRule> {
  const { data } = await http.put(`/admin/ai/route-rules/${id}`, payload)
  return data.data
}

export async function deleteAdminAiGatewayRouteRule(id: number) {
  const { data } = await http.delete(`/admin/ai/route-rules/${id}`)
  return data.data
}

export async function getAdminAiGatewaySafetyRules(): Promise<AdminAiGatewaySafetyRule[]> {
  const { data } = await http.get('/admin/ai/safety-rules')
  return data.data || []
}

export async function createAdminAiGatewaySafetyRule(
  payload: AdminAiGatewaySafetyRuleUpsertPayload
): Promise<AdminAiGatewaySafetyRule> {
  const { data } = await http.post('/admin/ai/safety-rules', payload)
  return data.data
}

export async function updateAdminAiGatewaySafetyRule(
  id: number,
  payload: AdminAiGatewaySafetyRuleUpsertPayload
): Promise<AdminAiGatewaySafetyRule> {
  const { data } = await http.put(`/admin/ai/safety-rules/${id}`, payload)
  return data.data
}

export async function deleteAdminAiGatewaySafetyRule(id: number) {
  const { data } = await http.delete(`/admin/ai/safety-rules/${id}`)
  return data.data
}

export async function getAdminAiUsageQuotas(): Promise<AdminAiUsageQuota[]> {
  const { data } = await http.get('/admin/ai/quotas')
  return data.data || []
}

export async function createAdminAiUsageQuota(
  payload: AdminAiUsageQuotaUpsertPayload
): Promise<AdminAiUsageQuota> {
  const { data } = await http.post('/admin/ai/quotas', payload)
  return data.data
}

export async function updateAdminAiUsageQuota(
  id: number,
  payload: AdminAiUsageQuotaUpsertPayload
): Promise<AdminAiUsageQuota> {
  const { data } = await http.put(`/admin/ai/quotas/${id}`, payload)
  return data.data
}

export async function deleteAdminAiUsageQuota(id: number) {
  const { data } = await http.delete(`/admin/ai/quotas/${id}`)
  return data.data
}

export async function getAdminAiUsageTrend(days = 14): Promise<AdminAiUsageTrendItem[]> {
  const { data } = await http.get('/admin/ai/stats/usage-trend', { params: { days } })
  return data.data || []
}

export async function getAdminAiModelRanking(limit = 10): Promise<AdminAiModelRankingItem[]> {
  const { data } = await http.get('/admin/ai/stats/model-ranking', { params: { limit } })
  return data.data || []
}

export async function getAdminAiCapabilities(): Promise<AdminAiCapabilityConfig[]> {
  const { data } = await http.get('/admin/ai/capabilities')
  return data.data || []
}

export async function createAdminAiCapability(
  payload: AdminAiCapabilityUpsertPayload
): Promise<AdminAiCapabilityConfig> {
  const { data } = await http.post('/admin/ai/capabilities', payload)
  return data.data
}

export async function updateAdminAiCapability(
  id: number,
  payload: AdminAiCapabilityUpsertPayload
): Promise<AdminAiCapabilityConfig> {
  const { data } = await http.put(`/admin/ai/capabilities/${id}`, payload)
  return data.data
}

export async function deleteAdminAiCapability(id: number) {
  const { data } = await http.delete(`/admin/ai/capabilities/${id}`)
  return data.data
}

export async function getAdminAiScenes(): Promise<AdminAiSceneConfig[]> {
  const { data } = await http.get('/admin/ai/scenes')
  return data.data || []
}

export async function createAdminAiScene(payload: AdminAiSceneUpsertPayload): Promise<AdminAiSceneConfig> {
  const { data } = await http.post('/admin/ai/scenes', payload)
  return data.data
}

export async function updateAdminAiScene(
  id: number,
  payload: AdminAiSceneUpsertPayload
): Promise<AdminAiSceneConfig> {
  const { data } = await http.put(`/admin/ai/scenes/${id}`, payload)
  return data.data
}

export async function deleteAdminAiScene(id: number) {
  const { data } = await http.delete(`/admin/ai/scenes/${id}`)
  return data.data
}

export async function getAdminAiOpsSnapshot(): Promise<AdminAiOpsSnapshot> {
  const { data } = await http.get('/admin/ai/ops/snapshot')
  return data.data
}

export async function getAdminAiOpsAlerts(): Promise<AdminAiOpsAlert[]> {
  const { data } = await http.get('/admin/ai/ops/alerts')
  return data.data || []
}

export async function getAdminAiOpsLogTopics(): Promise<AdminAiOpsLogTopic[]> {
  const { data } = await http.get('/admin/ai/ops/log-topics')
  return data.data || []
}

export async function getAdminAiOpsLogs(params?: {
  region?: string
  topic?: string
  query?: string
  limit?: number
}): Promise<AdminAiOpsLogEntry[]> {
  const { data } = await http.get('/admin/ai/ops/logs', { params })
  return data.data || []
}

export async function analyzeAdminAiOps(
  payload: AdminAiOpsAnalyzePayload
): Promise<AdminAiOpsAnalysisResponse> {
  const { data } = await http.post('/admin/ai/ops/analyze', payload, { timeout: 120000 })
  return data.data
}
