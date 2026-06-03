<template>
  <div class="favorite-posts-page">
    <!-- 页面头部 -->
    <div class="page-header-custom">
      <button class="back-button" @click="goBack">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
      </button>
      <h2 class="page-title">我的收藏</h2>
      <div class="header-right-placeholder"></div>
    </div>
    
    <!-- 内容主体 -->
    <div class="posts-content">
      <!-- 帖子列表 -->
      <div class="posts-list" v-if="posts.length > 0">
        <div 
          v-for="post in posts" 
          :key="post.id" 
          class="post-item"
          @click="navigateToPostDetail(post.id)"
        >
          <div class="post-header">
            <img :src="post.authorAvatar || 'https://via.placeholder.com/40x40?text=User'" alt="用户头像" class="post-avatar">
            <div class="post-user-info">
              <span class="post-username">{{ post.authorName || '用户' }}</span>
              <span class="post-time">{{ formatDate(post.createdAt) }}</span>
            </div>
          </div>
          <div class="post-content">
            <h3 class="post-title" v-if="post.title">{{ post.title }}</h3>
            <p class="post-text">{{ post.content }}</p>
            <!-- 帖子图片展示 -->
            <div class="post-images" v-if="post.images && post.images.length > 0">
              <img 
                v-for="(image, index) in (typeof post.images[0] === 'string' ? post.images : post.images.map((img: any) => img.url || img)).slice(0, 3)" 
                :key="index" 
                :src="typeof image === 'string' ? image : image.url" 
                alt="帖子图片" 
                class="post-image"
              >
              <span class="more-images" v-if="post.images.length > 3">
                +{{ post.images.length - 3 }}
              </span>
            </div>
          </div>
          <div class="post-stats">
            <div class="stat">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
              </svg>
              {{ post.commentCount || 0 }}
            </div>
            <div class="stat">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
              {{ post.likeCount || 0 }}
            </div>
            <div class="stat favorited-stat">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon">
                <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
              </svg>
              <span>已收藏</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="loading" class="empty-state">
        <div class="loading-spinner"></div>
        <p class="empty-text">加载中...</p>
      </div>
      
      <!-- 无内容状态 -->
      <div class="empty-state" v-else-if="!loading && posts.length === 0">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
          <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
        </svg>
        <p class="empty-text">暂无收藏内容</p>
        <p class="empty-hint">去发现页看看有什么有趣的帖子吧</p>
        <button class="go-discover-btn" @click="goToDiscover">去发现</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMyFavorites } from '../../api/forum'
import { notify } from '@/utils/notify'

// 定义帖子数据类型
interface Post {
  id: number
  title?: string
  content: string
  images: any[]
  likeCount?: number
  commentCount?: number
  createdAt: string
  authorId?: number
  authorName?: string
  authorAvatar?: string
}

const router = useRouter()

const loading = ref(false)
const posts = ref<Post[]>([])

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  // 小于1小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  // 小于24小时
  else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  // 小于7天
  else if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  }
  // 显示具体日期
  else {
    return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
  }
}

// 加载收藏列表
const loadFavorites = async () => {
  loading.value = true
  try {
    const postList = await fetchMyFavorites(1, 100)
    posts.value = postList.map((post: any) => ({
      id: post.id,
      title: post.title || '',
      content: post.content || '',
      images: post.images || [],
      likeCount: post.likeCount || 0,
      commentCount: post.commentCount || 0,
      createdAt: post.createdAt || post.createTime || new Date().toISOString(),
      authorId: post.authorId || post.userId || 0,
      authorName: post.authorName || post.username || '用户',
      authorAvatar: post.authorAvatar || post.avatarUrl || ''
    }))
  } catch (error: any) {
    console.error('加载收藏列表失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '加载收藏列表失败，请重试'
    notify(errorMsg)
    posts.value = []
  } finally {
    loading.value = false
  }
}

// 导航到帖子详情
const navigateToPostDetail = (postId: number) => {
  router.push(`/posts/${postId}`)
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 去发现页
const goToDiscover = () => {
  router.push('/home')
}

// 生命周期
onMounted(() => {
  loadFavorites()
})
</script>

<style scoped>
.favorite-posts-page {
  min-height: 100vh;
  background-color: #f8f8f8;
}

/* 自定义页面头部 */
.page-header-custom {
  position: sticky;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: white;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  z-index: 100;
}

.back-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: transparent;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-button:hover {
  background-color: #f5f5f5;
}

.back-button svg {
  width: 22px;
  height: 22px;
}

.page-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0;
  flex: 1;
  text-align: center;
}

.header-right-placeholder {
  width: 36px;
  height: 36px;
}

.posts-content {
  padding: 0 16px 20px;
}

.posts-list {
  margin-top: 16px;
}

.post-item {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
}

.post-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.post-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 12px;
}

.post-user-info {
  display: flex;
  flex-direction: column;
}

.post-username {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.post-time {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.post-content {
  margin-bottom: 12px;
}

.post-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.post-text {
  font-size: 15px;
  line-height: 1.5;
  color: #333;
  margin: 0 0 12px;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  position: relative;
}

.post-image {
  width: calc(33.333% - 5.333px);
  aspect-ratio: 1;
  border-radius: 8px;
  object-fit: cover;
}

.more-images {
  position: absolute;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  width: calc(33.333% - 5.333px);
  aspect-ratio: 1;
}

.post-stats {
  display: flex;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.stat {
  display: flex;
  align-items: center;
  margin-right: 24px;
  color: #666;
  font-size: 14px;
}

.stat .icon {
  width: 16px;
  height: 16px;
  margin-right: 6px;
}

.favorited-stat {
  color: #ffa502;
}

.favorited-stat .icon {
  fill: currentColor;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  color: #ccc;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #666;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 14px;
  color: #999;
  margin-bottom: 20px;
}

.go-discover-btn {
  background-color: #1677ff;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
}

.go-discover-btn:hover {
  background-color: #0958d9;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1677ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .post-item {
    margin-bottom: 12px;
    padding: 14px;
  }

  .post-text {
    font-size: 14px;
  }

  .post-images {
    gap: 6px;
  }

  .stat {
    margin-right: 16px;
    font-size: 13px;
  }
}
</style>

