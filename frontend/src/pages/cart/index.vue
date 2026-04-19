<template>
  <view class="cart-page">
    <!-- 购物车列表 -->
    <view class="cart-list" v-if="cartList.length > 0">
      <!-- 按发布者分组 -->
      <view class="cart-group" v-for="group in groupedCartList" :key="group.publisherId">
        <view class="group-header">
          <view class="group-publisher" @click="goPublisherHome(group.publisherId)">
            <image class="publisher-avatar" :src="group.publisherAvatar" mode="aspectFill"></image>
            <text class="publisher-name">{{ group.publisherName }}</text>
            <u-icon name="arrow-right" size="14" color="#999"></u-icon>
          </view>
        </view>
        
        <view class="group-items">
          <view class="cart-item" v-for="item in group.items" :key="item.id">
            <view class="item-checkbox">
              <checkbox 
                :checked="isSelected(item.id)" 
                @click="toggleSelect(item.id)"
                :disabled="item.locked"
              ></checkbox>
            </view>
            <image class="item-image" :src="item.cover" mode="aspectFill" @click="goDetail(item.productId)"></image>
            <view class="item-info">
              <view class="item-title" @click="goDetail(item.productId)">{{ item.title }}</view>
              <view class="item-meta">{{ item.artistName }}</view>
              <view class="item-bottom">
                <text class="item-price">¥{{ item.price }}</text>
                <view class="item-num" v-if="!item.locked">
                  <view class="num-btn" @click="decreaseNum(item)">-</view>
                  <text class="num-value">{{ item.num }}</text>
                  <view class="num-btn" @click="increaseNum(item)">+</view>
                </view>
                <view class="locked-tag" v-else>
                  <u-icon name="lock" size="12" color="#999"></u-icon>
                  <text>锁定中</text>
                </view>
              </view>
            </view>
            <view class="item-delete" @click="deleteItem(item.id)" v-if="!item.locked">
              <u-icon name="close" size="16" color="#999"></u-icon>
            </view>
          </view>
        </view>
        
        <!-- 艺荐官信息 -->
        <view class="promoter-info" v-if="group.promoterId">
          <u-icon name="account" size="14" color="#999"></u-icon>
          <text>艺荐官: {{ group.promoterName }}</text>
        </view>
      </view>
    </view>
    
    <!-- 空购物车 -->
    <view class="empty-cart" v-else>
      <image class="empty-icon" src="/static/icons/cart-empty.png" mode="aspectFit"></image>
      <text class="empty-text">购物车空空如也</text>
      <view class="empty-btn" @click="goGallery">去逛逛</view>
    </view>
    
    <!-- 底部结算栏 -->
    <view class="settlement-bar" v-if="cartList.length > 0">
      <view class="select-all">
        <checkbox :checked="isAllSelected" @click="toggleSelectAll"></checkbox>
        <text>全选</text>
      </view>
      <view class="total-info">
        <text class="total-label">合计:</text>
        <text class="total-price">¥{{ totalPrice }}</text>
      </view>
      <view class="settle-btn" :class="{ disabled: selectedCount === 0 }" @click="goSettle">
        结算{{ selectedCount > 0 ? `(${selectedCount})` : '' }}
      </view>
    </view>
  </view>
</template>

<script>
import { useCartStore } from '@/store/modules/cart.js'
import { removeFromCart, updateCartNum, lockCartItems } from '@/api/cart.js'

export default {
  data() {
    return {
      groupedCartList: []
    }
  },
  
  computed: {
    cartStore() {
      return useCartStore()
    },
    cartList() {
      return this.cartStore.cartList
    },
    selectedList() {
      return this.cartStore.selectedList
    },
    isAllSelected() {
      return this.cartList.length > 0 && this.selectedList.length === this.cartList.length
    },
    selectedCount() {
      return this.selectedList.length
    },
    totalPrice() {
      return this.cartStore.totalPrice
    }
  },
  
  onShow() {
    this.initCart()
  },
  
  methods: {
    initCart() {
      this.cartStore.initFromStorage()
      this.groupCartList()
    },
    
    groupCartList() {
      // 按发布者分组
      const groups = {}
      this.cartList.forEach(item => {
        const publisherId = item.publisherId || 'default'
        if (!groups[publisherId]) {
          groups[publisherId] = {
            publisherId,
            publisherName: item.publisherName || '未知卖家',
            publisherAvatar: item.publisherAvatar || '/static/avatar/default.jpg',
            promoterId: item.promoterId,
            promoterName: item.promoterName || '',
            items: []
          }
        }
        groups[publisherId].items.push(item)
      })
      this.groupedCartList = Object.values(groups)
    },
    
    isSelected(id) {
      return this.selectedList.includes(id)
    },
    
    toggleSelect(id) {
      this.cartStore.toggleSelect(id)
    },
    
    toggleSelectAll() {
      if (this.isAllSelected) {
        this.cartStore.unselectAll()
      } else {
        this.cartStore.selectAll()
      }
    },
    
    async decreaseNum(item) {
      if (item.num > 1) {
        item.num--
        this.cartStore.updateQuantity(item.id, item.num)
        try {
          await updateCartNum({ id: item.id, num: item.num })
        } catch (e) {}
      }
    },
    
    async increaseNum(item) {
      item.num++
      this.cartStore.updateQuantity(item.id, item.num)
      try {
        await updateCartNum({ id: item.id, num: item.num })
      } catch (e) {}
    },
    
    async deleteItem(id) {
      uni.showModal({
        title: '提示',
        content: '确定要删除该商品吗？',
        success: async (res) => {
          if (res.confirm) {
            this.cartStore.removeFromCart(id)
            this.groupCartList()
            try {
              await removeFromCart([id])
            } catch (e) {}
          }
        }
      })
    },
    
    async goSettle() {
      if (this.selectedCount === 0) return
      
      const selectedIds = this.selectedList
      const selectedItems = this.cartStore.selectedItems
      
      // 检查是否有锁定商品
      const hasLocked = selectedItems.some(item => item.locked)
      if (hasLocked) {
        uni.showToast({ title: '部分商品已被锁定', icon: 'none' })
        return
      }
      
      try {
        // 锁定购物车商品
        await lockCartItems(selectedIds)
        
        // 更新本地锁定状态
        selectedIds.forEach(id => {
          const item = this.cartList.find(i => i.id === id)
          if (item) item.locked = true
        })
        
        // 跳转到确认订单页
        uni.navigateTo({ url: '/pages/order/confirm' })
      } catch (e) {
        // 即使锁定失败也跳转
        uni.navigateTo({ url: '/pages/order/confirm' })
      }
    },
    
    goGallery() {
      uni.switchTab({ url: '/pages/gallery/index' })
    },
    
    goDetail(id) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    },
    
    goPublisherHome(id) {
      uni.navigateTo({ url: `/pages/artist/home?id=${id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.cart-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.cart-list {
  padding: 20rpx;
}

.cart-group {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.group-header {
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.group-publisher {
  display: flex;
  align-items: center;
}

.publisher-avatar {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  margin-right: 12rpx;
}

.publisher-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.item-checkbox {
  margin-right: 20rpx;
}

.item-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 8rpx;
  margin-right: 20rpx;
}

.item-info {
  flex: 1;
}

.item-title {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 10rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 16rpx;
}

.item-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-price {
  font-size: 30rpx;
  color: #e74c3c;
  font-weight: 600;
}

.item-num {
  display: flex;
  align-items: center;
}

.num-btn {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  background: #f5f5f5;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #333;
}

.num-value {
  min-width: 60rpx;
  text-align: center;
  font-size: 28rpx;
  color: #333;
}

.locked-tag {
  display: flex;
  align-items: center;
  font-size: 22rpx;
  color: #999;
}

.locked-tag text {
  margin-left: 6rpx;
}

.item-delete {
  padding: 10rpx;
  margin-left: 10rpx;
}

.promoter-info {
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  font-size: 22rpx;
  color: #999;
  background: #fafafa;
}

.promoter-info text {
  margin-left: 8rpx;
}

.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 30rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
  margin-bottom: 40rpx;
}

.empty-btn {
  padding: 20rpx 60rpx;
  background: #333;
  color: #fff;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.settlement-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  height: 100rpx;
  padding: 0 30rpx;
  padding-bottom: env(safe-area-inset-bottom);
  background: #fff;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.select-all {
  display: flex;
  align-items: center;
}

.select-all text {
  margin-left: 12rpx;
  font-size: 28rpx;
  color: #333;
}

.total-info {
  flex: 1;
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  margin-right: 30rpx;
}

.total-label {
  font-size: 26rpx;
  color: #666;
}

.total-price {
  font-size: 36rpx;
  color: #e74c3c;
  font-weight: 600;
}

.settle-btn {
  padding: 0 50rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: #333;
  color: #fff;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.settle-btn.disabled {
  background: #ccc;
  color: #fff;
}
</style>
