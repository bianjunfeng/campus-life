<template>
  <div class="follow-list-page">
    <!-- 顶部导航栏 -->
    <div class="page-header">
      <button class="back-button" @click="goBack">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
      </button>
      <h1 class="page-title">{{ pageTitle }}</h1>
      <div class="header-placeholder"></div>
    </div>

    <!-- 标签页切换 -->
    <div class="tabs-container">
      <div class="tab-item" :class="{ active: activeTab === 'mutual' }" @click="switchTab('mutual')">
        <span class="tab-text">互相关注</span>
        <div v-if="activeTab === 'mutual'" class="tab-underline"></div>
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'following' }" @click="switchTab('following')">
        <span class="tab-text">关注</span>
        <div v-if="activeTab === 'following'" class="tab-underline"></div>
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'followers' }" @click="switchTab('followers')">
        <span class="tab-text">粉丝</span>
        <div v-if="activeTab === 'followers'" class="tab-underline"></div>
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'recommended' }" @click="switchTab('recommended')">
        <span class="tab-text">推荐</span>
        <span v-if="recommendedCount > 0" class="tab-badge">{{ recommendedCount }}</span>
        <div v-if="activeTab === 'recommended'" class="tab-underline"></div>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="user-list">
      <!-- 空状态 -->
      <div v-if="getCurrentList().length === 0" class="empty-state" :key="'empty-state'">
        <p>{{ getEmptyStateText() }}</p>
      </div>

      <!-- 用户列表项 -->
      <div v-else>
        <div v-if="loading" class="loading-state">
          <p>加载中...</p>
        </div>
        <div v-else>
          <div class="user-item" v-for="user in getCurrentList()" :key="user.id">
            <div class="user-info" @click="navigateToUserProfile(user.userId)">
              <div class="avatar-container">
                <img :src="user.avatar || 'https://placehold.co/100x100/CCCCCC/FFFFFF?text=头像'" :alt="user.name" class="user-avatar" />
              </div>
              <div class="user-details">
                <h3 class="user-name">{{ user.name || '未设置用户名' }}</h3>
                <p v-if="user.bio" class="user-bio">{{ user.bio }}</p>
              </div>
            </div>
            <div class="user-actions">
              <button class="message-button" @click.stop="sendMessage(user.userId)">
                发私信
              </button>
              <button class="more-button" @click.stop="showMoreOptions(user)">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="1"></circle>
                  <circle cx="12" cy="5" r="1"></circle>
                  <circle cx="12" cy="19" r="1"></circle>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部弹出菜单 -->
    <div v-if="showActionMenu" class="action-menu-overlay" @click="closeActionMenu">
      <div class="action-menu" @click.stop>
        <div class="action-menu-item cancel-follow" @click="handleUnfollow">
          <span class="action-text">取消关注</span>
        </div>
        <div class="action-menu-item cancel" @click="closeActionMenu">
          <span class="action-text">取消</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getFollowers, getFollowings, getMutualFollows, unfollowUser, type UserFollowInfo } from '../../api/user'
import { getUserInfo } from '../../api/auth'
import { notify } from '@/utils/notify'

const router = useRouter()
const route = useRoute()
const targetUserId = computed(() => {
  const userId = route.params.userId
  return userId ? Number(userId) : null
})
const currentUserId = ref<number | null>(null)
const activeTab = ref('mutual')
const pageTitle = computed(() => '关注和粉丝')
const recommendedCount = ref(0) // 推荐标签的徽章数字
const loading = ref(false)
const showActionMenu = ref(false) // 控制底部菜单显示
const selectedUser = ref<UserFollowInfo | null>(null) // 当前选中的用户
const isUnfollowing = ref(false) // 取消关注中状态

// 数据列表
const mutualList = ref<UserFollowInfo[]>([])
const followingList = ref<UserFollowInfo[]>([])
const followersList = ref<UserFollowInfo[]>([])
const recommendedList = ref<UserFollowInfo[]>([]) // 推荐列表暂时为空，后续可以实现推荐算法

// 获取当前用户ID
const loadCurrentUser = async () => {
  try {
    const userInfo = await getUserInfo()
    currentUserId.value = userInfo.id
  } catch (error) {
    console.error('获取当前用户信息失败:', error)
    currentUserId.value = null
  }
}

// 加载数据
const loadData = async () => {
  const userId = targetUserId.value || currentUserId.value
  if (!userId) {
    console.error('无法确定要查看的用户ID')
    return
  }

  loading.value = true
  try {
    // 根据当前标签页加载对应数据
    switch (activeTab.value) {
      case 'mutual':
        if (targetUserId.value || currentUserId.value) {
          const mutualData = await getMutualFollows(userId)
          mutualList.value = mutualData.list || []
        }
        break
      case 'following':
        if (targetUserId.value || currentUserId.value) {
          const followingData = await getFollowings(userId)
          followingList.value = followingData.list || []
        }
        break
      case 'followers':
        if (targetUserId.value || currentUserId.value) {
          const followersData = await getFollowers(userId)
          followersList.value = followersData.list || []
        }
        break
      case 'recommended':
        // 推荐列表暂时为空，后续可以实现推荐算法
        recommendedList.value = []
        break
    }
  } catch (error: any) {
    console.error('加载数据失败:', error)
    notify(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 监听标签页切换，重新加载数据
watch(activeTab, () => {
  loadData()
})

// 组件挂载时的初始化
onMounted(async () => {
  // 获取当前用户信息
  await loadCurrentUser()
  
  // 根据URL查询参数设置默认选中的标签页
  const tabParam = route.query.tab as string
  if (tabParam === 'mutual') {
    activeTab.value = 'mutual'
  } else if (tabParam === 'followers') {
    activeTab.value = 'followers'
  } else if (tabParam === 'recommended') {
    activeTab.value = 'recommended'
  } else if (tabParam === 'following') {
    activeTab.value = 'following'
  } else {
    // 默认显示"互相关注"标签页
    activeTab.value = 'mutual'
  }
  
  // 加载数据
  await loadData()
})

// 返回上一页
const goBack = () => {
  router.back()
}

// 切换标签页
const switchTab = (tab: string) => {
  activeTab.value = tab
  // 切换标签页时会自动触发 watch，重新加载数据
}

// 跳转到用户个人主页
const navigateToUserProfile = (userId: number) => {
  router.push(`/user/${userId}`)
}

// 检查用户是否已关注（从数据中获取）
const isUserFollowing = (user: UserFollowInfo): boolean => {
  return user.isFollowing === true
}

// 获取当前标签页的用户列表
const getCurrentList = (): UserFollowInfo[] => {
  switch (activeTab.value) {
    case 'mutual':
      return mutualList.value
    case 'following':
      return followingList.value
    case 'followers':
      return followersList.value
    case 'recommended':
      return recommendedList.value
    default:
      return []
  }
}

// 获取空状态文本
const getEmptyStateText = () => {
  switch (activeTab.value) {
    case 'mutual':
      return '还没有互相关注的用户'
    case 'following':
      return '还没有关注任何人'
    case 'followers':
      return '还没有粉丝'
    case 'recommended':
      return '暂无推荐用户'
    default:
      return '暂无数据'
  }
}

// 发送私信
const sendMessage = (userId: number) => {
  // 跳转到私信页面
  router.push(`/messages/${userId}`)
  // 在实际应用中，这里应该打开私信对话框或跳转到私信页面
  console.log('Send message to user:', userId)
}

// 显示更多选项
const showMoreOptions = (user: UserFollowInfo) => {
  selectedUser.value = user
  // 在"互相关注"、"关注"标签页，或者"粉丝"标签页中已关注的用户，显示取消关注选项
  const canUnfollow = activeTab.value === 'mutual' || 
                      activeTab.value === 'following' || 
                      (activeTab.value === 'followers' && user.isFollowing)
  
  if (canUnfollow) {
    showActionMenu.value = true
  } else {
    // 其他情况可以显示其他选项，暂时不处理
    console.log('Show more options for user:', user)
  }
}

// 关闭底部菜单
const closeActionMenu = () => {
  showActionMenu.value = false
  selectedUser.value = null
}

// 取消关注
const handleUnfollow = async () => {
  if (!selectedUser.value || isUnfollowing.value) {
    return
  }

  const userId = selectedUser.value.userId
  isUnfollowing.value = true

  try {
    await unfollowUser(userId)
    // 关闭菜单
    closeActionMenu()
    // 重新加载数据
    await loadData()
    // 显示成功提示
    notify('已取消关注')
  } catch (error: any) {
    console.error('取消关注失败:', error)
    notify(error.message || '取消关注失败')
  } finally {
    isUnfollowing.value = false
  }
}
</script>

<style scoped>
.follow-list-page {
  background-color: #f8f8f8;
  min-height: 100vh;
}

/* 顶部导航栏 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #e6e6e6;
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-button {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  transition: background-color 0.2s;
  border-radius: 4px;
}

.back-button:hover {
  background-color: #f5f5f5;
}

.back-button svg {
  width: 24px;
  height: 24px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.header-placeholder {
  width: 40px;
}

/* 标签页 */
.tabs-container {
  display: flex;
  background-color: #ffffff;
  margin-bottom: 8px;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16px 0;
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.tab-text {
  font-size: 16px;
  color: #999999;
}

.tab-item.active .tab-text {
  color: #333333;
  font-weight: 600;
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background-color: #ff4757;
  color: white;
  border-radius: 9px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
}

.tab-underline {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background-color: #ff4757;
  border-radius: 3px;
}

/* 用户列表 */
.user-list {
  background-color: #ffffff;
}

.empty-state {
  padding: 60px 0;
  text-align: center;
  color: #999999;
}

.loading-state {
  padding: 60px 0;
  text-align: center;
  color: #999999;
}

.user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.user-info {
  display: flex;
  align-items: center;
  flex: 1;
  cursor: pointer;
}

.avatar-container {
  margin-right: 12px;
}

.user-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.user-details {
  flex: 1;
}

.user-name {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 500;
}

.user-bio {
  margin: 0;
  font-size: 13px;
  color: #999999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-button {
  padding: 6px 16px;
  border: none;
  border-radius: 20px;
  background-color: #f5f5f5;
  color: #333333;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
}

.message-button:hover {
  background-color: #e8e8e8;
}

.more-button {
  width: 32px;
  height: 32px;
  border: none;
  background-color: transparent;
  color: #666666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.3s;
  padding: 0;
}

.more-button:hover {
  background-color: #f5f5f5;
  color: #333333;
}

.more-button svg {
  width: 20px;
  height: 20px;
}

/* 底部弹出菜单 */
.action-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.action-menu {
  width: 100%;
  background-color: #ffffff;
  border-radius: 16px 16px 0 0;
  padding: 8px 0;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.action-menu-item {
  padding: 16px 20px;
  text-align: center;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f0f0f0;
}

.action-menu-item:last-child {
  border-bottom: none;
}

.action-menu-item:hover {
  background-color: #f5f5f5;
}

.action-menu-item.cancel-follow .action-text {
  color: #ff4757;
  font-weight: 500;
}

.action-menu-item.cancel .action-text {
  color: #333333;
  font-weight: 500;
}

.action-text {
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .page-title {
    font-size: 17px;
  }
  
  .tab-text {
    font-size: 15px;
  }
  
  .user-name {
    font-size: 15px;
  }
  
  .user-bio {
    font-size: 13px;
  }
  
  .follow-button {
    padding: 6px 16px;
    font-size: 13px;
  }
}
</style>
