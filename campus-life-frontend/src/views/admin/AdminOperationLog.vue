<template>
  <div class="admin-operation-log">
    <!-- 筛选条件 -->
    <div class="filter-section">
      <input 
        type="text" 
        v-model="filterAdminId" 
        placeholder="管理员ID"
        @keyup.enter="loadLogs"
      />
      <select v-model="filterOperationType" @change="loadLogs">
        <option value="">全部操作类型</option>
        <option value="post">帖子</option>
        <option value="comment">评论</option>
        <option value="merchant">商家</option>
        <option value="voucher">优惠券</option>
        <option value="member">会员</option>
      </select>
      <select v-model="filterTargetType" @change="loadLogs">
        <option value="">全部目标类型</option>
        <option value="post">帖子</option>
        <option value="comment">评论</option>
        <option value="merchant">商家</option>
        <option value="voucher">优惠券</option>
        <option value="member">会员</option>
      </select>
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
            <th>操作类型</th>
            <th>目标类型</th>
            <th>目标ID</th>
            <th>操作</th>
            <th>旧状态</th>
            <th>新状态</th>
            <th>IP地址</th>
            <th>操作时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logList" :key="log.id">
            <td>{{ log.id }}</td>
            <td>{{ log.adminId }}</td>
            <td>{{ log.operationType }}</td>
            <td>{{ log.targetType }}</td>
            <td>{{ log.targetId }}</td>
            <td>{{ log.action }}</td>
            <td>{{ log.oldStatus !== null ? log.oldStatus : '-' }}</td>
            <td>{{ log.newStatus !== null ? log.newStatus : '-' }}</td>
            <td>{{ log.ipAddress }}</td>
            <td>{{ formatTime(log.createTime) }}</td>
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
        <h3>操作日志详情</h3>
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
            <span class="detail-label">操作类型：</span>
            <span class="detail-value">{{ selectedLog.operationType }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">目标类型：</span>
            <span class="detail-value">{{ selectedLog.targetType }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">目标ID：</span>
            <span class="detail-value">{{ selectedLog.targetId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作：</span>
            <span class="detail-value">{{ selectedLog.action }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">旧状态：</span>
            <span class="detail-value">{{ selectedLog.oldStatus !== null ? selectedLog.oldStatus : '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">新状态：</span>
            <span class="detail-value">{{ selectedLog.newStatus !== null ? selectedLog.newStatus : '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作原因：</span>
            <span class="detail-value">{{ selectedLog.reason || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">IP地址：</span>
            <span class="detail-value">{{ selectedLog.ipAddress }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作时间：</span>
            <span class="detail-value">{{ formatTime(selectedLog.createTime) }}</span>
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
import { getAdminOperationLogList, type AdminOperationLog } from '../../api/admin'

const logList = ref<AdminOperationLog[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterAdminId = ref('')
const filterOperationType = ref('')
const filterTargetType = ref('')
const startDate = ref('')
const endDate = ref('')
const showDetailDialog = ref(false)
const selectedLog = ref<AdminOperationLog | null>(null)

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
    if (filterOperationType.value) {
      params.operationType = filterOperationType.value
    }
    if (filterTargetType.value) {
      params.targetType = filterTargetType.value
    }
    if (startDate.value) {
      params.startTime = startDate.value + ' 00:00:00'
    }
    if (endDate.value) {
      params.endTime = endDate.value + ' 23:59:59'
    }
    
    const data = await getAdminOperationLogList(params)
    logList.value = data.list
    total.value = data.total
  } catch (error) {
    console.error('加载管理员操作日志失败:', error)
  }
}

const resetFilters = () => {
  filterAdminId.value = ''
  filterOperationType.value = ''
  filterTargetType.value = ''
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

const viewDetails = (log: AdminOperationLog) => {
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
.admin-operation-log {
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
  font-size: 13px;
}

.data-table th {
  background: #f5f5f5;
  padding: 12px 8px;
  text-align: left;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #e0e0e0;
}

.data-table td {
  padding: 12px 8px;
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
  max-height: 80vh;
  overflow-y: auto;
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
    font-size: 11px;
  }
  
  .data-table th,
  .data-table td {
    padding: 6px 4px;
  }
  
  .dialog {
    min-width: auto;
    width: 90%;
    padding: 16px;
  }
}
</style>

