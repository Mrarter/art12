<template>
  <view class="performance-page">
    <!-- 头部数据 -->
    <view class="header-card">
      <view class="total-section">
        <text class="total-label">累计业绩</text>
        <text class="total-value">¥{{ formatMoney(totalAmount) }}</text>
      </view>
      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value">{{ orderCount }}</text>
          <text class="stat-label">成交订单</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ customerCount }}</text>
          <text class="stat-label">成交客户</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">¥{{ formatMoney(avgAmount) }}</text>
          <text class="stat-label">平均单价</text>
        </view>
      </view>
    </view>

    <!-- 收益趋势 -->
    <view class="trend-card">
      <view class="card-header">
        <text class="card-title">收益趋势</text>
        <view class="period-selector">
          <view
            v-for="item in periodOptions"
            :key="item.value"
            :class="['period-btn', { active: selectedPeriod === item.value }]"
            @click="changePeriod(item.value)"
          >
            {{ item.label }}
          </view>
        </view>
      </view>
      <view class="trend-chart">
        <view class="simple-chart">
          <view
            v-for="(item, index) in trendData"
            :key="index"
            class="chart-bar-wrap"
          >
            <view
              class="chart-bar"
              :style="{ height: (item.value / maxTrendValue * 160) + 'rpx' }"
            ></view>
            <text class="chart-label">{{ item.label }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 分销达人榜 -->
    <view class="ranking-card">
      <view class="card-header">
        <text class="card-title">分销达人榜</text>
        <view class="more-link" @click="viewAllRanking">
          <text>查看全部</text>
          <u-icon name="arrow-right" size="12" color="#999"></u-icon>
        </view>
      </view>
      <view class="ranking-list">
        <view
          v-for="(item, index) in rankingList"
          :key="item.id"
          :class="['ranking-item', { 'top-three': index < 3 }]"
        >
          <view class="rank-badge" v-if="index < 3">
            {{ index + 1 }}
          </view>
          <text class="rank-number" v-else>{{ index + 1 }}</text>
          <image class="rank-avatar" :src="item.avatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
          <view class="rank-info">
            <text class="rank-name">{{ item.nickname }}</text>
            <text class="rank-count">{{ item.orderCount }}单</text>
          </view>
          <view class="rank-amount">
            <text class="amount">¥{{ formatMoney(item.amount) }}</text>
            <text class="label">收益</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 业绩明细 -->
    <view class="detail-card">
      <view class="card-header">
        <text class="card-title">业绩明细</text>
        <view class="filter-btn" @click="showFilter = true">
          <u-icon name="filter" size="14" color="#666"></u-icon>
          <text>筛选</text>
        </view>
      </view>
      <view class="detail-list">
        <view class="detail-item" v-for="item in detailList" :key="item.id">
          <view class="detail-info">
            <text class="detail-title">{{ item.title }}</text>
            <text class="detail-time">{{ item.createTime }}</text>
          </view>
          <view class="detail-right">
            <text class="detail-amount">+¥{{ formatMoney(item.amount) }}</text>
            <text class="detail-status" :class="'status-' + item.status">
              {{ getStatusText(item.status) }}
            </text>
          </view>
        </view>
        <view class="load-more" v-if="hasMore" @click="loadMore">
          <text>{{ loadingMore ? '加载中...' : '加载更多' }}</text>
        </view>
        <view class="empty-state" v-if="detailList.length === 0">
          <text>暂无业绩记录</text>
        </view>
      </view>
    </view>

    <!-- 筛选弹窗 -->
    <u-popup v-model="showFilter" mode="bottom" border-radius="16">
      <view class="filter-popup">
        <view class="popup-header">
          <text class="popup-title">筛选条件</text>
          <view class="close-btn" @click="showFilter = false">
            <u-icon name="close" size="20" color="#999"></u-icon>
          </view>
        </view>
        <view class="filter-content">
          <view class="filter-section">
            <text class="filter-label">时间范围</text>
            <view class="filter-options">
              <view
                v-for="item in timeOptions"
                :key="item.value"
                :class="['filter-option', { active: filterTime === item.value }]"
                @click="filterTime = item.value"
              >
                {{ item.label }}
              </view>
            </view>
          </view>
          <view class="filter-section">
            <text class="filter-label">订单类型</text>
            <view class="filter-options">
              <view
                v-for="item in typeOptions"
                :key="item.value"
                :class="['filter-option', { active: filterType === item.value }]"
                @click="filterType = item.value"
              >
                {{ item.label }}
              </view>
            </view>
          </view>
        </view>
        <view class="popup-footer">
          <view class="btn-reset" @click="resetFilter">重置</view>
          <view class="btn-confirm" @click="applyFilter">确定</view>
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script>
import { getEarningsTrend, getRankingList, getEarningsDetail } from '@/api/promoter.js'

export default {
  data() {
    return {
      totalAmount: 0,
      orderCount: 0,
      customerCount: 0,
      avgAmount: 0,

      selectedPeriod: 'week',
      periodOptions: [
        { label: '近7天', value: 'week' },
        { label: '近30天', value: 'month' },
        { label: '近90天', value: 'quarter' }
      ],
      trendData: [],
      maxTrendValue: 0,

      rankingList: [],
      detailList: [],
      page: 1,
      pageSize: 10,
      hasMore: false,
      loadingMore: false,

      showFilter: false,
      filterTime: 'all',
      filterType: 'all',
      timeOptions: [
        { label: '全部', value: 'all' },
        { label: '今天', value: 'today' },
        { label: '本周', value: 'week' },
        { label: '本月', value: 'month' },
        { label: '本年', value: 'year' }
      ],
      typeOptions: [
        { label: '全部', value: 'all' },
        { label: '一级佣金', value: 'level1' },
        { label: '二级佣金', value: 'level2' },
        { label: '团队奖励', value: 'team' }
      ]
    }
  },

  onLoad() {
    this.loadOverview()
    this.loadTrend()
    this.loadRanking()
    this.loadDetail()
  },

  methods: {
    async loadOverview() {
      // 模拟数据
      this.totalAmount = 128500
      this.orderCount = 156
      this.customerCount = 89
      this.avgAmount = this.orderCount > 0 ? Math.round(this.totalAmount / this.orderCount) : 0
    },

    async loadTrend() {
      // 模拟趋势数据
      const weekData = [
        { label: '周一', value: 12500 },
        { label: '周二', value: 15800 },
        { label: '周三', value: 8900 },
        { label: '周四', value: 21000 },
        { label: '周五', value: 18300 },
        { label: '周六', value: 25600 },
        { label: '周日', value: 19400 }
      ]
      const monthData = [
        { label: '第1周', value: 85000 },
        { label: '第2周', value: 96000 },
        { label: '第3周', value: 78000 },
        { label: '第4周', value: 112000 }
      ]
      const quarterData = [
        { label: '1月', value: 280000 },
        { label: '2月', value: 320000 },
        { label: '3月', value: 265000 }
      ]

      this.trendData = this.selectedPeriod === 'week' ? weekData :
                       this.selectedPeriod === 'month' ? monthData : quarterData
      this.maxTrendValue = Math.max(...this.trendData.map(item => item.value), 1)
    },

    async loadRanking() {
      // 模拟排行榜
      this.rankingList = [
        { id: 1, nickname: '艺术达人小王', avatar: '/static/avatar/demo1.jpg', orderCount: 289, amount: 358000 },
        { id: 2, nickname: '收藏家老李', avatar: '/static/avatar/demo2.jpg', orderCount: 245, amount: 298000 },
        { id: 3, nickname: '画室老板', avatar: '/static/avatar/demo3.jpg', orderCount: 198, amount: 245000 },
        { id: 4, nickname: '艺术爱好者', avatar: '/static/avatar/demo4.jpg', orderCount: 156, amount: 189000 },
        { id: 5, nickname: '品鉴师', avatar: '/static/avatar/demo5.jpg', orderCount: 134, amount: 165000 }
      ]
    },

    async loadDetail() {
      // 模拟明细
      this.detailList = [
        { id: 1, title: '山水长卷·云岭晴岚', amount: 3840, status: 'settled', createTime: '2024-04-21 14:30' },
        { id: 2, title: '荷花图·清韵', amount: 2640, status: 'settled', createTime: '2024-04-21 10:15' },
        { id: 3, title: '松鹤延年', amount: 5120, status: 'pending', createTime: '2024-04-20 16:45' },
        { id: 4, title: '书法对联', amount: 1280, status: 'settled', createTime: '2024-04-20 11:20' },
        { id: 5, title: '油画风景', amount: 7680, status: 'settled', createTime: '2024-04-19 20:30' }
      ]
      this.hasMore = true
    },

    changePeriod(period) {
      this.selectedPeriod = period
      this.loadTrend()
    },

    viewAllRanking() {
      uni.navigateTo({ url: '/pages/promoter/ranking' })
    },

    loadMore() {
      if (this.loadingMore || !this.hasMore) return
      this.loadingMore = true
      this.page++
      setTimeout(() => {
        this.loadingMore = false
        this.hasMore = false
      }, 1000)
    },

    resetFilter() {
      this.filterTime = 'all'
      this.filterType = 'all'
    },

    applyFilter() {
      this.showFilter = false
      this.page = 1
      this.loadDetail()
    },

    formatMoney(amount) {
      if (!amount) return '0.00'
      return amount.toLocaleString()
    },

    getStatusText(status) {
      const map = {
        settled: '已结算',
        pending: '待结算',
        cancelled: '已取消'
      }
      return map[status] || ''
    }
  }
}
</script>

<style lang="scss" scoped>
.performance-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20rpx;
  padding-bottom: 40rpx;
}

.header-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  margin-bottom: 20rpx;
  color: #fff;
}

.total-section {
  text-align: center;
  margin-bottom: 40rpx;
}

.total-label {
  font-size: 28rpx;
  opacity: 0.8;
}

.total-value {
  display: block;
  font-size: 56rpx;
  font-weight: 700;
  margin-top: 12rpx;
}

.stats-row {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.stat-label {
  font-size: 22rpx;
  opacity: 0.8;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.period-selector {
  display: flex;
  gap: 12rpx;
}

.period-btn {
  font-size: 22rpx;
  color: #999;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  background: #f5f5f5;

  &.active {
    background: #667eea;
    color: #fff;
  }
}

.trend-card {
  @extend .card;
}

.trend-chart {
  height: 200rpx;
}

.simple-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 200rpx;
  padding-top: 40rpx;
}

.chart-bar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.chart-bar {
  width: 48rpx;
  background: linear-gradient(180deg, #667eea 0%, #a78bfa 100%);
  border-radius: 8rpx 8rpx 0 0;
  margin-bottom: 12rpx;
  min-height: 20rpx;
}

.chart-label {
  font-size: 20rpx;
  color: #999;
}

.ranking-card {
  @extend .card;
}

.more-link {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #999;

  text {
    margin-right: 6rpx;
  }
}

.ranking-list {
  .ranking-item {
    display: flex;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    &.top-three {
      .rank-badge {
        background: linear-gradient(135deg, #f5a623, #f8b500);
        color: #fff;
        font-size: 24rpx;
        font-weight: 600;
      }
    }
  }
}

.rank-badge {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #666;
  margin-right: 16rpx;
}

.rank-number {
  width: 40rpx;
  text-align: center;
  font-size: 26rpx;
  color: #999;
  margin-right: 16rpx;
}

.rank-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  margin-right: 16rpx;
}

.rank-info {
  flex: 1;
}

.rank-name {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 6rpx;
}

.rank-count {
  font-size: 22rpx;
  color: #999;
}

.rank-amount {
  text-align: right;
}

.rank-amount .amount {
  display: block;
  font-size: 28rpx;
  color: #e74c3c;
  font-weight: 600;
  margin-bottom: 4rpx;
}

.rank-amount .label {
  font-size: 20rpx;
  color: #999;
}

.detail-card {
  @extend .card;
}

.filter-btn {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #666;
  padding: 8rpx 16rpx;
  background: #f5f5f5;
  border-radius: 20rpx;

  text {
    margin-left: 6rpx;
  }
}

.detail-list {
  .detail-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }
  }
}

.detail-info {
  flex: 1;
}

.detail-title {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.detail-time {
  font-size: 22rpx;
  color: #999;
}

.detail-right {
  text-align: right;
}

.detail-amount {
  display: block;
  font-size: 30rpx;
  color: #e74c3c;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.detail-status {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;

  &.status-settled {
    background: rgba(80, 200, 120, 0.1);
    color: #50c878;
  }

  &.status-pending {
    background: rgba(255, 152, 0, 0.1);
    color: #ff9800;
  }

  &.status-cancelled {
    background: rgba(153, 153, 153, 0.1);
    color: #999;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
}

.empty-state {
  text-align: center;
  padding: 60rpx;
  color: #ccc;
  font-size: 28rpx;
}

.filter-popup {
  padding: 30rpx;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}

.popup-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.close-btn {
  padding: 10rpx;
}

.filter-content {
  padding-bottom: 30rpx;
}

.filter-section {
  margin-bottom: 30rpx;
}

.filter-label {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 16rpx;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.filter-option {
  padding: 12rpx 28rpx;
  border-radius: 30rpx;
  font-size: 26rpx;
  background: #f5f5f5;
  color: #666;

  &.active {
    background: #667eea;
    color: #fff;
  }
}

.popup-footer {
  display: flex;
  gap: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f5f5f5;
}

.btn-reset, .btn-confirm {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 40rpx;
  font-size: 30rpx;
}

.btn-reset {
  background: #f5f5f5;
  color: #666;
}

.btn-confirm {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
}
</style>