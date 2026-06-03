<template>
  <div class="admin-reports">
    <div class="filter-section">
      <select v-model.number="filterStatus" @change="reloadReports">
        <option :value="-1">全部状态</option>
        <option :value="0">待处理</option>
        <option :value="1">已处理</option>
        <option :value="2">已忽略</option>
      </select>
      <select v-model.number="filterType" @change="reloadReports">
        <option :value="-1">全部类型</option>
        <option :value="1">帖子举报</option>
        <option :value="2">评论举报</option>
      </select>
      <input type="text" v-model.trim="searchKeyword" placeholder="搜索举报原因/举报人/目标内容" @keyup.enter="reloadReports">
      <button class="btn search" @click="reloadReports">查询</button>
    </div>

    <div v-if="loading" class="state-tip">举报数据加载中...</div>
    <div v-else-if="errorMsg" class="state-tip error">{{ errorMsg }}</div>

    <table v-else class="reports-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>举报类型</th>
          <th>举报原因</th>
          <th>举报人</th>
          <th>目标内容</th>
          <th>举报时间</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="report in reportsList" :key="report.id">
          <td>#{{ report.id }}</td>
          <td>{{ getReportTypeText(report.targetType) }}</td>
          <td class="report-content">{{ report.reason }}</td>
          <td>{{ report.reporterName || `用户#${report.reporterId}` }}</td>
          <td class="report-content">{{ report.targetSummary || `目标#${report.targetId}` }}</td>
          <td>{{ formatTime(report.createTime) }}</td>
          <td>
            <span :class="['status-badge', getStatusClass(report.status)]">{{ getStatusText(report.status) }}</span>
          </td>
          <td>
            <button
              v-if="report.status === 0"
              @click="processReport(report.id, 1)"
              class="btn process"
            >
              处理
            </button>
            <button
              v-if="report.status === 0"
              @click="processReport(report.id, 2)"
              class="btn ignore"
            >
              忽略
            </button>
            <button @click="viewDetail(report)" class="btn view">详情</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="pager">
      <button class="btn page" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ page }} / {{ totalPages }} 页</span>
      <button class="btn page" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getAdminReportList, processAdminReport, type AdminReport } from '../../api/admin'
import { showAlert, showPrompt } from '../../utils/dialog'

const loading = ref(false)
const errorMsg = ref('')
const reportsList = ref<AdminReport[]>([])

const filterStatus = ref<number>(-1)
const filterType = ref<number>(-1)
const searchKeyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

const totalPages = computed(() => Math.max(1, Math.ceil((total.value || 0) / size.value)))

const reloadReports = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await getAdminReportList({
      page: page.value,
      size: size.value,
      status: filterStatus.value >= 0 ? filterStatus.value : undefined,
      targetType: filterType.value >= 0 ? filterType.value : undefined,
      keyword: searchKeyword.value || undefined
    })
    reportsList.value = data.list || []
    total.value = Number(data.total || 0)
  } catch (e: any) {
    reportsList.value = []
    total.value = 0
    errorMsg.value = e?.message || '举报数据加载失败'
  } finally {
    loading.value = false
  }
}

const processReport = async (id: number, status: 1 | 2) => {
  const actionText = status === 1 ? '处理' : '忽略'
  let action: 'NONE' | 'DELETE_POST' | 'BAN_POST' | 'DELETE_COMMENT' | 'BAN_COMMENT' | 'IGNORE' = 'NONE'
  const current = reportsList.value.find((r) => r.id === id)
  if (status === 1 && current) {
    const guide = current.targetType === 1
      ? '请输入处理方式: 1=仅标记已处理, 2=删除帖子, 3=屏蔽帖子'
      : '请输入处理方式: 1=仅标记已处理, 2=删除评论, 3=屏蔽评论'
    const actionInput = (await showPrompt(guide, '1', '选择处理方式')) || '1'
    const selected = Number(actionInput)
    if (selected === 2) {
      action = current.targetType === 1 ? 'DELETE_POST' : 'DELETE_COMMENT'
    } else if (selected === 3) {
      action = current.targetType === 1 ? 'BAN_POST' : 'BAN_COMMENT'
    } else {
      action = 'NONE'
    }
  }
  if (status === 2) {
    action = 'IGNORE'
  }
  const handleResult = (await showPrompt(`请输入${actionText}备注（可选）`, '', `${actionText}备注`)) || ''
  try {
    await processAdminReport(id, { status, handleResult, action })
    await reloadReports()
  } catch (e: any) {
    await showAlert(e?.message || `${actionText}失败`)
  }
}

const viewDetail = (report: AdminReport) => {
  const lines = [
    `举报ID: ${report.id}`,
    `类型: ${getReportTypeText(report.targetType)}`,
    `举报人: ${report.reporterName || `用户#${report.reporterId}`}`,
    `目标: ${report.targetSummary || `目标#${report.targetId}`}`,
    `原因: ${report.reason}`,
    `状态: ${getStatusText(report.status)}`,
    `处理结果: ${report.handleResult || '暂无'}`,
    `时间: ${formatTime(report.createTime)}`
  ]
  void showAlert(lines.join('\n'), 2600)
}

const changePage = (next: number) => {
  page.value = next
  reloadReports()
}

const getReportTypeText = (type: number) => {
  if (type === 1) return '帖子'
  if (type === 2) return '评论'
  return '其他'
}

const getStatusText = (status: number) => {
  if (status === 0) return '待处理'
  if (status === 1) return '已处理'
  if (status === 2) return '已忽略'
  return '未知'
}

const getStatusClass = (status: number) => {
  if (status === 0) return 'pending'
  if (status === 1) return 'processed'
  return 'ignored'
}

const formatTime = (value?: string) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}

onMounted(() => {
  reloadReports()
})
</script>

<style scoped>
.admin-reports {
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
  font-size: 14px;
  min-height: 38px;
}

.filter-section select:focus,
.filter-section input:focus {
  outline: none;
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}

.filter-section input {
  min-width: 260px;
}

.reports-table {
  width: 100%;
  border-collapse: collapse;
  background-color: #fff;
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.reports-table th,
.reports-table td {
  padding: 10px;
  text-align: left;
  border-bottom: 1px solid #edf2f7;
  font-size: 13px;
}

.reports-table th {
  background-color: #f8fbff;
  font-weight: 600;
  color: #334155;
}

.report-content {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge {
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.pending {
  background-color: #fff3cd;
  color: #856404;
}

.status-badge.processed {
  background-color: #d4edda;
  color: #155724;
}

.status-badge.ignored {
  background-color: #eceff4;
  color: #4c566a;
}

.btn {
  padding: 6px 10px;
  margin-right: 5px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
}

.btn.process {
  background-color: #1677ff;
  color: #fff;
}

.btn.ignore {
  background-color: #faad14;
  color: #fff;
}

.btn.view {
  background-color: #17a2b8;
  color: #fff;
}

.btn.search {
  background-color: #1677ff;
  border-color: #1677ff;
  color: #fff;
}

.state-tip {
  padding: 16px;
  text-align: center;
  color: #475569;
  background: #fff;
  border: 1px solid #e3ebf5;
  border-radius: 12px;
}

.state-tip.error {
  color: #d4380d;
}

.pager {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  color: #475569;
}

.btn.page {
  background: #f8fafc;
  border-color: #d3ddec;
  color: #475569;
}

.btn.page:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
