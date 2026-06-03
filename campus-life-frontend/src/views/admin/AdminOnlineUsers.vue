<template>
  <div class="admin-online-users">
    <!-- 筛选条件 -->
    <div class="filter-section">
      <input 
        type="text" 
        v-model="filterUsername" 
        placeholder="用户名"
        @keyup.enter="loadUsers"
      />
      <input 
        type="text" 
        v-model="filterIp" 
        placeholder="IP地址"
        @keyup.enter="loadUsers"
      />
      <button @click="loadUsers">查询</button>
      <button @click="resetFilters" class="btn-secondary">重置</button>
      <button @click="refreshData" class="btn-refresh">刷新</button>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">在线用户数</div>
          <div class="stat-value">{{ totalOnline }}</div>
        </div>
      </div>
    </div>
    
    <!-- 在线用户列表 -->
    <div class="user-list">
      <table class="data-table">
        <thead>
          <tr>
            <th>用户ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>登录IP</th>
            <th>登录地点</th>
            <th>登录时间</th>
            <th>最后活跃时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in userList" :key="user.userId">
            <td>{{ user.userId }}</td>
            <td>{{ user.username || '-' }}</td>
            <td>{{ user.nickname || '-' }}</td>
            <td>{{ user.loginIp || '-' }}</td>
            <td>{{ user.loginLocation || '-' }}</td>
            <td>{{ formatTime(user.loginTime) }}</td>
            <td>{{ formatTime(user.lastActiveTime) }}</td>
            <td class="action-cell">
              <button @click="forceLogout(user.userId)" class="btn btn-danger">强制下线</button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- 分页 -->
      <div class="pagination">
        <button @click="prevPage" :disabled="page === 1">上一页</button>
        <span>第 {{ page }} 页，共 {{ totalPages }} 页，共 {{ total }} 条</span>
        <button @click="nextPage" :disabled="page >= totalPages">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { getOnlineUsers, forceLogoutUser, type OnlineUser } from '../../api/admin'
import { showAlert, showConfirm } from '../../utils/dialog'

const userList = ref<OnlineUser[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const totalOnline = ref(0)
const filterUsername = ref('')
const filterIp = ref('')
let refreshTimer: number | null = null

const totalPages = computed(() => Math.ceil(total.value / size.value))

const loadUsers = async () => {
  try {
    const params: any = {
      page: page.value,
      size: size.value
    }
    if (filterUsername.value) {
      params.username = filterUsername.value
    }
    if (filterIp.value) {
      params.ip = filterIp.value
    }
    
    const data = await getOnlineUsers(params)
    userList.value = data.list
    total.value = data.total
    totalOnline.value = data.totalOnline || data.total
  } catch (error) {
    console.error('加载在线用户失败:', error)
  }
}

const resetFilters = () => {
  filterUsername.value = ''
  filterIp.value = ''
  page.value = 1
  loadUsers()
}

const refreshData = () => {
  loadUsers()
}

const prevPage = () => {
  if (page.value > 1) {
    page.value--
    loadUsers()
  }
}

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    loadUsers()
  }
}

const forceLogout = async (userId: number) => {
  const confirmed = await showConfirm('确定要强制该用户下线吗？')
  if (!confirmed) return
  try {
    await forceLogoutUser(userId)
    showAlert('操作成功')
    loadUsers()
  } catch (error) {
    console.error('强制下线失败:', error)
    showAlert('操作失败')
  }
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 自动刷新（每30秒）
const startAutoRefresh = () => {
  refreshTimer = window.setInterval(() => {
    loadUsers()
  }, 30000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadUsers()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.admin-online-users {
  width: 100%;
}

.filter-section {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-section input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.filter-section button {
  padding: 8px 16px;
  background: #1677ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.filter-section button.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.filter-section button.btn-refresh {
  background: #52c41a;
}

.filter-section button:hover {
  opacity: 0.9;
}

.stats-section {
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: #e6f4ff;
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon svg {
  width: 28px;
  height: 28px;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #333;
}

.user-list {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  background: #f5f5f5;
  padding: 12px;
  text-align: left;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #e0e0e0;
}

.data-table td {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.action-cell {
  white-space: nowrap;
}

.btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  margin-right: 8px;
}

.btn-danger {
  background: #ff4d4f;
  color: white;
}

.btn-danger:hover {
  background: #ff7875;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fafafa;
}

.pagination button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .filter-section {
    flex-direction: column;
  }
  
  .filter-section input,
  .filter-section button {
    width: 100%;
  }
  
  .data-table {
    font-size: 12px;
  }
  
  .data-table th,
  .data-table td {
    padding: 8px;
  }
  
  .stat-card {
    padding: 16px;
  }
  
  .stat-value {
    font-size: 24px;
  }
}
</style>
