<template>
  <div class="add-friend-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 18-6-6 6-6"/>
        </svg>
      </button>
      <h1 class="page-title">添加好友</h1>
      <button class="settings-btn" @click="goToSettings">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22-.08.47 0 .59.22l1.92-3.32c.12.22-.07.47-.12.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/>
        </svg>
      </button>
    </div>

    <!-- 个人名片区域 -->
    <div class="profile-card">
      <div class="qrcode-container">
        <img :src="qrcodeUrl" alt="个人二维码" class="qrcode-image">
        <div class="avatar-overlay">
          <img :src="currentUser.icon || defaultAvatar" alt="头像" class="avatar">
        </div>
      </div>
      <div class="user-info">
        <h2 class="nickname">{{ currentUser.nickName || '未设置昵称' }}</h2>
        <p class="user-id">用户ID：{{ currentUser.id || '--' }}</p>
        <p v-if="currentUser.phone" class="user-phone">手机号：{{ currentUser.phone }}</p>
        <p v-if="currentUser.bio" class="user-bio">{{ currentUser.bio }}</p>
      </div>
    </div>

    <!-- 功能按钮区域 -->
    <div class="action-section">
      <button class="action-btn" @click="scanQRCode">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M19.75 19.75l-5.5-5.5a.75.75 0 0 1 1.06-1.06l5.5 5.5a.75.75 0 0 1-.05 1.06 10.07 10.07 0 0 1-1.06-.05zm-1.06-14.44l-5.5 5.5a.75.75 0 0 1-1.06-1.06l5.5-5.5a.75.75 0 1 1 1.06 1.06zM4.25 4.25l5.5 5.5a.75.75 0 0 1-1.06 1.06l-5.5-5.5a.75.75 0 1 1 1.06-1.06zM5.31 19.75a10.07 10.07 0 0 1-.05-1.06l5.5-5.5a.75.75 0 0 1 1.06 1.06l-5.5 5.5a.75.75 0 0 1-1.06-.05zM12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0-2a2 2 0 1 1 0-4 2 2 0 0 1 0 4z"/>
        </svg>
        <span>扫一扫</span>
      </button>
      <button class="action-btn" @click="openContacts">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M16 1H8C6.34 1 5 2.34 5 4v16c0 1.66 1.34 3 3 3h8c1.66 0 3-1.34 3-3V4c0-1.66-1.34-3-3-3zm-2 20h-4v-1h4v1zm3.25-3H6.75V4h10.5v14z"/>
        </svg>
        <span>通讯录</span>
      </button>
    </div>

    <!-- 分享区域 -->
    <div class="share-section">
      <button class="share-btn" @click="shareToWechat">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
        </svg>
        <span>微信好友</span>
        <span class="share-hint">分享个人名片至微信</span>
      </button>
      <button class="share-btn" @click="shareToQQ">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M17.66 10.09c-.94.66-2.07 1.08-3.26 1.18-.29.03-.52-.24-.5-.53.08-1.18.48-2.32 1.14-3.25.13-.18.05-.45-.13-.58-.18-.13-.45-.05-.58.13-.82 1.15-1.33 2.48-1.38 3.88-.03.27-.24.48-.51.48-.29 0-.53-.25-.5-.53.06-1.56.6-2.99 1.52-4.19.2-.27.17-.7-.07-.88-.24-.17-.7.07-.87.27-1.15 1.46-1.84 3.21-1.84 5.05 0 2.52 1.4 4.81 3.54 5.96 2.31 1.23 4.98 1.03 6.88-.52.17-.13.45-.05.58.13.13.18.05.45-.13.58-.94.67-2.06 1.09-3.25 1.18-.29.02-.52-.24-.49-.52.09-1.18.5-2.32 1.16-3.24.12-.18.05-.45-.12-.58-.18-.13-.45-.05-.59.13z"/>
        </svg>
        <span>QQ好友</span>
        <span class="share-hint">分享个人名片至QQ</span>
      </button>
    </div>

    <!-- 可能感兴趣的人 -->
    <div class="recommendations-section">
      <h3 class="section-title">你可能感兴趣的人</h3>
      <div class="recommendations-list">
        <div class="recommend-item" v-for="(item, index) in recommendations" :key="index">
          <img :src="item.avatar" alt="头像" class="recommend-avatar">
          <div class="recommend-info">
            <h4 class="recommend-name">{{ item.name }}</h4>
          </div>
          <button class="follow-btn" :disabled="item.isFollowing" @click="followUser(index)">
            {{ item.isFollowing ? '已关注' : '关注' }}
          </button>
          <button class="close-btn" @click="removeRecommendation(index)">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo, type UserInfo } from '../../api/auth'
import { followUser as followUserApi, getFollowers, getFollowings, type UserFollowInfo } from '../../api/user'
import { showAlert } from '../../utils/dialog'

const router = useRouter()

// 当前登录用户信息
const currentUser = ref<UserInfo>({
  id: 0,
  phone: '',
  nickName: '',
  role: 'GUEST'
})

// 默认头像
const defaultAvatar = 'https://picsum.photos/50/50'

// 二维码URL（可以根据用户ID生成）
const qrcodeUrl = ref('https://picsum.photos/200/200')

// 加载状态
const loading = ref(true)

// 加载当前用户信息
const loadCurrentUser = async () => {
  try {
    loading.value = true
    const userInfo = await getUserInfo()
    currentUser.value = userInfo
    
    // 根据用户ID生成二维码URL（这里可以使用二维码生成库）
    // 暂时使用占位符，实际应该生成包含用户ID的二维码
    qrcodeUrl.value = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(`campus-life://user/${userInfo.id}`)}`
  } catch (error) {
    console.error('获取用户信息失败:', error)
    // 如果API失败，尝试从localStorage读取
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      try {
        currentUser.value = JSON.parse(userInfoStr)
      } catch (e) {
        console.error('解析用户信息失败:', e)
      }
    }
  } finally {
    loading.value = false
  }
}

// 组件挂载时加载用户信息
onMounted(() => {
  loadCurrentUser().then(() => {
    loadRecommendations()
  })
})

const recommendations = ref<Array<{
  id: number
  name: string
  avatar: string
  isFollowing: boolean
}>>([])

// 返回上一页
const goBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  router.push('/message')
}

// 跳转到设置页面
const goToSettings = () => {
  router.push('/privacy-settings')
}

// 扫一扫功能
const scanQRCode = () => {
  void showAlert('扫一扫功能开发中')
}

// 打开通讯录
const openContacts = () => {
  void showAlert('通讯录功能开发中')
}

// 分享到微信
const shareToWechat = () => {
  void showAlert('分享到微信功能开发中')
}

// 分享到QQ
const shareToQQ = () => {
  void showAlert('分享到QQ功能开发中')
}

// 关注用户
const followUser = async (index: number) => {
  const target = recommendations.value[index]
  if (!target || target.isFollowing) return
  try {
    await followUserApi(target.id)
    recommendations.value[index].isFollowing = true
    await showAlert(`已关注 ${target.name}`)
  } catch (error: any) {
    await showAlert(error?.message || '关注失败，请重试')
  }
}

// 移除推荐
const removeRecommendation = (index: number) => {
  recommendations.value.splice(index, 1)
}

const loadRecommendations = async () => {
  if (!currentUser.value.id) return
  try {
    const [followersRes, followingsRes] = await Promise.all([
      getFollowers(currentUser.value.id, 1, 50),
      getFollowings(currentUser.value.id, 1, 50)
    ])
    const followingIds = new Set((followingsRes.list || []).map(item => Number(item.userId || item.id)))
    const mergedMap = new Map<number, UserFollowInfo>()
    ;(followersRes.list || []).forEach(item => {
      const uid = Number(item.userId || item.id)
      if (uid && uid !== currentUser.value.id) mergedMap.set(uid, item)
    })
    ;(followingsRes.list || []).forEach(item => {
      const uid = Number(item.userId || item.id)
      if (uid && uid !== currentUser.value.id && !mergedMap.has(uid)) mergedMap.set(uid, item)
    })

    recommendations.value = Array.from(mergedMap.entries()).map(([id, item]) => ({
      id,
      name: item.name || `用户${id}`,
      avatar: item.avatar || defaultAvatar,
      isFollowing: followingIds.has(id)
    }))
  } catch (error) {
    recommendations.value = []
  }
}
</script>

<style scoped>
.add-friend-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 20px;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 12px;
  background-color: white;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn,
.settings-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: #333;
  cursor: pointer;
  border-radius: 50%;
  transition: background-color 0.3s;
}

.back-btn:hover,
.settings-btn:hover {
  background-color: #f5f5f5;
}

.back-btn svg,
.settings-btn svg {
  width: 20px;
  height: 20px;
}

.page-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

/* 个人名片区域 */
.profile-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 20px;
  background-color: white;
  margin-bottom: 10px;
}

.qrcode-container {
  position: relative;
  margin-bottom: 20px;
}

.qrcode-image {
  width: 200px;
  height: 200px;
  border-radius: 12px;
  border: 1px solid #e0e0e0;
}

.avatar-overlay {
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  background-color: white;
  border-radius: 50%;
  padding: 3px;
  border: 1px solid #e0e0e0;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
}

.user-info {
  text-align: center;
}

.nickname {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.user-id {
  font-size: 14px;
  color: #666;
  margin: 0 0 4px 0;
}

.user-phone {
  font-size: 13px;
  color: #999;
  margin: 0 0 4px 0;
}

.user-bio {
  font-size: 13px;
  color: #666;
  margin: 8px 0 0 0;
  padding: 8px;
  background-color: #f5f5f5;
  border-radius: 4px;
  max-width: 300px;
}

/* 功能按钮区域 */
.action-section {
  display: flex;
  background-color: white;
  margin-bottom: 10px;
  padding: 20px;
  gap: 20px;
}

.action-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: #333;
  cursor: pointer;
  padding: 10px;
  border-radius: 8px;
  transition: background-color 0.3s;
}

.action-btn:hover {
  background-color: #f5f5f5;
}

.action-btn svg {
  width: 32px;
  height: 32px;
  margin-bottom: 8px;
  color: #1890ff;
}

.action-btn span {
  font-size: 14px;
  color: #333;
}

/* 分享区域 */
.share-section {
  background-color: white;
  margin-bottom: 10px;
}

.share-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 16px 20px;
  background: none;
  border: none;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.3s;
}

.share-btn:last-child {
  border-bottom: none;
}

.share-btn:hover {
  background-color: #f5f5f5;
}

.share-btn svg {
  width: 20px;
  height: 20px;
  color: #1890ff;
  margin-right: 12px;
}

.share-btn span:first-of-type {
  font-size: 15px;
  color: #333;
  flex: 1;
}

.share-hint {
  font-size: 13px;
  color: #999;
}

/* 可能感兴趣的人 */
.recommendations-section {
  background-color: white;
  padding: 16px 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px 0;
}

.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recommend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.recommend-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  margin-right: 12px;
}

.recommend-info {
  flex: 1;
}

.recommend-name {
  font-size: 15px;
  color: #333;
  margin: 0;
}

.follow-btn {
  padding: 6px 16px;
  background-color: #ff4d4f;
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
  margin-right: 12px;
}

.follow-btn:hover {
  background-color: #ff7875;
}

.close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: #999;
  transition: all 0.3s;
}

.close-btn:hover {
  background-color: #f5f5f5;
  color: #666;
}

.close-btn svg {
  width: 18px;
  height: 18px;
}

/* 暗色模式样式 */
:global(.dark-mode) .add-friend-container {
  background-color: #1a1a1a;
}

:global(.dark-mode) .page-header,
:global(.dark-mode) .profile-card,
:global(.dark-mode) .action-section,
:global(.dark-mode) .share-section,
:global(.dark-mode) .recommendations-section {
  background-color: #1f1f1f;
  border-color: #333;
}

:global(.dark-mode) .page-title,
:global(.dark-mode) .nickname,
:global(.dark-mode) .action-btn span,
:global(.dark-mode) .share-btn span:first-of-type,
:global(.dark-mode) .section-title,
:global(.dark-mode) .recommend-name {
  color: #ffffff;
}

:global(.dark-mode) .user-id,
:global(.dark-mode) .share-hint {
  color: #999;
}

:global(.dark-mode) .back-btn,
:global(.dark-mode) .settings-btn {
  color: #ffffff;
}

:global(.dark-mode) .share-btn:hover,
:global(.dark-mode) .action-btn:hover,
:global(.dark-mode) .back-btn:hover,
:global(.dark-mode) .settings-btn:hover,
:global(.dark-mode) .close-btn:hover {
  background-color: #333;
}

:global(.dark-mode) .qrcode-image,
:global(.dark-mode) .avatar-overlay {
  border-color: #333;
}

:global(.dark-mode) .avatar-overlay {
  background-color: #1f1f1f;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .qrcode-image {
    width: 180px;
    height: 180px;
  }
  
  .profile-card {
    padding: 20px;
  }
  
  .action-section {
    padding: 16px;
  }
  
  .share-btn,
  .recommendations-section {
    padding: 12px 16px;
  }
}
</style>
