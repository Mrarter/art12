<template>
  <view class="confirm-page">
    <!-- 商品信息 -->
    <view class="goods-section" v-if="goodsList.length > 0">
      <view class="section-title">
        <text class="title-text">商品信息</text>
      </view>
      <view class="goods-card" v-for="item in goodsList" :key="item.id">
        <image class="goods-image" :src="item.coverImage || 'https://picsum.photos/200/200?random=goods'" mode="aspectFill"></image>
        <view class="goods-info">
          <text class="goods-title">{{ item.title }}</text>
          <text class="goods-meta">{{ item.artType || '艺术品' }} · {{ item.size || '标准尺寸' }}</text>
          <view class="goods-price-row">
            <text class="goods-price">¥{{ formatPrice(item.price) }}</text>
            <text class="goods-qty">x{{ item.quantity }}</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 收货地址 -->
    <view class="address-section" @click="goAddress">
      <view class="section-title">
        <text class="title-text">收货地址</text>
      </view>
      <view class="address-card">
        <view class="address-empty" v-if="!selectedAddress">
          <text class="add-icon">+</text>
          <text class="add-text">添加收货地址</text>
        </view>
        <view class="address-info" v-else>
          <view class="address-header">
            <text class="receiver">{{ selectedAddress.receiverName }}</text>
            <text class="phone">{{ selectedAddress.phone }}</text>
          </view>
          <text class="address-text">{{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }}{{ selectedAddress.detailAddress }}</text>
        </view>
        <text class="arrow-icon">›</text>
      </view>
    </view>
    
    <!-- 佣金说明 -->
    <view class="commission-section" v-if="hasPromoter">
      <view class="commission-card">
        <view class="commission-icon">🎖️</view>
        <view class="commission-content">
          <text class="commission-title">分享赚佣金</text>
          <text class="commission-desc">通过艺荐官链接购买，艺荐官可获得 ¥{{ formatPrice(commissionAmount) }} 佣金</text>
        </view>
      </view>
    </view>
    
    <!-- 价格明细 -->
    <view class="price-section">
      <view class="section-title">
        <text class="title-text">价格明细</text>
      </view>
      <view class="price-card">
        <view class="price-row">
          <text class="price-label">商品金额</text>
          <text class="price-value">¥{{ formatPrice(totalAmount) }}</text>
        </view>
        <view class="price-row">
          <text class="price-label">运费</text>
          <text class="price-value free">包邮</text>
        </view>
        <view class="price-row total">
          <text class="price-label">合计</text>
          <text class="price-value total-price">¥{{ formatPrice(totalAmount) }}</text>
        </view>
      </view>
    </view>
    
    <!-- 备注 -->
    <view class="remark-section">
      <view class="section-title">
        <text class="title-text">订单备注</text>
      </view>
      <view class="remark-card">
        <textarea 
          class="remark-input" 
          v-model="remark" 
          placeholder="选填，可备注您的特殊需求" 
          placeholder-class="placeholder"
          maxlength="200"
        />
        <text class="remark-count">{{ remark.length }}/200</text>
      </view>
    </view>
    
    <!-- 底部提交栏 -->
    <view class="submit-bar">
      <view class="submit-info">
        <text class="submit-label">实付款</text>
        <text class="submit-price">¥{{ formatPrice(totalAmount) }}</text>
      </view>
      <button class="btn-submit" @click="onSubmit" :loading="submitting" :disabled="submitting">
        {{ submitting ? '提交中...' : '提交订单' }}
      </button>
    </view>
  </view>
</template>

<script>
import { getCartList, getAddressList, createOrderFromCart, directBuy } from '@/api/order'
import { getProductDetail } from '@/api/product'

export default {
  data() {
    return {
      goodsType: 'direct',
      goodsList: [],
      selectedAddress: null,
      remark: '',
      submitting: false,
      cartIds: [],
      artworkId: null,
      loading: false
    }
  },

  computed: {
    totalAmount() {
      return this.goodsList.reduce((sum, item) => sum + (item.price || 0) * (item.quantity || 1), 0)
    },
    commissionAmount() {
      return this.totalAmount * 0.05
    },
    hasPromoter() {
      return false
    }
  },

  onLoad(options) {
    if (options.artworkId) {
      this.goodsType = 'direct'
      this.artworkId = parseInt(options.artworkId)
      this.fetchGoodsDetail(options.artworkId, parseInt(options.quantity) || 1)
    } else if (options.cartIds) {
      this.goodsType = 'cart'
      this.cartIds = options.cartIds.split(',').map(Number)
      this.fetchCartGoods()
    }
    this.fetchAddress()
  },

  methods: {
    async fetchGoodsDetail(id, quantity) {
      this.loading = true
      try {
        const res = await getProductDetail(id)
        if (res && res.id) {
          this.goodsList = [{
            id: res.id,
            title: res.title,
            coverImage: res.coverImage || res.cover,
            price: res.price,
            quantity: quantity,
            artType: res.artType || res.category,
            size: res.size
          }]
        } else {
          this.loadMockGoodsData(id)
        }
      } catch (e) {
        console.error('获取商品详情失败', e)
        this.loadMockGoodsData(id)
      } finally {
        this.loading = false
      }
    },

    async fetchCartGoods() {
      this.loading = true
      try {
        const list = await getCartList()
        if (list && list.length > 0) {
          const selectedItems = list.filter(item => this.cartIds.includes(item.id))
          this.goodsList = selectedItems.map(item => ({
            id: item.artworkId,
            title: item.title,
            coverImage: item.coverImage || item.cover,
            price: item.price,
            quantity: item.quantity,
            artType: item.artType || item.category,
            size: item.size
          }))
        }
        if (this.goodsList.length === 0) {
          this.loadMockGoodsData()
        }
      } catch (e) {
        console.error('获取购物车商品失败', e)
        this.loadMockGoodsData()
      } finally {
        this.loading = false
      }
    },

    loadMockGoodsData(id) {
      const mockData = [
        { id: 1, title: '山水长卷 · 张大千', coverImage: 'https://picsum.photos/200/200?random=1', price: 128000, quantity: 1, artType: '国画', size: '180x98cm' },
        { id: 2, title: '奔马图 · 徐悲鸿', coverImage: 'https://picsum.photos/200/200?random=2', price: 256000, quantity: 1, artType: '油画', size: '120x80cm' },
        { id: 3, title: '虾趣图 · 齐白石', coverImage: 'https://picsum.photos/200/200?random=3', price: 88000, quantity: 1, artType: '国画', size: '68x136cm' }
      ]
      
      if (id) {
        this.goodsList = [{
          id: parseInt(id),
          title: '当代名家书画作品',
          coverImage: 'https://picsum.photos/200/200?random=art' + id,
          price: 128000,
          quantity: 1,
          artType: '油画',
          size: '100x80cm'
        }]
      } else if (this.cartIds.length > 0) {
        this.goodsList = mockData.filter(item => this.cartIds.includes(item.id))
        if (this.goodsList.length === 0) {
          this.goodsList = [mockData[0]]
        }
      } else {
        this.goodsList = [mockData[0]]
      }
    },

    async fetchAddress() {
      try {
        const list = await getAddressList()
        if (list && list.length > 0) {
          this.selectedAddress = list.find(addr => addr.isDefault) || list[0] || null
        }
        if (!this.selectedAddress) {
          this.loadMockAddress()
        }
      } catch (e) {
        console.error('获取收货地址失败', e)
        this.loadMockAddress()
      }
    },

    loadMockAddress() {
      this.selectedAddress = {
        id: 1,
        receiverName: '张三',
        phone: '138****8888',
        province: '北京市',
        city: '朝阳区',
        district: '三里屯街道',
        detailAddress: 'SOHO现代城A座1201室'
      }
    },

    goAddress() {
      uni.navigateTo({
        url: '/pages/user/address?select=true',
        events: {
          selectAddress: (address) => {
            this.selectedAddress = address
          }
        }
      })
    },

    async onSubmit() {
      if (!this.selectedAddress) {
        uni.showToast({ title: '请选择收货地址', icon: 'none' })
        return
      }
      
      if (this.submitting) return
      this.submitting = true
      
      try {
        let order = null
        const params = {
          addressId: this.selectedAddress.id,
          remark: this.remark
        }
        
        if (this.goodsType === 'direct') {
          order = await directBuy({
            productId: this.artworkId,
            quantity: this.goodsList[0].quantity,
            ...params
          })
        } else {
          order = await createOrderFromCart({
            cartIds: this.cartIds,
            ...params
          })
        }
        
        uni.navigateTo({
          url: `/pages/order/pay?orderId=${order.id}&amount=${order.payAmount}`
        })
      } catch (e) {
        console.error('创建订单失败', e)
        uni.showToast({ title: '订单提交成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } finally {
        this.submitting = false
      }
    },

    formatPrice(price) {
      if (!price) return '0'
      const yuan = price / 100
      if (yuan >= 10000) {
        return (yuan / 10000).toFixed(yuan % 10000 === 0 ? 0 : 1) + '万'
      }
      return yuan.toLocaleString()
    }
  }
}
</script>

<style lang="scss" scoped>
/* 深色主题色 */
$bg-primary: #0D0D0D;
$bg-secondary: #1A1A1A;
$bg-card: #242424;
$text-primary: #FFFFFF;
$text-secondary: #B3B3B3;
$text-muted: #666666;
$accent-gold: #D4AF37;
$accent-orange: #E8A838;

.confirm-page {
  min-height: 100vh;
  padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  background-color: $bg-primary;
}

.section-title {
  padding: 20rpx 30rpx 16rpx;
  
  .title-text {
    font-size: 28rpx;
    font-weight: 600;
    color: $text-secondary;
  }
}

/* 商品信息 */
.goods-section {
  margin-bottom: 16rpx;
}

.goods-card {
  display: flex;
  background-color: $bg-card;
  margin: 0 20rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
}

.goods-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  background-color: $bg-secondary;
  margin-right: 20rpx;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.goods-title {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-meta {
  font-size: 24rpx;
  color: $text-muted;
  margin-bottom: auto;
}

.goods-price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.goods-price {
  font-size: 30rpx;
  color: $accent-gold;
  font-weight: 600;
}

.goods-qty {
  font-size: 26rpx;
  color: $text-muted;
}

/* 收货地址 */
.address-section {
  margin-bottom: 16rpx;
}

.address-card {
  display: flex;
  align-items: center;
  background-color: $bg-card;
  margin: 0 20rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
}

.address-empty {
  display: flex;
  align-items: center;
  flex: 1;
  
  .add-icon {
    width: 48rpx;
    height: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: $accent-gold;
    color: $bg-primary;
    font-size: 32rpx;
    font-weight: 600;
    border-radius: 50%;
    margin-right: 16rpx;
  }
  
  .add-text {
    font-size: 28rpx;
    color: $text-secondary;
  }
}

.address-info {
  flex: 1;
  
  .address-header {
    display: flex;
    align-items: center;
    margin-bottom: 12rpx;
    
    .receiver {
      font-size: 30rpx;
      font-weight: 600;
      color: $text-primary;
      margin-right: 20rpx;
    }
    
    .phone {
      font-size: 28rpx;
      color: $text-secondary;
    }
  }
  
  .address-text {
    font-size: 26rpx;
    color: $text-muted;
    line-height: 1.5;
  }
}

.arrow-icon {
  font-size: 40rpx;
  color: $text-muted;
  margin-left: 16rpx;
}

/* 佣金说明 */
.commission-section {
  margin-bottom: 16rpx;
}

.commission-card {
  display: flex;
  align-items: center;
  background-color: $bg-card;
  margin: 0 20rpx;
  padding: 20rpx 24rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(212, 175, 55, 0.2);
  
  .commission-icon {
    font-size: 40rpx;
    margin-right: 16rpx;
  }
  
  .commission-content {
    flex: 1;
    
    .commission-title {
      display: block;
      font-size: 28rpx;
      color: $text-primary;
      font-weight: 500;
      margin-bottom: 6rpx;
    }
    
    .commission-desc {
      font-size: 24rpx;
      color: $text-muted;
    }
  }
}

/* 价格明细 */
.price-section {
  margin-bottom: 16rpx;
}

.price-card {
  background-color: $bg-card;
  margin: 0 20rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  
  .price-label {
    font-size: 28rpx;
    color: $text-secondary;
  }
  
  .price-value {
    font-size: 28rpx;
    color: $text-primary;
    
    &.free {
      color: $accent-gold;
    }
  }
  
  &.total {
    border-top: 1rpx solid rgba(255, 255, 255, 0.06);
    margin-top: 8rpx;
    padding-top: 24rpx;
    
    .price-label {
      font-size: 30rpx;
      font-weight: 600;
      color: $text-primary;
    }
    
    .total-price {
      font-size: 36rpx;
      font-weight: 600;
      color: $accent-gold;
    }
  }
}

/* 备注 */
.remark-section {
  margin-bottom: 16rpx;
}

.remark-card {
  background-color: $bg-card;
  margin: 0 20rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
  position: relative;
}

.remark-input {
  width: 100%;
  min-height: 160rpx;
  font-size: 28rpx;
  color: $text-primary;
  background: transparent;
  line-height: 1.6;
}

.placeholder {
  color: $text-muted;
}

.remark-count {
  position: absolute;
  bottom: 16rpx;
  right: 20rpx;
  font-size: 22rpx;
  color: $text-muted;
}

/* 底部提交栏 */
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
  background-color: $bg-secondary;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.submit-info {
  display: flex;
  flex-direction: column;
  
  .submit-label {
    font-size: 24rpx;
    color: $text-muted;
    margin-bottom: 4rpx;
  }
  
  .submit-price {
    font-size: 44rpx;
    font-weight: 600;
    color: $accent-gold;
  }
}

.btn-submit {
  width: 260rpx;
  height: 88rpx;
  background: linear-gradient(135deg, $accent-gold 0%, $accent-orange 100%);
  color: $bg-primary;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  
  &::after {
    border: none;
  }
  
  &:active {
    opacity: 0.9;
  }
  
  &[disabled] {
    opacity: 0.6;
  }
}
</style>
