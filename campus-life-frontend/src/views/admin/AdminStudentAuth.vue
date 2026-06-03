<template>
  <div class="admin-student-auth">
    <div class="filter-section">
      <select v-model="filterStatus">
        <option value="all">全部状态</option>
        <option value="pending">待审核</option>
        <option value="approved">已通过</option>
        <option value="rejected">已拒绝</option>
      </select>
      <input type="text" v-model="searchKeyword" placeholder="搜索姓名/学号/学校/用户ID">
      <button class="btn view" @click="reload">查询</button>
    </div>
    <table class="auth-table">
      <thead>
        <tr>
          <th>申请ID</th>
          <th>用户ID</th>
          <th>学号</th>
          <th>姓名</th>
          <th>学校</th>
          <th>学院</th>
          <th>提交时间</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="auth in filteredAuthList" :key="auth.id">
          <td>{{ auth.id }}</td>
          <td>{{ auth.userId }}</td>
          <td>{{ auth.studentId }}</td>
          <td>{{ auth.name }}</td>
          <td>{{ auth.school || '-' }}</td>
          <td>{{ auth.college }}</td>
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
        <tr v-if="!filteredAuthList.length">
          <td colspan="10" class="empty-row">暂无认证申请</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  getStudentAuthRequests,
  approveStudentAuth,
  rejectStudentAuth,
  type StudentAuthRequestItem
} from '../../api/admin'
import { showAlert } from '../../utils/dialog'

interface StudentAuth {
  id: number
  userId: number
  studentId: string | number
  name: string
  school?: string
  major?: string
  grade?: string
  className?: string
  studentCardImg?: string
  college: string
  submitTime: string
  status: 'pending' | 'approved' | 'rejected'
  reason?: string
  reviewTime?: string
}

const filterStatus = ref('all')
const searchKeyword = ref('')
const studentAuthList = ref<StudentAuth[]>([])

const mapStatus = (status: number): StudentAuth['status'] => {
  if (status === 1) return 'approved'
  if (status === 2) return 'rejected'
  return 'pending'
}

const loadStudentAuthList = async () => {
  try {
    const statusMap: Record<string, number | undefined> = {
      all: undefined,
      pending: 0,
      approved: 1,
      rejected: 2
    }
    const data = await getStudentAuthRequests({
      page: 1,
      size: 300,
      status: statusMap[filterStatus.value],
      keyword: searchKeyword.value.trim() || undefined
    })
    const list = (data?.list || []) as StudentAuthRequestItem[]
    studentAuthList.value = list.map(item => ({
      id: item.id,
      userId: item.userId,
      studentId: item.studentNo || '-',
      name: item.realName || `用户${item.userId}`,
      school: item.school || '-',
      college: item.college || '-',
      major: item.major || '-',
      grade: item.grade || '-',
      className: item.className || '-',
      studentCardImg: item.studentCardImg || '',
      submitTime: item.createTime || '',
      status: mapStatus(Number(item.status || 0)),
      reason: item.reason || '',
      reviewTime: item.reviewTime || ''
    }))
  } catch (error: any) {
    studentAuthList.value = []
    await showAlert(error?.message || '加载学生审核列表失败')
  }
}

const reload = () => {
  void loadStudentAuthList()
}

const filteredAuthList = computed(() => {
  return studentAuthList.value.filter(auth => {
    const matchesStatus = filterStatus.value === 'all' || auth.status === filterStatus.value
    const kw = searchKeyword.value.trim()
    if (!kw) return matchesStatus
    const matchesKeyword =
      auth.name.includes(kw) ||
      String(auth.studentId).includes(kw) ||
      String(auth.userId).includes(kw) ||
      String(auth.school || '').includes(kw)
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
  const auth = studentAuthList.value.find(a => a.id === id)
  if (!auth) return
  try {
    await approveStudentAuth(id, '后台审核通过')
    auth.status = 'approved'
    await showAlert('已通过学生审核')
  } catch (error: any) {
    await showAlert(error?.message || '审核通过失败')
  }
}

const rejectAuth = async (id: number) => {
  const auth = studentAuthList.value.find(a => a.id === id)
  if (!auth) return
  try {
    await rejectStudentAuth(id, '后台审核驳回')
    auth.status = 'rejected'
    await showAlert('已拒绝学生审核')
  } catch (error: any) {
    await showAlert(error?.message || '审核拒绝失败')
  }
}

const viewDetail = async (id: number) => {
  const auth = studentAuthList.value.find(a => a.id === id)
  if (!auth) return
  await showAlert(
    `申请ID：${auth.id}\n用户ID：${auth.userId}\n姓名：${auth.name}\n学校：${auth.school || '-'}\n学院：${auth.college}\n专业：${auth.major || '-'}\n年级：${auth.grade || '-'}\n班级：${auth.className || '-'}\n学号：${auth.studentId}\n提交时间：${auth.submitTime}\n审核时间：${auth.reviewTime || '-'}\n驳回原因：${auth.reason || '-'}`
  )
}

onMounted(() => {
  loadStudentAuthList()
})
</script>

<style scoped>
.admin-student-auth {
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

.empty-row {
  text-align: center;
  color: #888;
}
</style>
