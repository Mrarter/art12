<template>
  <view class="track-page">
    <!-- 物流信息头部 -->
    <view class="track-header card">
      <view class="express-info">
        <view class="express-company">
          <text class="label">承运公司：</text>
          <text class="value">{{ expressInfo.company }}</text>
        </view>
        <view class="express-no">
          <text class="label">运单号：</text>
          <text class="value" id="expressNo">{{ expressInfo.no }}</text>
          <text class="copy-btn" @click="copyNo">复制</text>
        </view>
      </view>
    </view>

    <!-- 物流状态时间线 -->
    <view class="track-timeline card">
      <view class="timeline-header">
        
        <text class="current-status">{{ currentStatus }}</text>
      </view>

      <view class="timeline-list">
        <view
          class="timeline-item"
          v-for="(item, index) in timeline"
          :key="index"
          :class="{ current: item.isCurrent, past: item.isPast }"
        >
          <view class="timeline-dot">
            <view class="dot-inner"></view>
          </view>
          <view class="timeline-content">
            <view class="timeline-status">{{ item.status }}</view>
            <view class="timeline-desc">{{ item.desc }}</view>
            <view class="timeline-time">{{ item.time }}</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 收货信息 -->
    <view class="receiver-info card">
      <view class="info-header">
        
        <text>收货信息</text>
      </view>
      <view class="info-content">
        <view class="receiver-row">
          <text class="label">收货人：</text>
          <text class="value">{{ receiverInfo.name }}</text>
        </view>
        <view class="receiver-row">
          <text class="label">联系电话：</text>
          <text class="value">{{ receiverInfo.phone }}</text>
        </view>
        <view class="receiver-row">
          <text class="label">收货地址：</text>
          <text class="value address">{{ receiverInfo.address }}</text>
        </view>
      </view>
    </view>

    <!-- 商品信息 -->
    <view class="product-info card">
      <view class="info-header">
        <text>🛍</text>
        <text>商品信息</text>
      </view>
      <view class="product-list">
        <view class="product-item" v-for="item in productList" :key="item.id">
          <image class="product-img" :src="item.cover" mode="aspectFill"></image>
          <view class="product-detail">
            <view class="product-title">{{ item.title }}</view>
            <view class="product-meta">{{ item.artist }}</view>
          </view>
          <view class="product-price">
            <text>¥{{ item.price }}</text>
            <text class="num">x{{ item.num }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 物流公司选择 -->
    <view class="express-selector card">
      <view class="info-header">
        
        <text>其他物流查询</text>
      </view>
      <view class="express-list">
        <view
          class="express-item"
          v-for="item in expressOptions"
          :key="item.code"
          :class="{ active: item.code === selectedExpress }"
          @click="selectExpress(item.code)"
        >
          {{ item.name }}
        </view>
      </view>
    </view>

    <!-- 联系快递员 -->
    <view class="contact-courier" @click="callCourier">
      
      <text>联系快递员</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      expressInfo: {
        company: '顺丰速运',
        no: 'SF1234567890123'
      },
      currentStatus: '包裹正在运输中',
      timeline: [
        {
          status: '已签收',
          desc: '感谢使用顺丰速运，期待再次为您服务',
          time: '2024-01-15 14:30:22',
          isCurrent: false,
          isPast: true
        },
        {
          status: '派送中',
          desc: '您的包裹正在派送中，请保持电话畅通',
          time: '2024-01-15 08:22:15',
          isCurrent: true,
          isPast: true
        },
        {
          status: '到达【杭州中转场】',
          desc: '快件已到达杭州中转场',
          time: '2024-01-14 20:15:33',
          isCurrent: false,
          isPast: true
        },
        {
          status: '运输中',
          desc: '快件已从上海发出，正在发往杭州',
          time: '2024-01-14 16:42:08',
          isCurrent: false,
          isPast: true
        },
        {
          status: '已揽收',
          desc: '商家已发货，包裹已被顺丰小哥揽收',
          time: '2024-01-13 18:05:42',
          isCurrent: false,
          isPast: true
        },
        {
          status: '已发出',
          desc: '包裹已从【上海旗舰店】发出',
          time: '2024-01-13 17:30:00',
          isCurrent: false,
          isPast: true
        }
      ],
      receiverInfo: {
        name: '张三',
        phone: '138****8888',
        address: '浙江省杭州市西湖区文三路123号智慧大厦15层1501室'
      },
      productList: [
        {
          id: 1,
          cover: 'https://picsum.photos/200',
          title: '山水之间',
          artist: '李明',
          price: '8888.00',
          num: 1
        }
      ],
      expressOptions: [
        { code: 'SF', name: '顺丰速运' },
        { code: 'YTO', name: '圆通速递' },
        { code: 'ZTO', name: '中通快递' },
        { code: 'STO', name: '申通快递' },
        { code: 'YT', name: '韵达快递' }
      ],
      selectedExpress: 'SF'
    }
  },

  onLoad(options) {
    if (options.orderId) {
      this.loadTrackInfo(options.orderId)
    }
  },

  methods: {
    loadTrackInfo(orderId) {
      // 模拟加载物流信息
      console.log('加载订单物流信息:', orderId)
    },

    copyNo() {
      uni.setClipboardData({
        data: this.expressInfo.no,
        success: () => {
          uni.showToast({ title: '已复制运单号', icon: 'success' })
        }
      })
    },

    selectExpress(code) {
      this.selectedExpress = code
      const express = this.expressOptions.find(e => e.code === code)
      if (express) {
        uni.showToast({ title: `已切换至${express.name}`, icon: 'success' })
      }
    },

    callCourier() {
      uni.makePhoneCall({
        phoneNumber: '95338',
        success: () => {
          console.log('拨打电话成功')
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.track-page {
  min-height: 100vh;
  background: #f5f6f7;
  padding: 20rpx;
  padding-bottom: 140rpx;
}

.track-header {
  padding: 30rpx;

  .express-info {
    .express-company,
    .express-no {
      display: flex;
      align-items: center;
      margin-bottom: 16rpx;

      .label {
        font-size: 28rpx;
        color: #666;
      }

      .value {
        font-size: 28rpx;
        color: #333;
        font-weight: 600;
      }

      .copy-btn {
        margin-left: 20rpx;
        padding: 6rpx 16rpx;
        font-size: 22rpx;
        color: #667eea;
        border: 1rpx solid #667eea;
        border-radius: 8rpx;
      }
    }
  }
}

.track-timeline {
  padding: 30rpx;

  .timeline-header {
    display: flex;
    align-items: center;
    margin-bottom: 40rpx;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f0f0f0;

    .current-status {
      margin-left: 16rpx;
      font-size: 32rpx;
      font-weight: 600;
      color: #333;
    }
  }

  .timeline-list {
    .timeline-item {
      display: flex;
      padding-bottom: 40rpx;
      position: relative;

      &::before {
        content: '';
        position: absolute;
        left: 10rpx;
        top: 30rpx;
        width: 2rpx;
        height: calc(100% - 20rpx);
        background: #e0e0e0;
      }

      &:last-child::before {
        display: none;
      }

      &.past::before {
        background: #667eea;
      }

      .timeline-dot {
        width: 22rpx;
        height: 22rpx;
        border-radius: 50%;
        background: #e0e0e0;
        margin-right: 24rpx;
        position: relative;
        z-index: 1;
        flex-shrink: 0;

        .dot-inner {
          width: 12rpx;
          height: 12rpx;
          border-radius: 50%;
          background: #fff;
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
        }
      }

      &.past .timeline-dot {
        background: #667eea;
      }

      &.current .timeline-dot {
        background: #667eea;
        width: 28rpx;
        height: 28rpx;
        box-shadow: 0 0 0 8rpx rgba(102, 126, 234, 0.2);

        .dot-inner {
          width: 14rpx;
          height: 14rpx;
        }
      }

      .timeline-content {
        flex: 1;

        .timeline-status {
          font-size: 28rpx;
          font-weight: 600;
          color: #999;
          margin-bottom: 8rpx;
        }

        .timeline-desc {
          font-size: 26rpx;
          color: #999;
          margin-bottom: 8rpx;
          line-height: 1.5;
        }

        .timeline-time {
          font-size: 24rpx;
          color: #ccc;
        }
      }

      &.past .timeline-content .timeline-status {
        color: #333;
      }

      &.current .timeline-content .timeline-status {
        color: #667eea;
      }
    }
  }
}

.receiver-info,
.product-info,
.express-selector {
  padding: 30rpx;

  .info-header {
    display: flex;
    align-items: center;
    margin-bottom: 24rpx;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f0f0f0;

    text {
      margin-left: 12rpx;
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }
  }

  .info-content {
    .receiver-row {
      display: flex;
      margin-bottom: 16rpx;

      .label {
        font-size: 28rpx;
        color: #666;
        flex-shrink: 0;
      }

      .value {
        font-size: 28rpx;
        color: #333;

        &.address {
          flex: 1;
          line-height: 1.5;
        }
      }
    }
  }

  .product-list {
    .product-item {
      display: flex;
      padding: 20rpx 0;
      border-bottom: 1rpx solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      .product-img {
        width: 140rpx;
        height: 140rpx;
        border-radius: 12rpx;
        flex-shrink: 0;
      }

      .product-detail {
        flex: 1;
        margin-left: 20rpx;
        display: flex;
        flex-direction: column;
        justify-content: center;

        .product-title {
          font-size: 28rpx;
          color: #333;
          margin-bottom: 8rpx;
        }

        .product-meta {
          font-size: 24rpx;
          color: #999;
        }
      }

      .product-price {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        justify-content: center;

        text {
          font-size: 28rpx;
          color: #333;
          font-weight: 600;

          &.num {
            font-size: 24rpx;
            color: #999;
            font-weight: normal;
            margin-top: 8rpx;
          }
        }
      }
    }
  }

  .express-list {
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;

    .express-item {
      padding: 16rpx 32rpx;
      font-size: 26rpx;
      color: #666;
      background: #f5f5f5;
      border-radius: 8rpx;

      &.active {
        color: #fff;
        background: #667eea;
      }
    }
  }
}

.contact-courier {
  position: fixed;
  bottom: 40rpx;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 600rpx;
  height: 88rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 44rpx;
  box-shadow: 0 8rpx 30rpx rgba(102, 126, 234, 0.4);

  text {
    margin-left: 12rpx;
    font-size: 30rpx;
    color: #fff;
    font-weight: 500;
  }
}
</style>
