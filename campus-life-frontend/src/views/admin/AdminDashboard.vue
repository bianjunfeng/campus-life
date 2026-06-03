<template>
  <div class="admin-dashboard">
    <!-- 欢迎信息 -->
    <div class="welcome-section">
      <h2>欢迎回来! {{ userName }}</h2>
    </div>

    <!-- 关键指标卡片 -->
    <div class="metrics-grid">
      <div class="metric-card">
        <div class="metric-icon ip">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
            <line x1="8" y1="21" x2="16" y2="21"></line>
            <line x1="12" y1="17" x2="12" y2="21"></line>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">今日操作IP</div>
          <div class="metric-value">{{ metrics.todayIP }}</div>
          <div class="metric-trend">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
              <polyline points="17 6 23 6 23 12"></polyline>
            </svg>
          </div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon member">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">会员数</div>
          <div class="metric-value">{{ metrics.memberCount }}</div>
          <div class="metric-trend">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
              <polyline points="17 6 23 6 23 12"></polyline>
            </svg>
          </div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon post">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">帖子数</div>
          <div class="metric-value">{{ metrics.postCount }}</div>
          <div class="metric-trend">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
              <polyline points="17 6 23 6 23 12"></polyline>
            </svg>
          </div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon voucher">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="8" width="18" height="4" rx="1"></rect>
            <path d="M12 8v13"></path>
            <path d="M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7"></path>
            <path d="M7.5 8a2.5 2.5 0 0 1 0-5A4.8 8 0 0 1 12 8a4.8 8 0 0 1 4.5-5 2.5 2.5 0 0 1 0 5"></path>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">优惠券数</div>
          <div class="metric-value">{{ metrics.voucherCount }}</div>
          <div class="metric-trend">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
              <polyline points="17 6 23 6 23 12"></polyline>
            </svg>
          </div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon merchant">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <path d="M16 10a4 4 0 0 1-8 0"></path>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">商家数</div>
          <div class="metric-value">{{ metrics.merchantCount }}</div>
          <div class="metric-trend">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
              <polyline points="17 6 23 6 23 12"></polyline>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- 贡献度图表 -->
    <div class="chart-section">
      <div class="chart-card">
        <div class="chart-header">
          <h3>帖子/优惠券贡献度</h3>
          <p class="chart-subtitle">一年内帖子和优惠券的提交数量</p>
        </div>
        <div class="contribution-chart" ref="contributionChartRef"></div>
      </div>
    </div>

    <!-- 饼图分析 -->
    <div class="pie-charts-section">
      <div class="pie-chart-card">
        <div class="chart-header">
          <h3>内容类型占比</h3>
        </div>
        <div class="pie-chart" ref="contentTypeChartRef"></div>
        <div class="chart-legend" v-if="contentTypeData.length > 0">
          <div 
            v-for="(item, index) in contentTypeData" 
            :key="index" 
            class="legend-item"
          >
            <span class="legend-color" :style="{ backgroundColor: item.color }"></span>
            <span class="legend-label">{{ item.name }}</span>
            <span class="legend-value">{{ item.percent }}%</span>
          </div>
        </div>
      </div>

      <div class="pie-chart-card">
        <div class="chart-header">
          <h3>优惠券类型占比</h3>
        </div>
        <div class="pie-chart" ref="voucherTypeChartRef"></div>
        <div class="chart-legend" v-if="voucherTypeData.length > 0">
          <div 
            v-for="(item, index) in voucherTypeData" 
            :key="index" 
            class="legend-item"
          >
            <span class="legend-color" :style="{ backgroundColor: item.color }"></span>
            <span class="legend-label">{{ item.name }}</span>
            <span class="legend-value">{{ item.percent }}%</span>
          </div>
        </div>
      </div>

      <div class="pie-chart-card">
        <div class="chart-header">
          <h3>商家类型占比</h3>
        </div>
        <div class="pie-chart" ref="merchantTypeChartRef"></div>
        <div class="chart-legend" v-if="merchantTypeData.length > 0">
          <div 
            v-for="(item, index) in merchantTypeData" 
            :key="index" 
            class="legend-item"
          >
            <span class="legend-color" :style="{ backgroundColor: item.color }"></span>
            <span class="legend-label">{{ item.name }}</span>
            <span class="legend-value">{{ item.percent }}%</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { HeatmapChart, PieChart } from 'echarts/charts'
import { GraphicComponent, GridComponent, TooltipComponent, VisualMapComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getDashboardStats } from '../../api/admin'

echarts.use([HeatmapChart, PieChart, GraphicComponent, GridComponent, TooltipComponent, VisualMapComponent, CanvasRenderer])

type EChartsInstance = ReturnType<typeof echarts.init>

const userName = ref('管理员')

// 关键指标
const metrics = ref({
  todayIP: 0,
  memberCount: 0,
  postCount: 0,
  voucherCount: 0,
  merchantCount: 0
})

// 图表引用
const contributionChartRef = ref<HTMLElement>()
const contentTypeChartRef = ref<HTMLElement>()
const voucherTypeChartRef = ref<HTMLElement>()
const merchantTypeChartRef = ref<HTMLElement>()

// 图表实例
let contributionChart: EChartsInstance | null = null
let contentTypeChart: EChartsInstance | null = null
let voucherTypeChart: EChartsInstance | null = null
let merchantTypeChart: EChartsInstance | null = null

// 饼图数据
const contentTypeData = ref<Array<{ name: string; value: number; percent: number; color: string }>>([])
const voucherTypeData = ref<Array<{ name: string; value: number; percent: number; color: string }>>([])
const merchantTypeData = ref<Array<{ name: string; value: number; percent: number; color: string }>>([])

// 获取用户名
const getUserName = () => {
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr)
      userName.value = userInfo.username || userInfo.name || '管理员'
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
  }
}

// 加载统计数据
const loadDashboardStats = async () => {
  try {
    const data = await getDashboardStats()
    if (data) {
      metrics.value = {
        todayIP: data.todayIP || 0,
        memberCount: data.memberCount || 0,
        postCount: data.postCount || 0,
        voucherCount: data.voucherCount || 0,
        merchantCount: data.merchantCount || 0
      }

      // 更新贡献度图表
      if (data.contributionData) {
        updateContributionChart(data.contributionData)
      } else {
        updateContributionChart(buildEmptyContributionData())
      }

      // 更新饼图
      if (data.contentTypeData && contentTypeChartRef.value) {
        contentTypeData.value = data.contentTypeData
        updatePieChart(contentTypeChart, contentTypeChartRef.value, data.contentTypeData, '内容量')
      } else {
        contentTypeData.value = []
      }
      if (data.voucherTypeData && voucherTypeChartRef.value) {
        voucherTypeData.value = data.voucherTypeData
        updatePieChart(voucherTypeChart, voucherTypeChartRef.value, data.voucherTypeData, '优惠券量')
      } else {
        voucherTypeData.value = []
      }
      if (data.merchantTypeData && merchantTypeChartRef.value) {
        merchantTypeData.value = data.merchantTypeData
        updatePieChart(merchantTypeChart, merchantTypeChartRef.value, data.merchantTypeData, '商家量')
      } else {
        merchantTypeData.value = []
      }
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    metrics.value = {
      todayIP: 0,
      memberCount: 0,
      postCount: 0,
      voucherCount: 0,
      merchantCount: 0
    }
    contentTypeData.value = []
    voucherTypeData.value = []
    merchantTypeData.value = []
    updateContributionChart(buildEmptyContributionData())
  }
}

const buildEmptyContributionData = () => {
  const months = Array.from({ length: 12 }, (_, i) => `${i + 1}月`)
  const weeks = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const data: number[][] = []
  for (let i = 0; i < months.length; i++) {
    for (let j = 0; j < weeks.length; j++) {
      data.push([i, j, 0])
    }
  }
  return { data, months, weeks }
}

// 更新贡献度图表
const updateContributionChart = (chartData: any) => {
  if (!contributionChartRef.value) return

  if (!contributionChart) {
    contributionChart = echarts.init(contributionChartRef.value)
  }

  const option = {
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        return `${chartData.months[params.data[0]]} ${chartData.weeks[params.data[1]]}<br/>数量: ${params.data[2]}`
      }
    },
    grid: {
      height: '60%',
      top: '10%'
    },
    xAxis: {
      type: 'category',
      data: chartData.months,
      splitArea: {
        show: true
      }
    },
    yAxis: {
      type: 'category',
      data: chartData.weeks,
      splitArea: {
        show: true
      }
    },
    visualMap: {
      min: 0,
      max: 10,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '5%',
      inRange: {
        color: ['#ebedf0', '#c6e48b', '#7bc96f', '#239a3b', '#196127']
      }
    },
    series: [{
      name: '贡献度',
      type: 'heatmap',
      data: chartData.data,
      label: {
        show: true
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }

  contributionChart.setOption(option)
}

// 更新饼图
const updatePieChart = (
  chartInstance: EChartsInstance | null,
  chartRef: HTMLElement | undefined,
  data: Array<{ name: string; value: number; percent: number; color: string }>,
  centerText: string
) => {
  if (!chartRef) return

  let chart = chartInstance
  if (!chart) {
    chart = echarts.init(chartRef)
    // 更新对应的图表实例引用
    if (chartRef === contentTypeChartRef.value) {
      contentTypeChart = chart
    } else if (chartRef === voucherTypeChartRef.value) {
      voucherTypeChart = chart
    } else if (chartRef === merchantTypeChartRef.value) {
      merchantTypeChart = chart
    }
  }

  const total = data.reduce((sum, item) => sum + item.value, 0)
  const colors = data.map(item => item.color)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    series: [{
      name: centerText,
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
      labelLine: {
        show: false
      },
      data: data.map(item => ({
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
        text: centerText,
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

  chart.setOption(option)
}

// 窗口大小改变时重新调整图表
const handleResize = () => {
  contributionChart?.resize()
  contentTypeChart?.resize()
  voucherTypeChart?.resize()
  merchantTypeChart?.resize()
}

onMounted(() => {
  getUserName()
  nextTick(() => {
    loadDashboardStats()
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  contributionChart?.dispose()
  contentTypeChart?.dispose()
  voucherTypeChart?.dispose()
  merchantTypeChart?.dispose()
})
</script>

<style scoped>
.admin-dashboard {
  width: 100%;
}

.welcome-section {
  margin-bottom: 20px;
}

.welcome-section h2 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
  font-weight: 700;
}

/* 关键指标卡片 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.metric-card {
  background: #fff;
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.2s;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.metric-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-icon svg {
  width: 28px;
  height: 28px;
}

.metric-icon.ip {
  background: #eaf3ff;
  color: #2563eb;
}

.metric-icon.member {
  background: #ecfeff;
  color: #0891b2;
}

.metric-icon.post {
  background: #eefbf2;
  color: #16a34a;
}

.metric-icon.voucher {
  background: #fff4ec;
  color: #f97316;
}

.metric-icon.merchant {
  background: #fff7ed;
  color: #ea580c;
}

.metric-content {
  flex: 1;
}

.metric-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1;
  margin-bottom: 4px;
}

.metric-trend {
  color: #4caf50;
  display: flex;
  align-items: center;
}

.metric-trend svg {
  width: 16px;
  height: 16px;
}

/* 图表区域 */
.chart-section {
  margin-bottom: 18px;
}

.chart-card {
  background: #fff;
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  padding: 18px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.chart-header {
  margin-bottom: 20px;
}

.chart-header h3 {
  margin: 0 0 8px 0;
  font-size: 17px;
  color: #0f172a;
  font-weight: 600;
}

.chart-subtitle {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.contribution-chart {
  width: 100%;
  height: 300px;
}

/* 饼图区域 */
.pie-charts-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 14px;
}

.pie-chart-card {
  background: #fff;
  border: 1px solid #e3ebf5;
  border-radius: 12px;
  padding: 18px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.pie-chart {
  width: 100%;
  height: 300px;
}

.chart-legend {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-label {
  color: #475569;
}

.legend-value {
  color: #64748b;
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .welcome-section h2 {
    font-size: 20px;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .metric-card {
    padding: 16px;
  }

  .metric-icon {
    width: 48px;
    height: 48px;
  }

  .metric-icon svg {
    width: 24px;
    height: 24px;
  }

  .metric-value {
    font-size: 24px;
  }

  .metric-label {
    font-size: 12px;
  }

  .chart-card,
  .pie-chart-card {
    padding: 16px;
  }

  .chart-header h3 {
    font-size: 16px;
  }

  .chart-subtitle {
    font-size: 12px;
  }

  .pie-charts-section {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .contribution-chart,
  .pie-chart {
    height: 250px;
  }

  .chart-legend {
    gap: 8px;
  }

  .legend-item {
    font-size: 11px;
  }

  .legend-color {
    width: 10px;
    height: 10px;
  }
}

@media (max-width: 480px) {
  .welcome-section {
    margin-bottom: 16px;
  }

  .welcome-section h2 {
    font-size: 18px;
  }

  .metrics-grid {
    gap: 10px;
  }

  .metric-card {
    padding: 12px;
    gap: 12px;
  }

  .metric-icon {
    width: 40px;
    height: 40px;
  }

  .metric-icon svg {
    width: 20px;
    height: 20px;
  }

  .metric-value {
    font-size: 20px;
  }

  .contribution-chart,
  .pie-chart {
    height: 200px;
  }

  .chart-card,
  .pie-chart-card {
    padding: 12px;
  }
}
</style>
