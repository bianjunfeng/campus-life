<template>
  <div class="admin-merchant-auth">
    <div class="filter-section">
      <select v-model="filterStatus">
        <option value="all">全部状态</option>
        <option value="pending">待审核</option>
        <option value="approved">已通过</option>
        <option value="rejected">已拒绝</option>
      </select>
      <input type="text" v-model="searchKeyword" placeholder="搜索商家名称或联系人">
    </div>
    <table class="auth-table">
      <thead>
        <tr>
          <th>商家名称</th>
          <th>联系人</th>
          <th>联系电话</th>
          <th>提交时间</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="auth in filteredAuthList" :key="auth.id">
          <td>{{ auth.merchantName }}</td>
          <td>{{ auth.contactPerson }}</td>
          <td>{{ auth.contactPhone }}</td>
          <td>{{ auth.submitTime }}</td>
          <td>
            <span :class="['status-badge', auth.status]">{{ getStatusText(auth.status) }}</span>
          </td>
          <td>
            <button v-if="auth.status === 'pending'" @click="approveAuth(auth.id)" class="btn approve">通过</button>
            <button v-if="auth.status === 'pending'" @click="rejectAuth(auth.id)" class="btn reject">拒绝</button>
            <button @click="viewDetail(auth.id)" class="btn view">查看详情</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { approveMerchant, getAdminMerchantList, rejectMerchant, type AdminMerchant } from '../../api/admin'
import { showAlert } from '../../utils/dialog'

interface MerchantAuth {
  id: number
  merchantName: string
  contactPerson: string
  contactPhone: string
  submitTime: string
  status: 'pending' | 'approved' | 'rejected'
}

const filterStatus = ref('all')
const searchKeyword = ref('')
const merchantAuthList = ref<MerchantAuth[]>([])

const mapStatus = (status?: number): MerchantAuth['status'] => {
  if (status === 0) return 'pending'
  if (status === 1) return 'approved'
  return 'rejected'
}

const loadMerchantAuthList = async () => {
  try {
    const result = await getAdminMerchantList({
      page: 1,
      size: 200
    })
    const list = (result?.list || []) as AdminMerchant[]
    merchantAuthList.value = list.map(item => ({
      id: item.id,
      merchantName: item.name || `商家${item.id}`,
      contactPerson: item.contactName || '-',
      contactPhone: item.contactPhone || '-',
      submitTime: item.createTime || '',
      status: mapStatus(item.status)
    }))
  } catch (error: any) {
    merchantAuthList.value = []
    await showAlert(error?.message || '加载商家审核列表失败')
  }
}

const filteredAuthList = computed(() => {
  return merchantAuthList.value.filter(auth => {
    const matchesStatus = filterStatus.value === 'all' || auth.status === filterStatus.value
    const matchesKeyword = auth.merchantName.includes(searchKeyword.value) || auth.contactPerson.includes(searchKeyword.value)
    return matchesStatus && matchesKeyword
  })
})

const getStatusText = (status: string) => {
  switch(status) {
    case 'pending': return '待审核'
    case 'approved': return '已通过'
    case 'rejected': return '已拒绝'
    default: return '未知'
  }
}

const approveAuth = async (id: number) => {
  const auth = merchantAuthList.value.find(a => a.id === id)
  if (!auth) return
  try {
    await approveMerchant(id, '后台审核通过')
    auth.status = 'approved'
    await showAlert('已通过商家审核')
  } catch (error: any) {
    await showAlert(error?.message || '审核通过失败')
  }
}

const rejectAuth = async (id: number) => {
  const auth = merchantAuthList.value.find(a => a.id === id)
  if (!auth) return
  try {
    await rejectMerchant(id, '后台审核拒绝')
    auth.status = 'rejected'
    await showAlert('已拒绝商家审核')
  } catch (error: any) {
    await showAlert(error?.message || '审核拒绝失败')
  }
}

const viewDetail = async (id: number) => {
  await showAlert(`商家ID：${id}`)
}

onMounted(() => {
  loadMerchantAuthList()
})
</script>

<style scoped>
.admin-merchant-auth {
  width: 100%;
}

.filter-section {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.filter-section select,
.filter-section input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.auth-table {
  width: 100%;
  border-collapse: collapse;
  background-color: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.auth-table th,
.auth-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.auth-table th {
  background-color: #f5f5f5;
  font-weight: 600;
  color: #555;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.pending {
  background-color: #fff3cd;
  color: #856404;
}

.status-badge.approved {
  background-color: #d4edda;
  color: #155724;
}

.status-badge.rejected {
  background-color: #f8d7da;
  color: #721c24;
}

.btn {
  padding: 6px 12px;
  margin-right: 5px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn.approve {
  background-color: #28a745;
  color: white;
}

.btn.reject {
  background-color: #dc3545;
  color: white;
}

.btn.view {
  background-color: #17a2b8;
  color: white;
}
</style>
