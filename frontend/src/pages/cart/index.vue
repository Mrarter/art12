<template>
  <view class="cart-page">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <view class="nav-title">购物车</view>
      <view class="nav-edit" @click="toggleEditMode">
        <text>{{ isEditMode ? '完成' : '编辑' }}</text>
      </view>
    </view>

    <!-- 购物车列表 -->
    <view class="cart-list" v-if="cartList.length > 0">
      <!-- 按发布者分组 -->
      <view class="cart-group" v-for="group in groupedCartList" :key="group.publisherId">
        <!-- 发布者头部 -->
        <view class="group-header">
          <view class="header-left">
            <checkbox 
              :checked="isGroupSelected(group)" 
              @click="toggleGroupSelect(group)"
              color="#c9a227"
            ></checkbox>
            <view class="publisher-info" @click="goPublisherHome(group.publisherId)">
              <image class="publisher-avatar" :src="group.publisherAvatar" mode="aspectFill"></image>
              <text class="publisher-name">{{ group.publisherName }}</text>
              
            </view>
          </view>
        </view>
        
        <!-- 商品列表 -->
        <view class="group-items">
          <view class="cart-item" v-for="item in group.items" :key="item.id">
            <view class="item-checkbox">
              <checkbox 
                :checked="isSelected(item.id)" 
                @click="toggleSelect(item.id)"
                :disabled="item.locked"
                color="#c9a227"
              ></checkbox>
            </view>
            <image class="item-image" :src="item.cover" mode="aspectFill" @click="goDetail(item.productId)"></image>
            <view class="item-info">
              <view class="item-header">
                <view class="item-title" @click="goDetail(item.productId)">{{ item.title }}</view>
                <view class="item-delete" @click="deleteItem(item.id)" v-if="!item.locked && isEditMode">
                  
                </view>
              </view>
              <view class="item-artist">{{ item.artistName }}</view>
              <view class="item-footer">
                <view class="item-price-wrap">
                  <text class="item-price">¥{{ formatPrice(item.price) }}</text>
                  <text class="item-original" v-if="item.originalPrice">¥{{ formatPrice(item.originalPrice) }}</text>
                </view>
                <view class="item-num" v-if="!item.locked">
                  <view class="num-btn minus" @click="decreaseNum(item)">
                    
                  </view>
                  <text class="num-value">{{ item.num }}</text>
                  <view class="num-btn plus" @click="increaseNum(item)">
                    
                  </view>
                </view>
                <view class="locked-tag" v-else>
                  <text>🔒</text>
                </view>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 艺荐官信息 -->
        <view class="promoter-info" v-if="group.promoterId">
          <view class="promoter-badge">
            <text>★</text>
            <text>艺荐官</text>
          </view>
          <text class="promoter-name">{{ group.promoterName }}</text>
        </view>
      </view>
    </view>
    
    <!-- 空购物车 -->
    <view class="empty-cart" v-else>
      <view class="empty-icon-wrap">
        <text>🛒</text>
      </view>
      <text class="empty-title">购物车空空如也</text>
      <text class="empty-subtitle">快去发现心仪的艺术品吧</text>
      <view class="empty-btn" @click="goGallery">
        <text>去逛逛</text>
      </view>
    </view>
    
    <!-- 底部结算栏 -->
    <view class="settlement-bar" v-if="cartList.length > 0">
      <view class="bar-left">
        <checkbox 
          :checked="isAllSelected" 
          @click="toggleSelectAll"
          color="#c9a227"
        ></checkbox>
        <text class="select-text">全选</text>
        <view class="total-wrap">
          <text class="total-label">合计</text>
          <text class="total-price">¥{{ formatPrice(totalPrice) }}</text>
        </view>
      </view>
      <view class="bar-right">
        <view class="settle-btn" :class="{ disabled: selectedCount === 0 }" @click="handleSettle">
          <text v-if="isEditMode">删除{{ selectedCount > 0 ? `(${selectedCount})` : '' }}</text>
          <text v-else>结算{{ selectedCount > 0 ? `(${selectedCount})` : '' }}</text>
        </view>
      </view>
    </view>
    
    <!-- 自定义TabBar -->
    <CustomTabBar :currentIndex="3" />
  </view>
</template>

<script>
import CustomTabBar from '@/components/custom-tab-bar/index.vue'
import { useCartStore } from '@/store/modules/cart.js'
import { removeFromCart, updateCartNum, lockCartItems } from '@/api/cart.js'

export default {
  components: {
    CustomTabBar
  },
  data() {
    return {
      groupedCartList: [],
      isEditMode: false
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
    
    isGroupSelected(group) {
      const allIds = group.items.map(item => item.id)
      return allIds.length > 0 && allIds.every(id => this.selectedList.includes(id))
    },
    
    toggleSelect(id) {
      this.cartStore.toggleSelect(id)
    },
    
    toggleGroupSelect(group) {
      const allIds = group.items.map(item => item.id)
      const allSelected = allIds.every(id => this.selectedList.includes(id))
      if (allSelected) {
        allIds.forEach(id => {
          if (this.selectedList.includes(id)) {
            this.cartStore.toggleSelect(id)
          }
        })
      } else {
        allIds.forEach(id => {
          if (!this.selectedList.includes(id)) {
            this.cartStore.toggleSelect(id)
          }
        })
      }
    },
    
    toggleSelectAll() {
      if (this.isAllSelected) {
        this.cartStore.unselectAll()
      } else {
        this.cartStore.selectAll()
      }
    },
    
    toggleEditMode() {
      this.isEditMode = !this.isEditMode
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
        title: '',
        content: '确定要删除该商品吗？',
        confirmColor: '#c9a227',
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
    
    async handleSettle() {
      if (this.selectedCount === 0) {
        uni.showToast({ title: '请选择商品', icon: 'none' })
        return
      }
      
      if (this.isEditMode) {
        // 编辑模式：批量删除
        uni.showModal({
          title: '',
          content: `确定要删除选中的 ${this.selectedCount} 件商品吗？`,
          confirmColor: '#c9a227',
          success: async (res) => {
            if (res.confirm) {
              const idsToDelete = [...this.selectedList]
              idsToDelete.forEach(id => {
                this.cartStore.removeFromCart(id)
              })
              this.groupCartList()
              this.isEditMode = false
              try {
                await removeFromCart(idsToDelete)
              } catch (e) {}
              uni.showToast({ title: '删除成功', icon: 'success' })
            }
          }
        })
      } else {
        // 结算模式
        this.goSettle()
      }
    },
    
    async goSettle() {
      const selectedIds = this.selectedList
      const selectedItems = this.cartStore.selectedItems
      
      const hasLocked = selectedItems.some(item => item.locked)
      if (hasLocked) {
        uni.showToast({ title: '部分商品已被锁定', icon: 'none' })
        return
      }
      
      try {
        await lockCartItems(selectedIds)
        selectedIds.forEach(id => {
          const item = this.cartList.find(i => i.id === id)
          if (item) item.locked = true
        })
        uni.navigateTo({ url: '/pages/order/confirm' })
      } catch (e) {
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
    },
    
    formatPrice(price) {
      if (!price) return '0'
      const yuan = price / 100  // 分转元
      if (yuan >= 10000) {
        return (yuan / 10000).toFixed(yuan % 10000 === 0 ? 0 : 1) + '万'
      }
      return yuan.toLocaleString()
    }
  }
}
</script>

<style lang="scss" scoped>
/* 变量定义 */
$bg-primary: #0d0d0d;
$bg-card: #1a1a1a;
$bg-elevated: #242424;
$text-primary: #ffffff;
$text-secondary: #999999;
$text-muted: #666666;
$accent-gold: #c9a227;
$accent-gold-light: #e5c76b;
$border-color: rgba(255, 255, 255, 0.08);
$danger-color: #e74c3c;

.cart-page {
  min-height: 100vh;
  background: $bg-primary;
  padding-bottom: 140rpx;
}

/* 导航栏 */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  padding-top: calc(var(--status-bar-height) + 20rpx);
  background: $bg-card;
  border-bottom: 1rpx solid $border-color;
}

.nav-title {
  font-size: 36rpx;
  font-weight: 600;
  color: $text-primary;
}

.nav-edit {
  font-size: 28rpx;
  color: $accent-gold;
}

/* 购物车列表 */
.cart-list {
  padding: 20rpx 24rpx;
  padding-bottom: calc(100rpx + env(safe-area-inset-bottom));
}

.cart-group {
  background: $bg-card;
  border-radius: 20rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
}

/* 分组头部 */
.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 24rpx;
  border-bottom: 1rpx solid $border-color;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.publisher-info {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.publisher-avatar {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  border: 1rpx solid $border-color;
}

.publisher-name {
  font-size: 26rpx;
  color: $text-primary;
  font-weight: 500;
}

/* 商品项 */
.group-items {
  padding: 0 24rpx;
}

.cart-item {
  display: flex;
  align-items: flex-start;
  padding: 28rpx 0;
  border-bottom: 1rpx solid $border-color;
  
  &:last-child {
    border-bottom: none;
  }
}

.item-checkbox {
  margin-right: 20rpx;
  padding-top: 40rpx;
}

.item-image {
  width: 180rpx;
  height: 180rpx;
  border-radius: 12rpx;
  background: $bg-elevated;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.item-title {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  line-height: 1.4;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-delete {
  padding: 8rpx;
  margin: -8rpx;
  margin-left: 16rpx;
}

.item-artist {
  font-size: 22rpx;
  color: $text-secondary;
  margin-bottom: 16rpx;
}

.item-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-price-wrap {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}

.item-price {
  font-size: 32rpx;
  color: $text-primary;
  font-weight: 600;
}

.item-original {
  font-size: 22rpx;
  color: $text-muted;
  text-decoration: line-through;
}

.item-num {
  display: flex;
  align-items: center;
  background: $bg-elevated;
  border-radius: 8rpx;
  overflow: hidden;
}

.num-btn {
  width: 52rpx;
  height: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &.minus {
    border-right: 1rpx solid $border-color;
  }
  
  &.plus {
    border-left: 1rpx solid $border-color;
  }
}

.num-value {
  min-width: 60rpx;
  text-align: center;
  font-size: 26rpx;
  color: $text-primary;
}

.locked-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52rpx;
  height: 52rpx;
  background: $bg-elevated;
  border-radius: 8rpx;
}

/* 艺荐官信息 */
.promoter-info {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 20rpx 24rpx;
  background: rgba($accent-gold, 0.05);
  border-top: 1rpx solid $border-color;
}

.promoter-badge {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 4rpx 12rpx;
  background: rgba($accent-gold, 0.15);
  border-radius: 16rpx;
  font-size: 20rpx;
  color: $accent-gold;
}

.promoter-name {
  font-size: 24rpx;
  color: $text-secondary;
}

/* 空购物车 */
.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
}

.empty-icon-wrap {
  width: 200rpx;
  height: 200rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-card;
  border-radius: 50%;
  margin-bottom: 40rpx;
}

.empty-title {
  font-size: 32rpx;
  color: $text-primary;
  font-weight: 500;
  margin-bottom: 16rpx;
}

.empty-subtitle {
  font-size: 26rpx;
  color: $text-secondary;
  margin-bottom: 60rpx;
}

.empty-btn {
  padding: 24rpx 80rpx;
  background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
  border-radius: 44rpx;
  
  text {
    font-size: 30rpx;
    color: $bg-primary;
    font-weight: 500;
  }
}

/* 底部结算栏 */
.settlement-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: $bg-card;
  border-top: 1rpx solid $border-color;
}

.bar-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.select-text {
  font-size: 28rpx;
  color: $text-primary;
}

.total-wrap {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
  margin-left: 20rpx;
}

.total-label {
  font-size: 24rpx;
  color: $text-secondary;
}

.total-price {
  font-size: 40rpx;
  color: $text-primary;
  font-weight: 700;
}

.bar-right {
  display: flex;
  align-items: center;
}

.settle-btn {
  padding: 0 48rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
  border-radius: 44rpx;
  box-shadow: 0 4rpx 20rpx rgba($accent-gold, 0.3);
  
  text {
    font-size: 30rpx;
    color: $bg-primary;
    font-weight: 600;
  }
  
  &.disabled {
    background: $bg-elevated;
    box-shadow: none;
    
    text {
      color: $text-muted;
    }
  }
}

/* checkbox 样式覆盖 */
::v-deep {
  .uni-checkbox-wrapper {
    .uni-checkbox-input {
      width: 40rpx;
      height: 40rpx;
      border-radius: 50%;
      border: 2rpx solid $text-muted;
      background: transparent;
      
      &.uni-checkbox-input-checked {
        background: $accent-gold;
        border-color: $accent-gold;
      }
    }
  }
}
</style>
