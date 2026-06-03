<template>
  <div class="agent-list-container">
    <header class="page-header">
      <button class="back-btn" @click="router.back()" aria-label="返回">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 18-6-6 6-6"></path>
        </svg>
      </button>
      <h1 class="page-title">智能体助手</h1>
      <div class="header-actions">
        <button class="knowledge-btn" @click="openKnowledgeBases">知识库</button>
        <button class="add-agent-btn" @click="startNewChat">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>新建对话</span>
        </button>
      </div>
    </header>

    <div class="content-area">
      <div v-if="sessions.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
        </div>
        <h2>开始新的对话</h2>
        <p>选择一个智能体助手开始聊天，或直接发送消息创建新会话</p>

        <div class="quick-start-options">
          <div class="option-card" @click="startChatWithType('general')">
            <div class="option-icon chat-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
            </div>
            <h3>智能对话</h3>
            <p>与 AI 助手进行通用对话</p>
          </div>

          <div class="option-card" @click="startChatWithType('campus_qa')">
            <div class="option-icon qa-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
            </div>
            <h3>知识问答</h3>
            <p>围绕平台和校园生活问题进行问答</p>
          </div>

          <div class="option-card" @click="startChatWithType('merchant_ops')">
            <div class="option-icon quick-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
            </div>
            <h3>商家助手</h3>
            <p>提供商家运营和平台使用指引</p>
          </div>
        </div>
      </div>

      <div v-else class="sessions-list">
        <div
          v-for="session in sessions"
          :key="session.sessionId"
          class="session-item"
          @click="openSession(session.sessionId)"
        >
          <div class="session-avatar">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
            </svg>
          </div>
          <div class="session-info">
            <h3>{{ session.title || '新对话' }}</h3>
            <p>{{ session.lastMessagePreview || '暂无消息' }}</p>
            <span class="session-time">{{ formatTime(session.lastMessageAt) }}</span>
          </div>
          <button class="delete-btn" @click.stop="removeSession(session.sessionId)">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div class="quick-input-container">
      <div class="quick-input-wrapper">
        <input
          v-model="quickMessage"
          type="text"
          placeholder="输入消息，立即开始对话..."
          class="quick-input"
          @keydown.enter="handleQuickSend"
        />
        <button class="quick-send-btn" @click="handleQuickSend" :disabled="!quickMessage.trim()">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="22" y1="2" x2="11" y2="13"></line>
            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteConversation, getConversationList, type AgentConversationSummary } from '../../api/agent'
import { showAlert, showConfirm } from '../../utils/dialog'

const router = useRouter()
const route = useRoute()
const sessions = ref<AgentConversationSummary[]>([])
const quickMessage = ref('')

const agentBasePath = computed(() => {
  if (route.path.startsWith('/admin/ai/agent')) return '/admin/ai/agent'
  if (route.path.startsWith('/merchant/ai')) return '/merchant/ai'
  return '/agent'
})

const loadSessions = async () => {
  try {
    sessions.value = await getConversationList()
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

const startNewChat = () => {
  router.push(`${agentBasePath.value}/chat`)
}

const openKnowledgeBases = () => {
  router.push(`${agentBasePath.value}/knowledge-bases`)
}

const startChatWithType = (assistantType: string) => {
  router.push({
    path: `${agentBasePath.value}/chat`,
    query: { assistantType }
  })
}

const openSession = (sessionId: string) => {
  router.push({
    path: `${agentBasePath.value}/chat`,
    query: { sessionId }
  })
}

const removeSession = async (sessionId: string) => {
  if (await showConfirm('确定要删除这个会话吗？')) {
    try {
      await deleteConversation(sessionId)
      sessions.value = sessions.value.filter(item => item.sessionId !== sessionId)
    } catch (error: any) {
      await showAlert('删除会话失败: ' + (error.message || '未知错误'))
    }
  }
}

const handleQuickSend = () => {
  if (!quickMessage.value.trim()) return
  router.push({
    path: `${agentBasePath.value}/chat`,
    query: {
      message: quickMessage.value,
      assistantType: 'general'
    }
  })
  quickMessage.value = ''
}

const formatTime = (value?: string) => {
  if (!value) return '刚刚'
  const date = new Date(value)
  const diff = Date.now() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.agent-list-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 12px;
  background: white;
  border-bottom: 1px solid #eee;
}

.back-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  color: #333;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.back-btn:hover {
  background: #f5f5f5;
}

.page-title {
  font-size: 16px;
  color: #333;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.add-agent-btn,
.knowledge-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.knowledge-btn {
  background: #eef1f5;
  color: #333;
}

.add-agent-btn:hover {
  background: #5568d3;
}

.add-agent-btn svg {
  width: 18px;
  height: 18px;
}

.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 24px;
}

.empty-icon svg {
  width: 40px;
  height: 40px;
}

.empty-state h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 8px;
}

.empty-state p {
  color: #666;
  margin-bottom: 32px;
}

.quick-start-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  max-width: 800px;
  width: 100%;
}

.option-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.option-card:hover {
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.option-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.chat-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.qa-icon {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
}

.quick-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.option-icon svg {
  width: 24px;
  height: 24px;
}

.option-card h3 {
  font-size: 18px;
  color: #333;
  margin-bottom: 8px;
}

.option-card p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.sessions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.session-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.session-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.session-avatar svg {
  width: 24px;
  height: 24px;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-info h3 {
  font-size: 16px;
  color: #333;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-info p {
  font-size: 14px;
  color: #666;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 12px;
  color: #999;
}

.delete-btn {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  color: #999;
  transition: color 0.2s;
  flex-shrink: 0;
}

.delete-btn:hover {
  color: #f44336;
}

.delete-btn svg {
  width: 20px;
  height: 20px;
}

.quick-input-container {
  padding: 16px;
  background: white;
  border-top: 1px solid #e0e0e0;
}

.quick-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f5f5f5;
  border-radius: 24px;
  padding: 8px 16px;
}

.quick-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 15px;
  padding: 4px 0;
}

.quick-send-btn {
  width: 32px;
  height: 32px;
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

.quick-send-btn:hover:not(:disabled) {
  background: #5568d3;
}

.quick-send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.quick-send-btn svg {
  width: 18px;
  height: 18px;
}
</style>
