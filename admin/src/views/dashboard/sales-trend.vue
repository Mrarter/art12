<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">销售数据分析</span>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="default"
          @change="handleDateChange"
        />
        <el-select v-model="compareType" size="default" style="width: 120px" @change="loadData">
          <el-option label="不对比" value="none" />
          <el-option label="环比上周" value="week" />
          <el-option label="环比上月" value="month" />
          <el-option label="同比去年" value="year" />
        </el-select>
        <el-button type="primary" @click="loadData">刷新</el-button>
        <el-button @click="handleExport" :loading="exporting">导出报表</el-button>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="metrics-grid">
      <div class="metric-card">
        <div class="metric-header">
          <span class="metric-title">今日销售额</span>
          <span class="metric-trend" :class="metrics.todaySalesTrend > 0 ? 'up' : 'down'">
            {{ metrics.todaySalesTrend > 0 ? '↑' : '↓' }}
            {{ Math.abs(metrics.todaySalesTrend) }}%
          </span>
        </div>
        <div class="metric-value">¥{{ formatNumber(metrics.todaySales) }}</div>
        <div class="metric-compare">
          昨日：¥{{ formatNumber(metrics.yesterdaySales) }}
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-header">
          <span class="metric-title">今日订单数</span>
          <span class="metric-trend" :class="metrics.todayOrdersTrend > 0 ? 'up' : 'down'">
            {{ metrics.todayOrdersTrend > 0 ? '↑' : '↓' }}
            {{ Math.abs(metrics.todayOrdersTrend) }}%
          </span>
        </div>
        <div class="metric-value">{{ metrics.todayOrders }}</div>
        <div class="metric-compare">
          昨日：{{ metrics.yesterdayOrders }} 单
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-header">
          <span class="metric-title">客单价</span>
          <span class="metric-trend" :class="metrics.avgOrderTrend > 0 ? 'up' : 'down'">
            {{ metrics.avgOrderTrend > 0 ? '↑' : '↓' }}
            {{ Math.abs(metrics.avgOrderTrend) }}%
          </span>
        </div>
        <div class="metric-value">¥{{ formatNumber(metrics.avgOrderValue) }}</div>
        <div class="metric-compare">
          昨日：¥{{ formatNumber(metrics.yesterdayAvgOrder) }}
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-header">
          <span class="metric-title">支付转化率</span>
          <span class="metric-trend" :class="metrics.conversionTrend > 0 ? 'up' : 'down'">
            {{ metrics.conversionTrend > 0 ? '↑' : '↓' }}
            {{ Math.abs(metrics.conversionTrend) }}%
          </span>
        </div>
        <div class="metric-value">{{ metrics.conversionRate }}%</div>
        <div class="metric-compare">
          昨日：{{ metrics.yesterdayConversion }}%
        </div>
      </div>
    </div>

    <!-- 销售趋势图 -->
    <div class="chart-section">
      <div class="section-header">
        <span class="section-title">销售趋势</span>
        <div class="chart-tabs">
          <span 
            v-for="tab in chartTabs" 
            :key="tab.value"
            :class="['chart-tab', { active: chartType === tab.value }]"
            @click="switchChartType(tab.value)"
          >
            {{ tab.label }}
          </span>
        </div>
      </div>
      <div class="chart-container">
        <div ref="salesChartRef" class="chart"></div>
      </div>
    </div>

    <!-- 分类销售对比 -->
    <div class="charts-row">
      <div class="chart-section half">
        <div class="section-header">
          <span class="section-title">品类销售占比</span>
        </div>
        <div class="chart-container">
          <div ref="categoryChartRef" class="chart"></div>
        </div>
      </div>
      <div class="chart-section half">
        <div class="section-header">
          <span class="section-title">渠道销售对比</span>
        </div>
        <div class="chart-container">
          <div ref="channelChartRef" class="chart"></div>
        </div>
      </div>
    </div>

    <!-- 排行榜 -->
    <div class="rankings-row">
      <div class="ranking-section">
        <div class="section-header">
          <span class="section-title">热销作品 TOP10</span>
        </div>
        <el-table :data="topProducts" border stripe size="small">
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column label="作品" min-width="200">
            <template #default="{ row }">
              <div class="product-cell">
                <el-image :src="row.cover" class="product-cover" fit="cover" />
                <span class="product-name">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="sales" label="销量" width="100">
            <template #default="{ row }">
              <span class="sales-num">{{ row.sales }} 件</span>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="销售额" width="120">
            <template #default="{ row }">
              <span class="amount-num">¥{{ formatNumber(row.amount) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="ranking-section">
        <div class="section-header">
          <span class="section-title">活跃艺术家 TOP10</span>
        </div>
        <el-table :data="topArtists" border stripe size="small">
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column label="艺术家" min-width="200">
            <template #default="{ row }">
              <div class="artist-cell">
                <el-avatar :src="row.avatar" :size="32">{{ row.name?.charAt(0) }}</el-avatar>
                <span class="artist-name">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="worksCount" label="作品数" width="100" />
          <el-table-column prop="sales" label="销量" width="100" />
          <el-table-column prop="amount" label="销售额" width="120">
            <template #default="{ row }">
              <span class="amount-num">¥{{ formatNumber(row.amount) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const dateRange = ref([])
const compareType = ref('week')
const chartType = ref('day')
const exporting = ref(false)

const salesChartRef = ref()
const categoryChartRef = ref()
const channelChartRef = ref()

let salesChart = null
let categoryChart = null
let channelChart = null

const chartTabs = [
  { label: '按天', value: 'day' },
  { label: '按周', value: 'week' },
  { label: '按月', value: 'month' }
]

const metrics = reactive({
  todaySales: 125680,
  yesterdaySales: 98230,
  todaySalesTrend: 27.9,
  todayOrders: 156,
  yesterdayOrders: 124,
  todayOrdersTrend: 25.8,
  avgOrderValue: 805.6,
  yesterdayAvgOrder: 792.2,
  avgOrderTrend: 1.7,
  conversionRate: 68.5,
  yesterdayConversion: 65.2,
  conversionTrend: 5.1
})

const topProducts = ref([
  { id: 1, name: '山水长卷', cover: 'https://picsum.photos/200', sales: 25, amount: 3200000 },
  { id: 2, name: '荷花图', cover: 'https://picsum.photos/201', sales: 18, amount: 1584000 },
  { id: 3, name: '松云图', cover: 'https://picsum.photos/202', sales: 15, amount: 2340000 },
  { id: 4, name: '庐山图', cover: 'https://picsum.photos/203', sales: 12, amount: 3360000 },
  { id: 5, name: '梅花图', cover: 'https://picsum.photos/204', sales: 10, amount: 880000 }
])

const topArtists = ref([
  { id: 1, name: '张大千', avatar: '', worksCount: 128, sales: 156, amount: 18672000 },
  { id: 2, name: '齐白石', avatar: '', worksCount: 256, sales: 142, amount: 12456000 },
  { id: 3, name: '徐悲鸿', avatar: '', worksCount: 89, sales: 98, amount: 8624000 },
  { id: 4, name: '李可染', avatar: '', worksCount: 67, sales: 76, amount: 6840000 },
  { id: 5, name: '黄永玉', avatar: '', worksCount: 54, sales: 65, amount: 5850000 }
])

const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(2) + '万'
  }
  return num.toLocaleString()
}

const handleDateChange = (val) => {
  loadData()
}

const switchChartType = (type) => {
  chartType.value = type
  updateSalesChart()
}

const handleExport = async () => {
  exporting.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1500))
    ElMessage.success('报表导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const initSalesChart = () => {
  if (!salesChartRef.value) return
  
  salesChart = echarts.init(salesChartRef.value)
  updateSalesChart()
}

const updateSalesChart = () => {
  if (!salesChart) return

  const days = 30
  const labels = Array.from({ length: days }, (_, i) => {
    const date = new Date()
    date.setDate(date.getDate() - (days - 1 - i))
    return `${date.getMonth() + 1}/${date.getDate()}`
  })

  const salesData = Array.from({ length: days }, () => 
    Math.floor(50000 + Math.random() * 100000)
  )

  const orderData = Array.from({ length: days }, () => 
    Math.floor(50 + Math.random() * 150)
  )

  const compareData = Array.from({ length: days }, () => 
    Math.floor(40000 + Math.random() * 80000)
  )

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#333' }
    },
    legend: {
      data: ['销售额', '订单数'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: labels,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#666' }
    },
    yAxis: [
      {
        type: 'value',
        name: '销售额(元)',
        axisLine: { lineStyle: { color: '#667eea' } },
        axisLabel: {
          color: '#666',
          formatter: (val) => val >= 10000 ? (val / 10000) + '万' : val
        },
        splitLine: { lineStyle: { color: '#f5f7fa' } }
      },
      {
        type: 'value',
        name: '订单数',
        axisLine: { lineStyle: { color: '#50c878' } },
        axisLabel: { color: '#666' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        data: salesData,
        itemStyle: { color: '#667eea' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0.05)' }
          ])
        }
      },
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: orderData,
        itemStyle: { color: '#50c878' },
        lineStyle: { type: 'dashed' }
      }
    ]
  }

  salesChart.setOption(option)
}

const initCategoryChart = () => {
  if (!categoryChartRef.value) return

  categoryChart = echarts.init(categoryChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' }
        },
        labelLine: { show: false },
        data: [
          { value: 45, name: '国画', itemStyle: { color: '#667eea' } },
          { value: 25, name: '油画', itemStyle: { color: '#50c878' } },
          { value: 15, name: '版画', itemStyle: { color: '#ff9800' } },
          { value: 10, name: '书法', itemStyle: { color: '#e74c3c' } },
          { value: 5, name: '雕塑', itemStyle: { color: '#9c27b0' } }
        ]
      }
    ]
  }

  categoryChart.setOption(option)
}

const initChannelChart = () => {
  if (!channelChartRef.value) return

  channelChart = echarts.init(channelChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['小程序', 'App', 'PC网页', 'H5', '公众号'],
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#666' }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#666',
        formatter: (val) => val >= 10000 ? (val / 10000) + '万' : val
      },
      splitLine: { lineStyle: { color: '#f5f7fa' } }
    },
    series: [
      {
        name: '销售额',
        type: 'bar',
        data: [856800, 524300, 389600, 245800, 156200],
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#764ba2' }
          ])
        },
        barWidth: '40%'
      }
    ]
  }

  channelChart.setOption(option)
}

const loadData = async () => {
  // 模拟加载数据
  await new Promise(resolve => setTimeout(resolve, 500))
  updateSalesChart()
}

const handleResize = () => {
  salesChart?.resize()
  categoryChart?.resize()
  channelChart?.resize()
}

onMounted(() => {
  nextTick(() => {
    initSalesChart()
    initCategoryChart()
    initChannelChart()
  })
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  salesChart?.dispose()
  categoryChart?.dispose()
  channelChart?.dispose()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}
.metric-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.metric-title {
  font-size: 14px;
  color: #666;
}
.metric-trend {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}
.metric-trend.up {
  background: rgba(80, 200, 120, 0.1);
  color: #50c878;
}
.metric-trend.down {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
}
.metric-value {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.metric-compare {
  font-size: 12px;
  color: #999;
}

.chart-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.chart-tabs {
  display: flex;
  gap: 8px;
}
.chart-tab {
  padding: 6px 16px;
  font-size: 13px;
  color: #666;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}
.chart-tab:hover {
  background: #ecf5ff;
  color: #409eff;
}
.chart-tab.active {
  background: #667eea;
  color: #fff;
}
.chart-container {
  height: 350px;
}
.chart {
  width: 100%;
  height: 100%;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.chart-section.half {
  margin-bottom: 0;
}

.rankings-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.ranking-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.product-cover {
  width: 40px;
  height: 40px;
  border-radius: 4px;
}
.product-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sales-num {
  color: #409eff;
}
.amount-num {
  color: #e74c3c;
  font-weight: 600;
}

.artist-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.artist-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1200px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row,
  .rankings-row {
    grid-template-columns: 1fr;
  }
}
</style>
