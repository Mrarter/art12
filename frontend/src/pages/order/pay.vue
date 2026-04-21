<template>
  <view class="pay-page">
    <!-- 订单信息 -->
    <view class="order-info">
      <view class="info-header">
        <text class="order-title">订单信息</text>
        <text class="order-no">订单号：{{ orderInfo.orderNo }}</text>
      </view>
      <view class="info-list">
        <view class="info-item">
          <text class="label">商品金额</text>
          <text class="value">¥{{ orderInfo.goodsAmount }}</text>
        </view>
        <view class="info-item">
          <text class="label">运费</text>
          <text class="value">¥{{ orderInfo.freight }}</text>
        </view>
        <view class="info-item" v-if="orderInfo.couponAmount > 0">
          <text class="label">优惠券</text>
          <text class="value discount">-¥{{ orderInfo.couponAmount }}</text>
        </view>
      </view>
      <view class="info-total">
        <text class="total-label">应付金额</text>
        <text class="total-price">¥{{ orderInfo.payAmount }}</text>
      </view>
    </view>

    <!-- 支付方式 -->
    <view class="pay-methods">
      <view class="methods-header">
        <text class="header-title">选择支付方式</text>
      </view>
      
      <view class="method-item" 
        v-for="item in payMethods" 
        :key="item.id"
        :class="{ active: selectedPay === item.id }"
        @click="selectPay(item)"
      >
        <view class="method-icon">
          <image :src="item.icon" mode="aspectFit"></image>
        </view>
        <view class="method-info">
          <text class="method-name">{{ item.name }}</text>
          <text class="method-desc">{{ item.desc }}</text>
        </view>
        <view class="method-check">
          <u-icon name="checkmark-circle-fill" size="24" color="#667eea" v-if="selectedPay === item.id"></u-icon>
          <u-icon name="circle" size="24" color="#ddd" v-else></u-icon>
        </view>
      </view>
    </view>

    <!-- 支付安全 -->
    <view class="security-tips">
      <u-icon name="lock" size="18" color="#999"></u-icon>
      <text>支付安全由微信支付全程保障</text>
    </view>

    <!-- 底部按钮 -->
    <view class="pay-footer">
      <view class="footer-left">
        <text class="amount-label">应付</text>
        <text class="amount-value">¥{{ orderInfo.payAmount }}</text>
      </view>
      <button class="pay-btn" @click="doPay" :loading="paying">
        {{ paying ? '支付中...' : '立即支付' }}
      </button>
    </view>

    <!-- 支付成功弹窗 -->
    <view class="pay-success-modal" v-if="showSuccess">
      <view class="success-content">
        <view class="success-icon">
          <u-icon name="checkmark-circle" size="100" color="#4caf50"></u-icon>
        </view>
        <text class="success-title">支付成功</text>
        <text class="success-desc">您的订单已支付成功</text>
        <view class="success-actions">
          <button class="action-btn primary" @click="goOrderDetail">查看订单</button>
          <button class="action-btn secondary" @click="goHome">返回首页</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const orderInfo = ref({
  orderNo: 'ORD202401011234567890',
  goodsAmount: 8888,
  freight: 0,
  couponAmount: 100,
  payAmount: 8788
})

const payMethods = ref([
  {
    id: 'wechat',
    name: '微信支付',
    desc: '推荐',
    icon: '/static/icons/wechat.png'
  },
  {
    id: 'balance',
    name: '余额支付',
    desc: '可用余额 ¥1000.00',
    icon: '/static/icons/balance.png'
  }
])

const selectedPay = ref('wechat')
const paying = ref(false)
const showSuccess = ref(false)

const selectPay = (item) => {
  selectedPay.value = item.id
}

const doPay = async () => {
  if (paying.value) return
  
  paying.value = true
  
  try {
    // 模拟支付过程
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    paying.value = false
    showSuccess.value = true
    
    // 更新订单状态（实际应该调用API）
  } catch (e) {
    paying.value = false
    uni.showToast({ title: '支付失败，请重试', icon: 'none' })
  }
}

const goOrderDetail = () => {
  uni.redirectTo({ url: `/pages/order/detail?orderId=${orderInfo.value.orderNo}` })
}

const goHome = () => {
  uni.switchTab({ url: '/pages/index/index' })
}

onMounted(() => {
  // 获取订单信息
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}
  
  if (options.orderId) {
    // 实际应该从API获取订单详情
    orderInfo.value.orderNo = options.orderId
  }
})
</script>

<style lang="scss" scoped>
.pay-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: env(safe-area-inset-bottom);
}

.order-info {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;

  .info-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30rpx;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f0f0f0;

    .order-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }

    .order-no {
      font-size: 24rpx;
      color: #999;
    }
  }

  .info-list {
    .info-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20rpx 0;

      .label {
        font-size: 28rpx;
        color: #666;
      }

      .value {
        font-size: 28rpx;
        color: #333;
      }

      .discount {
        color: #ff4d4f;
      }
    }
  }

  .info-total {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 30rpx;
    margin-top: 20rpx;
    border-top: 1rpx solid #f0f0f0;

    .total-label {
      font-size: 28rpx;
      color: #333;
    }

    .total-price {
      font-size: 40rpx;
      font-weight: 700;
      color: #ff4d4f;
    }
  }
}

.pay-methods {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;

  .methods-header {
    margin-bottom: 30rpx;

    .header-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }
  }

  .method-item {
    display: flex;
    align-items: center;
    padding: 30rpx 0;
    border-bottom: 1rpx solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    &.active {
      .method-check {
        color: #667eea;
      }
    }

    .method-icon {
      width: 64rpx;
      height: 64rpx;
      margin-right: 20rpx;

      image {
        width: 100%;
        height: 100%;
      }
    }

    .method-info {
      flex: 1;

      .method-name {
        font-size: 28rpx;
        color: #333;
        font-weight: 500;
        display: block;
        margin-bottom: 8rpx;
      }

      .method-desc {
        font-size: 24rpx;
        color: #999;
      }
    }

    .method-check {
      color: #ddd;
    }
  }
}

.security-tips {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  padding: 40rpx;
  font-size: 24rpx;
  color: #999;
}

.pay-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 20rpx rgba(0,0,0,0.05);

  .footer-left {
    flex: 1;

    .amount-label {
      font-size: 26rpx;
      color: #666;
      margin-right: 8rpx;
    }

    .amount-value {
      font-size: 40rpx;
      font-weight: 700;
      color: #ff4d4f;
    }
  }

  .pay-btn {
    width: 280rpx;
    height: 96rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 32rpx;
    font-weight: 600;
    border-radius: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;

    &::after {
      border: none;
    }
  }
}

.pay-success-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;

  .success-content {
    width: 600rpx;
    background: #fff;
    border-radius: 24rpx;
    padding: 60rpx 40rpx;
    text-align: center;

    .success-icon {
      margin-bottom: 30rpx;
    }

    .success-title {
      font-size: 40rpx;
      font-weight: 700;
      color: #333;
      display: block;
      margin-bottom: 16rpx;
    }

    .success-desc {
      font-size: 28rpx;
      color: #999;
      display: block;
      margin-bottom: 50rpx;
    }

    .success-actions {
      display: flex;
      gap: 24rpx;

      .action-btn {
        flex: 1;
        height: 88rpx;
        border-radius: 44rpx;
        font-size: 30rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        border: none;

        &::after {
          border: none;
        }

        &.primary {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: #fff;
        }

        &.secondary {
          background: #f5f6f8;
          color: #666;
        }
      }
    }
  }
}
</style>