<template>
  <view class="gallery-page">
    <!-- 顶部艺术门类Tab -->
    <view class="category-tabs">
      <scroll-view class="category-scroll" scroll-x enable-flex>
        <view 
          class="category-item" 
          :class="{ active: currentCategory === item.id }" 
          v-for="item in categoryList" 
          :key="item.id"
          @click="selectCategory(item.id)"
        >
          <text class="category-name">{{ item.name }}</text>
          <view class="category-indicator" v-if="currentCategory === item.id"></view>
        </view>
      </scroll-view>
    </view>

    <!-- 商品列表 -->
    <scroll-view class="product-list" scroll-y @scrolltolower="loadMore" refresher-enabled @refresherrefresh="onRefresh">
      <view class="waterfall-container">
        <view class="waterfall-column">
          <view class="product-card" v-for="item in leftList" :key="item.id" @click="goDetail(item.id)">
            <view class="image-wrapper">
              <image class="product-image" :src="item.cover" mode="widthFix" :lazy-load="true"></image>
              <!-- SOLD标签 -->
              <view class="sold-badge" v-if="item.stock === 0">
                <view class="sold-text">SOLD</view>
              </view>
              <!-- 新作发布标签 -->
              <view class="stock-badge" v-if="item.isNew">
                <text>新作发布</text>
              </view>
            </view>
            <view class="product-info">
              <view class="product-title">{{ item.title }}</view>
              <view class="product-meta">
                <text class="artist-name">{{ item.artistName }}</text>
                <text class="meta-sep">|</text>
                <text class="category-name">{{ item.category }}</text>
              </view>
              <view class="product-size">
                <text>🖼</text>
                <text>{{ item.size }}</text>
              </view>
              <view class="product-footer">
                <view class="price-info">
                  <text class="current-price">¥{{ formatPrice(item.price) }}</text>
                  <text class="original-price" v-if="item.originalPrice">¥{{ formatPrice(item.originalPrice) }}</text>
                </view>
                <view class="price-change" v-if="item.priceChange > 0">
                  
                  <text>{{ item.priceChange }}%</text>
                </view>
              </view>
            </view>
          </view>
        </view>
        <view class="waterfall-column">
          <view class="product-card" v-for="item in rightList" :key="item.id" @click="goDetail(item.id)">
            <view class="image-wrapper">
              <image class="product-image" :src="item.cover" mode="widthFix" :lazy-load="true"></image>
              <!-- SOLD标签 -->
              <view class="sold-badge" v-if="item.stock === 0">
                <view class="sold-text">SOLD</view>
              </view>
              <!-- 新作发布标签 -->
              <view class="stock-badge" v-if="item.isNew">
                <text>新作发布</text>
              </view>
            </view>
            <view class="product-info">
              <view class="product-title">{{ item.title }}</view>
              <view class="product-meta">
                <text class="artist-name">{{ item.artistName }}</text>
                <text class="meta-sep">|</text>
                <text class="category-name">{{ item.category }}</text>
              </view>
              <view class="product-size">
                <text>🖼</text>
                <text>{{ item.size }}</text>
              </view>
              <view class="product-footer">
                <view class="price-info">
                  <text class="current-price">¥{{ formatPrice(item.price) }}</text>
                  <text class="original-price" v-if="item.originalPrice">¥{{ formatPrice(item.originalPrice) }}</text>
                </view>
                <view class="price-change" v-if="item.priceChange > 0">
                  
                  <text>{{ item.priceChange }}%</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
      
      <view class="load-more" v-if="hasMore && loading">
        <text class="loading-text">加载中...</text>
        <text>加载中...</text>
      </view>
      <view class="no-more" v-else-if="!hasMore && productList.length > 0">
        <view class="divider-line"></view>
        <text>— 没有更多了 —</text>
        <view class="divider-line"></view>
      </view>
      <view class="empty-state" v-else-if="productList.length === 0 && !loading">
        <image class="empty-image" src="/static/empty/product.png" mode="aspectFit"></image>
        <text class="empty-text">暂无相关作品</text>
        <view class="empty-btn" @click="loadRandomProducts">换一批</view>
      </view>
    </scroll-view>

    <!-- 筛选弹出层 -->
    <!-- 筛选功能暂未实现 -->
  </view>
</template>

<script>
import { getGalleryList, getCategories } from '@/api/product.js'

export default {
  data() {
    return {
      loading: false,
      categoryList: [
        { id: '', name: '全部' },
        { id: 'oil', name: '油画' },
        { id: 'sculpture', name: '雕塑' },
        { id: 'ink', name: '水墨' },
        { id: 'print', name: '版画' },
        { id: 'install', name: '装置' },
        { id: 'photo', name: '摄影' }
      ],
      currentCategory: '',

      yearOptions: [
        { label: '不限', value: '' },
        { label: '2020年后', value: '2020+' },
        { label: '2010-2020', value: '2010-2020' },
        { label: '2000-2010', value: '2000-2010' },
        { label: '2000年前', value: '2000-' }
      ],
      sizeOptions: [
        { label: '不限', value: '' },
        { label: '小尺寸(<50cm)', value: 'small' },
        { label: '中尺寸(50-100cm)', value: 'medium' },
        { label: '大尺寸(>100cm)', value: 'large' }
      ],
      holdTimeOptions: [
        { label: '不限', value: '' },
        { label: '3个月内', value: '3m' },
        { label: '6个月内', value: '6m' },
        { label: '1年内', value: '1y' },
        { label: '1年以上', value: '1y+' }
      ],
      artistTypeOptions: [
        { label: '不限', value: '' },
        { label: '大师级', value: 'master' },
        { label: '人气艺术家', value: 'popular' },
        { label: '认证艺术家', value: 'verified' }
      ],
      productList: [],
      page: 1,
      pageSize: 10,
      hasMore: true,
      showFilterPopup: false,
      filterType: 'sort',
      tempMinPrice: '',
      tempMaxPrice: '',
      filterParams: {
        minPrice: '',
        maxPrice: '',
        year: '',
        size: '',
        holdTime: '',
        region: '',
        artistId: '',
        artistName: '',
        artistType: ''
      },
      artistKeyword: '',
      artistList: [
        { id: 1, name: '张大千', avatar: '', badge: '大师级' },
        { id: 2, name: '齐白石', avatar: '', badge: '大师级' },
        { id: 3, name: '徐悲鸿', avatar: '', badge: '大师级' },
        { id: 4, name: '潘天寿', avatar: '', badge: '大师级' },
        { id: 5, name: '李可染', avatar: '', badge: '人气艺术家' },
        { id: 6, name: '吴冠中', avatar: '', badge: '大师级' }
      ]
    }
  },
  
  computed: {
    leftList() {
      return this.productList.filter((item, index) => index % 2 === 0)
    },
    rightList() {
      return this.productList.filter((item, index) => index % 2 === 1)
    },
    hasActiveFilters() {
      return this.filterParams.minPrice || 
             this.filterParams.maxPrice || 
             this.filterParams.year || 
             this.filterParams.size || 
             this.filterParams.artistId ||
             this.filterParams.artistType
    },
    formatPriceFilter() {
      const min = this.filterParams.minPrice
      const max = this.filterParams.maxPrice
      if (min && max) return `${this.formatPrice(min)}-${this.formatPrice(max)}`
      if (min) return `${this.formatPrice(min)}以上`
      if (max) return `${this.formatPrice(max)}以下`
      return ''
    },
    getYearLabel() {
      const item = this.yearOptions.find(o => o.value === this.filterParams.year)
      return item ? item.label : ''
    },
    getSizeLabel() {
      const item = this.sizeOptions.find(o => o.value === this.filterParams.size)
      return item ? item.label : ''
    },
    getFilterTitle() {
      const titles = {
        price: '价格区间',
        year: '创作年代',
        size: '尺寸',
        more: '更多筛选'
      }
      return titles[this.filterType] || '筛选'
    }
  },
  
  onLoad() {
    this.loadCategories()
    this.loadProducts()
  },
  
  onShow() {
    // 每次显示时刷新数据
  },
  
  methods: {
    async loadCategories() {
      try {
        const res = await getCategories()
        if (res && res.length > 0) {
          this.categoryList = [
            { id: '', name: '全部' },
            ...res.map(item => ({ id: item.id, name: item.name }))
          ]
        }
      } catch (e) {
        // 使用默认分类
      }
    },
    
    async loadProducts() {
      try {
        this.loading = true
        const res = await getGalleryList({
          category: this.currentCategory,
          page: this.page,
          pageSize: this.pageSize,
          ...this.filterParams
        })
        
        // 处理 PageResult 格式：{ records: [], total: xxx }
        const list = res?.records || res?.list || res || []
        
        if (this.page === 1) {
          this.productList = list
        } else {
          this.productList = [...this.productList, ...list]
        }
        
        this.hasMore = list.length >= this.pageSize
      } catch (e) {
        this.loadMockData()
      } finally {
        this.loading = false
      }
    },
    
    loadMockData() {
      const mockData = [
        { id: 1, cover: '/static/product/demo1.jpg', title: '山水长卷', artistName: '张大千', category: '油画', size: '100x200cm', price: 128000, originalPrice: 150000, priceChange: 5.2, stock: 0, isNew: true },
        { id: 2, cover: '/static/product/demo2.jpg', title: '虾趣图', artistName: '齐白石', category: '水墨', size: '50x80cm', price: 88000, originalPrice: 100000, priceChange: 3.8, stock: 3, isNew: false },
        { id: 3, cover: '/static/product/demo3.jpg', title: '奔马图', artistName: '徐悲鸿', category: '油画', size: '120x80cm', price: 256000, priceChange: 8.5, stock: 1, isNew: true },
        { id: 4, cover: '/static/product/demo4.jpg', title: '松鹰图', artistName: '潘天寿', category: '水墨', size: '80x150cm', price: 158000, originalPrice: 180000, priceChange: 2.1, stock: 5, isNew: false },
        { id: 5, cover: '/static/product/demo5.jpg', title: '春山云起', artistName: '李可染', category: '油画', size: '60x90cm', price: 98000, priceChange: 0, stock: 0, isNew: true },
        { id: 6, cover: '/static/product/demo6.jpg', title: '江南水乡', artistName: '吴冠中', category: '水墨', size: '45x70cm', price: 156000, priceChange: 4.2, stock: 2, isNew: false }
      ]
      
      if (this.page === 1) {
        this.productList = mockData
      } else {
        this.productList = [...this.productList, ...mockData]
      }
      this.hasMore = false
    },
    
    loadRandomProducts() {
      this.resetFilter()
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    
    formatPrice(price) {
      if (!price) return '0'
      if (price >= 10000) {
        return (price / 10000).toFixed(price % 10000 === 0 ? 0 : 1) + '万'
      }
      return price.toLocaleString()
    },
    
    selectCategory(id) {
      this.currentCategory = id
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    
    showFilter(type) {
      this.filterType = type
      this.tempMinPrice = this.filterParams.minPrice
      this.tempMaxPrice = this.filterParams.maxPrice
      this.showFilterPopup = true
    },
    
    selectYear(item) {
      this.filterParams.year = item.value
      this.showFilterPopup = false
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    
    selectSize(item) {
      this.filterParams.size = item.value
      this.showFilterPopup = false
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    
    selectHoldTime(item) {
      this.filterParams.holdTime = item.value
      this.showFilterPopup = false
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    
    selectArtistType(item) {
      this.filterParams.artistType = item.value
      this.showFilterPopup = false
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    
    searchArtist() {
      if (this.artistKeyword) {
        // 模拟搜索
        uni.showToast({ title: '搜索中...', icon: 'loading' })
      }
    },
    
    selectArtist(item) {
      if (this.filterParams.artistId === item.id) {
        this.filterParams.artistId = ''
        this.filterParams.artistName = ''
      } else {
        this.filterParams.artistId = item.id
        this.filterParams.artistName = item.name
      }
      this.showFilterPopup = false
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    
    setPriceRange(min, max) {
      this.tempMinPrice = min
      this.tempMaxPrice = max
      this.filterParams.minPrice = min
      this.filterParams.maxPrice = max
      this.showFilterPopup = false
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    
    applyPriceFilter() {
      if (this.tempMinPrice || this.tempMaxPrice) {
        this.filterParams.minPrice = this.tempMinPrice
        this.filterParams.maxPrice = this.tempMaxPrice
        this.showFilterPopup = false
        this.page = 1
        this.productList = []
        this.loadProducts()
      }
    },
    
    clearPrice() {
      this.filterParams.minPrice = ''
      this.filterParams.maxPrice = ''
      this.page = 1
      this.loadProducts()
    },
    
    clearYear() {
      this.filterParams.year = ''
      this.page = 1
      this.loadProducts()
    },
    
    clearSize() {
      this.filterParams.size = ''
      this.page = 1
      this.loadProducts()
    },
    
    clearArtist() {
      this.filterParams.artistId = ''
      this.filterParams.artistName = ''
      this.page = 1
      this.loadProducts()
    },
    
    clearArtistType() {
      this.filterParams.artistType = ''
      this.page = 1
      this.loadProducts()
    },
    
    clearAllFilters() {
      this.resetFilter()
      this.page = 1
      this.loadProducts()
    },
    
    resetFilter() {
      this.filterParams = {
        minPrice: '',
        maxPrice: '',
        year: '',
        size: '',
        holdTime: '',
        region: '',
        artistId: '',
        artistName: '',
        artistType: ''
      }
      this.tempMinPrice = ''
      this.tempMaxPrice = ''
      this.artistKeyword = ''
    },
    
    loadMore() {
      if (this.hasMore && !this.loading) {
        this.page++
        this.loadProducts()
      }
    },
    
    onRefresh() {
      this.page = 1
      this.loadProducts().then(() => {
        uni.showToast({ title: '刷新成功', icon: 'success' })
      })
    },
    
    goDetail(id) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
// 深色艺术主题色板
$bg-primary: #0d0d0d;
$bg-card: #1a1a1a;
$bg-elevated: #242424;
$text-primary: #ffffff;
$text-secondary: #a0a0a0;
$text-muted: #666666;
$accent-gold: #c9a227;
$accent-gold-light: #e6c65c;

.gallery-page {
  min-height: 100vh;
  background: $bg-primary;
  display: flex;
  flex-direction: column;
}

// 分类标签 - 紧凑胶囊式
.category-tabs {
  background: $bg-primary;
  position: sticky;
  top: 0;
  z-index: 99;
  padding: 16rpx 0;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
}

.category-scroll {
  white-space: nowrap;
  padding: 0 20rpx;
}

.category-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 14rpx 28rpx;
  margin: 0 8rpx;
  border-radius: 30rpx;
  background: $bg-card;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s;
  
  .category-name {
    font-size: 26rpx;
    color: $text-secondary;
    transition: all 0.3s;
  }
  
  .category-indicator {
    display: none;
  }
}

.category-item.active {
  background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
  border-color: transparent;
  box-shadow: 0 4rpx 16rpx rgba(201, 162, 39, 0.3);
  
  .category-name {
    font-size: 26rpx;
    font-weight: 600;
    color: $bg-primary;
  }
}

// 商品列表
.product-list {
  flex: 1;
  height: calc(100vh - 100rpx);
  padding: 16rpx;
  background: $bg-primary;
}

// 下拉刷新样式
:deep(.uni-scroll-view-refresher) {
  background: $bg-primary !important;
}

:deep(.uni-red-dot) {
  background: $accent-gold !important;
}

.waterfall-container {
  display: flex;
  justify-content: space-between;
}

.waterfall-column {
  width: 49%;
}

.product-card {
  background: $bg-card;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
  transition: all 0.3s;
  
  &:active {
    transform: scale(0.98);
    opacity: 0.9;
  }
}

.image-wrapper {
  position: relative;
  width: 100%;
}

.product-image {
  width: 100%;
  display: block;
  background: $bg-elevated;
}

// SOLD标签 - 设计图风格
.sold-badge {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  
  .sold-text {
    padding: 10rpx 36rpx;
    border: 2rpx solid rgba(255, 255, 255, 0.9);
    border-radius: 4rpx;
    font-size: 26rpx;
    color: #fff;
    font-weight: 600;
    letter-spacing: 6rpx;
  }
}

// 库存告急标签
.stock-badge {
  position: absolute;
  bottom: 16rpx;
  left: 16rpx;
  padding: 6rpx 14rpx;
  background: rgba(0, 0, 0, 0.7);
  border: 1rpx solid rgba(255, 255, 255, 0.2);
  border-radius: 4rpx;
  font-size: 20rpx;
  color: #fff;
}

.product-info {
  padding: 18rpx;
}

.product-title {
  font-size: 26rpx;
  color: $text-primary;
  font-weight: 500;
  margin-bottom: 10rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  display: flex;
  align-items: center;
  font-size: 22rpx;
  color: $text-muted;
  margin-bottom: 8rpx;
  
  .artist-name {
    color: $text-secondary;
    max-width: 100rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .category-name {
    max-width: 70rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.meta-sep {
  margin: 0 10rpx;
  color: rgba(255, 255, 255, 0.15);
}

.product-size {
  display: flex;
  align-items: center;
  gap: 6rpx;
  font-size: 20rpx;
  color: $text-muted;
  margin-bottom: 12rpx;
}

.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price-info {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
}

.current-price {
  font-size: 28rpx;
  color: $accent-gold;
  font-weight: 600;
  
  &::before {
    content: '¥';
    font-size: 20rpx;
    font-weight: 500;
  }
}

.original-price {
  font-size: 20rpx;
  color: $text-muted;
  text-decoration: line-through;
  margin-left: 8rpx;
  
  &::before {
    content: '¥';
    font-size: 16rpx;
  }
}

.price-change {
  display: flex;
  align-items: center;
  gap: 4rpx;
  background: rgba(231, 76, 60, 0.15);
  padding: 4rpx 10rpx;
  border-radius: 4rpx;
  font-size: 18rpx;
  color: #e74c3c;
}

// 加载状态
.load-more, .no-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40rpx 0;
  font-size: 24rpx;
  color: $text-muted;
}

.load-more text {
  margin-left: 12rpx;
}

.no-more {
  flex-direction: column;
  gap: 16rpx;
  
  .divider-line {
    width: 100rpx;
    height: 1rpx;
    background: rgba(255, 255, 255, 0.1);
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
  
  .empty-image {
    width: 240rpx;
    height: 240rpx;
    margin-bottom: 32rpx;
    opacity: 0.5;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: $text-muted;
    margin-bottom: 32rpx;
  }
  
  .empty-btn {
    padding: 14rpx 44rpx;
    background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
    border-radius: 36rpx;
    font-size: 28rpx;
    color: $bg-primary;
    font-weight: 500;
  }
}

// 筛选弹窗 - 深色主题
.filter-popup {
  padding: 32rpx;
  background: $bg-card;
}

.popup-header {
  margin-bottom: 32rpx;
  
  .popup-title {
    font-size: 32rpx;
    font-weight: 600;
    color: $text-primary;
  }
}

.filter-content {
  margin-bottom: 32rpx;
}

.filter-section {
  margin-bottom: 40rpx;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.filter-title {
  font-size: 26rpx;
  font-weight: 500;
  color: $text-secondary;
  margin-bottom: 20rpx;
}

.filter-options-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.filter-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 24rpx;
  background: $bg-elevated;
  border-radius: 10rpx;
  font-size: 26rpx;
  color: $text-secondary;
  border: 1rpx solid transparent;
  transition: all 0.3s;
  
  &.active {
    background: rgba(201, 162, 39, 0.12);
    border-color: rgba(201, 162, 39, 0.4);
    color: $accent-gold;
    font-weight: 500;
  }
}

.price-range {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}

.price-input {
  flex: 1;
  height: 84rpx;
  background: $bg-elevated;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 10rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
  text-align: center;
  color: $text-primary;
}

.range-sep {
  margin: 0 16rpx;
  color: $text-muted;
  font-size: 26rpx;
}

.quick-price {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.quick-item {
  padding: 18rpx;
  background: $bg-elevated;
  border-radius: 10rpx;
  font-size: 24rpx;
  color: $text-secondary;
  text-align: center;
  border: 1rpx solid transparent;
  transition: all 0.3s;
  
  &.active {
    background: rgba(201, 162, 39, 0.12);
    border-color: rgba(201, 162, 39, 0.4);
    color: $accent-gold;
  }
}

// 艺术家搜索
.artist-search {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.artist-input {
  flex: 1;
  height: 80rpx;
  background: $bg-elevated;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 10rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
  color: $text-primary;
}

.search-btn {
  width: 100rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
  border-radius: 10rpx;
}

.artist-list {
  margin-top: 20rpx;
}

.artist-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.artist-chip {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 14rpx 20rpx;
  background: $bg-elevated;
  border-radius: 32rpx;
  font-size: 24rpx;
  color: $text-secondary;
  border: 1rpx solid transparent;
  transition: all 0.3s;
  
  &.active {
    background: rgba(201, 162, 39, 0.12);
    border-color: rgba(201, 162, 39, 0.4);
    color: $accent-gold;
  }
  
  .artist-avatar {
    width: 40rpx;
    height: 40rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
  }
  
  .artist-name {
    max-width: 100rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .artist-badge {
    padding: 3rpx 10rpx;
    background: rgba(201, 162, 39, 0.15);
    color: $accent-gold;
    border-radius: 4rpx;
    font-size: 18rpx;
  }
}

// 隐藏页面底部意外出现的筛选文字
.gallery-page::after {
  content: none !important;
}

// 隐藏任何 icon-filter 伪元素
.icon-filter::before {
  content: '' !important;
}
</style>

<style>
.loading-spinner {
  width: 40rpx;
  height: 40rpx;
  border: 3rpx solid #f0f0f0;
  border-top-color: #c9a227;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
