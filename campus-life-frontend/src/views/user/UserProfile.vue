<template>
  <div class="user-profile-page">
    <!-- 顶部导航栏 -->
    <div class="page-header">
      <button class="back-button" @click="goBack">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M15 6L9 12L15 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <h1 class="page-title">{{ pageTitle }}</h1>
      <div class="header-placeholder"></div>
    </div>

    <!-- 用户信息头部 -->
    <div class="user-header">
      <div class="user-avatar-large">
        <img :src="userInfo.avatar" :alt="userInfo.name" class="avatar" />
      </div>
      <h2 class="user-name-large">{{ userInfo.name }}</h2>
      <p class="user-id">用户ID: {{ userInfo.id }}</p>
      <p class="user-location">地区: {{ userInfo.location || '未设置' }}</p>
      
      <!-- 提及内容 -->
      <div class="mention-section" v-if="userInfo.bio">
        <span class="mention-label">简介:</span>
        <span class="mention-content">{{ userInfo.bio }}</span>
      </div>

      <!-- 数据统计 -->
      <div class="user-stats">
        <div class="stat-item" @click="navigateToUserFollowing">
          <span class="stat-number">{{ userInfo.stats.following }}</span>
          <span class="stat-label">关注</span>
        </div>
        <div class="stat-item" @click="navigateToUserFollowers">
          <span class="stat-number">{{ userInfo.stats.followers }}</span>
          <span class="stat-label">粉丝</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">{{ userInfo.stats.likes }}</span>
          <span class="stat-label">获赞与收藏</span>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div v-if="!isSelfProfile" class="action-buttons">
        <button class="follow-button" :class="{ following: isFollowing }" @click="toggleFollow">
          {{ isFollowing ? '已关注' : '关注' }}
        </button>
        <button class="message-button" @click="sendMessage">私信</button>
      </div>
    </div>

    <!-- 内容标签 -->
    <div class="content-tabs">
      <div class="tab-item" :class="{ active: activeTab === 'notes' }" @click="switchTab('notes')">
        <span class="tab-text">笔记 {{ userPosts.length > 0 ? `(${userPosts.length})` : '' }}</span>
        <div v-if="activeTab === 'notes'" class="tab-underline"></div>
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'collections' }" @click="switchTab('collections')">
        <span class="tab-text">收藏 {{ userCollections.length > 0 ? `(${userCollections.length})` : '' }}</span>
      </div>
    </div>

    <!-- 内容列表 - 根据选中的标签显示不同内容 -->
    <div v-if="activeTab === 'notes'" class="content-grid">
      <div v-if="userPosts.length === 0" class="empty-state">
        <p>该用户还没有发布任何笔记</p>
      </div>
      <div class="content-item" v-for="(item, index) in userPosts" :key="item.id" @click="navigateToPostDetail(item.id)">
        <div class="content-image">
          <img :src="item.imageUrl" :alt="item.title" class="post-image" />
        </div>
        <p class="content-title">{{ item.title }}</p>
        <div class="content-stats">
          <span class="like-count">{{ item.likes }}赞</span>
        </div>
      </div>
    </div>
    
    <div v-else-if="activeTab === 'collections'" class="content-grid">
      <div v-if="userCollections.length === 0" class="empty-state">
        <p>该用户还没有公开收藏内容</p>
      </div>
      <div class="content-item" v-for="(item, index) in userCollections" :key="item.id" @click="navigateToCollectionDetail(item.originalPostId)">
        <div class="content-image">
          <img :src="item.imageUrl" :alt="item.title" class="post-image" />
        </div>
        <p class="content-title">{{ item.title }}</p>
        <p class="content-author">by {{ item.author }}</p>
        <div class="content-stats">
          <span class="like-count">{{ item.likes }}赞</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchPostsByUserId, fetchUserFavorites } from '../../api/forum'
import { getPublicUserProfile, followUser, unfollowUser } from '../../api/user'
import { getCurrentUserObject, getStoredUserInfoObject } from '../../utils/authStorage'
import { notify } from '@/utils/notify'

const router = useRouter()
const route = useRoute()
const userId = computed(() => parseInt(route.params.id as string, 10))
const currentLoginUserId = ref(0)
const activeTab = ref('notes')
const isFollowing = ref(false)
const loading = ref(false)

const userInfo = ref({
  id: '0',
  name: '用户',
  avatar: 'https://via.placeholder.com/200x200?text=User',
  location: '未知',
  bio: '',
  stats: {
    following: 0,
    followers: 0,
    likes: 0
  }
})

const pageTitle = computed(() => '用户主页')
const isSelfProfile = computed(() => currentLoginUserId.value > 0 && currentLoginUserId.value === userId.value)

const userPosts = ref<Array<{
  id: number
  imageUrl: string
  title: string
  likes: number
}>>([])

const userCollections = ref<Array<{
  id: number
  imageUrl: string
  title: string
  likes: number
  author: string
  originalPostId: number
}>>([])

const getCover = (post: any): string => {
  if (post.coverImage) return post.coverImage
  if (Array.isArray(post.images) && post.images.length > 0) {
    const firstImage = post.images[0]
    return typeof firstImage === 'string' ? firstImage : (firstImage.url || '')
  }
  return 'https://via.placeholder.com/300x300?text=No+Image'
}

const resolveCurrentUserId = (): number => {
  const currentUser = getCurrentUserObject<any>() || getStoredUserInfoObject<any>()
  const id = Number(currentUser?.id)
  return Number.isFinite(id) && id > 0 ? id : 0
}

const loadProfile = async () => {
  if (!userId.value || Number.isNaN(userId.value)) return

  loading.value = true
  try {
    currentLoginUserId.value = resolveCurrentUserId()
    const [profile, posts, favorites] = await Promise.all([
      getPublicUserProfile(userId.value),
      fetchPostsByUserId(userId.value, 1, 100).catch(() => []),
      fetchUserFavorites(userId.value, 1, 100).catch(() => [])
    ])

    userPosts.value = posts.map((p: any) => ({
      id: p.id,
      imageUrl: getCover(p),
      title: p.title || (p.content ? String(p.content).slice(0, 28) : '无标题'),
      likes: p.likeCount || 0
    }))

    userCollections.value = favorites.map((p: any) => ({
      id: p.id,
      imageUrl: getCover(p),
      title: p.title || (p.content ? String(p.content).slice(0, 28) : '无标题'),
      likes: p.likeCount || 0,
      author: p.authorName || '用户',
      originalPostId: p.id
    }))

    userInfo.value = {
      id: String(profile.id || userId.value),
      name: profile.nickName || `用户${userId.value}`,
      avatar: profile.icon || 'https://via.placeholder.com/200x200?text=User',
      location: profile.region || '未设置',
      bio: profile.bio || '',
      stats: {
        following: profile.followingCount || 0,
        followers: profile.followerCount || 0,
        likes: posts.reduce((sum: number, p: any) => sum + (p.likeCount || 0), 0)
      }
    }
    isFollowing.value = Boolean(profile.isFollowing)
  } catch (error: any) {
    console.error('加载用户主页失败:', error)
    notify(error.message || '加载用户主页失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)
watch(() => route.params.id, () => loadProfile())

// 返回上一页
const goBack = () => {
  router.back()
}

// 跳转到帖子详情页
const navigateToPostDetail = (postId: number) => {
  router.push(`/posts/${postId}`)
}

// 关注/取消关注用户
const toggleFollow = async () => {
  if (isSelfProfile.value) return
  const wasFollowing = isFollowing.value
  isFollowing.value = !isFollowing.value
  try {
    if (wasFollowing) {
      await unfollowUser(userId.value)
      userInfo.value.stats.followers = Math.max(0, userInfo.value.stats.followers - 1)
    } else {
      await followUser(userId.value)
      userInfo.value.stats.followers += 1
    }
  } catch (error: any) {
    isFollowing.value = wasFollowing
    notify(error.message || '关注操作失败')
  }
}

// 发送私信
const sendMessage = () => {
  if (isSelfProfile.value) return
  router.push({
    path: `/messages/${userId.value}`,
    query: {
      name: userInfo.value.name,
      avatarUrl: userInfo.value.avatar
    }
  })
}

// 跳转到用户关注列表
const navigateToUserFollowing = () => {
  router.push({ path: `/user/${userId.value}/follow`, query: { tab: 'following' } })
}

// 跳转到用户粉丝列表
const navigateToUserFollowers = () => {
  router.push({ path: `/user/${userId.value}/follow`, query: { tab: 'followers' } })
}

// 切换内容标签
const switchTab = (tab: string) => {
  activeTab.value = tab
}

// 跳转到收藏内容的原帖子详情页
const navigateToCollectionDetail = (originalPostId: number) => {
  router.push(`/posts/${originalPostId}`)
}
</script>

<style scoped>
.user-profile-page {
  min-height: 100vh;
  background-color: #ffffff;
}

/* 顶部导航栏 */
.page-header {
  position: sticky;
  top: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #f0f0f0;
  z-index: 100;
}

.back-button {
  width: 36px;
  height: 36px;
  border: none;
  background-color: transparent;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
}

.back-button:hover {
  background-color: #f5f5f5;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.header-placeholder {
  width: 36px;
}

/* 用户信息头部 */
.user-header {
  padding: 24px 16px;
  text-align: center;
  border-bottom: 8px solid #f5f5f5;
}

.user-avatar-large {
  margin-bottom: 16px;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.user-name-large {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.user-id,
.user-location {
  font-size: 13px;
  color: #999;
  margin-bottom: 4px;
}

.mention-section {
  margin: 12px 0;
  padding: 8px 16px;
  background-color: #f5f5f5;
  border-radius: 12px;
  display: flex;
  align-items: center;
}

.mention-label {
  color: #666;
  margin-right: 8px;
}

.mention-content {
  color: #333;
}

/* 数据统计 */
.user-stats {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin: 20px 0;
}

.stat-item {
  cursor: pointer;
  transition: color 0.3s;
}

.stat-item:hover {
  color: #ff6b6b;
}

.stat-item {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.follow-button,
.message-button {
  padding: 8px 24px;
  border-radius: 20px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
}

.follow-button {
  background-color: #ff4757;
  color: white;
  border: none;
}

.follow-button.following {
  background-color: #f5f5f5;
  color: #333;
}

.message-button {
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #ddd;
}

/* 内容标签 */
.content-tabs {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

/* 空状态提示 */
.empty-state {
  grid-column: 1 / -1;
  padding: 40px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  cursor: pointer;
  position: relative;
}

.tab-item.active .tab-text {
  color: #333;
  font-weight: 500;
}

.tab-underline {
  position: absolute;
  bottom: 0;
  left: 40%;
  width: 20%;
  height: 2px;
  background-color: #ff4757;
}

.tab-text {
  color: #999;
  font-size: 15px;
}

/* 内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
  padding: 4px;
}

.content-item {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.content-item:hover {
  transform: translateY(-2px);
}

.content-image {
  aspect-ratio: 1;
  overflow: hidden;
}

.post-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.content-title {
  font-size: 13px;
  color: #333;
  margin: 4px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.content-stats {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.content-author {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.like-count {
  display: flex;
  align-items: center;
}

/* 响应式设计 */
@media (min-width: 768px) {
  .content-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
