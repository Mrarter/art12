<template>
  <view class="performance-page">
    <!-- 顶部装饰 -->
    <view class="page-decoration">
      <view class="deco-circle deco-circle-1"></view>
      <view class="deco-circle deco-circle-2"></view>
    </view>

    <!-- 头部数据卡片 -->
    <view class="header-card">
      <view class="total-section">
        <text class="total-label">累计业绩</text>
        <view class="total-value-wrap">
          <text class="total-symbol">¥</text>
          <text class="total-value">{{ formatMoney(totalAmount) }}</text>
        </view>
      </view>
      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value">{{ orderCount }}</text>
          <text class="stat-label">成交订单</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ customerCount }}</text>
          <text class="stat-label">成交客户</text>
        </view>
        <view class="stat-divider"></view>
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
          <u-icon name="arrow-right" size="12" color="#666"></u-icon>
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
          <u-icon name="filter" size="14" color="#c9a227"></u-icon>
          <text>筛选</text>
        </view>
      </view>
      <view class="detail-list">
        <view class="detail-item" v-for="item in detailList" :key="item.id">
          <view class="detail-left">
            <text class="detail-title">{{ item.title }}</text>
            <text class="detail-time">{{ item.createTime }}</text>
          </view>
          <view class="detail-right">
            <text class="detail-amount">+¥{{ formatMoney(item.amount) }}</text>
            <view :class="['detail-status', 'status-' + item.status]">
              {{ getStatusText(item.status) }}
            </view>
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
    <u-popup v-model="showFilter" mode="bottom" border-radius="24" :closeable="true">
      <view class="filter-popup">
        <view class="popup-header">
          <text class="popup-title">筛选条件</text>
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
      this.totalAmount = 128500
      this.orderCount = 156
      this.customerCount = 89
      this.avgAmount = this.orderCount > 0 ? Math.round(this.totalAmount / this.orderCount) : 0
    },

    async loadTrend() {
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
      this.rankingList = [
        { id: 1, nickname: '艺术达人小王', avatar: '/static/avatar/demo1.jpg', orderCount: 289, amount: 358000 },
        { id: 2, nickname: '收藏家老李', avatar: '/static/avatar/demo2.jpg', orderCount: 245, amount: 298000 },
        { id: 3, nickname: '画室老板', avatar: '/static/avatar/demo3.jpg', orderCount: 198, amount: 245000 },
        { id: 4, nickname: '艺术爱好者', avatar: '/static/avatar/demo4.jpg', orderCount: 156, amount: 189000 },
        { id: 5, nickname: '品鉴师', avatar: '/static/avatar/demo5.jpg', orderCount: 134, amount: 165000 }
      ]
    },

    async loadDetail() {
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
/* 变量定义 */
$bg-primary: #0d0d0d;
$bg-card: #1a1a1a;
$bg-elevated: #242424;
$text-primary: #ffffff;
$text-secondary: #999999;
$text-muted: #666666;
$accent-gold: #c9a227;
$accent-gold-light: #e5c76b;
$border-color: rgba(255, 255, 255, 0.08);

.performance-page {
  min-height: 100vh;
  background: $bg-primary;
  padding: 0 24rpx 40rpx;
  position: relative;
}

/* 顶部装饰 */
.page-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 400rpx;
  overflow: hidden;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba($accent-gold, 0.15) 0%, transparent 70%);
}

.deco-circle-1 {
  width: 600rpx;
  height: 600rpx;
  top: -200rpx;
  right: -100rpx;
}

.deco-circle-2 {
  width: 400rpx;
  height: 400rpx;
  top: -100rpx;
  left: -100rpx;
  background: radial-gradient(circle, rgba($accent-gold, 0.08) 0%, transparent 70%);
}

/* 头部数据卡片 */
.header-card {
  position: relative;
  background: linear-gradient(135deg, rgba($accent-gold, 0.2) 0%, rgba($accent-gold, 0.05) 100%);
  border: 1rpx solid rgba($accent-gold, 0.2);
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
}

.header-card::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 200rpx;
  height: 200rpx;
  background: radial-gradient(circle, rgba($accent-gold, 0.2) 0%, transparent 70%);
  border-radius: 50%;
}

.total-section {
  text-align: center;
  margin-bottom: 40rpx;
  position: relative;
  z-index: 1;
}

.total-label {
  font-size: 28rpx;
  color: rgba($text-primary, 0.7);
  margin-bottom: 16rpx;
  display: block;
}

.total-value-wrap {
  display: flex;
  align-items: baseline;
  justify-content: center;
}

.total-symbol {
  font-size: 36rpx;
  font-weight: 600;
  color: $accent-gold;
  margin-right: 4rpx;
}

.total-value {
  font-size: 64rpx;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: -2rpx;
}

.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  position: relative;
  z-index: 1;
}

.stat-item {
  text-align: center;
  flex: 1;
}

.stat-value {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 8rpx;
}

.stat-label {
  font-size: 24rpx;
  color: $text-secondary;
}

.stat-divider {
  width: 1rpx;
  height: 48rpx;
  background: $border-color;
}

/* 通用卡片样式 */
.trend-card,
.ranking-card,
.detail-card {
  background: $bg-card;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28rpx;
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $text-primary;
}

/* 周期选择器 */
.period-selector {
  display: flex;
  gap: 8rpx;
}

.period-btn {
  font-size: 24rpx;
  color: $text-muted;
  padding: 10rpx 20rpx;
  border-radius: 20rpx;
  background: $bg-elevated;
  transition: all 0.3s;

  &.active {
    background: $accent-gold;
    color: $bg-primary;
    font-weight: 500;
  }
}

/* 趋势图 */
.trend-chart {
  height: 220rpx;
}

.simple-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 180rpx;
  padding-top: 20rpx;
}

.chart-bar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.chart-bar {
  width: 40rpx;
  background: linear-gradient(180deg, $accent-gold 0%, rgba($accent-gold, 0.3) 100%);
  border-radius: 8rpx 8rpx 0 0;
  margin-bottom: 12rpx;
  min-height: 20rpx;
  transition: height 0.3s;
}

.chart-label {
  font-size: 22rpx;
  color: $text-secondary;
}

/* 排行榜 */
.more-link {
  display: flex;
  align-items: center;
  gap: 6rpx;
  
  text {
    font-size: 26rpx;
    color: $text-secondary;
  }
}

.ranking-list {
  .ranking-item {
    display: flex;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid $border-color;

    &:last-child {
      border-bottom: none;
    }

    &.top-three {
      .rank-badge {
        background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
        color: $bg-primary;
        font-weight: 700;
      }
    }
  }
}

.rank-badge {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: $bg-elevated;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 600;
  color: $text-muted;
  margin-right: 16rpx;
}

.rank-number {
  width: 44rpx;
  text-align: center;
  font-size: 26rpx;
  color: $text-muted;
  margin-right: 16rpx;
}

.rank-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  margin-right: 16rpx;
  background: $bg-elevated;
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-name {
  display: block;
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  margin-bottom: 6rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-count {
  font-size: 22rpx;
  color: $text-secondary;
}

.rank-amount {
  text-align: right;
}

.rank-amount .amount {
  display: block;
  font-size: 30rpx;
  color: $accent-gold;
  font-weight: 600;
  margin-bottom: 4rpx;
}

.rank-amount .label {
  font-size: 20rpx;
  color: $text-secondary;
}

/* 业绩明细 */
.filter-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  font-size: 26rpx;
  color: $accent-gold;
  padding: 10rpx 20rpx;
  background: rgba($accent-gold, 0.1);
  border-radius: 20rpx;
}

.detail-list {
  .detail-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 0;
    border-bottom: 1rpx solid $border-color;

    &:last-child {
      border-bottom: none;
    }
  }
}

.detail-left {
  flex: 1;
  min-width: 0;
}

.detail-title {
  display: block;
  font-size: 28rpx;
  color: $text-primary;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-time {
  font-size: 22rpx;
  color: $text-secondary;
}

.detail-right {
  text-align: right;
  flex-shrink: 0;
  margin-left: 20rpx;
}

.detail-amount {
  display: block;
  font-size: 32rpx;
  color: $accent-gold;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.detail-status {
  display: inline-block;
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;

  &.status-settled {
    background: rgba(46, 204, 113, 0.15);
    color: #2ecc71;
  }

  &.status-pending {
    background: rgba($accent-gold, 0.15);
    color: $accent-gold;
  }

  &.status-cancelled {
    background: rgba($text-muted, 0.15);
    color: $text-muted;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: $text-secondary;
}

.empty-state {
  text-align: center;
  padding: 60rpx;
  color: $text-muted;
  font-size: 28rpx;
}

/* 筛选弹窗 */
.filter-popup {
  padding: 32rpx;
}

.popup-header {
  margin-bottom: 32rpx;
}

.popup-title {
  font-size: 34rpx;
  font-weight: 600;
  color: $text-primary;
}

.filter-content {
  padding-bottom: 32rpx;
}

.filter-section {
  margin-bottom: 32rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.filter-label {
  display: block;
  font-size: 28rpx;
  color: $text-secondary;
  margin-bottom: 20rpx;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.filter-option {
  padding: 14rpx 32rpx;
  border-radius: 32rpx;
  font-size: 28rpx;
  background: $bg-elevated;
  color: $text-secondary;
  border: 1rpx solid $border-color;
  transition: all 0.3s;

  &.active {
    background: rgba($accent-gold, 0.15);
    border-color: $accent-gold;
    color: $accent-gold;
  }
}

.popup-footer {
  display: flex;
  gap: 24rpx;
  padding-top: 32rpx;
  border-top: 1rpx solid $border-color;
}

.btn-reset, .btn-confirm {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 500;
}

.btn-reset {
  background: $bg-elevated;
  color: $text-secondary;
}

.btn-confirm {
  background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
  color: $bg-primary;
  box-shadow: 0 4rpx 20rpx rgba($accent-gold, 0.3);
}
</style>
