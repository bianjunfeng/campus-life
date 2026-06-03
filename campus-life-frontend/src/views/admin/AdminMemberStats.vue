<template>
  <div class="admin-member-stats">
    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="navigateTo('/admin/home')">首页</span>
      <span class="breadcrumb-separator">></span>
      <span class="breadcrumb-item" @click="navigateTo('/admin/dashboard')">工作台</span>
      <span class="breadcrumb-separator">></span>
      <span class="breadcrumb-item active">会员统计</span>
    </div>

    <!-- 统计概览卡片 -->
    <div class="stats-overview">
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">学生总数</div>
          <div class="stat-value">{{ overview.totalStudents }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon merchant">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <path d="M16 10a4 4 0 0 1-8 0"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">商家总数</div>
          <div class="stat-value">{{ overview.totalMerchants }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
            <polyline points="9 22 9 12 15 12 15 22"></polyline>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">学校数量</div>
          <div class="stat-value">{{ overview.totalSchools }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">已认证学生</div>
          <div class="stat-value">{{ overview.verifiedStudents }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon merchant">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">已认证商家</div>
          <div class="stat-value">{{ overview.verifiedMerchants }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">本月新增</div>
          <div class="stat-value">{{ overview.monthlyNew }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 性别分布 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3>性别分布</h3>
        </div>
        <div class="chart-content">
          <div class="pie-chart" ref="genderChartRef"></div>
          <div class="chart-legend">
            <div v-for="(item, index) in genderData" :key="index" class="legend-item">
              <span class="legend-color" :style="{ backgroundColor: item.color }"></span>
              <span class="legend-label">{{ item.name }}</span>
              <span class="legend-value">{{ item.value }}人 ({{ item.percent }}%)</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 年级分布 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3>年级分布</h3>
        </div>
        <div class="chart-content">
          <div class="bar-chart" ref="gradeChartRef"></div>
        </div>
      </div>

      <!-- 学校分布（Top 10） -->
      <div class="chart-card full-width">
        <div class="chart-header">
          <h3>学校分布（Top 10）</h3>
        </div>
        <div class="chart-content">
          <div class="bar-chart" ref="schoolChartRef"></div>
        </div>
      </div>

      <!-- 学院分布（Top 10） -->
      <div class="chart-card full-width">
        <div class="chart-header">
          <h3>学院分布（Top 10）</h3>
        </div>
        <div class="chart-content">
          <div class="bar-chart" ref="collegeChartRef"></div>
        </div>
      </div>
    </div>

    <!-- 详细统计表格 -->
    <div class="table-card">
      <div class="table-header">
        <h3>学校详细统计</h3>
        <div class="table-actions">
          <select v-model="tableSortBy" class="sort-select">
            <option value="count">按人数排序</option>
            <option value="name">按名称排序</option>
          </select>
        </div>
      </div>
      <table class="data-table desktop-table">
        <thead>
          <tr>
            <th>排名</th>
            <th>学校名称</th>
            <th>学生人数</th>
            <th>占比</th>
            <th>已认证</th>
            <th>未认证</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(school, index) in schoolTableData" :key="school.name">
            <td>
              <span class="rank-badge" :class="getRankClass(index)">{{ index + 1 }}</span>
            </td>
            <td>{{ school.name }}</td>
            <td>{{ school.count }}</td>
            <td>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: school.percent + '%' }"></div>
                <span class="progress-text">{{ school.percent }}%</span>
              </div>
            </td>
            <td>{{ school.verified }}</td>
            <td>{{ school.unverified }}</td>
          </tr>
        </tbody>
      </table>

      <!-- 移动端卡片列表 -->
      <div class="mobile-card-list">
        <div v-for="(school, index) in schoolTableData" :key="school.name" class="mobile-card">
          <div class="card-header">
            <span class="rank-badge" :class="getRankClass(index)">{{ index + 1 }}</span>
            <h4 class="card-title">{{ school.name }}</h4>
          </div>
          <div class="card-body">
            <div class="card-row">
              <span class="card-label">学生人数：</span>
              <span class="card-value">{{ school.count }}</span>
            </div>
            <div class="card-row">
              <span class="card-label">占比：</span>
              <span class="card-value">{{ school.percent }}%</span>
            </div>
            <div class="card-stats">
              <div class="stat-item">
                <span class="stat-label">已认证</span>
                <span class="stat-value">{{ school.verified }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">未认证</span>
                <span class="stat-value">{{ school.unverified }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { GraphicComponent, GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getMemberStats } from '../../api/admin'

echarts.use([BarChart, PieChart, GraphicComponent, GridComponent, TooltipComponent, CanvasRenderer])

type EChartsInstance = ReturnType<typeof echarts.init>

const router = useRouter()

// 图表引用
const genderChartRef = ref<HTMLElement>()
const gradeChartRef = ref<HTMLElement>()
const schoolChartRef = ref<HTMLElement>()
const collegeChartRef = ref<HTMLElement>()

// 图表实例
let genderChart: EChartsInstance | null = null
let gradeChart: EChartsInstance | null = null
let schoolChart: EChartsInstance | null = null
let collegeChart: EChartsInstance | null = null

// 统计概览
const overview = ref({
  totalStudents: 0,
  totalMerchants: 0,
  totalSchools: 0,
  verifiedStudents: 0,
  verifiedMerchants: 0,
  monthlyNew: 0
})

// 性别数据
const genderData = ref<Array<{ name: string; value: number; percent: number; color: string }>>([])

// 年级数据
const gradeData = ref<Array<{ name: string; value: number }>>([])

// 学校数据
const schoolData = ref<Array<{ name: string; value: number; verified: number; unverified: number }>>([])

// 学院数据
const collegeData = ref<Array<{ name: string; value: number }>>([])

// 表格排序
const tableSortBy = ref('count')

// 学校表格数据（排序后）
const schoolTableData = computed(() => {
  const data = [...schoolData.value]
  if (tableSortBy.value === 'name') {
    return data.sort((a, b) => a.name.localeCompare(b.name))
  }
  return data.sort((a, b) => b.value - a.value)
})

// 加载统计数据
const loadStats = async () => {
  try {
    const data = await getMemberStats()
    if (data) {
      overview.value = {
        totalStudents: data.totalStudents || 0,
        totalMerchants: data.totalMerchants || 0,
        totalSchools: data.totalSchools || 0,
        verifiedStudents: data.verifiedStudents || 0,
        verifiedMerchants: data.verifiedMerchants || 0,
        monthlyNew: data.monthlyNew || 0
      }

      if (data.genderData) {
        genderData.value = data.genderData
        updateGenderChart()
      }

      if (data.gradeData) {
        gradeData.value = data.gradeData
        updateGradeChart()
      }

      if (data.schoolData) {
        schoolData.value = data.schoolData
        updateSchoolChart()
      }

      if (data.collegeData) {
        collegeData.value = data.collegeData
        updateCollegeChart()
      }
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    overview.value = {
      totalStudents: 0,
      totalMerchants: 0,
      totalSchools: 0,
      verifiedStudents: 0,
      verifiedMerchants: 0,
      monthlyNew: 0
    }
    genderData.value = []
    gradeData.value = []
    schoolData.value = []
    collegeData.value = []
    updateGenderChart()
    updateGradeChart()
    updateSchoolChart()
    updateCollegeChart()
  }
}

// 更新性别分布图表
const updateGenderChart = () => {
  if (!genderChartRef.value) return

  if (!genderChart) {
    genderChart = echarts.init(genderChartRef.value)
  }

  const total = genderData.value.reduce((sum, item) => sum + item.value, 0)
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}人 ({d}%)'
    },
    series: [{
      name: '性别分布',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        }
      },
      data: genderData.value.map(item => ({
        value: item.value,
        name: item.name,
        itemStyle: {
          color: item.color
        }
      })),
      center: ['50%', '45%']
    }],
    graphic: [{
      type: 'text',
      left: 'center',
      top: '45%',
      style: {
        text: '总人数',
        textAlign: 'center',
        fill: '#333',
        fontSize: 14,
        fontWeight: 'normal'
      }
    }, {
      type: 'text',
      left: 'center',
      top: '52%',
      style: {
        text: total.toString(),
        textAlign: 'center',
        fill: '#333',
        fontSize: 24,
        fontWeight: 'bold'
      }
    }]
  }

  genderChart.setOption(option)
}

// 更新年级分布图表
const updateGradeChart = () => {
  if (!gradeChartRef.value) return

  if (!gradeChart) {
    gradeChart = echarts.init(gradeChartRef.value)
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: gradeData.value.map(item => item.name),
      axisLabel: {
        rotate: 0
      }
    },
    yAxis: {
      type: 'value',
      name: '人数'
    },
    series: [{
      name: '学生人数',
      type: 'bar',
      data: gradeData.value.map(item => item.value),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ])
      },
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#2378f7' },
            { offset: 0.7, color: '#2378f7' },
            { offset: 1, color: '#83bff6' }
          ])
        }
      },
      label: {
        show: true,
        position: 'top'
      }
    }]
  }

  gradeChart.setOption(option)
}

// 更新学校分布图表
const updateSchoolChart = () => {
  if (!schoolChartRef.value) return

  if (!schoolChart) {
    schoolChart = echarts.init(schoolChartRef.value)
  }

  const top10 = schoolData.value.slice(0, 10)
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '人数'
    },
    yAxis: {
      type: 'category',
      data: top10.map(item => item.name),
      axisLabel: {
        interval: 0,
        formatter: (value: string) => {
          return value.length > 8 ? value.substring(0, 8) + '...' : value
        }
      }
    },
    series: [{
      name: '学生人数',
      type: 'bar',
      data: top10.map(item => item.value),
      itemStyle: {
        color: '#91cc75'
      },
      label: {
        show: true,
        position: 'right'
      }
    }]
  }

  schoolChart.setOption(option)
}

// 更新学院分布图表
const updateCollegeChart = () => {
  if (!collegeChartRef.value) return

  if (!collegeChart) {
    collegeChart = echarts.init(collegeChartRef.value)
  }

  const top10 = collegeData.value.slice(0, 10)
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '人数'
    },
    yAxis: {
      type: 'category',
      data: top10.map(item => item.name),
      axisLabel: {
        interval: 0
      }
    },
    series: [{
      name: '学生人数',
      type: 'bar',
      data: top10.map(item => item.value),
      itemStyle: {
        color: '#fac858'
      },
      label: {
        show: true,
        position: 'right'
      }
    }]
  }

  collegeChart.setOption(option)
}

// 获取排名样式
const getRankClass = (index: number) => {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return ''
}

// 窗口大小改变时重新调整图表
const handleResize = () => {
  genderChart?.resize()
  gradeChart?.resize()
  schoolChart?.resize()
  collegeChart?.resize()
}

// 导航
const navigateTo = (path: string) => {
  router.push(path)
}

onMounted(() => {
  nextTick(() => {
    loadStats()
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  genderChart?.dispose()
  gradeChart?.dispose()
  schoolChart?.dispose()
  collegeChart?.dispose()
})
</script>

<style scoped>
.admin-member-stats {
  width: 100%;
}

/* 面包屑导航 */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
}

.breadcrumb-item {
  color: #666;
  cursor: pointer;
  transition: color 0.3s;
}

.breadcrumb-item:hover {
  color: #1677ff;
}

.breadcrumb-item.active {
  color: #333;
  cursor: default;
}

.breadcrumb-separator {
  color: #999;
}

/* 统计概览 */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: #e6f4ff;
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon svg {
  width: 28px;
  height: 28px;
}

.stat-icon.merchant {
  background: #fff7e6;
  color: #fa8c16;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  line-height: 1;
}

/* 图表网格 */
.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

.chart-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chart-card.full-width {
  grid-column: 1 / -1;
}

.chart-header {
  margin-bottom: 20px;
}

.chart-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
  font-weight: 600;
}

.chart-content {
  position: relative;
}

.pie-chart {
  width: 100%;
  height: 300px;
}

.bar-chart {
  width: 100%;
  height: 400px;
}

.chart-legend {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  flex-shrink: 0;
}

.legend-label {
  color: #666;
  flex: 1;
}

.legend-value {
  color: #333;
  font-weight: 600;
}

/* 表格卡片 */
.table-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.table-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
  font-weight: 600;
}

.sort-select {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.data-table th {
  background: #fafafa;
  font-weight: 600;
  color: #333;
}

.data-table tbody tr:hover {
  background: #fafafa;
}

.rank-badge {
  display: inline-block;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  text-align: center;
  line-height: 24px;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  background: #f5f5f5;
}

.rank-gold {
  background: #fffbe6;
  color: #faad14;
  border: 1px solid #ffe7ba;
}

.rank-silver {
  background: #f0f2f5;
  color: #8c8c8c;
  border: 1px solid #d9d9d9;
}

.rank-bronze {
  background: #fff2e8;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.progress-bar {
  position: relative;
  width: 100%;
  height: 24px;
  background: #f5f5f5;
  border-radius: 12px;
  overflow: hidden;
}

.progress-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: linear-gradient(90deg, #1677ff 0%, #69c0ff 100%);
  transition: width 0.3s;
}

.progress-text {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 12px;
  font-weight: 600;
  color: #333;
  z-index: 1;
}

/* 移动端卡片列表 */
.mobile-card-list {
  display: none;
}

.mobile-card {
  background: white;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.card-title {
  flex: 1;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
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
  color: #666;
  min-width: 80px;
}

.card-value {
  color: #333;
  flex: 1;
  font-weight: 500;
}

.card-stats {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 4px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .desktop-table {
    display: none;
  }

  .mobile-card-list {
    display: block;
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }

  .chart-card.full-width {
    grid-column: 1;
  }

  .pie-chart,
  .bar-chart {
    height: 250px;
  }

  .stats-overview {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 769px) {
  .mobile-card-list {
    display: none;
  }
}
</style>
