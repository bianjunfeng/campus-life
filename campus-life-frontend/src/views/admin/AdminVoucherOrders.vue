<template>
  <div class="admin-voucher-orders">
    <section class="filter-section">
      <input v-model.trim="filters.keyword" type="text" placeholder="订单号/用户名/券标题" @keyup.enter="handleQuery" />
      <input v-model.number="filters.userId" type="number" placeholder="用户ID" @keyup.enter="handleQuery" />
      <input v-model.number="filters.voucherId" type="number" placeholder="券ID" @keyup.enter="handleQuery" />
      <select v-model="filters.status">
        <option value="">订单状态</option>
        <option value="0">待支付</option>
        <option value="1">待使用</option>
        <option value="2">已完成</option>
        <option value="3">已过期</option>
        <option value="4">已退款</option>
        <option value="5">已取消</option>
      </select>
      <select v-model="filters.paymentStatus">
        <option value="">支付状态</option>
        <option value="0">待支付</option>
        <option value="1">支付成功</option>
        <option value="2">支付失败</option>
        <option value="3">已退款</option>
      </select>
      <select v-model="filters.orderSource">
        <option value="">订单来源</option>
        <option value="app">普通券</option>
        <option value="seckill">秒杀券</option>
      </select>
      <select v-model="filters.timeRange">
        <option value="today">今日</option>
        <option value="week">本周</option>
        <option value="month">本月</option>
        <option value="quarter">本季度</option>
        <option value="year">本年</option>
      </select>
      <button @click="handleQuery">查询</button>
      <button class="btn-reset" @click="handleReset">重置</button>
    </section>

    <section class="stats-section">
      <div class="stat-card">
        <h3>订单总数</h3>
        <p class="stat-value">{{ statSummary.totalOrders }}</p>
      </div>
      <div class="stat-card">
        <h3>总金额</h3>
        <p class="stat-value">¥{{ statSummary.totalAmount.toFixed(2) }}</p>
      </div>
      <div class="stat-card">
        <h3>平均订单金额</h3>
        <p class="stat-value">¥{{ statSummary.avgOrderAmount.toFixed(2) }}</p>
      </div>
      <div class="stat-card">
        <h3>已使用订单</h3>
        <p class="stat-value">{{ statSummary.usedOrders }}</p>
      </div>
      <div class="stat-card">
        <h3>待支付</h3>
        <p class="stat-value">{{ statSummary.pendingOrders }}</p>
      </div>
      <div class="stat-card">
        <h3>秒杀订单</h3>
        <p class="stat-value">{{ statSummary.seckillOrders }}</p>
      </div>
    </section>

    <section class="charts-section">
      <div class="chart-card">
        <h3>订单趋势</h3>
        <div v-if="trendRows.length" class="simple-list">
          <div v-for="item in trendRows" :key="item.day" class="simple-row">
            <span>{{ item.day }}</span>
            <span>{{ item.orderCount }} 单 / ¥{{ item.amount.toFixed(2) }}</span>
          </div>
        </div>
        <div v-else class="chart-placeholder">暂无趋势数据</div>
      </div>
      <div class="chart-card">
        <h3>优惠券使用 Top10</h3>
        <div v-if="voucherUsageRows.length" class="simple-list">
          <div v-for="item in voucherUsageRows" :key="`${item.voucherId}-${item.voucherTitle}`" class="simple-row">
            <span>#{{ item.voucherId }} {{ item.voucherTitle || '未知券' }}</span>
            <span>{{ item.useCount }} 单</span>
          </div>
        </div>
        <div v-else class="chart-placeholder">暂无使用数据</div>
      </div>
    </section>

    <section class="orders-table-section">
      <h3>订单列表</h3>
      <div class="table-wrap">
        <table class="orders-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>用户</th>
              <th>券信息</th>
              <th>来源</th>
              <th>支付方式</th>
              <th>金额</th>
              <th>订单状态</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="state-text">加载中...</td>
            </tr>
            <tr v-else-if="orderRows.length === 0">
              <td colspan="8" class="state-text">暂无订单数据</td>
            </tr>
            <tr v-else v-for="order in orderRows" :key="order.id">
              <td>{{ order.orderNo }}</td>
              <td>{{ order.userName || `用户${order.userId}` }}</td>
              <td>
                <div>{{ order.voucherTitle || '-' }}</div>
                <div class="sub-text">店铺: {{ order.shopName || '-' }}</div>
              </td>
              <td>{{ sourceText(order.orderSource) }}</td>
              <td>{{ paymentMethodText(order.paymentMethod) }}</td>
              <td>¥{{ Number(order.payAmount || 0).toFixed(2) }}</td>
              <td>
                <span :class="['status-badge', `status-${order.statusClass || 'unknown'}`]">
                  {{ order.statusText || '-' }}
                </span>
              </td>
              <td>{{ formatTime(order.createTime) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination">
        <button @click="prevPage" :disabled="page <= 1">上一页</button>
        <span>第 {{ page }} 页 / 共 {{ totalPages }} 页（共 {{ total }} 条）</span>
        <button @click="nextPage" :disabled="page >= totalPages">下一页</button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getAdminVoucherOrderList, getAdminVoucherOrderStats, type AdminVoucherOrderItem } from '../../api/admin'
import { showAlert } from '../../utils/dialog'

const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const orderRows = ref<AdminVoucherOrderItem[]>([])
const trendRows = ref<Array<{ day: string; orderCount: number; amount: number }>>([])
const voucherUsageRows = ref<Array<{ voucherId: number; voucherTitle: string; useCount: number }>>([])
const statSummary = ref({
  totalOrders: 0,
  totalAmount: 0,
  avgOrderAmount: 0,
  usedOrders: 0,
  pendingOrders: 0,
  seckillOrders: 0
})

const filters = ref({
  keyword: '',
  userId: undefined as number | undefined,
  voucherId: undefined as number | undefined,
  status: '',
  paymentStatus: '',
  orderSource: '',
  timeRange: 'today',
  startDate: '',
  endDate: ''
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

const buildQuery = () => ({
  keyword: filters.value.keyword || undefined,
  userId: filters.value.userId,
  voucherId: filters.value.voucherId,
  status: filters.value.status === '' ? undefined : Number(filters.value.status),
  paymentStatus: filters.value.paymentStatus === '' ? undefined : Number(filters.value.paymentStatus),
  orderSource: filters.value.orderSource || undefined,
  timeRange: filters.value.timeRange,
  startDate: filters.value.startDate || undefined,
  endDate: filters.value.endDate || undefined
})

const loadOrders = async () => {
  loading.value = true
  try {
    const query = buildQuery()
    const [listRes, statsRes] = await Promise.all([
      getAdminVoucherOrderList({ ...query, page: page.value, size: size.value }),
      getAdminVoucherOrderStats(query)
    ])
    orderRows.value = listRes.list || []
    total.value = Number(listRes.total || 0)
    trendRows.value = (statsRes.trend || []).map(item => ({
      day: String(item.day),
      orderCount: Number(item.orderCount || 0),
      amount: Number(item.amount || 0)
    }))
    voucherUsageRows.value = (statsRes.voucherUsage || []).map(item => ({
      voucherId: Number(item.voucherId || 0),
      voucherTitle: String(item.voucherTitle || ''),
      useCount: Number(item.useCount || 0)
    }))
    const summary = statsRes.summary || ({} as any)
    statSummary.value = {
      totalOrders: Number(summary.totalOrders || 0),
      totalAmount: Number(summary.totalAmount || 0),
      avgOrderAmount: Number(summary.avgOrderAmount || 0),
      usedOrders: Number(summary.usedOrders || 0),
      pendingOrders: Number(summary.pendingOrders || 0),
      seckillOrders: Number(summary.seckillOrders || 0)
    }
  } catch (error) {
    console.error('加载订单管理数据失败:', error)
    await showAlert('加载订单管理数据失败，请重试')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  page.value = 1
  void loadOrders()
}

const handleReset = () => {
  filters.value = {
    keyword: '',
    userId: undefined,
    voucherId: undefined,
    status: '',
    paymentStatus: '',
    orderSource: '',
    timeRange: 'today',
    startDate: '',
    endDate: ''
  }
  page.value = 1
  void loadOrders()
}

const prevPage = () => {
  if (page.value <= 1) return
  page.value -= 1
  void loadOrders()
}

const nextPage = () => {
  if (page.value >= totalPages.value) return
  page.value += 1
  void loadOrders()
}

const formatTime = (time: string | undefined) => {
  if (!time) return '-'
  const value = String(time).replace('T', ' ')
  return value.length >= 16 ? value.substring(0, 16) : value
}

const sourceText = (source: string | undefined) => {
  if (!source) return '-'
  if (source === 'seckill') return '秒杀券'
  if (source === 'app') return '普通券'
  return source
}

const paymentMethodText = (method: string | undefined) => {
  if (!method) return '未支付'
  if (method === 'wallet') return '钱包'
  if (method.toLowerCase().includes('wechat')) return '微信'
  if (method.toLowerCase().includes('alipay')) return '支付宝'
  return method
}

onMounted(() => {
  void loadOrders()
})
</script>

<style scoped>
.admin-voucher-orders {
  width: 100%;
}

.filter-section {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.filter-section select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.filter-section input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.filter-section button {
  padding: 8px 16px;
  border-radius: 6px;
  border: 1px solid #1677ff;
  background: #1677ff;
  color: #fff;
  cursor: pointer;
}

.filter-section .btn-reset {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #334155;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  background-color: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.stat-card h3 {
  font-size: 14px;
  margin-bottom: 8px;
  color: #64748b;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.charts-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.chart-card {
  background-color: #fff;
  padding: 14px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.chart-card h3 {
  font-size: 15px;
  margin-bottom: 12px;
  color: #334155;
}

.chart-placeholder {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  border-radius: 4px;
  color: #888;
}

.simple-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.simple-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  color: #334155;
}

.orders-table-section h3 {
  font-size: 15px;
  margin-bottom: 10px;
  color: #334155;
}

.table-wrap {
  overflow-x: auto;
  border: 1px solid #e3ebf5;
  border-radius: 10px;
}

.orders-table {
  width: 100%;
  border-collapse: collapse;
  background-color: #fff;
  min-width: 980px;
}

.orders-table th,
.orders-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.orders-table th {
  background-color: #f5f5f5;
  font-weight: 600;
  color: #555;
}

.sub-text {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.state-text {
  text-align: center;
  color: #64748b;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
}

.status-pending { background: #fef9c3; color: #92400e; }
.status-paid { background: #dbeafe; color: #1e40af; }
.status-used { background: #dcfce7; color: #166534; }
.status-expired { background: #e5e7eb; color: #374151; }
.status-refunded { background: #fae8ff; color: #7e22ce; }
.status-cancelled { background: #fee2e2; color: #991b1b; }
.status-unknown { background: #f1f5f9; color: #334155; }

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.pagination button {
  padding: 6px 12px;
  border: 1px solid #d3ddec;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

@media (max-width: 768px) {
  .filter-section > * {
    width: 100%;
  }
}
</style>
