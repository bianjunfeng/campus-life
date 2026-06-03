<template>
  <div class="admin-post-manage">
    <!-- 筛选条件 -->
    <div class="filter-section">
      <select v-model="filterStatus" @change="loadPosts">
        <option value="">全部状态</option>
        <option value="0">正常</option>
        <option value="1">仅自己可见</option>
        <option value="2">已删除</option>
        <option value="3">屏蔽</option>
      </select>
      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="搜索帖子标题或内容"
        @keyup.enter="loadPosts"
      />
      <button @click="loadPosts">搜索</button>
      <button class="btn-reset" @click="resetSearch">重置</button>
    </div>
    
    <!-- 帖子列表 -->
    <div class="post-list">
      <!-- PC端表格 -->
      <table class="data-table desktop-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>标题</th>
            <th>作者</th>
            <th>状态</th>
            <th>置顶</th>
            <th>热门</th>
            <th>点赞数</th>
            <th>评论数</th>
            <th>发布时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="post in postList" :key="post.id">
            <td>{{ post.id }}</td>
            <td class="title-cell">{{ post.title || '(无标题)' }}</td>
            <td>{{ post.authorName || '未知' }}</td>
            <td>
              <span :class="['status-badge', getStatusClass(post.status)]">
                {{ getStatusText(post.status) }}
              </span>
            </td>
            <td>{{ post.isPinned ? '是' : '否' }}</td>
            <td>{{ post.isHot ? '是' : '否' }}</td>
            <td>{{ post.likeCount }}</td>
            <td>{{ post.commentCount }}</td>
            <td>{{ formatTime(post.createTime) }}</td>
            <td class="action-cell">
              <button 
                v-if="post.status !== 3" 
                @click="handleBan(post.id)" 
                class="btn btn-danger"
              >
                屏蔽
              </button>
              <button 
                v-else 
                @click="handleUnban(post.id)" 
                class="btn btn-success"
              >
                解除屏蔽
              </button>
              <button 
                v-if="!post.isPinned" 
                @click="handlePin(post.id)" 
                class="btn btn-primary"
              >
                置顶
              </button>
              <button 
                v-else 
                @click="handleUnpin(post.id)" 
                class="btn btn-secondary"
              >
                取消置顶
              </button>
              <button 
                v-if="!post.isHot" 
                @click="handleSetHot(post.id)" 
                class="btn btn-warning"
              >
                设为热门
              </button>
              <button 
                v-else 
                @click="handleRemoveHot(post.id)" 
                class="btn btn-secondary"
              >
                取消热门
              </button>
              <button 
                @click="handleDelete(post.id)" 
                class="btn btn-danger"
              >
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 移动端卡片列表 -->
      <div class="mobile-card-list">
        <div v-for="post in postList" :key="post.id" class="mobile-card">
          <div class="card-header">
            <div class="card-title">
              <span class="card-id">#{{ post.id }}</span>
              <span class="card-title-text">{{ post.title || '(无标题)' }}</span>
            </div>
            <span :class="['status-badge', getStatusClass(post.status)]">
              {{ getStatusText(post.status) }}
            </span>
          </div>
          <div class="card-body">
            <div class="card-row">
              <span class="card-label">作者：</span>
              <span class="card-value">{{ post.authorName || '未知' }}</span>
            </div>
            <div class="card-row">
              <span class="card-label">发布时间：</span>
              <span class="card-value">{{ formatTime(post.createTime) }}</span>
            </div>
            <div class="card-stats">
              <div class="stat-item">
                <span class="stat-label">点赞</span>
                <span class="stat-value">{{ post.likeCount }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">评论</span>
                <span class="stat-value">{{ post.commentCount }}</span>
              </div>
              <div class="stat-item" v-if="post.isPinned">
                <span class="stat-badge pinned">置顶</span>
              </div>
              <div class="stat-item" v-if="post.isHot">
                <span class="stat-badge hot">热门</span>
              </div>
            </div>
          </div>
          <div class="card-actions">
            <button 
              v-if="post.status !== 3" 
              @click="handleBan(post.id)" 
              class="btn btn-danger btn-sm"
            >
              屏蔽
            </button>
            <button 
              v-else 
              @click="handleUnban(post.id)" 
              class="btn btn-success btn-sm"
            >
              解除屏蔽
            </button>
            <button 
              v-if="!post.isPinned" 
              @click="handlePin(post.id)" 
              class="btn btn-primary btn-sm"
            >
              置顶
            </button>
            <button 
              v-else 
              @click="handleUnpin(post.id)" 
              class="btn btn-secondary btn-sm"
            >
              取消置顶
            </button>
            <button 
              v-if="!post.isHot" 
              @click="handleSetHot(post.id)" 
              class="btn btn-warning btn-sm"
            >
              热门
            </button>
            <button 
              v-else 
              @click="handleRemoveHot(post.id)" 
              class="btn btn-secondary btn-sm"
            >
              取消热门
            </button>
            <button 
              @click="handleDelete(post.id)" 
              class="btn btn-danger btn-sm"
            >
              删除
            </button>
          </div>
        </div>
      </div>
      
      <!-- 分页 -->
      <div class="pagination">
        <button @click="prevPage" :disabled="page === 1">上一页</button>
        <span>第 {{ page }} 页，共 {{ totalPages }} 页</span>
        <button @click="nextPage" :disabled="page >= totalPages">下一页</button>
      </div>
    </div>
    
    <!-- 操作确认对话框 -->
    <div v-if="showConfirmDialog" class="dialog-overlay" @click="showConfirmDialog = false">
      <div class="dialog" @click.stop>
        <h3>{{ confirmActionText }}</h3>
        <textarea v-model="operationReason" placeholder="请输入操作原因（可选）"></textarea>
        <div class="dialog-buttons">
          <button @click="confirmOperation" class="btn btn-primary">确认</button>
          <button @click="showConfirmDialog = false" class="btn btn-secondary">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { 
  getAdminPostList, 
  deletePost, 
  banPost, 
  unbanPost, 
  pinPost, 
  unpinPost, 
  setHotPost, 
  removeHotPost,
  type AdminPost 
} from '../../api/admin'
import { notify } from '@/utils/notify'

const postList = ref<AdminPost[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterStatus = ref('')
const searchKeyword = ref('')

const totalPages = computed(() => Math.ceil(total.value / size.value))

const showConfirmDialog = ref(false)
const confirmActionText = ref('')
const operationReason = ref('')
const pendingAction = ref<{ type: string; postId: number } | null>(null)

const loadPosts = async () => {
  try {
    const params: any = {
      page: page.value,
      size: size.value
    }
    if (filterStatus.value) {
      params.status = parseInt(filterStatus.value)
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    
    const result = await getAdminPostList(params)
    postList.value = result.list
    total.value = result.total
  } catch (error) {
    console.error('加载帖子列表失败:', error)
    notify('加载失败')
  }
}

const resetSearch = () => {
  filterStatus.value = ''
  searchKeyword.value = ''
  page.value = 1
  void loadPosts()
}

const handleBan = (postId: number) => {
  pendingAction.value = { type: 'ban', postId }
  confirmActionText.value = '屏蔽帖子'
  showConfirmDialog.value = true
}

const handleUnban = (postId: number) => {
  pendingAction.value = { type: 'unban', postId }
  confirmActionText.value = '解除屏蔽'
  showConfirmDialog.value = true
}

const handlePin = (postId: number) => {
  pendingAction.value = { type: 'pin', postId }
  confirmActionText.value = '置顶帖子'
  showConfirmDialog.value = true
}

const handleUnpin = (postId: number) => {
  pendingAction.value = { type: 'unpin', postId }
  confirmActionText.value = '取消置顶'
  showConfirmDialog.value = true
}

const handleSetHot = (postId: number) => {
  pendingAction.value = { type: 'setHot', postId }
  confirmActionText.value = '设为热门'
  showConfirmDialog.value = true
}

const handleRemoveHot = (postId: number) => {
  pendingAction.value = { type: 'removeHot', postId }
  confirmActionText.value = '取消热门'
  showConfirmDialog.value = true
}

const handleDelete = (postId: number) => {
  pendingAction.value = { type: 'delete', postId }
  confirmActionText.value = '删除帖子'
  showConfirmDialog.value = true
}

const confirmOperation = async () => {
  if (!pendingAction.value) return
  
  try {
    const { type, postId } = pendingAction.value
    const reason = operationReason.value || ''
    
    switch (type) {
      case 'ban':
        await banPost(postId, reason)
        break
      case 'unban':
        await unbanPost(postId, reason)
        break
      case 'pin':
        await pinPost(postId, reason)
        break
      case 'unpin':
        await unpinPost(postId, reason)
        break
      case 'setHot':
        await setHotPost(postId, reason)
        break
      case 'removeHot':
        await removeHotPost(postId, reason)
        break
      case 'delete':
        await deletePost(postId, reason)
        break
    }
    
    notify('操作成功')
    showConfirmDialog.value = false
    operationReason.value = ''
    pendingAction.value = null
    loadPosts()
  } catch (error) {
    console.error('操作失败:', error)
    notify('操作失败')
  }
}

const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '正常',
    1: '仅自己可见',
    2: '已删除',
    3: '屏蔽'
  }
  return statusMap[status] || '未知'
}

const getStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    0: 'status-normal',
    1: 'status-private',
    2: 'status-deleted',
    3: 'status-banned'
  }
  return classMap[status] || ''
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

const prevPage = () => {
  if (page.value > 1) {
    page.value--
    loadPosts()
  }
}

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    loadPosts()
  }
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
.admin-post-manage {
  width: 100%;
}

.filter-section {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  padding: 16px;
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.filter-section select,
.filter-section input {
  padding: 8px 12px;
  border: 1px solid #d3ddec;
  border-radius: 8px;
  flex: 1;
  min-width: 120px;
  min-height: 38px;
}

.filter-section button {
  padding: 8px 16px;
  background: #1677ff;
  color: white;
  border: 1px solid #1677ff;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.filter-section .btn-reset {
  background: #f8fafc;
  color: #475569;
  border-color: #d3ddec;
}

.post-list {
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  display: table;
}

/* 移动端卡片式布局 */
.mobile-card-list {
  display: none;
}

.mobile-card {
  background: white;
  border: 1px solid #e3ebf5;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.card-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-id {
  color: #64748b;
  font-size: 12px;
}

.card-title-text {
  font-weight: 600;
  color: #1f2937;
  flex: 1;
  word-break: break-word;
}

.card-body {
  margin-bottom: 12px;
}

.card-row {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.card-label {
  color: #64748b;
  min-width: 70px;
}

.card-value {
  color: #1f2937;
  flex: 1;
}

.card-stats {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.stat-badge {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.stat-badge.pinned {
  background: #fff3cd;
  color: #856404;
}

.stat-badge.hot {
  background: #f8d7da;
  color: #721c24;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 12px;
  border-top: 1px solid #edf2f7;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .desktop-table {
    display: none;
  }

  .mobile-card-list {
    display: block;
  }

  .filter-section {
    flex-direction: column;
  }

  .filter-section select,
  .filter-section input {
    width: 100%;
  }

  .filter-section button {
    width: 100%;
  }
}

@media (min-width: 769px) {
  .mobile-card-list {
    display: none;
  }
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #edf2f7;
}

.data-table th {
  background: #f8fbff;
  font-weight: 600;
  color: #334155;
}

.title-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
}

.status-normal {
  background: #d4edda;
  color: #155724;
}

.status-private {
  background: #fff3cd;
  color: #856404;
}

.status-deleted {
  background: #f8d7da;
  color: #721c24;
}

.status-banned {
  background: #f8d7da;
  color: #721c24;
}

.action-cell {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.btn {
  padding: 4px 8px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
}

.btn-primary {
  background: #1677ff;
  color: white;
}

.btn-danger {
  background: #ef4444;
  border-color: #ef4444;
  color: white;
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

.btn-secondary {
  background: #f8fafc;
  border-color: #d3ddec;
  color: #475569;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  padding: 14px 16px;
  background: #f8fbff;
  border-top: 1px solid #e3ebf5;
}

.pagination button {
  padding: 8px 16px;
  border: 1px solid #d3ddec;
  border-radius: 8px;
  background: white;
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
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e3ebf5;
  min-width: 400px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.16);
}

.dialog h3 {
  margin-bottom: 15px;
}

.dialog textarea {
  width: 100%;
  min-height: 100px;
  padding: 8px;
  border: 1px solid #d3ddec;
  border-radius: 8px;
  margin-bottom: 15px;
}

.dialog-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
