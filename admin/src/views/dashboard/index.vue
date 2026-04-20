<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="icon" style="background: #409eff">
            <el-icon><User /></el-icon>
          </div>
          <div class="info">
            <p class="value">{{ stats.userCount }}</p>
            <p class="label">用户总数</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="icon" style="background: #67c23a">
            <el-icon><Goods /></el-icon>
          </div>
          <div class="info">
            <p class="value">{{ stats.artworkCount }}</p>
            <p class="label">作品总数</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="icon" style="background: #e6a23c">
            <el-icon><ShoppingCart /></el-icon>
          </div>
          <div class="info">
            <p class="value">{{ stats.orderCount }}</p>
            <p class="label">订单总数</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="icon" style="background: #f56c6c">
            <el-icon><Coin /></el-icon>
          </div>
          <div class="info">
            <p class="value">¥{{ formatMoney(stats.totalSales) }}</p>
            <p class="label">总销售额</p>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="charts">
      <el-col :span="16">
        <div class="chart-card">
          <div class="title">销售趋势</div>
          <div ref="salesChartRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-card">
          <div class="title">作品分类分布</div>
          <div ref="categoryChartRef" class="chart"></div>
        </div>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="lists">
      <el-col :span="12">
        <div class="list-card">
          <div class="title">
            <span>待审核作品</span>
            <el-button type="primary" link @click="$router.push('/product/audit')">
              查看更多
            </el-button>
          </div>
          <el-table :data="pendingArtworks" style="width: 100%">
            <el-table-column prop="title" label="作品名称" />
            <el-table-column prop="artistName" label="艺术家" width="100" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag type="warning">待审核</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleAudit(row)">审核</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="list-card">
          <div class="title">
            <span>最新订单</span>
            <el-button type="primary" link @click="$router.push('/order/list')">
              查看更多
            </el-button>
          </div>
          <el-table :data="latestOrders" style="width: 100%">
            <el-table-column prop="orderNo" label="订单号" width="180" />
            <el-table-column prop="amount" label="金额" width="100">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getOrderStatusType(row.status)">
                  {{ getOrderStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const stats = reactive({
  userCount: 0,
  artworkCount: 0,
  orderCount: 0,
  totalSales: 0
})

const pendingArtworks = ref([])
const latestOrders = ref([])

const salesChartRef = ref()
const categoryChartRef = ref()

const formatMoney = (value) => {
  if (value >= 10000) {
    return (value / 10000).toFixed(2) + '万'
  }
  return value.toFixed(2)
}

const getOrderStatusType = (status) => {
  const map = { pending: 'warning', paid: 'success', shipped: 'primary', completed: 'success', cancelled: 'info' }
  return map[status] || 'info'
}

const getOrderStatusText = (status) => {
  const map = { pending: '待付款', paid: '已付款', shipped: '已发货', completed: '已完成', cancelled: '已取消' }
  return map[status] || status
}

const handleAudit = (row) => {
  // TODO: 跳转到审核页面
}

const initCharts = () => {
  // 销售趋势图
  const salesChart = echarts.init(salesChartRef.value)
  salesChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '订单数'] },
    xAxis: {
      type: 'category',
      data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
    },
    yAxis: [
      { type: 'value', name: '销售额' },
      { type: 'value', name: '订单数' }
    ],
    series: [
      { name: '销售额', type: 'bar', data: [8200, 9320, 9010, 12340, 12900, 13300, 15200] },
      { name: '订单数', type: 'line', yAxisIndex: 1, data: [12, 18, 15, 25, 28, 32, 38] }
    ]
  })
  
  // 分类分布图
  const categoryChart = echarts.init(categoryChartRef.value)
  categoryChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: 335, name: '国画' },
        { value: 234, name: '油画' },
        { value: 154, name: '书法' },
        { value: 135, name: '版画' },
        { value: 98, name: '雕塑' }
      ]
    }]
  })
}

onMounted(async () => {
  // 调用后端 API 获取统计数据
  try {
    const data = await request.get('/dashboard/stats')
    stats.userCount = data.userCount || 0
    stats.artworkCount = data.artworkCount || 0
    stats.orderCount = data.orderCount || 0
    stats.totalSales = data.totalSales || 0
  } catch (e) {
    // 使用模拟数据
    stats.userCount = 12580
    stats.artworkCount = 3560
    stats.orderCount = 8920
    stats.totalSales = 2568000
  }
  
  // 获取待审核作品
  try {
    const auditData = await request.get('/product/audit/list')
    pendingArtworks.value = (auditData.records || []).slice(0, 5)
  } catch (e) {
    pendingArtworks.value = [
      { id: 1, title: '山水长卷', artistName: '李明轩' },
      { id: 2, title: '花开富贵', artistName: '王芳' },
      { id: 3, title: '书法对联', artistName: '张强' }
    ]
  }
  
  // 获取最新订单
  try {
    const orderData = await request.get('/order/list', { params: { page: 1, size: 5 } })
    latestOrders.value = (orderData.records || []).map(item => ({
      id: item.id,
      orderNo: item.orderNo,
      amount: item.amount || item.totalAmount,
      status: item.status,
      createTime: item.createTime || item.createDate
    }))
  } catch (e) {
    latestOrders.value = [
      { id: 1, orderNo: 'ORD202310010001', amount: 1680, status: 'completed', createTime: '2023-10-15 14:30:22' },
      { id: 2, orderNo: 'ORD202310010002', amount: 3680, status: 'paid', createTime: '2023-10-15 15:20:11' },
      { id: 3, orderNo: 'ORD202310010003', amount: 880, status: 'shipped', createTime: '2023-10-15 16:45:33' },
      { id: 4, orderNo: 'ORD202310010004', amount: 5200, status: 'pending', createTime: '2023-10-15 17:10:05' }
    ]
  }
  
  initCharts()
})
</script>

<style scoped lang="scss">
.dashboard {
  .stat-cards {
    margin-bottom: 20px;
    
    .stat-card {
      background: #fff;
      padding: 20px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      gap: 20px;
      
      .icon {
        width: 60px;
        height: 60px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-size: 28px;
      }
      
      .info {
        .value {
          font-size: 24px;
          font-weight: bold;
          color: #333;
        }
        .label {
          font-size: 14px;
          color: #999;
          margin-top: 5px;
        }
      }
    }
  }
  
  .charts {
    margin-bottom: 20px;
    
    .chart-card {
      background: #fff;
      padding: 20px;
      border-radius: 4px;
      
      .title {
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 15px;
        padding-bottom: 10px;
        border-bottom: 1px solid #eee;
      }
      
      .chart {
        height: 300px;
      }
    }
  }
  
  .lists {
    .list-card {
      background: #fff;
      padding: 20px;
      border-radius: 4px;
      
      .title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 15px;
        padding-bottom: 10px;
        border-bottom: 1px solid #eee;
      }
    }
  }
}
</style>
