<template>
  <view class="performance-page">
    <!-- 时间选择 -->
    <view class="time-selector">
      <view class="time-item" :class="{ active: timeRange === 'week' }" @click="changeTimeRange('week')">本周</view>
      <view class="time-item" :class="{ active: timeRange === 'month' }" @click="changeTimeRange('month')">本月</view>
      <view class="time-item" :class="{ active: timeRange === 'year' }" @click="changeTimeRange('year')">本年</view>
    </view>

    <!-- 业绩概览 -->
    <view class="performance-overview">
      <view class="overview-card main">
        <text class="label">总收益</text>
        <text class="value">¥12,580</text>
        <view class="compare">
          <text class="arrow up">↑</text>
          <text class="text">较上期 +15.8%</text>
        </view>
      </view>
      <view class="overview-row">
        <view class="overview-card">
          <text class="label">订单数</text>
          <text class="value">45</text>
        </view>
        <view class="overview-card">
          <text class="label">成交客户</text>
          <text class="value">38</text>
        </view>
        <view class="overview-card">
          <text class="label">平均单价</text>
          <text class="value">¥8,420</text>
        </view>
      </view>
    </view>

    <!-- 收益趋势图 -->
    <view class="trend-chart">
      <text class="chart-title">收益趋势</text>
      <view class="chart-container">
        <view class="y-axis">
          <text>5000</text>
          <text>3000</text>
          <text>1000</text>
        </view>
        <view class="chart-area">
          <view class="chart-line">
            <view class="line-path" style="height: 50%"></view>
            <view class="line-path" style="height: 70%"></view>
            <view class="line-path" style="height: 40%"></view>
            <view class="line-path" style="height: 85%"></view>
            <view class="line-path" style="height: 65%"></view>
            <view class="line-path" style="height: 90%"></view>
          </view>
          <view class="x-axis">
            <text>1日</text>
            <text>5日</text>
            <text>10日</text>
            <text>15日</text>
            <text>20日</text>
            <text>25日</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 排行榜 -->
    <view class="ranking-section">
      <text class="section-title">分销达人榜</text>
      <view class="ranking-list">
        <view class="ranking-item" v-for="(item, index) in rankingList" :key="index">
          <view class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</view>
          <image class="avatar" :src="item.avatar" mode="aspectFill" />
          <view class="info">
            <text class="name">{{ item.name }}</text>
            <text class="earnings">收益 ¥{{ item.earnings }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 业绩明细 -->
    <view class="detail-section">
      <text class="section-title">业绩明细</text>
      <view class="detail-list">
        <view class="detail-item" v-for="(item, index) in detailList" :key="index">
          <view class="detail-left">
            <text class="title">{{ item.title }}</text>
            <text class="time">{{ item.time }}</text>
          </view>
          <text class="amount">+¥{{ item.amount }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      timeRange: 'month',
      rankingList: [
        { avatar: '/static/avatar-default.png', name: '李明', earnings: '8,560' },
        { avatar: '/static/avatar-default.png', name: '王芳', earnings: '7,230' },
        { avatar: '/static/avatar-default.png', name: '张伟', earnings: '6,890' },
        { avatar: '/static/avatar-default.png', name: '赵丽', earnings: '5,420' },
        { avatar: '/static/avatar-default.png', name: '孙强', earnings: '4,850' }
      ],
      detailList: [
        { title: '订单分成 - 张大千山水画', time: '2024-03-15 14:30', amount: '1,680' },
        { title: '订单分成 - 齐白石花鸟', time: '2024-03-14 10:20', amount: '980' },
        { title: '团队奖励', time: '2024-03-13 16:45', amount: '500' },
        { title: '订单分成 - 徐悲鸿骏马', time: '2024-03-12 09:15', amount: '2,880' }
      ]
    }
  },
  methods: {
    changeTimeRange(range) {
      this.timeRange = range
    }
  }
}
</script>

<style lang="scss" scoped>
.performance-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 30rpx;
}

.time-selector {
  background: #fff;
  display: flex;
  padding: 20rpx;
  justify-content: center;
  gap: 40rpx;
}

.time-item {
  padding: 10rpx 30rpx;
  font-size: 28rpx;
  color: #666;
  border-radius: 30rpx;
}

.time-item.active {
  background: #667eea;
  color: #fff;
}

.performance-overview {
  padding: 24rpx;
}

.overview-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.overview-card.main {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  text-align: center;
}

.overview-card.main .label {
  font-size: 26rpx;
  opacity: 0.9;
}

.overview-card.main .value {
  font-size: 56rpx;
  font-weight: 700;
  margin: 16rpx 0;
}

.overview-card.main .compare {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8rpx;
  font-size: 24rpx;
}

.overview-row {
  display: flex;
  gap: 20rpx;
}

.overview-row .overview-card {
  flex: 1;
  text-align: center;
  margin-bottom: 0;
}

.overview-row .label {
  font-size: 24rpx;
  color: #999;
  display: block;
}

.overview-row .value {
  font-size: 32rpx;
  font-weight: 700;
  color: #333;
  margin-top: 8rpx;
  display: block;
}

.trend-chart {
  background: #fff;
  margin: 0 24rpx 24rpx;
  border-radius: 16rpx;
  padding: 24rpx;
}

.chart-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 20rpx;
}

.chart-container {
  display: flex;
  height: 300rpx;
}

.y-axis {
  width: 80rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 20rpx 0;
}

.y-axis text {
  font-size: 22rpx;
  color: #999;
  text-align: right;
  padding-right: 10rpx;
}

.chart-area {
  flex: 1;
  position: relative;
}

.chart-line {
  height: 200rpx;
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  padding-bottom: 20rpx;
}

.line-path {
  width: 40rpx;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  border-radius: 8rpx 8rpx 0 0;
}

.x-axis {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
}

.x-axis text {
  font-size: 22rpx;
  color: #999;
}

.ranking-section {
  background: #fff;
  margin: 0 24rpx 24rpx;
  border-radius: 16rpx;
  padding: 24rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 20rpx;
}

.ranking-list {
  display: flex;
  gap: 16rpx;
}

.ranking-item {
  flex: 1;
  text-align: center;
  position: relative;
}

.rank {
  position: absolute;
  top: -10rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  background: #ffd700;
  color: #fff;
  border-radius: 50%;
  font-size: 22rpx;
  font-weight: 700;
}

.rank-1 {
  background: #ffd700;
}

.rank-2 {
  background: #c0c0c0;
}

.rank-3 {
  background: #cd7f32;
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  margin-top: 20rpx;
}

.info {
  margin-top: 8rpx;
}

.name {
  font-size: 24rpx;
  color: #333;
  display: block;
}

.earnings {
  font-size: 22rpx;
  color: #e63946;
  display: block;
}

.detail-section {
  background: #fff;
  margin: 0 24rpx;
  border-radius: 16rpx;
  padding: 24rpx;
}

.detail-list {
  margin-top: 10rpx;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f5f5f5;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-left {
  display: flex;
  flex-direction: column;
}

.detail-left .title {
  font-size: 28rpx;
  color: #333;
}

.detail-left .time {
  font-size: 24rpx;
  color: #999;
  margin-top: 6rpx;
}

.amount {
  font-size: 32rpx;
  font-weight: 600;
  color: #e63946;
}
</style>