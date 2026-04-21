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

    <!-- 订单列表 -->
    <view class="order-list">
      <view class="order-card" v-for="order in filteredOrders" :key="order.id">
        <view class="order-header">
          <view class="order-time">{{ order.createTime }}</view>
          <view class="order-status" :class="'status-' + order.status">
            {{ getStatusText(order.status) }}
          </view>
        </view>

        <view class="order-items">
          <view class="order-item" v-for="item in order.items" :key="item.id" @click="goOrderDetail(order.id)">
            <image class="item-image" :src="item.cover" mode="aspectFill"></image>
            <view class="item-info">
              <view class="item-title">{{ item.title }}</view>
              <view class="item-artist">{{ item.artist }}</view>
            </view>
            <view class="item-price">
              <text>¥{{ item.price }}</text>
              <text class="item-num">x{{ item.num }}</text>
            </view>
          </view>
        </view>

        <view class="order-footer">
          <view class="order-total">
            <text>共{{ order.items.length }}件商品</text>
            <text class="total-price">合计：¥{{ order.total }}</text>
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
              <view class="action-btn secondary" @click.stop="goReview(order)">去评价</view>
              <view class="action-btn secondary" @click.stop="buyAgain(order)">再次购买</view>
            </template>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="filteredOrders.length === 0">
      <image class="empty-icon" src="/static/icons/order-empty.png" mode="aspectFit"></image>
      <text class="empty-text">暂无相关订单</text>
      <view class="empty-btn" @click="goGallery">去逛逛</view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentFilter: 'all',
      orders: [
        {
          id: 1,
          createTime: '2024-01-15 14:30',
          status: 'pending',
          items: [
            { id: 1, cover: 'https://picsum.photos/200/200?random=20', title: '山水之间', artist: '李明', price: '8888', num: 1 }
          ],
          total: '8888.00'
        },
        {
          id: 2,
          createTime: '2024-01-12 09:20',
          status: 'shipped',
          items: [
            { id: 2, cover: 'https://picsum.photos/200/200?random=21', title: '春意盎然', artist: '王芳', price: '12800', num: 1 }
          ],
          total: '12800.00'
        },
        {
          id: 3,
          createTime: '2024-01-08 16:45',
          status: 'completed',
          items: [
            { id: 3, cover: 'https://picsum.photos/200/200?random=22', title: '都市夜景', artist: '张伟', price: '15600', num: 1 }
          ],
          total: '15600.00'
        }
      ]
    }
  },

  computed: {
    filteredOrders() {
      if (this.currentFilter === 'all') {
        return this.orders
      }
      return this.orders.filter(order => order.status === this.currentFilter)
    }
  },

  methods: {
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

    goOrderDetail(id) {
      uni.navigateTo({
        url: `/pages/order/detail?id=${id}`
      })
    },

    applyRefund(order) {
      uni.navigateTo({
        url: `/pages/order/refund?orderId=${order.id}`
      })
    },

    viewLogistics(order) {
      uni.navigateTo({
        url: `/pages/order/logistics?orderId=${order.id}`
      })
    },

    confirmReceive(order) {
      uni.showModal({
        title: '确认收货',
        content: '确认已收到商品？',
        success: (res) => {
          if (res.confirm) {
            order.status = 'completed'
            uni.showToast({ title: '确认收货成功', icon: 'success' })
          }
        }
      })
    },

    goReview(order) {
      uni.navigateTo({
        url: `/pages/order/review?orderId=${order.id}`
      })
    },

    buyAgain(order) {
      uni.showToast({ title: '已加入购物车', icon: 'success' })
    },

    goGallery() {
      uni.switchTab({
        url: '/pages/gallery/index'
      })
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
</style>
