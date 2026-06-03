<template>
  <div class="admin-audit-log">
    <!-- 筛选条件 -->
    <div class="filter-section">
      <input 
        type="text" 
        v-model="filterContentId" 
        placeholder="内容ID"
        @keyup.enter="loadLogs"
      />
      <select v-model="filterContentType" @change="loadLogs">
        <option value="">全部内容类型</option>
        <option value="post">笔记</option>
        <option value="comment">评论</option>
      </select>
      <input 
        type="text" 
        v-model="filterProvider" 
        placeholder="审核提供商"
        @keyup.enter="loadLogs"
      />
      <select v-model="filterResult" @change="loadLogs">
        <option value="">全部审核结果</option>
        <option value="pass">通过</option>
        <option value="reject">拒绝</option>
        <option value="manual">人工审核</option>
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
            <th>内容ID</th>
            <th>内容类型</th>
            <th>审核结果</th>
            <th>审核分数</th>
            <th>审核提供商</th>
            <th>审核时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logList" :key="log.id">
            <td>{{ log.id }}</td>
            <td>{{ log.contentId }}</td>
            <td>{{ getContentTypeText(log.contentType) }}</td>
            <td>
              <span :class="['status-badge', getResultClass(log.result)]">
                {{ getResultText(log.result) }}
              </span>
            </td>
            <td>{{ log.score !== null ? log.score : '-' }}</td>
            <td>{{ log.provider || '-' }}</td>
            <td>{{ formatTime(log.auditTime) }}</td>
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
        <h3>审核日志详情</h3>
        <div class="detail-content" v-if="selectedLog">
          <div class="detail-row">
            <span class="detail-label">日志ID：</span>
            <span class="detail-value">{{ selectedLog.id }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">内容ID：</span>
            <span class="detail-value">{{ selectedLog.contentId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">内容类型：</span>
            <span class="detail-value">{{ getContentTypeText(selectedLog.contentType) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">审核结果：</span>
            <span :class="['status-badge', getResultClass(selectedLog.result)]">
              {{ getResultText(selectedLog.result) }}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">审核分数：</span>
            <span class="detail-value">{{ selectedLog.score !== null ? selectedLog.score : '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">审核提供商：</span>
            <span class="detail-value">{{ selectedLog.provider || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">审核详情：</span>
            <span class="detail-value">{{ selectedLog.details || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">审核时间：</span>
            <span class="detail-value">{{ formatTime(selectedLog.auditTime) }}</span>
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
import { getAuditLogList, type AuditLog } from '../../api/admin'

const logList = ref<AuditLog[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterContentId = ref('')
const filterContentType = ref('')
const filterProvider = ref('')
const filterResult = ref('')
const startDate = ref('')
const endDate = ref('')
const showDetailDialog = ref(false)
const selectedLog = ref<AuditLog | null>(null)

const totalPages = computed(() => Math.ceil(total.value / size.value))

const loadLogs = async () => {
  try {
    const params: any = {
      page: page.value,
      size: size.value
    }
    if (filterContentId.value) {
      params.contentId = parseInt(filterContentId.value)
    }
    if (filterContentType.value) {
      params.contentType = filterContentType.value
    }
    if (filterProvider.value) {
      params.provider = filterProvider.value
    }
    if (filterResult.value) {
      params.result = filterResult.value
    }
    if (startDate.value) {
      params.startTime = startDate.value + ' 00:00:00'
    }
    if (endDate.value) {
      params.endTime = endDate.value + ' 23:59:59'
    }
    
    const data = await getAuditLogList(params)
    logList.value = data.list
    total.value = data.total
  } catch (error) {
    console.error('加载审核日志失败:', error)
  }
}

const resetFilters = () => {
  filterContentId.value = ''
  filterContentType.value = ''
  filterProvider.value = ''
  filterResult.value = ''
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

const viewDetails = (log: AuditLog) => {
  selectedLog.value = log
  showDetailDialog.value = true
}

const getContentTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    post: '笔记',
    comment: '评论'
  }
  return typeMap[type] || type
}

const getResultText = (result: string) => {
  const resultMap: Record<string, string> = {
    pass: '通过',
    reject: '拒绝',
    manual: '人工审核'
  }
  return resultMap[result] || result
}

const getResultClass = (result: string) => {
  const classMap: Record<string, string> = {
    pass: 'status-success',
    reject: 'status-danger',
    manual: 'status-warning'
  }
  return classMap[result] || ''
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
.admin-audit-log {
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

.status-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-success {
  background: #f6ffed;
  color: #52c41a;
}

.status-danger {
  background: #fff2f0;
  color: #ff4d4f;
}

.status-warning {
  background: #fffbe6;
  color: #faad14;
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
  align-items: center;
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

