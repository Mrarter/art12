<template>
  <view class="order-list-page">
    <!-- 状态Tab -->
    <view class="status-tabs">
      <view 
        class="tab-item" 
        :class="{ active: currentStatus === 'all' }"
        @click="switchStatus('all')"
      >
        全部
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentStatus === 'pending' }"
        @click="switchStatus('pending')"
      >
        待付款
        <view class="tab-badge" v-if="pendingPayCount">{{ pendingPayCount }}</view>
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentStatus === 'paid' }"
        @click="switchStatus('paid')"
      >
        已付款
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentStatus === 'completed' }"
        @click="switchStatus('completed')"
      >
        已完成
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentStatus === 'refund' }"
        @click="switchStatus('refund')"
      >
        退款
      </view>
    </view>
    
    <!-- 订单列表 -->
    <scroll-view class="order-list" scroll-y @scrolltolower="loadMore">
      <view class="order-card" v-for="order in orderList" :key="order.id" @click="goDetail(order.id)">
        <view class="order-header">
          <!-- 卖家信息 -->
          <view class="seller-info" v-if="order.sellerName" @click.stop>
            <image class="seller-avatar" :src="getFullImageUrl(order.sellerAvatar)" mode="aspectFill"></image>
            <text class="seller-name">{{ order.sellerName }}</text>
          </view>
          <text class="order-no">订单号: {{ order.orderNo }}</text>
          <text class="order-status" :class="'status-' + order.status">
            {{ getStatusText(order.status) }}
          </text>
        </view>
        
        <!-- 订单商品 -->
        <view class="order-goods">
          <view class="goods-item" v-for="item in order.items" :key="item.id">
            <image class="goods-image" :src="getFullImageUrl(item.coverImage)" mode="aspectFill" @error="onImageError($event, item)"></image>
            <view class="goods-info">
              <text class="goods-title">{{ item.title }}</text>
              <text class="goods-meta">{{ item.artType }}</text>
            </view>
            <view class="goods-right">
              <text class="goods-price">¥{{ formatPrice(item.price) }}</text>
              <text class="goods-qty">x{{ item.quantity }}</text>
            </view>
          </view>
        </view>
        
        <view class="order-footer">
          <view class="order-info">
            <text class="order-time">{{ formatTime(order.createTime) }}</text>
            <text class="order-count">共{{ order.items?.length || 0 }}件商品</text>
          </view>
          <view class="order-amount">
            <text class="amount-label">实付款:</text>
            <text class="amount-value">¥{{ formatPrice(order.payAmount) }}</text>
          </view>
        </view>
        
        <!-- 操作按钮 -->
        <view class="order-actions" @click.stop>
          <template v-if="order.status === 'PENDING_PAYMENT'">
            <button class="action-btn cancel" @click="onCancel(order)">取消订单</button>
            <button class="action-btn primary" @click="onPay(order)">去支付</button>
          </template>
          <template v-else-if="order.status === 'SHIPPED'">
            <button class="action-btn primary" @click="onConfirm(order)">确认收货</button>
          </template>
          <template v-else-if="order.status === 'COMPLETED' || order.status === 'REFUNDED'">
            <button class="action-btn" @click="onDelete(order)">删除订单</button>
          </template>
          <button class="action-btn" @click="goDetail(order.id)">查看详情</button>
        </view>
      </view>
      
      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && orderList.length === 0">
        <image class="empty-image" src="/static/images/empty-order.png" mode="aspectFit"></image>
        <text class="empty-text">暂无相关订单</text>
      </view>
      
      <!-- 加载状态 -->
      <view class="load-more" v-if="orderList.length > 0">
        <text v-if="loading">加载中...</text>
        <text v-else-if="noMore">没有更多了</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrderList, cancelOrder, confirmReceive } from '@/api/order'
import { getFullImageUrl } from '@/utils/image'

// 状态映射（后端使用字符串）
const statusMap = {
  all: null,
  pending: 'PENDING_PAYMENT',
  paid: 'PAID',
  shipped: 'SHIPPED',
  received: 'RECEIVED',
  completed: 'COMPLETED',
  refund: 'REFUNDING'
}

// 状态
const currentStatus = ref('all')
const orderList = ref([])
const loading = ref(false)
const noMore = ref(false)
const page = ref(1)
const pageSize = 10
const pendingPayCount = ref(0)

// Tab切换
const switchStatus = (status) => {
  currentStatus.value = status
  orderList.value = []
  page.value = 1
  noMore.value = false
  fetchOrderList(true)
}

// 获取订单列表
const fetchOrderList = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    noMore.value = false
  }
  
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize,
      status: statusMap[currentStatus.value]
    }
    
    const result = await getOrderList(params)
    
    // 处理 PageResult 格式：{ records: [], total: xxx } 或直接数组
    const rawList = result?.records || result?.list || result || []
    const list = Array.isArray(rawList) ? rawList : []
    
    // 数据归一化：确保每项有 items 数组、合理的默认值
    const normalized = list.map(item => ({
      ...item,
      items: item.items || item.goods || item.orderItems || [],
      payAmount: item.payAmount || item.amount || item.totalAmount || 0,
      orderNo: item.orderNo || item.orderNumber || item.id || ''
    }))
    
    if (reset) {
      orderList.value = normalized
    } else {
      orderList.value = [...orderList.value, ...normalized]
    }
    
    pendingPayCount.value = result?.pendingPayCount || 0
    
    if (list.length < pageSize) {
      noMore.value = true
    } else {
      page.value++
    }
  } catch (e) {
    console.warn('[订单列表] 加载失败:', e)
    if (reset) orderList.value = []
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  if (!noMore.value) {
    fetchOrderList()
  }
}

// 跳转详情
const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/order/detail?id=${id}` })
}

// 取消订单
const onCancel = (order) => {
  uni.showModal({
    title: '提示',
    content: '确定取消该订单？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelOrder(order.id)
          uni.showToast({ title: '订单已取消', icon: 'success' })
          fetchOrderList(true)
        } catch (e) {
          uni.showToast({ title: '取消失败', icon: 'none' })
        }
      }
    }
  })
}

// 去支付
const onPay = (order) => {
  uni.navigateTo({
    url: `/pages/order/pay?orderId=${order.id}&amount=${order.payAmount}`
  })
}

// 确认收货
const onConfirm = (order) => {
  uni.showModal({
    title: '提示',
    content: '确认已收到货物？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await confirmReceive(order.id)
          uni.showToast({ title: '已确认收货', icon: 'success' })
          fetchOrderList(true)
        } catch (e) {
          uni.showToast({ title: '操作失败', icon: 'none' })
        }
      }
    }
  })
}

// 删除订单
const onDelete = (order) => {
  uni.showModal({
    title: '提示',
    content: '确定删除该订单？',
    success: async (res) => {
      if (res.confirm) {
        // TODO: 调用删除接口
        orderList.value = orderList.value.filter(o => o.id !== order.id)
        uni.showToast({ title: '已删除', icon: 'success' })
      }
    }
  })
}

// 获取状态文字
const getStatusText = (status) => {
  const map = {
    'CANCELLED': '已取消',
    'PENDING_PAYMENT': '待付款',
    'PAID': '已付款',
    'SHIPPED': '已发货',
    'RECEIVED': '已收货',
    'COMPLETED': '已完成',
    'REFUNDING': '退款中',
    'REFUNDED': '已退款'
  }
  return map[status] || '未知'
}

// 订单链路沿用“分”单位，展示时统一转元
const formatPrice = (price) => {
  if (price === null || price === undefined || price === '') return '0.00'
  return (Number(price) / 100).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 图片加载失败处理
const onImageError = (e, item) => {
  console.warn('图片加载失败:', item.coverImage)
  // 使用默认占位图
  if (e.target) {
    e.target.src = '/static/icons/artwork-default.png'
  }
}

// 初始化
onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const status = currentPage.options?.status
  if (status) {
    currentStatus.value = status
  }
  fetchOrderList(true)
})
</script>

<style lang="scss" scoped>
.order-list-page {
  min-height: 100vh;
  background-color: #f8f8f8;
}

.status-tabs {
  display: flex;
  background-color: #ffffff;
  padding: 0 20rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  
  .tab-item {
    flex: 1;
    text-align: center;
    padding: 24rpx 0;
    font-size: 28rpx;
    color: #666666;
    position: relative;
    
    &.active {
      color: #333333;
      font-weight: 600;
      
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 4rpx;
        background-color: #333333;
        border-radius: 2rpx;
      }
    }
    
    .tab-badge {
      position: absolute;
      top: 12rpx;
      right: 20rpx;
      min-width: 32rpx;
      height: 32rpx;
      padding: 0 8rpx;
      background-color: #ff4d4f;
      color: #ffffff;
      font-size: 20rpx;
      border-radius: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
}

.order-list {
  padding: 20rpx 30rpx;
  height: calc(100vh - 100rpx);
}

.order-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
  
  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx;
    border-bottom: 1rpx solid #f5f5f5;
    
    .seller-info {
      display: flex;
      align-items: center;
      
      .seller-avatar {
        width: 40rpx;
        height: 40rpx;
        border-radius: 50%;
        margin-right: 8rpx;
        background-color: #f5f5f5;
      }
      
      .seller-name {
        font-size: 26rpx;
        color: #333333;
        font-weight: 500;
      }
    }
    
    .order-no {
      font-size: 24rpx;
      color: #999999;
    }
    
    .order-status {
      font-size: 26rpx;
      
      &.status-PENDING_PAYMENT { color: #ff4d4f; }
      &.status-PAID { color: #1890ff; }
      &.status-SHIPPED { color: #faad14; }
      &.status-COMPLETED { color: #52c41a; }
      &.status-REFUNDING, &.status-REFUNDED, &.status-CANCELLED { color: #999999; }
    }
  }
  
  .order-goods {
    padding: 20rpx 24rpx;
    
    .goods-item {
      display: flex;
      margin-bottom: 16rpx;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .goods-image {
        width: 140rpx;
        height: 140rpx;
        border-radius: 8rpx;
        background-color: #f5f5f5;
        margin-right: 16rpx;
      }
      
      .goods-info {
        flex: 1;
        display: flex;
        flex-direction: column;
        
        .goods-title {
          font-size: 28rpx;
          color: #333333;
          @include ellipsis(1);
          margin-bottom: 8rpx;
        }
        
        .goods-meta {
          font-size: 24rpx;
          color: #999999;
        }
      }
      
      .goods-right {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        
        .goods-price {
          font-size: 28rpx;
          color: #333333;
        }
        
        .goods-qty {
          font-size: 24rpx;
          color: #999999;
          margin-top: auto;
        }
      }
    }
  }
  
  .order-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 24rpx 20rpx;
    
    .order-info {
      display: flex;
      align-items: center;
      
      .order-time {
        font-size: 24rpx;
        color: #999999;
        margin-right: 20rpx;
      }
      
      .order-count {
        font-size: 24rpx;
        color: #999999;
      }
    }
    
    .order-amount {
      .amount-label {
        font-size: 24rpx;
        color: #666666;
      }
      
      .amount-value {
        font-size: 30rpx;
        font-weight: 600;
        color: #333333;
        margin-left: 8rpx;
      }
    }
  }
  
  .order-actions {
    display: flex;
    justify-content: flex-end;
    padding: 16rpx 24rpx;
    border-top: 1rpx solid #f5f5f5;
    
    .action-btn {
      min-width: 160rpx;
      height: 64rpx;
      padding: 0 24rpx;
      font-size: 26rpx;
      background-color: #f5f5f5;
      color: #666666;
      border-radius: 32rpx;
      margin-left: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      
      &.primary {
        background-color: #333333;
        color: #ffffff;
      }
      
      &.cancel {
        background-color: transparent;
        border: 1rpx solid #cccccc;
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
  
  .empty-image {
    width: 200rpx;
    height: 200rpx;
    margin-bottom: 30rpx;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: #999999;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx 0;
  font-size: 24rpx;
  color: #999999;
}

/* 订单页暗色视觉优化 */
.order-list-page {
  background: #0b0b0c;
  color: #f6f2e8;
}

.status-tabs {
  background: rgba(11, 11, 12, 0.96);
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
  padding: 0 24rpx;

  .tab-item {
    color: #8f8a80;
    font-weight: 600;

    &.active {
      color: #f6f2e8;

      &::after {
        background-color: #c9a227;
        width: 42rpx;
      }
    }
  }
}

.order-list {
  padding: 24rpx;
  box-sizing: border-box;
}

.order-card {
  background: #171719;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 18rpx;
  box-shadow: 0 18rpx 42rpx rgba(0, 0, 0, 0.22);

  .order-header {
    border-bottom-color: rgba(255, 255, 255, 0.08);

    .seller-info .seller-name {
      color: #f6f2e8;
    }

    .order-no {
      color: #8f8a80;
    }

    .order-status {
      font-weight: 700;

      &.status-PENDING_PAYMENT { color: #ff7a7a; }
      &.status-PAID { color: #c9a227; }
      &.status-SHIPPED { color: #f0b64d; }
      &.status-COMPLETED { color: #69d37b; }
    }
  }

  .order-goods .goods-item {
    .goods-image {
      background-color: #202024;
    }

    .goods-info {
      .goods-title {
        color: #f6f2e8;
        font-weight: 700;
      }

      .goods-meta {
        color: #8f8a80;
      }
    }

    .goods-right {
      .goods-price {
        color: #f2c85b;
        font-weight: 800;
      }

      .goods-qty {
        color: #8f8a80;
      }
    }
  }

  .order-footer {
    .order-info {
      .order-time,
      .order-count {
        color: #8f8a80;
      }
    }

    .order-amount {
      .amount-label {
        color: #8f8a80;
      }

      .amount-value {
        color: #f2c85b;
      }
    }
  }

  .order-actions {
    border-top-color: rgba(255, 255, 255, 0.08);

    .action-btn {
      background: #202024;
      color: #f6f2e8;

      &.primary {
        background: #c9a227;
        color: #16130b;
        font-weight: 800;
      }

      &.cancel {
        border-color: rgba(255, 255, 255, 0.16);
        color: #9b958a;
      }
    }
  }
}

.empty-state {
  min-height: 56vh;
  justify-content: center;

  .empty-image {
    opacity: 0.62;
  }

  .empty-text {
    color: #8f8a80;
  }
}

.load-more {
  color: #68645c;
}
</style>
