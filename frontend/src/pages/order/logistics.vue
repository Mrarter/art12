<template>
  <view class="logistics-page">
    <!-- 物流头部 -->
    <view class="logistics-header">
      <view class="express-info">
        <text class="express-name">{{ logisticsInfo.expressName }}</text>
        <text class="express-no">{{ logisticsInfo.expressNo }}</text>
      </view>
      <view class="delivery-status" :class="statusClass">
        <text class="status-text">{{ logisticsInfo.statusText }}</text>
      </view>
    </view>

    <!-- 收货信息 -->
    <view class="receiver-info">
      <view class="receiver-avatar">
        
      </view>
      <view class="receiver-detail">
        <text class="receiver-name">{{ logisticsInfo.receiver }}</text>
        <text class="receiver-phone">{{ logisticsInfo.phone }}</text>
        <text class="receiver-address">{{ logisticsInfo.address }}</text>
      </view>
    </view>

    <!-- 物流时间线 -->
    <view class="timeline-section">
      <view class="timeline-header">
        <text class="header-title">物流动态</text>
      </view>
      
      <view class="timeline">
        <view 
          class="timeline-item" 
          v-for="(item, index) in logisticsInfo.traces" 
          :key="index"
          :class="{ current: index === 0 }"
        >
          <view class="timeline-dot">
            <view class="dot" v-if="index === 0"></view>
            <view class="line" v-else></view>
          </view>
          <view class="timeline-content">
            <text class="trace-desc">{{ item.desc }}</text>
            <text class="trace-time">{{ item.time }}</text>
            <text class="trace-location" v-if="item.location">{{ item.location }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 商品信息 -->
    <view class="goods-section">
      <view class="goods-header">
        <text class="header-title">商品信息</text>
      </view>
      <view class="goods-list">
        <view class="goods-item" v-for="(item, index) in orderInfo.goods" :key="index">
          <image :src="item.cover" mode="aspectFill" class="goods-cover"></image>
          <view class="goods-info">
            <text class="goods-title">{{ item.title }}</text>
            <text class="goods-spec">{{ item.spec }}</text>
            <view class="goods-price">
              <text class="price">¥{{ item.price }}</text>
              <text class="count">x{{ item.count }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部操作 -->
    <view class="logistics-footer">
      <button class="action-btn copy" @click="copyExpressNo">
        
        <text>复制单号</text>
      </button>
      <button class="action-btn contact" @click="contactExpress">
        
        <text>联系快递</text>
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const logisticsInfo = ref({
  expressName: '顺丰速运',
  expressNo: 'SF1234567890123',
  statusText: '派送中',
  statusCode: 'delivering',
  receiver: '张三',
  phone: '138****8888',
  address: '广东省深圳市南山区科技园xx路xx号',
  traces: [
    {
      desc: '您的订单已由【深圳科技园营业部】收取，正在派送途中，请您保持手机畅通',
      time: '2024-01-15 14:30:22',
      location: '深圳'
    },
    {
      desc: '快件已到达【深圳科技园营业部】',
      time: '2024-01-15 09:15:00',
      location: '深圳'
    },
    {
      desc: '快件已从【深圳分拨中心】发出，正在送往【深圳科技园营业部】',
      time: '2024-01-15 02:30:00',
      location: '深圳'
    },
    {
      desc: '快件已到达【深圳分拨中心】',
      time: '2024-01-14 22:00:00',
      location: '深圳'
    },
    {
      desc: '快件已从【广州分拨中心】发出，正在送往【深圳分拨中心】',
      time: '2024-01-14 18:30:00',
      location: '广州'
    },
    {
      desc: '快件已到达【广州分拨中心】',
      time: '2024-01-14 15:00:00',
      location: '广州'
    },
    {
      desc: '商家已发货，快件正在运输中',
      time: '2024-01-14 10:30:00',
      location: ''
    }
  ]
})

const orderInfo = ref({
  orderNo: 'ORD202401011234567890',
  goods: [
    {
      cover: 'https://pic.imgdb.cn/item/1.jpg',
      title: '山水意境·云起',
      spec: '原作/带框',
      price: '8888',
      count: 1
    }
  ]
})

const statusClass = computed(() => {
  const map = {
    pending: 'pending',
    delivering: 'delivering',
    completed: 'completed'
  }
  return map[logisticsInfo.value.statusCode] || ''
})

const copyExpressNo = () => {
  uni.setClipboardData({
    data: logisticsInfo.value.expressNo,
    success: () => {
      uni.showToast({ title: '单号已复制', icon: 'success' })
    }
  })
}

const contactExpress = () => {
  uni.makePhoneCall({
    phoneNumber: '95338', // 顺丰客服电话
    fail: () => {
      uni.showToast({ title: '拨打失败', icon: 'none' })
    }
  })
}

onMounted(() => {
  // 获取物流信息
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}
  
  if (options.orderId) {
    // 实际应该从API获取物流信息
    orderInfo.value.orderNo = options.orderId
  }
})
</script>

<style lang="scss" scoped>
.logistics-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 160rpx;
}

.logistics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 40rpx 30rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;

  .express-info {
    .express-name {
      font-size: 32rpx;
      font-weight: 600;
      display: block;
      margin-bottom: 12rpx;
    }

    .express-no {
      font-size: 26rpx;
      opacity: 0.9;
    }
  }

  .delivery-status {
    padding: 12rpx 24rpx;
    border-radius: 32rpx;
    background: rgba(255,255,255,0.2);

    &.pending {
      background: rgba(255,193,7,0.3);
    }

    &.delivering {
      background: rgba(76,175,80,0.3);
    }

    &.completed {
      background: rgba(33,150,243,0.3);
    }

    .status-text {
      font-size: 26rpx;
    }
  }
}

.receiver-info {
  display: flex;
  align-items: flex-start;
  padding: 30rpx;
  background: #fff;
  margin-bottom: 20rpx;

  .receiver-avatar {
    width: 80rpx;
    height: 80rpx;
    background: #f0f2f5;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;
  }

  .receiver-detail {
    flex: 1;

    .receiver-name {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
      margin-right: 16rpx;
    }

    .receiver-phone {
      font-size: 28rpx;
      color: #666;
    }

    .receiver-address {
      font-size: 26rpx;
      color: #999;
      margin-top: 12rpx;
      display: block;
    }
  }
}

.timeline-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;

  .timeline-header {
    margin-bottom: 30rpx;

    .header-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }
  }
}

.timeline {
  .timeline-item {
    display: flex;
    padding-bottom: 40rpx;

    &:last-child {
      padding-bottom: 0;
    }

    &.current {
      .timeline-dot {
        .dot {
          background: #667eea;
          box-shadow: 0 0 0 6rpx rgba(102,126,234,0.2);
        }
      }

      .timeline-content {
        .trace-desc {
          color: #333;
          font-weight: 500;
        }
      }
    }

    .timeline-dot {
      width: 40rpx;
      display: flex;
      flex-direction: column;
      align-items: center;

      .dot {
        width: 20rpx;
        height: 20rpx;
        background: #ddd;
        border-radius: 50%;
      }

      .line {
        width: 4rpx;
        flex: 1;
        background: #e0e0e0;
      }
    }

    .timeline-content {
      flex: 1;
      margin-left: 20rpx;
      padding-bottom: 10rpx;

      .trace-desc {
        font-size: 26rpx;
        color: #666;
        line-height: 1.5;
        display: block;
        margin-bottom: 12rpx;
      }

      .trace-time {
        font-size: 24rpx;
        color: #999;
        display: block;
      }

      .trace-location {
        font-size: 22rpx;
        color: #bbb;
        display: block;
        margin-top: 8rpx;
      }
    }
  }
}

.goods-section {
  background: #fff;
  padding: 30rpx;

  .goods-header {
    margin-bottom: 30rpx;

    .header-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }
  }

  .goods-item {
    display: flex;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .goods-cover {
      width: 160rpx;
      height: 160rpx;
      border-radius: 12rpx;
      margin-right: 20rpx;
    }

    .goods-info {
      flex: 1;

      .goods-title {
        font-size: 28rpx;
        color: #333;
        font-weight: 500;
        display: block;
        margin-bottom: 12rpx;
      }

      .goods-spec {
        font-size: 24rpx;
        color: #999;
        display: block;
        margin-bottom: 16rpx;
      }

      .goods-price {
        display: flex;
        justify-content: space-between;

        .price {
          font-size: 28rpx;
          color: #ff4d4f;
          font-weight: 600;
        }

        .count {
          font-size: 26rpx;
          color: #999;
        }
      }
    }
  }
}

.logistics-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 20rpx;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 20rpx rgba(0,0,0,0.05);

  .action-btn {
    flex: 1;
    height: 88rpx;
    border-radius: 44rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12rpx;
    font-size: 28rpx;
    border: none;

    &::after {
      border: none;
    }

    &.copy {
      background: #f0f2f5;
      color: #667eea;
    }

    &.contact {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
    }
  }
}
</style>