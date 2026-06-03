<template>
  <div class="admin-cache">
    <!-- 筛选条件 -->
    <div class="filter-section">
      <input 
        type="text" 
        v-model="filterKey" 
        placeholder="缓存键（支持模糊搜索）"
        @keyup.enter="loadCache"
      />
      <button @click="loadCache">查询</button>
      <button @click="resetFilters" class="btn-secondary">重置</button>
      <button @click="refreshData" class="btn-refresh">刷新</button>
      <button @click="clearAllCache" class="btn-danger">清空所有缓存</button>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="9" y1="3" x2="9" y2="21"></line>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">缓存数量</div>
          <div class="stat-value">{{ totalCache }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon memory">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="4" width="20" height="16" rx="2"></rect>
            <line x1="6" y1="8" x2="6" y2="16"></line>
            <line x1="10" y1="8" x2="10" y2="16"></line>
            <line x1="14" y1="8" x2="14" y2="16"></line>
            <line x1="18" y1="8" x2="18" y2="16"></line>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">内存使用</div>
          <div class="stat-value">{{ memoryUsage }}</div>
        </div>
      </div>
    </div>
    
    <!-- 缓存列表 -->
    <div class="cache-list">
      <table class="data-table">
        <thead>
          <tr>
            <th>缓存键</th>
            <th>缓存值（预览）</th>
            <th>类型</th>
            <th>大小</th>
            <th>过期时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="cache in cacheList" :key="cache.key">
            <td class="key-cell">{{ cache.key }}</td>
            <td class="value-cell">{{ previewValue(cache.value) }}</td>
            <td>{{ cache.type }}</td>
            <td>{{ formatSize(cache.size) }}</td>
            <td>{{ cache.expireTime ? formatTime(cache.expireTime) : '永久' }}</td>
            <td class="action-cell">
              <button @click="viewDetails(cache)" class="btn btn-primary">查看详情</button>
              <button @click="deleteCache(cache.key)" class="btn btn-danger">删除</button>
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
        <h3>缓存详情</h3>
        <div class="detail-content" v-if="selectedCache">
          <div class="detail-row">
            <span class="detail-label">缓存键：</span>
            <span class="detail-value">{{ selectedCache.key }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">类型：</span>
            <span class="detail-value">{{ selectedCache.type }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">大小：</span>
            <span class="detail-value">{{ formatSize(selectedCache.size) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">过期时间：</span>
            <span class="detail-value">{{ selectedCache.expireTime ? formatTime(selectedCache.expireTime) : '永久' }}</span>
          </div>
          <div class="detail-row full-width">
            <span class="detail-label">缓存值：</span>
            <pre class="detail-value-pre">{{ formatValue(selectedCache.value) }}</pre>
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
import { getCacheList, deleteCacheItem, clearAllCache as clearAllCacheApi, type CacheItem } from '../../api/admin'
import { showAlert, showConfirm } from '../../utils/dialog'

const cacheList = ref<CacheItem[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const totalCache = ref(0)
const memoryUsage = ref('0 MB')
const filterKey = ref('')
const showDetailDialog = ref(false)
const selectedCache = ref<CacheItem | null>(null)

const totalPages = computed(() => Math.ceil(total.value / size.value))

const loadCache = async () => {
  try {
    const params: any = {
      page: page.value,
      size: size.value
    }
    if (filterKey.value) {
      params.key = filterKey.value
    }
    
    const data = await getCacheList(params)
    cacheList.value = data.list
    total.value = data.total
    totalCache.value = data.totalCache || data.total
    memoryUsage.value = data.memoryUsage || '0 MB'
  } catch (error) {
    console.error('加载缓存列表失败:', error)
  }
}

const resetFilters = () => {
  filterKey.value = ''
  page.value = 1
  loadCache()
}

const refreshData = () => {
  loadCache()
}

const prevPage = () => {
  if (page.value > 1) {
    page.value--
    loadCache()
  }
}

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    loadCache()
  }
}

const previewValue = (value: any) => {
  if (value === null || value === undefined) return '-'
  const str = typeof value === 'string' ? value : JSON.stringify(value)
  return str.length > 50 ? str.substring(0, 50) + '...' : str
}

const formatValue = (value: any) => {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'string') return value
  return JSON.stringify(value, null, 2)
}

const formatSize = (size: number) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const viewDetails = (cache: CacheItem) => {
  selectedCache.value = cache
  showDetailDialog.value = true
}

const deleteCache = async (key: string) => {
  const confirmed = await showConfirm(`确定要删除缓存 "${key}" 吗？`)
  if (!confirmed) return
  try {
    await deleteCacheItem(key)
    showAlert('删除成功')
    loadCache()
  } catch (error) {
    console.error('删除缓存失败:', error)
    showAlert('删除失败')
  }
}

const clearAllCache = async () => {
  const confirmed = await showConfirm('确定要清空所有缓存吗？此操作不可恢复！')
  if (!confirmed) return
  try {
    await clearAllCacheApi()
    showAlert('清空成功')
    loadCache()
  } catch (error) {
    console.error('清空缓存失败:', error)
    showAlert('清空失败')
  }
}

onMounted(() => {
  loadCache()
})
</script>

<style scoped>
.admin-cache {
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
  flex: 1;
  min-width: 200px;
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

.filter-section button.btn-danger {
  background: #ff4d4f;
}

.filter-section button:hover {
  opacity: 0.9;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
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

.stat-icon.memory {
  background: #f6ffed;
  color: #52c41a;
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
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.cache-list {
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

.key-cell {
  font-family: monospace;
  font-size: 12px;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.value-cell {
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
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
  min-width: 600px;
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

.detail-row.full-width {
  flex-direction: column;
}

.detail-label {
  width: 120px;
  color: #666;
  font-weight: 500;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: #333;
  word-break: break-all;
}

.detail-value-pre {
  flex: 1;
  color: #333;
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  font-family: monospace;
  font-size: 12px;
  margin: 8px 0 0 0;
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
  .filter-section button {
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
