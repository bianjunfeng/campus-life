import http from './http'

// 消息类型定义
export interface Message {
  id: number
  conversationId: string
  fromUserId: number
  fromUserName?: string
  fromUserAvatar?: string
  toUserId: number
  toUserName?: string
  toUserAvatar?: string
  content: string
  status: number  // 0-未读;1-已读;2-撤回/删除
  createTime: string
}

export interface Conversation {
  conversationId: string
  otherUserId: number
  otherUserName: string
  otherUserAvatar: string
  lastMessage: string
  lastMessageTime: string
  unreadCount: number
  isOnline?: boolean
}

export interface NotificationItem {
  id: string
  type: string
  actorUserId: number
  actorUserName: string
  actorUserAvatar?: string
  message: string
  createTime: string
  postId?: number
  unread?: boolean
}

export type NotificationCategory = 'likes-favorites' | 'comments' | 'follows' | 'system' | 'all'

// 发送消息
export const sendMessage = async (toUserId: number, content: string): Promise<Message> => {
  const response = await http.post('/message/send', {
    toUserId,
    content
  })
  const data = response.data
  if (data.code === 200) {
    return data.data
  }
  throw new Error(data.message || '发送消息失败')
}

// 获取会话列表
export const getConversations = async (): Promise<Conversation[]> => {
  const response = await http.get('/message/conversations')
  const data = response.data
  if (data.code === 200) {
    return data.data || []
  }
  throw new Error(data.message || '获取会话列表失败')
}

// 获取会话的消息列表
export const getMessagesByConversationId = async (
  conversationId: string,
  page: number = 1,
  pageSize: number = 50
): Promise<{ messages: Message[]; page: number; pageSize: number }> => {
  const response = await http.get(`/message/conversation/${conversationId}/messages`, {
    params: { page, pageSize }
  })
  const data = response.data
  if (data.code === 200) {
    return data.data
  }
  throw new Error(data.message || '获取消息列表失败')
}

// 通过用户ID获取消息列表
export const getMessagesByUserId = async (
  otherUserId: number,
  page: number = 1,
  pageSize: number = 50
): Promise<{ conversationId: string; messages: Message[]; page: number; pageSize: number }> => {
  const response = await http.get(`/message/user/${otherUserId}/messages`, {
    params: { page, pageSize }
  })
  const data = response.data
  if (data.code === 200) {
    return data.data
  }
  throw new Error(data.message || '获取消息列表失败')
}

// 标记会话为已读
export const markConversationAsRead = async (conversationId: string): Promise<void> => {
  const response = await http.put(`/message/conversation/${conversationId}/read`)
  const data = response.data
  if (data.code !== 200) {
    throw new Error(data.message || '标记已读失败')
  }
}

// 获取未读消息数量
export const getUnreadCount = async (): Promise<number> => {
  const response = await http.get('/message/unread-count')
  const data = response.data
  if (data.code === 200) {
    return data.data?.unreadCount || 0
  }
  throw new Error(data.message || '获取未读消息数量失败')
}

export const markNotificationsAsRead = async (category: NotificationCategory): Promise<number> => {
  const response = await http.put('/message/notifications/read', { category })
  const data = response.data
  if (data.code === 200) {
    return data.data?.updatedCount || 0
  }
  throw new Error(data.message || '标记通知已读失败')
}

export const markNotificationAsRead = async (notificationId: number | string): Promise<number> => {
  const response = await http.put(`/message/notifications/${notificationId}/read`)
  const data = response.data
  if (data.code === 200) {
    return data.data?.updatedCount || 0
  }
  throw new Error(data.message || '标记通知已读失败')
}

export const getLikeAndFavoriteNotifications = async (): Promise<NotificationItem[]> => {
  const response = await http.get('/message/notifications/likes-favorites')
  const data = response.data
  if (data.code === 200) {
    return data.data || []
  }
  throw new Error(data.message || '获取赞和收藏通知失败')
}

export const getCommentNotifications = async (): Promise<NotificationItem[]> => {
  const response = await http.get('/message/notifications/comments')
  const data = response.data
  if (data.code === 200) {
    return data.data || []
  }
  throw new Error(data.message || '获取评论和@通知失败')
}

export const getFollowNotifications = async (): Promise<NotificationItem[]> => {
  const response = await http.get('/message/notifications/follows')
  const data = response.data
  if (data.code === 200) {
    return data.data || []
  }
  throw new Error(data.message || '获取新增关注通知失败')
}

export const getSystemNotifications = async (): Promise<NotificationItem[]> => {
  const response = await http.get('/message/notifications/system')
  const data = response.data
  if (data.code === 200) {
    return data.data || []
  }
  throw new Error(data.message || '获取系统通知失败')
}
