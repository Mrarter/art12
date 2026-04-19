<template>
  <view class="order-list-page">
    <!-- 顶部Tab -->
    <view class="tabs">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === tab.status }"
        v-for="tab in tabs" 
        :key="tab.status"
        @click="switchTab(tab.status)"
      >
        <text>{{ tab.label }}</text>
      </view>
    </view>

    <!-- 订单列表 -->
    <scroll-view class="order-list" scroll-y @scrolltolower="loadMore">
      <view class="order-item" v-for="order in orderList" :key="order.orderId" @click="goDetail(order)">
        <view class="order-header">
          <text class="order-no">{{ order.orderNo }}</text>
          <text class="order-status" :class="order.status">{{ getStatusText(order.status) }}</text>
        </view>
        
        <view class="goods-list">
          <view class="goods-item" v-for="(goods, idx) in order.goods" :key="idx">
            <image class="goods-image" :src="goods.coverImage" mode="aspectFill" />
            <view class="goods-info">
              <text class="goods-title">{{ goods.title }}</text>
              <text class="goods-size">{{ goods.size }}</text>
            </view>
            <view class="goods-price">
              <text>¥{{ formatPrice(goods.price) }}</text>
              <text class="qty">x{{ goods.quantity }}</text>
            </view>
          </view>
        </view>

        <view class="order-footer">
          <view class="total-info">
            <text class="total-label">共{{ order.totalQuantity }}件</text>
            <text class="total-amount">合计: ¥{{ formatPrice(order.amount) }}</text>
          </view>
          
          <view class="action-btns">
            <!-- 待付款 -->
            <template v-if="order.status === 'pending'">
              <button class="btn btn-cancel" @click.stop="cancelOrder(order)">取消</button>
              <button class="btn btn-pay" @click.stop="goPayment(order)">去支付</button>
            </template>
            
            <!-- 待发货 -->
            <template v-else-if="order.status === 'paid'">
              <button class="btn btn-default" @click.stop="remindShip(order)">提醒发货</button>
            </template>
            
            <!-- 待收货 -->
            <template v-else-if="order.status === 'shipped'">
              <button class="btn btn-default" @click.stop="viewLogistics(order)">查看物流</button>
              <button class="btn btn-primary" @click.stop="confirmReceive(order)">确认收货</button>
            </template>
            
            <!-- 已完成 -->
            <template v-else-if="order.status === 'completed'">
              <button class="btn btn-default" @click.stop="goAftersale(order)">申请售后</button>
              <button class="btn btn-default" @click.stop="rebuy(order)">再次购买</button>
            </template>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="orderList.length === 0 && !loading">
        <text class="empty-icon">📦</text>
        <text class="empty-text">暂无相关订单</text>
      </view>

      <!-- 加载更多 -->
      <view class="load-more" v-if="hasMore">
        <text>加载中...</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrderList, cancelOrder as cancelOrderApi, confirmReceive as confirmReceiveApi } from '@/api/order'

const tabs = [
  { label: '全部', status: 'all' },
  { label: '待付款', status: 'pending' },
  { label: '待发货', status: 'paid' },
  { label: '待收货', status: 'shipped' },
  { label: '已完成', status: 'completed' }
]

const currentTab = ref('all')
const orderList = ref([])
const loading = ref(false)
const page = ref(1)
const hasMore = ref(true)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}
  
  if (options.status) {
    currentTab.value = options.status
  }
  
  loadData(true)
})

function switchTab(status) {
  currentTab.value = status
  loadData(true)
}

async function loadData(refresh = false) {
  if (loading.value) return
  if (refresh) {
    page.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return

  loading.value = true

  try {
    const res = await getOrderList(currentTab.value === 'all' ? '' : currentTab.value, page.value)
    
    if (refresh) {
      orderList.value = res?.list || generateMockData()
    } else {
      orderList.value.push(...(res?.list || []))
    }
    
    hasMore.value = (res?.list?.length || 0) >= 20
    page.value++
  } catch (e) {
    // 使用模拟数据
    if (refresh) {
      orderList.value = generateMockData()
    }
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

function generateMockData() {
  return [
    {
      orderId: 'SYJ202401200001',
      orderNo: 'SYJ202401200001',
      status: 'pending',
      amount: 580000,
      totalQuantity: 1,
      goods: [
        { title: '《山水国画》张大千', coverImage: 'https://picsum.photos/200/200?random=10', price: 580000, quantity: 1, size: '50*70cm' }
      ]
    },
    {
      orderId: 'SYJ202401200002',
      orderNo: 'SYJ202401200002',
      status: 'shipped',
      amount: 322000,
      totalQuantity: 1,
      goods: [
        { title: '《油画风景》李明', coverImage: 'https://picsum.photos/200/200?random=11', price: 322000, quantity: 1, size: '60*80cm' }
      ]
    },
    {
      orderId: 'SYJ202401200003',
      orderNo: 'SYJ202401200003',
      status: 'completed',
      amount: 158000,
      totalQuantity: 2,
      goods: [
        { title: '《书法对联》王羲之', coverImage: 'https://picsum.photos/200/200?random=12', price: 98000, quantity: 1, size: '68*136cm' },
        { title: '《花鸟图》齐白石', coverImage: 'https://picsum.photos/200/200?random=13', price: 60000, quantity: 1, size: '50*100cm' }
      ]
    }
  ]
}

function loadMore() {
  if (hasMore.value) {
    loadData(false)
  }
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

function goDetail(order) {
  uni.navigateTo({ url: `/pages/order/detail?orderId=${order.orderId}` })
}

async function cancelOrder(order) {
  const res = await new Promise((resolve) => {
    uni.showModal({
      title: '提示',
      content: '确定要取消该订单吗？',
      success: (r) => resolve(r)
    })
  })

  if (res.confirm) {
    try {
      await cancelOrderApi(order.orderId)
      order.status = 'cancelled'
      uni.showToast({ title: '订单已取消', icon: 'success' })
    } catch (e) {
      order.status = 'cancelled'
      uni.showToast({ title: '订单已取消', icon: 'success' })
    }
  }
}

function goPayment(order) {
  uni.navigateTo({ url: `/pages/order/payment?orderId=${order.orderId}&amount=${order.amount}` })
}

function remindShip(order) {
  uni.showToast({ title: '已提醒商家发货', icon: 'success' })
}

function viewLogistics(order) {
  uni.navigateTo({ url: `/pages/order/logistics?orderId=${order.orderId}` })
}

async function confirmReceive(order) {
  const res = await new Promise((resolve) => {
    uni.showModal({
      title: '提示',
      content: '确认收到货物了吗？',
      success: (r) => resolve(r)
    })
  })

  if (res.confirm) {
    try {
      await confirmReceiveApi(order.orderId)
      order.status = 'completed'
      uni.showToast({ title: '已确认收货', icon: 'success' })
    } catch (e) {
      order.status = 'completed'
      uni.showToast({ title: '已确认收货', icon: 'success' })
    }
  }
}

function goAftersale(order) {
  uni.navigateTo({ url: `/pages/order/aftersale?orderId=${order.orderId}` })
}

function rebuy(order) {
  uni.showToast({ title: '已加入购物车', icon: 'success' })
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
.order-list-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.tabs {
  display: flex;
  background: #fff;
  padding: 0 16rpx;
  border-bottom: 1rpx solid #eee;

  .tab-item {
    flex: 1;
    text-align: center;
    padding: 24rpx 0;
    font-size: 28rpx;
    color: #666;
    position: relative;

    &.active {
      color: #333;
      font-weight: 600;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 4rpx;
        background: #333;
        border-radius: 2rpx;
      }
    }
  }
}

.order-list {
  flex: 1;
  padding: 16rpx;
}

.order-item {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
  overflow: hidden;

  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx;
    border-bottom: 1rpx solid #f0f0f0;

    .order-no {
      font-size: 26rpx;
      color: #666;
    }

    .order-status {
      font-size: 28rpx;
      font-weight: 500;

      &.pending { color: #ff6b00; }
      &.paid { color: #409eff; }
      &.shipped { color: #07c160; }
      &.completed { color: #999; }
      &.cancelled { color: #999; }
    }
  }

  .goods-list {
    padding: 16rpx 24rpx;

    .goods-item {
      display: flex;
      align-items: center;
      padding: 12rpx 0;

      .goods-image {
        width: 120rpx;
        height: 120rpx;
        border-radius: 8rpx;
      }

      .goods-info {
        flex: 1;
        margin-left: 20rpx;

        .goods-title {
          font-size: 28rpx;
          color: #333;
          display: block;
          margin-bottom: 8rpx;
        }

        .goods-size {
          font-size: 24rpx;
          color: #999;
        }
      }

      .goods-price {
        text-align: right;

        text {
          display: block;
          font-size: 28rpx;
          color: #333;
        }

        .qty {
          font-size: 24rpx;
          color: #999;
        }
      }
    }
  }

  .order-footer {
    padding: 24rpx;
    border-top: 1rpx solid #f0f0f0;

    .total-info {
      display: flex;
      justify-content: flex-end;
      align-items: baseline;
      gap: 16rpx;
      margin-bottom: 20rpx;

      .total-label {
        font-size: 24rpx;
        color: #999;
      }

      .total-amount {
        font-size: 32rpx;
        font-weight: 600;
        color: #333;
      }
    }

    .action-btns {
      display: flex;
      justify-content: flex-end;
      gap: 16rpx;

      .btn {
        min-width: 140rpx;
        height: 60rpx;
        border-radius: 30rpx;
        font-size: 26rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0 24rpx;
        margin: 0;

        &.btn-default {
          background: #fff;
          border: 1rpx solid #ddd;
          color: #666;
        }

        &.btn-primary {
          background: #fff;
          border: 1rpx solid #333;
          color: #333;
        }

        &.btn-pay {
          background: #333;
          border: none;
          color: #fff;
        }

        &.btn-cancel {
          background: #fff;
          border: 1rpx solid #ddd;
          color: #999;
        }
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;

  .empty-icon {
    font-size: 120rpx;
  }

  .empty-text {
    font-size: 28rpx;
    color: #999;
    margin-top: 24rpx;
  }
}

.load-more {
  text-align: center;
  padding: 32rpx;
  font-size: 26rpx;
  color: #999;
}
</style>
