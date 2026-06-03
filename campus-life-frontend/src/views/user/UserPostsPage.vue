<template>
  <div class="user-posts-page">
    <!-- 页面头部 -->
    <div class="page-header-custom">
      <button class="back-button" @click="goBack">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
      </button>
      <h2 class="page-title">我的发布</h2>
      <div class="header-right-placeholder"></div>
    </div>
    
    <!-- 原有的 HeaderBar 隐藏或移除 -->
    <!-- <HeaderBar 
      :show-search="false"
      :show-sidebar-toggle="true"
      :show-message-button="false"
      title="我的发布"
      @toggle-sidebar="handleToggleSidebar"
      @search="handleSearch"
    /> -->
    
    <!-- 内容主体 -->
    <div class="posts-content">
      <!-- 筛选选项 -->
      <div class="filter-tabs">
        <div 
          class="filter-tab"
          :class="{ active: activeTab === 'all' }"
          @click="setActiveTab('all')"
        >
          全部
        </div>
        <div 
          class="filter-tab"
          :class="{ active: activeTab === 'posts' }"
          @click="setActiveTab('posts')"
        >
          笔记
        </div>
        <div 
          class="filter-tab"
          :class="{ active: activeTab === 'comments' }"
          @click="setActiveTab('comments')"
        >
          评论
        </div>
      </div>
      
      <!-- 帖子列表 -->
      <div class="posts-list" v-if="filteredPosts.length > 0">
        <div 
          v-for="post in filteredPosts" 
          :key="post.id" 
          class="post-item"
        >
          <div class="post-header">
            <img :src="post.authorAvatar || 'https://via.placeholder.com/40x40?text=User'" alt="用户头像" class="post-avatar">
            <div class="post-user-info">
              <span class="post-username">{{ post.authorName || '用户' }}</span>
              <span class="post-time">{{ formatDate(post.createdAt) }}</span>
            </div>
          </div>
          <div class="post-content" @click="navigateToPostDetail(post.id)">
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
              {{ post.commentCount || post.comments || 0 }}
            </div>
            <div class="stat">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
              {{ post.likeCount || post.likes || 0 }}
            </div>
            <div class="stat" v-if="post.status === 1">
              <span class="status-badge">仅自己可见</span>
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
      <div class="empty-state" v-else-if="!loading && filteredPosts.length === 0">
        <img src="https://via.placeholder.com/120x120?text=No+Posts" alt="无内容" class="empty-image">
        <p class="empty-text">暂无发布内容</p>
        <button class="create-post-btn" @click="navigateToCreatePost">去发布</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import HeaderBar from '../../components/HeaderBar.vue'
import { fetchMyPosts } from '../../api/forum'
import { notify } from '@/utils/notify'

// 定义帖子数据类型
interface Post {
  id: number
  type?: string
  content: string
  images: any[]
  likeCount?: number
  likes?: number
  commentCount?: number
  comments?: number
  shares?: number
  status?: number
  createdAt: string
  authorId?: number
  authorName?: string
  authorAvatar?: string
}

const router = useRouter()

// 当前激活的标签页
const activeTab = ref('all')
const loading = ref(false)

// 帖子数据
const posts = ref<Post[]>([])

// 根据当前标签页过滤帖子
const filteredPosts = ref<Post[]>([])


// 生命周期钩子
onMounted(() => {
  loadPosts()
})

// 设置激活标签
const setActiveTab = (tab: string) => {
  activeTab.value = tab
  updateFilteredPosts()
}

// 更新筛选后的帖子列表
const updateFilteredPosts = () => {
  if (activeTab.value === 'all') {
    filteredPosts.value = posts.value
  } else if (activeTab.value === 'posts') {
    filteredPosts.value = posts.value.filter(post => post.type === 'post')
  } else {
    // 评论类型的内容
    filteredPosts.value = [] // 暂时为空，后续可扩展
  }
}

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

// 处理侧边栏开关
const handleToggleSidebar = () => {
  // 这里可以添加侧边栏切换的逻辑
  console.log('Toggle sidebar')
}

// 处理搜索
const handleSearch = (keyword: string) => {
  console.log('Search:', keyword)
}

// 加载帖子列表
const loadPosts = async () => {
  loading.value = true
  try {
    console.log('开始加载我的帖子列表...')
    const postList = await fetchMyPosts(1, 100)
    console.log('获取到的帖子列表:', postList)
    posts.value = postList.map((post: any) => ({
      id: post.id,
      type: 'post',
      content: post.content || '',
      images: post.images || [],
      likeCount: post.likeCount || 0,
      commentCount: post.commentCount || 0,
      status: post.status || 0,
      createdAt: post.createdAt || post.createTime || new Date().toISOString(),
      authorId: post.authorId || post.userId || 0,
      authorName: post.authorName || post.username || '用户',
      authorAvatar: post.authorAvatar || post.avatarUrl || ''
    }))
    console.log('处理后的帖子列表:', posts.value)
    updateFilteredPosts()
  } catch (error: any) {
    console.error('加载帖子失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '加载帖子失败，请重试'
    notify(errorMsg)
    // 即使失败也显示空状态
    posts.value = []
    updateFilteredPosts()
  } finally {
    loading.value = false
  }
}

// 导航到帖子详情
const navigateToPostDetail = (postId: number) => {
  router.push(`/posts/${postId}`)
}

// 导航到创建帖子页面
const navigateToCreatePost = () => {
  router.push('/posts/new')
}

// 返回上一页
const goBack = () => {
  router.back()
}

</script>

<style scoped>
.user-posts-page {
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

.filter-tabs {
  display: flex;
  background-color: #ffffff;
  border-radius: 8px;
  margin-top: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.filter-tab {
  flex: 1;
  padding: 12px 16px;
  text-align: center;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 2px solid transparent;
}

.filter-tab.active {
  color: #1677ff;
  border-bottom-color: #1677ff;
  background-color: #f0f7ff;
}

.filter-tab:hover:not(.active) {
  background-color: #f5f5f5;
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
  cursor: pointer;
}

.status-badge {
  background-color: #fff7e6;
  color: #d48806;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.post-text {
  font-size: 15px;
  line-height: 1.5;
  color: #333;
  margin: 0 0 12px;
  word-break: break-word;
}

.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  text-align: center;
}

.empty-image {
  width: 100px;
  height: 100px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-text {
  font-size: 16px;
  color: #666;
  margin-bottom: 20px;
}

.create-post-btn {
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

.create-post-btn:hover {
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
  .filter-tabs {
    margin-top: 12px;
  }
  
  .filter-tab {
    font-size: 14px;
    padding: 10px 12px;
  }
  
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
