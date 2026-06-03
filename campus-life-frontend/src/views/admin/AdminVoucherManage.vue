<template>
  <div class="admin-voucher-manage">
    <div class="filter-section">
      <select v-model="filterStatus" @change="loadVouchers">
        <option value="">全部状态</option>
        <option value="0">下架</option>
        <option value="1">上架</option>
      </select>
      <input
        type="text"
        v-model.trim="searchKeyword"
        placeholder="券ID/标题"
        @keyup.enter="loadVouchers"
      />
      <input
        type="number"
        v-model.number="filterMerchantId"
        placeholder="商家ID"
        @keyup.enter="loadVouchers"
      />
      <button @click="loadVouchers">搜索</button>
      <button class="btn-reset" @click="resetSearch">重置</button>
    </div>
    <div class="health-panel">
      <div class="health-toolbar">
        <button class="btn btn-secondary" :disabled="healthLoading" @click="loadSeckillHealth">
          {{ healthLoading ? '刷新中...' : '刷新秒杀健康状态' }}
        </button>
        <label class="auto-refresh">
          <input v-model="autoRefresh" type="checkbox" />
          自动刷新(10s)
        </label>
        <span class="health-meta">状态: {{ healthData?.overallStatus || '-' }}</span>
        <span class="health-meta">更新时间: {{ formatTime(healthData?.checkedAt) || '-' }}</span>
      </div>
      <div v-if="healthData" class="health-grid">
        <div>Redis: {{ healthData.redis?.status || '-' }}</div>
        <div>RabbitMQ: {{ healthData.rabbitmq?.status || '-' }}</div>
        <div>Sentinel规则数: {{ healthData.sentinel?.flowRuleCount ?? '-' }}</div>
        <div>秒杀队列消息: {{ healthData.rabbitmq?.seckillQueue?.messageCount ?? '-' }}</div>
        <div>秒杀消费者数: {{ healthData.rabbitmq?.seckillQueue?.consumerCount ?? '-' }}</div>
        <div>重试队列消息: {{ healthData.rabbitmq?.seckillRetryQueue?.messageCount ?? '-' }}</div>
        <div>死信队列消息: {{ healthData.rabbitmq?.seckillDeadQueue?.messageCount ?? '-' }}</div>
        <div>Canal队列消息: {{ healthData.rabbitmq?.canalQueue?.messageCount ?? '-' }}</div>
        <div>秒杀请求数: {{ healthData.metrics?.submitRequestCount ?? 0 }}</div>
        <div>通过预检数: {{ healthData.metrics?.acceptedCount ?? 0 }}</div>
        <div>消费成功率: {{ healthData.metrics?.processSuccessRate ?? 0 }}%</div>
        <div>预检通过率: {{ healthData.metrics?.acceptRate ?? 0 }}%</div>
        <div>平均处理耗时: {{ healthData.metrics?.avgProcessLatencyMs ?? 0 }}ms</div>
        <div>平均排队延迟: {{ healthData.metrics?.avgQueueDelayMs ?? 0 }}ms</div>
      </div>
      <div v-if="healthData?.alerts?.length" class="alert-list">
        <div
          v-for="(item, idx) in healthData.alerts"
          :key="idx"
          :class="['alert-item', item.level === 'CRITICAL' ? 'critical' : 'warn']"
        >
          {{ item.level }} - {{ item.message }}
        </div>
      </div>
      <div v-if="healthData?.metrics?.topFailReasons?.length" class="reason-list">
        <div class="reason-title">失败原因 Top</div>
        <div v-for="(reason, idx) in healthData.metrics.topFailReasons" :key="idx" class="reason-row">
          <span>{{ reason.reason }}</span>
          <span>{{ reason.count }}</span>
        </div>
      </div>
    </div>

    <div class="voucher-list">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>标题</th>
            <th>商家ID</th>
            <th>面值</th>
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
            <td>{{ voucher.merchantId }}</td>
            <td>{{ voucher.amount }}</td>
            <td>{{ voucher.stock }}</td>
            <td>
              <span :class="['status-badge', getStatusClass(voucher.status)]">
                {{ getStatusText(voucher.status) }}
              </span>
            </td>
            <td>{{ formatTime(voucher.beginTime) }}</td>
            <td>{{ formatTime(voucher.endTime) }}</td>
            <td class="action-cell">
              <button
                v-if="voucher.status !== 1"
                @click="handleOnline(voucher.id)"
                class="btn btn-primary"
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
                v-if="voucher.status === 1"
                @click="handleViolationOffline(voucher.id)"
                class="btn btn-warning"
              >
                违规下架
              </button>
              <button @click="handlePreheat(voucher.id)" class="btn btn-secondary">预热</button>
              <button @click="handleMonitor(voucher.id)" class="btn btn-secondary">监控</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button @click="prevPage" :disabled="page === 1">上一页</button>
        <span>第 {{ page }} 页，共 {{ totalPages }} 页</span>
        <button @click="nextPage" :disabled="page >= totalPages">下一页</button>
      </div>
    </div>

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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import {
  getAdminVoucherList,
  monitorVoucher,
  seckillHealth,
  offlineVoucher,
  onlineVoucher,
  preheatVoucher,
  violationOfflineVoucher,
  type AdminVoucher
} from '../../api/admin'
import { notify } from '@/utils/notify'

const voucherList = ref<AdminVoucher[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterStatus = ref('')
const searchKeyword = ref('')
const filterMerchantId = ref<number | null>(null)

const totalPages = computed(() => Math.ceil(total.value / size.value))

const showConfirmDialog = ref(false)
const confirmActionText = ref('')
const operationReason = ref('')
const pendingAction = ref<{ type: string; voucherId: number } | null>(null)
const healthData = ref<any>(null)
const healthLoading = ref(false)
const autoRefresh = ref(false)
let healthTimer: number | null = null

const loadVouchers = async () => {
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
    if (filterMerchantId.value) {
      params.merchantId = filterMerchantId.value
    }

    const result = await getAdminVoucherList(params)
    voucherList.value = result.list
    total.value = result.total
  } catch (error) {
    console.error('加载券列表失败:', error)
    notify('加载失败')
  }
}

const resetSearch = () => {
  filterStatus.value = ''
  searchKeyword.value = ''
  filterMerchantId.value = null
  page.value = 1
  void loadVouchers()
}

const handleOffline = (voucherId: number) => {
  pendingAction.value = { type: 'offline', voucherId }
  confirmActionText.value = '下架券'
  showConfirmDialog.value = true
}

const handleViolationOffline = (voucherId: number) => {
  pendingAction.value = { type: 'violationOffline', voucherId }
  confirmActionText.value = '违规下架券'
  showConfirmDialog.value = true
}

const handleOnline = async (voucherId: number) => {
  try {
    await onlineVoucher(voucherId)
    notify('上架成功')
    loadVouchers()
  } catch (error) {
    console.error('上架失败:', error)
    notify('上架失败')
  }
}

const handlePreheat = async (voucherId: number) => {
  try {
    await preheatVoucher(voucherId)
    notify('预热完成')
  } catch (error) {
    console.error('预热失败:', error)
    notify('预热失败')
  }
}

const handleMonitor = async (voucherId: number) => {
  try {
    const monitor = await monitorVoucher(voucherId)
    notify(
      `券ID: ${monitor.voucherId}\nRedis库存: ${monitor.redisStock}\nDB库存: ${monitor.dbVoucherStock}\n秒杀库存: ${monitor.dbSeckillStock}\n预约人数: ${monitor.reservedUserCount}`
    )
  } catch (error) {
    console.error('监控查询失败:', error)
    notify('监控查询失败')
  }
}

const loadSeckillHealth = async () => {
  healthLoading.value = true
  try {
    healthData.value = await seckillHealth()
  } catch (error) {
    console.error('秒杀健康状态加载失败:', error)
  } finally {
    healthLoading.value = false
  }
}

const setupHealthTimer = () => {
  if (healthTimer) {
    window.clearInterval(healthTimer)
    healthTimer = null
  }
  if (!autoRefresh.value) return
  healthTimer = window.setInterval(() => {
    void loadSeckillHealth()
  }, 10000)
}

const confirmOperation = async () => {
  if (!pendingAction.value) return

  try {
    const { type, voucherId } = pendingAction.value
    const reason = operationReason.value || ''

    switch (type) {
      case 'offline':
        await offlineVoucher(voucherId, reason)
        break
      case 'violationOffline':
        await violationOfflineVoucher(voucherId, reason)
        break
    }

    notify('操作成功')
    showConfirmDialog.value = false
    operationReason.value = ''
    pendingAction.value = null
    loadVouchers()
  } catch (error) {
    console.error('操作失败:', error)
    notify('操作失败')
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

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
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
  void loadSeckillHealth()
})

watch(autoRefresh, () => {
  setupHealthTimer()
})

onUnmounted(() => {
  if (healthTimer) {
    window.clearInterval(healthTimer)
    healthTimer = null
  }
})
</script>

<style scoped>
.admin-voucher-manage {
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

.health-panel {
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.health-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.health-meta {
  font-size: 12px;
  color: #64748b;
}

.auto-refresh {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #475569;
}

.health-grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  font-size: 13px;
  color: #334155;
}

.alert-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.alert-item {
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 13px;
}

.alert-item.warn {
  background: #fff7ed;
  color: #b45309;
  border: 1px solid #fed7aa;
}

.alert-item.critical {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.reason-list {
  margin-top: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  padding: 8px 10px;
}

.reason-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
}

.reason-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #475569;
  padding: 2px 0;
}

.voucher-list {
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

.status-badge {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
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
  gap: 8px;
  flex-wrap: wrap;
}

.btn {
  padding: 4px 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  color: #fff;
  font-weight: 600;
}

.btn-primary {
  background: #1677ff;
}

.btn-danger {
  background: #ef4444;
  border-color: #ef4444;
}

.btn-warning {
  background: #f59e0b;
  border-color: #f59e0b;
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
  gap: 12px;
  padding: 14px 16px;
  background: #f8fbff;
  border-top: 1px solid #e3ebf5;
}

.pagination button {
  padding: 8px 16px;
  border: 1px solid #d3ddec;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog {
  width: 420px;
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e3ebf5;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.16);
}

.dialog textarea {
  width: 100%;
  min-height: 100px;
  margin-top: 12px;
  padding: 8px;
  border: 1px solid #d3ddec;
  border-radius: 8px;
}

.dialog-buttons {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 768px) {
  .filter-section > * {
    width: 100%;
  }

  .health-grid {
    grid-template-columns: 1fr;
  }
}
</style>
