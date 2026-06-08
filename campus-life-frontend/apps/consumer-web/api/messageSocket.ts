import { createMessageWebSocketTicket, type Message } from './message'

type SocketStatus = 'idle' | 'connecting' | 'open' | 'closed'
type MessageListener = (message: Message) => void
type ReconnectListener = () => void
type StatusListener = (status: SocketStatus) => void

interface PendingAck {
  resolve: (message: Message) => void
  reject: (error: Error) => void
  timeoutId: number
}

const createClientMessageId = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `msg_${Date.now()}_${Math.random().toString(16).slice(2)}`
}

const buildWebSocketUrl = (ticket: string) => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/message?ticket=${encodeURIComponent(ticket)}`
}

class MessageSocketClient {
  private socket: WebSocket | null = null
  private status: SocketStatus = 'idle'
  private connectPromise: Promise<void> | null = null
  private reconnectTimer: number | null = null
  private heartbeatTimer: number | null = null
  private reconnectAttempt = 0
  private intentionalClose = false
  private hasOpenedOnce = false
  private pendingAcks = new Map<string, PendingAck>()
  private messageListeners = new Set<MessageListener>()
  private reconnectListeners = new Set<ReconnectListener>()
  private statusListeners = new Set<StatusListener>()

  connect(): Promise<void> {
    if (this.socket?.readyState === WebSocket.OPEN) {
      return Promise.resolve()
    }
    if (this.connectPromise) {
      return this.connectPromise
    }
    this.intentionalClose = false
    this.connectPromise = this.openSocket()
    return this.connectPromise
  }

  disconnect() {
    this.intentionalClose = true
    this.clearReconnectTimer()
    this.stopHeartbeat()
    this.rejectAllPending(new Error('消息连接已关闭'))
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
    this.setStatus('closed')
  }

  isReady() {
    return this.socket?.readyState === WebSocket.OPEN
  }

  getStatus() {
    return this.status
  }

  async sendChatMessage(toUserId: number, content: string): Promise<Message> {
    await this.connect()
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      throw new Error('消息连接不可用')
    }

    const clientMessageId = createClientMessageId()
    const payload = {
      type: 'chat.send',
      clientMessageId,
      toUserId,
      content
    }

    return new Promise<Message>((resolve, reject) => {
      const timeoutId = window.setTimeout(() => {
        this.pendingAcks.delete(clientMessageId)
        reject(new Error('消息发送超时'))
      }, 10000)

      this.pendingAcks.set(clientMessageId, { resolve, reject, timeoutId })

      try {
        this.socket?.send(JSON.stringify(payload))
      } catch (error: any) {
        window.clearTimeout(timeoutId)
        this.pendingAcks.delete(clientMessageId)
        reject(error instanceof Error ? error : new Error('消息发送失败'))
      }
    })
  }

  onChatMessage(listener: MessageListener) {
    this.messageListeners.add(listener)
    return () => this.messageListeners.delete(listener)
  }

  onReconnect(listener: ReconnectListener) {
    this.reconnectListeners.add(listener)
    return () => this.reconnectListeners.delete(listener)
  }

  onStatusChange(listener: StatusListener) {
    this.statusListeners.add(listener)
    return () => this.statusListeners.delete(listener)
  }

  private async openSocket(): Promise<void> {
    this.clearReconnectTimer()
    this.setStatus('connecting')

    try {
      const { ticket } = await createMessageWebSocketTicket()
      await new Promise<void>((resolve, reject) => {
        const socket = new WebSocket(buildWebSocketUrl(ticket))
        let opened = false
        const wasReconnect = this.hasOpenedOnce

        socket.onopen = () => {
          opened = true
          this.socket = socket
          this.connectPromise = null
          this.reconnectAttempt = 0
          this.hasOpenedOnce = true
          this.setStatus('open')
          this.startHeartbeat()
          resolve()
          if (wasReconnect) {
            this.reconnectListeners.forEach(listener => listener())
          }
        }

        socket.onmessage = event => this.handleMessage(event.data)

        socket.onerror = () => {
          if (!opened) {
            reject(new Error('消息连接失败'))
          }
        }

        socket.onclose = () => {
          if (this.socket === socket) {
            this.socket = null
          }
          this.connectPromise = null
          this.stopHeartbeat()
          this.rejectAllPending(new Error('消息连接已断开'))
          if (!this.intentionalClose) {
            this.setStatus('closed')
            this.scheduleReconnect()
          }
          if (!opened) {
            reject(new Error('消息连接已关闭'))
          }
        }
      })
    } catch (error) {
      this.connectPromise = null
      this.setStatus('closed')
      if (!this.intentionalClose) {
        this.scheduleReconnect()
      }
      throw error
    }
  }

  private handleMessage(data: string) {
    let envelope: any
    try {
      envelope = JSON.parse(data)
    } catch {
      return
    }

    if (envelope.type === 'chat.ack') {
      const pending = this.pendingAcks.get(envelope.clientMessageId)
      if (pending) {
        window.clearTimeout(pending.timeoutId)
        this.pendingAcks.delete(envelope.clientMessageId)
        pending.resolve(envelope.message)
      }
      return
    }

    if (envelope.type === 'chat.error') {
      const pending = this.pendingAcks.get(envelope.clientMessageId)
      if (pending) {
        window.clearTimeout(pending.timeoutId)
        this.pendingAcks.delete(envelope.clientMessageId)
        pending.reject(new Error(envelope.message || '消息发送失败'))
      }
      return
    }

    if (envelope.type === 'chat.message' && envelope.message) {
      this.messageListeners.forEach(listener => listener(envelope.message))
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer || this.intentionalClose) {
      return
    }
    const delay = Math.min(30000, 1000 * Math.pow(2, this.reconnectAttempt))
    this.reconnectAttempt += 1
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null
      this.openSocket().catch(() => {})
    }, delay)
  }

  private clearReconnectTimer() {
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = window.setInterval(() => {
      if (this.socket?.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify({ type: 'ping', clientTime: Date.now() }))
      }
    }, 25000)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      window.clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  private rejectAllPending(error: Error) {
    this.pendingAcks.forEach(pending => {
      window.clearTimeout(pending.timeoutId)
      pending.reject(error)
    })
    this.pendingAcks.clear()
  }

  private setStatus(status: SocketStatus) {
    if (this.status === status) {
      return
    }
    this.status = status
    this.statusListeners.forEach(listener => listener(status))
  }
}

export const messageSocket = new MessageSocketClient()
