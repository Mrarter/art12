<template>
  <view class="refund-page">
    <!-- 订单信息 -->
    <view class="order-info">
      <image class="product-image" :src="order.cover || '/static/icons/artwork-default.png'" mode="aspectFill"></image>
      <view class="product-detail">
        <text class="product-name">{{ order.title }}</text>
        <text class="product-author">{{ order.artistName }}</text>
        <view class="product-price">
          <text class="price">¥{{ formatPrice(order.price) }}</text>
          <text class="count">x{{ order.quantity }}</text>
        </view>
      </view>
    </view>

    <!-- 退款类型 -->
    <view class="refund-type card">
      <view class="section-title">退款原因</view>
      <view class="type-options">
        <view
          class="type-item"
          :class="{ active: refundType === 'refund' }"
          @click="refundType = 'refund'"
        >
          <u-icon name="red-packet" size="24" :color="refundType === 'refund' ? '#fff' : '#e74c3c'"></u-icon>
          <text>仅退款</text>
          <text class="type-desc">未收到货或不需要商品</text>
        </view>
        <view
          class="type-item"
          :class="{ active: refundType === 'return' }"
          @click="refundType = 'return'"
        >
          <u-icon name="bag" size="24" :color="refundType === 'return' ? '#fff' : '#e74c3c'"></u-icon>
          <text>退款退货</text>
          <text class="type-desc">已收到货，需要退货退款</text>
        </view>
      </view>
    </view>

    <!-- 退款金额 -->
    <view class="refund-amount card">
      <view class="section-title">退款金额</view>
      <view class="amount-display">
        <text class="currency">¥</text>
        <text class="amount">{{ formatPrice(refundAmount) }}</text>
      </view>
      <view class="amount-detail">
        <view class="detail-item">
          <text class="detail-label">商品金额</text>
          <text class="detail-value">¥{{ formatPrice(order.price * order.quantity) }}</text>
        </view>
        <view class="detail-item">
          <text class="detail-label">运费</text>
          <text class="detail-value">¥{{ formatPrice(order.freight || 0) }}</text>
        </view>
        <view class="detail-item" v-if="order.couponAmount > 0">
          <text class="detail-label">优惠券</text>
          <text class="detail-value">-¥{{ formatPrice(order.couponAmount) }}</text>
        </view>
      </view>
    </view>

    <!-- 退款说明 -->
    <view class="refund-reason card">
      <view class="section-title">退款说明</view>
      <textarea
        class="reason-input"
        v-model="reason"
        placeholder="请详细描述退款原因（选填）"
        maxlength="200"
      ></textarea>
      <view class="char-count">{{ reason.length }}/200</view>
    </view>

    <!-- 上传凭证 -->
    <view class="refund-images card" v-if="refundType === 'return'">
      <view class="section-title">上传凭证（选填）</view>
      <view class="images-grid">
        <view class="image-item" v-for="(img, index) in images" :key="index">
          <image :src="img" mode="aspectFill"></image>
          <view class="remove-btn" @click="removeImage(index)">
            <text>×</text>
          </view>
        </view>
        <view class="add-image" v-if="images.length < 3" @click="chooseImage">
          <u-icon name="camera" size="40" color="#ccc"></u-icon>
          <text>添加图片</text>
        </view>
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-bar">
      <button class="submit-btn" @click="submitRefund">提交申请</button>
    </view>
  </view>
</template>

<script>
import { refundApply } from '@/api/order'
import { uploadFile } from '@/api/file'

export default {
  data() {
    return {
      orderId: null,
      order: {
        title: '',
        artistName: '',
        price: 0,
        quantity: 1,
        freight: 0,
        couponAmount: 0,
        cover: ''
      },
      refundType: 'refund',
      reason: '',
      images: []
    }
  },

  computed: {
    refundAmount() {
      const goodsAmount = (this.order.price || 0) * (this.order.quantity || 1)
      const freight = this.order.freight || 0
      const coupon = this.order.couponAmount || 0
      return goodsAmount + freight - coupon
    }
  },

  onLoad(options) {
    if (options.orderId) {
      this.orderId = Number(options.orderId)
      this.loadOrderInfo()
    }
    if (options.orderData) {
      try {
        this.order = JSON.parse(decodeURIComponent(options.orderData))
      } catch (e) {
        console.error('解析订单数据失败', e)
      }
    }
  },

  methods: {
    loadOrderInfo() {
      // 从上一页传递的数据或从接口获取
    },

    async submitRefund() {
      if (this.refundType === 'return' && this.images.length > 0) {
        // 上传图片
        uni.showLoading({ title: '提交中...' })
        try {
          const uploadPromises = this.images.map(img => {
            if (img.startsWith('blob:') || img.startsWith('file:') || img.startsWith('/tmp')) {
              return uploadFile(img)
            }
            return Promise.resolve(img)
          })
          this.images = await Promise.all(uploadPromises)
        } catch (e) {
          console.error('上传图片失败', e)
        }
        uni.hideLoading()
      }

      try {
        uni.showLoading({ title: '提交中...' })
        const res = await refundApply({
          orderId: this.orderId,
          type: this.refundType,
          reason: this.reason,
          images: this.images.join(','),
          amount: this.refundAmount
        })

        if (res.code === 200) {
          uni.showToast({ title: '提交成功', icon: 'success' })
          setTimeout(() => {
            uni.navigateBack()
          }, 1500)
        } else {
          uni.showToast({ title: res.message || '提交失败', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: '提交失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },

    chooseImage() {
      uni.chooseImage({
        count: 3 - this.images.length,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.images = [...this.images, ...res.tempFilePaths]
        }
      })
    },

    removeImage(index) {
      this.images.splice(index, 1)
    },

    formatPrice(price) {
      if (!price) return '0.00'
      return Number(price).toFixed(2)
    }
  }
}
</script>

<style lang="scss" scoped>
.refund-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

.card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 20rpx;
}

/* 订单信息 */
.order-info {
  display: flex;
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.product-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  margin-right: 24rpx;
  background: #f0f0f0;
}

.product-detail {
  flex: 1;
}

.product-name {
  display: block;
  font-size: 30rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 8rpx;
}

.product-author {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 16rpx;
}

.product-price {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  font-size: 30rpx;
  color: #e74c3c;
  font-weight: 600;
}

.count {
  font-size: 24rpx;
  color: #999;
}

/* 退款类型 */
.type-options {
  display: flex;
  gap: 20rpx;
}

.type-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx 20rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
  text-align: center;

  text:first-of-type {
    font-size: 28rpx;
    color: #333;
    font-weight: 500;
    margin-top: 12rpx;
    margin-bottom: 8rpx;
  }
}

.type-desc {
  font-size: 22rpx;
  color: #999;
}

.type-item.active {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  border-color: #e74c3c;

  text, .type-desc {
    color: #fff;
  }
}

/* 退款金额 */
.amount-display {
  display: flex;
  align-items: baseline;
  justify-content: center;
  margin-bottom: 30rpx;
}

.currency {
  font-size: 32rpx;
  color: #e74c3c;
  font-weight: 600;
}

.amount {
  font-size: 56rpx;
  color: #e74c3c;
  font-weight: 700;
}

.amount-detail {
  border-top: 1rpx solid #f0f0f0;
  padding-top: 20rpx;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
}

.detail-label {
  font-size: 26rpx;
  color: #999;
}

.detail-value {
  font-size: 26rpx;
  color: #333;
}

/* 退款说明 */
.reason-input {
  width: 100%;
  height: 200rpx;
  padding: 20rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #333;
  line-height: 1.6;
}

.char-count {
  text-align: right;
  font-size: 22rpx;
  color: #999;
  margin-top: 12rpx;
}

/* 上传凭证 */
.images-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.image-item {
  position: relative;
  width: 180rpx;
  height: 180rpx;

  image {
    width: 100%;
    height: 100%;
    border-radius: 12rpx;
  }
}

.remove-btn {
  position: absolute;
  top: -16rpx;
  right: -16rpx;
  width: 44rpx;
  height: 44rpx;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    color: #fff;
    font-size: 32rpx;
  }
}

.add-image {
  width: 180rpx;
  height: 180rpx;
  background: #f9f9f9;
  border: 2rpx dashed #ddd;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  text {
    font-size: 22rpx;
    color: #999;
    margin-top: 8rpx;
  }
}

/* 提交按钮 */
.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.1);
}

.submit-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #fff;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 500;
}
</style>
