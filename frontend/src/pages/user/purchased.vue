<template>
  <view class="purchased-page">
    <!-- 筛选Tab -->
    <view class="filter-bar">
      <view
        class="filter-item"
        :class="{ active: currentFilter === 'all' }"
        @click="switchFilter('all')"
      >
        全部
      </view>
      <view
        class="filter-item"
        :class="{ active: currentFilter === 'pending' }"
        @click="switchFilter('pending')"
      >
        待发货
      </view>
      <view
        class="filter-item"
        :class="{ active: currentFilter === 'shipped' }"
        @click="switchFilter('shipped')"
      >
        待收货
      </view>
      <view
        class="filter-item"
        :class="{ active: currentFilter === 'completed' }"
        @click="switchFilter('completed')"
      >
        已完成
      </view>
    </view>

    <!-- 加载中 -->
    <view class="loading-state" v-if="loading && !loaded">
      <text>加载中...</text>
    </view>

    <!-- 订单列表 -->
    <view class="order-list" v-if="loaded">
      <view class="order-card" v-for="order in filteredOrders" :key="order.id">
        <view class="order-header">
          <view class="order-time">{{ formatTime(order.createTime) }}</view>
          <view class="order-status" :class="'status-' + order.status">
            {{ getStatusText(order.status) }}
          </view>
        </view>

        <view class="order-items">
          <view class="order-item" v-for="item in order.items" :key="item.id" @click="goOrderDetail(order.id)">
            <image class="item-image" :src="item.cover" mode="aspectFill"></image>
            <view class="item-info">
              <view class="item-title">{{ item.title }}</view>
              <view class="item-artist" v-if="item.artist">{{ item.artist }}</view>
              <view class="item-meta" v-if="item.meta">{{ item.meta }}</view>
            </view>
            <view class="item-price">
              <text>¥{{ formatPrice(item.price) }}</text>
              <text class="item-num">x{{ item.num }}</text>
            </view>
          </view>
        </view>

        <view class="order-footer">
          <view class="order-total">
            <text>共{{ order.itemCount }}件商品</text>
            <text class="total-price">合计：¥{{ formatPrice(order.total) }}</text>
          </view>
          <view class="order-actions">
            <template v-if="order.status === 'pending'">
              <view class="action-btn secondary" @click.stop="applyRefund(order)">申请退款</view>
            </template>
            <template v-else-if="order.status === 'shipped'">
              <view class="action-btn secondary" @click.stop="viewLogistics(order)">查看物流</view>
              <view class="action-btn primary" @click.stop="confirmReceive(order)">确认收货</view>
            </template>
            <template v-else-if="order.status === 'completed'">
              <view class="action-btn primary" @click.stop="goResale(order)">去转售</view>
              <view class="action-btn secondary" @click.stop="goReview(order)">去评价</view>
            </template>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="loaded && filteredOrders.length === 0">
      <image class="empty-icon" src="/static/icons/order-empty.png" mode="aspectFit"></image>
      <text class="empty-text">暂无相关订单</text>
      <view class="empty-btn" @click="goGallery">去逛逛</view>
    </view>
  </view>
</template>

<script>
import { getOrderList, confirmReceive } from '@/api/order'
import { getFullImageUrl } from '@/utils/image.js'
import { fenToYuan, formatYuanNumber } from '@/utils/price'

const normalizeImage = (url) => {
  if (!url || typeof url !== 'string') return '/static/images/placeholder.png'
  const t = url.trim()
  if (!t || t === '[]' || t === '{}') return '/static/images/placeholder.png'
  return getFullImageUrl(t, '/static/images/placeholder.png')
}

const firstValue = (...values) => values.find(value => value !== undefined && value !== null && value !== '')

const isPurchasedOrder = (order = {}) => {
  const status = String(order.status || '').toUpperCase()
  const paymentStatus = String(order.paymentStatus || '').toUpperCase()
  if (status === 'PENDING_PAYMENT' || paymentStatus === 'UNPAID') return false
  return true
}

const normalizeOrderItem = (item = {}, order = {}) => {
  const material = firstValue(item.material, item.medium, item.artType, item.category, item.type)
  const size = firstValue(item.size, item.specName)
  const year = firstValue(item.year, item.createYear)
  const meta = [material, size, year].filter(Boolean).join(' / ')

  return {
    id: firstValue(item.id, item.orderItemId, item.artworkId, item.goodsId, item.productId, order.id),
    artworkId: firstValue(item.artworkId, item.goodsId, item.productId, item.id),
    cover: normalizeImage(firstValue(item.goodsImage, item.coverImage, item.cover, item.image, item.picUrl, item.thumbnail)),
    title: firstValue(item.goodsName, item.title, item.name, item.artworkTitle, item.productName, order.goodsName, order.title, '未命名作品'),
    artist: firstValue(item.artistName, item.authorName, item.sellerName, order.sellerName, ''),
    meta,
    price: firstValue(item.price, item.currentPrice, item.salePrice, item.payAmount, item.amount, item.subtotal, order.payAmount, order.amount, order.totalAmount, 0),
    num: Number(firstValue(item.quantity, item.count, item.num, 1)) || 1
  }
}

const extractOrderItems = (order = {}) => {
  const rawItems = firstValue(
    order.goodsList,
    order.items,
    order.goods,
    order.orderItems,
    order.details,
    order.products
  )
  if (Array.isArray(rawItems) && rawItems.length > 0) {
    return rawItems.map(item => normalizeOrderItem(item, order))
  }

  const hasTopLevelGoods = firstValue(
    order.goodsName,
    order.title,
    order.artworkTitle,
    order.coverImage,
    order.goodsImage,
    order.artworkId,
    order.goodsId
  )
  return hasTopLevelGoods ? [normalizeOrderItem(order, order)] : []
}

export default {
  data() {
    return {
      currentFilter: 'all',
      orders: [],
      loading: false,
      loaded: false
    }
  },

  computed: {
    filteredOrders() {
      if (this.currentFilter === 'all') return this.orders
      return this.orders.filter(o => o.status === this.currentFilter)
    }
  },

  onLoad(options = {}) {
    const filter = String(options.type || '').toLowerCase()
    if (['all', 'pending', 'shipped', 'completed'].includes(filter)) {
      this.currentFilter = filter
    }
    this.fetchOrders()
  },

  onShow() {
    // 从发布转售页面返回时刷新
    if (this.loaded) this.fetchOrders()
  },

  methods: {
    async fetchOrders() {
      if (this.loading) return
      this.loading = true
      try {
        const data = await getOrderList({ page: 1, pageSize: 50 })
        const rawList = data?.records || data?.list || data || []
        const list = Array.isArray(rawList) ? rawList : []

        // 后端状态映射为前端 short status
        const statusMap = {
          PENDING_PAYMENT: 'pending_payment',
          PAID: 'paid',
          SHIPPED: 'shipped',
          RECEIVED: 'received',
          COMPLETED: 'completed',
          CANCELLED: 'cancelled',
          REFUNDING: 'refunding',
          REFUNDED: 'refunded'
        }

        this.orders = list.filter(isPurchasedOrder).map(order => {
          const items = extractOrderItems(order)
          let shortStatus = statusMap[order.status] || order.status
          // 已付款/已收货统一归入"待发货"范围
          if (shortStatus === 'paid') shortStatus = 'pending'
          if (shortStatus === 'received') shortStatus = 'completed'

          return {
            id: order.id,
            createTime: order.createTime || order.createdAt || '',
            status: shortStatus,
            items,
            itemCount: items.reduce((sum, item) => sum + (Number(item.num) || 1), 0),
            total: order.payAmount || order.amount || order.totalAmount || 0
          }
        }).filter(o => o.status !== 'cancelled' && o.status !== 'refunding' && o.status !== 'refunded')

        this.loaded = true
      } catch (e) {
        console.warn('[已购作品] 加载失败:', e)
        this.orders = []
      } finally {
        this.loading = false
      }
    },

    switchFilter(filter) {
      this.currentFilter = filter
    },

    getStatusText(status) {
      const map = {
        pending: '待发货',
        shipped: '待收货',
        completed: '已完成',
        refunded: '已退款'
      }
      return map[status] || status
    },

    formatPrice(price) {
      if (!price && price !== 0) return ''
      return formatYuanNumber(fenToYuan(price))
    },

    goOrderDetail(id) {
      uni.navigateTo({ url: `/pages/order/detail?id=${id}` })
    },

    async confirmReceive(order) {
      uni.showModal({
        title: '确认收货',
        content: '确认已收到商品？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await confirmReceive(order.id)
            order.status = 'completed'
            uni.showToast({ title: '确认收货成功', icon: 'success' })
          } catch (e) {
            uni.showToast({ title: '操作失败', icon: 'none' })
          }
        }
      })
    },

    applyRefund(order) {
      uni.navigateTo({ url: `/pages/order/refund?orderId=${order.id}` })
    },

    viewLogistics(order) {
      uni.navigateTo({ url: `/pages/order/logistics?orderId=${order.id}` })
    },

    goReview(order) {
      uni.navigateTo({ url: `/pages/order/review?orderId=${order.id}` })
    },

    buyAgain(order) {
      uni.showToast({ title: '已加入购物车', icon: 'success' })
    },

    goResale(order) {
      // 跳转到发布转售页，携带第一个作品的 artworkId
      const artworkId = order.items[0]?.artworkId || order.items[0]?.id
      if (!artworkId) return
      uni.navigateTo({ url: `/pages/resale/publish?artworkId=${artworkId}` })
    },

    formatTime(time) {
      if (!time) return ''
      const d = new Date(time)
      if (isNaN(d.getTime())) return String(time).substring(0, 16).replace('T', ' ')
      return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
    },

    goGallery() {
      uni.switchTab({ url: '/pages/gallery/index' })
    }
  }
}
</script>

<style lang="scss" scoped>
.purchased-page {
  min-height: 100vh;
  background: #f5f6f7;
}

.filter-bar {
  display: flex;
  background: #fff;
  padding: 0 20rpx;

  .filter-item {
    flex: 1;
    padding: 28rpx 0;
    text-align: center;
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
        width: 40rpx;
        height: 4rpx;
        background: #667eea;
        border-radius: 2rpx;
      }
    }
  }
}

.order-list {
  padding: 20rpx;

  .order-card {
    background: #fff;
    border-radius: 16rpx;
    margin-bottom: 20rpx;
    overflow: hidden;

    .order-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24rpx 30rpx;
      border-bottom: 1rpx solid #f5f5f5;

      .order-time {
        font-size: 24rpx;
        color: #999;
      }

      .order-status {
        font-size: 26rpx;
        font-weight: 500;

        &.status-pending { color: #ff9800; }
        &.status-shipped { color: #667eea; }
        &.status-completed { color: #52c41a; }
      }
    }

    .order-items {
      .order-item {
        display: flex;
        padding: 24rpx 30rpx;

        .item-image {
          width: 160rpx;
          height: 160rpx;
          border-radius: 12rpx;
          flex-shrink: 0;
        }

        .item-info {
          flex: 1;
          margin-left: 20rpx;
          display: flex;
          flex-direction: column;
          justify-content: center;

          .item-title {
            font-size: 28rpx;
            color: #333;
            font-weight: 500;
          }

          .item-artist {
            font-size: 24rpx;
            color: #999;
            margin-top: 8rpx;
          }

          .item-meta {
            font-size: 23rpx;
            color: #9b958a;
            margin-top: 8rpx;
            line-height: 1.35;
          }
        }

        .item-price {
          display: flex;
          flex-direction: column;
          align-items: flex-end;
          justify-content: center;

          text {
            font-size: 28rpx;
            color: #333;
            font-weight: 500;
          }

          .item-num {
            font-size: 24rpx;
            color: #999;
            font-weight: normal;
            margin-top: 8rpx;
          }
        }
      }
    }

    .order-footer {
      padding: 20rpx 30rpx 30rpx;

      .order-total {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        margin-bottom: 20rpx;

        text {
          font-size: 26rpx;
          color: #666;
        }

        .total-price {
          margin-left: 16rpx;
          font-size: 30rpx;
          font-weight: 600;
          color: #333;
        }
      }

      .order-actions {
        display: flex;
        justify-content: flex-end;
        gap: 16rpx;

        .action-btn {
          padding: 12rpx 28rpx;
          font-size: 26rpx;
          border-radius: 32rpx;

          &.primary {
            color: #fff;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          }

          &.secondary {
            color: #666;
            background: #f5f5f5;
          }
        }
      }
    }
  }
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 120rpx 0;
  font-size: 28rpx;
  color: #8f8a80;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;

  .empty-icon {
    width: 200rpx;
    height: 200rpx;
  }

  .empty-text {
    margin-top: 32rpx;
    font-size: 28rpx;
    color: #999;
  }

  .empty-btn {
    margin-top: 32rpx;
    padding: 20rpx 60rpx;
    font-size: 28rpx;
    color: #667eea;
    border: 2rpx solid #667eea;
    border-radius: 40rpx;
  }
}

/* 已购作品暗色视觉优化 */
.purchased-page {
  background: #0b0b0c;
  color: #f6f2e8;
}

.filter-bar {
  background: rgba(11, 11, 12, 0.96);
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
  padding: 0 24rpx;

  .filter-item {
    color: #8f8a80;
    font-weight: 600;

    &.active {
      color: #f6f2e8;

      &::after {
        background: #c9a227;
      }
    }
  }
}

.order-list {
  padding: 24rpx;

  .order-card {
    background: #171719;
    border: 1rpx solid rgba(255, 255, 255, 0.08);
    border-radius: 18rpx;
    box-shadow: 0 18rpx 42rpx rgba(0, 0, 0, 0.22);

    .order-header {
      border-bottom-color: rgba(255, 255, 255, 0.08);

      .order-time {
        color: #8f8a80;
      }

      .order-status {
        font-weight: 800;
      }
    }

    .order-items .order-item {
      .item-image {
        background: #202024;
      }

      .item-info {
        .item-title {
          color: #f6f2e8;
          font-weight: 800;
        }

        .item-artist {
          color: #8f8a80;
        }

        .item-meta {
          color: #8f8a80;
        }
      }

      .item-price {
        text {
          color: #f2c85b;
        }

        .item-num {
          color: #8f8a80;
        }
      }
    }

    .order-footer {
      .order-total {
        text {
          color: #8f8a80;
        }

        .total-price {
          color: #f2c85b;
        }
      }

      .order-actions .action-btn {
        &.primary {
          color: #16130b;
          background: #c9a227;
          font-weight: 800;
        }

        &.secondary {
          color: #f6f2e8;
          background: #202024;
          border: 1rpx solid rgba(255, 255, 255, 0.08);
        }
      }
    }
  }
}

.empty-state {
  min-height: 58vh;
  justify-content: center;

  .empty-icon {
    opacity: 0.62;
  }

  .empty-text {
    color: #8f8a80;
  }

  .empty-btn {
    color: #c9a227;
    border-color: rgba(201, 162, 39, 0.55);
  }
}
</style>
