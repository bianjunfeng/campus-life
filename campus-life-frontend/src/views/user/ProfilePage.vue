<template>
  <div class="profile-page">
    <!-- 通用页面头部 -->
    <HeaderBar 
      v-if="showHeaderBar"
      :show-search="false"
      :show-sidebar-toggle="true"
      :show-message-button="false"
      title="个人中心"
      @toggle-sidebar="handleToggleSidebar"
      @search="handleSearch"
    />
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <!-- 用户信息头部 -->
    <div v-else class="profile-header">
      <div class="avatar-container">
        <img :src="userInfo.icon || 'https://via.placeholder.com/100x100?text=User'" :alt="userInfo.nickName || '用户'" class="avatar">
      </div>
      <div class="user-info">
        <h2 class="username">{{ userInfo.nickName || '用户' }}</h2>
        <p class="user-bio">{{ userInfo.bio || '这个人很懒，什么都没有留下' }}</p>
        <button class="edit-profile-btn" @click="navigateToEditProfile">编辑资料</button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div v-if="!loading" class="stats-container">
      <div class="stat-item" @click="navigateToUserPosts">
        <span class="stat-number">{{ stats.postCount }}</span>
        <span class="stat-label">发布</span>
      </div>
      <div class="stat-item" @click="navigateToFollowing">
        <span class="stat-number">{{ stats.followingCount }}</span>
        <span class="stat-label">关注</span>
      </div>
      <div class="stat-item" @click="navigateToFollowers">
        <span class="stat-number">{{ stats.followerCount }}</span>
        <span class="stat-label">粉丝</span>
      </div>
    </div>

    <div v-if="!loading" class="immutable-section">
      <div class="immutable-title">认证与基础信息（不可修改）</div>
      <div class="immutable-item">
        <span class="immutable-label">手机号</span>
        <span class="immutable-value">{{ userInfo.phone || '-' }}</span>
      </div>
      <div class="immutable-item">
        <span class="immutable-label">认证类型</span>
        <span class="immutable-value">{{ authInfo.typeText }}</span>
      </div>
      <div class="immutable-item" v-if="authInfo.type === 'student'">
        <span class="immutable-label">学校/学号</span>
        <span class="immutable-value">{{ authInfo.school || '-' }} / {{ authInfo.studentNo || '-' }}</span>
      </div>
      <div class="immutable-item" v-if="authInfo.type === 'merchant'">
        <span class="immutable-label">商家名称</span>
        <span class="immutable-value">{{ authInfo.merchantName || '-' }}</span>
      </div>
      <div class="immutable-item" v-if="authInfo.statusText">
        <span class="immutable-label">认证状态</span>
        <span class="immutable-value">{{ authInfo.statusText }}</span>
      </div>
    </div>

    <div v-if="!loading" class="cart-section" @click="navigateToCart">
      <div class="cart-header">
        <div class="cart-title-wrap">
          <h3 class="cart-title">购物车</h3>
          <span class="cart-count">{{ cartSummary.count }} 件商品</span>
        </div>
        <span class="cart-action">查看全部</span>
      </div>
      <div v-if="cartSummary.count > 0" class="cart-preview-list">
        <div v-for="item in cartPreview" :key="item.id" class="cart-preview-item">
          <img :src="item.image || 'https://via.placeholder.com/64x64?text=商品'" :alt="item.title || '商品'" class="cart-preview-image">
          <div class="cart-preview-info">
            <p class="cart-preview-title">{{ item.title || '未命名商品' }}</p>
            <p class="cart-preview-sub">x{{ item.quantity || 1 }} · ¥{{ Number(item.price || 0).toFixed(2) }}</p>
          </div>
        </div>
      </div>
      <div v-else class="cart-empty">购物车暂无商品，去福利页挑选吧</div>
      <div class="cart-footer">
        <span>合计</span>
        <strong>¥{{ cartSummary.amount.toFixed(2) }}</strong>
      </div>
    </div>

    <!-- 功能菜单 -->
    <div class="menu-section">
      <div class="menu-item" @click="navigateToMessage">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
          </svg>
        </div>
        <span class="menu-text">我的消息</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <div class="menu-item" @click="navigateToOrders">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="16" rx="2" ry="2"></rect>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
        </div>
        <span class="menu-text">我的订单</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <div class="menu-item" @click="navigateToRefunds">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 7h13a4 4 0 0 1 0 8H8"></path>
            <path d="M7 11l-4 4 4 4"></path>
            <path d="M14 11h3"></path>
          </svg>
        </div>
        <span class="menu-text">退款记录</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <div class="menu-item" @click="navigateToFavorites">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
          </svg>
        </div>
        <span class="menu-text">我的收藏</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <div class="menu-item" @click="navigateToHistory">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
            <line x1="3" y1="3" x2="21" y2="21"></line>
          </svg>
        </div>
        <span class="menu-text">浏览历史</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <div class="menu-item" @click="navigateToDrafts">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
        </div>
        <span class="menu-text">我的草稿</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <div class="menu-item" @click="navigateToSettings">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="16" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12.01" y2="8"></line>
          </svg>
        </div>
        <span class="menu-text">设置</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <!-- 学生认证入口（仅学生用户可见，且未认证，且未通过任一认证） -->
      <div v-if="!loading && !isMerchant && !isAdmin && !hasAnyAuthVerified" class="menu-item auth-menu-item" @click="navigateToStudentAuth">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="8.5" cy="7" r="4"></circle>
            <path d="M20 8v6M23 11h-6"></path>
          </svg>
        </div>
        <span class="menu-text">学生认证</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <!-- 商家认证入口（仅非商家用户可见，且未认证，且未通过任一认证） -->
      <div v-if="!loading && !isAdmin && ((isMerchant && !merchantVerified) || (!isMerchant && !hasAnyAuthVerified))" class="menu-item auth-menu-item" @click="navigateToMerchantAuth">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
            <circle cx="12" cy="10" r="3"></circle>
          </svg>
        </div>
        <span class="menu-text">商家认证</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <!-- 商品管理入口（仅商家可见） -->
      <div v-if="isMerchant && merchantVerified" class="menu-item merchant-menu-item" @click="navigateToMerchantVouchers">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="16" rx="2" ry="2"></rect>
            <line x1="3" y1="10" x2="21" y2="10"></line>
            <path d="M7 14h.01M11 14h.01M15 14h.01M7 18h.01M11 18h.01M15 18h.01"></path>
          </svg>
        </div>
        <span class="menu-text">商品管理</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <div v-if="isMerchant && merchantVerified" class="menu-item merchant-menu-item" @click="navigateToMerchantRefunds">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 7h13a4 4 0 0 1 0 8H8"></path>
            <path d="M7 11l-4 4 4 4"></path>
            <path d="M14 11h3"></path>
          </svg>
        </div>
        <span class="menu-text">退款审核</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <!-- 管理后台入口（仅管理员可见） -->
      <div v-if="isAdmin" class="menu-item admin-menu-item" @click="navigateToAdmin">
        <div class="menu-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="3" y1="9" x2="21" y2="9"></line>
            <line x1="9" y1="21" x2="9" y2="9"></line>
          </svg>
        </div>
        <span class="menu-text">管理后台</span>
        <div class="menu-arrow">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>
    </div>

    <!-- 退出登录按钮 -->
    <button class="logout-btn" @click="handleLogout">退出登录</button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import HeaderBar from '../../components/HeaderBar.vue'
import { fetchCurrentUser, logoutApi } from '../../api/auth'
import { clearAuthState, getStoredUserInfoObject, getStoredUserRole } from '../../utils/authStorage'
import type { UserInfo } from '../../api/auth'
import { getCartItems } from '../../api/cart'
import http from '../../api/http'

const router = useRouter()

// 控制HeaderBar显示
const showHeaderBar = ref(false)

// 用户信息
const userInfo = ref<UserInfo>({
  id: 0,
  phone: '',
  nickName: '用户',
  icon: '',
  role: 'GUEST'
})

// 判断是否为管理员
const isAdmin = ref(false)

// 判断是否为商家
const isMerchant = ref(false)
const merchantVerified = ref(false)

// 判断是否已通过任一认证（学生或商家，二选一）
const hasAnyAuthVerified = ref(false)
const authStatus = ref<any>({})
const authInfo = ref({
  type: 'none' as 'none' | 'student' | 'merchant',
  typeText: '未认证',
  school: '',
  studentNo: '',
  merchantName: '',
  statusText: ''
})

// 统计数据
const stats = ref({
  postCount: 0,
  followingCount: 0,
  followerCount: 0
})

const cartPreview = ref<Array<{
  id: number
  title?: string
  image?: string
  quantity?: number
  price?: number
}>>([])

const cartSummary = ref({
  count: 0,
  amount: 0
})

// 加载状态
const loading = ref(true)

const normalizeUserInfo = (raw: any): UserInfo => ({
  id: Number(raw?.id || 0),
  phone: raw?.phone || '',
  nickName: raw?.nickName || raw?.username || raw?.name || '用户',
  icon: raw?.icon || raw?.avatarUrl || raw?.avatar || '',
  bio: raw?.bio || raw?.intro || '',
  gender: raw?.gender || '',
  birthday: raw?.birthday || '',
  region: raw?.region || raw?.area || '',
  occupation: raw?.occupation || '',
  role: raw?.role || 'GUEST'
})

// 加载用户信息
const loadUserInfo = async () => {
  loading.value = true
  try {
    const result = await fetchCurrentUser()
    if (result.code === 200 && result.data) {
      const serverUser = normalizeUserInfo(result.data)
      
      // 从会话存储更新（如果存在）
      const saved = getStoredUserInfoObject<any>()
      if (saved) {
        const savedUser = normalizeUserInfo(saved)
        // 以服务端实时数据为准，缓存仅补充缺失字段
        userInfo.value = { ...savedUser, ...serverUser }
      } else {
        userInfo.value = serverUser
      }
      
      // 检查是否为管理员
      isAdmin.value = userInfo.value.role === 'ADMIN' || userInfo.value.role === 2 || getStoredUserRole() === 'ADMIN'
      
      // 检查是否为商家
      isMerchant.value = userInfo.value.role === 'MERCHANT' || userInfo.value.role === 1 || getStoredUserRole() === 'MERCHANT'
      
      // 检查是否已通过任一认证（学生或商家，二选一）
      await checkAnyAuthStatus()
      await loadAuthInfo()
      
      // TODO: 加载统计数据（发布数、关注数、粉丝数）
      // 这里暂时使用默认值，后续可以调用专门的统计API
      loadUserStats()
      loadCartSummary()
    }
  } catch (error: any) {
    console.error('加载用户信息失败:', error)
    // 如果API失败，尝试从会话存储读取
    const saved = getStoredUserInfoObject<any>()
    if (saved) {
      userInfo.value = normalizeUserInfo(saved)
      isAdmin.value = userInfo.value.role === 'ADMIN' || userInfo.value.role === 2 || getStoredUserRole() === 'ADMIN'
      isMerchant.value = userInfo.value.role === 'MERCHANT' || userInfo.value.role === 1 || getStoredUserRole() === 'MERCHANT'
      merchantVerified.value = false
      await loadAuthInfo()
      loadCartSummary()
    }
  } finally {
    loading.value = false
  }
}

const loadAuthInfo = async () => {
  authInfo.value = {
    type: 'none',
    typeText: '未认证',
    school: '',
    studentNo: '',
    merchantName: '',
    statusText: ''
  }
  try {
    const currentAuth = authStatus.value || {}
    if (currentAuth.merchantVerified === true) {
      authInfo.value.type = 'merchant'
      authInfo.value.typeText = '商家认证'
      authInfo.value.merchantName = currentAuth.merchantName || ''
      authInfo.value.statusText = currentAuth.merchantStatusText || '已通过'
      return
    }
    if (currentAuth.studentVerified === true) {
      authInfo.value.type = 'student'
      authInfo.value.typeText = '学生认证'
      authInfo.value.school = currentAuth.school || ''
      authInfo.value.studentNo = currentAuth.studentNo || ''
      authInfo.value.statusText = currentAuth.studentStatusText || '已通过'
      return
    }

    const [studentRes, merchantRes] = await Promise.all([
      http.get('/auth-request/student'),
      http.get('/auth-request/merchant')
    ])
    const student = studentRes.data?.data
    const merchant = merchantRes.data?.data

    if (student?.hasRequest && Number(student.status) !== 1) {
      authInfo.value.type = 'student'
      authInfo.value.typeText = '学生认证'
      authInfo.value.school = student.school || ''
      authInfo.value.studentNo = student.studentNo || ''
      authInfo.value.statusText = student.statusText || ''
      return
    }
    if (merchant?.hasRequest && Number(merchant.status) !== 1) {
      authInfo.value.type = 'merchant'
      authInfo.value.typeText = '商家认证'
      authInfo.value.merchantName = merchant.merchantName || ''
      authInfo.value.statusText = merchant.statusText || ''
    }
  } catch (e) {
    console.error('加载认证信息失败:', e)
  }
}

// 加载用户统计数据
const loadUserStats = async () => {
  try {
    const { data } = await http.get('/users/me/stats')
    if (data.code === 200 && data.data) {
      stats.value = {
        postCount: data.data.postCount || 0,
        followingCount: data.data.followingCount || 0,
        followerCount: data.data.followerCount || 0
      }
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    // 使用默认值
    stats.value = {
      postCount: 0,
      followingCount: 0,
      followerCount: 0
    }
  }
}

const loadCartSummary = async () => {
  try {
    const list = await getCartItems()
    const normalized = (list || []).map((item: any) => ({
      id: Number(item.id || 0),
      title: item.title || '',
      image: item.image || '',
      quantity: 1,
      price: Number(item.price || 0)
    }))
    cartPreview.value = normalized.slice(0, 2)
    cartSummary.value.count = normalized.length
    cartSummary.value.amount = normalized.reduce((sum: number, item: any) => {
      return sum + Number(item.price || 0)
    }, 0)
  } catch (e) {
    cartPreview.value = []
    cartSummary.value = { count: 0, amount: 0 }
  }
}

// 检查是否已通过任一认证（学生或商家，二选一）
const checkAnyAuthStatus = async () => {
  try {
    // 通过查询检查是否已通过学生或商家任一认证
    const { data } = await http.get('/users/me/auth-status/any')
    if (data.code === 200 && data.data) {
      authStatus.value = data.data
      merchantVerified.value = data.data.merchantVerified === true
      // 如果已通过任一认证，都不显示认证入口
      if (data.data.shouldHideAllAuth !== undefined) {
        hasAnyAuthVerified.value = data.data.shouldHideAllAuth
      } else {
        // 兼容旧逻辑：如果已通过任一认证，不显示认证入口
        hasAnyAuthVerified.value = data.data.hasAnyAuth === true
      }
    } else {
      authStatus.value = {}
      hasAnyAuthVerified.value = false
      merchantVerified.value = false
    }
  } catch (error) {
    console.error('检查认证状态失败:', error)
    authStatus.value = {}
    hasAnyAuthVerified.value = false
    merchantVerified.value = false
  }
}

// 定义emits
const emit = defineEmits(['toggle-sidebar'])

// 处理侧边栏开关
const handleToggleSidebar = () => {
  emit('toggle-sidebar')
}

// 处理搜索（虽然不显示搜索框，但为了API一致性保留）
const handleSearch = (keyword: string) => {
  console.log('Search:', keyword)
}

// 导航到消息页面
const navigateToMessage = () => {
  router.push('/message')
}

// 导航到订单页面
const navigateToOrders = () => {
  router.push('/me/orders')
}

const navigateToRefunds = () => {
  router.push('/me/refunds')
}

// 导航到浏览历史页面
const navigateToHistory = () => {
  router.push('/history')
}

// 导航到草稿页面
const navigateToDrafts = () => {
  router.push('/drafts')
}

// 导航到设置页面
const navigateToSettings = () => {
  router.push('/general-settings')
}

// 导航到商家商品管理
const navigateToMerchantVouchers = () => {
  if (!merchantVerified.value) {
    return
  }
  router.push('/merchant/vouchers')
}

const navigateToMerchantRefunds = () => {
  if (!merchantVerified.value) {
    return
  }
  router.push('/merchant/refunds')
}

// 导航到管理后台
const navigateToAdmin = () => {
  router.push('/admin')
}

// 导航到学生认证页面
const navigateToStudentAuth = () => {
  router.push('/auth/student')
}

// 导航到商家认证页面
const navigateToMerchantAuth = () => {
  router.push('/auth/merchant')
}

// 处理退出登录
const handleLogout = async () => {
  try {
    await logoutApi()
  } catch (error) {
    console.warn('退出登录接口调用失败，按本地登出处理', error)
  } finally {
    clearAuthState()
    localStorage.removeItem('rememberedUser')
    router.push('/home')
  }
}

// 导航到编辑资料页面
const navigateToEditProfile = () => {
  router.push('/me/edit')
}

// 导航到用户发布内容页面
const navigateToUserPosts = () => {
  router.push('/me/posts')
}

// 导航到关注列表页面
const navigateToFollowing = () => {
  router.push({ path: '/me/follow', query: { tab: 'following' } })
}

// 导航到粉丝列表页面
const navigateToFollowers = () => {
  router.push({ path: '/me/follow', query: { tab: 'followers' } })
}

// 导航到收藏列表页面
const navigateToFavorites = () => {
  router.push('/me/favorites')
}

const navigateToCart = () => {
  router.push('/cart')
}

// 页面加载时获取用户信息
onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  padding: 10px 20px 20px;
  background-color: #f8f8f8;
  min-height: 100vh;
}

.profile-header {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-container {
  margin-right: 20px;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 3px solid #1677ff;
}

.user-info {
  flex: 1;
}

.username {
  margin: 0 0 5px 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.user-bio {
  margin: 0 0 10px 0;
  color: #666;
  font-size: 14px;
}

.edit-profile-btn {
  background-color: #1677ff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.edit-profile-btn:hover {
  background-color: #0958d9;
}

.stats-container {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-around;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-item:hover {
  transform: translateY(-2px);
}

.stat-item:first-child:hover {
  color: #1677ff;
}

.stat-number {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.immutable-section {
  background-color: #ffffff;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.immutable-title {
  padding: 14px 20px 10px;
  font-size: 14px;
  color: #999;
}

.immutable-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid #f3f3f3;
}

.immutable-label {
  color: #666;
  font-size: 14px;
}

.immutable-value {
  color: #222;
  font-size: 14px;
  font-weight: 500;
}

.cart-section {
  background-color: #ffffff;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 14px 16px;
  cursor: pointer;
}

.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.cart-title-wrap {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.cart-title {
  margin: 0;
  font-size: 16px;
  color: #222;
}

.cart-count {
  font-size: 12px;
  color: #888;
}

.cart-action {
  color: #1677ff;
  font-size: 13px;
}

.cart-preview-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cart-preview-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cart-preview-image {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  object-fit: cover;
  background: #f2f2f2;
}

.cart-preview-info {
  min-width: 0;
  flex: 1;
}

.cart-preview-title {
  margin: 0;
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cart-preview-sub {
  margin: 4px 0 0;
  font-size: 12px;
  color: #999;
}

.cart-empty {
  color: #999;
  font-size: 13px;
  padding: 2px 0 8px;
}

.cart-footer {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #666;
  font-size: 13px;
}

.cart-footer strong {
  font-size: 18px;
  color: #e03131;
}

.menu-section {
  background-color: #ffffff;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.3s;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:hover {
  background-color: #f5f5f5;
}

.auth-menu-item {
  border-top: 2px solid #f0f0f0;
  margin-top: 8px;
  padding-top: 16px;
}

.auth-menu-item .menu-text {
  color: #1677ff;
  font-weight: 500;
}

.merchant-menu-item {
  border-top: 2px solid #f0f0f0;
  margin-top: 8px;
  padding-top: 16px;
}

.merchant-menu-item .menu-text {
  color: #1677ff;
  font-weight: 500;
}

.admin-menu-item {
  border-top: 2px solid #f0f0f0;
  margin-top: 8px;
  padding-top: 16px;
}

.admin-menu-item .menu-text {
  color: #1677ff;
  font-weight: 500;
}

.menu-icon {
  width: 24px;
  height: 24px;
  margin-right: 16px;
  color: #666;
}

.menu-text {
  flex: 1;
  font-size: 16px;
  color: #333;
}

.menu-arrow {
  width: 20px;
  height: 20px;
  color: #ccc;
}

.logout-btn {
  width: 100%;
  padding: 15px;
  background-color: #ffffff;
  color: #ff4d4f;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.logout-btn:hover {
  background-color: #f5f5f5;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
  }
  
  .avatar-container {
    margin-right: 0;
    margin-bottom: 15px;
  }
  
  .stats-container {
    padding: 15px;
  }
  
  .stat-number {
    font-size: 16px;
  }
  
  .stat-label {
    font-size: 13px;
  }
}
</style>
