<template>
  <view class="coupon-page">
    <!-- Tab切换 -->
    <view class="tab-header">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'available' }"
        @click="switchTab('available')"
      >
        可用({{ availableCount }})
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'used' }"
        @click="switchTab('used')"
      >
        已使用({{ usedCount }})
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'expired' }"
        @click="switchTab('expired')"
      >
        已过期({{ expiredCount }})
      </view>
    </view>

    <!-- 优惠券列表 -->
    <scroll-view class="coupon-list" scroll-y>
      <view 
        class="coupon-item"
        :class="{ disabled: currentTab !== 'available' }"
        v-for="item in filteredCoupons"
        :key="item.id"
      >
        <view class="coupon-left">
          <view class="price-section">
            <text class="currency">¥</text>
            <text class="amount">{{ item.value }}</text>
          </view>
          <text class="coupon-type">{{ item.typeText }}</text>
        </view>
        
        <view class="coupon-right">
          <view class="coupon-info">
            <text class="coupon-name">{{ item.name }}</text>
            <text class="coupon-desc" v-if="item.condition">{{ item.condition }}</text>
            <text class="coupon-time">{{ item.startTime }} - {{ item.endTime }}</text>
          </view>
          <view class="coupon-action" v-if="currentTab === 'available'">
            <button class="use-btn" @click="useCoupon(item)">立即使用</button>
          </view>
          <view class="coupon-status" v-else>
            <text>{{ currentTab === 'used' ? '已使用' : '已过期' }}</text>
          </view>
        </view>
        
        <view class="coupon-decorator">
          <view class="circle top"></view>
          <view class="dashed-line"></view>
          <view class="circle bottom"></view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="filteredCoupons.length === 0">
        <image class="empty-icon" src="/static/icons/empty-coupon.png" mode="aspectFit"></image>
        <text class="empty-text">{{ emptyText }}</text>
      </view>
    </scroll-view>

    <!-- 领取优惠券按钮 -->
    <view class="bottom-bar" v-if="currentTab === 'available'">
      <button class="get-coupon-btn" @click="goGetCoupon">
        <text>领更多优惠券</text>
      </button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentTab: 'available',
      coupons: [
        {
          id: 1,
          name: '新人专享券',
          value: 100,
          typeText: '满500可用',
          condition: '全场通用',
          startTime: '2026-04-01',
          endTime: '2026-04-30',
          status: 'available'
        },
        {
          id: 2,
          name: '限时折扣券',
          value: 200,
          typeText: '满1000可用',
          condition: '限指定艺术品',
          startTime: '2026-04-15',
          endTime: '2026-04-25',
          status: 'available'
        },
        {
          id: 3,
          name: '会员专享券',
          value: 500,
          typeText: '满3000可用',
          condition: '限VIP会员',
          startTime: '2026-04-01',
          endTime: '2026-05-31',
          status: 'available'
        },
        {
          id: 4,
          name: '拍卖专用券',
          value: 1000,
          typeText: '满5000可用',
          condition: '限拍卖专场',
          startTime: '2026-04-01',
          endTime: '2026-06-30',
          status: 'available'
        },
        {
          id: 5,
          name: '艺术家专享',
          value: 50,
          typeText: '无门槛',
          condition: '限指定艺术家',
          startTime: '2026-03-01',
          endTime: '2026-03-31',
          status: 'used'
        },
        {
          id: 6,
          name: '节日特惠券',
          value: 80,
          typeText: '满400可用',
          condition: '全场通用',
          startTime: '2026-02-01',
          endTime: '2026-02-28',
          status: 'expired'
        }
      ]
    }
  },

  computed: {
    filteredCoupons() {
      return this.coupons.filter(c => c.status === this.currentTab)
    },
    availableCount() {
      return this.coupons.filter(c => c.status === 'available').length
    },
    usedCount() {
      return this.coupons.filter(c => c.status === 'used').length
    },
    expiredCount() {
      return this.coupons.filter(c => c.status === 'expired').length
    },
    emptyText() {
      const texts = {
        available: '暂无可用优惠券',
        used: '暂无已使用优惠券',
        expired: '暂无已过期优惠券'
      }
      return texts[this.currentTab]
    }
  },

  methods: {
    switchTab(tab) {
      this.currentTab = tab
    },

    useCoupon(item) {
      uni.switchTab({ url: '/pages/index/index' })
    },

    goGetCoupon() {
      uni.showToast({ title: '优惠券中心开发中', icon: 'none' })
    }
  }
}
</script>

<style lang="scss" scoped>
.coupon-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

.tab-header {
  display: flex;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;

  .tab-item {
    flex: 1;
    height: 96rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    color: #666;
    position: relative;

    &.active {
      color: #667eea;
      font-weight: 600;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 60rpx;
        height: 4rpx;
        background: #667eea;
        border-radius: 2rpx;
      }
    }
  }
}

.coupon-list {
  padding: 20rpx;

  .coupon-item {
    display: flex;
    background: #fff;
    border-radius: 16rpx;
    overflow: hidden;
    margin-bottom: 20rpx;

    &.disabled {
      opacity: 0.6;

      .coupon-left {
        background: #ccc;
      }
    }

    .coupon-left {
      width: 220rpx;
      background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 30rpx 20rpx;
      position: relative;

      .price-section {
        display: flex;
        align-items: baseline;

        .currency {
          font-size: 28rpx;
          color: #fff;
        }

        .amount {
          font-size: 60rpx;
          font-weight: 700;
          color: #fff;
        }
      }

      .coupon-type {
        font-size: 22rpx;
        color: rgba(255, 255, 255, 0.9);
        margin-top: 8rpx;
      }
    }

    .coupon-right {
      flex: 1;
      padding: 24rpx;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      .coupon-info {
        .coupon-name {
          font-size: 30rpx;
          font-weight: 600;
          color: #333;
          display: block;
          margin-bottom: 8rpx;
        }

        .coupon-desc {
          font-size: 24rpx;
          color: #666;
          display: block;
          margin-bottom: 6rpx;
        }

        .coupon-time {
          font-size: 22rpx;
          color: #999;
        }
      }

      .use-btn {
        width: 140rpx;
        height: 56rpx;
        background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
        color: #fff;
        font-size: 24rpx;
        border-radius: 28rpx;
        border: none;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .coupon-status {
        text {
          font-size: 24rpx;
          color: #999;
        }
      }
    }

    .coupon-decorator {
      position: absolute;
      right: 200rpx;
      top: 0;
      bottom: 0;
      width: 20rpx;

      .circle {
        position: absolute;
        width: 24rpx;
        height: 24rpx;
        background: #f5f5f5;
        border-radius: 50%;
        left: -12rpx;

        &.top {
          top: -12rpx;
        }

        &.bottom {
          bottom: -12rpx;
        }
      }

      .dashed-line {
        position: absolute;
        top: 0;
        bottom: 0;
        left: -1rpx;
        border-left: 2rpx dashed #f0f0f0;
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;

  .empty-icon {
    width: 200rpx;
    height: 200rpx;
    opacity: 0.5;
  }

  .empty-text {
    font-size: 28rpx;
    color: #999;
    margin-top: 20rpx;
  }
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.05);

  .get-coupon-btn {
    width: 100%;
    height: 88rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 32rpx;
    border-radius: 44rpx;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
