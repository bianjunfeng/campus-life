<template>
  <div class="follows-container">
    <!-- 顶部标题栏 -->
    <div class="page-header-bar">
      <button class="back-btn" @click="handleBack">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
      </button>
      <h1 class="page-title">新增关注</h1>
      <div class="header-placeholder"></div>
    </div>

    <!-- 消息列表 -->
    <div class="message-list">
      <div 
        v-for="message in messages" 
        :key="message.id"
        :class="['message-item', { 'unread': message.unread }]"
        @click="handleMessageClick(message)"
      >
        <!-- 用户头像 -->
        <div class="avatar-container" @click.stop="goToUser(message)">
          <img 
            :src="message.avatarUrl" 
            :alt="message.name"
            class="avatar"
          >
        </div>

        <!-- 消息内容 -->
        <div class="message-content">
          <div class="message-header">
            <span class="user-name" @click.stop="goToUser(message)">{{ message.name }}</span>
            <span class="message-time">{{ formatTime(message.time) }}</span>
          </div>
          
          <div class="message-preview">
            <span v-if="message.unread" class="unread-dot"></span>
            <span class="message-text">{{ message.message }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="messages.length === 0" class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
        <circle cx="9" cy="7" r="4"></circle>
        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
      </svg>
      <p>暂无新增关注</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getFollowNotifications } from '@/api/message'

interface Message {
  id: string
  name: string
  avatarUrl: string
  message: string
  time: string
  unread: boolean
  userId?: number
}

const router = useRouter()

const messages = ref<Message[]>([])
const LAST_SEEN_KEY = 'lastSeenFollowsNotificationsAt'

const handleBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  router.push('/message')
}

const resolveUnreadState = (createTime: string, unread?: boolean, seenAt?: string) => {
  if (unread === false) return false
  const seenTime = seenAt ? new Date(seenAt).getTime() : Number.NaN
  if (Number.isNaN(seenTime)) return true
  const itemTime = new Date(createTime).getTime()
  if (Number.isNaN(itemTime)) return true
  return itemTime > seenTime
}

// 格式化时间
const formatTime = (dateString: string): string => {
  if (!dateString) return ''
  const date = new Date(dateString)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const messageDayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diffDays = Math.floor((todayStart.getTime() - messageDayStart.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays > 1 && diffDays < 7) {
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    return weekdays[date.getDay()]
  }
  return `${date.getFullYear()}年${(date.getMonth() + 1).toString().padStart(2, '0')}月${date.getDate().toString().padStart(2, '0')}日`
}

// 处理消息点击
const handleMessageClick = (message: Message) => {
  message.unread = false
  goToUser(message)
}

const goToUser = (message: Message) => {
  if (message.userId) {
    router.push({
      path: `/user/${message.userId}`,
      query: {
        name: message.name,
        avatarUrl: message.avatarUrl
      }
    })
  }
}

const loadNotifications = async () => {
  try {
    const data = await getFollowNotifications()
    const seenAt = new Date().toISOString()
    sessionStorage.setItem(LAST_SEEN_KEY, seenAt)
    messages.value = data.map(item => ({
      id: item.id,
      name: item.actorUserName || '用户',
      avatarUrl: item.actorUserAvatar || 'https://via.placeholder.com/50x50?text=U',
      message: item.message || '关注了你',
      time: item.createTime,
      unread: resolveUnreadState(item.createTime, item.unread, seenAt),
      userId: item.actorUserId
    }))
  } catch (error) {
    console.error('加载新增关注通知失败:', error)
    messages.value = []
  }
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped>
.follows-container {
  min-height: 100vh;
  background-color: var(--bg-secondary);
}

/* 顶部标题栏 */
.page-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: var(--bg-primary);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.back-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: none;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.3s;
  color: var(--text-primary);
  flex-shrink: 0;
}

.back-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  flex: 1;
  text-align: center;
}

.header-placeholder {
  width: 36px;
  flex-shrink: 0;
}

.message-list {
  padding: 0;
}

.message-item {
  display: flex;
  padding: 12px 16px;
  background-color: var(--bg-primary);
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
  transition: background-color 0.2s;
}

.message-item:hover {
  background-color: var(--bg-secondary);
}

.message-item.unread {
  background-color: rgba(22, 119, 255, 0.05);
}

.avatar-container {
  position: relative;
  margin-right: 12px;
  cursor: pointer;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.user-name {
  font-weight: 500;
  color: var(--text-primary);
  font-size: var(--font-md);
  cursor: pointer;
}

.message-time {
  font-size: var(--font-xs);
  color: var(--text-tertiary);
}

.message-preview {
  display: flex;
  align-items: center;
  gap: 6px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background-color: var(--primary-color);
  border-radius: 50%;
  flex-shrink: 0;
}

.message-text {
  flex: 1;
  font-size: var(--font-sm);
  color: var(--text-secondary);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--text-tertiary);
}

.empty-state svg {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  font-size: var(--font-xl);
  margin: 0;
  color: var(--text-secondary);
}
</style>
