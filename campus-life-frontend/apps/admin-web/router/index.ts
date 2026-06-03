import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { createAuthGuard } from '../../../src/shared/router/auth'

const LoginPage = () => import('../../../src/views/auth/LoginPage.vue')
const RegisterPage = () => import('../../../src/views/auth/RegisterPage.vue')
const OAuthCallback = () => import('../../../src/views/auth/OAuthCallback.vue')
const AccountCenter = () => import('../../../src/views/account/AccountCenter.vue')

const routes: Array<RouteRecordRaw> = [
  { path: '/', redirect: '/admin/dashboard' },
  { path: '/home', redirect: '/admin/dashboard' },
  { path: '/login', name: 'Login', component: LoginPage, meta: { requiresAuth: false } },
  { path: '/register', name: 'Register', component: RegisterPage, meta: { requiresAuth: false } },
  { path: '/oauth/wechat/callback', name: 'WechatCallback', component: OAuthCallback, meta: { requiresAuth: false } },
  { path: '/oauth/qq/callback', name: 'QQCallback', component: OAuthCallback, meta: { requiresAuth: false } },
  {
    path: '/admin',
    component: () => import('../../../src/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: { name: 'AdminDashboard' } },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../../../src/views/admin/AdminDashboard.vue') },
      { path: 'home', name: 'AdminHome', component: () => import('../../../src/views/admin/AdminHome.vue') },
      { path: 'student-auth', name: 'AdminStudentAuth', component: () => import('../../../src/views/admin/AdminStudentAuth.vue') },
      { path: 'merchant-auth', name: 'AdminMerchantAuth', component: () => import('../../../src/views/admin/AdminMerchantAuth.vue') },
      { path: 'reports', name: 'AdminReports', component: () => import('../../../src/views/admin/AdminReports.vue') },
      { path: 'voucher-orders', name: 'AdminVoucherOrders', component: () => import('../../../src/views/admin/AdminVoucherOrders.vue') },
      { path: 'refunds', name: 'AdminRefundManage', component: () => import('../../../src/views/admin/AdminRefundManage.vue') },
      { path: 'payment-reconciliation', name: 'AdminPaymentReconciliation', component: () => import('../../../src/views/admin/AdminPaymentReconciliation.vue') },
      { path: 'posts', name: 'AdminPostManage', component: () => import('../../../src/views/admin/AdminPostManage.vue') },
      { path: 'merchants', name: 'AdminMerchantManage', component: () => import('../../../src/views/admin/AdminShopManage.vue') },
      { path: 'vouchers', name: 'AdminVoucherManage', component: () => import('../../../src/views/admin/AdminVoucherManage.vue') },
      { path: 'comments', name: 'AdminCommentManage', component: () => import('../../../src/views/admin/AdminCommentManage.vue') },
      { path: 'members', name: 'AdminMemberManage', component: () => import('../../../src/views/admin/AdminMemberManage.vue') },
      { path: 'member-stats', name: 'AdminMemberStats', component: () => import('../../../src/views/admin/AdminMemberStats.vue') },
      { path: 'ai', name: 'AdminAI', component: () => import('../../../src/views/admin/AdminAI.vue') },
      { path: 'ai/agents', name: 'AdminAgentHub', component: () => import('../../../src/views/admin/AdminAgentHub.vue') },
      { path: 'ai/agent', name: 'AdminAgentList', component: () => import('../../../src/views/agent/AgentListPage.vue') },
      { path: 'ai/agent/chat', name: 'AdminAgentChat', component: () => import('../../../src/views/agent/AgentChatPage.vue') },
      { path: 'ai/agent/knowledge-bases', name: 'AdminKnowledgeBaseList', component: () => import('../../../src/views/agent/KnowledgeBaseListPage.vue') },
      { path: 'ai/agent/knowledge-bases/:id', name: 'AdminKnowledgeBaseDetail', component: () => import('../../../src/views/agent/KnowledgeBaseDetailPage.vue') },
      { path: 'logs/member-login', name: 'AdminMemberLoginLog', component: () => import('../../../src/views/admin/AdminMemberLoginLog.vue') },
      { path: 'logs/admin-login', name: 'AdminAdminLoginLog', component: () => import('../../../src/views/admin/AdminAdminLoginLog.vue') },
      { path: 'logs/admin-operation', name: 'AdminOperationLog', component: () => import('../../../src/views/admin/AdminOperationLog.vue') },
      { path: 'logs/audit', name: 'AdminAuditLog', component: () => import('../../../src/views/admin/AdminAuditLog.vue') },
      { path: 'monitor/online-users', name: 'AdminOnlineUsers', component: () => import('../../../src/views/admin/AdminOnlineUsers.vue') },
      { path: 'monitor/cache', name: 'AdminCache', component: () => import('../../../src/views/admin/AdminCache.vue') },
      { path: 'tools/apis', name: 'AdminAPIs', component: () => import('../../../src/views/admin/AdminAPIs.vue') },
      { path: 'system/users', name: 'AdminUserManage', component: () => import('../../../src/views/admin/AdminUserManage.vue') },
      { path: 'profile/info', name: 'AdminProfileInfo', component: AccountCenter },
      { path: 'profile/settings', name: 'AdminProfileSettings', redirect: { name: 'AdminProfileInfo', query: { panel: 'edit' } } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/admin/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(createAuthGuard('AdminDashboard', 'ADMIN'))

export default router
