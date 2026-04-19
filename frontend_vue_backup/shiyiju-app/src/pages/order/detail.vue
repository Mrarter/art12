<template>
  <view class="order-detail-page">
    <!-- 订单状态 -->
    <view class="status-section" :class="order.status">
      <view class="status-icon">
        <text v-if="order.status === 'pending'">⏰</text>
        <text v-else-if="order.status === 'paid'">📦</text>
        <text v-else-if="order.status === 'shipped'">🚚</text>
        <text v-else-if="order.status === 'completed'">✅</text>
        <text v-else>❌</text>
      </view>
      <text class="status-text">{{ getStatusText(order.status) }}</text>
      <text class="status-desc" v-if="order.status === 'pending'">请尽快完成支付</text>
    </view>

    <!-- 收货地址 -->
    <view class="section address-section">
      <view class="section-icon">📍</view>
      <view class="address-info">
        <text class="contact">{{ order.address?.contact }} {{ order.address?.phone }}</text>
        <text class="detail">{{ order.address?.province }}{{ order.address?.city }}{{ order.address?.district }}{{ order.address?.address }}</text>
      </view>
    </view>

    <!-- 商品列表 -->
    <view class="section goods-section">
      <view class="shop-header">
        <text class="shop-name">拾艺局官方旗舰店</text>
      </view>
      <view class="goods-list">
        <view class="goods-item" v-for="(goods, idx) in order.goods" :key="idx">
          <image class="goods-image" :src="goods.coverImage" mode="aspectFill" @click="goArtwork(goods.artworkId)" />
          <view class="goods-info">
            <text class="goods-title" @click="goArtwork(goods.artworkId)">{{ goods.title }}</text>
            <text class="goods-size">{{ goods.size }}</text>
            <view class="goods-footer">
              <text class="goods-price">¥{{ formatPrice(goods.price) }}</text>
              <text class="goods-qty">x{{ goods.quantity }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 订单信息 -->
    <view class="section order-info-section">
      <view class="info-row">
        <text class="info-label">订单编号</text>
        <view class="info-value">
          <text>{{ order.orderNo }}</text>
          <text class="copy-btn" @click="copyOrderNo">复制</text>
        </view>
      </view>
      <view class="info-row">
        <text class="info-label">下单时间</text>
        <text class="info-value">{{ order.createTime }}</text>
      </view>
      <view class="info-row" v-if="order.payTime">
        <text class="info-label">支付时间</text>
        <text class="info-value">{{ order.payTime }}</text>
      </view>
      <view class="info-row" v-if="order.shipTime">
        <text class="info-label">发货时间</text>
        <text class="info-value">{{ order.shipTime }}</text>
      </view>
      <view class="info-row" v-if="order.completeTime">
        <text class="info-label">完成时间</text>
        <text class="info-value">{{ order.completeTime }}</text>
      </view>
    </view>

    <!-- 价格明细 -->
    <view class="section price-section">
      <view class="price-row">
        <text class="price-label">商品金额</text>
        <text class="price-value">¥{{ formatPrice(order.goodsAmount) }}</text>
      </view>
      <view class="price-row">
        <text class="price-label">运费</text>
        <text class="price-value">{{ order.freight > 0 ? '¥' + order.freight : '免运费' }}</text>
      </view>
      <view class="price-row">
        <text class="price-label">优惠券</text>
        <text class="price-value">-¥{{ order.couponAmount || 0 }}</text>
      </view>
      <view class="price-row total">
        <text class="price-label">实付款</text>
        <text class="price-value">¥{{ formatPrice(order.amount) }}</text>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar safe-area-bottom" v-if="order.status !== 'cancelled' && order.status !== 'completed'">
      <template v-if="order.status === 'pending'">
        <button class="btn btn-default" @click="cancelOrder">取消订单</button>
        <button class="btn btn-primary" @click="goPayment">去支付</button>
      </template>
      <template v-else-if="order.status === 'paid'">
        <button class="btn btn-default" @click="remindShip">提醒发货</button>
      </template>
      <template v-else-if="order.status === 'shipped'">
        <button class="btn btn-default" @click="viewLogistics">查看物流</button>
        <button class="btn btn-primary" @click="confirmReceive">确认收货</button>
      </template>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrderDetail, cancelOrder as cancelApi, confirmReceive as confirmReceiveApi } from '@/api/order'

const order = ref({
  orderId: '',
  orderNo: '',
  status: 'pending',
  amount: 0,
  goodsAmount: 0,
  freight: 0,
  couponAmount: 0,
  createTime: '',
  payTime: '',
  shipTime: '',
  completeTime: '',
  address: {},
  goods: []
})

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}

  if (options.orderId) {
    loadOrderDetail(options.orderId)
  }
})

async function loadOrderDetail(orderId) {
  try {
    const res = await getOrderDetail(orderId)
    order.value = res || generateMockOrder(orderId)
  } catch (e) {
    order.value = generateMockOrder(orderId)
  }
}

function generateMockOrder(orderId) {
  const mockOrders = {
    'SYJ202401200001': {
      orderId: 'SYJ202401200001',
      orderNo: 'SYJ202401200001',
      status: 'pending',
      amount: 580000,
      goodsAmount: 580000,
      freight: 0,
      couponAmount: 0,
      createTime: '2024-01-20 10:30:25',
      address: { contact: '张三', phone: '138****8001', province: '北京市', city: '北京市', district: '朝阳区', address: '建国路88号SOHO现代城' },
      goods: [{ artworkId: 1, title: '《山水国画》张大千', coverImage: 'https://picsum.photos/200/200?random=10', price: 580000, quantity: 1, size: '50*70cm' }]
    },
    'SYJ202401200002': {
      orderId: 'SYJ202401200002',
      orderNo: 'SYJ202401200002',
      status: 'shipped',
      amount: 322000,
      goodsAmount: 320000,
      freight: 2000,
      couponAmount: 0,
      createTime: '2024-01-20 14:15:30',
      payTime: '2024-01-20 14:20:00',
      shipTime: '2024-01-21 09:00:00',
      address: { contact: '李四', phone: '138****8002', province: '上海市', city: '上海市', district: '黄浦区', address: '南京东路100号' },
      goods: [{ artworkId: 2, title: '《油画风景》李明', coverImage: 'https://picsum.photos/200/200?random=11', price: 322000, quantity: 1, size: '60*80cm' }]
    }
  }

  return mockOrders[orderId] || mockOrders['SYJ202401200001']
}

function getStatusText(status) {
  const map = {
    pending: '待付款',
    paid: '待发货',
    shipped: '待收货',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

function copyOrderNo() {
  uni.setClipboardData({
    data: order.value.orderNo,
    success: () => uni.showToast({ title: '已复制', icon: 'success' })
  })
}

function goArtwork(artworkId) {
  uni.navigateTo({ url: `/pages/artwork/detail?id=${artworkId}` })
}

function goPayment() {
  uni.navigateTo({ url: `/pages/order/payment?orderId=${order.value.orderId}&amount=${order.value.amount}` })
}

async function cancelOrder() {
  const res = await new Promise((resolve) => {
    uni.showModal({
      title: '提示',
      content: '确定要取消该订单吗？',
      success: (r) => resolve(r)
    })
  })

  if (res.confirm) {
    try {
      await cancelApi(order.value.orderId)
    } catch (e) {}
    order.value.status = 'cancelled'
    uni.showToast({ title: '订单已取消', icon: 'success' })
  }
}

function remindShip() {
  uni.showToast({ title: '已提醒商家发货', icon: 'success' })
}

function viewLogistics() {
  uni.navigateTo({ url: `/pages/order/logistics?orderId=${order.value.orderId}` })
}

async function confirmReceive() {
  const res = await new Promise((resolve) => {
    uni.showModal({
      title: '提示',
      content: '确认收到货物了吗？',
      success: (r) => resolve(r)
    })
  })

  if (res.confirm) {
    try {
      await confirmReceiveApi(order.value.orderId)
    } catch (e) {}
    order.value.status = 'completed'
    uni.showToast({ title: '已确认收货', icon: 'success' })
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
.order-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

.status-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 32rpx;
  color: #fff;

  &.pending { background: linear-gradient(135deg, #ff9500, #ff6b00); }
  &.paid { background: linear-gradient(135deg, #409eff, #1677ff); }
  &.shipped { background: linear-gradient(135deg, #07c160, #06ae56); }
  &.completed { background: #999; }
  &.cancelled { background: #999; }

  .status-icon {
    font-size: 64rpx;
    margin-bottom: 16rpx;
  }

  .status-text {
    font-size: 36rpx;
    font-weight: 600;
  }

  .status-desc {
    font-size: 26rpx;
    opacity: 0.9;
    margin-top: 8rpx;
  }
}

.section {
  background: #fff;
  margin-bottom: 16rpx;
}

.address-section {
  display: flex;
  align-items: flex-start;
  padding: 32rpx;

  .section-icon {
    font-size: 40rpx;
    margin-right: 20rpx;
  }

  .address-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8rpx;

    .contact {
      font-size: 32rpx;
      font-weight: 500;
      color: #333;
    }

    .detail {
      font-size: 28rpx;
      color: #666;
    }
  }
}

.goods-section {
  .shop-header {
    padding: 24rpx 32rpx;
    border-bottom: 1rpx solid #f0f0f0;

    .shop-name {
      font-size: 28rpx;
      color: #333;
      font-weight: 500;
    }
  }

  .goods-list {
    .goods-item {
      display: flex;
      padding: 24rpx 32rpx;
      border-bottom: 1rpx solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .goods-image {
        width: 160rpx;
        height: 160rpx;
        border-radius: 8rpx;
      }

      .goods-info {
        flex: 1;
        margin-left: 24rpx;
        display: flex;
        flex-direction: column;
        justify-content: space-between;

        .goods-title {
          font-size: 28rpx;
          color: #333;
        }

        .goods-size {
          font-size: 24rpx;
          color: #999;
        }

        .goods-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .goods-price {
            font-size: 30rpx;
            font-weight: 600;
            color: #333;
          }

          .goods-qty {
            font-size: 26rpx;
            color: #999;
          }
        }
      }
    }
  }
}

.order-info-section {
  padding: 24rpx 32rpx;

  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16rpx 0;

    .info-label {
      font-size: 28rpx;
      color: #999;
    }

    .info-value {
      font-size: 28rpx;
      color: #333;
      display: flex;
      align-items: center;
      gap: 16rpx;

      .copy-btn {
        font-size: 24rpx;
        color: #409eff;
      }
    }
  }
}

.price-section {
  padding: 24rpx 32rpx;

  .price-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12rpx 0;

    .price-label {
      font-size: 28rpx;
      color: #666;
    }

    .price-value {
      font-size: 28rpx;
      color: #333;
    }

    &.total {
      margin-top: 16rpx;
      padding-top: 16rpx;
      border-top: 1rpx solid #eee;

      .price-label {
        font-size: 30rpx;
        font-weight: 500;
        color: #333;
      }

      .price-value {
        font-size: 36rpx;
        font-weight: 700;
        color: #333;
      }
    }
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  padding: 16rpx 32rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;

  .btn {
    min-width: 160rpx;
    height: 72rpx;
    border-radius: 36rpx;
    font-size: 28rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0;

    &.btn-default {
      background: #fff;
      border: 1rpx solid #ddd;
      color: #666;
    }

    &.btn-primary {
      background: #333;
      border: none;
      color: #fff;
    }
  }
}
</style>
