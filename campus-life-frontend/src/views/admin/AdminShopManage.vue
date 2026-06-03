<template>
  <div class="admin-shop-manage">
    <!-- 筛选条件 -->
    <div class="filter-section">
      <select v-model="filterStatus" @change="loadMerchants">
        <option value="">全部状态</option>
        <option value="0">待审核</option>
        <option value="1">正常</option>
        <option value="2">冻结</option>
        <option value="3">关闭</option>
      </select>
      <input 
        type="text" 
        v-model="searchKeyword" 
        placeholder="搜索商家名称或联系人"
        @keyup.enter="loadMerchants"
      />
      <button @click="loadMerchants">搜索</button>
      <button class="btn-reset" @click="resetSearch">重置</button>
    </div>
    
    <!-- 商家列表 -->
    <div class="merchant-list">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>商家名称</th>
            <th>联系人</th>
            <th>联系电话</th>
            <th>地址</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="merchant in merchantList" :key="merchant.id">
            <td>{{ merchant.id }}</td>
            <td>{{ merchant.name }}</td>
            <td>{{ merchant.contactName || '-' }}</td>
            <td>{{ merchant.contactPhone || '-' }}</td>
            <td class="address-cell">{{ merchant.address || '-' }}</td>
            <td>
              <span :class="['status-badge', getStatusClass(merchant.status)]">
                {{ getStatusText(merchant.status) }}
              </span>
            </td>
            <td>{{ formatTime(merchant.createTime) }}</td>
            <td class="action-cell">
              <template v-if="merchant.status === 0">
                <button @click="handleApprove(merchant.id)" class="btn btn-success">通过</button>
                <button @click="handleReject(merchant.id)" class="btn btn-danger">拒绝</button>
              </template>
              <template v-else-if="merchant.status === 1">
                <button @click="handleFreeze(merchant.id)" class="btn btn-warning">冻结</button>
              </template>
              <template v-else-if="merchant.status === 2">
                <button @click="handleUnfreeze(merchant.id)" class="btn btn-success">解冻</button>
              </template>
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
  getAdminMerchantList, 
  approveMerchant, 
  rejectMerchant, 
  freezeMerchant, 
  unfreezeMerchant,
  type AdminMerchant 
} from '../../api/admin'
import { notify } from '@/utils/notify'

const merchantList = ref<AdminMerchant[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterStatus = ref('')
const searchKeyword = ref('')

const totalPages = computed(() => Math.ceil(total.value / size.value))

const showConfirmDialog = ref(false)
const confirmActionText = ref('')
const operationReason = ref('')
const pendingAction = ref<{ type: string; merchantId: number } | null>(null)

const loadMerchants = async () => {
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
    
    const result = await getAdminMerchantList(params)
    merchantList.value = result.list
    total.value = result.total
  } catch (error) {
    console.error('加载商家列表失败:', error)
    notify('加载失败')
  }
}

const resetSearch = () => {
  filterStatus.value = ''
  searchKeyword.value = ''
  page.value = 1
  void loadMerchants()
}

const handleApprove = (merchantId: number) => {
  pendingAction.value = { type: 'approve', merchantId }
  confirmActionText.value = '审核通过商家'
  showConfirmDialog.value = true
}

const handleReject = (merchantId: number) => {
  pendingAction.value = { type: 'reject', merchantId }
  confirmActionText.value = '审核拒绝商家'
  showConfirmDialog.value = true
}

const handleFreeze = (merchantId: number) => {
  pendingAction.value = { type: 'freeze', merchantId }
  confirmActionText.value = '冻结商家'
  showConfirmDialog.value = true
}

const handleUnfreeze = (merchantId: number) => {
  pendingAction.value = { type: 'unfreeze', merchantId }
  confirmActionText.value = '解冻商家'
  showConfirmDialog.value = true
}

const confirmOperation = async () => {
  if (!pendingAction.value) return
  
  try {
    const { type, merchantId } = pendingAction.value
    const reason = operationReason.value || ''
    
    switch (type) {
      case 'approve':
        await approveMerchant(merchantId, reason)
        break
      case 'reject':
        await rejectMerchant(merchantId, reason)
        break
      case 'freeze':
        await freezeMerchant(merchantId, reason)
        break
      case 'unfreeze':
        await unfreezeMerchant(merchantId, reason)
        break
    }
    
    notify('操作成功')
    showConfirmDialog.value = false
    operationReason.value = ''
    pendingAction.value = null
    loadMerchants()
  } catch (error) {
    console.error('操作失败:', error)
    notify('操作失败')
  }
}

const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '待审核',
    1: '正常',
    2: '冻结',
    3: '关闭'
  }
  return statusMap[status] || '未知'
}

const getStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    0: 'status-pending',
    1: 'status-normal',
    2: 'status-frozen',
    3: 'status-closed'
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
    loadMerchants()
  }
}

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    loadMerchants()
  }
}

onMounted(() => {
  loadMerchants()
})
</script>

<style scoped>
.admin-shop-manage {
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

.merchant-list {
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

.address-cell {
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

.status-pending {
  background: #fff3cd;
  color: #856404;
}

.status-normal {
  background: #d4edda;
  color: #155724;
}

.status-frozen {
  background: #f8d7da;
  color: #721c24;
}

.status-closed {
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

@media (max-width: 768px) {
  .filter-section > * {
    width: 100%;
  }
}
</style>
