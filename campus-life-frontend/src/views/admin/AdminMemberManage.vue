<template>
  <div class="admin-member-manage">
    <section class="hero-panel">
      <div class="hero-main">
        <p class="hero-tag">MEMBER CENTER</p>
        <h2 class="hero-title">会员管理</h2>
        <p class="hero-subtitle">统一管理会员信息、状态、筛选和批量操作</p>
      </div>
      <div class="hero-breadcrumb">
        <button class="crumb-btn" @click="navigateTo('/admin/home')">首页</button>
        <span>/</span>
        <button class="crumb-btn" @click="navigateTo('/admin/dashboard')">工作台</button>
        <span>/</span>
        <span class="crumb-current">会员管理</span>
      </div>
    </section>

    <section class="overview-grid">
      <article class="overview-card">
        <p class="overview-label">会员总数</p>
        <p class="overview-value">{{ total }}</p>
      </article>
      <article class="overview-card">
        <p class="overview-label">启用会员</p>
        <p class="overview-value success">{{ enabledCount }}</p>
      </article>
      <article class="overview-card">
        <p class="overview-label">禁用会员</p>
        <p class="overview-value danger">{{ disabledCount }}</p>
      </article>
      <article class="overview-card">
        <p class="overview-label">已选中</p>
        <p class="overview-value">{{ selectedCount }}</p>
      </article>
    </section>

    <section class="query-panel">
      <div class="query-panel-title">筛选条件</div>
      <div class="filter-grid filter-grid-primary">
        <div class="filter-item">
          <label>关键词</label>
          <input
            v-model="filters.keyword"
            type="text"
            placeholder="用户ID/昵称/手机/邮箱"
            class="filter-input"
          />
        </div>
        <div class="filter-item">
          <label>用户号</label>
          <input
            v-model="filters.xiaolanshuId"
            type="text"
            placeholder="请输入用户号"
            class="filter-input"
          />
        </div>
        <div class="filter-item">
          <label>会员名称</label>
          <input
            v-model="filters.memberName"
            type="text"
            placeholder="请输入会员名称"
            class="filter-input"
          />
        </div>
        <div class="filter-item">
          <label>手机号</label>
          <input
            v-model="filters.phone"
            type="text"
            placeholder="请输入手机号"
            class="filter-input"
          />
        </div>
        <div class="filter-item">
          <label>邮箱</label>
          <input
            v-model="filters.email"
            type="text"
            placeholder="请输入邮箱"
            class="filter-input"
          />
        </div>
      </div>
      <div class="filter-grid filter-grid-secondary">
        <div class="filter-item">
          <label>会员类型</label>
          <select v-model="filters.role" class="filter-select">
            <option value="">全部</option>
            <option value="0">学生</option>
            <option value="1">商家</option>
          </select>
        </div>
        <div class="filter-item">
          <label>状态</label>
          <select v-model="filters.status" class="filter-select">
            <option value="">全部</option>
            <option value="1">启用</option>
            <option value="0">禁用</option>
          </select>
        </div>
        <div class="filter-item range-item">
          <label>注册时间</label>
          <div class="date-range">
            <input v-model="filters.startDate" type="date" class="filter-input" />
            <span class="date-separator">至</span>
            <input v-model="filters.endDate" type="date" class="filter-input" />
          </div>
        </div>
      </div>
      <div class="query-actions">
        <button class="btn btn-primary" @click="handleQuery">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"></circle>
            <path d="m21 21-4.35-4.35"></path>
          </svg>
          查询
        </button>
        <button class="btn btn-secondary" @click="handleReset">重置</button>
      </div>
    </section>

    <section class="list-panel">
      <header class="list-toolbar">
        <div class="toolbar-left">
          <button class="btn btn-danger" :disabled="selectedIds.length === 0" @click="handleBatchDisable">
            禁用
          </button>
        </div>
        <div class="toolbar-right">
          当前页 {{ memberList.length }} 条 / 共 {{ total }} 条
        </div>
      </header>

      <div v-if="isLoading" class="state-block">正在加载会员数据...</div>
      <div v-else-if="memberList.length === 0" class="state-block empty">暂无会员数据，请调整筛选条件后重试</div>
      <template v-else>
        <div class="table-wrap">
          <table class="data-table desktop-table">
            <thead>
              <tr>
                <th>
                  <input class="checkbox" type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
                </th>
                <th>编号</th>
                <th>用户号</th>
                <th>头像</th>
                <th>会员名称</th>
                <th>性别</th>
                <th>手机号</th>
                <th>会员类型</th>
                <th>笔记数量</th>
                <th>商品数量</th>
                <th>登录IP</th>
                <th>登录地点</th>
                <th>状态</th>
                <th>注册时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="member in memberList" :key="member.id">
                <td>
                  <input v-model="selectedIds" class="checkbox" type="checkbox" :value="member.id" />
                </td>
                <td>{{ member.id }}</td>
                <td>{{ member.xiaolanshuId || '-' }}</td>
                <td>
                  <img
                    :src="member.avatar || '/default-avatar.png'"
                    :alt="member.name"
                    class="avatar-img"
                    @error="handleAvatarError"
                  />
                </td>
                <td>{{ member.name || '微信用户' }}</td>
                <td>{{ getGenderText(member.gender) }}</td>
                <td>{{ member.phone || '-' }}</td>
                <td>{{ getRoleText(member.role) }}</td>
                <td>{{ member.noteCount || 0 }}</td>
                <td>{{ member.productCount || 0 }}</td>
                <td>{{ member.loginIp || '-' }}</td>
                <td>{{ member.loginLocation || '内网' }}</td>
                <td>
                  <label class="switch">
                    <input type="checkbox" :checked="member.status === 1" @change="toggleStatus(member.id, $event)" />
                    <span class="slider"></span>
                  </label>
                </td>
                <td>{{ formatTime(member.createTime) }}</td>
                <td class="action-cell">
                  <a href="#" class="action-link" @click.prevent="viewMember(member)">详情</a>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mobile-card-list">
          <div v-for="member in memberList" :key="member.id" class="mobile-card">
            <div class="card-header">
              <input v-model="selectedIds" class="checkbox" type="checkbox" :value="member.id" />
              <img
                :src="member.avatar || '/default-avatar.png'"
                :alt="member.name"
                class="card-avatar"
                @error="handleAvatarError"
              />
              <div class="card-title">
                <span class="card-name">{{ member.name || '微信用户' }}</span>
                <span class="card-id">ID: {{ member.id }}</span>
              </div>
              <label class="switch">
                <input type="checkbox" :checked="member.status === 1" @change="toggleStatus(member.id, $event)" />
                <span class="slider"></span>
              </label>
            </div>
            <div class="card-body">
              <div class="card-row">
                <span class="card-label">用户号：</span>
                <span class="card-value">{{ member.xiaolanshuId || '-' }}</span>
              </div>
              <div class="card-row">
                <span class="card-label">手机号：</span>
                <span class="card-value">{{ member.phone || '-' }}</span>
              </div>
              <div class="card-row">
                <span class="card-label">来源：</span>
                <span class="card-value">{{ getSourceText(member.source) }}</span>
              </div>
              <div class="card-stats">
                <div class="stat-item">
                  <span class="stat-label">笔记</span>
                  <span class="stat-value">{{ member.noteCount || 0 }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">商品</span>
                  <span class="stat-value">{{ member.productCount || 0 }}</span>
                </div>
              </div>
              <div class="card-row">
                <span class="card-label">登录IP：</span>
                <span class="card-value">{{ member.loginIp || '-' }}</span>
              </div>
              <div class="card-row">
                <span class="card-label">登录地点：</span>
                <span class="card-value">{{ member.loginLocation || '内网' }}</span>
              </div>
              <div class="card-row">
                <span class="card-label">注册时间：</span>
                <span class="card-value">{{ formatTime(member.createTime) }}</span>
              </div>
            </div>
            <div class="card-actions">
              <button class="btn btn-sm btn-primary" @click="viewMember(member)">详情</button>
            </div>
          </div>
        </div>

        <div class="pagination">
          <button class="page-btn" :disabled="page === 1" @click="prevPage">上一页</button>
          <span class="page-info">第 {{ page }} 页，共 {{ totalPages }} 页</span>
          <button class="page-btn" :disabled="page >= totalPages" @click="nextPage">下一页</button>
        </div>
      </template>
    </section>

    <div v-if="showDetailDialog" class="dialog-overlay" @click="showDetailDialog = false">
      <div class="dialog" @click.stop>
        <h3>会员详情</h3>
        <div v-if="selectedMember" class="detail-content">
          <div class="detail-row"><span class="detail-label">ID：</span><span class="detail-value">{{ selectedMember.id }}</span></div>
          <div class="detail-row"><span class="detail-label">用户号：</span><span class="detail-value">{{ selectedMember.xiaolanshuId || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">名称：</span><span class="detail-value">{{ selectedMember.name || '微信用户' }}</span></div>
          <div class="detail-row"><span class="detail-label">手机号：</span><span class="detail-value">{{ selectedMember.phone || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">角色：</span><span class="detail-value">{{ getRoleText(selectedMember.role) }}</span></div>
          <div class="detail-row"><span class="detail-label">状态：</span><span class="detail-value">{{ selectedMember.status === 1 ? '启用' : '禁用' }}</span></div>
          <div class="detail-row"><span class="detail-label">笔记数：</span><span class="detail-value">{{ selectedMember.noteCount || 0 }}</span></div>
          <div class="detail-row"><span class="detail-label">商品数：</span><span class="detail-value">{{ selectedMember.productCount || 0 }}</span></div>
          <div class="detail-row"><span class="detail-label">登录IP：</span><span class="detail-value">{{ selectedMember.loginIp || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">登录地点：</span><span class="detail-value">{{ selectedMember.loginLocation || '内网' }}</span></div>
          <div class="detail-row"><span class="detail-label">注册时间：</span><span class="detail-value">{{ formatTime(selectedMember.createTime) }}</span></div>
        </div>
        <div class="dialog-buttons">
          <button class="btn btn-secondary" @click="showDetailDialog = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMemberList, updateMemberStatus } from '../../api/admin'
import { showAlert, showConfirm } from '../../utils/dialog'

const router = useRouter()

// 筛选条件
const filters = ref({
  keyword: '',
  xiaolanshuId: '',
  memberName: '',
  phone: '',
  email: '',
  role: '',
  status: '',
  startDate: '',
  endDate: ''
})

// 会员列表
const memberList = ref<any[]>([])
const selectedIds = ref<number[]>([])
const isLoading = ref(false)
const enabledTotal = ref(0)
const disabledTotal = ref(0)
const selectedMember = ref<any | null>(null)
const showDetailDialog = ref(false)

// 分页
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const selectedCount = computed(() => selectedIds.value.length)
const enabledCount = computed(() => enabledTotal.value)
const disabledCount = computed(() => disabledTotal.value)

const isAllSelected = computed(() => {
  return memberList.value.length > 0 && selectedIds.value.length === memberList.value.length
})

// 加载会员列表
const loadMembers = async () => {
  isLoading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize.value,
      ...filters.value
    }
    const data = await getMemberList(params)
    if (data) {
      memberList.value = data.list || []
      total.value = data.total || 0
      enabledTotal.value = typeof data.enabledTotal === 'number'
        ? data.enabledTotal
        : memberList.value.filter(member => member.status === 1).length
      disabledTotal.value = typeof data.disabledTotal === 'number'
        ? data.disabledTotal
        : memberList.value.filter(member => member.status === 0).length
    }
  } catch (error) {
    console.error('加载会员列表失败:', error)
    memberList.value = []
    total.value = 0
    enabledTotal.value = 0
    disabledTotal.value = 0
    void showAlert('加载会员列表失败，请重试')
  } finally {
    isLoading.value = false
  }
}

// 查询
const handleQuery = () => {
  page.value = 1
  loadMembers()
}

// 重置
const handleReset = () => {
  filters.value = {
    xiaolanshuId: '',
    keyword: '',
    memberName: '',
    phone: '',
    email: '',
    role: '',
    status: '',
    startDate: '',
    endDate: ''
  }
  handleQuery()
}

// 批量禁用
const handleBatchDisable = async () => {
  if (selectedIds.value.length === 0) return
  const confirmed = await showConfirm(`确定要禁用选中的 ${selectedIds.value.length} 个会员吗？`)
  if (!confirmed) return
  try {
    await Promise.all(selectedIds.value.map(id => updateMemberStatus(id, 0)))
    selectedIds.value = []
    loadMembers()
  } catch (error) {
    console.error('禁用失败:', error)
  }
}

// 切换状态
const toggleStatus = async (id: number, event: Event) => {
  const checked = (event.target as HTMLInputElement).checked
  try {
    await updateMemberStatus(id, checked ? 1 : 0)
    loadMembers()
  } catch (error) {
    console.error('更新状态失败:', error)
    // 恢复原状态
    loadMembers()
  }
}

const viewMember = (member: any) => {
  selectedMember.value = member
  showDetailDialog.value = true
}

// 全选/取消全选
const toggleSelectAll = (event: Event) => {
  const checked = (event.target as HTMLInputElement).checked
  if (checked) {
    selectedIds.value = memberList.value.map(m => m.id)
  } else {
    selectedIds.value = []
  }
}

// 上一页
const prevPage = () => {
  if (page.value > 1) {
    page.value--
    loadMembers()
  }
}

// 下一页
const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    loadMembers()
  }
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

// 获取性别文本
const getGenderText = (gender: number) => {
  switch (gender) {
    case 1: return '男'
    case 2: return '女'
    default: return '未知'
  }
}

const getRoleText = (role: number) => {
  switch (Number(role)) {
    case 2: return '管理员'
    case 1: return '商家'
    case 0: return '学生'
    default: return '普通用户'
  }
}

// 获取来源文本
const getSourceText = (source: string) => {
  const sourceMap: Record<string, string> = {
    'app': 'app端',
    'web': 'web端',
    'miniapp': '小程序端'
  }
  return sourceMap[source] || source || '-'
}

// 头像加载错误处理
const handleAvatarError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjAiIGN5PSIyMCIgcj0iMjAiIGZpbGw9IiNGNUY1RjUiLz4KPHBhdGggZD0iTTIwIDEyQzIyLjIwOTEgMTIgMjQgMTMuNzkwOSAyNCAxNkMyNCAxOC4yMDkxIDIyLjIwOTEgMjAgMjAgMjBDMTcuNzkwOSAyMCAxNiAxOC4yMDkxIDE2IDE2QzE2IDEzLjc5MDkgMTcuNzkwOSAxMiAyMCAxMloiIGZpbGw9IiM5OTk5OTkiLz4KPHBhdGggZD0iTTIwIDIyQzE1LjU4MTcgMjIgMTIgMjQuNTgxNyAxMiAyOVYzMkgyOFYyOUMyOCAyNC41ODE3IDI0LjQxODMgMjIgMjAgMjJaIiBmaWxsPSIjOTk5OTk5Ii8+Cjwvc3ZnPgo='
}

// 导航
const navigateTo = (path: string) => {
  router.push(path)
}

onMounted(() => {
  loadMembers()
})
</script>

<style scoped>
.admin-member-manage {
  width: 100%;
  color: #1f2937;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-panel,
.query-panel,
.list-panel,
.overview-card {
  background: #ffffff;
  border: 1px solid #e6edf7;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.hero-panel {
  padding: 18px 20px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(135deg, #ffffff, #f7fbff);
}

.hero-tag {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.12em;
  color: #64748b;
  font-weight: 700;
}

.hero-title {
  margin: 5px 0 0;
  font-size: 22px;
  line-height: 1.2;
  color: #0f172a;
}

.hero-subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: #64748b;
}

.hero-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
  font-size: 13px;
  padding: 7px 10px;
  border-radius: 10px;
  background: #f8fbff;
}

.crumb-btn {
  border: none;
  padding: 0;
  background: transparent;
  color: #475569;
  cursor: pointer;
}

.crumb-btn:hover {
  color: #1677ff;
}

.crumb-current {
  color: #0f172a;
  font-weight: 600;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.overview-card {
  padding: 14px 16px;
}

.overview-label {
  margin: 0 0 8px;
  font-size: 13px;
  color: #64748b;
}

.overview-value {
  margin: 0;
  font-size: 24px;
  line-height: 1;
  font-weight: 700;
  color: #0f172a;
}

.overview-value.success {
  color: #16a34a;
}

.overview-value.danger {
  color: #dc2626;
}

.query-panel,
.list-panel {
  padding: 18px;
}

.query-panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 14px;
}

.filter-grid {
  display: grid;
  gap: 12px;
}

.filter-grid-primary {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.filter-grid-secondary {
  margin-top: 12px;
  grid-template-columns: 1.1fr 1.1fr 2.8fr;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.filter-item label {
  font-size: 13px;
  color: #475569;
  font-weight: 600;
}

.filter-input,
.filter-select {
  border: 1px solid #d5e2f2;
  border-radius: 8px;
  background: #fff;
  height: 38px;
  padding: 0 12px;
  font-size: 14px;
  color: #0f172a;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.filter-input:focus,
.filter-select:focus {
  outline: none;
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}

.range-item .date-range {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 8px;
  align-items: center;
}

.date-separator {
  font-size: 13px;
  color: #94a3b8;
}

.query-actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #eef3f9;
}

.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef3f9;
}

.toolbar-left {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar-right {
  color: #64748b;
  font-size: 13px;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
  border: 1px solid #eef3f9;
  border-radius: 10px;
}

.state-block {
  min-height: 220px;
  border: 1px dashed #dce6f2;
  border-radius: 10px;
  background: #f8fafc;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #64748b;
  font-size: 14px;
}

.state-block.empty {
  color: #94a3b8;
}

.data-table {
  width: 100%;
  min-width: 1240px;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 10px 10px;
  text-align: left;
  border-bottom: 1px solid #eef3f9;
}

.data-table th {
  background: #f8fbff;
  color: #334155;
  font-weight: 700;
  white-space: nowrap;
}

.data-table tbody tr:hover {
  background: #f8fbff;
}

.checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e5edf8;
}

.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 22px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  inset: 0;
  border-radius: 22px;
  background-color: #cbd5e1;
  cursor: pointer;
  transition: 0.3s;
}

.slider:before {
  content: "";
  position: absolute;
  left: 2px;
  bottom: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ffffff;
  transition: 0.3s;
}

input:checked + .slider {
  background-color: #1677ff;
}

input:checked + .slider:before {
  transform: translateX(22px);
}

.action-cell {
  white-space: nowrap;
}

.action-link {
  color: #1677ff;
  text-decoration: none;
  margin-right: 10px;
}

.action-link:hover {
  color: #0958d9;
}

.action-link.danger {
  color: #ef4444;
}

.action-link.danger:hover {
  color: #dc2626;
}

.mobile-card-list {
  display: none;
}

.mobile-card {
  background: #fff;
  border: 1px solid #e6edf7;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eef3f9;
}

.card-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e5edf8;
}

.card-title {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-name {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.card-id {
  font-size: 12px;
  color: #64748b;
}

.card-row {
  display: flex;
  font-size: 13px;
  margin-bottom: 7px;
}

.card-label {
  color: #64748b;
  width: 74px;
  flex-shrink: 0;
}

.card-value {
  color: #0f172a;
  flex: 1;
}

.card-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 10px 0;
}

.stat-item {
  border: 1px solid #e6edf7;
  border-radius: 8px;
  padding: 8px;
  text-align: center;
  background: #f8fbff;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.stat-value {
  display: block;
  margin-top: 4px;
  font-size: 17px;
  color: #0f172a;
  font-weight: 700;
}

.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #eef3f9;
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  width: min(520px, calc(100vw - 32px));
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e6edf7;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.16);
  padding: 20px;
}

.dialog h3 {
  margin: 0 0 16px;
  font-size: 18px;
  color: #0f172a;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  display: flex;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eef3f9;
}

.detail-label {
  width: 88px;
  color: #64748b;
  font-weight: 600;
  flex-shrink: 0;
}

.detail-value {
  color: #0f172a;
  word-break: break-all;
}

.dialog-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 14px;
}

.page-info {
  color: #64748b;
  font-size: 13px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: 0.2s ease;
}

.btn svg {
  width: 15px;
  height: 15px;
}

.btn-primary {
  background: #1677ff;
  color: #fff;
  border-color: #1677ff;
}

.btn-primary:hover:not(:disabled) {
  background: #0958d9;
  border-color: #0958d9;
}

.btn-secondary {
  background: #f8fafc;
  color: #475569;
  border-color: #d5e2f2;
}

.btn-secondary:hover:not(:disabled) {
  background: #eef3f9;
}

.btn-danger {
  background: #ef4444;
  color: #fff;
  border-color: #ef4444;
}

.btn-danger:hover:not(:disabled) {
  background: #dc2626;
  border-color: #dc2626;
}

.btn-sm {
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
}

.btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.page-btn {
  height: 34px;
  border-radius: 8px;
  border: 1px solid #d5e2f2;
  background: #fff;
  color: #475569;
  padding: 0 14px;
  cursor: pointer;
}

.page-btn:hover:not(:disabled) {
  border-color: #1677ff;
  color: #1677ff;
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

@media (max-width: 1080px) {
  .filter-grid-primary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .filter-grid-secondary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .range-item {
    grid-column: span 3;
  }
}

@media (max-width: 768px) {
  .hero-panel {
    flex-direction: column;
    padding: 14px;
  }

  .hero-title {
    font-size: 19px;
  }

  .hero-breadcrumb {
    width: 100%;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .query-panel,
  .list-panel {
    padding: 14px;
  }

  .filter-grid-primary,
  .filter-grid-secondary {
    grid-template-columns: 1fr;
  }

  .range-item {
    grid-column: auto;
  }

  .query-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .list-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar-right {
    text-align: right;
  }

  .desktop-table {
    display: none;
  }

  .mobile-card-list {
    display: block;
  }

  .pagination {
    justify-content: space-between;
  }
}
</style>
