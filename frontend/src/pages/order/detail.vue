<template>
  <view class="order-detail">
    <view class="status-banner" :class="statusClass">
      <view>
        <text class="status-label">订单状态</text>
        <text class="status-text">{{ getStatusText(orderInfo.status) }}</text>
      </view>
      <text class="status-no">{{ orderInfo.orderNo || '--' }}</text>
    </view>

    <view v-if="loading" class="state-panel">订单加载中...</view>
    <view v-else-if="loadError" class="state-panel error">
      <text>{{ loadError }}</text>
      <view class="retry-btn" @click="loadOrderDetail">重新加载</view>
    </view>

    <template v-else>
      <view class="card address-card" v-if="hasAddress">
        <text class="card-title">收货信息</text>
        <view class="address-user">
          <text class="user-name">{{ orderInfo.address.receiverName || '--' }}</text>
          <text class="user-phone">{{ orderInfo.address.receiverPhone || '--' }}</text>
        </view>
        <text class="address-detail">{{ fullAddress }}</text>
      </view>

      <view class="card goods-card">
        <view class="goods-header">
          <image class="shop-logo" :src="orderInfo.sellerAvatar || '/static/images/avatar.png'" mode="aspectFill"></image>
          <text class="shop-name">{{ orderInfo.sellerName || '拾艺局旗舰店' }}</text>
        </view>
        <view
          class="goods-item"
          v-for="item in orderInfo.goodsList"
          :key="item.id"
          @click="goGoodsDetail(item.goodsId)"
        >
          <image class="goods-image" :src="getFullImageUrl(item.goodsImage)" mode="aspectFill"></image>
          <view class="goods-info">
            <text class="goods-title">{{ item.goodsName }}</text>
            <text class="goods-spec" v-if="item.specName">{{ item.specName }}</text>
            <view class="goods-bottom">
              <text class="goods-price">¥{{ formatMoney(item.price) }}</text>
              <text class="goods-count">x{{ item.count }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="card detail-card">
        <text class="card-title">订单信息</text>
        <view class="info-row">
          <text class="info-label">订单编号</text>
          <view class="info-value">
            <text>{{ orderInfo.orderNo || '--' }}</text>
            <text class="copy-btn" @click="copyOrderNo">复制</text>
          </view>
        </view>
        <view class="info-row">
          <text class="info-label">下单时间</text>
          <text class="info-value">{{ formatDateTime(orderInfo.createTime) }}</text>
        </view>
        <view class="info-row" v-if="orderInfo.payTime">
          <text class="info-label">支付时间</text>
          <text class="info-value">{{ formatDateTime(orderInfo.payTime) }}</text>
        </view>
        <view class="info-row" v-if="orderInfo.deliveryTime">
          <text class="info-label">发货时间</text>
          <text class="info-value">{{ formatDateTime(orderInfo.deliveryTime) }}</text>
        </view>
        <view class="info-row" v-if="orderInfo.completeTime">
          <text class="info-label">完成时间</text>
          <text class="info-value">{{ formatDateTime(orderInfo.completeTime) }}</text>
        </view>
        <view class="info-row" v-if="orderInfo.remark">
          <text class="info-label">订单备注</text>
          <text class="info-value remark">{{ orderInfo.remark }}</text>
        </view>
      </view>

      <view class="card amount-card">
        <text class="card-title">金额明细</text>
        <view class="amount-row">
          <text class="amount-label">商品金额</text>
          <text class="amount-value">¥{{ formatMoney(orderInfo.goodsAmount) }}</text>
        </view>
        <view class="amount-row">
          <text class="amount-label">运费</text>
          <text class="amount-value">¥{{ formatMoney(orderInfo.freight) }}</text>
        </view>
        <view class="amount-row" v-if="Number(orderInfo.discountAmount) > 0">
          <text class="amount-label">优惠</text>
          <text class="amount-value discount">-¥{{ formatMoney(orderInfo.discountAmount) }}</text>
        </view>
        <view class="amount-row total">
          <text class="amount-label">实付款</text>
          <text class="amount-value">¥{{ formatMoney(orderInfo.payAmount) }}</text>
        </view>
      </view>
    </template>

    <view class="bottom-bar" v-if="!loading && !loadError">
      <view class="ghost-btn" @click="contactSeller">联系卖家</view>
      <view class="ghost-btn" @click="viewLogistics" v-if="['SHIPPED', 'COMPLETED'].includes(orderInfo.status)">查看物流</view>
      <view class="ghost-btn" @click="cancelOrder" v-if="orderInfo.status === 'PENDING_PAYMENT'">取消订单</view>
      <view class="primary-btn" @click="payOrder" v-if="orderInfo.status === 'PENDING_PAYMENT'">去支付</view>
      <view class="primary-btn" @click="confirmReceive" v-if="orderInfo.status === 'SHIPPED'">确认收货</view>
      <view class="ghost-btn" @click="applyRefund" v-if="['PENDING_PAYMENT', 'PAID', 'SHIPPED'].includes(orderInfo.status)">申请退款</view>
      <view class="primary-btn" @click="reviewOrder" v-if="orderInfo.status === 'COMPLETED'">去评价</view>
    </view>
  </view>
</template>

<script>
import { getOrderDetail, cancelOrder, confirmReceive } from '@/api/order.js'
import { getFullImageUrl } from '@/utils/image.js'

export default {
  data() {
    return {
      loading: false,
      loadError: '',
      orderId: '',
      orderInfo: this.emptyOrder()
    }
  },

  computed: {
    statusClass() {
      return `status-${String(this.orderInfo.status || 'unknown').toLowerCase()}`
    },
    fullAddress() {
      const address = this.orderInfo.address || {}
      return address.fullAddress || [
        address.province,
        address.city,
        address.district,
        address.detail
      ].filter(Boolean).join('')
    },
    hasAddress() {
      const address = this.orderInfo.address || {}
      return Boolean(
        address.receiverName ||
        address.receiverPhone ||
        address.fullAddress ||
        address.province ||
        address.city ||
        address.district ||
        address.detail
      )
    }
  },

  onLoad(options) {
    this.orderId = options.id || options.orderId || ''
    if (this.orderId) {
      this.loadOrderDetail()
    } else {
      this.loadError = '订单参数缺失'
    }
  },

  methods: {
    emptyOrder() {
      return {
        status: '',
        address: null,
        goodsList: [],
        goodsAmount: 0,
        freight: 0,
        discountAmount: 0,
        payAmount: 0
      }
    },

    async loadOrderDetail() {
      this.loading = true
      this.loadError = ''
      try {
        const detail = await getOrderDetail(this.orderId)
        if (!detail) throw new Error('订单不存在')
        this.orderInfo = {
          ...this.emptyOrder(),
          ...detail,
          goodsList: detail.goodsList || []
        }
      } catch (e) {
        this.orderInfo = this.emptyOrder()
        this.loadError = e?.message || '订单加载失败'
      } finally {
        this.loading = false
      }
    },

    formatMoney(value) {
      if (value === null || value === undefined || value === '') return '0.00'
      return (Number(value) / 100).toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })
    },

    formatDateTime(value) {
      if (!value) return '--'
      return String(value).replace('T', ' ')
    },

    getStatusText(status) {
      const map = {
        PENDING_PAYMENT: '待支付',
        PAID: '待发货',
        SHIPPED: '运输中',
        RECEIVED: '已收货',
        COMPLETED: '已完成',
        CANCELLED: '已取消',
        REFUNDING: '退款中',
        REFUNDED: '已退款'
      }
      return map[status] || '未知状态'
    },

    copyOrderNo() {
      if (!this.orderInfo.orderNo) return
      uni.setClipboardData({
        data: this.orderInfo.orderNo,
        success: () => uni.showToast({ title: '复制成功', icon: 'success' })
      })
    },

    goGoodsDetail(id) {
      if (id) uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
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
          if (!res.confirm) return
          try {
            await cancelOrder(this.orderId)
            uni.showToast({ title: '订单已取消', icon: 'success' })
            this.orderInfo.status = 'CANCELLED'
          } catch (e) {
            uni.showToast({ title: '取消失败', icon: 'none' })
          }
        }
      })
    },

    payOrder() {
      uni.navigateTo({ url: `/pages/order/pay?orderId=${this.orderId}&amount=${this.orderInfo.payAmount}` })
    },

    confirmReceive() {
      uni.showModal({
        title: '确认收货',
        content: '请确认您已收到商品且商品完好无损',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await confirmReceive(this.orderId)
            uni.showToast({ title: '已确认收货', icon: 'success' })
            this.orderInfo.status = 'COMPLETED'
          } catch (e) {
            uni.showToast({ title: '确认失败', icon: 'none' })
          }
        }
      })
    },

    applyRefund() {
      uni.navigateTo({ url: `/pages/order/refund?id=${this.orderId}` })
    },

    reviewOrder() {
      uni.navigateTo({ url: `/pages/order/review?id=${this.orderId}` })
    },

    getFullImageUrl
  }
}
</script>

<style lang="scss" scoped>
.order-detail {
  min-height: 100vh;
  background: #0d0d0f;
  color: #f5f0e8;
  padding: 20rpx 20rpx 150rpx;
  box-sizing: border-box;
}

.status-banner,
.card,
.state-panel {
  border: 1rpx solid rgba(214, 170, 76, 0.18);
  border-radius: 18rpx;
  background: #171719;
}

.status-banner {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  min-height: 132rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-sizing: border-box;
  background: linear-gradient(135deg, rgba(212, 158, 45, 0.24), rgba(23, 23, 25, 0.95));
}

.status-label {
  display: block;
  color: #a9a39a;
  font-size: 24rpx;
  margin-bottom: 8rpx;
}

.status-text {
  display: block;
  color: #f2c65e;
  font-size: 38rpx;
  font-weight: 700;
}

.status-no {
  max-width: 320rpx;
  color: #8d877f;
  font-size: 22rpx;
  text-align: right;
  word-break: break-all;
}

.state-panel {
  padding: 42rpx 28rpx;
  text-align: center;
  color: #c8c2b9;
}

.state-panel.error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}

.retry-btn {
  color: #f2c65e;
}

.card {
  margin-bottom: 20rpx;
  padding: 26rpx;
}

.card-title {
  display: block;
  color: #f5f0e8;
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 22rpx;
}

.address-user {
  display: flex;
  gap: 18rpx;
  align-items: center;
  margin-bottom: 12rpx;
}

.user-name {
  font-size: 30rpx;
  font-weight: 600;
}

.user-phone,
.address-detail {
  color: #aaa39a;
  font-size: 25rpx;
}

.goods-header {
  display: flex;
  align-items: center;
  margin-bottom: 18rpx;
}

.shop-logo {
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  margin-right: 12rpx;
}

.shop-name {
  color: #e8dfd0;
  font-size: 26rpx;
}

.goods-item {
  display: flex;
  gap: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.goods-image {
  width: 138rpx;
  height: 138rpx;
  flex: 0 0 138rpx;
  border-radius: 10rpx;
  background: #232326;
}

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-title {
  display: -webkit-box;
  overflow: hidden;
  color: #f5f0e8;
  font-size: 27rpx;
  line-height: 1.4;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.goods-spec {
  display: block;
  color: #8e877d;
  font-size: 23rpx;
  margin-top: 8rpx;
}

.goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 22rpx;
}

.goods-price {
  color: #f2c65e;
  font-size: 28rpx;
  font-weight: 600;
}

.goods-count {
  color: #8e877d;
  font-size: 24rpx;
}

.info-row,
.amount-row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  align-items: center;
  min-height: 54rpx;
}

.info-label,
.amount-label {
  color: #8e877d;
  font-size: 25rpx;
}

.info-value,
.amount-value {
  color: #f5f0e8;
  font-size: 25rpx;
  text-align: right;
}

.info-value {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.info-value.remark {
  max-width: 420rpx;
  line-height: 1.45;
}

.copy-btn {
  color: #f2c65e;
  font-size: 22rpx;
  padding: 4rpx 10rpx;
  border: 1rpx solid rgba(242, 198, 94, 0.4);
  border-radius: 999rpx;
}

.amount-row.total {
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
  margin-top: 14rpx;
  padding-top: 16rpx;
}

.amount-row.total .amount-value {
  color: #f2c65e;
  font-size: 34rpx;
  font-weight: 700;
}

.amount-value.discount {
  color: #d97979;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 12rpx;
  align-items: center;
  justify-content: flex-end;
  min-height: 110rpx;
  padding: 18rpx 20rpx calc(18rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: rgba(14, 14, 16, 0.96);
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.ghost-btn,
.primary-btn {
  min-width: 128rpx;
  height: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 22rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
}

.ghost-btn {
  color: #dfd6c8;
  border: 1rpx solid rgba(255, 255, 255, 0.14);
}

.primary-btn {
  color: #16120b;
  background: #d9aa3d;
  font-weight: 600;
}
</style>
