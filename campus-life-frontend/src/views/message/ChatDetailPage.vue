<template>
  <div class="chat-detail-container">
    <!-- 聊天页面头部 -->
    <header class="chat-header">
      <button class="back-btn" @click="handleBack">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 19l-7-7 7-7"></path>
        </svg>
      </button>
      
      <div class="chat-info">
        <div class="user-info">
          <img 
            :src="currentUser.avatarUrl" 
            :alt="currentUser.name" 
            class="avatar"
            @click.stop="navigateToUserProfile"
            style="cursor: pointer"
          >
          <div>
            <span class="user-name" @click.stop="navigateToUserProfile" style="cursor: pointer">{{ currentUser.name }}</span>
            <span v-if="currentUser.isOnline" class="status">在线</span>
          </div>
        </div>
      </div>
      
      <button class="more-btn" @click="showMoreOptions = !showMoreOptions">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="1"></circle>
          <circle cx="19" cy="12" r="1"></circle>
          <circle cx="5" cy="12" r="1"></circle>
        </svg>
      </button>
    </header>

    <!-- 消息列表区域 -->
    <div class="message-container" ref="messageContainer">
      <!-- 消息列表 -->
      <div v-for="(msg, index) in chatMessages" :key="index" class="message-item">
        <div class="message-top-time">{{ formatMessageDateLabel(msg.createTime) }}</div>
        <div :class="['message-wrapper', { 'sent': msg.sent }]">
          <div class="message-content-wrapper">
            <div v-if="!msg.sent" class="message-avatar-left">
              <img :src="currentUser.avatarUrl" :alt="currentUser.name" class="avatar-small">
            </div>
            
            <div :class="['message-content', { 'sent': msg.sent }]">
              <p>{{ msg.text }}</p>
            </div>
            
            <div v-if="msg.sent" class="message-avatar-right">
              <img :src="selfUser.avatarUrl" :alt="selfUser.name" class="avatar-small">
            </div>
          </div>
        </div>
      </div>

      <!-- 正在输入提示 -->
      <div v-if="isTyping" class="typing-indicator">
        <img :src="currentUser.avatarUrl" :alt="currentUser.name" class="avatar-small">
        <div class="typing-bubble">
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
        </div>
      </div>
    </div>

    <!-- 消息输入区域 -->
    <div class="input-container">
      <div class="input-wrapper">
        <textarea 
          v-model="newMessage" 
          placeholder="输入消息..."
          rows="1"
          class="message-input"
          @keydown.enter.prevent="handleEnterKey"
        ></textarea>
      </div>
      
      <button class="send-btn" @click="sendMessage" :disabled="!newMessage.trim()">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13"></line>
          <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
        </svg>
      </button>
    </div>

    <!-- 更多选项菜单 -->
    <div v-if="showMoreOptions" class="more-options-menu">
      <button class="option-item" @click="showMoreOptions = false">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path>
        </svg>
        <span>收藏对话</span>
      </button>
      <button class="option-item" @click="showMoreOptions = false">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path>
        </svg>
        <span>保存聊天记录</span>
      </button>
      <button class="option-item" @click="showMoreOptions = false">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
          <circle cx="12" cy="7" r="4"></circle>
        </svg>
        <span>个人资料</span>
      </button>
      <button class="option-item cancel" @click="showMoreOptions = false">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
        <span>取消</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { sendMessage as sendMessageApi, syncMessagesByUserId, markConversationAsRead, type Message } from '@/api/message'
import { messageSocket } from '@/api/messageSocket'
import { getStoredUserInfoObject } from '@/utils/authStorage'
import { notify } from '@/utils/notify'

// 类型定义
interface ChatMessage {
  id?: number
  text: string
  time: string
  sent: boolean
  createTime?: string
}

interface User {
  id: number
  name: string
  avatarUrl: string
  isOnline: boolean
}

// 路由相关
const router = useRouter()
const route = useRoute()

// 状态管理
const showMoreOptions = ref(false)
const newMessage = ref('')
const isTyping = ref(false)
const messageContainer = ref<HTMLElement>()
const loading = ref(false)
const conversationId = ref<string>('')
const currentUserId = ref<number>(0)
const otherUserId = ref<number>(0)
let handleDocumentClick: ((event: MouseEvent) => void) | null = null
let unsubscribeMessage: (() => void) | null = null
let unsubscribeReconnect: (() => void) | null = null

// 对方用户信息
const currentUser = ref<User>({
  id: 0,
  name: '',
  avatarUrl: '',
  isOnline: false
})
const selfUser = ref<User>({
  id: 0,
  name: '',
  avatarUrl: '',
  isOnline: false
})

// 聊天消息数据
const chatMessages = ref<ChatMessage[]>([])

// 格式化日期
const formatDate = (date: Date): string => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const messageDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diffDays = Math.floor((today.getTime() - messageDate.getTime()) / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// 每条消息顶部日期标签：一周内显示“昨天/星期几”，一周外显示“年月日”
const formatMessageDateLabel = (date?: string): string => {
  if (!date) return ''
  const d = new Date(date)
  if (Number.isNaN(d.getTime())) return ''

  const now = new Date()
  const startNow = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const startMsg = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const diffDays = Math.floor((startNow.getTime() - startMsg.getTime()) / (1000 * 60 * 60 * 24))
  const hh = d.getHours().toString().padStart(2, '0')
  const mm = d.getMinutes().toString().padStart(2, '0')
  const minuteTime = `${hh}:${mm}`

  if (diffDays === 0) return `今天 ${minuteTime}`
  if (diffDays === 1) return `昨天 ${minuteTime}`
  if (diffDays > 1 && diffDays < 7) {
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    return `${weekdays[d.getDay()]} ${minuteTime}`
  }
  return `${d.getFullYear()}年${(d.getMonth() + 1).toString().padStart(2, '0')}月${d.getDate().toString().padStart(2, '0')}日 ${minuteTime}`
}

// 格式化时间
const formatTime = (date?: Date | string): string => {
  const d = date ? (typeof date === 'string' ? new Date(date) : date) : new Date()
  const hours = d.getHours().toString().padStart(2, '0')
  const minutes = d.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

const resolveSelfUser = () => {
  const userInfo = getStoredUserInfoObject<any>()
  if (userInfo) {
    currentUserId.value = Number(userInfo.id || userInfo.userId || 0)
    selfUser.value = {
      id: currentUserId.value,
      name: userInfo.username || userInfo.nickName || userInfo.name || '我',
      avatarUrl: userInfo.avatarUrl || userInfo.icon || '',
      isOnline: true
    }
  }
}

const isCurrentConversationMessage = (message: Message) => {
  if (conversationId.value && message.conversationId === conversationId.value) {
    return true
  }
  return (
    (message.fromUserId === currentUserId.value && message.toUserId === otherUserId.value) ||
    (message.fromUserId === otherUserId.value && message.toUserId === currentUserId.value)
  )
}

const appendMessage = (message: Message) => {
  if (!message?.id || chatMessages.value.some(item => item.id === message.id)) {
    return false
  }
  if (!conversationId.value) {
    conversationId.value = message.conversationId
  }
  chatMessages.value.push({
    id: message.id,
    text: message.content,
    time: formatTime(message.createTime),
    sent: message.fromUserId === currentUserId.value,
    createTime: message.createTime
  })
  return true
}

const syncMissingMessages = async () => {
  if (!otherUserId.value) return
  const latestId = chatMessages.value.reduce((max, message) => Math.max(max, message.id || 0), 0)
  if (!latestId) {
    await loadMessages()
    return
  }
  try {
    const result = await syncMessagesByUserId(otherUserId.value, { afterId: latestId, limit: 200 })
    if (result.conversationId) {
      conversationId.value = result.conversationId
    }
    let appended = false
    result.messages.forEach((message: Message) => {
      if (appendMessage(message)) {
        appended = true
      }
    })
    if (appended) {
      await markCurrentConversationAsRead()
      scrollToBottom()
    }
  } catch (error) {
    console.error('同步断线消息失败:', error)
  }
}

const markCurrentConversationAsRead = async () => {
  if (!conversationId.value) return
  try {
    await markConversationAsRead(conversationId.value)
  } catch (e) {
    console.error('标记已读失败:', e)
  }
}

// 发送消息
const sendMessage = async () => {
  if (!newMessage.value.trim() || loading.value) return
  
  const content = newMessage.value.trim()
  newMessage.value = ''
  loading.value = true
  
  try {
    let message: Message
    if (messageSocket.isReady()) {
      message = await messageSocket.sendChatMessage(otherUserId.value, content)
    } else {
      message = await sendMessageApi(otherUserId.value, content)
    }
    
    appendMessage(message)
    
    // 更新会话ID
    if (!conversationId.value) {
      conversationId.value = message.conversationId
    }
    
    // 自动滚动到底部
    scrollToBottom()
  } catch (error: any) {
    console.error('发送消息失败:', error)
    notify(error.message || '发送消息失败')
    newMessage.value = content  // 恢复输入内容
  } finally {
    loading.value = false
  }
}

// 处理回车键发送
const handleEnterKey = (e: KeyboardEvent) => {
  if (e.shiftKey) {
    // Shift+Enter 插入换行
    return
  } else {
    // Enter 发送消息
    sendMessage()
  }
}

// 返回上一页
const handleBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  router.push('/message')
}

// 跳转到用户个人主页
const navigateToUserProfile = () => {
  // 使用路由参数中的用户ID跳转到个人主页
  const userId = route.params.id as string
  router.push({
    path: `/user/${userId}`,
    query: {
      name: currentUser.value.name,
      avatarUrl: currentUser.value.avatarUrl
    }
  })
}

// 滚动到底部
const scrollToBottom = () => {
  setTimeout(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  }, 100)
}

// 加载消息历史
const loadMessages = async () => {
  if (!otherUserId.value) return
  
  try {
    loading.value = true
    resolveSelfUser()
    const result = await syncMessagesByUserId(otherUserId.value, { limit: 100 })
    
    conversationId.value = result.conversationId
    
    // 转换消息格式
    chatMessages.value = result.messages.map((msg: Message) => ({
      id: msg.id,
      text: msg.content,
      time: formatTime(msg.createTime),
      sent: msg.fromUserId === currentUserId.value,
      createTime: msg.createTime
    }))
    
    // 从消息中加载对方用户信息
    if (result.messages.length > 0) {
      loadOtherUserInfo(result.messages)
    } else {
      const queryName = route.query.name
      const queryAvatar = route.query.avatarUrl
      currentUser.value = {
        id: otherUserId.value,
        name: typeof queryName === 'string' && queryName ? queryName : `用户${otherUserId.value}`,
        avatarUrl: typeof queryAvatar === 'string' ? queryAvatar : '',
        isOnline: false
      }
    }
    
    // 标记为已读
    await markCurrentConversationAsRead()
    
    // 滚动到底部
    scrollToBottom()
  } catch (error: any) {
    console.error('加载消息失败:', error)
    chatMessages.value = []
  } finally {
    loading.value = false
  }
}

// 加载对方用户信息（从消息中获取）
const loadOtherUserInfo = (messages: Message[]) => {
  if (messages.length === 0) return
  
  // 从第一条消息中获取对方用户信息
  const firstMessage = messages[0]
  const otherUser = firstMessage.fromUserId === otherUserId.value 
    ? { name: firstMessage.fromUserName, avatar: firstMessage.fromUserAvatar }
    : { name: firstMessage.toUserName, avatar: firstMessage.toUserAvatar }
  
  currentUser.value = {
    id: otherUserId.value,
    name: otherUser.name || '用户',
    avatarUrl: otherUser.avatar || '',
    isOnline: false  // 在线状态需要额外实现
  }
}

// 监听路由参数变化
watch(() => route.params, async (newParams) => {
  const userId = newParams.id as string
  otherUserId.value = parseInt(userId)
  
  if (otherUserId.value) {
    // 加载消息（消息中包含用户信息）
    await loadMessages()

    messageSocket.connect().catch(error => {
      console.warn('消息WebSocket连接失败:', error)
    })
  }
}, { immediate: true })

// 组件挂载
onMounted(() => {
  // 滚动到底部
  scrollToBottom()
  
  // 点击外部关闭菜单
  handleDocumentClick = (event: MouseEvent) => {
    const target = event.target as HTMLElement
    if (!target.closest('.more-btn') && !target.closest('.more-options-menu')) {
      showMoreOptions.value = false
    }
  }
  
  document.addEventListener('click', handleDocumentClick)

  unsubscribeMessage = messageSocket.onChatMessage(async (message: Message) => {
    if (!isCurrentConversationMessage(message)) return
    if (appendMessage(message)) {
      await markCurrentConversationAsRead()
      scrollToBottom()
    }
  })

  unsubscribeReconnect = messageSocket.onReconnect(() => {
    syncMissingMessages()
  })
})

// 组件卸载
onUnmounted(() => {
  if (unsubscribeMessage) {
    unsubscribeMessage()
    unsubscribeMessage = null
  }
  if (unsubscribeReconnect) {
    unsubscribeReconnect()
    unsubscribeReconnect = null
  }
  if (handleDocumentClick) {
    document.removeEventListener('click', handleDocumentClick)
    handleDocumentClick = null
  }
})
</script>

<style scoped>
.chat-detail-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: var(--bg-secondary);
}

/* 聊天页面头部 */
.chat-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background-color: var(--bg-primary);
  box-shadow: var(--shadow-light);
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  color: var(--text-primary);
  margin-right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.chat-info {
  flex: 1;
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 8px;
}

.user-name {
  font-weight: 500;
  color: var(--text-primary);
  font-size: var(--font-md);
}

.status {
  display: block;
  font-size: var(--font-xs);
  color: var(--success-color);
  margin-top: 2px;
}

.more-btn {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.more-btn svg {
  width: 20px;
  height: 20px;
}

/* 消息容器 */
.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 日期分隔线 */
.date-divider {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 16px 0;
}

.date-divider span {
  background-color: rgba(0, 0, 0, 0.1);
  color: var(--text-tertiary);
  font-size: var(--font-xs);
  padding: 4px 12px;
  border-radius: 10px;
}

/* 消息包装器 */
.message-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 8px;
}

.message-top-time {
  text-align: center;
  font-size: var(--font-xs);
  color: var(--text-tertiary);
}

.message-wrapper {
  display: flex;
  justify-content: flex-start;
}

.message-wrapper.sent {
  justify-content: flex-end;
}

.message-content-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 76%;
}

.message-avatar-left {
  display: flex;
  align-items: flex-end;
}

.message-avatar-right {
  display: flex;
  align-items: flex-end;
}

.avatar-small {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

/* 消息内容 */
.message-content {
  max-width: 100%;
  min-width: 56px;
  position: relative;
  padding: 10px 12px;
  border-radius: 16px;
  background-color: var(--bg-primary);
  color: var(--text-primary);
  font-size: var(--font-md);
  word-wrap: break-word;
  box-shadow: var(--shadow-light);
}

.message-content.sent {
  background-color: var(--primary-color);
  color: white;
}

.message-content p {
  margin: 0 0 4px 0;
  line-height: 1.4;
}

.message-time {
  font-size: var(--font-xs);
  opacity: 0.7;
  text-align: right;
  display: block;
}

/* 正在输入提示 */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
}

.typing-bubble {
  background-color: var(--bg-primary);
  padding: 8px 16px;
  border-radius: 16px;
  box-shadow: var(--shadow-light);
  display: flex;
  gap: 4px;
  align-items: center;
}

.typing-dot {
  width: 6px;
  height: 6px;
  background-color: var(--text-tertiary);
  border-radius: 50%;
  animation: typing-bounce 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing-bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 输入区域 */
.input-container {
  display: flex;
  align-items: flex-end;
  padding: 12px 16px;
  background-color: var(--bg-primary);
  border-top: 1px solid var(--border-light);
  gap: 12px;
}

.input-wrapper {
  flex: 1;
  position: relative;
  max-height: 100px;
}

.message-input {
  width: 100%;
  padding: 10px 16px;
  border-radius: 20px;
  border: 1px solid var(--border-normal);
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  font-size: var(--font-md);
  resize: none;
  overflow-y: auto;
  min-height: 40px;
  max-height: 100px;
}

.message-input:focus {
  outline: none;
  border-color: var(--primary-color);
}

.message-input::placeholder {
  color: var(--text-tertiary);
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background-color: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

.send-btn:disabled {
  background-color: var(--border-light);
  cursor: not-allowed;
}

.send-btn svg {
  width: 20px;
  height: 20px;
}

/* 更多选项菜单 */
.more-options-menu {
  position: absolute;
  top: 60px;
  right: 16px;
  background-color: var(--bg-primary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-normal);
  z-index: 101;
  overflow: hidden;
  min-width: 180px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
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

.option-item:hover {
  background-color: var(--bg-secondary);
}

.option-item svg {
  width: 18px;
  height: 18px;
  color: var(--text-secondary);
}

.option-item.cancel {
  color: var(--text-tertiary);
  border-top: 1px solid var(--border-light);
  margin-top: 4px;
}

.option-item.cancel:hover {
  background-color: var(--bg-secondary);
  color: var(--text-secondary);
}

/* 响应式设计 */
@media (max-width: 375px) {
  .chat-header {
    padding: 10px 12px;
  }
  
  .avatar {
    width: 32px;
    height: 32px;
  }
  
  .message-container {
    padding: 12px;
  }
  
  .message-content {
    max-width: 80%;
    padding: 8px 10px;
  }
  
  .input-container {
    padding: 10px 12px;
  }
}
</style>
