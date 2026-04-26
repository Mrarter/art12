<template>
  <view class="user-page">
    <!-- 顶部渐变背景 -->
    <view class="header-bg"></view>

    <!-- 顶部导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-left">
        <view class="nav-btn icon-btn" @click="goInspiration">
          <text class="icon-lightbulb">💡</text>
          <text class="nav-text">创意灵感</text>
        </view>
      </view>
      <view class="nav-right">
        <view class="nav-btn icon-btn" @click="goScan">
          <text class="icon-scan">🔍</text>
        </view>
      </view>
    </view>

    <!-- 用户信息区域 -->
    <view class="user-section" @click="handleUserClick">
      <!-- 未登录状态 -->
      <view class="login-state" v-if="!userStore.isAuthenticated">
        <view class="login-avatar">
          <image class="avatar-img" src="/static/avatar/default.jpg" mode="aspectFill"></image>
        </view>
        <view class="login-text">
          <text class="login-title">登录 / 注册</text>
        </view>
      </view>

      <!-- 已登录状态 -->
      <view class="user-logined" v-else>
        <view class="avatar-container">
          <image
            class="user-avatar"
            :src="userInfo.avatar || '/static/avatar/default.jpg'"
            mode="aspectFill"
          ></image>
          <view class="avatar-vip" v-if="isVip">
            <text>VIP</text>
          </view>
        </view>

        <view class="user-name">{{ userInfo.nickname || '用户' }}</view>

        <view class="user-meta">
          <text class="uid">ID: {{ formatUid(userInfo.uid) }}</text>
          <view class="credit-badge" v-if="creditScore > 0">
            <text class="credit-icon">🛡</text>
            <text class="credit-text">信用 {{ creditScore }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 社交数据 -->
    <view class="social-stats" v-if="userStore.isAuthenticated">
      <view class="stat-item" @click="goFollowing">
        <text class="stat-value">{{ formatCount(userStats.following) }}</text>
        <text class="stat-label">关注</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item" @click="goFollowers">
        <text class="stat-value">{{ formatCount(userStats.followers) }}</text>
        <text class="stat-label">粉丝</text>
        <view class="red-dot" v-if="hasNewFollowers"></view>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item" @click="goMedals">
        <text class="stat-value">{{ medalCount }}</text>
        <text class="stat-label">勋章</text>
      </view>
    </view>

    <!-- 认证卡片 -->
    <view class="cert-section" v-if="userStore.isAuthenticated">
      <view class="cert-card" v-if="!userInfo.isArtist">
        <view class="cert-content">
          <text class="cert-title">认证艺术家</text>
          <text class="cert-desc">让更多人发现你的作品</text>
        </view>
        <view class="cert-btn" @click.stop="goCertApply">
          <text>去认证</text>
        </view>
      </view>

      <view class="cert-card artist" v-else>
        <view class="cert-content">
          <text class="cert-title">🎨 艺术家中心</text>
          <text class="cert-desc">管理作品，查看数据</text>
        </view>
        <view class="cert-btn" @click.stop="goArtistHome">
          <text>进入</text>
        </view>
      </view>
    </view>

    <!-- 资产统计 -->
    <view class="asset-section" v-if="userStore.isAuthenticated">
      <view class="asset-grid">
        <view class="asset-item" @click="goWallet">
          <text class="asset-value">{{ charcoalCount }}</text>
          <text class="asset-label">炭粒</text>
        </view>
        <view class="asset-item" @click="goCoupon">
          <text class="asset-value">{{ couponCount }}</text>
          <text class="asset-label">优惠券</text>
        </view>
        <view class="asset-item" @click="goFavorites">
          <text class="asset-value">{{ markedCount }}</text>
          <text class="asset-label">标记</text>
        </view>
        <view class="asset-item" @click="goCart">
          <text class="asset-value">{{ cartCount }}</text>
          <text class="asset-label">购物车</text>
        </view>
      </view>
    </view>

    <!-- 交易服务 -->
    <view class="transaction-section">
      <view class="trans-grid">
        <view class="trans-item" @click="goOrderList('bought')">
          <text class="trans-icon">🛍</text>
          <text class="trans-name">我买到的</text>
        </view>
        <view class="trans-item" @click="goOrderList('sold')">
          <text class="trans-icon">📤</text>
          <text class="trans-name">我卖出的</text>
        </view>
        <view class="trans-item" @click="goAfterSale">
          <text class="trans-icon">🔧</text>
          <text class="trans-name">售后</text>
        </view>
        <view class="trans-item" @click="goMyReviews">
          <text class="trans-icon">⭐</text>
          <text class="trans-name">评价</text>
        </view>
      </view>
    </view>

    <!-- 活动卡片 -->
    <view class="activity-section" v-if="userStore.isAuthenticated">
      <view class="activity-grid">
        <view class="activity-card" @click="goSignIn">
          <view class="activity-left">
            <text class="activity-title">🎁 签到</text>
            <text class="activity-desc">连续签到领积分</text>
          </view>
          <view class="sign-btn" v-if="!isSignedToday">
            <text>去签到</text>
          </view>
          <view class="signed-badge" v-else>
            <text>已签到</text>
          </view>
        </view>

        <view class="activity-card" @click="goCertificate">
          <view class="activity-left">
            <text class="activity-title">🏆 收藏证书</text>
            <text class="activity-desc">查看藏品认证</text>
          </view>
          <view class="cert-count">
            <text>{{ certificateCount }}份</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 功能入口列表 -->
    <view class="menu-section">
      <view class="menu-grid">
        <view class="menu-item" @click="goHistory">
          <text class="menu-icon">📜</text>
          <text class="menu-text">浏览记录</text>
        </view>
        <view class="menu-item" @click="goLiked">
          <text class="menu-icon">👍</text>
          <text class="menu-text">我赞过的</text>
        </view>
        <view class="menu-item" @click="goMedalHall">
          <text class="menu-icon">🏅</text>
          <text class="menu-text">勋章馆</text>
        </view>
      </view>
    </view>

    <!-- 更多服务 -->
    <view class="more-section">
      <view class="more-list">
        <view class="more-item" @click="goAddress">
          <text class="more-icon">📍</text>
          <text class="more-text">收货地址</text>
          <text class="more-arrow">›</text>
        </view>
        <view class="more-item" @click="goHelp">
          <text class="more-icon">❓</text>
          <text class="more-text">帮助中心</text>
          <text class="more-arrow">›</text>
        </view>
        <view class="more-item" @click="goFeedback">
          <text class="more-icon">💬</text>
          <text class="more-text">意见反馈</text>
          <text class="more-arrow">›</text>
        </view>
        <view class="more-item" @click="goAbout">
          <text class="more-icon">ℹ️</text>
          <text class="more-text">关于我们</text>
          <text class="more-arrow">›</text>
        </view>
        <view class="more-item last" @click="goSettings">
          <text class="more-icon">⚙️</text>
          <text class="more-text">设置</text>
          <text class="more-arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 底部安全区 -->
    <view class="safe-area-bottom"></view>
    
    <!-- 自定义TabBar -->
    <CustomTabBar :currentIndex="4" />
  </view>
</template>

<script>
import CustomTabBar from '@/components/custom-tab-bar/index.vue'
import { useUserStore } from '@/store/modules/user.js'
import { getOrderCounts } from '@/api/order.js'

export default {
  components: {
    CustomTabBar
  },
  data() {
    return {
      statusBarHeight: 20,
      isVip: false,
      creditScore: 0,
      charcoalCount: 0,
      couponCount: 0,
      markedCount: 0,
      cartCount: 0,
      certificateCount: 0,
      medalCount: 0,
      hasNewFollowers: false,
      isSignedToday: false,
      userStats: {
        favorites: 0,
        following: 0,
        followers: 0
      },
      orderCounts: {
        pending: 0,
        paid: 0,
        shipped: 0
      }
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
      const systemInfo = uni.getSystemInfoSync()
      this.statusBarHeight = systemInfo.statusBarHeight || 20
      this.userStore.initUserInfo()

      if (this.userStore.isAuthenticated) {
        this.loadUserStats()
        this.loadOrderCounts()
      }
    },

    async loadUserStats() {
      // 模拟数据
      this.userStats = {
        favorites: 128,
        following: 45,
        followers: 1234
      }
      this.charcoalCount = 580
      this.couponCount = 3
      this.markedCount = 56
      this.cartCount = 2
      this.certificateCount = 12
      this.medalCount = 8
      this.creditScore = 850
      this.isSignedToday = false
      this.hasNewFollowers = true
      this.isVip = false
    },

    async loadOrderCounts() {
      try {
        const res = await getOrderCounts()
        this.orderCounts = { ...this.orderCounts, ...res }
      } catch (e) {
        console.log('获取订单数量失败', e)
      }
    },

    handleUserClick() {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
      }
    },

    goInspiration() {
      uni.navigateTo({ url: '/pages/inspiration/index' })
    },

    goScan() {
      uni.scanCode({
        success: (res) => {
          console.log('扫码结果', res)
        }
      })
    },

    goArtistHome() {
      uni.navigateTo({ url: `/pages/artist/home?id=${this.userInfo.id}` })
    },

    goCertApply() {
      uni.navigateTo({ url: '/pages/artist/cert' })
    },

    goSettings() {
      uni.navigateTo({ url: '/pages/user/settings' })
    },

    goFollowing() {
      uni.navigateTo({ url: '/pages/user/following' })
    },

    goFollowers() {
      uni.navigateTo({ url: '/pages/user/followers' })
    },

    goMedals() {
      uni.navigateTo({ url: '/pages/user/medals' })
    },

    goWallet() {
      uni.navigateTo({ url: '/pages/user/wallet' })
    },

    goCoupon() {
      uni.navigateTo({ url: '/pages/user/coupon' })
    },

    goFavorites() {
      uni.navigateTo({ url: '/pages/user/favorites' })
    },

    goCart() {
      uni.switchTab({ url: '/pages/cart/index' })
    },

    goOrderList(type) {
      uni.navigateTo({ url: `/pages/order/list?type=${type}` })
    },

    goAfterSale() {
      uni.navigateTo({ url: '/pages/order/after-sale' })
    },

    goMyReviews() {
      uni.navigateTo({ url: '/pages/user/my-reviews' })
    },

    goSignIn() {
      uni.navigateTo({ url: '/pages/signin/index' })
    },

    goCertificate() {
      uni.navigateTo({ url: '/pages/certificate/list' })
    },

    goHistory() {
      uni.navigateTo({ url: '/pages/user/history' })
    },

    goLiked() {
      uni.navigateTo({ url: '/pages/user/liked' })
    },

    goMedalHall() {
      uni.navigateTo({ url: '/pages/medal/hall' })
    },

    goAddress() {
      uni.navigateTo({ url: '/pages/user/address' })
    },

    goHelp() {
      uni.navigateTo({ url: '/pages/help/index' })
    },

    goFeedback() {
      uni.navigateTo({ url: '/pages/user/feedback' })
    },

    goAbout() {
      uni.navigateTo({ url: '/pages/about/index' })
    },

    formatUid(uid) {
      if (!uid) return '------'
      return uid.toString().padStart(6, '0')
    },

    formatCount(count) {
      if (!count) return '0'
      if (count >= 10000) {
        return (count / 10000).toFixed(1) + 'w'
      }
      if (count >= 1000) {
        return (count / 1000).toFixed(1) + 'k'
      }
      return count.toString()
    }
  }
}
</script>

<style lang="scss" scoped>
// 深色艺术主题变量
$bg-primary: #0a0a0a;
$bg-card: #1a1a1a;
$bg-elevated: #242424;
$text-primary: #ffffff;
$text-secondary: #999999;
$text-muted: #666666;
$accent-gold: #c9a227;
$border-color: rgba(255, 255, 255, 0.06);
$divider-color: rgba(255, 255, 255, 0.08);

.user-page {
  min-height: 100vh;
  background: $bg-primary;
  padding-bottom: env(safe-area-inset-bottom);
  position: relative;
}

/* 顶部渐变背景 */
.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 500rpx;
  background: linear-gradient(
    180deg,
    rgba($accent-gold, 0.08) 0%,
    $bg-primary 100%
  );
  pointer-events: none;
}

/* 导航栏 */
.nav-bar {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 32rpx;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn {
  padding: 12rpx 20rpx;
  background: $bg-card;
  border-radius: 24rpx;
  gap: 8rpx;

  .nav-text {
    font-size: 26rpx;
    color: $text-primary;
    font-weight: 500;
  }

  .icon-lightbulb,
  .icon-scan {
    font-size: 28rpx;
  }
}

/* 用户信息区域 */
.user-section {
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx 32rpx 20rpx;
}

/* 未登录状态 */
.login-state {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.login-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  overflow: hidden;
  background: $bg-card;

  .avatar-img {
    width: 100%;
    height: 100%;
  }
}

.login-text {
  .login-title {
    font-size: 36rpx;
    font-weight: 600;
    color: $text-primary;
  }
}

/* 已登录状态 */
.user-logined {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-container {
  position: relative;
  width: 140rpx;
  height: 140rpx;
  margin-bottom: 20rpx;
}

.user-avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 4rpx solid rgba($accent-gold, 0.4);
  background: $bg-card;
}

.avatar-vip {
  position: absolute;
  bottom: -8rpx;
  left: 50%;
  transform: translateX(-50%);
  padding: 4rpx 16rpx;
  background: linear-gradient(135deg, #ffd700, #ffb347);
  border-radius: 12rpx;
  font-size: 18rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.user-name {
  font-size: 40rpx;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 12rpx;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;

  .uid {
    font-size: 24rpx;
    color: $text-muted;
    font-family: 'Courier New', monospace;
  }

  .credit-badge {
    display: flex;
    align-items: center;
    gap: 6rpx;
    padding: 6rpx 14rpx;
    background: rgba(#2ecc71, 0.15);
    border-radius: 12rpx;

    .credit-icon {
      font-size: 20rpx;
    }

    .credit-text {
      font-size: 20rpx;
      color: #2ecc71;
      font-weight: 500;
    }
  }
}

/* 社交数据 */
.social-stats {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24rpx 32rpx;
  margin: 0 32rpx;
  background: $bg-card;
  border-radius: 20rpx;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  position: relative;

  .stat-value {
    font-size: 36rpx;
    font-weight: 700;
    color: $text-primary;
  }

  .stat-label {
    font-size: 24rpx;
    color: $text-secondary;
  }
}

.stat-divider {
  width: 1rpx;
  height: 40rpx;
  background: $divider-color;
}

.red-dot {
  position: absolute;
  top: -4rpx;
  right: 30%;
  width: 12rpx;
  height: 12rpx;
  background: #ff4757;
  border-radius: 50%;
}

/* 认证卡片 */
.cert-section {
  position: relative;
  z-index: 10;
  padding: 20rpx 32rpx 0;
}

.cert-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  background: $bg-card;
  border-radius: 20rpx;
  border: 1rpx solid rgba(#2ecc71, 0.2);

  &.artist {
    border-color: rgba($accent-gold, 0.2);
  }
}

.cert-content {
  .cert-title {
    display: block;
    font-size: 32rpx;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 6rpx;
  }

  .cert-desc {
    font-size: 24rpx;
    color: $text-muted;
  }
}

.cert-btn {
  padding: 12rpx 28rpx;
  background: linear-gradient(135deg, #2ecc71, #27ae60);
  border-radius: 24rpx;
  font-size: 26rpx;
  font-weight: 500;
  color: #ffffff;

  &:active {
    opacity: 0.8;
  }
}

.cert-card.artist .cert-btn {
  background: linear-gradient(135deg, $accent-gold, #b8941f);
}

/* 资产统计 */
.asset-section {
  position: relative;
  z-index: 10;
  padding: 20rpx 32rpx 0;
}

.asset-grid {
  display: flex;
  background: $bg-card;
  border-radius: 20rpx;
  overflow: hidden;
}

.asset-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28rpx 0;
  position: relative;

  &:not(:last-child)::after {
    content: '';
    position: absolute;
    right: 0;
    top: 20%;
    height: 60%;
    width: 1rpx;
    background: $divider-color;
  }

  .asset-value {
    font-size: 36rpx;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 8rpx;
  }

  .asset-label {
    font-size: 24rpx;
    color: $text-secondary;
  }
}

/* 交易服务 */
.transaction-section {
  position: relative;
  z-index: 10;
  padding: 20rpx 32rpx 0;
}

.trans-grid {
  display: flex;
  background: $bg-card;
  border-radius: 20rpx;
  overflow: hidden;
}

.trans-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28rpx 0;
  position: relative;

  &:not(:last-child)::after {
    content: '';
    position: absolute;
    right: 0;
    top: 20%;
    height: 60%;
    width: 1rpx;
    background: $divider-color;
  }

  .trans-icon {
    font-size: 40rpx;
    margin-bottom: 12rpx;
  }

  .trans-name {
    font-size: 24rpx;
    color: $text-secondary;
  }
}

/* 活动卡片 */
.activity-section {
  position: relative;
  z-index: 10;
  padding: 20rpx 32rpx 0;
}

.activity-grid {
  display: flex;
  gap: 16rpx;
}

.activity-card {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  background: $bg-card;
  border-radius: 16rpx;
}

.activity-left {
  .activity-title {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 6rpx;
  }

  .activity-desc {
    font-size: 22rpx;
    color: $text-muted;
  }
}

.sign-btn {
  padding: 10rpx 20rpx;
  background: linear-gradient(135deg, $accent-gold, #b8941f);
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 500;
  color: #1a1a1a;
}

.signed-badge {
  padding: 10rpx 20rpx;
  background: rgba($accent-gold, 0.15);
  border-radius: 20rpx;
  font-size: 22rpx;
  color: $accent-gold;
}

.cert-count {
  padding: 10rpx 20rpx;
  background: rgba(231, 76, 60, 0.15);
  border-radius: 20rpx;
  font-size: 22rpx;
  color: #e74c3c;
}

/* 功能入口 */
.menu-section {
  position: relative;
  z-index: 10;
  padding: 20rpx 32rpx 0;
}

.menu-grid {
  display: flex;
  background: $bg-card;
  border-radius: 20rpx;
  overflow: hidden;
}

.menu-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28rpx 0;
  position: relative;

  &:not(:last-child)::after {
    content: '';
    position: absolute;
    right: 0;
    top: 20%;
    height: 60%;
    width: 1rpx;
    background: $divider-color;
  }

  .menu-icon {
    font-size: 36rpx;
    margin-bottom: 10rpx;
  }

  .menu-text {
    font-size: 24rpx;
    color: $text-secondary;
  }
}

/* 更多服务 */
.more-section {
  position: relative;
  z-index: 10;
  padding: 20rpx 32rpx 0;
}

.more-list {
  background: $bg-card;
  border-radius: 20rpx;
  overflow: hidden;
}

.more-item {
  display: flex;
  align-items: center;
  padding: 32rpx 28rpx;
  border-bottom: 1rpx solid $divider-color;

  &:active {
    background: $bg-elevated;
  }

  &.last {
    border-bottom: none;
  }

  .more-icon {
    font-size: 32rpx;
    margin-right: 20rpx;
  }

  .more-text {
    flex: 1;
    font-size: 30rpx;
    color: $text-primary;
  }

  .more-arrow {
    font-size: 28rpx;
    color: $text-muted;
  }
}

/* 安全区 */
.safe-area-bottom {
  height: calc(120rpx + 100rpx + env(safe-area-inset-bottom));
}
</style>
