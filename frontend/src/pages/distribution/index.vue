<template>
  <view class="distribution">
    <!-- 头部信息 -->
    <view class="header">
      <view class="user-info">
        <image class="avatar" src="/static/avatar-default.png" mode="aspectFill" />
        <view class="info">
          <text class="nickname">艺术收藏家</text>
          <text class="level">金牌分销商</text>
        </view>
      </view>
      <view class="invite-code">
        <text class="label">邀请码</text>
        <text class="code">ART2024</text>
      </view>
    </view>

    <!-- 核心数据 -->
    <view class="stats-card">
      <view class="stats-grid">
        <view class="stat-item">
          <text class="value">¥12,580</text>
          <text class="label">累计收益</text>
        </view>
        <view class="stat-item">
          <text class="value">¥2,350</text>
          <text class="label">本月收益</text>
        </view>
        <view class="stat-item">
          <text class="value">156</text>
          <text class="label">我的客户</text>
        </view>
        <view class="stat-item">
          <text class="value">89</text>
          <text class="label">团队成员</text>
        </view>
      </view>
    </view>

    <!-- 收益趋势图 -->
    <view class="earnings-chart">
      <view class="chart-header">
        <text class="title">收益趋势</text>
        <view class="tabs">
          <text class="tab active">近7天</text>
          <text class="tab">近30天</text>
        </view>
      </view>
      <view class="chart-placeholder">
        <view class="bar-group">
          <view class="bar" style="height: 60%"></view>
          <text class="bar-label">周一</text>
        </view>
        <view class="bar-group">
          <view class="bar" style="height: 80%"></view>
          <text class="bar-label">周二</text>
        </view>
        <view class="bar-group">
          <view class="bar" style="height: 45%"></view>
          <text class="bar-label">周三</text>
        </view>
        <view class="bar-group">
          <view class="bar" style="height: 90%"></view>
          <text class="bar-label">周四</text>
        </view>
        <view class="bar-group">
          <view class="bar" style="height: 75%"></view>
          <text class="bar-label">周五</text>
        </view>
        <view class="bar-group">
          <view class="bar" style="height: 95%"></view>
          <text class="bar-label">周六</text>
        </view>
        <view class="bar-group">
          <view class="bar" style="height: 70%"></view>
          <text class="bar-label">周日</text>
        </view>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="action-grid">
      <view class="action-item" @click="goToTeam">
        <image class="icon" src="/static/icon-team.png" mode="aspectFit" />
        <text class="name">我的团队</text>
      </view>
      <view class="action-item" @click="goToPerformance">
        <image class="icon" src="/static/icon-chart.png" mode="aspectFit" />
        <text class="name">分销业绩</text>
      </view>
      <view class="action-item" @click="goToCustomer">
        <image class="icon" src="/static/icon-customer.png" mode="aspectFit" />
        <text class="name">我的客户</text>
      </view>
      <view class="action-item" @click="goToPoster">
        <image class="icon" src="/static/icon-poster.png" mode="aspectFit" />
        <text class="name">推广海报</text>
      </view>
      <view class="action-item" @click="goToRules">
        <image class="icon" src="/static/icon-rule.png" mode="aspectFit" />
        <text class="name">分销规则</text>
      </view>
      <view class="action-item" @click="goToWithdrawal">
        <image class="icon" src="/static/icon-withdraw.png" mode="aspectFit" />
        <text class="name">提现</text>
      </view>
    </view>

    <!-- 分销商品推荐 -->
    <view class="recommend-section">
      <view class="section-header">
        <text class="title">分销商品</text>
        <text class="more" @click="goToProductList">查看更多</text>
      </view>
      <scroll-view class="product-scroll" scroll-x>
        <view class="product-card">
          <image class="product-image" src="/static/artwork-placeholder.png" mode="aspectFill" />
          <text class="product-name">张大千 山水画</text>
          <text class="product-price">¥168,000</text>
          <text class="commission">预计赚 ¥8,400</text>
        </view>
        <view class="product-card">
          <image class="product-image" src="/static/artwork-placeholder.png" mode="aspectFill" />
          <text class="product-name">齐白石 花鸟系列</text>
          <text class="product-price">¥98,000</text>
          <text class="commission">预计赚 ¥4,900</text>
        </view>
        <view class="product-card">
          <image class="product-image" src="/static/artwork-placeholder.png" mode="aspectFill" />
          <text class="product-name">徐悲鸿 骏马图</text>
          <text class="product-price">¥288,000</text>
          <text class="commission">预计赚 ¥14,400</text>
        </view>
      </scroll-view>
    </view>

    <!-- 底部导航 -->
    <view class="bottom-nav">
      <view class="nav-item" @click="shareToFriend">
        <text class="btn-primary">分享赚更多</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {}
  },
  methods: {
    goToTeam() {
      uni.navigateTo({ url: '/pages/distribution/team' })
    },
    goToPerformance() {
      uni.navigateTo({ url: '/pages/distribution/performance' })
    },
    goToCustomer() {
      uni.navigateTo({ url: '/pages/distribution/customer' })
    },
    goToPoster() {
      uni.navigateTo({ url: '/pages/distribution/poster' })
    },
    goToRules() {
      uni.navigateTo({ url: '/pages/distribution/rules' })
    },
    goToWithdrawal() {
      uni.navigateTo({ url: '/pages/user/wallet' })
    },
    goToProductList() {
      uni.navigateTo({ url: '/pages/product/list' })
    },
    shareToFriend() {
      uni.showToast({ title: '分享功能开发中', icon: 'none' })
    }
  }
}
</script>

<style lang="scss" scoped>
.distribution {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40rpx 30rpx;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
}

.info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.nickname {
  font-size: 32rpx;
  font-weight: 600;
}

.level {
  font-size: 22rpx;
  background: rgba(255, 255, 255, 0.2);
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}

.invite-code {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8rpx;
}

.invite-code .label {
  font-size: 22rpx;
  opacity: 0.8;
}

.invite-code .code {
  font-size: 28rpx;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.2);
  padding: 8rpx 20rpx;
  border-radius: 8rpx;
}

.stats-card {
  background: #fff;
  margin: -40rpx 24rpx 24rpx;
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
}

.stat-item {
  text-align: center;
}

.stat-item .value {
  font-size: 32rpx;
  font-weight: 700;
  color: #333;
  display: block;
}

.stat-item .label {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
  display: block;
}

.earnings-chart {
  background: #fff;
  margin: 0 24rpx 24rpx;
  border-radius: 16rpx;
  padding: 24rpx;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}

.chart-header .title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.tabs {
  display: flex;
  gap: 16rpx;
}

.tab {
  font-size: 24rpx;
  color: #999;
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
}

.tab.active {
  background: #667eea;
  color: #fff;
}

.chart-placeholder {
  height: 200rpx;
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  padding: 0 20rpx;
}

.bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
}

.bar {
  width: 40rpx;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  border-radius: 8rpx 8rpx 0 0;
}

.bar-label {
  font-size: 20rpx;
  color: #999;
}

.action-grid {
  background: #fff;
  margin: 0 24rpx 24rpx;
  border-radius: 16rpx;
  padding: 20rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30rpx;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.icon {
  width: 64rpx;
  height: 64rpx;
  background: #f0f0f0;
  border-radius: 16rpx;
}

.name {
  font-size: 24rpx;
  color: #333;
}

.recommend-section {
  background: #fff;
  margin: 0 24rpx;
  border-radius: 16rpx;
  padding: 24rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.more {
  font-size: 24rpx;
  color: #667eea;
}

.product-scroll {
  white-space: nowrap;
}

.product-card {
  display: inline-block;
  width: 280rpx;
  margin-right: 20rpx;
  vertical-align: top;
}

.product-image {
  width: 280rpx;
  height: 200rpx;
  border-radius: 12rpx;
  background: #f5f5f5;
}

.product-name {
  font-size: 26rpx;
  color: #333;
  margin-top: 16rpx;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-price {
  font-size: 28rpx;
  color: #e63946;
  font-weight: 600;
  margin-top: 8rpx;
  display: block;
}

.commission {
  font-size: 22rpx;
  color: #667eea;
  margin-top: 8rpx;
  display: block;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.btn-primary {
  display: block;
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 44rpx;
}
</style>