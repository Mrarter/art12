<template>
  <view class="settle-page">
    <!-- 商品列表 -->
    <view class="product-list card">
      <view class="list-header">
        <text>🛍</text>
        <text>待结算商品</text>
        <text class="count">({{ productList.length }})</text>
      </view>

      <view class="product-item" v-for="item in productList" :key="item.id">
        <image class="product-img" :src="item.cover" mode="aspectFill"></image>
        <view class="product-info">
          <view class="product-title">{{ item.title }}</view>
          <view class="product-artist">{{ item.artist }}</view>
          <view class="product-bottom">
            <text class="product-price">¥{{ item.price }}</text>
            <text class="product-num">x{{ item.num }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 收货地址 -->
    <view class="address-section card" @click="selectAddress">
      <view class="section-icon">
        
      </view>
      <view class="address-content" v-if="selectedAddress">
        <view class="address-top">
          <text class="receiver">{{ selectedAddress.name }}</text>
          <text class="phone">{{ selectedAddress.phone }}</text>
        </view>
        <view class="address-detail">{{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }}{{ selectedAddress.detail }}</view>
      </view>
      <view class="address-empty" v-else>
        <text>请选择收货地址</text>
      </view>
      
    </view>

    <!-- 配送方式 -->
    <view class="delivery-section card">
      <view class="section-title">配送方式</view>
      <view class="delivery-options">
        <view
          class="delivery-item"
          v-for="item in deliveryOptions"
          :key="item.id"
          :class="{ active: item.id === selectedDelivery }"
          @click="selectDelivery(item.id)"
        >
          
          <text>{{ item.name }}</text>
          <text class="delivery-fee" v-if="item.fee > 0">+¥{{ item.fee }}</text>
        </view>
      </view>
    </view>

    <!-- 优惠信息 -->
    <view class="discount-section card">
      <view class="discount-item" @click="useCoupon">
        <view class="discount-left">
          
          <text>优惠券</text>
        </view>
        <view class="discount-right">
          <text :class="{ 'coupon-active': selectedCoupon }">{{ couponText }}</text>
          
        </view>
      </view>
      <view class="discount-item" @click="usePoints">
        <view class="discount-left">
          <text>★</text>
          <text>积分抵扣</text>
        </view>
        <view class="discount-right">
          <text class="points-info">可用{{ availablePoints }}积分抵¥{{ pointsDiscount }}</text>
          <switch :checked="usePointsEnabled" @change="togglePoints" color="#667eea"></switch>
        </view>
      </view>
    </view>

    <!-- 艺荐官佣金 -->
    <view class="commission-section card" v-if="commission > 0">
      <view class="section-header">
        <text>↗</text>
        <text>分享赚佣金</text>
      </view>
      <view class="commission-info">
        <text class="commission-tip">分享此订单可获得佣金</text>
        <text class="commission-amount">+¥{{ commission }}</text>
      </view>
    </view>

    <!-- 订单备注 -->
    <view class="remark-section card">
      <view class="remark-label">订单备注</view>
      <input
        class="remark-input"
        v-model="remark"
        placeholder="选填，可备注特殊要求"
        maxlength="100"
      />
    </view>

    <!-- 价格明细 -->
    <view class="price-detail card">
      <view class="detail-row">
        <text class="detail-label">商品总价</text>
        <text class="detail-value">¥{{ productTotal }}</text>
      </view>
      <view class="detail-row">
        <text class="detail-label">运费</text>
        <text class="detail-value">+¥{{ deliveryFee }}</text>
      </view>
      <view class="detail-row" v-if="couponDiscount > 0">
        <text class="detail-label">优惠券</text>
        <text class="detail-value discount">-¥{{ couponDiscount }}</text>
      </view>
      <view class="detail-row" v-if="pointsDiscount > 0">
        <text class="detail-label">积分抵扣</text>
        <text class="detail-value discount">-¥{{ pointsDiscount }}</text>
      </view>
      <view class="detail-row total">
        <text class="detail-label">合计</text>
        <text class="detail-value">¥{{ finalTotal }}</text>
      </view>
    </view>

    <!-- 底部提交栏 -->
    <view class="submit-bar">
      <view class="total-info">
        <text class="total-label">实付款：</text>
        <text class="total-price">¥{{ finalTotal }}</text>
      </view>
      <view class="submit-btn" @click="submitOrder">提交订单</view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      productList: [
        {
          id: 1,
          cover: 'https://picsum.photos/200',
          title: '山水之间',
          artist: '李明',
          price: '8888.00',
          num: 1
        }
      ],
      selectedAddress: {
        id: 1,
        name: '张三',
        phone: '138****8888',
        province: '浙江省',
        city: '杭州市',
        district: '西湖区',
        detail: '文三路123号智慧大厦15层1501室'
      },
      deliveryOptions: [
        { id: 'express', name: '快递配送', icon: 'car', fee: 0 },
        { id: 'self', name: '上门自提', icon: 'home', fee: 0 }
      ],
      selectedDelivery: 'express',
      deliveryFee: 0,
      availablePoints: 5000,
      usePointsEnabled: false,
      selectedCoupon: null,
      remark: '',
      commission: 0
    }
  },

  computed: {
    productTotal() {
      return this.productList.reduce((sum, item) => sum + parseFloat(item.price) * item.num, 0).toFixed(2)
    },
    couponDiscount() {
      return this.selectedCoupon ? this.selectedCoupon.discount : 0
    },
    pointsDiscount() {
      return this.usePointsEnabled ? Math.min(this.availablePoints / 100, this.productTotal * 0.1) : 0
    },
    finalTotal() {
      return (parseFloat(this.productTotal) + this.deliveryFee - this.couponDiscount - this.pointsDiscount).toFixed(2)
    },
    couponText() {
      return this.selectedCoupon ? `-¥${this.selectedCoupon.discount}` : '暂无可用'
    }
  },

  onLoad(options) {
    if (options.products) {
      try {
        this.productList = JSON.parse(decodeURIComponent(options.products))
      } catch (e) {
        console.error('解析商品数据失败', e)
      }
    }
    this.calculateCommission()
  },

  methods: {
    selectAddress() {
      uni.navigateTo({
        url: '/pages/user/address?mode=select'
      })
    },

    selectDelivery(id) {
      this.selectedDelivery = id
    },

    useCoupon() {
      uni.showToast({ title: '暂无可用优惠券', icon: 'none' })
    },

    usePoints() {
      this.usePointsEnabled = !this.usePointsEnabled
    },

    togglePoints(e) {
      this.usePointsEnabled = e.detail.value
    },

    calculateCommission() {
      // 根据商品计算可能获得的佣金
      this.commission = (this.productTotal * 0.05).toFixed(2)
    },

    submitOrder() {
      if (!this.selectedAddress) {
        uni.showToast({ title: '请选择收货地址', icon: 'none' })
        return
      }

      uni.showModal({
        title: '确认提交',
        content: `确认提交订单，实付款 ¥${this.finalTotal}？`,
        success: (res) => {
          if (res.confirm) {
            uni.showLoading({ title: '提交中...' })
            setTimeout(() => {
              uni.hideLoading()
              uni.showToast({ title: '订单提交成功', icon: 'success' })
              setTimeout(() => {
                uni.redirectTo({
                  url: `/pages/order/pay?orderId=${Date.now()}`
                })
              }, 1000)
            }, 1000)
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.settle-page {
  min-height: 100vh;
  background: #f5f6f7;
  padding: 20rpx;
  padding-bottom: 140rpx;
}

.product-list {
  padding: 30rpx;

  .list-header {
    display: flex;
    align-items: center;
    margin-bottom: 24rpx;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f0f0f0;

    text {
      margin-left: 12rpx;
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }

    .count {
      font-weight: normal;
      color: #999;
    }
  }

  .product-item {
    display: flex;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .product-img {
      width: 160rpx;
      height: 160rpx;
      border-radius: 12rpx;
      flex-shrink: 0;
    }

    .product-info {
      flex: 1;
      margin-left: 24rpx;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      .product-title {
        font-size: 28rpx;
        color: #333;
        font-weight: 500;
      }

      .product-artist {
        font-size: 24rpx;
        color: #999;
        margin-top: 8rpx;
      }

      .product-bottom {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 12rpx;

        .product-price {
          font-size: 30rpx;
          color: #333;
          font-weight: 600;
        }

        .product-num {
          font-size: 26rpx;
          color: #999;
        }
      }
    }
  }
}

.address-section {
  display: flex;
  align-items: center;
  padding: 30rpx;

  .section-icon {
    margin-right: 20rpx;
  }

  .address-content {
    flex: 1;

    .address-top {
      display: flex;
      align-items: center;
      margin-bottom: 12rpx;

      .receiver {
        font-size: 32rpx;
        font-weight: 600;
        color: #333;
        margin-right: 20rpx;
      }

      .phone {
        font-size: 28rpx;
        color: #666;
      }
    }

    .address-detail {
      font-size: 26rpx;
      color: #666;
      line-height: 1.5;
    }
  }

  .address-empty {
    flex: 1;
    font-size: 28rpx;
    color: #999;
  }
}

.delivery-section {
  padding: 30rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 24rpx;
  }

  .delivery-options {
    display: flex;
    gap: 20rpx;

    .delivery-item {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 30rpx;
      background: #f5f5f5;
      border-radius: 12rpx;
      border: 2rpx solid transparent;

      text {
        margin-top: 12rpx;
        font-size: 28rpx;
        color: #666;
      }

      .delivery-fee {
        font-size: 24rpx;
        color: #999;
      }

      &.active {
        background: rgba(102, 126, 234, 0.1);
        border-color: #667eea;

        text {
          color: #667eea;
        }
      }
    }
  }
}

.discount-section {
  .discount-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 28rpx 30rpx;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .discount-left {
      display: flex;
      align-items: center;

      text {
        margin-left: 12rpx;
        font-size: 28rpx;
        color: #333;
      }
    }

    .discount-right {
      display: flex;
      align-items: center;

      text {
        font-size: 26rpx;
        color: #999;
        margin-right: 10rpx;
      }

      .coupon-active {
        color: #ff4d4f;
      }

      .points-info {
        font-size: 24rpx;
      }
    }
  }
}

.commission-section {
  padding: 30rpx;

  .section-header {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    text {
      margin-left: 12rpx;
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }
  }

  .commission-info {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20rpx;
    background: rgba(102, 126, 234, 0.1);
    border-radius: 12rpx;

    .commission-tip {
      font-size: 26rpx;
      color: #666;
    }

    .commission-amount {
      font-size: 32rpx;
      font-weight: 600;
      color: #667eea;
    }
  }
}

.remark-section {
  display: flex;
  align-items: center;
  padding: 30rpx;

  .remark-label {
    font-size: 28rpx;
    color: #333;
    margin-right: 20rpx;
    flex-shrink: 0;
  }

  .remark-input {
    flex: 1;
    font-size: 28rpx;
    color: #333;
  }
}

.price-detail {
  padding: 30rpx;

  .detail-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16rpx 0;

    .detail-label {
      font-size: 28rpx;
      color: #666;
    }

    .detail-value {
      font-size: 28rpx;
      color: #333;

      &.discount {
        color: #ff4d4f;
      }
    }

    &.total {
      margin-top: 16rpx;
      padding-top: 20rpx;
      border-top: 1rpx solid #f0f0f0;

      .detail-label {
        font-size: 30rpx;
        font-weight: 600;
        color: #333;
      }

      .detail-value {
        font-size: 36rpx;
        font-weight: 600;
        color: #ff4d4f;
      }
    }
  }
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.1);

  .total-info {
    .total-label {
      font-size: 28rpx;
      color: #666;
    }

    .total-price {
      font-size: 40rpx;
      font-weight: 600;
      color: #ff4d4f;
    }
  }

  .submit-btn {
    padding: 24rpx 60rpx;
    font-size: 32rpx;
    color: #fff;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 44rpx;
  }
}
</style>
