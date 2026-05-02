<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card primary">
          <div class="stat-icon">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-content">
            <p class="stat-label">用户总数</p>
            <p class="stat-value">{{ stats.userCount }}</p>
            <p class="stat-trend">
              <span class="up">↑ {{ stats.userGrowth }}%</span>
              <span class="period">较上周</span>
            </p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card success">
          <div class="stat-icon">
            <el-icon><Goods /></el-icon>
          </div>
          <div class="stat-content">
            <p class="stat-label">作品总数</p>
            <p class="stat-value">{{ stats.productCount }}</p>
            <p class="stat-trend">
              <span class="up">↑ {{ stats.productGrowth }}%</span>
              <span class="period">较上周</span>
            </p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card warning">
          <div class="stat-icon">
            <el-icon><Ticket /></el-icon>
          </div>
          <div class="stat-content">
            <p class="stat-label">订单总数</p>
            <p class="stat-value">{{ stats.orderCount }}</p>
            <p class="stat-trend">
              <span class="up">↑ {{ stats.orderGrowth }}%</span>
              <span class="period">较上周</span>
            </p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card danger">
          <div class="stat-icon">
            <el-icon><Money /></el-icon>
          </div>
          <div class="stat-content">
            <p class="stat-label">成交金额</p>
            <p class="stat-value">¥{{ formatNumber(stats.amount) }}</p>
            <p class="stat-trend">
              <span class="up">↑ {{ stats.amountGrowth }}%</span>
              <span class="period">较上周</span>
            </p>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">数据趋势</span>
            <el-radio-group v-model="chartPeriod" size="small">
              <el-radio-button label="week">近7天</el-radio-button>
              <el-radio-button label="month">近30天</el-radio-button>
              <el-radio-button label="year">近一年</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="trendChartRef" class="chart-container"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">订单状态分布</span>
          </div>
          <div ref="pieChartRef" class="chart-container"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 待处理事项 -->
    <el-row :gutter="20" class="todo-row">
      <el-col :span="8">
        <div class="todo-card">
          <div class="todo-header">
            <span class="todo-title">待审核</span>
            <el-tag type="warning">{{ todoList.pendingArtist }} 待处理</el-tag>
          </div>
          <div class="todo-list">
            <div class="todo-item" @click="goPage('/user/artist?tab=pending')">
              <el-icon><User /></el-icon>
              <span>艺术家认证</span>
              <el-badge :value="todoList.pendingArtist" type="warning" />
            </div>
            <div class="todo-item" @click="goPage('/product/audit')">
              <el-icon><Picture /></el-icon>
              <span>作品审核</span>
              <el-badge :value="todoList.pendingProduct" type="warning" />
            </div>
            <div class="todo-item" @click="goPage('/auction/lot')">
              <el-icon><Tickets /></el-icon>
              <span>拍品审核</span>
              <el-badge :value="todoList.pendingLot" type="warning" />
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="todo-card">
          <div class="todo-header">
            <span class="todo-title">进行中的拍卖</span>
          </div>
          <div class="auction-list">
            <div class="auction-item" v-for="item in activeAuctions" :key="item.id">
              <div class="auction-info">
                <span class="auction-name">{{ item.name }}</span>
                <span class="auction-time">截至 {{ item.endTime }}</span>
              </div>
              <div class="auction-stat">
                <span class="count">{{ item.bidCount }}</span>
                <span class="label">出价</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="todo-card">
          <div class="todo-header">
            <span class="todo-title">最新订单</span>
            <el-button type="primary" link @click="goPage('/order/list')">查看全部</el-button>
          </div>
          <div class="order-list">
            <div class="order-item" v-for="item in recentOrders" :key="item.orderNo">
              <div class="order-info">
                <span class="order-no">{{ item.orderNo }}</span>
                <span class="order-amount">¥{{ item.amount }}</span>
              </div>
              <div class="order-status">
                <el-tag :type="getOrderStatusType(item.status)" size="small">
                  {{ getOrderStatusText(item.status) }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import request from '@/api/request'

const router = useRouter()

const stats = reactive({
  userCount: 0,
  userGrowth: 0,
  productCount: 0,
  productGrowth: 0,
  orderCount: 0,
  orderGrowth: 0,
  amount: 0,
  amountGrowth: 0
})

const chartPeriod = ref('week')

const trendChartRef = ref()
const pieChartRef = ref()

const todoList = reactive({
  pendingArtist: 0,
  pendingProduct: 0,
  pendingLot: 0
})

const activeAuctions = ref([])

const recentOrders = ref([])
const orderStatusData = ref([])
const trendData = ref([])

const formatNumber = (num) => {
  const value = Number(num || 0)
  if (value >= 10000) {
    return (value / 10000).toFixed(1) + '万'
  }
  return value.toLocaleString()
}

const getOrderStatusType = (status) => {
  const map = { pending: 'warning', paid: 'primary', shipped: 'info', completed: 'success', cancelled: 'info' }
  return map[status] || 'info'
}

const getOrderStatusText = (status) => {
  const map = { pending: '待付款', paid: '已付款', shipped: '已发货', completed: '已完成', cancelled: '已取消' }
  return map[status] || status
}

const goPage = (path) => {
  router.push(path)
}

const loadDashboard = async () => {
  const data = await request.get('/dashboard/stats')
  Object.assign(stats, {
    userCount: data.userCount || 0,
    productCount: data.productCount || data.artworkCount || 0,
    orderCount: data.orderCount || 0,
    amount: data.amount || data.totalSales || 0,
    userGrowth: data.userGrowth || 0,
    productGrowth: data.productGrowth || 0,
    orderGrowth: data.orderGrowth || 0,
    amountGrowth: data.amountGrowth || 0
  })
  Object.assign(todoList, data.todo || {})
  recentOrders.value = data.recentOrders || []
  orderStatusData.value = data.orderStatus || []
  trendData.value = data.trend || []
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  
  const chart = echarts.init(trendChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['订单数', '成交额'],
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
      boundaryGap: false,
      data: trendData.value.map(item => String(item.date).slice(5))
    },
    yAxis: [
      {
        type: 'value',
        name: '订单数',
        axisLabel: {
          formatter: '{value}'
        }
      },
      {
        type: 'value',
        name: '金额(万)',
        axisLabel: {
          formatter: '{value}'
        }
      }
    ],
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        data: trendData.value.map(item => Number(item.orderCount || 0)),
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(102, 126, 234, 0.5)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0.1)' }
          ])
        },
        lineStyle: { color: '#667eea' },
        itemStyle: { color: '#667eea' }
      },
      {
        name: '成交额',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: trendData.value.map(item => Number(item.amount || 0) / 10000),
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(118, 75, 162, 0.5)' },
            { offset: 1, color: 'rgba(118, 75, 162, 0.1)' }
          ])
        },
        lineStyle: { color: '#764ba2' },
        itemStyle: { color: '#764ba2' }
      }
    ]
  }
  chart.setOption(option)
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  
  const chart = echarts.init(pieChartRef.value)
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      itemWidth: 10,
      itemHeight: 10
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
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
        data: (orderStatusData.value.length ? orderStatusData.value : [{ value: 0, name: '暂无订单' }])
      }
    ]
  }
  chart.setOption(option)
}

onMounted(async () => {
  await loadDashboard().catch(() => {})
  nextTick(() => {
    initTrendChart()
    initPieChart()
  })
  
  window.addEventListener('resize', () => {
    if (trendChartRef.value) echarts.getInstanceByDom(trendChartRef.value)?.resize()
    if (pieChartRef.value) echarts.getInstanceByDom(pieChartRef.value)?.resize()
  })
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  padding: 20px;
  border-radius: 8px;
  color: #fff;
}

.stat-card.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-card.success {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-card.warning {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card.danger {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stat-icon {
  width: 60px;
  height: 60px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-right: 16px;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin: 0 0 8px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.stat-trend {
  font-size: 12px;
  opacity: 0.8;
}

.stat-trend .up {
  margin-right: 8px;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
}

.chart-container {
  height: 300px;
}

.todo-row {
  margin-bottom: 20px;
}

.todo-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  height: 100%;
}

.todo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.todo-title {
  font-size: 16px;
  font-weight: 600;
}

.todo-list {
  .todo-item {
    display: flex;
    align-items: center;
    padding: 12px;
    margin-bottom: 8px;
    background: #f5f6f8;
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.3s;
  }

  .todo-item:hover {
    background: #e8ecf1;
  }

  .todo-item span {
    flex: 1;
    margin-left: 12px;
    font-size: 14px;
  }
}

.auction-list {
  .auction-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;
  }

  .auction-item:last-child {
    border-bottom: none;
  }

  .auction-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .auction-name {
    font-size: 14px;
    color: #333;
  }

  .auction-time {
    font-size: 12px;
    color: #999;
  }

  .auction-stat {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .auction-stat .count {
    font-size: 18px;
    font-weight: 600;
    color: #667eea;
  }

  .auction-stat .label {
    font-size: 12px;
    color: #999;
  }
}

.order-list {
  .order-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;
  }

  .order-item:last-child {
    border-bottom: none;
  }

  .order-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .order-no {
    font-size: 13px;
    color: #666;
  }

  .order-amount {
    font-size: 14px;
    font-weight: 600;
    color: #f56c6c;
  }
}
</style>
