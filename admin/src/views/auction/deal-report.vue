<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">拍卖成交统计</span>
      <div class="header-actions">
        <el-select v-model="selectedSession" placeholder="选择专场" clearable @change="loadData">
          <el-option label="全部专场" value="" />
          <el-option v-for="s in sessionList" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
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
        <div class="stat-icon gold">
          <el-icon><Trophy /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">成交总额</p>
          <p class="stat-value">¥{{ formatNumber(stats.totalAmount) }}</p>
          <p class="stat-trend" :class="stats.amountGrowth >= 0 ? 'up' : 'down'">
            {{ stats.amountGrowth >= 0 ? '↑' : '↓' }} {{ Math.abs(stats.amountGrowth) }}% 较上期
          </p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon><Goods /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">成交件数</p>
          <p class="stat-value">{{ formatNumber(stats.totalLots) }} 件</p>
          <p class="stat-trend">
            流拍 {{ stats.unsoldLots }} 件
          </p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">成交率</p>
          <p class="stat-value">{{ stats.soldRate }}%</p>
          <p class="stat-trend">
            场次 {{ stats.sessionCount }} 场
          </p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">参与人数</p>
          <p class="stat-value">{{ formatNumber(stats.bidderCount) }}</p>
          <p class="stat-trend">
            活跃 {{ stats.activeBidders }} 人
          </p>
        </div>
      </div>
    </div>

    <!-- 趋势图表 -->
    <div class="chart-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>成交趋势</span>
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

    <!-- 专场排行榜 -->
    <div class="rank-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>专场成交排行榜</span>
          </div>
        </template>
        <el-table :data="sessionRank" border stripe>
          <el-table-column label="排名" width="80" align="center">
            <template #default="{ $index }">
              <span :class="['rank', { top3: $index < 3 }]">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sessionName" label="专场名称" min-width="180" />
          <el-table-column prop="startDate" label="拍卖日期" width="120" />
          <el-table-column label="上拍件数" width="100" align="center">
            <template #default="{ row }">
              {{ row.totalLots }} 件
            </template>
          </el-table-column>
          <el-table-column label="成交件数" width="100" align="center">
            <template #default="{ row }">
              <span class="success">{{ row.soldLots }} 件</span>
            </template>
          </el-table-column>
          <el-table-column label="成交率" width="100" align="center">
            <template #default="{ row }">
              <el-progress :percentage="row.soldRate" :color="getRateColor(row.soldRate)" />
            </template>
          </el-table-column>
          <el-table-column label="成交总额" width="140" align="right">
            <template #default="{ row }">
              <span class="money">¥{{ formatNumber(row.totalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="最高成交价" width="140" align="right">
            <template #default="{ row }">
              <span class="money highlight">¥{{ formatNumber(row.maxPrice) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 艺术家成交排行 -->
    <div class="rank-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>艺术家成交排行 TOP10</span>
          </div>
        </template>
        <el-table :data="artistRank" border stripe>
          <el-table-column label="排名" width="80" align="center">
            <template #default="{ $index }">
              <span :class="['rank', { top3: $index < 3 }]">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="艺术家" min-width="200">
            <template #default="{ row }">
              <div class="artist-info">
                <el-avatar :src="row.avatar" :size="40" />
                <div>
                  <p class="name">{{ row.artistName }}</p>
                  <p class="badge" v-if="row.badge">{{ row.badge }}</p>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="lotCount" label="上拍件数" width="100" align="center" />
          <el-table-column prop="soldCount" label="成交件数" width="100" align="center">
            <template #default="{ row }">
              <span class="success">{{ row.soldCount }} 件</span>
            </template>
          </el-table-column>
          <el-table-column label="成交率" width="100" align="center">
            <template #default="{ row }">
              <span>{{ row.soldRate }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="成交总额" width="140" align="right">
            <template #default="{ row }">
              <span class="money">¥{{ formatNumber(row.totalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="最高成交价" width="140" align="right">
            <template #default="{ row }">
              <span class="money highlight">¥{{ formatNumber(row.maxPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="viewArtistDetail(row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 成交明细列表 -->
    <div class="table-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>成交明细</span>
            <div class="header-actions">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索作品/艺术家"
                style="width: 200px"
                clearable
              />
              <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px">
                <el-option label="全部" value="" />
                <el-option label="待付款" value="pending" />
                <el-option label="已付款" value="paid" />
                <el-option label="已完成" value="completed" />
              </el-select>
            </div>
          </div>
        </template>
        <el-table :data="filteredDealList" v-loading="loading" border stripe>
          <el-table-column prop="dealNo" label="订单号" width="180" />
          <el-table-column label="拍品" min-width="200">
            <template #default="{ row }">
              <div class="lot-info">
                <el-image :src="row.image" :preview-src-list="[row.image]" fit="cover" style="width: 60px; height: 60px; border-radius: 4px;" />
                <div class="lot-detail">
                  <p class="lot-title">{{ row.title }}</p>
                  <p class="lot-artist">{{ row.artistName }}</p>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="sessionName" label="所属专场" width="140" />
          <el-table-column label="成交价" width="120" align="right">
            <template #default="{ row }">
              <span class="money">¥{{ formatNumber(row.dealPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="买家" width="120">
            <template #default="{ row }">
              <div class="user-info">
                <el-avatar :src="row.buyerAvatar" :size="24" />
                <span>{{ row.buyerName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="拍卖时间" width="160">
            <template #default="{ row }">
              {{ row.dealTime }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="viewDealDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadData"
            @current-change="loadData"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Trophy, Goods, CircleCheck, User, Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import * as echarts from 'echarts'

const loading = ref(false)
const selectedSession = ref('')
const dateRange = ref([])
const chartType = ref('daily')
const searchKeyword = ref('')
const statusFilter = ref('')
const trendChartRef = ref()
let trendChart = null

const stats = reactive({
  totalAmount: 0,
  totalLots: 0,
  unsoldLots: 0,
  soldRate: 0,
  sessionCount: 0,
  bidderCount: 0,
  activeBidders: 0,
  amountGrowth: 0
})

const sessionList = ref([])
const sessionRank = ref([])
const artistRank = ref([])
const dealList = ref([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const filteredDealList = computed(() => {
  let result = dealList.value
  if (searchKeyword.value) {
    result = result.filter(item =>
      item.title?.includes(searchKeyword.value) ||
      item.artistName?.includes(searchKeyword.value)
    )
  }
  if (statusFilter.value) {
    result = result.filter(item => item.status === statusFilter.value)
  }
  return result
})

const formatNumber = (num) => {
  if (!num) return '0'
  return Number(num).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

const getRateColor = (rate) => {
  if (rate >= 80) return '#67c23a'
  if (rate >= 50) return '#e6a23c'
  return '#f56c6c'
}

const getStatusType = (status) => {
  const types = { pending: 'warning', paid: 'success', completed: 'info' }
  return types[status] || ''
}

const getStatusText = (status) => {
  const texts = { pending: '待付款', paid: '已付款', completed: '已完成' }
  return texts[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      sessionId: selectedSession.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      type: chartType.value,
      page: pagination.page,
      size: pagination.size
    }

    const [statsRes, sessionRes, artistRes, dealRes, trendRes] = await Promise.all([
      request.get('/auction/admin/stats', { params }),
      request.get('/auction/admin/session-rank', { params }),
      request.get('/auction/admin/artist-rank', { params }),
      request.get('/auction/admin/deals', { params }),
      request.get('/auction/admin/trend', { params })
    ])

    Object.assign(stats, statsRes)
    sessionList.value = sessionRes.list || []
    sessionRank.value = sessionRes.rank || []
    artistRank.value = artistRes.list || []
    dealList.value = dealRes.list || []
    pagination.total = dealRes.total || 0

    nextTick(() => {
      initTrendChart(trendRes.list || [])
    })
  } catch (error) {
    console.error('加载数据失败', error)
    mockData()
  } finally {
    loading.value = false
  }
}

const mockData = () => {
  stats.totalAmount = 5680000
  stats.totalLots = 186
  stats.unsoldLots = 28
  stats.soldRate = 84.9
  stats.sessionCount = 12
  stats.bidderCount = 425
  stats.activeBidders = 318
  stats.amountGrowth = 15.6

  sessionRank.value = [
    { sessionName: '当代艺术精品专场', startDate: '2026-04-18', totalLots: 25, soldLots: 23, soldRate: 92, totalAmount: 1680000, maxPrice: 580000 },
    { sessionName: '瓷器玉器专场', startDate: '2026-04-15', totalLots: 32, soldLots: 28, soldRate: 87.5, totalAmount: 1250000, maxPrice: 380000 },
    { sessionName: '书画名家专场', startDate: '2026-04-12', totalLots: 28, soldLots: 24, soldRate: 85.7, totalAmount: 980000, maxPrice: 420000 },
    { sessionName: '紫砂壶艺专场', startDate: '2026-04-08', totalLots: 20, soldLots: 16, soldRate: 80, totalAmount: 720000, maxPrice: 280000 },
    { sessionName: '现当代油画专场', startDate: '2026-04-05', totalLots: 18, soldLots: 14, soldRate: 77.8, totalAmount: 650000, maxPrice: 350000 }
  ]

  artistRank.value = [
    { artistName: '张大千', avatar: '', badge: '大师级', lotCount: 8, soldCount: 8, soldRate: 100, totalAmount: 1280000, maxPrice: 580000 },
    { artistName: '齐白石', avatar: '', badge: '大师级', lotCount: 6, soldCount: 6, soldRate: 100, totalAmount: 980000, maxPrice: 420000 },
    { artistName: '徐悲鸿', avatar: '', badge: '大师级', lotCount: 5, soldCount: 5, soldRate: 100, totalAmount: 850000, maxPrice: 380000 },
    { artistName: '傅抱石', avatar: '', badge: '人气艺术家', lotCount: 4, soldCount: 4, soldRate: 100, totalAmount: 620000, maxPrice: 280000 },
    { artistName: '李可染', avatar: '', badge: '人气艺术家', lotCount: 5, soldCount: 4, soldRate: 80, totalAmount: 480000, maxPrice: 220000 }
  ]

  dealList.value = [
    { dealNo: 'AUC202604180001', title: '山水四条屏', artistName: '张大千', image: '', sessionName: '当代艺术精品专场', dealPrice: 580000, buyerName: '收藏家A', buyerAvatar: '', dealTime: '2026-04-18 21:30', status: 'completed' },
    { dealNo: 'AUC202604180002', title: '荷花图', artistName: '张大千', image: '', sessionName: '当代艺术精品专场', dealPrice: 420000, buyerName: '收藏家B', buyerAvatar: '', dealTime: '2026-04-18 21:45', status: 'paid' },
    { dealNo: 'AUC202604180003', title: '虾趣图', artistName: '齐白石', image: '', sessionName: '当代艺术精品专场', dealPrice: 380000, buyerName: '收藏家C', buyerAvatar: '', dealTime: '2026-04-18 22:00', status: 'pending' }
  ]
  pagination.total = dealList.value.length

  const mockTrend = [
    { date: '04-15', amount: 680000, count: 12 },
    { date: '04-16', amount: 820000, count: 15 },
    { date: '04-17', amount: 750000, count: 14 },
    { date: '04-18', amount: 980000, count: 18 },
    { date: '04-19', amount: 1250000, count: 22 },
    { date: '04-20', amount: 890000, count: 16 },
    { date: '04-21', amount: 1050000, count: 20 }
  ]
  nextTick(() => initTrendChart(mockTrend))
}

const initTrendChart = (data) => {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()

  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['成交额', '成交件数'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date)
    },
    yAxis: [
      { type: 'value', name: '金额', axisLabel: { formatter: v => v >= 1000000 ? `${(v/1000000).toFixed(1)}M` : v >= 1000 ? `${(v/1000).toFixed(0)}K` : v } },
      { type: 'value', name: '件数', axisLabel: { formatter: '{value} 件' } }
    ],
    series: [
      { name: '成交额', type: 'bar', data: data.map(d => d.amount), itemStyle: { color: '#409eff' } },
      { name: '成交件数', type: 'line', yAxisIndex: 1, data: data.map(d => d.count), smooth: true }
    ]
  })
}

const viewArtistDetail = (row) => {
  ElMessage.info(`查看艺术家 ${row.artistName} 的详细成交记录`)
}

const viewDealDetail = (row) => {
  ElMessage.info(`查看订单 ${row.dealNo} 的详情`)
}

const exportReport = () => {
  ElMessage.success('报表导出功能开发中')
}

onMounted(() => {
  loadData()

  window.addEventListener('resize', () => {
    trendChart?.resize()
  })
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.title { font-size: 20px; font-weight: 600; }
.header-actions { display: flex; gap: 12px; align-items: center; }
.stats-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px; display: flex; align-items: center; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-right: 16px; font-size: 24px; color: #fff; }
.stat-icon.gold { background: linear-gradient(135deg, #f6d365, #fda085); }
.stat-icon.blue { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-icon.green { background: linear-gradient(135deg, #11998e, #38ef7d); }
.stat-icon.purple { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.stat-content .stat-label { color: #909399; font-size: 14px; margin-bottom: 4px; }
.stat-content .stat-value { font-size: 24px; font-weight: 600; color: #303133; }
.stat-trend { font-size: 12px; color: #909399; margin-top: 4px; }
.stat-trend.up { color: #67c23a; }
.stat-trend.down { color: #f56c6c; }
.chart-section, .rank-section, .table-section { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.chart-container { height: 320px; }
.rank .top3 { color: #f56c6c; font-weight: 700; }
.artist-info { display: flex; align-items: center; gap: 10px; }
.artist-info .name { font-weight: 500; }
.artist-info .badge { font-size: 12px; color: #e6a23c; }
.lot-info { display: flex; align-items: center; gap: 10px; }
.lot-detail .lot-title { font-weight: 500; }
.lot-detail .lot-artist { font-size: 12px; color: #909399; }
.user-info { display: flex; align-items: center; gap: 6px; }
.money { color: #606266; }
.money.highlight { color: #f56c6c; font-weight: 600; }
.success { color: #67c23a; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
