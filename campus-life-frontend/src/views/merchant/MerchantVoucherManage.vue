<template>
  <div class="merchant-voucher-manage">
    <div class="page-header">
      <div class="header-left">
        <h2>商品管理</h2>
      </div>
      <div class="header-actions">
        <button @click="showAddDialog = true" class="btn btn-primary">新增优惠券</button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <select v-model="filterStatus" @change="loadVouchers">
        <option value="">全部状态</option>
        <option value="0">下架</option>
        <option value="1">上架</option>
      </select>
      <button @click="loadVouchers">刷新</button>
    </div>
    
    <!-- 券列表 -->
    <div class="voucher-list">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>标题</th>
            <th>券类型</th>
            <th>副标题</th>
            <th>面值</th>
            <th>支付金额</th>
            <th>库存</th>
            <th>状态</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="voucher in voucherList" :key="voucher.id">
            <td>{{ voucher.id }}</td>
            <td>{{ voucher.title }}</td>
            <td>
              <span :class="['type-badge', voucher.voucherType === 'seckill' ? 'type-seckill' : 'type-group']">
                {{ voucher.voucherType === 'seckill' ? '秒杀券' : '团购券' }}
              </span>
            </td>
            <td>{{ voucher.subTitle || '-' }}</td>
            <td>¥{{ voucher.amount }}</td>
            <td>{{ voucher.payValue ? '¥' + voucher.payValue : '免费' }}</td>
            <td>{{ voucher.voucherType === 'seckill' ? (voucher.seckillStock ?? voucher.stock) : voucher.stock }}</td>
            <td>
              <span :class="['status-badge', getStatusClass(voucher.status)]">
                {{ getStatusText(voucher.status) }}
              </span>
            </td>
            <td>{{ formatTime(voucher.beginTime) }}</td>
            <td>{{ formatTime(voucher.endTime) }}</td>
            <td class="action-cell">
              <button 
                @click="handleEdit(voucher)" 
                class="btn btn-edit"
              >
                编辑
              </button>
              <button 
                v-if="voucher.status === 0" 
                @click="handleOnline(voucher.id)" 
                class="btn btn-success"
              >
                上架
              </button>
              <button 
                v-if="voucher.status === 1" 
                @click="handleOffline(voucher.id)" 
                class="btn btn-danger"
              >
                下架
              </button>
              <button 
                @click="handleDelete(voucher.id)" 
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
        <span>第 {{ page }} 页，共 {{ totalPages }} 页（共 {{ total }} 条）</span>
        <button @click="nextPage" :disabled="page >= totalPages">下一页</button>
      </div>
    </div>
    
    <!-- 新增/编辑对话框 -->
    <div v-if="showAddDialog || showEditDialog" class="dialog-overlay" @click="closeDialog">
      <div class="dialog" @click.stop>
        <h3>{{ showAddDialog ? '新增优惠券' : '编辑优惠券' }}</h3>
        <form @submit.prevent="saveVoucher">
          <div class="form-group">
            <label>券类型 *</label>
            <select v-model="formData.voucherType">
              <option value="group">团购券</option>
              <option value="seckill">秒杀券</option>
            </select>
          </div>
          <div class="form-group">
            <label>标题 *</label>
            <input v-model="formData.title" type="text" required placeholder="请输入优惠券标题" />
          </div>
          <div class="form-group">
            <label>副标题</label>
            <input v-model="formData.subTitle" type="text" placeholder="请输入副标题" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>面值（优惠金额）*</label>
              <input v-model.number="formData.amount" type="number" step="0.01" required placeholder="0.00" />
            </div>
            <div class="form-group">
              <label>支付金额</label>
              <input v-model.number="formData.payValue" type="number" step="0.01" placeholder="0.00（免费可不填）" />
            </div>
          </div>
          <div class="form-group">
            <label>{{ formData.voucherType === 'seckill' ? '秒杀库存 *' : '库存 *' }}</label>
            <input
              v-if="formData.voucherType === 'seckill'"
              v-model.number="formData.seckillStock"
              type="number"
              min="1"
              required
              placeholder="请输入秒杀库存"
            />
            <input
              v-else
              v-model.number="formData.stock"
              type="number"
              min="0"
              required
              placeholder="请输入库存数量"
            />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>{{ formData.voucherType === 'seckill' ? '秒杀开始时间 *' : '开始时间' }}</label>
              <input
                v-if="formData.voucherType === 'seckill'"
                v-model="formData.seckillStartTime"
                type="datetime-local"
                required
              />
              <input v-else v-model="formData.beginTime" type="datetime-local" />
            </div>
            <div class="form-group">
              <label>{{ formData.voucherType === 'seckill' ? '秒杀结束时间 *' : '结束时间' }}</label>
              <input
                v-if="formData.voucherType === 'seckill'"
                v-model="formData.seckillEndTime"
                type="datetime-local"
                required
              />
              <input v-else v-model="formData.endTime" type="datetime-local" required />
            </div>
          </div>
          <div class="dialog-buttons">
            <button type="submit" class="btn btn-primary">保存</button>
            <button type="button" @click="closeDialog" class="btn btn-secondary">取消</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { 
  getMerchantVoucherList,
  createMerchantVoucher,
  updateMerchantVoucher,
  onlineMerchantVoucher,
  offlineMerchantVoucher,
  deleteMerchantVoucher,
  type MerchantVoucher
} from '../../api/merchant'
import { showAlert, showConfirm } from '../../utils/dialog'

const voucherList = ref<MerchantVoucher[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterStatus = ref('')

const showAddDialog = ref(false)
const showEditDialog = ref(false)
const editingVoucher = ref<MerchantVoucher | null>(null)

const formData = ref({
  voucherType: 'group' as 'group' | 'seckill',
  title: '',
  subTitle: '',
  stock: 0,
  seckillStock: 1,
  amount: 0,
  payValue: undefined as number | undefined,
  beginTime: '',
  endTime: '',
  seckillStartTime: '',
  seckillEndTime: ''
})

const totalPages = computed(() => Math.ceil(total.value / size.value))

const loadVouchers = async () => {
  try {
    const params: any = {
      page: page.value,
      size: size.value
    }
    if (filterStatus.value) {
      params.status = parseInt(filterStatus.value)
    }
    
    const result = await getMerchantVoucherList(params)
    voucherList.value = result.list || []
    total.value = result.total
  } catch (error: any) {
    console.error('加载优惠券列表失败:', error)
    await showAlert(error.response?.data?.msg || '加载失败')
  }
}

const handleEdit = (voucher: MerchantVoucher) => {
  editingVoucher.value = voucher
  const voucherType = voucher.voucherType === 'seckill' ? 'seckill' : 'group'
  formData.value = {
    voucherType,
    title: voucher.title,
    subTitle: voucher.subTitle || '',
    stock: voucher.stock,
    seckillStock: voucher.seckillStock || voucher.stock || 1,
    amount: voucher.amount,
    payValue: voucher.payValue,
    beginTime: voucher.beginTime ? formatDateTimeLocal(voucher.beginTime) : '',
    endTime: voucher.endTime ? formatDateTimeLocal(voucher.endTime) : '',
    seckillStartTime: voucher.seckillStartTime
      ? formatDateTimeLocal(voucher.seckillStartTime)
      : (voucher.beginTime ? formatDateTimeLocal(voucher.beginTime) : ''),
    seckillEndTime: voucher.seckillEndTime
      ? formatDateTimeLocal(voucher.seckillEndTime)
      : (voucher.endTime ? formatDateTimeLocal(voucher.endTime) : '')
  }
  showEditDialog.value = true
}

const handleOnline = async (voucherId: number) => {
  if (!(await showConfirm('确定要上架此优惠券吗？'))) return
  
  try {
    await onlineMerchantVoucher(voucherId)
    await showAlert('上架成功')
    loadVouchers()
  } catch (error: any) {
    console.error('上架失败:', error)
    await showAlert(error.response?.data?.msg || '上架失败')
  }
}

const handleOffline = async (voucherId: number) => {
  if (!(await showConfirm('确定要下架此优惠券吗？'))) return
  
  try {
    await offlineMerchantVoucher(voucherId)
    await showAlert('下架成功')
    loadVouchers()
  } catch (error: any) {
    console.error('下架失败:', error)
    await showAlert(error.response?.data?.msg || '下架失败')
  }
}

const handleDelete = async (voucherId: number) => {
  if (!(await showConfirm('确定要删除此优惠券吗？删除后无法恢复。'))) return
  
  try {
    await deleteMerchantVoucher(voucherId)
    await showAlert('删除成功')
    loadVouchers()
  } catch (error: any) {
    console.error('删除失败:', error)
    await showAlert(error.response?.data?.msg || '删除失败')
  }
}

const saveVoucher = async () => {
  try {
    if (!formData.value.title.trim()) {
      await showAlert('请填写标题')
      return
    }
    if (formData.value.amount <= 0) {
      await showAlert('面值必须大于0')
      return
    }
    if (typeof formData.value.payValue === 'number' && formData.value.payValue < 0) {
      await showAlert('支付金额不能小于0')
      return
    }
    if (typeof formData.value.payValue === 'number' && formData.value.payValue > formData.value.amount) {
      await showAlert('支付金额不能大于面值')
      return
    }
    if (formData.value.voucherType === 'seckill') {
      if (!formData.value.seckillStock || formData.value.seckillStock <= 0) {
        await showAlert('秒杀券库存必须大于0')
        return
      }
      if (!formData.value.seckillStartTime || !formData.value.seckillEndTime) {
        await showAlert('秒杀券必须填写开始和结束时间')
        return
      }
      if (new Date(formData.value.seckillStartTime).getTime() >= new Date(formData.value.seckillEndTime).getTime()) {
        await showAlert('秒杀开始时间必须早于结束时间')
        return
      }
    } else if (formData.value.stock < 0) {
      await showAlert('库存不能小于0')
      return
    } else {
      if (!formData.value.endTime) {
        await showAlert('团购券必须填写截止时间')
        return
      }
      if (
        formData.value.beginTime &&
        new Date(formData.value.beginTime).getTime() > new Date(formData.value.endTime).getTime()
      ) {
        await showAlert('开始时间不能晚于截止时间')
        return
      }
    }

    const data: any = {
      voucherType: formData.value.voucherType,
      title: formData.value.title,
      subTitle: formData.value.subTitle || undefined,
      amount: formData.value.amount,
      payValue: formData.value.payValue || undefined,
      stock: formData.value.voucherType === 'seckill' ? formData.value.seckillStock : formData.value.stock,
      seckillStock: formData.value.voucherType === 'seckill' ? formData.value.seckillStock : undefined,
      beginTime: formData.value.voucherType === 'group' ? (formData.value.beginTime || undefined) : undefined,
      endTime: formData.value.voucherType === 'group' ? (formData.value.endTime || undefined) : undefined,
      seckillStartTime: formData.value.voucherType === 'seckill' ? formData.value.seckillStartTime : undefined,
      seckillEndTime: formData.value.voucherType === 'seckill' ? formData.value.seckillEndTime : undefined
    }

    if (showAddDialog.value) {
      await createMerchantVoucher(data)
      await showAlert('创建成功')
    } else if (editingVoucher.value) {
      await updateMerchantVoucher(editingVoucher.value.id, data)
      await showAlert('更新成功')
    }

    closeDialog()
    loadVouchers()
  } catch (error: any) {
    console.error('保存失败:', error)
    await showAlert(error.response?.data?.msg || '保存失败')
  }
}

const closeDialog = () => {
  showAddDialog.value = false
  showEditDialog.value = false
  editingVoucher.value = null
  formData.value = {
    voucherType: 'group',
    title: '',
    subTitle: '',
    stock: 0,
    seckillStock: 1,
    amount: 0,
    payValue: undefined,
    beginTime: '',
    endTime: '',
    seckillStartTime: '',
    seckillEndTime: ''
  }
}

const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '下架',
    1: '上架'
  }
  return statusMap[status] || '未知'
}

const getStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    0: 'status-offline',
    1: 'status-online'
  }
  return classMap[status] || ''
}

const formatTime = (time: string | undefined) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

const formatDateTimeLocal = (time: string) => {
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

const prevPage = () => {
  if (page.value > 1) {
    page.value--
    loadVouchers()
  }
}

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    loadVouchers()
  }
}

onMounted(() => {
  loadVouchers()
})
</script>

<style scoped>
.merchant-voucher-manage {
  width: 100%;
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-button {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 999px;
  background: #eef2ff;
  color: #1e3a8a;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.back-button:hover {
  background: #dbeafe;
}

.back-icon {
  font-size: 24px;
  line-height: 1;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.filter-section {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  padding: 15px;
  background: white;
  border-radius: 8px;
}

.filter-section select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.filter-section button {
  padding: 8px 16px;
  background: #1677ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.filter-section button:hover {
  background: #0958d9;
}

.voucher-list {
  background: white;
  border-radius: 8px;
  padding: 20px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
  font-size: 14px;
}

.data-table th {
  background: #fafafa;
  font-weight: 600;
  color: #333;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.type-badge {
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.type-group {
  color: #1d4ed8;
  background: #dbeafe;
}

.type-seckill {
  color: #b91c1c;
  background: #fee2e2;
}

.status-online {
  background: #d4edda;
  color: #155724;
}

.status-offline {
  background: #f8d7da;
  color: #721c24;
}

.action-cell {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s;
}

.btn-primary {
  background: #1677ff;
  color: white;
}

.btn-primary:hover {
  background: #0958d9;
}

.btn-success {
  background: #52c41a;
  color: white;
}

.btn-success:hover {
  background: #389e0d;
}

.btn-edit {
  background: #1677ff;
  color: white;
}

.btn-edit:hover {
  background: #0958d9;
}

.btn-danger {
  background: #ff4d4f;
  color: white;
}

.btn-danger:hover {
  background: #cf1322;
}

.btn-secondary {
  background: #d9d9d9;
  color: #333;
}

.btn-secondary:hover {
  background: #bfbfbf;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.pagination button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 14px;
}

.pagination button:hover:not(:disabled) {
  border-color: #1677ff;
  color: #1677ff;
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
  padding: 24px;
  border-radius: 8px;
  min-width: 500px;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}

.dialog h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #333;
}

.form-group {
  margin-bottom: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  background: #fff;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #1677ff;
}

.dialog-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .merchant-voucher-manage {
    padding: 10px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .header-left {
    width: 100%;
  }

  .dialog {
    min-width: 90%;
    max-width: 90%;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .data-table {
    font-size: 12px;
  }

  .data-table th,
  .data-table td {
    padding: 8px;
  }
}
</style>
