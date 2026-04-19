<template>
  <view class="user-page">
    <!-- 用户信息头部 -->
    <view class="user-header">
      <view class="user-info" @click="goLogin">
        <image class="user-avatar" :src="userInfo.avatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
        <view class="user-detail">
          <text class="user-name">{{ userInfo.nickname || '点击登录' }}</text>
          <text class="user-phone" v-if="userInfo.phone">{{ userInfo.phone }}</text>
        </view>
      </view>
      <u-icon name="arrow-right" size="18" color="#fff" @click="goLogin"></u-icon>
    </view>

    <!-- 身份入口区 -->
    <view class="identity-section">
      <!-- 艺术家入口 -->
      <view class="identity-card artist-card" v-if="userInfo.isArtist || showArtistEntry" @click="goArtistHome">
        <view class="identity-header">
          <image class="identity-icon" src="/static/icons/artist.png" mode="aspectFit"></image>
          <text class="identity-title">艺术家</text>
        </view>
        <view class="identity-stats">
          <view class="stat-item">
            <text class="stat-value">{{ artistStats.worksCount }}</text>
            <text class="stat-label">作品</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ artistStats.fansCount }}</text>
            <text class="stat-label">粉丝</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ artistStats.salesCount }}</text>
            <text class="stat-label">销量</text>
          </view>
        </view>
        <view class="identity-action">进入艺术家中心</view>
      </view>
      
      <!-- 收藏家入口 -->
      <view class="identity-card collector-card" @click="goCollect">
        <view class="identity-header">
          <image class="identity-icon" src="/static/icons/collector.png" mode="aspectFit"></image>
          <text class="identity-title">收藏家</text>
        </view>
        <view class="identity-menu">
          <view class="menu-item" @click.stop="goFavorites">
            <u-icon name="heart" size="22" color="#667eea"></u-icon>
            <text>我的收藏</text>
          </view>
          <view class="menu-item" @click.stop="goPurchased">
            <u-icon name="shopping-bag" size="22" color="#667eea"></u-icon>
            <text>已购作品</text>
          </view>
        </view>
      </view>
      
      <!-- 艺荐官入口 -->
      <view class="identity-card promoter-card" v-if="userInfo.isPromoter || showPromoterEntry" @click="goPromoter">
        <view class="identity-header">
          <image class="identity-icon" src="/static/icons/promoter.png" mode="aspectFit"></image>
          <text class="identity-title">艺荐官</text>
        </view>
        <view class="promoter-earnings">
          <view class="earnings-item">
            <text class="earnings-label">累计佣金</text>
            <text class="earnings-value">¥{{ promoterStats.totalCommission }}</text>
          </view>
          <view class="earnings-item">
            <text class="earnings-label">可提现</text>
            <text class="earnings-value highlight">¥{{ promoterStats.withdrawable }}</text>
          </view>
        </view>
        <view class="identity-action">进入艺荐中心</view>
      </view>
    </view>

    <!-- 订单入口 -->
    <view class="order-section">
      <view class="section-header">
        <text class="section-title">我的订单</text>
        <view class="section-more" @click="goOrderList('')">
          <text>全部订单</text>
          <u-icon name="arrow-right" size="14" color="#999"></u-icon>
        </view>
      </view>
      <view class="order-tabs">
        <view class="order-tab" v-for="item in orderTabs" :key="item.status" @click="goOrderList(item.status)">
          <image class="tab-icon" :src="item.icon" mode="aspectFit"></image>
          <text class="tab-text">{{ item.label }}</text>
          <view class="tab-badge" v-if="item.count > 0">{{ item.count > 99 ? '99+' : item.count }}</view>
        </view>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="function-section">
      <view class="function-list">
        <view class="function-item" v-for="item in functionList" :key="item.id" @click="goFunction(item)">
          <view class="function-left">
            <image class="function-icon" :src="item.icon" mode="aspectFit"></image>
            <text class="function-name">{{ item.name }}</text>
          </view>
          <u-icon name="arrow-right" size="16" color="#ccc"></u-icon>
        </view>
      </view>
    </view>

    <!-- 设置入口 -->
    <view class="settings-entry">
      <view class="settings-btn" @click="goSettings">
        <u-icon name="setting" size="20" color="#666"></u-icon>
        <text>设置</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '@/store/modules/user.js'
import { getPromoterStats } from '@/api/promoter.js'

export default {
  data() {
    return {
      artistStats: {
        worksCount: 0,
        fansCount: 0,
        salesCount: 0
      },
      promoterStats: {
        totalCommission: 0,
        withdrawable: 0
      },
      orderTabs: [
        { label: '待付款', status: 'pending_payment', icon: '/static/icons/order-pay.png', count: 0 },
        { label: '待发货', status: 'pending_ship', icon: '/static/icons/order-ship.png', count: 0 },
        { label: '待收货', status: 'shipped', icon: '/static/icons/order-receive.png', count: 0 },
        { label: '已完成', status: 'completed', icon: '/static/icons/order-complete.png', count: 0 }
      ],
      functionList: [
        { id: 'address', name: '收货地址', icon: '/static/icons/address.png', path: '/pages/address/list' },
        { id: 'coupon', name: '优惠券', icon: '/static/icons/coupon.png', path: '/pages/coupon/list' },
        { id: 'history', name: '浏览历史', icon: '/static/icons/history.png', path: '/pages/history/index' },
        { id: 'help', name: '帮助中心', icon: '/static/icons/help.png', path: '/pages/help/index' },
        { id: 'about', name: '关于我们', icon: '/static/icons/about.png', path: '/pages/about/index' }
      ],
      showArtistEntry: true,
      showPromoterEntry: true
    }
  },
  
  computed: {
    userStore() {
      return useUserStore()
    },
    userInfo() {
      return this.userStore.userInfo || {}
    }
  },
  
  onShow() {
    this.initUserInfo()
  },
  
  methods: {
    initUserInfo() {
      this.userStore.initUserInfo()
      
      if (this.userStore.isAuthenticated) {
        this.loadOrderCounts()
        this.loadArtistStats()
        this.loadPromoterStats()
      }
    },
    
    async loadOrderCounts() {
      // 加载各状态订单数量
      // 简化处理，实际应该调用接口
    },
    
    async loadArtistStats() {
      // 加载艺术家统计数据
    },
    
    async loadPromoterStats() {
      try {
        const res = await getPromoterStats()
        this.promoterStats = res
      } catch (e) {}
    },
    
    goLogin() {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
      }
    },
    
    goArtistHome() {
      if (this.userStore.isAuthenticated) {
        uni.navigateTo({ url: `/pages/artist/home?id=${this.userInfo.id}` })
      } else {
        uni.navigateTo({ url: '/pages/login/index' })
      }
    },
    
    goCollect() {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
    },
    
    goFavorites() {
      uni.navigateTo({ url: '/pages/user/favorites' })
    },
    
    goPurchased() {
      uni.navigateTo({ url: '/pages/user/purchased' })
    },
    
    goPromoter() {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url: '/pages/promoter/index' })
    },
    
    goOrderList(status) {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url: `/pages/order/list?status=${status}` })
    },
    
    goFunction(item) {
      if (!this.userStore.isAuthenticated && ['address', 'coupon', 'history'].includes(item.id)) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url: item.path })
    },
    
    goSettings() {
      uni.navigateTo({ url: '/pages/settings/index' })
    }
  }
}
</script>

<style lang="scss" scoped>
.user-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.user-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60rpx 30rpx 40rpx;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
  margin-right: 24rpx;
}

.user-detail {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 36rpx;
  color: #fff;
  font-weight: 600;
  margin-bottom: 10rpx;
}

.user-phone {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

.identity-section {
  display: flex;
  flex-wrap: wrap;
  padding: 20rpx;
  gap: 20rpx;
  margin-top: -30rpx;
}

.identity-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  flex: 1;
  min-width: 300rpx;
}

.identity-header {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.identity-icon {
  width: 40rpx;
  height: 40rpx;
  margin-right: 12rpx;
}

.identity-title {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
}

.identity-stats {
  display: flex;
  margin-bottom: 16rpx;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 32rpx;
  color: #333;
  font-weight: 600;
}

.stat-label {
  font-size: 22rpx;
  color: #999;
}

.identity-action {
  font-size: 24rpx;
  color: #667eea;
}

.identity-menu {
  display: flex;
}

.menu-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10rpx 0;
}

.menu-item text {
  font-size: 24rpx;
  color: #666;
  margin-top: 8rpx;
}

.promoter-earnings {
  display: flex;
  margin-bottom: 16rpx;
}

.earnings-item {
  flex: 1;
}

.earnings-label {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-bottom: 6rpx;
}

.earnings-value {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
}

.earnings-value.highlight {
  color: #e74c3c;
}

.order-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 24rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
}

.section-more {
  display: flex;
  align-items: center;
  font-size: 26rpx;
  color: #999;
}

.section-more text {
  margin-right: 6rpx;
}

.order-tabs {
  display: flex;
}

.order-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.tab-icon {
  width: 50rpx;
  height: 50rpx;
  margin-bottom: 10rpx;
}

.tab-text {
  font-size: 24rpx;
  color: #666;
}

.tab-badge {
  position: absolute;
  top: -6rpx;
  right: 20rpx;
  background: #e74c3c;
  color: #fff;
  font-size: 18rpx;
  padding: 2rpx 8rpx;
  border-radius: 20rpx;
  min-width: 32rpx;
  text-align: center;
}

.function-section {
  margin: 20rpx;
}

.function-list {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.function-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.function-item:last-child {
  border-bottom: none;
}

.function-left {
  display: flex;
  align-items: center;
}

.function-icon {
  width: 40rpx;
  height: 40rpx;
  margin-right: 20rpx;
}

.function-name {
  font-size: 28rpx;
  color: #333;
}

.settings-entry {
  margin: 20rpx;
}

.settings-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
}

.settings-btn text {
  font-size: 28rpx;
  color: #666;
  margin-left: 12rpx;
}
</style>
