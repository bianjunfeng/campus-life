<template>
  <div class="agent-chat-container">
    <header class="chat-header">
      <button class="back-btn" @click="handleBack">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 19l-7-7 7-7"></path>
        </svg>
      </button>

      <div class="chat-info">
        <div class="agent-info">
          <div class="agent-avatar">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
            </svg>
          </div>
          <div>
            <span class="agent-name">{{ currentTitle || '智能体助手' }}</span>
            <span v-if="isTyping" class="status">正在输入...</span>
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

    <div class="knowledge-toolbar">
      <div class="chat-mode-switch" aria-label="对话模式">
        <button
          :class="{ active: chatMode === 'quick' }"
          :disabled="isLoading"
          @click="chatMode = 'quick'"
        >
          快速
        </button>
        <button
          :class="{ active: chatMode === 'stream' }"
          :disabled="isLoading"
          @click="chatMode = 'stream'"
        >
          流式
        </button>
      </div>
      <select v-model="knowledgeMode" class="knowledge-mode">
        <option value="none">不使用知识库</option>
        <option value="personal">我的知识库</option>
        <option value="platform">平台知识库</option>
        <option value="mixed">平台 + 我的知识库</option>
      </select>
      <select
        v-if="knowledgeMode === 'personal' || knowledgeMode === 'mixed'"
        v-model="selectedKnowledgeBaseIds"
        class="knowledge-picker"
        multiple
      >
        <option v-for="item in knowledgeBases" :key="item.id" :value="item.id">
          {{ item.name }}
        </option>
      </select>
      <button class="knowledge-manage-btn" @click="openKnowledgeBases">管理</button>
    </div>

    <div class="message-container" ref="messageContainer">
      <div v-if="chatMessages.length === 0" class="welcome-message">
        <div class="welcome-content">
          <h2>开始新的对话</h2>
          <p>与 AI 助手进行智能对话，获取帮助和建议</p>
          <div class="quick-actions">
            <button
              v-for="action in quickActions"
              :key="action.text"
              class="quick-action-btn"
              @click="sendQuickMessage(action.text)"
            >
              {{ action.text }}
            </button>
          </div>
        </div>
      </div>

      <div v-for="(msg, index) in chatMessages" :key="`${msg.role}-${index}-${msg.time}`" :class="['message-wrapper', { sent: msg.sent }]">
        <div class="message-content-wrapper">
          <div v-if="msg.sent" class="message-avatar-right">
            <div class="user-avatar">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
          </div>

          <div :class="['message-content', { sent: msg.sent, error: msg.error, streaming: msg.streaming }]">
            <p v-if="msg.sent">{{ msg.text }}</p>
            <MarkdownMessage
              v-else
              :content="msg.text || (msg.streaming ? '正在生成...' : '')"
            />
            <span class="message-time">{{ formatTime(msg.time) }}</span>
          </div>

          <div v-if="!msg.sent" class="message-avatar-left">
            <div class="agent-avatar-small">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <div v-if="isTyping" class="typing-indicator">
        <div class="agent-avatar-small">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
        </div>
        <div class="typing-bubble">
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
        </div>
      </div>
    </div>

    <div class="input-container">
      <div class="input-wrapper">
        <textarea
          v-model="newMessage"
          placeholder="输入消息..."
          rows="1"
          class="message-input"
          @keydown.enter.prevent="handleEnterKey"
          @input="autoResize"
          ref="messageInput"
        ></textarea>
      </div>

      <button
        :class="['send-btn', { 'stop-btn': isLoading }]"
        @click="isLoading ? stopGeneration() : sendMessage()"
        :disabled="!isLoading && !newMessage.trim()"
        :aria-label="isLoading ? '中断生成' : '发送消息'"
      >
        <svg v-if="!isLoading" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13"></line>
          <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
        </svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
          <rect x="6" y="6" width="12" height="12" rx="2"></rect>
        </svg>
      </button>
    </div>

    <div v-if="showMoreOptions" class="more-options-menu">
      <button class="option-item" @click="clearHistory">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="3 6 5 6 21 6"></polyline>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
        </svg>
        <span>删除会话</span>
      </button>
      <button class="option-item" @click="showMoreOptions = false">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
        <span>关闭</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownMessage from '../../components/MarkdownMessage.vue'
import { createConversation, deleteConversation, getConversationDetail, sendAgentMessage, streamAgentMessage, type SendAgentMessagePayload } from '../../api/agent'
import { listKnowledgeBases, type KnowledgeBase } from '../../api/knowledge'
import { showAlert, showConfirm } from '../../utils/dialog'

interface ChatMessage {
  text: string
  sent: boolean
  time: string
  error?: boolean
  streaming?: boolean
}

const router = useRouter()
const route = useRoute()

const chatMessages = ref<ChatMessage[]>([])
const newMessage = ref('')
const isTyping = ref(false)
const isLoading = ref(false)
const showMoreOptions = ref(false)
const messageContainer = ref<HTMLElement | null>(null)
const messageInput = ref<HTMLTextAreaElement | null>(null)
const currentSessionId = ref<string | null>(null)
const currentAssistantType = ref<string>('general')
const currentTitle = ref('')
const knowledgeBases = ref<KnowledgeBase[]>([])
const knowledgeMode = ref<'none' | 'personal' | 'platform' | 'mixed'>('none')
const selectedKnowledgeBaseIds = ref<number[]>([])
const chatMode = ref<'quick' | 'stream'>('quick')
const activeAbortController = ref<AbortController | null>(null)
const generationStopped = ref(false)

const agentBasePath = computed(() => {
  if (route.path.startsWith('/admin/ai/agent')) return '/admin/ai/agent'
  if (route.path.startsWith('/merchant/ai')) return '/merchant/ai'
  return '/agent'
})

const quickActions = [
  { text: '你好，介绍一下自己' },
  { text: '平台的商家认证怎么申请？' },
  { text: '帮我生成一个帖子标题' },
  { text: '给我一些校园生活建议' }
]

const loadHistory = async (sessionId: string) => {
  try {
    const detail = await getConversationDetail(sessionId)
    currentSessionId.value = detail.conversation.sessionId
    currentTitle.value = detail.conversation.title
    currentAssistantType.value = detail.conversation.assistantType || 'general'
    chatMessages.value = detail.messages.map(message => ({
      text: message.content,
      sent: message.role === 'user',
      time: message.createdAt,
      error: message.messageStatus === 'FAILED'
    }))
    scrollToBottom()
  } catch (error) {
    console.error('加载历史记录失败:', error)
  }
}

const ensureConversation = async () => {
  if (currentSessionId.value) {
    return currentSessionId.value
  }
  const conversation = await createConversation({
    assistantType: currentAssistantType.value
  })
  currentSessionId.value = conversation.sessionId
  currentTitle.value = conversation.title
  await router.replace({
    path: `${agentBasePath.value}/chat`,
    query: {
      sessionId: conversation.sessionId,
      assistantType: currentAssistantType.value
    }
  })
  return conversation.sessionId
}

const sendMessage = async () => {
  const message = newMessage.value.trim()
  if (!message || isLoading.value) return
  const controller = new AbortController()
  activeAbortController.value = controller
  generationStopped.value = false

  chatMessages.value.push({
    text: message,
    sent: true,
    time: new Date().toISOString()
  })

  newMessage.value = ''
  autoResize()
  scrollToBottom()
  isTyping.value = true
  isLoading.value = true

  try {
    const sessionId = await ensureConversation()
    if (controller.signal.aborted) return

    const payload = buildMessagePayload(message)
    if (chatMode.value === 'stream') {
      await sendStreamMessage(sessionId, payload, controller.signal)
    } else {
      const response = await sendAgentMessage(sessionId, payload, controller.signal)
      currentSessionId.value = response.sessionId
      currentTitle.value = response.conversationTitle

      chatMessages.value.push({
        text: response.assistantMessage.content,
        sent: false,
        time: response.assistantMessage.createdAt,
        error: response.assistantMessage.messageStatus === 'FAILED'
      })
    }
  } catch (error: any) {
    if (generationStopped.value || isAbortError(error)) {
      return
    }
    console.error('发送消息失败:', error)
    chatMessages.value.push({
      text: `发送失败: ${error.message || '网络错误'}`,
      sent: false,
      time: new Date().toISOString(),
      error: true
    })
  } finally {
    if (activeAbortController.value === controller) {
      activeAbortController.value = null
    }
    isTyping.value = false
    isLoading.value = false
    scrollToBottom()
  }
}

const stopGeneration = () => {
  generationStopped.value = true
  activeAbortController.value?.abort()
  markLastStreamingMessageStopped()
  isTyping.value = false
  isLoading.value = false
}

const markLastStreamingMessageStopped = () => {
  const lastAssistant = [...chatMessages.value].reverse().find(message => !message.sent && message.streaming)
  if (!lastAssistant) {
    chatMessages.value.push({
      text: '已中断生成',
      sent: false,
      time: new Date().toISOString()
    })
    return
  }
  lastAssistant.streaming = false
  lastAssistant.text = lastAssistant.text.trim()
    ? `${lastAssistant.text}\n\n[已中断]`
    : '已中断生成'
}

const isAbortError = (error: any) => {
  const message = String(error?.message || '')
  return error?.name === 'AbortError'
    || error?.name === 'CanceledError'
    || error?.code === 'ERR_CANCELED'
    || /abort|cancel|取消|中断/i.test(message)
}

const buildMessagePayload = (message: string): SendAgentMessagePayload => ({
  content: message,
  sceneCode: resolveSceneCode(),
  knowledgeBaseIds: resolveKnowledgeBaseIds(),
  usePersonalKnowledge: knowledgeMode.value === 'personal' || knowledgeMode.value === 'mixed',
  usePlatformKnowledge: knowledgeMode.value === 'platform' || knowledgeMode.value === 'mixed'
})

const sendStreamMessage = async (sessionId: string, payload: SendAgentMessagePayload, signal?: AbortSignal) => {
  let assistantIndex = -1

  const ensureAssistantMessage = () => {
    if (assistantIndex < 0) {
      chatMessages.value.push({
        text: '',
        sent: false,
        time: new Date().toISOString(),
        streaming: true
      })
      assistantIndex = chatMessages.value.length - 1
    }
    return chatMessages.value[assistantIndex]
  }

  await streamAgentMessage(sessionId, payload, {
    onMeta: meta => {
      if (signal?.aborted) return
      currentSessionId.value = meta.sessionId
      currentTitle.value = meta.conversationTitle || currentTitle.value
    },
    onDelta: delta => {
      if (signal?.aborted) return
      const assistantMessage = ensureAssistantMessage()
      assistantMessage.text += delta.content || ''
      assistantMessage.streaming = true
      isTyping.value = false
      scrollToBottom()
    },
    onDone: response => {
      if (signal?.aborted) return
      currentSessionId.value = response.sessionId
      currentTitle.value = response.conversationTitle
      const assistantMessage = assistantIndex >= 0 ? chatMessages.value[assistantIndex] : ensureAssistantMessage()
      assistantMessage.text = response.assistantMessage.content || assistantMessage.text
      assistantMessage.time = response.assistantMessage.createdAt
      assistantMessage.error = response.assistantMessage.messageStatus === 'FAILED'
      assistantMessage.streaming = false
      isTyping.value = false
      scrollToBottom()
    },
    onError: error => {
      if (signal?.aborted || generationStopped.value) return
      const assistantMessage = ensureAssistantMessage()
      assistantMessage.text = `发送失败: ${error.message || '流式对话失败'}`
      assistantMessage.error = true
      assistantMessage.streaming = false
      isTyping.value = false
      scrollToBottom()
    }
  }, signal)
}

const sendQuickMessage = (text: string) => {
  newMessage.value = text
  sendMessage()
}

const resolveSceneCode = () => {
  if (currentAssistantType.value === 'merchant_ops') return 'chat.merchant_ops'
  if (currentAssistantType.value === 'campus_qa') return 'chat.campus_qa'
  if (knowledgeMode.value === 'mixed') return 'chat.mixed_qa'
  if (knowledgeMode.value === 'personal') return 'chat.personal_qa'
  if (knowledgeMode.value === 'platform') return 'chat.campus_qa'
  return 'chat.general'
}

const resolveKnowledgeBaseIds = () => {
  if (knowledgeMode.value !== 'personal' && knowledgeMode.value !== 'mixed') {
    return []
  }
  return selectedKnowledgeBaseIds.value.map(Number).filter(id => Number.isFinite(id) && id > 0)
}

const loadKnowledgeBases = async () => {
  try {
    knowledgeBases.value = await listKnowledgeBases()
  } catch (error) {
    console.error('加载知识库失败:', error)
  }
}

const openKnowledgeBases = () => {
  router.push(`${agentBasePath.value}/knowledge-bases`)
}

const clearHistory = async () => {
  if (!currentSessionId.value) {
    chatMessages.value = []
    showMoreOptions.value = false
    return
  }
  if (await showConfirm('确定要删除当前会话吗？')) {
    try {
      await deleteConversation(currentSessionId.value)
      chatMessages.value = []
      currentSessionId.value = null
      currentTitle.value = ''
      showMoreOptions.value = false
      await router.replace({
        path: `${agentBasePath.value}/chat`,
        query: {
          assistantType: currentAssistantType.value
        }
      })
    } catch (error: any) {
      await showAlert('删除会话失败: ' + (error.message || '未知错误'))
    }
  }
}

const handleBack = () => {
  router.back()
}

const formatTime = (value: string) => {
  const date = new Date(value)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

const autoResize = () => {
  nextTick(() => {
    if (messageInput.value) {
      messageInput.value.style.height = 'auto'
      messageInput.value.style.height = Math.min(messageInput.value.scrollHeight, 120) + 'px'
    }
  })
}

const handleEnterKey = (e: KeyboardEvent) => {
  if (e.shiftKey) return
  sendMessage()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

watch(chatMessages, () => {
  scrollToBottom()
}, { deep: true })

onMounted(async () => {
  const sessionId = route.query.sessionId as string | undefined
  const initialMessage = route.query.message as string | undefined
  const assistantType = route.query.assistantType as string | undefined

  if (assistantType) {
    currentAssistantType.value = assistantType
    if (assistantType === 'campus_qa') {
      knowledgeMode.value = 'platform'
    }
  }

  await loadKnowledgeBases()

  if (sessionId) {
    await loadHistory(sessionId)
  }

  if (initialMessage) {
    newMessage.value = initialMessage
    nextTick(() => {
      sendMessage()
    })
  }
})
</script>

<style scoped>
.agent-chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-btn, .more-btn {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-btn svg, .more-btn svg {
  width: 24px;
  height: 24px;
}

.chat-info {
  flex: 1;
  margin: 0 16px;
}

.knowledge-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.chat-mode-switch {
  display: inline-flex;
  flex: 0 0 auto;
  height: 36px;
  padding: 3px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #f7f8fa;
}

.chat-mode-switch button {
  min-width: 52px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
}

.chat-mode-switch button.active {
  background: #667eea;
  color: #fff;
}

.chat-mode-switch button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.knowledge-mode,
.knowledge-picker {
  min-width: 0;
  height: 36px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  color: #333;
  font-size: 14px;
  padding: 0 10px;
  outline: none;
}

.knowledge-mode {
  flex: 0 0 150px;
}

.knowledge-picker {
  flex: 1;
}

.knowledge-manage-btn {
  height: 36px;
  border: none;
  border-radius: 8px;
  background: #eef1f5;
  color: #333;
  padding: 0 12px;
  font-size: 14px;
}

.agent-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.agent-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.agent-avatar svg {
  width: 24px;
  height: 24px;
}

.agent-name {
  font-weight: 600;
  font-size: 16px;
  color: #333;
  display: block;
}

.status {
  font-size: 12px;
  color: #999;
  display: block;
  margin-top: 2px;
}

.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-message {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  text-align: center;
}

.welcome-content h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 8px;
}

.welcome-content p {
  color: #666;
  margin-bottom: 24px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 400px;
  margin: 0 auto;
}

.quick-action-btn {
  padding: 12px 24px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.quick-action-btn:hover {
  background: #f5f5f5;
  border-color: #667eea;
}

.message-wrapper {
  display: flex;
  width: 100%;
}

.message-wrapper.sent {
  justify-content: flex-end;
}

.message-content-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 70%;
}

.message-wrapper.sent .message-content-wrapper {
  flex-direction: row-reverse;
}

.message-avatar-right, .message-avatar-left {
  flex-shrink: 0;
}

.user-avatar, .agent-avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  color: #666;
}

.agent-avatar-small {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.user-avatar svg, .agent-avatar-small svg {
  width: 18px;
  height: 18px;
}

.message-content {
  background: white;
  padding: 12px 16px;
  border-radius: 18px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message-content.sent {
  background: #667eea;
  color: white;
}

.message-content.error {
  border: 1px solid #ffb3b3;
}

.message-content.streaming > p::after,
.message-content.streaming :deep(.markdown-message)::after {
  content: '';
  display: inline-block;
  width: 6px;
  height: 1em;
  margin-left: 2px;
  background: currentColor;
  vertical-align: -2px;
  animation: cursor-blink 1s steps(2, start) infinite;
}

@keyframes cursor-blink {
  to { visibility: hidden; }
}

.message-content > p {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  line-height: 1.5;
}

.message-time {
  font-size: 11px;
  color: #999;
  display: block;
  margin-top: 4px;
}

.message-content.sent .message-time {
  color: rgba(255, 255, 255, 0.7);
}

.typing-indicator {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.typing-bubble {
  background: white;
  padding: 12px 16px;
  border-radius: 18px;
  display: flex;
  gap: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #999;
  animation: typing 1.4s infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border-top: 1px solid #e0e0e0;
}

.input-wrapper {
  flex: 1;
  background: #f5f5f5;
  border-radius: 24px;
  padding: 8px 16px;
}

.message-input {
  width: 100%;
  border: none;
  background: transparent;
  resize: none;
  outline: none;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", "PingFang SC", "Hiragino Sans GB", Arial, sans-serif;
  font-size: 16px;
  font-weight: 400;
  line-height: 1.6;
  letter-spacing: 0;
  color: #1f2937;
  caret-color: #667eea;
  max-height: 120px;
  overflow-y: auto;
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}

.message-input::placeholder {
  color: #9ca3af;
  font-weight: 400;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #667eea;
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: #5568d3;
}

.send-btn.stop-btn {
  background: #ef4444;
}

.send-btn.stop-btn:hover:not(:disabled) {
  background: #dc2626;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.send-btn svg {
  width: 20px;
  height: 20px;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.more-options-menu {
  position: absolute;
  top: 60px;
  right: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 8px 0;
  min-width: 160px;
  z-index: 100;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 16px;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.2s;
}

.option-item:hover {
  background: #f5f5f5;
}

.option-item svg {
  width: 18px;
  height: 18px;
  color: #666;
}

.option-item span {
  color: #333;
  font-size: 14px;
}

@media (max-width: 560px) {
  .knowledge-toolbar {
    flex-wrap: wrap;
  }

  .chat-mode-switch {
    flex: 1 1 100%;
  }

  .chat-mode-switch button {
    flex: 1;
  }

  .knowledge-mode {
    flex: 1 1 160px;
  }

  .knowledge-picker {
    flex: 1 1 100%;
  }
}
</style>
