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
        <view class="tab-badge" v-if="item.badge && item.badge > 0">
          <text>{{ item.badge > 99 ? '99+' : item.badge }}</text>
        </view>
      </view>
      <text class="tab-text" :style="{ color: currentIndex === index ? selectedColor : color }">
        {{ item.text }}
      </text>
    </view>
  </view>
</template>

<script>
export default {
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
        {
          pagePath: '/pages/index/index',
          text: '首页',
          icon: '/static/tabbar/home-luxury.png',
          selectedIcon: '/static/tabbar/home-luxury-active.png',
          badge: 0
        },
        {
          pagePath: '/pages/gallery/index',
          text: '画廊',
          icon: '/static/tabbar/gallery-luxury.png',
          selectedIcon: '/static/tabbar/gallery-luxury-active.png',
          badge: 0
        },
        {
          pagePath: '/pages/auction/index',
          text: '拍卖',
          icon: '/static/tabbar/auction-luxury.png',
          selectedIcon: '/static/tabbar/auction-luxury-active.png',
          badge: 0
        },
        {
          pagePath: '/pages/cart/index',
          text: '购物车',
          icon: '/static/tabbar/cart-luxury.png',
          selectedIcon: '/static/tabbar/cart-luxury-active.png',
          badge: 0
        },
        {
          pagePath: '/pages/user/index',
          text: '我的',
          icon: '/static/tabbar/user-luxury.png',
          selectedIcon: '/static/tabbar/user-luxury-active.png',
          badge: 0
        }
      ]
    }
  },
  mounted() {
    // 监听购物车数量变化
    uni.$on('updateCartBadge', (count) => {
      this.tabList[3].badge = count
    })
  },
  beforeDestroy() {
    uni.$off('updateCartBadge')
  },
  methods: {
    switchTab(item, index) {
      if (this.currentIndex !== index) {
        uni.switchTab({
          url: item.pagePath
        })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.custom-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  align-items: flex-start;
  height: 100rpx;
  padding-top: 12rpx;
  padding-bottom: env(safe-area-inset-bottom);
  background-color: #1A1A1A;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
  z-index: 999;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  height: 100%;

  &.active {
    .tab-icon {
      transform: scale(1.1);
    }
  }
}

.tab-icon-wrap {
  position: relative;
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6rpx;
}

.tab-icon {
  width: 48rpx;
  height: 48rpx;
  transition: transform 0.2s ease;
}

.tab-badge {
  position: absolute;
  top: -4rpx;
  right: -8rpx;
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 6rpx;
  background: linear-gradient(135deg, #ff6b6b, #ee5a5a);
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    font-size: 18rpx;
    font-weight: 600;
    color: #ffffff;
    line-height: 1;
  }
}

.tab-text {
  font-size: 22rpx;
  font-weight: 500;
  line-height: 1;
}
</style>
