<template>
  <view class="custom-tab-bar">
    <view
      v-for="(item, index) in tabList"
      :key="index"
      class="tab-item"
      :class="{ active: currentIndex === index }"
      @click="switchTab(item, index)"
    >
      <view class="tab-icon-wrap">
        <image
          class="tab-icon"
          :src="currentIndex === index ? item.selectedIcon : item.icon"
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
        { pagePath: '/pages/gallery/index', text: '画廊', icon: '/static/tabbar/gallery-luxury.png', selectedIcon: '/static/tabbar/gallery-luxury-active.png', badge: 0 },
        { pagePath: '/pages/auction/index', text: '拍卖', icon: '/static/tabbar/auction-luxury.png', selectedIcon: '/static/tabbar/auction-luxury-active.png', badge: 0 },
        { pagePath: '/pages/cart/index', text: '购物车', icon: '/static/tabbar/cart-luxury.png', selectedIcon: '/static/tabbar/cart-luxury-active.png', badge: 0 },
        { pagePath: '/pages/user/index', text: '我的', icon: '/static/tabbar/user-luxury.png', selectedIcon: '/static/tabbar/user-luxury-active.png', badge: 0 }
      ]
    }
  },
  methods: {
    switchTab(item, index) {
      if (this.currentIndex !== index) {
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
