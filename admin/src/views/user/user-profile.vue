<template>
  <view class="user-profile-container">
    <!-- 页面标题 -->
    <view class="page-header">
      <h2>用户画像分析</h2>
      <view class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="small"
          value-format="YYYY-MM-DD"
          @change="loadData"
        />
        <el-button type="primary" size="small" @click="loadData">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
      </view>
    </view>

    <!-- 核心指标卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon total-users">
            <i class="el-icon-user"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">总用户数</div>
            <div class="stat-trend" :class="stats.userGrowth >= 0 ? 'up' : 'down'">
              <i :class="stats.userGrowth >= 0 ? 'el-icon-top' : 'el-icon-bottom'"></i>
              {{ Math.abs(stats.userGrowth) }}% 较上期
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon active-users">
            <i class="el-icon-s-data"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.activeUsers }}</div>
            <div class="stat-label">活跃用户</div>
            <div class="stat-trend up">
              <i class="el-icon-top"></i>
              {{ Math.abs(stats.activeRate) }}% 活跃率
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon artists">
            <i class="el-icon-s-custom"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.artistCount }}</div>
            <div class="stat-label">艺术家数量</div>
            <div class="stat-trend">
              认证率 {{ stats.artistRate }}%
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon promoters">
            <i class="el-icon-s-marketing"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.promoterCount }}</div>
            <div class="stat-label">艺荐官数量</div>
            <div class="stat-trend">
              {{ stats.promoterActive }} 位活跃
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户分布图表 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <div slot="header">
            <span>用户身份分布</span>
            <el-radio-group v-model="identityType" size="small" class="chart-type-selector">
              <el-radio-button label="pie">饼图</el-radio-button>
              <el-radio-button label="bar">柱状图</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="identityChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <div slot="header">
            <span>用户年龄分布</span>
            <el-select v-model="ageRangeType" size="small" style="width: 120px;">
              <el-option label="全部" value="all" />
              <el-option label="收藏家" value="collector" />
              <el-option label="艺术家" value="artist" />
            </el-select>
          </div>
          <div ref="ageChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户地域和消费分析 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <div slot="header">
            <span>用户地域分布 TOP10</span>
          </div>
          <div ref="regionChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <div slot="header">
            <span>用户消费能力分布</span>
          </div>
          <div ref="consumptionChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户行为分析 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card class="chart-card">
          <div slot="header">
            <span>用户活跃趋势（近30天）</span>
            <el-radio-group v-model="activeType" size="small">
              <el-radio-button label="daily">日</el-radio-button>
              <el-radio-button label="weekly">周</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="activeTrendChart" class="chart-container-large"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <div slot="header">
            <span>用户行为分析</span>
          </div>
          <div class="behavior-list">
            <div class="behavior-item" v-for="item in behaviorData" :key="item.action">
              <view class="behavior-icon" :style="{ background: item.color }">
                <i :class="item.icon"></i>
              </view>
              <view class="behavior-info">
                <text class="behavior-name">{{ item.action }}</text>
                <text class="behavior-count">{{ item.count }}次</text>
              </view>
              <view class="behavior-bar">
                <view class="bar-fill" :style="{ width: item.percent + '%', background: item.color }"></view>
              </view>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户等级分布 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <div slot="header">
            <span>艺荐官等级分布</span>
          </div>
          <div ref="promoterLevelChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <div slot="header">
            <span>用户留存率</span>
          </div>
          <div ref="retentionChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户明细列表 -->
    <el-card class="user-list-card">
      <div slot="header">
        <span>高价值用户 TOP50</span>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名/手机号"
          size="small"
          style="width: 200px; margin-left: 20px;"
          @keyup.enter.native="searchUsers"
        >
          <i slot="prefix" class="el-input__icon el-icon-search"></i>
        </el-input>
      </div>
      <el-table :data="userList" stripe border>
        <el-table-column prop="id" label="用户ID" width="100" />
        <el-table-column label="用户信息" min-width="180">
          <template slot-scope="scope">
            <div class="user-cell">
              <el-avatar :size="36" :src="scope.row.avatar">
                <i class="el-icon-user-solid"></i>
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ scope.row.nickname }}</div>
                <div class="user-phone">{{ scope.row.phone }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="身份" width="100">
          <template slot-scope="scope">
            <el-tag :type="getIdentityTagType(scope.row.identity)" size="small">
              {{ scope.row.identityText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="累计消费" width="120">
          <template slot-scope="scope">
            <span class="money">¥{{ scope.row.totalAmount.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column prop="followCount" label="关注数" width="100" />
        <el-table-column prop="fansCount" label="粉丝数" width="100" />
        <el-table-column label="活跃度" width="150">
          <template slot-scope="scope">
            <el-progress :percentage="scope.row.activity" :color="getActivityColor(scope.row.activity)" />
          </template>
        </el-table-column>
        <el-table-column label="用户等级" width="100">
          <template slot-scope="scope">
            <el-rate v-model="scope.row.level" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="viewUserDetail(scope.row)">详情</el-button>
            <el-button type="text" size="small" @click="sendMessage(scope.row)">发消息</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 用户详情弹窗 -->
    <el-dialog title="用户详情" :visible.sync="showUserDetail" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户ID">{{ userDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userDetail.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ userDetail.phone }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ userDetail.registerTime }}</el-descriptions-item>
        <el-descriptions-item label="身份">
          <el-tag :type="getIdentityTagType(userDetail.identity)" size="small">
            {{ userDetail.identityText }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="用户等级">
          <el-rate v-model="userDetail.level" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="累计消费">
          <span class="money">¥{{ userDetail.totalAmount?.toLocaleString() || 0 }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="订单数">{{ userDetail.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="关注数">{{ userDetail.followCount }}</el-descriptions-item>
        <el-descriptions-item label="粉丝数">{{ userDetail.fansCount }}</el-descriptions-item>
        <el-descriptions-item label="活跃度">
          <el-progress :percentage="userDetail.activity || 0" />
        </el-descriptions-item>
        <el-descriptions-item label="最近活跃">{{ userDetail.lastActiveTime }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer">
        <el-button @click="showUserDetail = false">关闭</el-button>
        <el-button type="primary" @click="sendMessage(userDetail)">发送消息</el-button>
      </div>
    </el-dialog>
  </view>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'UserProfile',
  data() {
    return {
      dateRange: [],
      searchKeyword: '',
      identityType: 'pie',
      ageRangeType: 'all',
      activeType: 'daily',
      stats: {
        totalUsers: 0,
        userGrowth: 0,
        activeUsers: 0,
        activeRate: 0,
        artistCount: 0,
        artistRate: 0,
        promoterCount: 0,
        promoterActive: 0
      },
      userList: [],
      behaviorData: [
        { action: '浏览作品', count: 15680, percent: 100, icon: 'el-icon-view', color: '#409EFF' },
        { action: '收藏作品', count: 8920, percent: 57, icon: 'el-icon-star-off', color: '#67C23A' },
        { action: '加入购物车', count: 5630, percent: 36, icon: 'el-icon-shopping-cart-2', color: '#E6A23C' },
        { action: '分享作品', count: 3210, percent: 20, icon: 'el-icon-share', color: '#F56C6C' },
        { action: '参与拍卖', count: 2150, percent: 14, icon: 'el-icon-trophy', color: '#909399' },
        { action: '发帖互动', count: 1280, percent: 8, icon: 'el-icon-chat-line-square', color: '#8E44AD' }
      ],
      showUserDetail: false,
      userDetail: {},
      charts: {}
    }
  },
  mounted() {
    this.initDateRange()
    this.loadData()
    this.$nextTick(() => {
      this.initCharts()
    })
  },
  methods: {
    initDateRange() {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 30 * 24 * 60 * 60 * 1000)
      this.dateRange = [this.formatDate(start), this.formatDate(end)]
    },
    
    formatDate(date) {
      return date.toISOString().split('T')[0]
    },
    
    loadData() {
      this.loadStats()
      this.loadUserList()
      this.updateCharts()
    },
    
    loadStats() {
      // 模拟数据
      this.stats = {
        totalUsers: 15680,
        userGrowth: 12.5,
        activeUsers: 8932,
        activeRate: 56.9,
        artistCount: 1256,
        artistRate: 8.0,
        promoterCount: 892,
        promoterActive: 456
      }
    },
    
    loadUserList() {
      // 模拟高价值用户数据
      this.userList = [
        {
          id: 1001,
          nickname: '艺术品收藏家',
          phone: '138****8888',
          avatar: '',
          identity: 'collector',
          identityText: '收藏家',
          totalAmount: 568000,
          orderCount: 45,
          followCount: 128,
          fansCount: 356,
          activity: 95,
          level: 5,
          registerTime: '2024-01-15',
          lastActiveTime: '2026-04-21'
        },
        {
          id: 1002,
          nickname: '艺术投资者',
          phone: '139****6666',
          avatar: '',
          identity: 'collector',
          identityText: '收藏家',
          totalAmount: 389000,
          orderCount: 32,
          followCount: 89,
          fansCount: 234,
          activity: 88,
          level: 4,
          registerTime: '2024-02-20',
          lastActiveTime: '2026-04-20'
        },
        {
          id: 1003,
          nickname: '当代艺术爱好者',
          phone: '137****5555',
          avatar: '',
          identity: 'collector',
          identityText: '收藏家',
          totalAmount: 256000,
          orderCount: 28,
          followCount: 156,
          fansCount: 178,
          activity: 82,
          level: 4,
          registerTime: '2024-03-10',
          lastActiveTime: '2026-04-21'
        },
        {
          id: 1004,
          nickname: '艺术家李明',
          phone: '136****4444',
          avatar: '',
          identity: 'artist',
          identityText: '艺术家',
          totalAmount: 189000,
          orderCount: 15,
          followCount: 234,
          fansCount: 1256,
          activity: 90,
          level: 5,
          registerTime: '2024-01-05',
          lastActiveTime: '2026-04-21'
        },
        {
          id: 1005,
          nickname: '艺术推广者王芳',
          phone: '135****3333',
          avatar: '',
          identity: 'promoter',
          identityText: '艺荐官',
          totalAmount: 98000,
          orderCount: 56,
          followCount: 345,
          fansCount: 567,
          activity: 95,
          level: 5,
          registerTime: '2024-02-01',
          lastActiveTime: '2026-04-21'
        }
      ]
    },
    
    searchUsers() {
      this.loadUserList()
    },
    
    viewUserDetail(row) {
      this.userDetail = { ...row }
      this.showUserDetail = true
    },
    
    sendMessage(user) {
      this.$prompt('请输入消息内容', `发送消息给 ${user.nickname}`, {
        confirmButtonText: '发送',
        cancelButtonText: '取消',
        inputType: 'textarea'
      }).then(({ value }) => {
        this.$message.success('消息已发送')
      }).catch(() => {})
    },
    
    getIdentityTagType(identity) {
      const types = {
        collector: '',
        artist: 'success',
        promoter: 'warning',
        admin: 'danger'
      }
      return types[identity] || ''
    },
    
    getActivityColor(percentage) {
      if (percentage >= 80) return '#67C23A'
      if (percentage >= 50) return '#E6A23C'
      return '#F56C6C'
    },
    
    initCharts() {
      this.initIdentityChart()
      this.initAgeChart()
      this.initRegionChart()
      this.initConsumptionChart()
      this.initActiveTrendChart()
      this.initPromoterLevelChart()
      this.initRetentionChart()
    },
    
    initIdentityChart() {
      const chartDom = this.$refs.identityChart
      if (!chartDom) return
      this.charts.identityChart = echarts.init(chartDom)
      this.updateIdentityChart()
    },
    
    updateIdentityChart() {
      if (!this.charts.identityChart) return
      
      const option = this.identityType === 'pie' ? {
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { orient: 'vertical', right: 10, top: 'center' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['40%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: false },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: 'bold' }
          },
          data: [
            { value: 10480, name: '收藏家', itemStyle: { color: '#409EFF' } },
            { value: 1256, name: '艺术家', itemStyle: { color: '#67C23A' } },
            { value: 892, name: '艺荐官', itemStyle: { color: '#E6A23C' } },
            { value: 3052, name: '普通用户', itemStyle: { color: '#909399' } }
          ]
        }]
      } : {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: ['收藏家', '艺术家', '艺荐官', '普通用户'] },
        yAxis: { type: 'value' },
        series: [{
          type: 'bar',
          data: [
            { value: 10480, itemStyle: { color: '#409EFF' } },
            { value: 1256, itemStyle: { color: '#67C23A' } },
            { value: 892, itemStyle: { color: '#E6A23C' } },
            { value: 3052, itemStyle: { color: '#909399' } }
          ],
          barWidth: '50%',
          itemStyle: { borderRadius: [8, 8, 0, 0] }
        }]
      }
      
      this.charts.identityChart.setOption(option)
    },
    
    initAgeChart() {
      const chartDom = this.$refs.ageChart
      if (!chartDom) return
      this.charts.ageChart = echarts.init(chartDom)
      this.updateAgeChart()
    },
    
    updateAgeChart() {
      if (!this.charts.ageChart) return
      
      const option = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['18-25岁', '26-35岁', '36-45岁', '46-55岁', '55岁以上'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: ['收藏家', '艺术家', '艺荐官'] },
        yAxis: { type: 'value' },
        series: [
          { name: '18-25岁', type: 'bar', stack: 'total', data: [1200, 280, 85], itemStyle: { color: '#91D5FF' } },
          { name: '26-35岁', type: 'bar', stack: 'total', data: [3800, 520, 320], itemStyle: { color: '#69C0FF' } },
          { name: '36-45岁', type: 'bar', stack: 'total', data: [2800, 320, 280], itemStyle: { color: '#40A9FF' } },
          { name: '46-55岁', type: 'bar', stack: 'total', data: [1800, 100, 150], itemStyle: { color: '#1890FF' } },
          { name: '55岁以上', type: 'bar', stack: 'total', data: [880, 36, 57], itemStyle: { color: '#096DD9' } }
        ]
      }
      
      this.charts.ageChart.setOption(option)
    },
    
    initRegionChart() {
      const chartDom = this.$refs.regionChart
      if (!chartDom) return
      this.charts.regionChart = echarts.init(chartDom)
      this.updateRegionChart()
    },
    
    updateRegionChart() {
      if (!this.charts.regionChart) return
      
      const option = {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'value' },
        yAxis: { type: 'category', data: ['广东', '北京', '上海', '浙江', '江苏', '四川', '湖北', '山东', '福建', '湖南'] },
        series: [{
          type: 'bar',
          data: [2456, 2134, 1987, 1568, 1320, 1120, 980, 876, 756, 645],
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#83BFF6' },
              { offset: 1, color: '#188DF0' }
            ]),
            borderRadius: [0, 4, 4, 0]
          },
          barWidth: '60%'
        }]
      }
      
      this.charts.regionChart.setOption(option)
    },
    
    initConsumptionChart() {
      const chartDom = this.$refs.consumptionChart
      if (!chartDom) return
      this.charts.consumptionChart = echarts.init(chartDom)
      this.updateConsumptionChart()
    },
    
    updateConsumptionChart() {
      if (!this.charts.consumptionChart) return
      
      const option = {
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie',
          radius: '65%',
          center: ['50%', '50%'],
          data: [
            { value: 456, name: '高消费(>10万)', itemStyle: { color: '#F56C6C' } },
            { value: 1234, name: '中消费(1-10万)', itemStyle: { color: '#E6A23C' } },
            { value: 3456, name: '低消费(<1万)', itemStyle: { color: '#67C23A' } },
            { value: 8534, name: '未消费', itemStyle: { color: '#909399' } }
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          },
          label: {
            formatter: '{b}\n{d}%'
          }
        }]
      }
      
      this.charts.consumptionChart.setOption(option)
    },
    
    initActiveTrendChart() {
      const chartDom = this.$refs.activeTrendChart
      if (!chartDom) return
      this.charts.activeTrendChart = echarts.init(chartDom)
      this.updateActiveTrendChart()
    },
    
    updateActiveTrendChart() {
      if (!this.charts.activeTrendChart) return
      
      const days = []
      const dau = []
      const wau = []
      
      for (let i = 30; i >= 0; i--) {
        const date = new Date()
        date.setDate(date.getDate() - i)
        days.push(date.toISOString().split('T')[0].slice(5))
        dau.push(Math.floor(Math.random() * 2000) + 6000)
      }
      
      for (let i = 4; i >= 0; i--) {
        wau.push(Math.floor(Math.random() * 5000) + 25000)
      }
      
      const option = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['DAU', 'WAU'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: days
        },
        yAxis: { type: 'value' },
        series: [
          {
            name: 'DAU',
            type: 'line',
            smooth: true,
            data: dau,
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
                { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
              ])
            },
            lineStyle: { color: '#409EFF', width: 2 },
            itemStyle: { color: '#409EFF' }
          },
          {
            name: 'WAU',
            type: 'line',
            smooth: true,
            data: wau,
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
                { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
              ])
            },
            lineStyle: { color: '#67C23A', width: 2 },
            itemStyle: { color: '#67C23A' }
          }
        ]
      }
      
      this.charts.activeTrendChart.setOption(option)
    },
    
    initPromoterLevelChart() {
      const chartDom = this.$refs.promoterLevelChart
      if (!chartDom) return
      this.charts.promoterLevelChart = echarts.init(chartDom)
      this.updatePromoterLevelChart()
    },
    
    updatePromoterLevelChart() {
      if (!this.charts.promoterLevelChart) return
      
      const option = {
        tooltip: { trigger: 'axis' },
        radar: {
          indicator: [
            { name: '推广能力', max: 100 },
            { name: '订单转化', max: 100 },
            { name: '团队规模', max: 100 },
            { name: '佣金收入', max: 100 },
            { name: '活跃度', max: 100 }
          ],
          radius: '65%'
        },
        series: [{
          type: 'radar',
          data: [
            {
              value: [85, 78, 90, 82, 88],
              name: '皇冠艺荐官',
              areaStyle: { color: 'rgba(230, 162, 60, 0.3)' },
              lineStyle: { color: '#E6A23C' },
              itemStyle: { color: '#E6A23C' }
            },
            {
              value: [70, 65, 72, 68, 75],
              name: '钻石艺荐官',
              areaStyle: { color: 'rgba(144, 147, 153, 0.3)' },
              lineStyle: { color: '#909399' },
              itemStyle: { color: '#909399' }
            },
            {
              value: [55, 50, 58, 52, 60],
              name: '金牌艺荐官',
              areaStyle: { color: 'rgba(64, 158, 255, 0.3)' },
              lineStyle: { color: '#409EFF' },
              itemStyle: { color: '#409EFF' }
            }
          ]
        }]
      }
      
      this.charts.promoterLevelChart.setOption(option)
    },
    
    initRetentionChart() {
      const chartDom = this.$refs.retentionChart
      if (!chartDom) return
      this.charts.retentionChart = echarts.init(chartDom)
      this.updateRetentionChart()
    },
    
    updateRetentionChart() {
      if (!this.charts.retentionChart) return
      
      const option = {
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = params[0].name + '<br/>'
            params.forEach(param => {
              result += param.marker + param.seriesName + ': ' + param.value + '%<br/>'
            })
            return result
          }
        },
        legend: { data: ['次日留存', '7日留存', '30日留存'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: ['新增日', '第2天', '第3天', '第4天', '第5天', '第6天', '第7天'] },
        yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
        series: [
          { name: '次日留存', type: 'line', data: [100, 45, 38, 32, 28, 25, 23], smooth: true, lineStyle: { color: '#409EFF' }, itemStyle: { color: '#409EFF' } },
          { name: '7日留存', type: 'line', data: [100, 45, 38, 35, 33, 32, 30], smooth: true, lineStyle: { color: '#67C23A' }, itemStyle: { color: '#67C23A' } },
          { name: '30日留存', type: 'line', data: [100, 45, 38, 35, 32, 28, 25], smooth: true, lineStyle: { color: '#E6A23C' }, itemStyle: { color: '#E6A23C' } }
        ]
      }
      
      this.charts.retentionChart.setOption(option)
    },
    
    updateCharts() {
      this.updateIdentityChart()
      this.updateAgeChart()
      this.updateRegionChart()
      this.updateConsumptionChart()
      this.updateActiveTrendChart()
      this.updatePromoterLevelChart()
      this.updateRetentionChart()
    }
  },
  
  watch: {
    identityType() {
      this.updateIdentityChart()
    },
    ageRangeType() {
      this.updateAgeChart()
    },
    activeType() {
      this.updateActiveTrendChart()
    }
  },
  
  beforeDestroy() {
    Object.values(this.charts).forEach(chart => {
      if (chart) chart.dispose()
    })
  }
}
</script>

<style scoped>
.user-profile-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
  margin-right: 16px;
}

.stat-icon.total-users { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.stat-icon.active-users { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.stat-icon.artists { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
.stat-icon.promoters { background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%); }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-trend {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.stat-trend.up {
  color: #67C23A;
}

.stat-trend.down {
  color: #F56C6C;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 380px;
}

.chart-type-selector {
  float: right;
}

.chart-container {
  height: 280px;
}

.chart-container-large {
  height: 280px;
}

.behavior-list {
  padding: 10px 0;
}

.behavior-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.behavior-item:last-child {
  border-bottom: none;
}

.behavior-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  margin-right: 12px;
}

.behavior-info {
  display: flex;
  flex-direction: column;
  width: 100px;
}

.behavior-name {
  font-size: 14px;
  color: #303133;
}

.behavior-count {
  font-size: 12px;
  color: #909399;
}

.behavior-bar {
  flex: 1;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  margin-left: 16px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}

.user-list-card {
  margin-top: 20px;
}

.user-cell {
  display: flex;
  align-items: center;
}

.user-info {
  margin-left: 12px;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.user-phone {
  font-size: 12px;
  color: #909399;
}

.money {
  color: #F56C6C;
  font-weight: 500;
}

::v-deep .el-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
