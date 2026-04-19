<template>
  <view class="payment-page">
    <!-- 订单信息 -->
    <view class="order-info">
      <view class="order-amount">
        <text class="label">支付金额</text>
        <text class="amount">¥{{ formatPrice(orderAmount) }}</text>
      </view>
      <view class="order-no">
        <text class="label">订单号</text>
        <text class="value">{{ orderId }}</text>
      </view>
    </view>

    <!-- 支付方式 -->
    <view class="payment-methods">
      <view class="section-title">选择支付方式</view>
      
      <view class="method-item" :class="{ active: payMethod === 'wechat' }" @click="payMethod = 'wechat'">
        <view class="method-icon wechat-icon">
          <text>WeChat</text>
        </view>
        <view class="method-info">
          <text class="method-name">微信支付</text>
          <text class="method-desc">推荐</text>
        </view>
        <view class="method-check" v-if="payMethod === 'wechat'">✓</view>
      </view>

      <view class="method-item" :class="{ active: payMethod === 'balance' }" @click="payMethod = 'balance'">
        <view class="method-icon balance-icon">
          <text>余额</text>
        </view>
        <view class="method-info">
          <text class="method-name">余额支付</text>
          <text class="method-desc">可用余额: ¥{{ formatPrice(userBalance) }}</text>
        </view>
        <view class="method-check" v-if="payMethod === 'balance'">✓</view>
      </view>

      <view class="method-item" :class="{ active: payMethod === 'alipay' }" @click="payMethod = 'alipay'">
        <view class="method-icon alipay-icon">
          <text>支付宝</text>
        </view>
        <view class="method-info">
          <text class="method-name">支付宝</text>
          <text class="method-desc"></text>
        </view>
        <view class="method-check" v-if="payMethod === 'alipay'">✓</view>
      </view>
    </view>

    <!-- 按钮 -->
    <view class="btn-bar safe-area-bottom">
      <button class="btn-pay" @click="handlePay" :loading="paying">
        确认支付 ¥{{ formatPrice(orderAmount) }}
      </button>
    </view>

    <!-- 倒计时提示 -->
    <view class="countdown-tip" v-if="countdown > 0">
      <text>请在 {{ countdown }} 秒内完成支付，超时订单将自动取消</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { unifiedOrder } from '@/api/order'

const orderId = ref('')
const orderAmount = ref(0)
const payMethod = ref('wechat')
const userBalance = ref(0)
const paying = ref(false)
const countdown = ref(1800) // 30分钟倒计时
let timer = null

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || currentPage.$page?.options || {}

  orderId.value = options.orderId || ''
  orderAmount.value = parseFloat(options.amount) || 0

  // 模拟用户余额
  userBalance.value = 100000

  // 开始倒计时
  timer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      clearInterval(timer)
      uni.showToast({ title: '订单已超时', icon: 'none' })
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    }
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

async function handlePay() {
  if (paying.value) return

  paying.value = true

  try {
    // 模拟支付成功
    uni.showLoading({ title: '支付中...' })

    setTimeout(() => {
      uni.hideLoading()
      uni.showToast({ title: '支付成功', icon: 'success' })
      
      setTimeout(() => {
        // 跳转到订单列表
        uni.redirectTo({ url: '/pages/order/list?status=paid' })
      }, 1500)
    }, 1500)

  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '支付失败', icon: 'none' })
  } finally {
    paying.value = false
  }
}

function formatPrice(price) {
  if (!price) return '0'
  if (price >= 10000) {
    return (price / 10000).toFixed(1) + '万'
  }
  return price.toLocaleString()
}
</script>

<style lang="scss" scoped>
.payment-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 160rpx;
}

.order-info {
  background: #fff;
  padding: 40rpx 32rpx;
  margin-bottom: 16rpx;

  .order-amount {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-bottom: 32rpx;
    border-bottom: 1rpx solid #eee;

    .label {
      font-size: 28rpx;
      color: #999;
      margin-bottom: 16rpx;
    }

    .amount {
      font-size: 56rpx;
      font-weight: 700;
      color: #333;
    }
  }

  .order-no {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 24rpx;

    .label {
      font-size: 28rpx;
      color: #999;
    }

    .value {
      font-size: 28rpx;
      color: #666;
    }
  }
}

.payment-methods {
  background: #fff;

  .section-title {
    padding: 24rpx 32rpx;
    font-size: 28rpx;
    color: #999;
    border-bottom: 1rpx solid #f0f0f0;
  }

  .method-item {
    display: flex;
    align-items: center;
    padding: 32rpx;
    border-bottom: 1rpx solid #f0f0f0;
    transition: background 0.2s;

    &.active {
      background: #f9f9f9;
    }

    &:last-child {
      border-bottom: none;
    }

    .method-icon {
      width: 80rpx;
      height: 80rpx;
      border-radius: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 24rpx;

      &.wechat-icon {
        background: #07c160;
        color: #fff;
        font-size: 20rpx;
      }

      &.balance-icon {
        background: #ff9500;
        color: #fff;
        font-size: 28rpx;
      }

      &.alipay-icon {
        background: #1677ff;
        color: #fff;
        font-size: 24rpx;
      }
    }

    .method-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4rpx;

      .method-name {
        font-size: 30rpx;
        color: #333;
      }

      .method-desc {
        font-size: 24rpx;
        color: #999;
      }
    }

    .method-check {
      width: 44rpx;
      height: 44rpx;
      border-radius: 50%;
      background: #333;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24rpx;
    }
  }
}

.btn-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16rpx 32rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;

  .btn-pay {
    width: 100%;
    height: 96rpx;
    background: #333;
    color: #fff;
    border-radius: 48rpx;
    font-size: 32rpx;
    font-weight: 500;
  }
}

.countdown-tip {
  text-align: center;
  padding: 24rpx;
  font-size: 24rpx;
  color: #999;
}
</style>
