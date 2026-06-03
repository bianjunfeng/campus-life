<template>
  <div class="admin-comment-manage">
    <!-- 筛选条件 -->
    <div class="filter-section">
      <select v-model="filterStatus" @change="loadComments">
        <option value="">全部状态</option>
        <option value="0">正常</option>
        <option value="1">已删除</option>
        <option value="2">屏蔽</option>
      </select>
      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="搜索评论内容"
        @keyup.enter="loadComments"
      />
      <input 
        type="number" 
        v-model.number="filterPostId" 
        placeholder="帖子ID"
        @keyup.enter="loadComments"
      />
      <button @click="loadComments">搜索</button>
      <button class="btn-reset" @click="resetSearch">重置</button>
    </div>
    
    <!-- 评论列表 -->
    <div class="comment-list">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>内容</th>
            <th>用户</th>
            <th>帖子ID</th>
            <th>状态</th>
            <th>点赞数</th>
            <th>发布时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="comment in commentList" :key="comment.id">
            <td>{{ comment.id }}</td>
            <td class="content-cell">{{ comment.content }}</td>
            <td>{{ comment.userName || '未知' }}</td>
            <td>{{ comment.postId }}</td>
            <td>
              <span :class="['status-badge', getStatusClass(comment.status)]">
                {{ getStatusText(comment.status) }}
              </span>
            </td>
            <td>{{ comment.likeCount }}</td>
            <td>{{ formatTime(comment.createTime) }}</td>
            <td class="action-cell">
              <button 
                v-if="comment.status !== 2" 
                @click="handleBan(comment.id)" 
                class="btn btn-danger"
              >
                屏蔽
              </button>
              <button 
                v-else 
                @click="handleUnban(comment.id)" 
                class="btn btn-success"
              >
                解除屏蔽
              </button>
              <button 
                @click="handleDelete(comment.id)" 
                class="btn btn-danger"
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
  getAdminCommentList, 
  deleteComment, 
  banComment, 
  unbanComment,
  type AdminComment 
} from '../../api/admin'
import { notify } from '@/utils/notify'

const commentList = ref<AdminComment[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterStatus = ref('')
const searchKeyword = ref('')
const filterPostId = ref<number | null>(null)

const totalPages = computed(() => Math.ceil(total.value / size.value))

const showConfirmDialog = ref(false)
const confirmActionText = ref('')
const operationReason = ref('')
const pendingAction = ref<{ type: string; commentId: number } | null>(null)

const loadComments = async () => {
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
    if (filterPostId.value) {
      params.postId = filterPostId.value
    }
    
    const result = await getAdminCommentList(params)
    commentList.value = result.list
    total.value = result.total
  } catch (error) {
    console.error('加载评论列表失败:', error)
    notify('加载失败')
  }
}

const resetSearch = () => {
  filterStatus.value = ''
  searchKeyword.value = ''
  filterPostId.value = null
  page.value = 1
  void loadComments()
}

const handleBan = (commentId: number) => {
  pendingAction.value = { type: 'ban', commentId }
  confirmActionText.value = '屏蔽评论'
  showConfirmDialog.value = true
}

const handleUnban = (commentId: number) => {
  pendingAction.value = { type: 'unban', commentId }
  confirmActionText.value = '解除屏蔽'
  showConfirmDialog.value = true
}

const handleDelete = (commentId: number) => {
  pendingAction.value = { type: 'delete', commentId }
  confirmActionText.value = '删除评论'
  showConfirmDialog.value = true
}

const confirmOperation = async () => {
  if (!pendingAction.value) return
  
  try {
    const { type, commentId } = pendingAction.value
    const reason = operationReason.value || ''
    
    switch (type) {
      case 'ban':
        await banComment(commentId, reason)
        break
      case 'unban':
        await unbanComment(commentId, reason)
        break
      case 'delete':
        await deleteComment(commentId, reason)
        break
    }
    
    notify('操作成功')
    showConfirmDialog.value = false
    operationReason.value = ''
    pendingAction.value = null
    loadComments()
  } catch (error) {
    console.error('操作失败:', error)
    notify('操作失败')
  }
}

const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '正常',
    1: '已删除',
    2: '屏蔽'
  }
  return statusMap[status] || '未知'
}

const getStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    0: 'status-normal',
    1: 'status-deleted',
    2: 'status-banned'
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
    loadComments()
  }
}

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    loadComments()
  }
}

onMounted(() => {
  loadComments()
})
</script>

<style scoped>
.admin-comment-manage {
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
}

.filter-section .btn-reset {
  background: #f8fafc;
  color: #475569;
  border-color: #d3ddec;
}

.comment-list {
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
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

.content-cell {
  max-width: 300px;
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

@media (max-width: 768px) {
  .filter-section > * {
    width: 100%;
  }
}
</style>
