import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { createAuthGuard, saveLoginState, clearLoginState } from '../shared/router/auth'
import MainLayout from '../components/layout/MainLayout.vue'

const LoginPage = () => import('../views/auth/LoginPage.vue')
const RegisterPage = () => import('../views/auth/RegisterPage.vue')
const DiscoverPage = () => import('../views/home/DiscoverPage.vue')
const WelfarePage = () => import('../views/welfare/WelfarePage.vue')
const MessagesPage = () => import('../views/message/MessagesPage.vue')
const ActivityPage = () => import('../views/message/ActivityPage.vue')
const AddFriendPage = () => import('../views/message/AddFriendPage.vue')
const SystemNotificationsPage = () => import('../views/message/SystemNotificationsPage.vue')
const GroupBuyDetailPage = () => import('../views/order/GroupBuyDetail.vue')
const GroupBuyOrder = () => import('../views/order/GroupBuyOrder.vue')
const CartPage = () => import('../views/order/CartPage.vue')
const HotPage = () => import('../views/home/HotPage.vue')
const PostDetailPage = () => import('../views/post/PostDetail.vue')
const EditProfilePage = () => import('../views/user/EditProfilePage.vue')
const PostWritePage = () => import('../views/post/PostWritePage.vue')
const StudentAuthPage = () => import('../views/auth/StudentAuthPage.vue')
const SearchPage = () => import('../views/post/SearchPage.vue')
const DraftPage = () => import('../views/post/DraftPage.vue')
const ChatDetailPage = () => import('../views/message/ChatDetailPage.vue')
const UserPostsPage = () => import('../views/user/UserPostsPage.vue')
const FollowListPage = () => import('../views/user/FollowListPage.vue')
const FavoritePostsPage = () => import('../views/user/FavoritePostsPage.vue')
const OrderListPage = () => import('../views/order/OrderList.vue')
const OrderDetailPage = () => import('../views/order/OrderDetail.vue')
const RefundApplyPage = () => import('../views/payment/RefundApplyPage.vue')
const RefundResultPage = () => import('../views/payment/RefundResultPage.vue')
const RefundListPage = () => import('../views/payment/RefundListPage.vue')
const RefundDetailPage = () => import('../views/payment/RefundDetailPage.vue')
const AccountCenter = () => import('../views/account/AccountCenter.vue')

// 个人中心页面组件 - 懒加载方式导入
const ProfilePage = () => import('../views/user/ProfilePage.vue')
// 其他用户主页组件
const UserProfilePage = () => import('../views/user/UserProfile.vue')
// 浏览记录页面组件 - 懒加载方式导入
const HistoryPage = () => import('../views/user/HistoryPage.vue')

const routes: Array<RouteRecordRaw> = [
  // 公共页面
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterPage,
    meta: { requiresAuth: false }
  },
  {
    path: '/oauth/wechat/callback',
    name: 'WechatCallback',
    component: () => import('../views/auth/OAuthCallback.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/oauth/qq/callback',
    name: 'QQCallback',
    component: () => import('../views/auth/OAuthCallback.vue'),
    meta: { requiresAuth: false }
  },
  // 主要导航页面 - 使用 MainLayout 布局
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      // 根路径重定向到首页
      {
        path: '',
        redirect: { name: 'Home' }
      },
      {
        path: 'home',
        name: 'Home',
        component: DiscoverPage
      },
      {
        path: 'follow',
        name: 'Follow',
        component: () => import('../views/user/FollowPage.vue')
      },
      {
        path: 'hot',
        name: 'Hot',
        component: HotPage
      },
      {
        path: 'welfare',
        name: 'Welfare',
        component: WelfarePage
      },
      {
        path: 'message',
        name: 'Message',
        component: MessagesPage
      },
      {
        path: 'message/likes',
        name: 'Likes',
        component: () => import('../views/message/LikesPage.vue')
      },
      {
        path: 'message/follows',
        name: 'Follows',
        component: () => import('../views/message/FollowsPage.vue')
      },
      {
        path: 'message/comments',
        name: 'Comments',
        component: () => import('../views/message/CommentsPage.vue')
      },
      {
        path: 'message/system',
        name: 'SystemNotifications',
        component: SystemNotificationsPage
      },
      {
        path: 'agent',
        name: 'AgentList',
        component: () => import('../views/agent/AgentListPage.vue')
      },
      {
        path: 'me',
        name: 'Me',
        component: ProfilePage
      }
    ]
  },

  // 次要页面
  {
    path: '/activity',
    name: 'Activity',
    component: ActivityPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/add-friend',
    name: 'AddFriend',
    component: AddFriendPage,
    meta: { requiresAuth: true }
  },
  // 隐私设置页面 - 添加好友页面的设置按钮指向这里
  {
    path: '/privacy-settings',
    name: 'PrivacySettings',
    component: () => import('../views/settings/SettingsPage.vue'),
    meta: { requiresAuth: true }
  },
  // 通用设置页面 - 左侧边栏的设置按钮指向这里
  {
    path: '/general-settings',
    name: 'GeneralSettings',
    component: () => import('../views/settings/GeneralSettingsPage.vue'),
    meta: { requiresAuth: true }
  },
  // 团购详情页面
  {
    path: '/group-buy/:id',
    name: 'GroupBuyDetail',
    component: GroupBuyDetailPage,
    props: true,
    meta: { requiresAuth: true }
  },
  // 团购订单页面
  {
    path: '/group-buy-order',
    name: 'GroupBuyOrder',
    component: GroupBuyOrder,
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: CartPage,
    meta: { requiresAuth: true }
  },
  // 帖子详情页面 - 公开访问
  {
    path: '/posts/:id',
    name: 'PostDetail',
    component: PostDetailPage,
    props: true,
    meta: { requiresAuth: false }
  },
  // 写帖子页面
  {
    path: '/posts/new',
    name: 'PostWrite',
    component: PostWritePage,
    meta: { requiresAuth: true }
  },
  {
    path: '/search',
    name: 'Search',
    component: SearchPage,
    meta: { requiresAuth: true }
  },
  // 编辑资料页面
  {
    path: '/me/edit',
    name: 'EditProfile',
    component: EditProfilePage,
    meta: { requiresAuth: true }
  },
  // 学生身份认证页面
  {
    path: '/auth/student',
    name: 'StudentAuth',
    component: StudentAuthPage,
    meta: { requiresAuth: true }
  },
  // 商家认证页面
  {
    path: '/auth/merchant',
    name: 'MerchantAuth',
    component: () => import('../views/auth/MerchantAuthPage.vue'),
    meta: { requiresAuth: true }
  },
  // 其他用户主页页面 - 公开访问
  {
    path: '/user/:id',
    name: 'UserProfile',
    component: UserProfilePage,
    props: true,
    meta: { requiresAuth: false }
  },
  {
    path: '/me/follow',
    name: 'FollowList',
    component: FollowListPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/user/:userId/follow',
    name: 'UserFollowList',
    component: FollowListPage,
    meta: { requiresAuth: true }
  },
  // 订单列表页面
  {
    path: '/me/orders',
    name: 'Orders',
    component: OrderListPage,
    meta: { requiresAuth: true }
  },
  // 订单详情页面
  {
    path: '/me/orders/:id',
    name: 'OrderDetail',
    component: OrderDetailPage,
    props: true,
    meta: { requiresAuth: true }
  },
  // 浏览记录页面
  {
    path: '/history',
    name: 'History',
    component: HistoryPage,
    meta: { requiresAuth: true }
  },
  // 本地草稿页面
  {
    path: '/drafts',
    name: 'Drafts',
    component: DraftPage,
    meta: { requiresAuth: true }
  },
  // 聊天详情页面
  {
    path: '/messages/:id',
    name: 'ChatDetail',
    component: ChatDetailPage,
    props: true,
    meta: { requiresAuth: true }
  },
  // 用户发布内容页面
  {
    path: '/me/posts',
    name: 'UserPosts',
    component: UserPostsPage,
    meta: { requiresAuth: true }
  },
  // 用户收藏页面
  {
    path: '/me/favorites',
    name: 'FavoritePosts',
    component: FavoritePostsPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/agent/knowledge-bases',
    name: 'KnowledgeBaseList',
    component: () => import('../views/agent/KnowledgeBaseListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/agent/knowledge-bases/:id',
    name: 'KnowledgeBaseDetail',
    component: () => import('../views/agent/KnowledgeBaseDetailPage.vue'),
    meta: { requiresAuth: true }
  },
  // 商家优惠券管理页面
  {
    path: '/merchant',
    component: () => import('../views/merchant/MerchantLayout.vue'),
    meta: { requiresAuth: true, requiresMerchant: true },
    children: [
      {
        path: '',
        redirect: { name: 'MerchantHome' }
      },
      {
        path: 'home',
        name: 'MerchantHome',
        component: () => import('../views/merchant/MerchantHomePage.vue')
      },
      {
        path: 'vouchers',
        name: 'MerchantVoucherManage',
        component: () => import('../views/merchant/MerchantVoucherManage.vue')
      },
      {
        path: 'refunds',
        name: 'MerchantRefundManage',
        component: () => import('../views/merchant/MerchantRefundReviewPage.vue')
      },
      {
        path: 'ai',
        name: 'MerchantAI',
        component: () => import('../views/merchant/MerchantAIPage.vue')
      },
      {
        path: 'ai/agent',
        name: 'MerchantAgentList',
        component: () => import('../views/agent/AgentListPage.vue')
      },
      {
        path: 'ai/agent/chat',
        alias: ['/merchant/ai/chat'],
        name: 'MerchantAgentChat',
        component: () => import('../views/agent/AgentChatPage.vue')
      },
      {
        path: 'ai/agent/knowledge-bases',
        alias: ['/merchant/ai/knowledge-bases'],
        name: 'MerchantKnowledgeBaseList',
        component: () => import('../views/agent/KnowledgeBaseListPage.vue')
      },
      {
        path: 'ai/agent/knowledge-bases/:id',
        alias: ['/merchant/ai/knowledge-bases/:id'],
        name: 'MerchantKnowledgeBaseDetail',
        component: () => import('../views/agent/KnowledgeBaseDetailPage.vue')
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
  // 智能体聊天页面
  {
    path: '/agent/chat',
    name: 'AgentChat',
    component: () => import('../views/agent/AgentChatPage.vue'),
    meta: { requiresAuth: true }
  },
  // 支付页面
  {
    path: '/payment',
    name: 'Payment',
    component: () => import('../views/payment/PaymentPage.vue'),
    meta: { requiresAuth: true }
  },
  // 钱包页面
  {
    path: '/wallet',
    name: 'Wallet',
    component: () => import('../views/wallet/WalletPage.vue'),
    meta: { requiresAuth: true }
  },
  // 支付成功页面
  {
    path: '/payment/success',
    name: 'PaymentSuccess',
    component: () => import('../views/payment/PaymentSuccessPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/payment/refund',
    name: 'RefundApply',
    component: RefundApplyPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/payment/refund/result',
    name: 'RefundResult',
    component: RefundResultPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/me/refunds',
    name: 'RefundList',
    component: RefundListPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/payment/refunds/:refundNo',
    name: 'RefundDetail',
    component: RefundDetailPage,
    props: true,
    meta: { requiresAuth: true }
  },
  // 支付失败页面
  {
    path: '/payment/failed',
    name: 'PaymentFailed',
    component: () => import('../views/payment/PaymentFailedPage.vue'),
    meta: { requiresAuth: true }
  },
  // 管理后台页面 - 使用统一布局
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: { name: 'AdminDashboard' }
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/admin/AdminDashboard.vue')
      },
      {
        path: 'home',
        name: 'AdminHome',
        component: () => import('../views/admin/AdminHome.vue')
      },
      {
        path: 'student-auth',
        name: 'AdminStudentAuth',
        component: () => import('../views/admin/AdminStudentAuth.vue')
      },
      {
        path: 'merchant-auth',
        name: 'AdminMerchantAuth',
        component: () => import('../views/admin/AdminMerchantAuth.vue')
      },
      {
        path: 'reports',
        name: 'AdminReports',
        component: () => import('../views/admin/AdminReports.vue')
      },
      {
        path: 'voucher-orders',
        name: 'AdminVoucherOrders',
        component: () => import('../views/admin/AdminVoucherOrders.vue')
      },
      {
        path: 'refunds',
        name: 'AdminRefundManage',
        component: () => import('../views/admin/AdminRefundManage.vue')
      },
      {
        path: 'payment-reconciliation',
        name: 'AdminPaymentReconciliation',
        component: () => import('../views/admin/AdminPaymentReconciliation.vue')
      },
      {
        path: 'posts',
        name: 'AdminPostManage',
        component: () => import('../views/admin/AdminPostManage.vue')
      },
      {
        path: 'merchants',
        name: 'AdminMerchantManage',
        component: () => import('../views/admin/AdminShopManage.vue')
      },
      {
        path: 'vouchers',
        name: 'AdminVoucherManage',
        component: () => import('../views/admin/AdminVoucherManage.vue')
      },
      {
        path: 'comments',
        name: 'AdminCommentManage',
        component: () => import('../views/admin/AdminCommentManage.vue')
      },
      {
        path: 'members',
        name: 'AdminMemberManage',
        component: () => import('../views/admin/AdminMemberManage.vue')
      },
      {
        path: 'member-stats',
        name: 'AdminMemberStats',
        component: () => import('../views/admin/AdminMemberStats.vue')
      },
      {
        path: 'ai',
        name: 'AdminAI',
        component: () => import('../views/admin/AdminAI.vue')
      },
      {
        path: 'logs/member-login',
        name: 'AdminMemberLoginLog',
        component: () => import('../views/admin/AdminMemberLoginLog.vue')
      },
      {
        path: 'logs/admin-login',
        name: 'AdminAdminLoginLog',
        component: () => import('../views/admin/AdminAdminLoginLog.vue')
      },
      {
        path: 'logs/admin-operation',
        name: 'AdminOperationLog',
        component: () => import('../views/admin/AdminOperationLog.vue')
      },
      {
        path: 'logs/audit',
        name: 'AdminAuditLog',
        component: () => import('../views/admin/AdminAuditLog.vue')
      },
      {
        path: 'monitor/online-users',
        name: 'AdminOnlineUsers',
        component: () => import('../views/admin/AdminOnlineUsers.vue')
      },
      {
        path: 'monitor/cache',
        name: 'AdminCache',
        component: () => import('../views/admin/AdminCache.vue')
      },
      {
        path: 'tools/apis',
        name: 'AdminAPIs',
        component: () => import('../views/admin/AdminAPIs.vue')
      },
      {
        path: 'system/users',
        name: 'AdminUserManage',
        component: () => import('../views/admin/AdminUserManage.vue')
      },
      {
        path: 'profile/info',
        name: 'AdminProfileInfo',
        component: AccountCenter
      },
      {
        path: 'profile/settings',
        name: 'AdminProfileSettings',
        redirect: { name: 'AdminProfileInfo', query: { panel: 'edit' } }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(createAuthGuard('Home'))

export default router
export { saveLoginState, clearLoginState }
