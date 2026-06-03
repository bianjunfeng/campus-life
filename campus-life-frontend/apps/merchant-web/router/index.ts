import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { createAuthGuard } from '../../../src/shared/router/auth'

const LoginPage = () => import('../../../src/views/auth/LoginPage.vue')
const RegisterPage = () => import('../../../src/views/auth/RegisterPage.vue')
const OAuthCallback = () => import('../../../src/views/auth/OAuthCallback.vue')
const MerchantLayout = () => import('../../../src/views/merchant/MerchantLayout.vue')
const MerchantHomePage = () => import('../../../src/views/merchant/MerchantHomePage.vue')
const MerchantAIPage = () => import('../../../src/views/merchant/MerchantAIPage.vue')
const AgentListPage = () => import('../../../src/views/agent/AgentListPage.vue')
const AgentChatPage = () => import('../../../src/views/agent/AgentChatPage.vue')
const KnowledgeBaseListPage = () => import('../../../src/views/agent/KnowledgeBaseListPage.vue')
const KnowledgeBaseDetailPage = () => import('../../../src/views/agent/KnowledgeBaseDetailPage.vue')
const AccountCenter = () => import('../../../src/views/account/AccountCenter.vue')

const routes: Array<RouteRecordRaw> = [
  { path: '/', redirect: '/merchant/home' },
  { path: '/login', name: 'Login', component: LoginPage, meta: { requiresAuth: false } },
  { path: '/register', name: 'Register', component: RegisterPage, meta: { requiresAuth: false } },
  { path: '/oauth/wechat/callback', name: 'WechatCallback', component: OAuthCallback, meta: { requiresAuth: false } },
  { path: '/oauth/qq/callback', name: 'QQCallback', component: OAuthCallback, meta: { requiresAuth: false } },
  {
    path: '/merchant',
    component: MerchantLayout,
    meta: { requiresAuth: true, requiresMerchant: true },
    children: [
      { path: '', redirect: { name: 'MerchantHome' } },
      {
        path: 'home',
        name: 'MerchantHome',
        component: MerchantHomePage
      },
      {
        path: 'vouchers',
        name: 'MerchantVoucherManage',
        component: () => import('../../../src/views/merchant/MerchantVoucherManage.vue')
      },
      {
        path: 'refunds',
        name: 'MerchantRefundManage',
        component: () => import('../../../src/views/merchant/MerchantRefundReviewPage.vue')
      },
      {
        path: 'ai',
        name: 'MerchantAI',
        component: MerchantAIPage
      },
      {
        path: 'ai/agent',
        name: 'MerchantAgentList',
        component: AgentListPage
      },
      {
        path: 'ai/agent/chat',
        alias: ['/merchant/ai/chat'],
        name: 'MerchantAgentChat',
        component: AgentChatPage
      },
      {
        path: 'ai/agent/knowledge-bases',
        alias: ['/merchant/ai/knowledge-bases'],
        name: 'MerchantKnowledgeBaseList',
        component: KnowledgeBaseListPage
      },
      {
        path: 'ai/agent/knowledge-bases/:id',
        alias: ['/merchant/ai/knowledge-bases/:id'],
        name: 'MerchantKnowledgeBaseDetail',
        component: KnowledgeBaseDetailPage
      },
      {
        path: 'profile/info',
        name: 'MerchantProfileInfo',
        component: AccountCenter
      },
      {
        path: 'profile/settings',
        name: 'MerchantProfileSettings',
        redirect: { name: 'MerchantProfileInfo', query: { panel: 'edit' } }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/merchant/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(createAuthGuard('MerchantHome', 'MERCHANT'))

export default router
