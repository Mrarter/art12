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
            <el-radio-group v-model="chartType" size="small" @change="loadData">
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
                <el-avatar :src="getFullImageUrl(row.avatar)" :size="40" />
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
                <el-image :src="getFullImageUrl(row.image)" :preview-src-list="row.image ? [getFullImageUrl(row.image)] : []" fit="cover" style="width: 60px; height: 60px; border-radius: 4px;" />
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
                <el-avatar :src="getFullImageUrl(row.buyerAvatar)" :size="24" />
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
import request, { getFullImageUrl } from '@/api/request'
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
      type: chartType.value,
      page: pagination.page,
      size: pagination.size
    }
    if (selectedSession.value !== '') params.sessionId = selectedSession.value
    if (dateRange.value?.[0]) params.startDate = dateRange.value[0]
    if (dateRange.value?.[1]) params.endDate = dateRange.value[1]

    params.days = chartType.value === 'daily' ? 30 : chartType.value === 'weekly' ? 90 : 365
    const [statsRes, sessionRes, artistRes, dealRes, trendRes, sessionsRes] = await Promise.all([
      request.get('/auction/admin/stats', { params }),
      request.get('/auction/admin/session-rank', { params }),
      request.get('/auction/admin/artist-rank', { params }),
      request.get('/auction/admin/deals', { params }),
      request.get('/auction/admin/trend', { params }),
      request.get('/auction/sessions', { params: { page: 1, size: 100 } })
    ])

    Object.assign(stats, statsRes)
    sessionList.value = (sessionsRes.records || []).map(item => ({ id: item.id, name: item.title || item.name }))
    sessionRank.value = sessionRes || []
    artistRank.value = artistRes || []
    dealList.value = dealRes.records || []
    pagination.total = dealRes.total || 0

    nextTick(() => {
      initTrendChart(trendRes || [])
    })
  } catch (error) {
    console.error('加载数据失败', error)
    Object.assign(stats, { totalAmount: 0, totalLots: 0, unsoldLots: 0, soldRate: 0, sessionCount: 0, bidderCount: 0, activeBidders: 0, amountGrowth: 0 })
    sessionRank.value = []
    artistRank.value = []
    dealList.value = []
    pagination.total = 0
    nextTick(() => initTrendChart([]))
  } finally {
    loading.value = false
  }
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
  if (!dealList.value.length) return ElMessage.warning('暂无成交数据可导出')
  const escape = value => `"${String(value ?? '').replaceAll('"', '""')}"`
  const rows = [['订单号', '拍品', '艺术家', '所属专场', '成交价', '买家', '成交时间']]
  dealList.value.forEach(item => rows.push([item.dealNo, item.title, item.artistName, item.sessionName, item.dealPrice, item.buyerName, item.dealTime]))
  const csv = '\ufeff' + rows.map(row => row.map(escape).join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `拍卖成交报表-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
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
