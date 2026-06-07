<template>
  <div class="system-page">
    <div class="page-header-bar">
      <button class="back-btn" @click="handleBack">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
      </button>
      <h1 class="page-title">系统通知</h1>
      <div class="header-placeholder"></div>
    </div>

    <div class="message-list">
      <div
        v-for="message in messages"
        :key="message.id"
        :class="['message-item', { unread: message.unread }]"
        @click="handleMessageClick(message)"
      >
        <div class="icon-wrap" :class="message.levelClass">
          <svg v-if="message.type === 'PAYMENT_RECON_ALERT'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3l-8.47-14.14a2 2 0 0 0-3.42 0z"></path>
            <line x1="12" y1="9" x2="12" y2="13"></line>
            <line x1="12" y1="17" x2="12.01" y2="17"></line>
          </svg>
          <svg v-else-if="message.type === 'MERCHANT_REFUND_TODO' || message.type === 'ADMIN_REFUND_TODO'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 7h13a4 4 0 0 1 0 8H8"></path>
            <path d="M7 11l-4 4 4 4"></path>
            <path d="M14 11h3"></path>
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
            <path d="M9 12l2 2 4-4"></path>
          </svg>
        </div>

        <div class="message-content">
          <div class="message-header">
            <span class="message-title">{{ titleFor(message.type) }}</span>
            <span class="message-time">{{ formatTime(message.time) }}</span>
          </div>
          <p class="message-text">
            <span v-if="message.unread" class="unread-dot"></span>
            {{ message.message }}
          </p>
          <p class="message-hint">{{ hintFor(message.type) }}</p>
        </div>
      </div>
    </div>

    <div v-if="!messages.length" class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
        <path d="M9 12l2 2 4-4"></path>
      </svg>
      <p>暂无系统通知</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getSystemNotifications, markNotificationsAsRead, type NotificationItem } from '@/api/message'

interface SystemMessage {
  id: string
  type: string
  message: string
  time: string
  unread: boolean
  levelClass: string
}

const router = useRouter()
const messages = ref<SystemMessage[]>([])

const handleBack = () => {
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/')) {
    router.back()
    return
  }
  router.push('/message')
}

const formatTime = (dateString: string): string => {
  if (!dateString) return ''
  const date = new Date(dateString)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN', { hour12: false })
}

const titleFor = (type: string) => {
  switch (type) {
    case 'MERCHANT_REFUND_TODO': return '商家退款待处理'
    case 'ADMIN_REFUND_TODO': return '平台退款待处理'
    case 'REFUND_APPROVED': return '退款已通过'
    case 'REFUND_REJECTED': return '退款已驳回'
    case 'REFUND_ESCALATED': return '退款已升级平台处理'
    case 'REFUND_SUCCESS': return '退款成功'
    case 'REFUND_FAILED': return '退款失败'
    case 'PAYMENT_RECON_ALERT': return '支付对账告警'
    default: return '系统通知'
  }
}

const hintFor = (type: string) => {
  switch (type) {
    case 'MERCHANT_REFUND_TODO': return '点击前往商家退款审核页'
    case 'ADMIN_REFUND_TODO': return '点击前往管理端退款审核页'
    case 'PAYMENT_RECON_ALERT': return '点击前往支付对账页'
    default: return '点击查看相关退款进度'
  }
}

const levelClassFor = (type: string) => {
  if (type === 'PAYMENT_RECON_ALERT') return 'danger'
  if (type === 'MERCHANT_REFUND_TODO' || type === 'ADMIN_REFUND_TODO') return 'warn'
  return 'normal'
}

const handleMessageClick = (message: SystemMessage) => {
  message.unread = false
  if (message.type === 'MERCHANT_REFUND_TODO') {
    router.push('/merchant/refunds')
    return
  }
  if (message.type === 'ADMIN_REFUND_TODO') {
    router.push('/admin/refunds')
    return
  }
  if (message.type === 'PAYMENT_RECON_ALERT') {
    router.push('/admin/payment-reconciliation')
    return
  }
  router.push('/me/refunds')
}

const loadNotifications = async () => {
  try {
    const data = await getSystemNotifications()
    messages.value = data.map((item: NotificationItem) => ({
      id: item.id,
      type: item.type,
      message: item.message || '系统状态已更新',
      time: item.createTime,
      unread: item.unread !== false,
      levelClass: levelClassFor(item.type)
    }))
  } catch (error) {
    console.error('加载系统通知失败:', error)
    messages.value = []
    return
  }

  try {
    await markNotificationsAsRead('system')
    messages.value.forEach(message => {
      message.unread = false
    })
  } catch (error) {
    console.error('标记系统通知已读失败:', error)
  }
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped>
.system-page { min-height: 100vh; background-color: var(--bg-secondary); }
.page-header-bar { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; background-color: var(--bg-primary); position: sticky; top: 0; z-index: 100; box-shadow: 0 2px 4px rgba(0,0,0,.05); }
.back-btn { width: 36px; height: 36px; border-radius: 50%; background: none; border: none; display: flex; align-items: center; justify-content: center; cursor: pointer; color: var(--text-primary); }
.back-btn svg { width: 20px; height: 20px; }
.page-title { font-size: 18px; font-weight: 600; color: var(--text-primary); margin: 0; flex: 1; text-align: center; }
.header-placeholder { width: 36px; }
.message-list { padding: 0; }
.message-item { display: flex; gap: 12px; padding: 14px 16px; background: var(--bg-primary); border-bottom: 1px solid var(--border-light); cursor: pointer; }
.message-item.unread { background-color: rgba(22, 119, 255, 0.05); }
.icon-wrap { width: 48px; height: 48px; flex-shrink: 0; border-radius: 14px; display: flex; align-items: center; justify-content: center; }
.icon-wrap svg { width: 22px; height: 22px; }
.icon-wrap.normal { background: rgba(22, 119, 255, 0.1); color: #1677ff; }
.icon-wrap.warn { background: rgba(250, 173, 20, 0.14); color: #d48806; }
.icon-wrap.danger { background: rgba(255, 77, 79, 0.12); color: #cf1322; }
.message-content { min-width: 0; flex: 1; }
.message-header { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.message-title { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.message-time { font-size: 12px; color: var(--text-tertiary); white-space: nowrap; }
.message-text { margin: 8px 0 0; display: flex; gap: 6px; line-height: 1.6; color: var(--text-secondary); font-size: 13px; }
.message-hint { margin: 8px 0 0; color: var(--text-tertiary); font-size: 12px; }
.unread-dot { width: 8px; height: 8px; margin-top: 6px; border-radius: 50%; background: var(--primary-color); flex-shrink: 0; }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 72px 20px; color: var(--text-tertiary); }
.empty-state svg { width: 64px; height: 64px; opacity: .5; margin-bottom: 16px; }
.empty-state p { margin: 0; color: var(--text-secondary); font-size: var(--font-xl); }
</style>
