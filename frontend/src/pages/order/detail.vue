<template>
  <view class="order-detail">
    <!-- 状态栏 -->
    <view class="status-bar" :class="'status-' + orderInfo.status">
      <view class="status-content">
        <text class="status-icon" :class="'icon-' + orderInfo.status"></text>
        <text class="status-text">{{ getStatusText(orderInfo.status) }}</text>
      </view>
    </view>

    <!-- 收货地址 -->
    <view class="address-section" v-if="orderInfo.address">
      <view class="address-icon">
        <u-icon name="map" size="40" color="#667eea"></u-icon>
      </view>
      <view class="address-info">
        <view class="address-user">
          <text class="user-name">{{ orderInfo.address.receiverName }}</text>
          <text class="user-phone">{{ orderInfo.address.receiverPhone }}</text>
        </view>
        <text class="address-detail">{{ orderInfo.address.province }}{{ orderInfo.address.city }}{{ orderInfo.address.district }}{{ orderInfo.address.detail }}</text>
      </view>
    </view>

    <!-- 商品列表 -->
    <view class="goods-section">
      <view class="goods-header">
        <image class="shop-logo" :src="orderInfo.sellerAvatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
        <text class="shop-name">{{ orderInfo.sellerName || '拾艺局旗舰店' }}</text>
      </view>
      <view class="goods-list">
        <view class="goods-item" v-for="item in orderInfo.goodsList" :key="item.id" @click="goGoodsDetail(item.goodsId)">
          <image class="goods-image" :src="item.goodsImage" mode="aspectFill"></image>
          <view class="goods-info">
            <text class="goods-title">{{ item.goodsName }}</text>
            <text class="goods-spec" v-if="item.specName">{{ item.specName }}</text>
            <view class="goods-bottom">
              <text class="goods-price">¥{{ item.price }}</text>
              <text class="goods-count">x{{ item.count }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 订单信息 -->
    <view class="order-info-section">
      <view class="info-row">
        <text class="info-label">订单编号</text>
        <view class="info-value">
          <text>{{ orderInfo.orderNo }}</text>
          <text class="copy-btn" @click="copyOrderNo">复制</text>
        </view>
      </view>
      <view class="info-row">
        <text class="info-label">下单时间</text>
        <text class="info-value">{{ orderInfo.createTime }}</text>
      </view>
      <view class="info-row" v-if="orderInfo.payTime">
        <text class="info-label">支付时间</text>
        <text class="info-value">{{ orderInfo.payTime }}</text>
      </view>
      <view class="info-row" v-if="orderInfo.deliveryTime">
        <text class="info-label">发货时间</text>
        <text class="info-value">{{ orderInfo.deliveryTime }}</text>
      </view>
      <view class="info-row" v-if="orderInfo.completeTime">
        <text class="info-label">完成时间</text>
        <text class="info-value">{{ orderInfo.completeTime }}</text>
      </view>
      <view class="info-row" v-if="orderInfo.remark">
        <text class="info-label">订单备注</text>
        <text class="info-value remark">{{ orderInfo.remark }}</text>
      </view>
    </view>

    <!-- 金额明细 -->
    <view class="amount-section">
      <view class="amount-row">
        <text class="amount-label">商品金额</text>
        <text class="amount-value">¥{{ orderInfo.goodsAmount }}</text>
      </view>
      <view class="amount-row">
        <text class="amount-label">运费</text>
        <text class="amount-value">¥{{ orderInfo.freight || '0.00' }}</text>
      </view>
      <view class="amount-row" v-if="orderInfo.discountAmount > 0">
        <text class="amount-label">优惠</text>
        <text class="amount-value discount">-¥{{ orderInfo.discountAmount }}</text>
      </view>
      <view class="amount-row total">
        <text class="amount-label">合计</text>
        <text class="amount-value">¥{{ orderInfo.totalAmount }}</text>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="left-actions">
        <view class="action-btn btn-contact" @click="contactSeller">
          <u-icon name="chat" size="18" color="#666"></u-icon>
          <text>联系卖家</text>
        </view>
        <view class="action-btn" @click="viewLogistics" v-if="orderInfo.status === 'shipped' || orderInfo.status === 'completed'">
          <u-icon name="bag" size="18" color="#666"></u-icon>
          <text>查看物流</text>
        </view>
      </view>
      <view class="right-actions">
        <view class="action-btn btn-cancel" @click="cancelOrder" v-if="orderInfo.status === 'pending'">取消订单</view>
        <view class="action-btn btn-pay" @click="payOrder" v-if="orderInfo.status === 'pending'">去支付</view>
        <view class="action-btn btn-confirm" @click="confirmReceive" v-if="orderInfo.status === 'shipped'">确认收货</view>
        <view class="action-btn" @click="applyRefund" v-if="['pending', 'paid', 'shipped'].includes(orderInfo.status)">申请退款</view>
        <view class="action-btn btn-review" @click="reviewOrder" v-if="orderInfo.status === 'completed'">去评价</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getOrderDetail, cancelOrder, confirmReceive } from '@/api/order.js'

export default {
  data() {
    return {
      orderId: '',
      orderInfo: {
        status: 'pending',
        address: {},
        goodsList: [],
        goodsAmount: 0,
        freight: 0,
        discountAmount: 0,
        totalAmount: 0
      }
    }
  },

  onLoad(options) {
    if (options.id) {
      this.orderId = options.id
      this.loadOrderDetail()
    }
  },

  methods: {
    async loadOrderDetail() {
      try {
        uni.showLoading({ title: '加载中...' })
        const res = await getOrderDetail(this.orderId)
        this.orderInfo = res.data || this.getMockData()
        uni.hideLoading()
      } catch (e) {
        uni.hideLoading()
        this.orderInfo = this.getMockData()
      }
    },

    getMockData() {
      return {
        orderNo: 'SYJ202404210001',
        status: 'pending',
        createTime: '2024-04-21 14:30:25',
        address: {
          receiverName: '张三',
          receiverPhone: '138****8888',
          province: '北京市',
          city: '北京市',
          district: '朝阳区',
          detail: 'XX街道XX小区XX栋XX室'
        },
        sellerName: '国画艺术家旗舰店',
        goodsList: [
          {
            id: 1,
            goodsId: 101,
            goodsName: '山水长卷·云岭晴岚',
            goodsImage: '/static/product/demo1.jpg',
            specName: '立轴 180x68cm',
            price: 128000,
            count: 1
          }
        ],
        goodsAmount: 128000,
        freight: 0,
        discountAmount: 0,
        totalAmount: 128000,
        remark: ''
      }
    },

    getStatusText(status) {
      const map = {
        pending: '待支付',
        paid: '待发货',
        shipped: '运输中',
        completed: '已完成',
        cancelled: '已取消',
        refunded: '已退款'
      }
      return map[status] || '未知状态'
    },

    copyOrderNo() {
      uni.setClipboardData({
        data: this.orderInfo.orderNo,
        success: () => {
          uni.showToast({ title: '复制成功', icon: 'success' })
        }
      })
    },

    goGoodsDetail(id) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    },

    contactSeller() {
      uni.navigateTo({ url: `/pages/message/chat?type=seller&orderId=${this.orderId}` })
    },

    viewLogistics() {
      uni.navigateTo({ url: `/pages/order/logistics?id=${this.orderId}` })
    },

    cancelOrder() {
      uni.showModal({
        title: '提示',
        content: '确定要取消该订单吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await cancelOrder(this.orderId)
              uni.showToast({ title: '订单已取消', icon: 'success' })
              this.orderInfo.status = 'cancelled'
            } catch (e) {
              uni.showToast({ title: '取消失败', icon: 'none' })
            }
          }
        }
      })
    },

    payOrder() {
      uni.navigateTo({ url: `/pages/order/pay?id=${this.orderId}&amount=${this.orderInfo.totalAmount}` })
    },

    confirmReceive() {
      uni.showModal({
        title: '确认收货',
        content: '请确认您已收到商品且商品完好无损',
        success: async (res) => {
          if (res.confirm) {
            try {
              await confirmReceive(this.orderId)
              uni.showToast({ title: '已确认收货', icon: 'success' })
              this.orderInfo.status = 'completed'
            } catch (e) {
              uni.showToast({ title: '确认失败', icon: 'none' })
            }
          }
        }
      })
    },

    applyRefund() {
      uni.navigateTo({ url: `/pages/order/refund?id=${this.orderId}` })
    },

    reviewOrder() {
      uni.navigateTo({ url: `/pages/order/review?id=${this.orderId}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.order-detail {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.status-bar {
  padding: 40rpx 30rpx;
  color: #fff;

  &.status-pending {
    background: linear-gradient(135deg, #ff9800, #ff5722);
  }

  &.status-paid {
    background: linear-gradient(135deg, #3498db, #2980b9);
  }

  &.status-shipped {
    background: linear-gradient(135deg, #9c27b0, #673ab7);
  }

  &.status-completed {
    background: linear-gradient(135deg, #50c878, #2ecc71);
  }

  &.status-cancelled, &.status-refunded {
    background: #999;
  }
}

.status-content {
  display: flex;
  align-items: center;
}

.status-text {
  font-size: 32rpx;
  font-weight: 600;
  margin-left: 16rpx;
}

.address-section {
  display: flex;
  align-items: center;
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.address-icon {
  margin-right: 20rpx;
}

.address-info {
  flex: 1;
}

.address-user {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}

.user-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-right: 20rpx;
}

.user-phone {
  font-size: 28rpx;
  color: #666;
}

.address-detail {
  font-size: 26rpx;
  color: #999;
}

.goods-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.goods-header {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}

.shop-logo {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  margin-right: 12rpx;
}

.shop-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.goods-item {
  display: flex;
  padding: 20rpx 0;
  border-top: 1rpx solid #f5f5f5;
}

.goods-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  margin-right: 20rpx;
  background: #f0f0f0;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.goods-title {
  font-size: 28rpx;
  color: #333;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.goods-spec {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
}

.goods-price {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
}

.goods-count {
  font-size: 26rpx;
  color: #999;
}

.order-info-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.info-label {
  font-size: 26rpx;
  color: #999;
}

.info-value {
  font-size: 26rpx;
  color: #333;
  display: flex;
  align-items: center;

  &.remark {
    color: #666;
    max-width: 400rpx;
    text-align: right;
  }
}

.copy-btn {
  font-size: 22rpx;
  color: #667eea;
  margin-left: 16rpx;
  padding: 4rpx 12rpx;
  border: 1rpx solid #667eea;
  border-radius: 8rpx;
}

.amount-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;

  &.total {
    padding-top: 24rpx;
    border-top: 1rpx solid #f5f5f5;
    margin-top: 12rpx;
  }
}

.amount-label {
  font-size: 26rpx;
  color: #666;
}

.amount-value {
  font-size: 26rpx;
  color: #333;

  &.discount {
    color: #e74c3c;
  }
}

.total .amount-value {
  font-size: 36rpx;
  font-weight: 600;
  color: #e74c3c;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.left-actions, .right-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 64rpx;
  padding: 0 28rpx;
  border-radius: 32rpx;
  font-size: 26rpx;
  border: 1rpx solid #ddd;
  color: #666;
  background: #fff;

  text {
    margin-left: 6rpx;
  }

  &.btn-cancel {
    border-color: #ddd;
    color: #999;
  }

  &.btn-pay {
    background: linear-gradient(135deg, #e74c3c, #c0392b);
    color: #fff;
    border: none;
  }

  &.btn-confirm {
    background: linear-gradient(135deg, #50c878, #2ecc71);
    color: #fff;
    border: none;
  }

  &.btn-review {
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: #fff;
    border: none;
  }
}
</style>