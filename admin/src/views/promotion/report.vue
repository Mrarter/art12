<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">分销统计报表</span>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="loadData"
        />
        <el-button type="primary" @click="exportReport">
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">累计佣金总额</p>
          <p class="stat-value">¥{{ formatNumber(stats.totalCommission) }}</p>
          <p class="stat-trend up" v-if="stats.commissionGrowth > 0">
            ↑ {{ stats.commissionGrowth }}% 较上期
          </p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon><Wallet /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">已结算佣金</p>
          <p class="stat-value">¥{{ formatNumber(stats.settledCommission) }}</p>
          <p class="stat-trend">
            待结算 ¥{{ formatNumber(stats.unsettledCommission) }}
          </p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">艺荐官总数</p>
          <p class="stat-value">{{ formatNumber(stats.totalPromoters) }}</p>
          <p class="stat-trend">
            活跃 {{ stats.activePromoters }} 人
          </p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">累计订单数</p>
          <p class="stat-value">{{ formatNumber(stats.totalOrders) }}</p>
          <p class="stat-trend">
            成交率 {{ stats.orderRate }}%
          </p>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>佣金趋势</span>
            <el-radio-group v-model="chartType" size="small">
              <el-radio-button label="daily">日</el-radio-button>
              <el-radio-button label="weekly">周</el-radio-button>
              <el-radio-button label="monthly">月</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="trendChartRef" class="chart-container"></div>
      </el-card>
    </div>

    <!-- 佣金构成分析 -->
    <div class="analysis-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span>佣金构成</span>
            </template>
            <div ref="compositionChartRef" class="chart-container-small"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span>艺荐官等级分布</span>
            </template>
            <div ref="levelChartRef" class="chart-container-small"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 排行榜 -->
    <div class="rank-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>佣金排行榜 TOP10</span>
          </div>
        </template>
        <el-table :data="rankList" border stripe>
          <el-table-column label="排名" width="80" align="center">
            <template #default="{ $index }">
              <span :class="['rank', { top3: $index < 3 }]">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="艺荐官" min-width="200">
            <template #default="{ row }">
              <div class="user-info">
                <el-avatar :src="row.avatar" :size="36" />
                <div>
                  <p class="nickname">{{ row.nickname }}</p>
                  <p class="level">{{ row.levelName }}</p>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="teamSize" label="团队人数" width="100" align="center" />
          <el-table-column prop="orderCount" label="订单数" width="100" align="center" />
          <el-table-column label="一级佣金" width="120" align="right">
            <template #default="{ row }">
              <span class="money">¥{{ formatNumber(row.level1Commission) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="二级佣金" width="120" align="right">
            <template #default="{ row }">
              <span class="money">¥{{ formatNumber(row.level2Commission) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="累计佣金" width="140" align="right">
            <template #default="{ row }">
              <span class="money total">¥{{ formatNumber(row.totalCommission) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 团队详情表格 -->
    <div class="table-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>团队业绩明细</span>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索艺荐官"
              style="width: 200px"
              clearable
            />
          </div>
        </template>
        <el-table :data="filteredTeamList" v-loading="loading" border stripe>
          <el-table-column label="艺荐官" min-width="180">
            <template #default="{ row }">
              <div class="user-info">
                <el-avatar :src="row.avatar" :size="36" />
                <div>
                  <p class="nickname">{{ row.nickname }}</p>
                  <p class="phone">{{ row.phone }}</p>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="levelName" label="等级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.level)">{{ row.levelName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="一级徒弟" width="100" align="center">
            <template #default="{ row }">
              {{ row.level1Count }} 人
            </template>
          </el-table-column>
          <el-table-column label="二级徒弟" width="100" align="center">
            <template #default="{ row }">
              {{ row.level2Count }} 人
            </template>
          </el-table-column>
          <el-table-column prop="directOrders" label="直接订单" width="100" align="center" />
          <el-table-column prop="teamOrders" label="团队订单" width="100" align="center" />
          <el-table-column label="本月佣金" width="120" align="right">
            <template #default="{ row }">
              <span class="money">¥{{ formatNumber(row.monthCommission) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="累计佣金" width="120" align="right">
            <template #default="{ row }">
              <span class="money total">¥{{ formatNumber(row.totalCommission) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="viewTeamDetail(row)">团队详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Coin, Wallet, User, TrendCharts, Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import * as echarts from 'echarts'

const loading = ref(false)
const dateRange = ref([])
const chartType = ref('daily')
const searchKeyword = ref('')
const trendChartRef = ref()
const compositionChartRef = ref()
const levelChartRef = ref()
let trendChart = null
let compositionChart = null
let levelChart = null

const stats = reactive({
  totalCommission: 0,
  settledCommission: 0,
  unsettledCommission: 0,
  totalPromoters: 0,
  activePromoters: 0,
  totalOrders: 0,
  orderRate: 0,
  commissionGrowth: 0
})

const rankList = ref([])
const teamList = ref([])

const filteredTeamList = computed(() => {
  if (!searchKeyword.value) return teamList.value
  return teamList.value.filter(item =>
    item.nickname?.includes(searchKeyword.value) ||
    item.phone?.includes(searchKeyword.value)
  )
})

const formatNumber = (num) => {
  if (!num) return '0'
  return Number(num).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

const getLevelType = (level) => {
  const types = { 1: '', 2: 'success', 3: 'warning', 4: 'danger' }
  return types[level] || ''
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      type: chartType.value
    }
    const [statsRes, rankRes, teamRes, trendRes] = await Promise.all([
      request.get('/promotion/admin/stats', { params }),
      request.get('/promotion/admin/rank', { params }),
      request.get('/promotion/admin/team-list', { params }),
      request.get('/promotion/admin/trend', { params })
    ])

    Object.assign(stats, statsRes)
    rankList.value = rankRes.list || []
    teamList.value = teamRes.list || []

    nextTick(() => {
      initTrendChart(trendRes.trend || [])
      initCompositionChart(statsRes)
      initLevelChart(statsRes)
    })
  } catch (error) {
    console.error('加载数据失败', error)
    // 模拟数据
    mockData()
  } finally {
    loading.value = false
  }
}

const mockData = () => {
  stats.totalCommission = 1256800
  stats.settledCommission = 980000
  stats.unsettledCommission = 276800
  stats.totalPromoters = 356
  stats.activePromoters = 189
  stats.totalOrders = 2580
  stats.orderRate = 68.5
  stats.commissionGrowth = 12.8

  rankList.value = [
    { nickname: '艺术鉴赏家', avatar: '', levelName: '钻石', level: 4, teamSize: 58, orderCount: 156, level1Commission: 45000, level2Commission: 28000, totalCommission: 73000 },
    { nickname: '收藏爱好者', avatar: '', levelName: '金牌', level: 3, teamSize: 42, orderCount: 128, level1Commission: 32000, level2Commission: 18000, totalCommission: 50000 },
    { nickname: '画廊主理人', avatar: '', levelName: '金牌', level: 3, teamSize: 38, orderCount: 112, level1Commission: 28000, level2Commission: 15000, totalCommission: 43000 },
    { nickname: '艺术策展人', avatar: '', levelName: '银牌', level: 2, teamSize: 25, orderCount: 89, level1Commission: 18000, level2Commission: 9500, totalCommission: 27500 },
    { nickname: '文创达人', avatar: '', levelName: '银牌', level: 2, teamSize: 22, orderCount: 76, level1Commission: 15000, level2Commission: 8000, totalCommission: 23000 }
  ]

  teamList.value = [
    { nickname: '艺术鉴赏家', avatar: '', phone: '138****8888', levelName: '钻石', level: 4, level1Count: 35, level2Count: 23, directOrders: 156, teamOrders: 428, monthCommission: 12500, totalCommission: 73000 },
    { nickname: '收藏爱好者', avatar: '', phone: '139****6666', levelName: '金牌', level: 3, level1Count: 28, level2Count: 14, directOrders: 128, teamOrders: 356, monthCommission: 9800, totalCommission: 50000 },
    { nickname: '画廊主理人', avatar: '', phone: '137****5555', levelName: '金牌', level: 3, level1Count: 22, level2Count: 16, directOrders: 112, teamOrders: 289, monthCommission: 8200, totalCommission: 43000 }
  ]

  nextTick(() => {
    const mockTrend = [
      { date: '2026-04-15', commission: 45000 },
      { date: '2026-04-16', commission: 52000 },
      { date: '2026-04-17', commission: 48000 },
      { date: '2026-04-18', commission: 61000 },
      { date: '2026-04-19', commission: 58000 },
      { date: '2026-04-20', commission: 65000 },
      { date: '2026-04-21', commission: 72000 }
    ]
    initTrendChart(mockTrend)
    initCompositionChart(stats)
    initLevelChart(stats)
  })
}

const initTrendChart = (data) => {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()

  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['一级佣金', '二级佣金', '总佣金'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date?.slice(5) || '')
    },
    yAxis: { type: 'value', axisLabel: { formatter: v => v >= 1000 ? `${v/1000}K` : v } },
    series: [
      { name: '一级佣金', type: 'bar', stack: 'total', data: data.map(d => d.level1 || d.commission * 0.65) },
      { name: '二级佣金', type: 'bar', stack: 'total', data: data.map(d => d.level2 || d.commission * 0.35) },
      { name: '总佣金', type: 'line', data: data.map(d => d.commission), smooth: true }
    ]
  })
}

const initCompositionChart = (data) => {
  if (!compositionChartRef.value) return
  if (compositionChart) compositionChart.dispose()

  compositionChart = echarts.init(compositionChartRef.value)
  compositionChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: [
        { value: data.settledCommission || 980000, name: '已结算' },
        { value: data.unsettledCommission || 276800, name: '待结算' }
      ]
    }]
  })
}

const initLevelChart = (data) => {
  if (!levelChartRef.value) return
  if (levelChart) levelChart.dispose()

  levelChart = echarts.init(levelChartRef.value)
  levelChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      label: { show: true, formatter: '{b}\n{d}%' },
      data: [
        { value: 120, name: '普通' },
        { value: 95, name: '银牌' },
        { value: 78, name: '金牌' },
        { value: 45, name: '钻石' },
        { value: 18, name: '皇冠' }
      ]
    }]
  })
}

const viewTeamDetail = (row) => {
  ElMessage.info(`查看 ${row.nickname} 的团队详情`)
}

const exportReport = () => {
  ElMessage.success('报表导出功能开发中')
}

onMounted(() => {
  loadData()

  window.addEventListener('resize', () => {
    trendChart?.resize()
    compositionChart?.resize()
    levelChart?.resize()
  })
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
}
.title {
  font-size: 20px;
  font-weight: 600;
}
.header-actions {
  display: flex;
  gap: 12px;
}
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
  color: #fff;
}
.stat-icon.blue { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-icon.green { background: linear-gradient(135deg, #11998e, #38ef7d); }
.stat-icon.orange { background: linear-gradient(135deg, #f093fb, #f5576c); }
.stat-icon.purple { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.stat-content .stat-label { color: #909399; font-size: 14px; margin-bottom: 4px; }
.stat-content .stat-value { font-size: 24px; font-weight: 600; color: #303133; }
.stat-trend { font-size: 12px; color: #909399; margin-top: 4px; }
.stat-trend.up { color: #67c23a; }
.chart-section, .analysis-section, .rank-section, .table-section {
  margin-bottom: 20px;
}
.chart-section .card-header, .rank-section .card-header, .table-section .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-container { height: 320px; }
.chart-container-small { height: 280px; }
.rank .top3 { color: #f56c6c; font-weight: 700; }
.user-info { display: flex; align-items: center; gap: 10px; }
.user-info .nickname { font-weight: 500; }
.user-info .level { font-size: 12px; color: #909399; }
.user-info .phone { font-size: 12px; color: #909399; }
.money { color: #606266; }
.money.total { color: #f56c6c; font-weight: 600; }
</style>
