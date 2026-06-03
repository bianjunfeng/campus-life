<template>
  <div class="admin-admin-login-log">
    <!-- 筛选条件 -->
    <div class="filter-section">
      <input 
        type="text" 
        v-model="filterAdminId" 
        placeholder="管理员ID"
        @keyup.enter="loadLogs"
      />
      <input 
        type="text" 
        v-model="filterUsername" 
        placeholder="用户名"
        @keyup.enter="loadLogs"
      />
      <input 
        type="date" 
        v-model="startDate" 
        placeholder="开始时间"
      />
      <input 
        type="date" 
        v-model="endDate" 
        placeholder="结束时间"
      />
      <button @click="loadLogs">查询</button>
      <button @click="resetFilters" class="btn-secondary">重置</button>
    </div>
    
    <!-- 日志列表 -->
    <div class="log-list">
      <table class="data-table">
        <thead>
          <tr>
            <th>日志ID</th>
            <th>管理员ID</th>
            <th>用户名</th>
            <th>登录IP</th>
            <th>登录地点</th>
            <th>登录时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logList" :key="log.id">
            <td>{{ log.id }}</td>
            <td>{{ log.adminId }}</td>
            <td>{{ log.username || '-' }}</td>
            <td>{{ log.loginIp || '-' }}</td>
            <td>{{ log.loginLocation || '-' }}</td>
            <td>{{ formatTime(log.loginTime) }}</td>
            <td class="action-cell">
              <button @click="viewDetails(log)" class="btn btn-primary">查看详情</button>
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
        <h3>管理员登录日志详情</h3>
        <div class="detail-content" v-if="selectedLog">
          <div class="detail-row">
            <span class="detail-label">日志ID：</span>
            <span class="detail-value">{{ selectedLog.id }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">管理员ID：</span>
            <span class="detail-value">{{ selectedLog.adminId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">用户名：</span>
            <span class="detail-value">{{ selectedLog.username || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">登录IP：</span>
            <span class="detail-value">{{ selectedLog.loginIp || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">登录地点：</span>
            <span class="detail-value">{{ selectedLog.loginLocation || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">登录时间：</span>
            <span class="detail-value">{{ formatTime(selectedLog.loginTime) }}</span>
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
import { getAdminLoginLogList, type AdminLoginLog } from '../../api/admin'

const logList = ref<AdminLoginLog[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterAdminId = ref('')
const filterUsername = ref('')
const startDate = ref('')
const endDate = ref('')
const showDetailDialog = ref(false)
const selectedLog = ref<AdminLoginLog | null>(null)

const totalPages = computed(() => Math.ceil(total.value / size.value))

const loadLogs = async () => {
  try {
    const params: any = {
      page: page.value,
      size: size.value
    }
    if (filterAdminId.value) {
      params.adminId = parseInt(filterAdminId.value)
    }
    if (filterUsername.value) {
      params.username = filterUsername.value
    }
    if (startDate.value) {
      params.startTime = startDate.value + ' 00:00:00'
    }
    if (endDate.value) {
      params.endTime = endDate.value + ' 23:59:59'
    }
    
    const data = await getAdminLoginLogList(params)
    logList.value = data.list
    total.value = data.total
  } catch (error) {
    console.error('加载管理员登录日志失败:', error)
  }
}

const resetFilters = () => {
  filterAdminId.value = ''
  filterUsername.value = ''
  startDate.value = ''
  endDate.value = ''
  page.value = 1
  loadLogs()
}

const prevPage = () => {
  if (page.value > 1) {
    page.value--
    loadLogs()
  }
}

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    loadLogs()
  }
}

const viewDetails = (log: AdminLoginLog) => {
  selectedLog.value = log
  showDetailDialog.value = true
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.admin-admin-login-log {
  width: 100%;
}

.filter-section {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-section input,
.filter-section select {
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

.filter-section button:hover {
  opacity: 0.9;
}

.log-list {
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

.btn-primary {
  background: #1677ff;
  color: white;
}

.btn-primary:hover {
  background: #0958d9;
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
  border-radius: 8px;
  padding: 24px;
  min-width: 500px;
  max-width: 90%;
}

.dialog h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #333;
}

.detail-content {
  margin-bottom: 20px;
}

.detail-row {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-label {
  width: 120px;
  color: #666;
  font-weight: 500;
}

.detail-value {
  flex: 1;
  color: #333;
}

.dialog-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

@media (max-width: 768px) {
  .filter-section {
    flex-direction: column;
  }
  
  .filter-section input,
  .filter-section select,
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
  
  .dialog {
    min-width: auto;
    width: 90%;
    padding: 16px;
  }
}
</style>

