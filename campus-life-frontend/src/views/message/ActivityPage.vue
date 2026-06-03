<template>
  <div class="activity-container">
    <!-- 页面头部 -->
    <header class="page-header">
      <h1>动态</h1>
      <div class="header-actions">
        <button class="filter-btn" @click="showFilter = !showFilter">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="22 3 2 12.5 12 11.5 22 3"></polygon>
          </svg>
          筛选
        </button>
      </div>
    </header>

    <!-- 筛选菜单 -->
    <div v-if="showFilter" class="filter-menu">
      <button 
        v-for="filter in filters" 
        :key="filter.id"
        :class="['filter-option', { active: activeFilter === filter.id }]"
        @click="activeFilter = filter.id; showFilter = false"
      >
        {{ filter.name }}
      </button>
    </div>

    <!-- 动态分类标签 -->
    <div class="activity-tabs">
      <button 
        :class="['tab-btn', { active: activeTab === 'all' }]"
        @click="activeTab = 'all'"
      >
        全部
      </button>
      <button 
        :class="['tab-btn', { active: activeTab === 'like' }]"
        @click="activeTab = 'like'"
      >
        点赞
      </button>
      <button 
        :class="['tab-btn', { active: activeTab === 'comment' }]"
        @click="activeTab = 'comment'"
      >
        评论
      </button>
      <button 
        :class="['tab-btn', { active: activeTab === 'follow' }]"
        @click="activeTab = 'follow'"
      >
        关注
      </button>
    </div>

    <div v-if="loading" class="empty-state">
      <p>动态加载中...</p>
    </div>

    <div v-else-if="error" class="empty-state">
      <p>{{ error }}</p>
      <span @click="loadActivities">点击重试</span>
    </div>

    <!-- 动态列表 -->
    <div v-else-if="filteredActivities.length > 0" class="activity-list">
      <div 
        v-for="activity in filteredActivities" 
        :key="activity.id"
        class="activity-item"
        @click="openActivity(activity)"
      >
        <!-- 活动类型图标 -->
        <div class="activity-icon">
          <svg v-if="activity.type === 'like'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
          </svg>
          <svg v-else-if="activity.type === 'comment'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
          <svg v-else-if="activity.type === 'follow'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
        </div>

        <!-- 活动内容 -->
        <div class="activity-content">
          <div class="activity-text">
            <span class="user-name">{{ activity.user.name }}</span>
            <span class="activity-action">{{ getActionText(activity) }}</span>
            <span class="activity-time">{{ formatTime(activity.createdAt) }}</span>
          </div>
          
          <!-- 相关帖子预览 -->
          <div v-if="activity.post" class="post-preview">
            <p class="post-text">{{ activity.post.text }}</p>
            <div v-if="activity.post.images.length > 0" class="post-images">
              <img 
                v-for="(image, index) in activity.post.images.slice(0, 3)" 
                :key="index"
                :src="image.url" 
                :alt="image.description || `图片${index + 1}`"
                class="post-image"
              >
              <div v-if="activity.post.images.length > 3" class="more-images">
                +{{ activity.post.images.length - 3 }}
              </div>
            </div>
          </div>

          <!-- 评论内容 -->
          <div v-if="activity.comment" class="comment-content">
            <p class="comment-text">{{ activity.comment.text }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
      </svg>
      <p>暂无动态</p>
      <span>关注更多用户，获取他们的最新动态</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getCommentNotifications,
  getFollowNotifications,
  getLikeAndFavoriteNotifications,
  type NotificationItem
} from '@/api/message'

// 类型定义
interface User {
  id: number
  name: string
  avatar: string
}

interface PostImage {
  url: string
  description?: string
}

interface Post {
  id: number
  text: string
  images: PostImage[]
}

interface Comment {
  id: number
  text: string
}

interface Activity {
  id: number
  type: 'like' | 'comment' | 'follow' | 'post'
  user: User
  post?: Post
  comment?: Comment
  createdAt: string
  unread?: boolean
}

interface Filter {
  id: string
  name: string
}

// 状态管理
const router = useRouter()
const activeTab = ref('all')
const activeFilter = ref('time')
const showFilter = ref(false)
const loading = ref(false)
const error = ref('')

// 筛选选项
const filters = ref<Filter[]>([
  { id: 'time', name: '最新优先' },
  { id: 'popular', name: '热门优先' }
])

const activities = ref<Activity[]>([])

const mapNotificationToActivity = (type: Activity['type'], item: NotificationItem): Activity => ({
  id: Number(item.id) || Date.now(),
  type,
  user: {
    id: item.actorUserId,
    name: item.actorUserName || '用户',
    avatar: item.actorUserAvatar || ''
  },
  post: item.postId
    ? {
        id: item.postId,
        text: item.message || '',
        images: []
      }
    : undefined,
  comment: type === 'comment'
    ? {
        id: Number(item.id) || 0,
        text: item.message || ''
      }
    : undefined,
  createdAt: item.createTime,
  unread: Boolean(item.unread)
})

const loadActivities = async () => {
  loading.value = true
  error.value = ''
  try {
    const [likeItems, commentItems, followItems] = await Promise.all([
      getLikeAndFavoriteNotifications(),
      getCommentNotifications(),
      getFollowNotifications()
    ])

    activities.value = [
      ...likeItems.map(item => mapNotificationToActivity('like', item)),
      ...commentItems.map(item => mapNotificationToActivity('comment', item)),
      ...followItems.map(item => mapNotificationToActivity('follow', item))
    ]
  } catch (e: any) {
    console.error('加载动态失败:', e)
    error.value = e?.message || '动态加载失败，请稍后重试'
    activities.value = []
  } finally {
    loading.value = false
  }
}

// 筛选动态
const filteredActivities = computed(() => {
  let filtered = activities.value
  
  // 按类型筛选
  if (activeTab.value !== 'all') {
    filtered = filtered.filter(activity => activity.type === activeTab.value)
  }
  
  // 按时间或热门排序
  if (activeFilter.value === 'time') {
    filtered = [...filtered].sort((a, b) => 
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  } else {
    filtered = [...filtered].sort((a, b) => {
      const unreadScore = Number(Boolean(b.unread)) - Number(Boolean(a.unread))
      if (unreadScore !== 0) return unreadScore
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })
  }
  
  return filtered
})

// 获取动作文本
const getActionText = (activity: Activity): string => {
  switch (activity.type) {
    case 'like':
      return activity.post?.text?.includes('收藏') ? '收藏了你的帖子' : '赞了你的帖子'
    case 'comment':
      return '评论了你的帖子'
    case 'follow':
      return '关注了你'
    case 'post':
      return '发布了新内容'
    default:
      return ''
  }
}

const openActivity = (activity: Activity) => {
  if (activity.post?.id) {
    router.push(`/posts/${activity.post.id}`)
    return
  }
  if (activity.user.id) {
    router.push(`/user/${activity.user.id}`)
  }
}

// 格式化时间
const formatTime = (dateString: string): string => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 60) {
    return `${minutes}分钟前`
  } else if (hours < 24) {
    return `${hours}小时前`
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

onMounted(() => {
  void loadActivities()
})
</script>

<style scoped>
.activity-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 60px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.page-header h1 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background-color: #f5f5f5;
  border: none;
  border-radius: 16px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.filter-btn:hover {
  background-color: #e0e0e0;
}

.filter-btn svg {
  width: 16px;
  height: 16px;
}

/* 筛选菜单 */
.filter-menu {
  position: absolute;
  top: 60px;
  right: 16px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 101;
  overflow: hidden;
}

.filter-option {
  display: block;
  width: 100%;
  padding: 12px 20px;
  background: none;
  border: none;
  font-size: 14px;
  color: #666;
  text-align: left;
  cursor: pointer;
  transition: all 0.3s;
}

.filter-option:hover {
  background-color: #f5f5f5;
}

.filter-option.active {
  color: #1677ff;
  background-color: #f0f9ff;
}

/* 动态分类标签 */
.activity-tabs {
  display: flex;
  background-color: white;
  padding: 0 16px;
  border-bottom: 1px solid #e0e0e0;
}

.tab-btn {
  flex: 1;
  padding: 12px 0;
  background: none;
  border: none;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: color 0.3s;
}

.tab-btn.active {
  color: #333;
  font-weight: 500;
  position: relative;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 2px;
  background-color: #333;
  border-radius: 1px;
}

/* 动态列表 */
.activity-list {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 动态项 */
.activity-item {
  display: flex;
  background-color: white;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* 动态图标 */
.activity-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f9ff;
  border-radius: 50%;
  margin-right: 12px;
  flex-shrink: 0;
}

.activity-icon svg {
  width: 20px;
  height: 20px;
  color: #1677ff;
}

/* 动态内容 */
.activity-content {
  flex: 1;
}

.activity-text {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 8px;
  line-height: 1.4;
}

.user-name {
  font-weight: 500;
  color: #333;
  margin-right: 6px;
}

.activity-action {
  color: #666;
  margin-right: 6px;
}

.activity-time {
  font-size: 12px;
  color: #999;
}

/* 帖子预览 */
.post-preview {
  background-color: #f5f5f5;
  border-radius: 8px;
  padding: 8px;
  margin-top: 8px;
}

.post-text {
  font-size: 13px;
  color: #666;
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-images {
  display: flex;
  gap: 4px;
  position: relative;
}

.post-image {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  object-fit: cover;
}

.more-images {
  position: absolute;
  right: 4px;
  top: 0;
  width: 50px;
  height: 50px;
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
}

/* 评论内容 */
.comment-content {
  background-color: #f0f9ff;
  border-radius: 8px;
  padding: 8px;
  margin-top: 8px;
}

.comment-text {
  font-size: 13px;
  color: #333;
  margin: 0;
  line-height: 1.4;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state svg {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 16px;
  margin: 0 0 8px 0;
}

.empty-state span {
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .activity-icon {
    width: 32px;
    height: 32px;
    margin-right: 10px;
  }
  
  .activity-icon svg {
    width: 18px;
    height: 18px;
  }
  
  .post-image {
    width: 40px;
    height: 40px;
  }
  
  .more-images {
    width: 40px;
    height: 40px;
    font-size: 12px;
  }
}
</style>
