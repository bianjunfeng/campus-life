<template>
  <div class="admin-profile-info">
    <div class="profile-card">
      <div class="profile-header">
        <div class="profile-avatar-wrapper">
          <img
            v-if="userInfo.icon"
            :src="userInfo.icon"
            alt="头像"
            class="profile-avatar"
          />
          <div v-else class="profile-avatar-placeholder">
            {{ userInfo.nickName ? userInfo.nickName.charAt(0) : 'U' }}
          </div>
        </div>
        <div class="profile-info">
          <h2 class="profile-name">{{ userInfo.nickName || '未设置昵称' }}</h2>
          <div class="profile-meta">
            <span class="role-badge" :class="getRoleClass(userInfo.role)">
              {{ getRoleText(userInfo.role) }}
            </span>
            <span v-if="userInfo.region" class="region">{{ userInfo.region }}</span>
            <span v-if="userInfo.occupation" class="region">{{ userInfo.occupation }}</span>
          </div>
          <div class="profile-contact">
            <span v-if="userInfo.phone" class="contact-item">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
              </svg>
              {{ userInfo.phone }}
            </span>
            <span v-if="userInfo.bio" class="contact-item bio-item">
              {{ userInfo.bio }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <div class="content-left">
        <div class="section-card">
          <div class="section-header">
            <h3 class="section-title">平台概览</h3>
          </div>
          <div class="metric-grid">
            <div v-for="metric in overviewMetrics" :key="metric.label" class="metric-item">
              <div class="metric-value">{{ metric.value }}</div>
              <div class="metric-label">{{ metric.label }}</div>
            </div>
          </div>
        </div>

        <div class="section-card">
          <div class="section-header">
            <h3 class="section-title">待处理事项</h3>
          </div>
          <div class="task-list">
            <div v-for="task in pendingTasks" :key="task.label" class="task-item">
              <div class="task-main">
                <div class="task-label">{{ task.label }}</div>
                <div class="task-desc">{{ task.desc }}</div>
              </div>
              <div class="task-count" :class="{ highlight: task.count > 0 }">{{ task.count }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="content-right">
        <div class="section-card">
          <div class="section-header">
            <h3 class="section-title">内容分布</h3>
          </div>
          <div v-if="distributionItems.length === 0" class="notification-empty compact-empty">
            <p>暂无统计数据</p>
          </div>
          <div v-else class="distribution-list">
            <div v-for="item in distributionItems" :key="item.name" class="distribution-item">
              <div class="distribution-main">
                <span class="distribution-name">{{ item.name }}</span>
                <span class="distribution-percent">{{ item.percent }}%</span>
              </div>
              <div class="distribution-bar">
                <span class="distribution-fill" :style="{ width: `${item.percent}%`, backgroundColor: item.color }"></span>
              </div>
              <div class="distribution-value">{{ item.value }}</div>
            </div>
          </div>
        </div>

        <div class="section-card">
          <div class="section-header">
            <h3 class="section-title">系统通知</h3>
          </div>
          <div class="notification-empty" v-if="loadingNotifications">
            <p>加载中...</p>
          </div>
          <div class="notification-empty" v-else-if="notifications.length === 0">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
            </svg>
            <p>暂无通知</p>
          </div>
          <div v-else class="notification-list">
            <div v-for="notification in notifications" :key="notification.id" class="notification-item">
              <div class="notification-top">
                <span class="notification-content">{{ notification.message }}</span>
                <span v-if="notification.unread" class="notification-badge">未读</span>
              </div>
              <div class="notification-time">{{ formatTime(notification.createTime) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getDashboardStats, getAdminStats, type AdminStats, type DashboardStats } from '../../api/admin'
import { getUserInfo, type UserInfo } from '../../api/auth'
import { getSystemNotifications, type NotificationItem } from '../../api/message'

type EditableUserInfo = UserInfo & {
  birthday?: string
  gender?: string
  occupation?: string
  region?: string
  bio?: string
}

type DistributionItem = {
  name: string
  value: number
  percent: number
  color: string
}

const userInfo = ref<EditableUserInfo>({
  id: 0,
  phone: '',
  nickName: '',
  icon: '',
  bio: '',
  gender: '',
  birthday: '',
  region: '',
  occupation: '',
  role: 'GUEST'
})

const dashboardStats = ref<DashboardStats>({
  todayIP: 0,
  memberCount: 0,
  postCount: 0,
  voucherCount: 0,
  merchantCount: 0
})

const adminStats = ref<AdminStats>({
  studentAuth: { pending: 0, approved: 0, rejected: 0 },
  merchantAuth: { pending: 0, approved: 0, rejected: 0 },
  posts: { total: 0, pending: 0, banned: 0 },
  comments: { total: 0, pending: 0, banned: 0 },
  merchants: { total: 0, normal: 0, frozen: 0 },
  vouchers: { total: 0, online: 0, offline: 0 }
})

const notifications = ref<NotificationItem[]>([])
const loadingNotifications = ref(false)

const overviewMetrics = computed(() => [
  { label: '会员总数', value: dashboardStats.value.memberCount || 0 },
  { label: '帖子总数', value: dashboardStats.value.postCount || 0 },
  { label: '商家总数', value: dashboardStats.value.merchantCount || 0 },
  { label: '券总数', value: dashboardStats.value.voucherCount || 0 },
  { label: '今日访问IP', value: dashboardStats.value.todayIP || 0 }
])

const pendingTasks = computed(() => [
  {
    label: '待审学生认证',
    desc: '等待管理员处理的学生认证申请',
    count: adminStats.value.studentAuth.pending || 0
  },
  {
    label: '待审商家认证',
    desc: '等待管理员处理的商家入驻申请',
    count: adminStats.value.merchantAuth.pending || 0
  },
  {
    label: '待处理帖子',
    desc: '当前处于待处理或异常状态的帖子',
    count: (adminStats.value.posts.pending || 0) + (adminStats.value.posts.banned || 0)
  },
  {
    label: '待处理评论',
    desc: '当前处于待处理或异常状态的评论',
    count: (adminStats.value.comments.pending || 0) + (adminStats.value.comments.banned || 0)
  }
])

const distributionItems = computed<DistributionItem[]>(() => {
  const contentData = dashboardStats.value.contentTypeData || []
  return contentData
    .filter(item => Number(item.value || 0) > 0)
    .slice(0, 6)
    .map(item => ({
      name: item.name,
      value: Number(item.value || 0),
      percent: Number(item.percent || 0),
      color: item.color || '#1677ff'
    }))
})

const loadUserInfo = async () => {
  try {
    userInfo.value = await getUserInfo()
  } catch (error) {
    console.error('加载用户信息失败:', error)
    try {
      const userInfoStr = localStorage.getItem('userInfo')
      if (userInfoStr) {
        const info = JSON.parse(userInfoStr)
        userInfo.value = {
          id: info.id || 0,
          phone: info.phone || '',
          nickName: info.nickName || info.username || info.name || '未设置昵称',
          icon: info.icon || info.avatar || '',
          bio: info.bio || '',
          gender: info.gender || '',
          birthday: info.birthday || '',
          region: info.region || '',
          occupation: info.occupation || '',
          role: info.role || 'GUEST'
        }
      }
    } catch (parseError) {
      console.error('解析用户信息失败:', parseError)
    }
  }
}

const loadDashboardData = async () => {
  try {
    dashboardStats.value = await getDashboardStats()
  } catch (error) {
    console.error('加载工作台统计失败:', error)
  }

  try {
    adminStats.value = await getAdminStats()
  } catch (error) {
    console.error('加载后台统计失败:', error)
  }
}

const loadNotifications = async () => {
  try {
    loadingNotifications.value = true
    notifications.value = (await getSystemNotifications()).slice(0, 6)
  } catch (error) {
    console.error('加载系统通知失败:', error)
    notifications.value = []
  } finally {
    loadingNotifications.value = false
  }
}

const getRoleText = (role: string) => {
  const roleMap: Record<string, string> = {
    GUEST: '游客',
    STUDENT: '学生',
    MERCHANT: '商家',
    ADMIN: '管理员'
  }
  return roleMap[role] || '未知'
}

const getRoleClass = (role: string) => {
  const classMap: Record<string, string> = {
    GUEST: 'role-guest',
    STUDENT: 'role-student',
    MERCHANT: 'role-merchant',
    ADMIN: 'role-admin'
  }
  return classMap[role] || ''
}

const formatTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  void loadUserInfo()
  void loadDashboardData()
  void loadNotifications()
})
</script>

<style scoped>
.admin-profile-info {
  width: 100%;
}

.profile-card {
  background: white;
  border-radius: 8px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
}

.profile-avatar-wrapper {
  position: relative;
}

.profile-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #f0f0f0;
}

.profile-avatar-placeholder {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 48px;
  font-weight: 700;
  border: 4px solid #f0f0f0;
}

.profile-info {
  flex: 1;
}

.profile-name {
  margin: 0 0 12px 0;
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.profile-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.role-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
}

.role-guest {
  background: #f0f0f0;
  color: #666;
}

.role-student {
  background: #e6f4ff;
  color: #1677ff;
}

.role-merchant {
  background: #fff7e6;
  color: #fa8c16;
}

.role-admin {
  background: #f6ffed;
  color: #52c41a;
}

.region {
  color: #666;
  font-size: 14px;
}

.profile-contact {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
}

.contact-item svg {
  width: 16px;
  height: 16px;
}

.bio-item {
  max-width: 520px;
  line-height: 1.6;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.content-left,
.content-right {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.metric-item {
  padding: 18px;
  border-radius: 10px;
  background: linear-gradient(180deg, #f7fbff 0%, #edf5ff 100%);
  border: 1px solid #d9e8ff;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
  color: #1677ff;
  margin-bottom: 6px;
}

.metric-label {
  font-size: 13px;
  color: #666;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.task-main {
  flex: 1;
}

.task-label {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.task-desc {
  font-size: 12px;
  color: #999;
}

.task-count {
  min-width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #f5f5f5;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
}

.task-count.highlight {
  background: #fff2f0;
  color: #ff4d4f;
}

.distribution-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.distribution-item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px 12px;
  align-items: center;
}

.distribution-main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
  color: #333;
}

.distribution-percent {
  color: #666;
}

.distribution-bar {
  grid-column: 1 / 2;
  height: 8px;
  border-radius: 999px;
  background: #f2f4f7;
  overflow: hidden;
}

.distribution-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.distribution-value {
  grid-column: 2 / 3;
  grid-row: 2 / 3;
  font-size: 13px;
  color: #666;
}

.notification-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
}

.notification-empty svg {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.notification-empty p {
  margin: 0;
  font-size: 14px;
}

.compact-empty {
  padding: 20px 0;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
}

.notification-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 6px;
}

.notification-content {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}

.notification-badge {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 999px;
  background: #fff2f0;
  color: #ff4d4f;
  font-size: 12px;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

@media (max-width: 768px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .profile-header {
    flex-direction: column;
    text-align: center;
  }

  .profile-meta,
  .profile-contact {
    justify-content: center;
  }
}
</style>
