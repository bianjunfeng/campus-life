<template>
  <div class="admin-user-manage">
    <!-- 筛选条件 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <label>用户名</label>
          <input 
            type="text" 
            v-model="filters.username" 
            placeholder="请输入用户名"
            @keyup.enter="loadUsers"
          />
        </div>
        <div class="filter-item">
          <label>手机号</label>
          <input 
            type="text" 
            v-model="filters.phone" 
            placeholder="请输入手机号"
            @keyup.enter="loadUsers"
          />
        </div>
        <div class="filter-item">
          <label>邮箱</label>
          <input 
            type="text" 
            v-model="filters.email" 
            placeholder="请输入邮箱"
            @keyup.enter="loadUsers"
          />
        </div>
      </div>
      <div class="filter-row">
        <div class="filter-item">
          <label>用户角色</label>
          <select v-model="filters.role" @change="loadUsers">
            <option value="">全部角色</option>
            <option value="0">学生</option>
            <option value="1">商家</option>
            <option value="2">管理员</option>
          </select>
        </div>
        <div class="filter-item">
          <label>用户状态</label>
          <select v-model="filters.status" @change="loadUsers">
            <option value="">全部状态</option>
            <option value="1">正常</option>
            <option value="0">禁用</option>
          </select>
        </div>
        <div class="filter-item">
          <label>注册时间</label>
          <div class="date-range">
            <input 
              type="date" 
              v-model="filters.startDate" 
            />
            <span class="date-separator">至</span>
            <input 
              type="date" 
              v-model="filters.endDate" 
            />
          </div>
        </div>
      </div>
      <div class="filter-actions">
        <button class="btn btn-primary" @click="loadUsers">查询</button>
        <button class="btn btn-secondary" @click="resetFilters">重置</button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section">
      <div class="stat-card" @click="filterByRole('0')">
        <div class="stat-icon student">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">学生</div>
          <div class="stat-value">{{ stats.student }}</div>
        </div>
      </div>
      <div class="stat-card" @click="filterByRole('1')">
        <div class="stat-icon merchant">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <path d="M16 10a4 4 0 0 1-8 0"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">商家</div>
          <div class="stat-value">{{ stats.merchant }}</div>
        </div>
      </div>
      <div class="stat-card" @click="filterByRole('2')">
        <div class="stat-icon admin">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
            <path d="M2 17l10 5 10-5"></path>
            <path d="M2 12l10 5 10-5"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">管理员</div>
          <div class="stat-value">{{ stats.admin }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon total">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">用户总数</div>
          <div class="stat-value">{{ stats.total }}</div>
        </div>
      </div>
    </div>
    
    <!-- 用户列表 -->
    <div class="user-list">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>头像</th>
            <th>用户名</th>
            <th>手机号</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>状态</th>
            <th>性别</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in userList" :key="user.id">
            <td>{{ user.id }}</td>
            <td>
              <img 
                v-if="user.avatarUrl" 
                :src="user.avatarUrl" 
                alt="头像" 
                class="avatar-img"
              />
              <span v-else class="avatar-placeholder">无</span>
            </td>
            <td>{{ user.username || '-' }}</td>
            <td>{{ user.phone || '-' }}</td>
            <td>{{ user.email || '-' }}</td>
            <td>
              <span :class="['role-badge', getRoleClass(user.role)]">
                {{ getRoleText(user.role) }}
              </span>
            </td>
            <td>
              <span :class="['status-badge', user.status === 1 ? 'status-active' : 'status-inactive']">
                {{ user.status === 1 ? '正常' : '禁用' }}
              </span>
            </td>
            <td>{{ getGenderText(user.gender) }}</td>
            <td>{{ formatTime(user.createTime) }}</td>
            <td class="action-cell">
              <button @click="viewDetails(user)" class="btn btn-primary btn-sm">详情</button>
              <button 
                v-if="user.status === 1" 
                @click="disableUser(user.id)" 
                class="btn btn-warning btn-sm"
              >
                禁用
              </button>
              <button 
                v-else 
                @click="enableUser(user.id)" 
                class="btn btn-success btn-sm"
              >
                启用
              </button>
              <button 
                v-if="user.role !== 2" 
                @click="deleteUser(user.id)" 
                class="btn btn-danger btn-sm"
              >
                删除
              </button>
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
    
    <!-- 详情对话框 -->
    <div v-if="showDetailDialog" class="dialog-overlay" @click="showDetailDialog = false">
      <div class="dialog" @click.stop>
        <h3>用户详情</h3>
        <div class="detail-content" v-if="selectedUser">
          <div class="detail-row">
            <span class="detail-label">用户ID：</span>
            <span class="detail-value">{{ selectedUser.id }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">用户名：</span>
            <span class="detail-value">{{ selectedUser.username || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">手机号：</span>
            <span class="detail-value">{{ selectedUser.phone || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">邮箱：</span>
            <span class="detail-value">{{ selectedUser.email || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">角色：</span>
            <span :class="['role-badge', getRoleClass(selectedUser.role)]">
              {{ getRoleText(selectedUser.role) }}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">状态：</span>
            <span :class="['status-badge', selectedUser.status === 1 ? 'status-active' : 'status-inactive']">
              {{ selectedUser.status === 1 ? '正常' : '禁用' }}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">性别：</span>
            <span class="detail-value">{{ getGenderText(selectedUser.gender) }}</span>
          </div>
          <div class="detail-row" v-if="selectedUser.bio">
            <span class="detail-label">个人简介：</span>
            <span class="detail-value">{{ selectedUser.bio }}</span>
          </div>
          <div class="detail-row" v-if="selectedUser.region">
            <span class="detail-label">地区：</span>
            <span class="detail-value">{{ selectedUser.region }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">注册时间：</span>
            <span class="detail-value">{{ formatTime(selectedUser.createTime) }}</span>
          </div>
        </div>
        <div class="dialog-buttons">
          <button @click="showDetailDialog = false" class="btn btn-secondary">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getUserList, updateUserStatus, deleteUserById, getUserStats, type SystemUser } from '../../api/admin'
import { showAlert, showConfirm } from '../../utils/dialog'

const userList = ref<SystemUser[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const stats = ref({
  student: 0,
  merchant: 0,
  admin: 0,
  total: 0
})
const filters = ref({
  username: '',
  phone: '',
  email: '',
  role: '',
  status: '',
  startDate: '',
  endDate: ''
})
const showDetailDialog = ref(false)
const selectedUser = ref<SystemUser | null>(null)

const totalPages = computed(() => Math.ceil(total.value / size.value))

const loadUsers = async () => {
  try {
    const params: any = {
      page: page.value,
      size: size.value
    }
    if (filters.value.username) {
      params.username = filters.value.username
    }
    if (filters.value.phone) {
      params.phone = filters.value.phone
    }
    if (filters.value.email) {
      params.email = filters.value.email
    }
    if (filters.value.role !== '') {
      params.role = parseInt(filters.value.role)
    }
    if (filters.value.status !== '') {
      params.status = parseInt(filters.value.status)
    }
    if (filters.value.startDate) {
      params.startDate = filters.value.startDate
    }
    if (filters.value.endDate) {
      params.endDate = filters.value.endDate
    }
    
    const data = await getUserList(params)
    userList.value = data.list
    total.value = data.total
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
}

const loadStats = async () => {
  try {
    const data = await getUserStats()
    stats.value = data
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const resetFilters = () => {
  filters.value = {
    username: '',
    phone: '',
    email: '',
    role: '',
    status: '',
    startDate: '',
    endDate: ''
  }
  page.value = 1
  loadUsers()
}

const filterByRole = (role: string) => {
  filters.value.role = role
  page.value = 1
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

const viewDetails = (user: SystemUser) => {
  selectedUser.value = user
  showDetailDialog.value = true
}

const enableUser = async (userId: number) => {
  if (await showConfirm('确定要启用该用户吗？')) {
    try {
      await updateUserStatus(userId, 1)
      await showAlert('操作成功')
      loadUsers()
      loadStats()
    } catch (error) {
      console.error('启用用户失败:', error)
      await showAlert('操作失败')
    }
  }
}

const disableUser = async (userId: number) => {
  if (await showConfirm('确定要禁用该用户吗？')) {
    try {
      await updateUserStatus(userId, 0)
      await showAlert('操作成功')
      loadUsers()
      loadStats()
    } catch (error) {
      console.error('禁用用户失败:', error)
      await showAlert('操作失败')
    }
  }
}

const deleteUser = async (userId: number) => {
  if (await showConfirm('确定要删除该用户吗？此操作不可恢复！')) {
    try {
      await deleteUserById(userId)
      await showAlert('删除成功')
      loadUsers()
      loadStats()
    } catch (error) {
      console.error('删除用户失败:', error)
      await showAlert('删除失败')
    }
  }
}

const getRoleText = (role: number) => {
  const roleMap: Record<number, string> = {
    0: '学生',
    1: '商家',
    2: '管理员'
  }
  return roleMap[role] || '未知'
}

const getRoleClass = (role: number) => {
  const classMap: Record<number, string> = {
    0: 'role-student',
    1: 'role-merchant',
    2: 'role-admin'
  }
  return classMap[role] || ''
}

const getGenderText = (gender?: number) => {
  if (gender === undefined || gender === null) return '-'
  const genderMap: Record<number, string> = {
    0: '保密',
    1: '男',
    2: '女'
  }
  return genderMap[gender] || '-'
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadUsers()
  loadStats()
})
</script>

<style scoped>
.admin-user-manage {
  width: 100%;
}

.filter-section {
  background: #fff;
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.filter-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-item {
  flex: 1;
  min-width: 200px;
}

.filter-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
}

.filter-item input,
.filter-item select {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #d3ddec;
  border-radius: 8px;
  font-size: 14px;
}

.filter-item input:focus,
.filter-item select:focus {
  outline: none;
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}

.date-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-separator {
  color: #999;
  font-size: 14px;
}

.filter-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 16px;
}

.btn {
  padding: 8px 16px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.btn-primary {
  background: #1677ff;
  color: white;
  border-color: #1677ff;
}

.btn-secondary {
  background: #f8fafc;
  color: #475569;
  border-color: #d3ddec;
}

.btn:hover {
  opacity: 0.9;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.student {
  background: #e6f4ff;
  color: #1677ff;
}

.stat-icon.merchant {
  background: #fff7e6;
  color: #fa8c16;
}

.stat-icon.admin {
  background: #f6ffed;
  color: #52c41a;
}

.stat-icon.total {
  background: #f1f5f9;
  color: #475569;
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
  color: #64748b;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.user-list {
  background: #fff;
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  background: #f8fbff;
  padding: 12px;
  text-align: left;
  font-weight: 600;
  color: #334155;
  border-bottom: 1px solid #e3ebf5;
}

.data-table td {
  padding: 12px;
  border-bottom: 1px solid #edf2f7;
}

.avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  display: inline-block;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #eef2f7;
  text-align: center;
  line-height: 40px;
  color: #999;
  font-size: 12px;
}

.role-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
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

.status-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: #f6ffed;
  color: #52c41a;
}

.status-inactive {
  background: #fff2f0;
  color: #ff4d4f;
}

.action-cell {
  white-space: nowrap;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
  margin-right: 4px;
}

.btn-success {
  background: #16a34a;
  border-color: #16a34a;
  color: white;
}

.btn-warning {
  background: #f59e0b;
  border-color: #f59e0b;
  color: white;
}

.btn-danger {
  background: #ef4444;
  border-color: #ef4444;
  color: white;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f8fbff;
  border-top: 1px solid #e3ebf5;
}

.pagination button {
  padding: 8px 16px;
  border: 1px solid #d3ddec;
  background: white;
  border-radius: 8px;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background: white;
  border-radius: 12px;
  border: 1px solid #e3ebf5;
  padding: 24px;
  min-width: 500px;
  max-width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.16);
}

.dialog h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #0f172a;
}

.detail-content {
  margin-bottom: 20px;
}

.detail-row {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #edf2f7;
  align-items: center;
}

.detail-label {
  width: 120px;
  color: #64748b;
  font-weight: 500;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: #1f2937;
}

.dialog-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
  }
  
  .filter-item {
    width: 100%;
  }
  
  .stats-section {
    grid-template-columns: 1fr;
  }
  
  .data-table {
    font-size: 12px;
  }
  
  .data-table th,
  .data-table td {
    padding: 8px;
  }
  
  .dialog {
    min-width: auto;
    width: 90%;
    padding: 16px;
  }
}
</style>
