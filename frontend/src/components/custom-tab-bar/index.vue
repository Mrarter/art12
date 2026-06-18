<template>
  <view class="custom-tab-bar">
    <view
      v-for="item in visibleTabList"
      :key="item.pagePath"
      class="tab-item"
      :class="{ active: currentIndex === item.index }"
      @click="switchTab(item)"
    >
      <view class="tab-icon-wrap">
        <image
          class="tab-icon"
          :src="currentIndex === item.index ? item.selectedIcon : item.icon"
          mode="aspectFit"
        ></image>
        <view class="tab-badge" v-if="item.badge > 0">
          <text>{{ item.badge > 99 ? '99+' : item.badge }}</text>
        </view>
      </view>
      <text class="tab-text">{{ item.text }}</text>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '@/store/modules/user.js'

export default {
  name: 'CustomTabBar',
  props: {
    currentIndex: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      color: '#666666',
      selectedColor: '#D4AF37',
      tabList: [
        { pagePath: '/pages/index/index', text: '首页', icon: '/static/tabbar/home-luxury.png', selectedIcon: '/static/tabbar/home-luxury-active.png', badge: 0 },
        { pagePath: '/pages/auction/index', text: '拍卖', icon: '/static/tabbar/auction-luxury.png', selectedIcon: '/static/tabbar/auction-luxury-active.png', badge: 0 },
        { pagePath: '/pages/artist/publish', text: '发布', icon: '/static/tabbar/gallery-luxury.png', selectedIcon: '/static/tabbar/gallery-luxury-active.png', badge: 0, navigateType: 'navigateTo' },
        { pagePath: '/pages/cart/index', text: '购物车', icon: '/static/tabbar/cart-luxury.png', selectedIcon: '/static/tabbar/cart-luxury-active.png', badge: 0 },
        { pagePath: '/pages/user/index', text: '我的', icon: '/static/tabbar/user-luxury.png', selectedIcon: '/static/tabbar/user-luxury-active.png', badge: 0 }
      ]
    }
  },
  computed: {
    userStore() {
      return useUserStore()
    },
    isLoggedIn() {
      return this.userStore.isAuthenticated || this.userStore.isLogin
    },
    identities() {
      const raw = this.userStore.identities || this.userStore.userInfo?.identities || this.userStore.userInfo?.identity || this.userStore.userInfo?.currentIdentity
      if (Array.isArray(raw)) return raw.length ? raw : ['collector']
      if (typeof raw === 'string') {
        try {
          const parsed = JSON.parse(raw)
          return Array.isArray(parsed) ? parsed : raw.split(',').filter(Boolean)
        } catch (e) {
          return raw.split(',').filter(Boolean)
        }
      }
      return ['collector']
    },
    canShowPublish() {
      const userInfo = this.userStore.userInfo || {}
      const artistStatus = userInfo.artistStatus ?? userInfo.certStatus ?? this.userStore.centerData?.artistStatus
      if (artistStatus !== null && artistStatus !== undefined && artistStatus !== '') {
        return Number(artistStatus) === 1
      }
      const artistFlags = [
        userInfo.isArtist,
        userInfo.certifiedArtist,
        userInfo.artistCertified,
        this.userStore.isArtist
      ]
      return artistFlags.some(Boolean)
    },
    visibleTabList() {
      return this.tabList
        .map((item, index) => ({ ...item, index }))
        .filter(item => item.text !== '发布' || this.canShowPublish)
    }
  },
  methods: {
    switchTab(item) {
      if (item.navigateType === 'navigateTo') {
        uni.navigateTo({ url: item.pagePath })
        return
      }

      if (this.currentIndex !== item.index) {
        uni.switchTab({ url: item.pagePath })
      }
    }
  },
  mounted() {
    uni.$on('updateCartBadge', (count) => {
      this.tabList[3].badge = count
    })
  },
  beforeDestroy() {
    uni.$off('updateCartBadge')
  }
}
</script>

<style lang="scss" scoped>
.custom-tab-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: calc(100rpx + constant(safe-area-inset-bottom));
  height: calc(100rpx + env(safe-area-inset-bottom));
  display: flex;
  background-color: #1A1A1A;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
  z-index: 999;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;

  &.active .tab-icon {
    transform: scale(1.1);
  }

  &.active .tab-text {
    color: #D4AF37;
  }
}

.tab-icon-wrap {
  position: relative;
  width: 60rpx;
  height: 60rpx;
}

.tab-icon {
  width: 100%;
  height: 100%;
  transition: transform 0.2s ease;
}

.tab-badge {
  position: absolute;
  top: -6rpx;
  right: -10rpx;
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 6rpx;
  background: linear-gradient(135deg, #ff6b6b, #ee5a5a);
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;

  text {
    font-size: 18rpx;
    font-weight: 600;
    color: #fff;
  }
}

.tab-text {
  font-size: 20rpx;
  font-weight: 500;
  color: #666;
  line-height: 1;
  margin-top: 4rpx;
}
</style>
