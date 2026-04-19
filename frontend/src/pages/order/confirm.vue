<template>
  <view class="confirm-page">
    <!-- 商品信息 -->
    <view class="goods-section card" v-if="goodsType === 'direct'">
      <view class="goods-item" v-for="item in goodsList" :key="item.id">
        <image class="goods-image" :src="item.coverImage"></image>
        <view class="goods-info">
          <text class="goods-title">{{ item.title }}</text>
          <text class="goods-meta">{{ item.artType }} · {{ item.size }}</text>
          <view class="goods-price-row">
            <text class="goods-price">¥{{ formatPrice(item.price) }}</text>
            <text class="goods-qty">x{{ item.quantity }}</text>
          </view>
        </view>
      </view>
    </view>
    
    <view class="goods-section card" v-else>
      <view class="goods-item" v-for="item in goodsList" :key="item.id">
        <image class="goods-image" :src="item.coverImage"></image>
        <view class="goods-info">
          <text class="goods-title">{{ item.title }}</text>
          <text class="goods-meta">{{ item.artType }}</text>
          <view class="goods-price-row">
            <text class="goods-price">¥{{ formatPrice(item.price) }}</text>
            <text class="goods-qty">x{{ item.quantity }}</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 收货地址 -->
    <view class="address-section card" @click="goAddress">
      <view class="address-empty" v-if="!selectedAddress">
        <text class="iconfont icon-location"></text>
        <text>添加收货地址</text>
        <text class="iconfont icon-arrow-right"></text>
      </view>
      <view class="address-info" v-else>
        <view class="address-detail">
          <text class="receiver">{{ selectedAddress.receiverName }}</text>
          <text class="phone">{{ selectedAddress.phone }}</text>
        </view>
        <text class="address-text">{{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }}{{ selectedAddress.detailAddress }}</text>
      </view>
      <text class="iconfont icon-arrow-right arrow"></text>
    </view>
    
    <!-- 佣金说明 -->
    <view class="commission-section card" v-if="hasPromoter">
      <view class="commission-header">
        <text class="iconfont icon-medal"></text>
        <text>分享赚佣金</text>
      </view>
      <view class="commission-info">
        <text class="commission-text">通过艺荐官链接购买，艺荐官可获得 ¥{{ formatPrice(commissionAmount) }} 佣金</text>
      </view>
    </view>
    
    <!-- 价格明细 -->
    <view class="price-section card">
      <view class="price-row">
        <text class="price-label">商品金额</text>
        <text class="price-value">¥{{ formatPrice(totalAmount) }}</text>
      </view>
      <view class="price-row">
        <text class="price-label">运费</text>
        <text class="price-value">包邮</text>
      </view>
      <view class="price-row total">
        <text class="price-label">合计</text>
        <text class="price-value">¥{{ formatPrice(totalAmount) }}</text>
      </view>
    </view>
    
    <!-- 备注 -->
    <view class="remark-section card">
      <view class="remark-header">
        <text>订单备注</text>
      </view>
      <textarea 
        class="remark-input" 
        v-model="remark" 
        placeholder="选填，可备注您的特殊需求" 
        maxlength="200"
      />
    </view>
    
    <!-- 底部提交栏 -->
    <view class="submit-bar safe-area-bottom">
      <view class="submit-info">
        <text class="submit-label">实付款：</text>
        <text class="submit-price">¥{{ formatPrice(totalAmount) }}</text>
      </view>
      <button class="btn-submit" @click="onSubmit" :loading="submitting">
        提交订单
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getProductDetail } from '@/api/product'
import { getCartList, getAddressList, createOrderFromCart, directBuy } from '@/api/order'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

// 状态
const goodsType = ref('direct') // direct-直接购买, cart-购物车
const goodsList = ref([])
const selectedAddress = ref(null)
const remark = ref('')
const submitting = ref(false)
const cartIds = ref([])
const artworkId = ref(null)

// 价格相关
const totalAmount = computed(() => {
  return goodsList.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

// 佣金(简化计算,实际从后端获取)
const commissionAmount = computed(() => {
  return totalAmount.value * 0.05
})

// 是否有艺荐官
const hasPromoter = computed(() => {
  return userStore.isPromoter
})

// 初始化
onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}
  
  if (options.artworkId) {
    // 直接购买
    goodsType.value = 'direct'
    artworkId.value = options.artworkId
    fetchGoodsDetail(options.artworkId, parseInt(options.quantity) || 1)
  } else if (options.cartIds) {
    // 购物车购买
    goodsType.value = 'cart'
    cartIds.value = options.cartIds.split(',').map(Number)
    fetchCartGoods()
  }
  
  // 获取收货地址
  fetchAddress()
})

// 获取商品详情
const fetchGoodsDetail = async (id, quantity) => {
  try {
    const data = await getProductDetail(id)
    goodsList.value = [{
      id: data.id,
      title: data.title,
      coverImage: data.coverImage,
      price: data.price,
      quantity: quantity,
      artType: data.artType,
      size: data.size
    }]
  } catch (e) {
    console.error('获取商品详情失败', e)
  }
}

// 获取购物车商品
const fetchCartGoods = async () => {
  try {
    const list = await getCartList()
    const selectedItems = list.filter(item => cartIds.value.includes(item.id))
    goodsList.value = selectedItems.map(item => ({
      id: item.artworkId,
      title: item.title,
      coverImage: item.coverImage,
      price: item.price,
      quantity: item.quantity,
      artType: item.artType,
      size: item.size
    }))
  } catch (e) {
    console.error('获取购物车商品失败', e)
  }
}

// 获取收货地址
const fetchAddress = async () => {
  try {
    const list = await getAddressList()
    // 获取默认地址或第一个地址
    selectedAddress.value = list.find(addr => addr.isDefault) || list[0] || null
  } catch (e) {
    selectedAddress.value = null
  }
}

// 跳转地址选择
const goAddress = () => {
  uni.navigateTo({
    url: '/pages/user/address?select=true',
    events: {
      selectAddress: (address) => {
        selectedAddress.value = address
      }
    }
  })
}

// 提交订单
const onSubmit = async () => {
  if (!selectedAddress.value) {
    uni.showToast({ title: '请选择收货地址', icon: 'none' })
    return
  }
  
  if (submitting.value) return
  submitting.value = true
  
  try {
    let order
    const params = {
      addressId: selectedAddress.value.id,
      remark: remark.value
    }
    
    if (goodsType.value === 'direct') {
      // 直接购买
      order = await directBuy({
        productId: artworkId.value,
        quantity: goodsList.value[0].quantity,
        ...params
      })
    } else {
      // 购物车购买
      order = await createOrderFromCart({
        cartIds: cartIds.value,
        ...params
      })
    }
    
    // 跳转支付
    uni.navigateTo({
      url: `/pages/order/pay?orderId=${order.id}&amount=${order.payAmount}`
    })
  } catch (e) {
    uni.showToast({ title: e.message || '创建订单失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

// 格式化价格
const formatPrice = (price) => {
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
  padding-bottom: 160rpx;
  background-color: #f8f8f8;
}

.card {
  margin: 20rpx 30rpx;
  padding: 24rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
}

.goods-section {
  .goods-item {
    display: flex;
    
    &:not(:last-child) {
      margin-bottom: 20rpx;
      padding-bottom: 20rpx;
      border-bottom: 1rpx solid #f5f5f5;
    }
    
    .goods-image {
      width: 160rpx;
      height: 160rpx;
      border-radius: 8rpx;
      background-color: #f5f5f5;
      margin-right: 20rpx;
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
        margin-bottom: auto;
      }
      
      .goods-price-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .goods-price {
          font-size: 30rpx;
          color: #333333;
          font-weight: 600;
        }
        
        .goods-qty {
          font-size: 26rpx;
          color: #999999;
        }
      }
    }
  }
}

.address-section {
  display: flex;
  align-items: center;
  min-height: 120rpx;
  
  .address-empty {
    display: flex;
    align-items: center;
    flex: 1;
    
    .iconfont {
      font-size: 40rpx;
      color: #999999;
      margin-right: 16rpx;
    }
    
    text {
      font-size: 28rpx;
      color: #333333;
    }
    
    .icon-arrow-right {
      margin-left: auto;
      color: #cccccc;
    }
  }
  
  .address-info {
    flex: 1;
    
    .address-detail {
      display: flex;
      align-items: center;
      margin-bottom: 12rpx;
      
      .receiver {
        font-size: 30rpx;
        font-weight: 600;
        color: #333333;
        margin-right: 20rpx;
      }
      
      .phone {
        font-size: 28rpx;
        color: #666666;
      }
    }
    
    .address-text {
      font-size: 26rpx;
      color: #666666;
      line-height: 1.4;
    }
  }
  
  .arrow {
    font-size: 28rpx;
    color: #cccccc;
    margin-left: 20rpx;
  }
}

.commission-section {
  .commission-header {
    display: flex;
    align-items: center;
    margin-bottom: 12rpx;
    
    .iconfont {
      font-size: 32rpx;
      color: #e6be8a;
      margin-right: 12rpx;
    }
    
    text {
      font-size: 28rpx;
      color: #333333;
      font-weight: 500;
    }
  }
  
  .commission-info {
    .commission-text {
      font-size: 24rpx;
      color: #999999;
    }
  }
}

.price-section {
  .price-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16rpx 0;
    
    .price-label {
      font-size: 28rpx;
      color: #666666;
    }
    
    .price-value {
      font-size: 28rpx;
      color: #333333;
    }
    
    &.total {
      border-top: 1rpx solid #f5f5f5;
      margin-top: 8rpx;
      padding-top: 24rpx;
      
      .price-label {
        font-size: 30rpx;
        font-weight: 600;
        color: #333333;
      }
      
      .price-value {
        font-size: 36rpx;
        font-weight: 600;
        color: #333333;
      }
    }
  }
}

.remark-section {
  .remark-header {
    margin-bottom: 16rpx;
    
    text {
      font-size: 28rpx;
      color: #333333;
    }
  }
  
  .remark-input {
    width: 100%;
    min-height: 160rpx;
    padding: 20rpx;
    background-color: #f8f8f8;
    border-radius: 12rpx;
    font-size: 28rpx;
    color: #333333;
    box-sizing: border-box;
  }
}

.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background-color: #ffffff;
  border-top: 1rpx solid #f0f0f0;
  
  .submit-info {
    display: flex;
    align-items: baseline;
    
    .submit-label {
      font-size: 26rpx;
      color: #666666;
    }
    
    .submit-price {
      font-size: 40rpx;
      font-weight: 600;
      color: #333333;
    }
  }
  
  .btn-submit {
    width: 240rpx;
    height: 88rpx;
    background-color: #333333;
    color: #ffffff;
    font-size: 32rpx;
    border-radius: 44rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
