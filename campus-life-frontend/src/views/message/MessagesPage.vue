<template>
  <div class="messages-container">
    <!-- 消息标题栏 -->
    <div class="message-header-bar">
      <h1 class="page-title">消息</h1>
      <div class="header-actions">
        <button class="action-btn" @click="handleSearchClick">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
        </button>
        <button class="action-btn" @click="handleAddClick">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
        </button>
      </div>
      <div v-if="searchLoading && conversationSearchKeyword" class="search-loading">正在搜索相关记录...</div>
    </div>

    <div v-if="showConversationSearch" class="conversation-search-wrap">
      <div class="conversation-search-box">
        <input
          ref="conversationSearchInput"
          v-model.trim="conversationSearchKeyword"
          class="conversation-search-input"
          type="text"
          placeholder="搜索相关对话消息"
        >
        <button
          v-if="conversationSearchKeyword"
          class="conversation-clear-btn"
          @click="conversationSearchKeyword = ''"
        >
          清空
        </button>
      </div>
    </div>

    <!-- 三个快捷按钮 -->
    <div class="quick-actions">
      <button class="quick-action-btn" @click="openLikesPage">
        <div class="action-icon likes-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
          </svg>
        </div>
        <span class="action-label">赞和收藏</span>
        <span v-if="unreadLikesCount > 0" class="action-badge">{{ unreadLikesCount >= 100 ? '99+' : unreadLikesCount }}</span>
      </button>
      <button class="quick-action-btn" @click="openFollowsPage">
        <div class="action-icon follows-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <span class="action-label">新增关注</span>
        <span v-if="unreadFollowsCount > 0" class="action-badge">{{ unreadFollowsCount >= 100 ? '99+' : unreadFollowsCount }}</span>
      </button>
      <button class="quick-action-btn" @click="openCommentsPage">
        <div class="action-icon comments-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
        </div>
        <span class="action-label">评论和@</span>
        <span v-if="unreadCommentsCount > 0" class="action-badge">{{ unreadCommentsCount >= 100 ? '99+' : unreadCommentsCount }}</span>
      </button>
      <button class="quick-action-btn" @click="openSystemPage">
        <div class="action-icon system-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
            <path d="M9 12l2 2 4-4"></path>
          </svg>
        </div>
        <span class="action-label">系统通知</span>
        <span v-if="unreadSystemCount > 0" class="action-badge">{{ unreadSystemCount >= 100 ? '99+' : unreadSystemCount }}</span>
      </button>
    </div>

    <!-- 消息列表 -->
    <div class="message-list">
      <div 
        v-for="message in displayedMessages" 
        :key="message.id"
        :class="['message-item', { 'unread': message.unread }]"
        @click="handleMessageItemClick(message)"
      >
        <!-- 用户头像 -->
        <div class="avatar-container">
          <img 
            :src="message.avatarUrl" 
            :alt="message.name"
            class="avatar"
          >
          <div v-if="message.isOnline" class="online-indicator"></div>
        </div>

        <!-- 消息内容 -->
        <div class="message-content">
          <div class="message-header">
            <span class="user-name">{{ message.name }}</span>
            <span class="message-time">{{ formatTime(message.time) }}</span>
          </div>
          
          <div class="message-preview">
            <span v-if="message.unread" class="unread-dot"></span>
            <span class="message-text">{{ getMessagePreview(message) }}</span>
            <span v-if="message.unreadCount > 1" class="unread-count">
              {{ message.unreadCount }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="displayedMessages.length === 0" class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
      </svg>
      <p>{{ showConversationSearch && conversationSearchKeyword ? '未找到相关记录（私聊/评论）' : '暂无消息' }}</p>
    </div>

    <!-- 聊天设置菜单 -->
    <div v-if="showSettings" class="settings-menu">
      <button class="setting-option" @click="markAllAsRead(); showSettings = false">
        全部标记为已读
      </button>
      <button class="setting-option" @click="clearAllMessages(); showSettings = false">
        清空所有消息
      </button>
      <button class="setting-option cancel" @click="showSettings = false">
        取消
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  getCommentNotifications,
  getConversations,
  getFollowNotifications,
  getMessagesByUserId,
  getLikeAndFavoriteNotifications,
  getSystemNotifications,
  markConversationAsRead,
  type Conversation,
  type NotificationItem
} from '@/api/message'
import { showConfirm } from '@/utils/dialog'
import { getStoredUserInfoObject } from '@/utils/authStorage'
import { notify } from '@/utils/notify'

// 类型定义
interface Message {
  id: number | string
  type: 'chat' | 'notification' | 'comment'
  name: string
  avatarUrl: string
  message: string
  time: string
  unread: boolean
  unreadCount: number
  isOnline?: boolean
  group?: boolean
  groupCount?: number
  conversationId?: string
  otherUserId?: number
  postId?: number
}

// 状态管理
const router = useRouter()
const showSettings = ref(false)
const loading = ref(false)
const messages = ref<Message[]>([])
const searchResults = ref<Message[]>([])
const showConversationSearch = ref(false)
const conversationSearchKeyword = ref('')
const conversationSearchInput = ref<HTMLInputElement>()
const searchLoading = ref(false)
let searchTimer: number | null = null
let searchVersion = 0
let pollInterval: number | null = null
const unreadLikesCountRef = ref(0)
const unreadFollowsCountRef = ref(0)
const unreadCommentsCountRef = ref(0)
const unreadSystemCountRef = ref(0)
const LAST_SEEN_LIKES_KEY = 'lastSeenLikesNotificationsAt'
const LAST_SEEN_FOLLOWS_KEY = 'lastSeenFollowsNotificationsAt'
const LAST_SEEN_COMMENTS_KEY = 'lastSeenCommentsNotificationsAt'
const LAST_SEEN_SYSTEM_KEY = 'lastSeenSystemNotificationsAt'

// 格式化时间
const formatTime = (dateString: string): string => {
  if (!dateString) return ''
  const date = new Date(dateString)
  if (Number.isNaN(date.getTime())) return ''

  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const messageDayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diffDays = Math.floor((todayStart.getTime() - messageDayStart.getTime()) / (1000 * 60 * 60 * 24))

  if (diffDays === 0) {
    return '今天'
  }
  if (diffDays === 1) {
    return '昨天'
  }
  if (diffDays > 1 && diffDays < 7) {
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    return weekdays[date.getDay()]
  }
  return `${date.getFullYear()}年${(date.getMonth() + 1).toString().padStart(2, '0')}月${date.getDate().toString().padStart(2, '0')}日`
}

// 加载会话列表
const loadConversations = async () => {
  if (loading.value) return
  try {
    loading.value = true
    const conversations = await getConversations()
    
    messages.value = conversations.map((conv: Conversation) => ({
      id: conv.otherUserId,
      type: 'chat' as const,
      name: conv.otherUserName || '用户',
      avatarUrl: conv.otherUserAvatar || '',
      message: conv.lastMessage || '',
      time: conv.lastMessageTime,
      unread: conv.unreadCount > 0,
      unreadCount: conv.unreadCount,
      isOnline: conv.isOnline || false,
      conversationId: conv.conversationId,
      otherUserId: conv.otherUserId
    }))
  } catch (error: any) {
    console.error('加载会话列表失败:', error)
    messages.value = []
  } finally {
    loading.value = false
  }
}

// 轮询更新会话列表
const startPolling = () => {
  if (pollInterval) return
  pollInterval = window.setInterval(() => {
    loadConversations()
    loadTopNotificationCounts()
  }, 5000)  // 每5秒轮询一次
}

// 停止轮询
const stopPolling = () => {
  if (pollInterval) {
    clearInterval(pollInterval)
    pollInterval = null
  }
}

// 计算未读消息数量
const unreadLikesCount = computed(() => {
  return unreadLikesCountRef.value
})

const unreadFollowsCount = computed(() => {
  return unreadFollowsCountRef.value
})

const unreadCommentsCount = computed(() => {
  return unreadCommentsCountRef.value
})

const unreadSystemCount = computed(() => {
  return unreadSystemCountRef.value
})

const displayedMessages = computed(() => {
  if (showConversationSearch.value && conversationSearchKeyword.value.trim()) {
    return searchResults.value
  }
  return messages.value
})

const normalize = (value?: string) => (value || '').toLowerCase()

const resolveCurrentUserId = () => {
  const userInfo = getStoredUserInfoObject<any>()
  return Number(userInfo?.id || userInfo?.userId || 0)
}

const sortByTimeDesc = (list: Message[]) => {
  return list.sort((a, b) => {
    const ta = new Date(a.time).getTime()
    const tb = new Date(b.time).getTime()
    return (Number.isNaN(tb) ? 0 : tb) - (Number.isNaN(ta) ? 0 : ta)
  })
}

const buildCommentSearchResults = (keywordLower: string, comments: NotificationItem[]): Message[] => {
  return comments
    .filter(item => normalize(item.actorUserName).includes(keywordLower) || normalize(item.message).includes(keywordLower))
    .map(item => ({
      id: `comment-${item.id}`,
      type: 'comment' as const,
      name: item.actorUserName || '用户',
      avatarUrl: item.actorUserAvatar || '',
      message: item.message || '',
      time: item.createTime,
      unread: Boolean(item.unread),
      unreadCount: 0,
      postId: item.postId,
      otherUserId: item.actorUserId
    }))
}

const buildChatSearchResults = async (keywordLower: string): Promise<Message[]> => {
  const chatResults: Message[] = []
  const currentUserId = resolveCurrentUserId()
  const tasks = messages.value.map(async (conv) => {
    if (!conv.otherUserId) {
      return
    }
    try {
      const detail = await getMessagesByUserId(conv.otherUserId, 1, 200)
      const matched = detail.messages.filter(msg => {
        return normalize(msg.content).includes(keywordLower) || normalize(conv.name).includes(keywordLower)
      })
      matched.forEach(msg => {
        chatResults.push({
          id: `chat-${msg.id}`,
          type: 'chat',
          name: conv.name,
          avatarUrl: conv.avatarUrl,
          message: msg.content || '',
          time: msg.createTime,
          unread: msg.status === 0 && msg.toUserId === currentUserId,
          unreadCount: 0,
          isOnline: conv.isOnline || false,
          conversationId: detail.conversationId || conv.conversationId,
          otherUserId: conv.otherUserId
        })
      })
    } catch (e) {
      console.error('搜索私聊记录失败:', e)
    }
  })
  await Promise.allSettled(tasks)
  return chatResults
}

const searchAllRelatedRecords = async () => {
  const keyword = conversationSearchKeyword.value.trim()
  if (!keyword) {
    searchResults.value = []
    return
  }
  const currentVersion = ++searchVersion
  searchLoading.value = true
  try {
    const [comments, chatList] = await Promise.all([
      getCommentNotifications().catch(() => [] as NotificationItem[]),
      buildChatSearchResults(normalize(keyword))
    ])
    if (currentVersion !== searchVersion) {
      return
    }
    searchResults.value = sortByTimeDesc([
      ...chatList,
      ...buildCommentSearchResults(normalize(keyword), comments)
    ])
  } finally {
    if (currentVersion === searchVersion) {
      searchLoading.value = false
    }
  }
}

const loadTopNotificationCounts = async () => {
  try {
    const [likes, follows, comments, systemList] = await Promise.all([
      getLikeAndFavoriteNotifications(),
      getFollowNotifications(),
      getCommentNotifications(),
      getSystemNotifications()
    ])
    unreadLikesCountRef.value = countUnreadNotifications(likes, LAST_SEEN_LIKES_KEY)
    unreadFollowsCountRef.value = countUnreadNotifications(follows, LAST_SEEN_FOLLOWS_KEY)
    unreadCommentsCountRef.value = countUnreadNotifications(comments, LAST_SEEN_COMMENTS_KEY)
    unreadSystemCountRef.value = countUnreadNotifications(systemList, LAST_SEEN_SYSTEM_KEY)
  } catch (error) {
    console.error('加载顶部互动通知数量失败:', error)
  }
}

const countUnreadNotifications = (
  items: Array<{ createTime: string; unread?: boolean }>,
  storageKey: string
): number => {
  const lastSeenRaw = sessionStorage.getItem(storageKey)
  const lastSeenTime = lastSeenRaw ? new Date(lastSeenRaw).getTime() : Number.NaN
  return items.filter(item => {
    if (item.unread === false) {
      return false
    }
    if (Number.isNaN(lastSeenTime)) {
      return true
    }
    const itemTime = new Date(item.createTime).getTime()
    if (Number.isNaN(itemTime)) {
      return true
    }
    return itemTime > lastSeenTime
  }).length
}

const markSeen = (storageKey: string) => {
  sessionStorage.setItem(storageKey, new Date().toISOString())
}

const openLikesPage = () => {
  markSeen(LAST_SEEN_LIKES_KEY)
  unreadLikesCountRef.value = 0
  router.push('/message/likes')
}

const openFollowsPage = () => {
  markSeen(LAST_SEEN_FOLLOWS_KEY)
  unreadFollowsCountRef.value = 0
  router.push('/message/follows')
}

const openCommentsPage = () => {
  markSeen(LAST_SEEN_COMMENTS_KEY)
  unreadCommentsCountRef.value = 0
  router.push('/message/comments')
}

const openSystemPage = () => {
  markSeen(LAST_SEEN_SYSTEM_KEY)
  unreadSystemCountRef.value = 0
  router.push('/message/system')
}


// 获取消息预览文本
const getMessagePreview = (message: Message): string => {
  if (message.group && message.groupCount) {
    return `${message.message}`
  }
  return message.message
}

// 标记消息为已读
const markAsRead = async (message: Message) => {
  if (message.unread && message.conversationId) {
    try {
      await markConversationAsRead(message.conversationId)
      message.unread = false
      message.unreadCount = 0
    } catch (e) {
      console.error('标记会话已读失败:', e)
    }
  }
  // 如果是私信，导航到聊天详情页
  if (message.type === 'chat' && message.otherUserId) {
    router.push({
      path: `/messages/${message.otherUserId}`
    })
  }
}

const handleMessageItemClick = async (message: Message) => {
  if (message.type === 'comment') {
    if (message.postId) {
      router.push(`/posts/${message.postId}`)
    }
    return
  }
  await markAsRead(message)
}

// 处理搜索点击
const handleSearchClick = () => {
  showConversationSearch.value = !showConversationSearch.value
  if (!showConversationSearch.value) {
    conversationSearchKeyword.value = ''
    searchResults.value = []
    searchLoading.value = false
    return
  }
  nextTick(() => conversationSearchInput.value?.focus())
}

// 处理添加点击
const handleAddClick = () => {
  // 可以打开添加好友或创建群聊的对话框
  router.push('/add-friend')
}

// 全部标记为已读
const markAllAsRead = async () => {
  const unreadConversations = messages.value.filter(msg => msg.unread && msg.conversationId)
  if (unreadConversations.length === 0) return

  const results = await Promise.allSettled(
    unreadConversations.map(msg => markConversationAsRead(msg.conversationId as string))
  )

  const succeededConversationIds = new Set(
    results.flatMap((result, index) => (
      result.status === 'fulfilled' ? [unreadConversations[index].conversationId as string] : []
    ))
  )

  messages.value.forEach(msg => {
    if (msg.conversationId && succeededConversationIds.has(msg.conversationId)) {
      msg.unread = false
      msg.unreadCount = 0
    }
  })

  if (succeededConversationIds.size !== unreadConversations.length) {
    notify('部分会话标记已读失败，已保留未同步状态')
  }

  await loadConversations()
}

// 清空所有消息
const clearAllMessages = async () => {
  if (await showConfirm('当前版本暂不支持清空服务端消息记录，是否关闭此提示？')) {
    notify('暂不支持清空消息记录')
  }
}

// 组件挂载
onMounted(() => {
  loadConversations()
  loadTopNotificationCounts()
  startPolling()
})

// 组件卸载
onUnmounted(() => {
  stopPolling()
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
})

watch(conversationSearchKeyword, (val) => {
  const keyword = val.trim()
  if (!keyword) {
    searchResults.value = []
    searchLoading.value = false
    return
  }
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = window.setTimeout(() => {
    searchAllRelatedRecords()
  }, 300)
})
</script>

<style scoped>
.messages-container {
  min-height: 100vh;
  background-color: var(--bg-secondary);
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background-color: var(--bg-primary);
  box-shadow: var(--shadow-light);
  position: sticky;
  top: 0;
  z-index: 100;
}

.page-header h1 {
  font-size: var(--font-xxl);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.header-actions {
  position: relative;
}

.settings-btn {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  color: var(--text-secondary);
}

.settings-btn svg {
  width: 20px;
  height: 20px;
}

/* 消息标题栏 */
.message-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: var(--bg-primary);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-btn {
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
}

.action-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.action-btn svg {
  width: 20px;
  height: 20px;
}

.conversation-search-wrap {
  background-color: var(--bg-primary);
  padding: 0 16px 12px;
  border-bottom: 1px solid var(--border-light);
}

.conversation-search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: var(--bg-secondary);
  border-radius: 10px;
  padding: 8px 10px;
}

.conversation-search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text-primary);
  font-size: var(--font-md);
}

.conversation-clear-btn {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--font-sm);
  cursor: pointer;
}

.search-loading {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
}

/* 快捷操作按钮区域 */
.quick-actions {
  display: flex;
  justify-content: space-around;
  padding: 16px;
  background-color: var(--bg-primary);
  border-bottom: 1px solid var(--border-normal);
}

.quick-action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  cursor: pointer;
  position: relative;
  padding: 0;
  flex: 1;
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
}

.action-icon svg {
  width: 24px;
  height: 24px;
}

.likes-icon {
  background-color: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}

.follows-icon {
  background-color: rgba(24, 144, 255, 0.1);
  color: #1890ff;
}

.comments-icon {
  background-color: rgba(82, 196, 26, 0.1);
  color: #52c41a;
}

.system-icon {
  background-color: rgba(250, 173, 20, 0.12);
  color: #d48806;
}

.quick-action-btn:hover .action-icon {
  transform: scale(1.05);
}

.action-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.action-badge {
  position: absolute;
  top: -4px;
  right: 8px;
  background-color: #ff4d4f;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

/* 消息列表 */
.message-list {
  padding: 0;
}

/* 消息项 */
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

/* 头像容器 */
.avatar-container {
  position: relative;
  margin-right: 12px;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.online-indicator {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 14px;
  height: 14px;
  background-color: var(--success-color);
  border: 2px solid var(--bg-primary);
  border-radius: 50%;
}

/* 消息内容 */
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-count {
  background-color: var(--error-color);
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
  flex-shrink: 0;
}

/* 空状态 */
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
  margin: 0 0 8px 0;
  color: var(--text-secondary);
}

.empty-state span {
  font-size: var(--font-sm);
  color: var(--text-tertiary);
}

/* 设置菜单 */
.settings-menu {
  position: absolute;
  top: 60px;
  right: 16px;
  background-color: var(--bg-primary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-normal);
  z-index: 101;
  overflow: hidden;
  min-width: 160px;
}

.setting-option {
  display: block;
  width: 100%;
  padding: 12px 16px;
  background: none;
  border: none;
  font-size: var(--font-md);
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  transition: background-color 0.3s;
}

.setting-option:hover {
  background-color: var(--bg-secondary);
}

.setting-option.cancel {
  color: var(--text-tertiary);
  border-top: 1px solid var(--border-light);
  margin-top: 4px;
}

.setting-option.cancel:hover {
  background-color: var(--bg-secondary);
  color: var(--text-secondary);
}

/* 徽章样式 */
.badge {
  background-color: var(--error-color);
  color: white;
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 8px;
  min-width: 16px;
  text-align: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.badge-large {
  padding: 1px 6px;
}

/* 响应式设计 */
@media (max-width: 375px) {
  .message-item {
    padding: 10px 12px;
  }
  
  .avatar {
    width: 44px;
    height: 44px;
  }
  
  .user-name {
    font-size: var(--font-sm);
  }
  
  .message-text {
    font-size: var(--font-xs);
  }
}
</style>
