import http from './http'
import { getAuthToken } from '../../../src/shared/utils/authStorage'

export interface AgentConversationSummary {
  sessionId: string
  assistantType: string
  title: string
  lastMessagePreview?: string
  lastMessageAt?: string
  messageCount: number
  pinned: boolean
  createdAt: string
  updatedAt: string
}

export interface AgentMessage {
  id: number
  sessionId: string
  role: 'user' | 'assistant' | 'system' | 'tool'
  contentType: string
  content: string
  messageStatus: 'SUCCESS' | 'FAILED' | 'GENERATING' | 'CANCELLED' | 'UNKNOWN'
  providerCode?: string
  modelCode?: string
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  latencyMs?: number
  finishReason?: string
  errorCode?: string
  errorMessage?: string
  replyToMessageId?: number
  createdAt: string
}

export interface AgentConversationDetail {
  conversation: AgentConversationSummary
  messages: AgentMessage[]
}

export interface SendMessageResponse {
  sessionId: string
  conversationTitle: string
  userMessage: AgentMessage
  assistantMessage: AgentMessage
  usage?: {
    promptTokens?: number
    completionTokens?: number
    totalTokens?: number
    latencyMs?: number
  }
  knowledgeReferences?: Array<{
    kbId: number
    knowledgeBaseName?: string
    documentId: number
    documentTitle?: string
    chunkIndex?: number
    score?: number
  }>
}

export interface SendAgentMessagePayload {
  content: string
  sceneCode?: string
  capabilityCode?: string
  providerCode?: string
  modelCode?: string
  knowledgeBaseIds?: number[]
  usePersonalKnowledge?: boolean
  usePlatformKnowledge?: boolean
}

export interface AgentStreamMeta {
  sessionId: string
  conversationTitle?: string
  capabilityCode?: string
  sceneCode?: string
  providerCode?: string
  modelCode?: string
  userMessage?: AgentMessage
  knowledgeReferences?: SendMessageResponse['knowledgeReferences']
}

export interface AgentStreamDelta {
  content: string
}

export interface AgentStreamError {
  message?: string
}

export interface AgentStreamHandlers {
  onMeta?: (meta: AgentStreamMeta) => void
  onDelta?: (delta: AgentStreamDelta) => void
  onDone?: (response: SendMessageResponse) => void
  onError?: (error: AgentStreamError) => void
}

export async function createConversation(payload?: { assistantType?: string; title?: string }): Promise<AgentConversationSummary> {
  const { data } = await http.post('/agent/conversations', payload || {})
  if (data.code === 200) {
    return data.data
  }
  throw new Error(data.message || '创建会话失败')
}

export async function getConversationList(): Promise<AgentConversationSummary[]> {
  const { data } = await http.get('/agent/conversations')
  if (data.code === 200) {
    return data.data || []
  }
  throw new Error(data.message || '获取会话列表失败')
}

export async function getConversationDetail(sessionId: string, page: number = 1, pageSize: number = 100): Promise<AgentConversationDetail> {
  const { data } = await http.get(`/agent/conversations/${sessionId}`, {
    params: { page, pageSize }
  })
  if (data.code === 200) {
    return data.data
  }
  throw new Error(data.message || '获取会话详情失败')
}

export async function renameConversation(sessionId: string, title: string): Promise<void> {
  const { data } = await http.patch(`/agent/conversations/${sessionId}`, { title })
  if (data.code !== 200) {
    throw new Error(data.message || '重命名会话失败')
  }
}

export async function deleteConversation(sessionId: string): Promise<void> {
  const { data } = await http.delete(`/agent/conversations/${sessionId}`)
  if (data.code !== 200) {
    throw new Error(data.message || '删除会话失败')
  }
}

export async function sendAgentMessage(
  sessionId: string,
  payload: string | SendAgentMessagePayload,
  signal?: AbortSignal
): Promise<SendMessageResponse> {
  const body = typeof payload === 'string' ? { content: payload } : payload
  const { data } = await http.post(`/agent/conversations/${sessionId}/messages`, body, { signal })
  if (data.code === 200) {
    return data.data
  }
  throw new Error(data.message || '发送消息失败')
}

export async function streamAgentMessage(
  sessionId: string,
  payload: string | SendAgentMessagePayload,
  handlers: AgentStreamHandlers = {},
  signal?: AbortSignal
): Promise<void> {
  const body = typeof payload === 'string' ? { content: payload } : payload
  const token = getAuthToken()
  const response = await fetch(`/api/agent/conversations/${encodeURIComponent(sessionId)}/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(body),
    signal
  })

  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }
  if (!response.body) {
    throw new Error('浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let eventName = 'message'
  let dataLines: string[] = []

  const flushEvent = () => {
    if (dataLines.length === 0) {
      eventName = 'message'
      return
    }
    const rawData = dataLines.join('\n')
    eventName = eventName || 'message'
    dataLines = []
    try {
      const payload = JSON.parse(rawData)
      if (eventName === 'meta') {
        handlers.onMeta?.(payload)
      } else if (eventName === 'delta') {
        handlers.onDelta?.(payload)
      } else if (eventName === 'done') {
        handlers.onDone?.(payload)
      } else if (eventName === 'error') {
        handlers.onError?.(payload)
      }
    } catch (error) {
      handlers.onError?.({ message: '流式响应解析失败' })
    } finally {
      eventName = 'message'
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split(/\r?\n/)
    buffer = lines.pop() || ''
    for (const line of lines) {
      if (line === '') {
        flushEvent()
      } else if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart())
      }
    }
  }
  buffer += decoder.decode()
  if (buffer) {
    const lines = buffer.split(/\r?\n/)
    for (const line of lines) {
      if (line === '') {
        flushEvent()
      } else if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart())
      }
    }
  }
  flushEvent()
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const data = await response.json()
    return data?.message || data?.msg || `流式请求失败: ${response.status}`
  } catch {
    return `流式请求失败: ${response.status}`
  }
}
