import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { createAuthGuard } from '../../../src/shared/router/auth'
import MainLayout from '../../../src/components/layout/MainLayout.vue'

const LoginPage = () => import('../../../src/views/auth/LoginPage.vue')
const RegisterPage = () => import('../../../src/views/auth/RegisterPage.vue')
const DiscoverPage = () => import('../../../src/views/home/DiscoverPage.vue')
const WelfarePage = () => import('../../../src/views/welfare/WelfarePage.vue')
const MessagesPage = () => import('../../../src/views/message/MessagesPage.vue')
const ActivityPage = () => import('../../../src/views/message/ActivityPage.vue')
const AddFriendPage = () => import('../../../src/views/message/AddFriendPage.vue')
const SystemNotificationsPage = () => import('../../../src/views/message/SystemNotificationsPage.vue')
const GroupBuyDetailPage = () => import('../../../src/views/order/GroupBuyDetail.vue')
const GroupBuyOrder = () => import('../../../src/views/order/GroupBuyOrder.vue')
const CartPage = () => import('../../../src/views/order/CartPage.vue')
const HotPage = () => import('../../../src/views/home/HotPage.vue')
const PostDetailPage = () => import('../../../src/views/post/PostDetail.vue')
const EditProfilePage = () => import('../../../src/views/user/EditProfilePage.vue')
const PostWritePage = () => import('../../../src/views/post/PostWritePage.vue')
const StudentAuthPage = () => import('../../../src/views/auth/StudentAuthPage.vue')
const SearchPage = () => import('../../../src/views/post/SearchPage.vue')
const DraftPage = () => import('../../../src/views/post/DraftPage.vue')
const ChatDetailPage = () => import('../../../src/views/message/ChatDetailPage.vue')
const UserPostsPage = () => import('../../../src/views/user/UserPostsPage.vue')
const FollowListPage = () => import('../../../src/views/user/FollowListPage.vue')
const FavoritePostsPage = () => import('../../../src/views/user/FavoritePostsPage.vue')
const OrderListPage = () => import('../../../src/views/order/OrderList.vue')
const OrderDetailPage = () => import('../../../src/views/order/OrderDetail.vue')
const RefundApplyPage = () => import('../../../src/views/payment/RefundApplyPage.vue')
const RefundResultPage = () => import('../../../src/views/payment/RefundResultPage.vue')
const RefundListPage = () => import('../../../src/views/payment/RefundListPage.vue')
const RefundDetailPage = () => import('../../../src/views/payment/RefundDetailPage.vue')
const ProfilePage = () => import('../../../src/views/user/ProfilePage.vue')
const UserProfilePage = () => import('../../../src/views/user/UserProfile.vue')
const HistoryPage = () => import('../../../src/views/user/HistoryPage.vue')

const routes: Array<RouteRecordRaw> = [
  { path: '/login', name: 'Login', component: LoginPage, meta: { requiresAuth: false } },
  { path: '/register', name: 'Register', component: RegisterPage, meta: { requiresAuth: false } },
  { path: '/oauth/wechat/callback', name: 'WechatCallback', component: () => import('../../../src/views/auth/OAuthCallback.vue'), meta: { requiresAuth: false } },
  { path: '/oauth/qq/callback', name: 'QQCallback', component: () => import('../../../src/views/auth/OAuthCallback.vue'), meta: { requiresAuth: false } },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: { name: 'Home' } },
      { path: 'home', name: 'Home', component: DiscoverPage },
      { path: 'follow', name: 'Follow', component: () => import('../../../src/views/user/FollowPage.vue'), meta: { requiresStudent: true } },
      { path: 'hot', name: 'Hot', component: HotPage },
      { path: 'welfare', name: 'Welfare', component: WelfarePage },
      { path: 'message', name: 'Message', component: MessagesPage, meta: { requiresStudent: true } },
      { path: 'message/likes', name: 'Likes', component: () => import('../../../src/views/message/LikesPage.vue'), meta: { requiresStudent: true } },
      { path: 'message/follows', name: 'Follows', component: () => import('../../../src/views/message/FollowsPage.vue'), meta: { requiresStudent: true } },
      { path: 'message/comments', name: 'Comments', component: () => import('../../../src/views/message/CommentsPage.vue'), meta: { requiresStudent: true } },
      { path: 'message/system', name: 'SystemNotifications', component: SystemNotificationsPage, meta: { requiresStudent: true } },
      { path: 'agent', name: 'AgentList', component: () => import('../../../src/views/agent/AgentListPage.vue'), meta: { requiresStudent: true } },
      { path: 'me', name: 'Me', component: ProfilePage, meta: { requiresStudent: true } }
    ]
  },
  { path: '/activity', name: 'Activity', component: ActivityPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/add-friend', name: 'AddFriend', component: AddFriendPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/privacy-settings', name: 'PrivacySettings', component: () => import('../../../src/views/settings/SettingsPage.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/general-settings', name: 'GeneralSettings', component: () => import('../../../src/views/settings/GeneralSettingsPage.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/group-buy/:id', name: 'GroupBuyDetail', component: GroupBuyDetailPage, props: true, meta: { requiresAuth: true } },
  { path: '/group-buy-order', name: 'GroupBuyOrder', component: GroupBuyOrder, props: true, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/cart', name: 'Cart', component: CartPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/posts/:id', name: 'PostDetail', component: PostDetailPage, props: true, meta: { requiresAuth: false } },
  { path: '/posts/new', name: 'PostWrite', component: PostWritePage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/search', name: 'Search', component: SearchPage, meta: { requiresAuth: true } },
  { path: '/me/edit', name: 'EditProfile', component: EditProfilePage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/auth/student', name: 'StudentAuth', component: StudentAuthPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/auth/merchant', name: 'MerchantAuth', component: () => import('../../../src/views/auth/MerchantAuthPage.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/user/:id', name: 'UserProfile', component: UserProfilePage, props: true, meta: { requiresAuth: false } },
  { path: '/me/follow', name: 'FollowList', component: FollowListPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/user/:userId/follow', name: 'UserFollowList', component: FollowListPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/me/orders', name: 'Orders', component: OrderListPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/me/orders/:id', name: 'OrderDetail', component: OrderDetailPage, props: true, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/history', name: 'History', component: HistoryPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/drafts', name: 'Drafts', component: DraftPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/messages/:id', name: 'ChatDetail', component: ChatDetailPage, props: true, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/me/posts', name: 'UserPosts', component: UserPostsPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/me/favorites', name: 'FavoritePosts', component: FavoritePostsPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/agent/knowledge-bases', name: 'KnowledgeBaseList', component: () => import('../../../src/views/agent/KnowledgeBaseListPage.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/agent/knowledge-bases/:id', name: 'KnowledgeBaseDetail', component: () => import('../../../src/views/agent/KnowledgeBaseDetailPage.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/agent/chat', name: 'AgentChat', component: () => import('../../../src/views/agent/AgentChatPage.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/payment', name: 'Payment', component: () => import('../../../src/views/payment/PaymentPage.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/wallet', name: 'Wallet', component: () => import('../../../src/views/wallet/WalletPage.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/payment/success', name: 'PaymentSuccess', component: () => import('../../../src/views/payment/PaymentSuccessPage.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/payment/refund', name: 'RefundApply', component: RefundApplyPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/payment/refund/result', name: 'RefundResult', component: RefundResultPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/me/refunds', name: 'RefundList', component: RefundListPage, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/payment/refunds/:refundNo', name: 'RefundDetail', component: RefundDetailPage, props: true, meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/payment/failed', name: 'PaymentFailed', component: () => import('../../../src/views/payment/PaymentFailedPage.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(createAuthGuard('Home'))

export default router
