<template>
  <view class="user-page">
    <!-- 顶部用户信息区 -->
    <view class="user-header">
      <!-- 头部操作按钮 -->
      <view class="header-actions">
        <view class="action-btn" @click="goSettings">
          
        </view>
        <view class="action-btn" @click="goMessage">
          
          <view class="badge" v-if="unreadCount > 0">{{ unreadCount }}</view>
        </view>
      </view>
      
      <!-- 用户信息 -->
      <view class="user-info" @click="handleUserClick">
        <view class="avatar-wrap">
          <image class="user-avatar" :src="userInfo.avatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
          <view class="avatar-badge" v-if="userInfo.isArtist">
            <text>★</text>
          </view>
        </view>
        <view class="user-detail">
          <text class="user-name">{{ userInfo.nickname || '登录/注册' }}</text>
          <text class="user-id" v-if="userInfo.id">ID: {{ userInfo.id }}</text>
          <view class="user-tags" v-if="userInfo.isArtist || userInfo.isPromoter">
            <text class="user-tag artist" v-if="userInfo.isArtist">艺术家</text>
            <text class="user-tag promoter" v-if="userInfo.isPromoter">艺荐官</text>
            <text class="user-tag collector" v-else>收藏家</text>
          </view>
        </view>
      </view>
      
      <!-- 用户数据统计 -->
      <view class="user-stats">
        <view class="stat-item" @click="goArtistWorks" v-if="userInfo.isArtist">
          <text class="stat-value">{{ artistStats.worksCount }}</text>
          <text class="stat-label">发布作品</text>
        </view>
        <view class="stat-item" @click="goFollowing" v-else>
          <text class="stat-value">{{ userStats.following }}</text>
          <text class="stat-label">关注</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item" @click="goFollowing">
          <text class="stat-value">{{ formatNumber(artistStats.fansCount || userStats.followers) }}</text>
          <text class="stat-label">粉丝关注</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item" @click="goFavorites">
          <text class="stat-value">{{ userStats.favorites }}</text>
          <text class="stat-label">收藏佳作</text>
        </view>
      </view>
    </view>

    <!-- 身份中心入口 -->
    <view class="identity-section" v-if="userStore.isAuthenticated">
      <!-- 艺术家中心入口 -->
      <view class="identity-card" v-if="userInfo.isArtist" @click="goArtistHome">
        <view class="card-left">
          <view class="card-icon artist-icon">
            <text>★</text>
          </view>
          <view class="card-info">
            <text class="card-title">艺术家中心</text>
            <text class="card-subtitle">管理您的艺术作品</text>
          </view>
        </view>
        <view class="card-arrow">
          
        </view>
      </view>
      
      <!-- 艺荐官中心入口 -->
      <view class="identity-card" v-if="userInfo.isPromoter" @click="goPromoter">
        <view class="card-left">
          <view class="card-icon promoter-icon">
            <text>💎</text>
          </view>
          <view class="card-info">
            <text class="card-title">艺荐官中心</text>
            <text class="card-subtitle">本月预估佣金: ¥{{ formatMoney(promoterStats.monthlyCommission) }}</text>
          </view>
        </view>
        <view class="card-arrow">
          
        </view>
      </view>
    </view>

    <!-- 功能网格 -->
    <view class="function-section">
      <view class="function-grid">
        <view class="grid-item" v-for="item in functionList" :key="item.id" @click="goFunction(item)">
          <view class="grid-icon" :style="{ background: item.bgColor }">
            
          </view>
          <text class="grid-name">{{ item.name }}</text>
        </view>
      </view>
    </view>

    <!-- 底部占位 -->
    <view class="page-footer"></view>
  </view>
</template>

<script>
import { useUserStore } from '@/store/modules/user.js'
import { getPromoterStats } from '@/api/promoter.js'

export default {
  data() {
    return {
      unreadCount: 0,
      userStats: {
        favorites: 48,
        following: 12,
        followers: 1200
      },
      artistStats: {
        worksCount: 12,
        fansCount: 1200
      },
      promoterStats: {
        monthlyCommission: 12850
      },
      functionList: [
        { id: 'works', name: '作品管理', icon: 'photo', path: '/pages/artist/manage', bgColor: 'rgba(201,162,39,0.15)', iconColor: '#c9a227' },
        { id: 'orders', name: '订单中心', icon: 'order', path: '/pages/order/list', bgColor: 'rgba(52,152,219,0.15)', iconColor: '#3498db' },
        { id: 'wallet', name: '钱包余额', icon: 'wallet', path: '/pages/user/wallet', bgColor: 'rgba(46,204,113,0.15)', iconColor: '#2ecc71' },
        { id: 'cert', name: '资质认证', icon: 'shield', path: '/pages/artist/cert', bgColor: 'rgba(155,89,182,0.15)', iconColor: '#9b59b6' }
      ]
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
        this.loadPromoterStats()
      }
    },
    
    async loadPromoterStats() {
      try {
        const res = await getPromoterStats()
        this.promoterStats = { ...this.promoterStats, ...res }
      } catch (e) {}
    },
    
    handleUserClick() {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
      } else {
        uni.navigateTo({ url: '/pages/user/profile' })
      }
    },
    
    goSettings() {
      uni.navigateTo({ url: '/pages/user/settings' })
    },
    
    goMessage() {
      uni.navigateTo({ url: '/pages/user/message' })
    },
    
    goArtistHome() {
      if (this.userStore.isAuthenticated && this.userInfo.isArtist) {
        uni.navigateTo({ url: `/pages/artist/home?id=${this.userInfo.id}` })
      } else {
        uni.navigateTo({ url: '/pages/login/index' })
      }
    },
    
    goArtistWorks() {
      if (this.userStore.isAuthenticated && this.userInfo.isArtist) {
        uni.navigateTo({ url: '/pages/artist/manage' })
      } else {
        uni.navigateTo({ url: '/pages/login/index' })
      }
    },
    
    goPromoter() {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url: '/pages/promoter/index' })
    },
    
    goFavorites() {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url: '/pages/user/favorites' })
    },
    
    goFollowing() {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url: '/pages/user/following' })
    },
    
    goFunction(item) {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url: item.path })
    },
    
    formatNumber(num) {
      if (!num) return '0'
      if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'k'
      }
      return num.toString()
    },
    
    formatMoney(amount) {
      if (!amount) return '0.00'
      return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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

.user-page {
  min-height: 100vh;
  background: $bg-primary;
  padding-bottom: env(safe-area-inset-bottom);
}

/* 顶部用户信息区 */
.user-header {
  padding: calc(var(--status-bar-height) + 20rpx) 32rpx 40rpx;
  background: $bg-primary;
}

/* 头部操作按钮 */
.header-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.action-btn {
  position: relative;
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge {
  position: absolute;
  top: 0;
  right: 0;
  min-width: 28rpx;
  height: 28rpx;
  line-height: 28rpx;
  text-align: center;
  background: #e74c3c;
  color: #fff;
  font-size: 18rpx;
  border-radius: 14rpx;
  padding: 0 6rpx;
}

/* 用户信息 */
.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 40rpx;
}

.avatar-wrap {
  position: relative;
  margin-right: 24rpx;
}

.user-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba($accent-gold, 0.3);
  background: $bg-elevated;
}

.avatar-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 36rpx;
  height: 36rpx;
  background: $accent-gold;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid $bg-primary;
}

.user-detail {
  flex: 1;
}

.user-name {
  display: block;
  font-size: 40rpx;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 8rpx;
}

.user-id {
  display: block;
  font-size: 24rpx;
  color: $text-secondary;
  margin-bottom: 12rpx;
}

.user-tags {
  display: flex;
  gap: 12rpx;
}

.user-tag {
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-size: 20rpx;
  font-weight: 500;
}

.user-tag.artist {
  background: rgba($accent-gold, 0.2);
  color: $accent-gold;
}

.user-tag.promoter {
  background: rgba(52, 152, 219, 0.2);
  color: #3498db;
}

.user-tag.collector {
  background: $bg-elevated;
  color: $text-secondary;
}

/* 用户数据统计 */
.user-stats {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 0 20rpx;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.stat-value {
  font-size: 36rpx;
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

/* 身份中心入口 */
.identity-section {
  padding: 0 32rpx;
  margin-top: 32rpx;
}

.identity-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $bg-card;
  border: 1rpx solid rgba($accent-gold, 0.3);
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
}

.card-left {
  display: flex;
  align-items: center;
}

.card-icon {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20rpx;
  margin-right: 20rpx;
  background: rgba($accent-gold, 0.15);
}

.card-info {
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 8rpx;
}

.card-subtitle {
  font-size: 24rpx;
  color: $text-secondary;
}

.card-arrow {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 功能网格 */
.function-section {
  padding: 32rpx;
}

.function-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24rpx;
}

.grid-item {
  background: $bg-card;
  border-radius: 20rpx;
  padding: 32rpx 28rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.grid-icon {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16rpx;
}

.grid-name {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
}

/* 底部占位 */
.page-footer {
  height: 40rpx;
}
</style>
