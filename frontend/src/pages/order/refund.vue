<template>
  <view class="refund-page">
    <view class="hero-card">
      <view class="hero-copy">
        <text class="hero-title">申请退款</text>
        <text class="hero-subtitle">提交后卖家会尽快处理，退款通过后将按实付金额原路退回</text>
      </view>
    </view>

    <!-- 订单信息 -->
    <view class="order-info card">
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
          <text class="type-tag">仅退款</text>
          <text>仅退款</text>
          <text class="type-desc">未收到货或不需要商品</text>
        </view>
        <view
          class="type-item"
          :class="{ active: refundType === 'return' }"
          @click="refundType = 'return'"
        >
          <text class="type-tag">退货</text>
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
          <text class="detail-label">退款方式</text>
          <text class="detail-value">原路退回</text>
        </view>
        <view class="detail-item">
          <text class="detail-label">实付金额</text>
          <text class="detail-value">¥{{ formatPrice(refundAmount) }}</text>
        </view>
        <view class="detail-item">
          <text class="detail-label">商品金额</text>
          <text class="detail-value">¥{{ formatPrice(goodsAmountFen) }}</text>
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
          <text class="add-mark">+</text>
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
import { getOrderDetail, refundApply } from '@/api/order'
import { uploadFile, openCropper } from '@/api/file'
import { fenToYuan, formatYuanNumber } from '@/utils/price'
import { getFullImageUrl } from '@/utils/image'

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
        cover: '',
        goodsAmount: 0,
        payAmount: 0
      },
      refundType: 'refund',
      reason: '',
      images: []
    }
  },

  computed: {
    goodsAmountFen() {
      const goodsAmount = Number(this.order.goodsAmount || 0)
      if (goodsAmount > 0) {
        return goodsAmount
      }
      return Number(this.order.price || 0) * Number(this.order.quantity || 1)
    },
    refundAmount() {
      const payAmount = Number(this.order.payAmount || 0)
      if (payAmount > 0) {
        return payAmount
      }
      const goodsAmount = this.goodsAmountFen
      const freight = Number(this.order.freight || 0)
      const coupon = Number(this.order.couponAmount || 0)
      return goodsAmount + freight - coupon
    }
  },

  onLoad(options) {
    const orderId = options.id || options.orderId
    if (orderId) {
      this.orderId = Number(orderId)
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
      return getOrderDetail(this.orderId)
        .then((detail) => {
          if (!detail) {
            throw new Error('订单不存在')
          }
          const firstItem = (detail.goodsList || detail.items || [])[0] || {}
          const quantity = Number(firstItem.count || firstItem.quantity || firstItem.num || 1)
          const unitPrice = this.normalizeFenAmount(
            firstItem.price ?? firstItem.unitPrice ?? firstItem.unit_price ?? 0
          )
          const goodsAmount = this.normalizeFenAmount(
            detail.goodsAmount ?? detail.goods_amount ?? detail.totalAmount ?? firstItem.subtotal ?? firstItem.subtotalAmount ?? firstItem.subtotal_amount ?? 0,
            unitPrice * quantity
          )
          const freight = this.normalizeFenAmount(detail.freight ?? detail.freightAmount ?? detail.freight_amount ?? 0)
          const couponAmount = this.normalizeFenAmount(detail.discountAmount ?? detail.discount_amount ?? 0)
          const payAmount = this.normalizeFenAmount(
            detail.payAmount ?? detail.pay_amount ?? 0,
            Math.max(goodsAmount + freight - couponAmount, 0)
          )

          this.order = {
            title: firstItem.goodsName || firstItem.title || firstItem.itemTitle || firstItem.item_title || '作品',
            artistName: firstItem.artistName || firstItem.authorName || firstItem.artist_name || detail.sellerName || '',
            price: unitPrice,
            quantity,
            freight,
            couponAmount,
            cover: getFullImageUrl(firstItem.goodsImage || firstItem.coverImage || firstItem.cover || firstItem.cover_url || ''),
            goodsAmount,
            payAmount
          }
        })
        .catch((error) => {
          console.error('加载退款订单信息失败', error)
          uni.showToast({ title: error?.message || '订单加载失败', icon: 'none' })
        })
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
        await refundApply({
          orderId: this.orderId,
          type: this.refundType,
          reason: this.reason,
          images: this.images.join(','),
          amount: Number(fenToYuan(this.refundAmount).toFixed(2))
        })

        uni.showToast({ title: '提交成功', icon: 'success' })
        setTimeout(() => {
          uni.redirectTo({ url: `/pages/order/detail?id=${this.orderId}` })
        }, 1500)
      } catch (e) {
        uni.showToast({ title: e?.message || '提交失败', icon: 'none' })
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
          const paths = res.tempFilePaths
          Promise.all(paths.map(p =>
            openCropper(p, { ratio: 'free', shape: 'square' }).catch(() => p)
          )).then(croppedList => {
            this.images = [...this.images, ...croppedList]
          })
        }
      })
    },

    removeImage(index) {
      this.images.splice(index, 1)
    },

    formatPrice(price) {
      return formatYuanNumber(fenToYuan(price))
    },

    normalizeFenAmount(rawValue, derivedValue = 0) {
      const amountFen = Number(rawValue || 0)
      const derivedFen = Number(derivedValue || 0)
      if (amountFen > 0 && derivedFen > 0 && amountFen > derivedFen * 10) {
        return derivedFen
      }
      return amountFen
    }
  }
}
</script>

<style lang="scss" scoped>
.refund-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(214, 170, 76, 0.12), transparent 28%),
    #0d0d0f;
  color: #f5f0e8;
  padding: 20rpx 20rpx 140rpx;
  box-sizing: border-box;
}

.hero-card,
.card {
  border: 1rpx solid rgba(214, 170, 76, 0.18);
  border-radius: 18rpx;
  background: #171719;
}

.hero-card {
  padding: 26rpx 28rpx;
  margin-bottom: 20rpx;
  background: linear-gradient(135deg, rgba(212, 158, 45, 0.24), rgba(23, 23, 25, 0.95));
}

.hero-title,
.hero-subtitle {
  display: block;
}

.hero-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #f5f0e8;
}

.hero-subtitle {
  margin-top: 10rpx;
  color: #b8b0a5;
  font-size: 23rpx;
  line-height: 1.45;
}

.card {
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 28rpx;
  color: #f5f0e8;
  font-weight: 600;
  margin-bottom: 20rpx;
}

/* 订单信息 */
.order-info {
  display: flex;
}

.product-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  margin-right: 24rpx;
  background: #232326;
}

.product-detail {
  flex: 1;
  min-width: 0;
}

.product-name {
  display: block;
  font-size: 30rpx;
  color: #f5f0e8;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.product-author {
  display: block;
  font-size: 24rpx;
  color: #aaa39a;
  margin-bottom: 16rpx;
}

.product-price {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  font-size: 30rpx;
  color: #f2c65e;
  font-weight: 600;
}

.count {
  font-size: 24rpx;
  color: #8e877d;
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
  align-items: flex-start;
  padding: 30rpx 20rpx;
  background: #202024;
  border-radius: 12rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.06);

  text:first-of-type {
    font-size: 20rpx;
    color: #d2ab53;
    font-weight: 600;
    margin-bottom: 18rpx;
    padding: 6rpx 14rpx;
    border-radius: 999rpx;
    background: rgba(210, 171, 83, 0.12);
  }

  text:nth-of-type(2) {
    font-size: 28rpx;
    color: #f5f0e8;
    font-weight: 500;
    margin-bottom: 8rpx;
  }
}

.type-desc {
  font-size: 22rpx;
  color: #9d958a;
  line-height: 1.45;
}

.type-item.active {
  background: linear-gradient(135deg, rgba(214, 170, 76, 0.2), rgba(32, 32, 36, 1));
  border-color: rgba(214, 170, 76, 0.38);

  text, .type-desc {
    color: #f8f2e7;
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
  color: #f2c65e;
  font-weight: 600;
}

.amount {
  font-size: 56rpx;
  color: #f2c65e;
  font-weight: 700;
}

.amount-detail {
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
  padding-top: 20rpx;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
}

.detail-label {
  font-size: 26rpx;
  color: #8e877d;
}

.detail-value {
  font-size: 26rpx;
  color: #f5f0e8;
}

/* 退款说明 */
.reason-input {
  width: 100%;
  height: 200rpx;
  padding: 20rpx;
  box-sizing: border-box;
  background: #202024;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #f5f0e8;
  line-height: 1.6;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
}

.char-count {
  text-align: right;
  font-size: 22rpx;
  color: #8e877d;
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
    background: #202024;
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
  background: #202024;
  border: 2rpx dashed rgba(214, 170, 76, 0.26);
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  text {
    font-size: 22rpx;
    color: #a9a39a;
    margin-top: 8rpx;
  }
}

.add-mark {
  font-size: 42rpx !important;
  color: #f2c65e !important;
  margin-top: 0 !important;
}

/* 提交按钮 */
.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(13, 13, 15, 0.96);
  box-shadow: 0 -12rpx 36rpx rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(12rpx);
}

.submit-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #d6aa4c, #f2c65e);
  color: #1a1610;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 600;
}

.submit-btn::after {
  border: none;
}
</style>
