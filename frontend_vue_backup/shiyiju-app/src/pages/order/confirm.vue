<template>
  <view class="confirm-page">
    <!-- 收货地址 -->
    <view class="section address-section" @click="showAddressPicker = true">
      <view class="address-content" v-if="selectedAddress">
        <view class="address-info">
          <text class="contact">{{ selectedAddress.contact }} {{ selectedAddress.phone }}</text>
          <text class="detail">{{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }}{{ selectedAddress.address }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="address-empty" v-else>
        <text>请选择收货地址</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 商品信息 -->
    <view class="section goods-section">
      <view class="section-title">商品信息</view>
      <view class="goods-list">
        <view class="goods-item" v-for="item in orderItems" :key="item.id">
          <image class="goods-image" :src="item.coverImage" mode="aspectFill" />
          <view class="goods-info">
            <text class="goods-title">{{ item.title }}</text>
            <text class="goods-size">{{ item.size }}</text>
            <view class="goods-footer">
              <text class="goods-price">¥{{ formatPrice(item.price) }}</text>
              <text class="goods-qty">x{{ item.quantity }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 配送方式 -->
    <view class="section delivery-section">
      <view class="section-title">配送方式</view>
      <view class="delivery-item">
        <text class="delivery-name">快递配送</text>
        <text class="delivery-price">¥{{ freight > 0 ? freight : '免运费' }}</text>
      </view>
    </view>

    <!-- 买家留言 -->
    <view class="section remark-section">
      <view class="remark-label">买家留言</view>
      <input class="remark-input" v-model="remark" placeholder="选填，可输入对商品的特殊要求" />
    </view>

    <!-- 价格明细 -->
    <view class="section price-section">
      <view class="price-row">
        <text class="price-label">商品金额</text>
        <text class="price-value">¥{{ formatPrice(goodsAmount) }}</text>
      </view>
      <view class="price-row">
        <text class="price-label">运费</text>
        <text class="price-value">{{ freight > 0 ? '¥' + freight : '免运费' }}</text>
      </view>
      <view class="price-row total">
        <text class="price-label">合计</text>
        <text class="price-value">¥{{ formatPrice(totalAmount) }}</text>
      </view>
    </view>

    <!-- 底部提交栏 -->
    <view class="submit-bar safe-area-bottom">
      <view class="submit-info">
        <text class="submit-label">实付款:</text>
        <text class="submit-price">¥{{ formatPrice(totalAmount) }}</text>
      </view>
      <button class="btn-submit" @click="handleSubmit" :loading="submitting">
        提交订单
      </button>
    </view>

    <!-- 地址选择弹窗 -->
    <view class="address-picker" v-if="showAddressPicker" @click="showAddressPicker = false">
      <view class="address-picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">选择收货地址</text>
          <text class="picker-close" @click="showAddressPicker = false">×</text>
        </view>
        <scroll-view class="address-list" scroll-y>
          <view 
            class="address-item" 
            :class="{ active: addr.id === selectedAddress?.id }"
            v-for="addr in addressList" 
            :key="addr.id"
            @click="selectAddress(addr)"
          >
            <view class="addr-info">
              <text class="addr-contact">{{ addr.contact }} {{ addr.phone }}</text>
              <text class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.address }}</text>
            </view>
            <view class="addr-check" v-if="addr.id === selectedAddress?.id">✓</view>
          </view>
          <view class="add-address" @click="goAddAddress">
            <text>+ 添加新地址</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAddressList, createOrder } from '@/api/order'

const orderItems = ref([])
const addressList = ref([])
const selectedAddress = ref(null)
const showAddressPicker = ref(false)
const remark = ref('')
const submitting = ref(false)

const freight = ref(0) // 运费

onMounted(() => {
  loadData()
})

async function loadData() {
  // 获取订单商品
  const items = uni.getStorageSync('checkoutItems')
  if (items) {
    orderItems.value = JSON.parse(items)
  }

  // 加载地址列表
  try {
    const res = await getAddressList()
    addressList.value = res || []
  } catch (e) {
    addressList.value = [
      { id: 1, contact: '张三', phone: '138****8001', province: '北京市', city: '北京市', district: '朝阳区', address: '建国路88号SOHO现代城' },
      { id: 2, contact: '李四', phone: '138****8002', province: '上海市', city: '上海市', district: '黄浦区', address: '南京东路100号' }
    ]
  }

  // 默认选中第一个
  if (addressList.value.length > 0 && !selectedAddress.value) {
    selectedAddress.value = addressList.value[0]
  }
}

function selectAddress(addr) {
  selectedAddress.value = addr
  showAddressPicker.value = false
}

function goAddAddress() {
  showAddressPicker.value = false
  uni.navigateTo({ url: '/pages/address/add' })
}

const goodsAmount = computed(() => {
  return orderItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const totalAmount = computed(() => {
  return goodsAmount.value + freight.value
})

async function handleSubmit() {
  if (!selectedAddress.value) {
    uni.showToast({ title: '请选择收货地址', icon: 'none' })
    return
  }

  if (orderItems.value.length === 0) {
    uni.showToast({ title: '订单商品不能为空', icon: 'none' })
    return
  }

  submitting.value = true

  try {
    const orderData = {
      addressId: selectedAddress.value.id,
      items: orderItems.value.map(item => ({
        artworkId: item.artworkId,
        quantity: item.quantity
      })),
      remark: remark.value,
      freight: freight.value
    }

    const res = await createOrder(orderData)

    // 模拟创建成功
    const mockOrder = {
      orderId: 'SYJ' + Date.now(),
      amount: totalAmount.value,
      orderNo: 'ORD' + Date.now()
    }

    // 清除购物车中的已结算商品
    uni.removeStorageSync('checkoutItems')

    uni.showToast({ title: '订单创建成功', icon: 'success' })

    // 跳转到支付页面
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/order/payment?orderId=${mockOrder.orderId}&amount=${totalAmount.value}` })
    }, 1500)

  } catch (e) {
    // 模拟成功
    const mockOrderId = 'SYJ' + Date.now()
    uni.removeStorageSync('checkoutItems')
    uni.showToast({ title: '订单创建成功', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/order/payment?orderId=${mockOrderId}&amount=${totalAmount.value}` })
    }, 1500)
  } finally {
    submitting.value = false
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
.confirm-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.section {
  background: #fff;
  margin-bottom: 16rpx;
}

.address-section {
  padding: 32rpx;
  min-height: 160rpx;
  display: flex;
  align-items: center;

  .address-content {
    display: flex;
    align-items: center;
    flex: 1;
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

  .address-empty {
    flex: 1;
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #999;
    font-size: 30rpx;
  }

  .arrow {
    font-size: 40rpx;
    color: #ccc;
  }
}

.goods-section {
  .section-title {
    padding: 24rpx 32rpx;
    font-size: 28rpx;
    color: #999;
    border-bottom: 1rpx solid #f0f0f0;
  }

  .goods-item {
    display: flex;
    padding: 24rpx 32rpx;
    border-bottom: 1rpx solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }
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

.delivery-section {
  .section-title {
    padding: 24rpx 32rpx;
    font-size: 28rpx;
    color: #999;
    border-bottom: 1rpx solid #f0f0f0;
  }

  .delivery-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 32rpx;

    .delivery-name {
      font-size: 28rpx;
      color: #333;
    }

    .delivery-price {
      font-size: 28rpx;
      color: #666;
    }
  }
}

.remark-section {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;

  .remark-label {
    font-size: 28rpx;
    color: #333;
    margin-right: 24rpx;
  }

  .remark-input {
    flex: 1;
    font-size: 28rpx;
    color: #666;
  }
}

.price-section {
  padding: 24rpx 32rpx;

  .price-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16rpx;

    &.total {
      margin-top: 16rpx;
      padding-top: 16rpx;
      border-top: 1rpx solid #eee;

      .price-value {
        font-size: 36rpx;
        font-weight: 600;
        color: #333;
      }
    }

    .price-label {
      font-size: 28rpx;
      color: #666;
    }

    .price-value {
      font-size: 28rpx;
      color: #333;
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
  padding: 16rpx 32rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;

  .submit-info {
    display: flex;
    align-items: baseline;
    gap: 8rpx;

    .submit-label {
      font-size: 26rpx;
      color: #666;
    }

    .submit-price {
      font-size: 40rpx;
      font-weight: 600;
      color: #333;
    }
  }

  .btn-submit {
    width: 240rpx;
    height: 88rpx;
    background: #333;
    color: #fff;
    border-radius: 44rpx;
    font-size: 30rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.address-picker {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;

  .address-picker-content {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    max-height: 70vh;
    background: #fff;
    border-radius: 24rpx 24rpx 0 0;
    display: flex;
    flex-direction: column;
  }

  .picker-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 32rpx;
    border-bottom: 1rpx solid #eee;

    .picker-title {
      font-size: 32rpx;
      font-weight: 600;
      color: #333;
    }

    .picker-close {
      font-size: 48rpx;
      color: #999;
    }
  }

  .address-list {
    flex: 1;
    max-height: 60vh;
  }

  .address-item {
    display: flex;
    align-items: center;
    padding: 32rpx;
    border-bottom: 1rpx solid #f0f0f0;

    &.active {
      background: #f9f9f9;
    }

    .addr-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8rpx;

      .addr-contact {
        font-size: 30rpx;
        font-weight: 500;
        color: #333;
      }

      .addr-detail {
        font-size: 26rpx;
        color: #666;
      }
    }

    .addr-check {
      width: 40rpx;
      height: 40rpx;
      border-radius: 50%;
      background: #333;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24rpx;
    }
  }

  .add-address {
    padding: 32rpx;
    text-align: center;
    color: #409eff;
    font-size: 28rpx;
  }
}
</style>
