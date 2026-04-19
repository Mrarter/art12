<template>
  <view class="cart-page">
    <!-- 购物车为空 -->
    <view class="empty-cart" v-if="cartList.length === 0 && !isLoading">
      <text class="empty-icon">🛒</text>
      <text class="empty-text">购物车是空的</text>
      <button class="btn-goshop" @click="goGallery">去逛逛</button>
    </view>

    <!-- 购物车列表 -->
    <view class="cart-list" v-else>
      <view 
        v-for="item in cartList" 
        :key="item.id"
        class="cart-item"
      >
        <view class="item-checkbox" @click="toggleSelect(item)">
          <text>{{ item.selected ? '✓' : '' }}</text>
        </view>
        <image class="item-image" :src="item.coverImage" mode="aspectFill" />
        <view class="item-info">
          <view class="item-title">{{ item.title }}</view>
          <view class="item-size">{{ item.size }}</view>
          <view class="item-footer">
            <text class="item-price">¥{{ formatPrice(item.price) }}</text>
            <view class="quantity-control">
              <text class="qty-btn" @click="decreaseQty(item)">-</text>
              <text class="qty-value">{{ item.quantity }}</text>
              <text class="qty-btn" @click="increaseQty(item)">+</text>
            </view>
          </view>
        </view>
        <view class="item-delete" @click="removeItem(item)">
          <text>×</text>
        </view>
      </view>
    </view>

    <!-- 底部结算栏 -->
    <view class="checkout-bar safe-area-bottom" v-if="cartList.length > 0">
      <view class="select-all" @click="toggleSelectAll">
        <view class="checkbox" :class="{ checked: isSelectAll }">
          <text v-if="isSelectAll">✓</text>
        </view>
        <text>全选</text>
      </view>
      <view class="total-info">
        <text class="total-label">合计:</text>
        <text class="total-price">¥{{ formatPrice(totalPrice) }}</text>
      </view>
      <button class="btn-checkout" @click="goCheckout" :disabled="selectedCount === 0">
        结算({{ selectedCount }})
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getCartList, removeFromCart as removeCart } from '@/api/order'

const cartList = ref([])
const isLoading = ref(false)

onMounted(() => {
  loadCart()
})

async function loadCart() {
  isLoading.value = true
  try {
    const res = await getCartList()
    cartList.value = res || []
  } catch (e) {
    // 使用模拟数据
    cartList.value = generateMockData()
  }
  isLoading.value = false
}

function generateMockData() {
  return [
    {
      id: 1,
      artworkId: 1,
      title: '《父亲》罗中立 布面油画',
      coverImage: 'https://picsum.photos/200/200?random=1',
      size: '50*70cm',
      price: 2800000,
      quantity: 1,
      selected: false
    },
    {
      id: 2,
      artworkId: 2,
      title: '《奔马图》张大千 水墨画',
      coverImage: 'https://picsum.photos/200/200?random=2',
      size: '68*136cm',
      price: 1580000,
      quantity: 1,
      selected: false
    }
  ]
}

const isSelectAll = computed(() => {
  return cartList.value.length > 0 && cartList.value.every(item => item.selected)
})

const selectedCount = computed(() => {
  return cartList.value.filter(item => item.selected).length
})

const totalPrice = computed(() => {
  return cartList.value
    .filter(item => item.selected)
    .reduce((sum, item) => sum + item.price * item.quantity, 0)
})

function toggleSelect(item) {
  item.selected = !item.selected
}

function toggleSelectAll() {
  const newValue = !isSelectAll.value
  cartList.value.forEach(item => item.selected = newValue)
}

function increaseQty(item) {
  item.quantity++
}

function decreaseQty(item) {
  if (item.quantity > 1) {
    item.quantity--
  }
}

async function removeItem(item) {
  uni.showModal({
    title: '提示',
    content: '确定要删除该商品吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await removeCart([item.id])
          cartList.value = cartList.value.filter(i => i.id !== item.id)
        } catch (e) {
          cartList.value = cartList.value.filter(i => i.id !== item.id)
        }
      }
    }
  })
}

function goGallery() {
  uni.switchTab({ url: '/pages/gallery/index' })
}

function goCheckout() {
  const selectedItems = cartList.value.filter(item => item.selected)
  if (selectedItems.length === 0) return
  
  // 将选中项存入本地存储
  uni.setStorageSync('checkoutItems', JSON.stringify(selectedItems))
  uni.navigateTo({ url: '/pages/order/confirm' })
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
.cart-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
  
  .empty-icon {
    font-size: 120rpx;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: #999;
    margin: 32rpx 0;
  }
  
  .btn-goshop {
    width: 240rpx;
    height: 72rpx;
    background: #333;
    color: #fff;
    border-radius: 36rpx;
    font-size: 28rpx;
  }
}

.cart-list {
  padding: 16rpx;
}

.cart-item {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  position: relative;
  
  .item-checkbox {
    width: 40rpx;
    height: 40rpx;
    border: 2rpx solid #ddd;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16rpx;
    color: #fff;
    background: #fff;
    
    &[data-checked="true"], text {
      background: #333;
      border-color: #333;
    }
  }
  
  .item-checkbox:has(text) {
    background: #333;
    border-color: #333;
  }
  
  .item-image {
    width: 160rpx;
    height: 160rpx;
    border-radius: 8rpx;
  }
  
  .item-info {
    flex: 1;
    margin-left: 20rpx;
    
    .item-title {
      font-size: 28rpx;
      color: #333;
      margin-bottom: 8rpx;
    }
    
    .item-size {
      font-size: 24rpx;
      color: #999;
      margin-bottom: 16rpx;
    }
    
    .item-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      
      .item-price {
        font-size: 32rpx;
        font-weight: 600;
        color: #333;
      }
    }
  }
  
  .quantity-control {
    display: flex;
    align-items: center;
    background: #f5f5f5;
    border-radius: 8rpx;
    
    .qty-btn {
      width: 56rpx;
      height: 56rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 32rpx;
      color: #666;
    }
    
    .qty-value {
      width: 60rpx;
      text-align: center;
      font-size: 28rpx;
    }
  }
  
  .item-delete {
    position: absolute;
    top: 16rpx;
    right: 16rpx;
    width: 40rpx;
    height: 40rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #999;
    font-size: 36rpx;
  }
}

.checkout-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 16rpx 32rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;
  
  .select-all {
    display: flex;
    align-items: center;
    gap: 12rpx;
    font-size: 28rpx;
    color: #333;
    
    .checkbox {
      width: 40rpx;
      height: 40rpx;
      border: 2rpx solid #ddd;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      background: #fff;
      
      &.checked {
        background: #333;
        border-color: #333;
      }
    }
  }
  
  .total-info {
    flex: 1;
    text-align: right;
    margin-right: 24rpx;
    
    .total-label {
      font-size: 26rpx;
      color: #666;
    }
    
    .total-price {
      font-size: 36rpx;
      font-weight: 600;
      color: #333;
    }
  }
  
  .btn-checkout {
    width: 200rpx;
    height: 80rpx;
    background: #333;
    color: #fff;
    border-radius: 40rpx;
    font-size: 28rpx;
    
    &[disabled] {
      background: #ccc;
    }
  }
}
</style>
