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
          {{ item.name }}
        </view>
      </scroll-view>
    </view>

    <!-- 筛选栏 -->
    <view class="filter-bar">
      <view class="filter-item" @click="showFilter('sort')">
        <text>{{ currentSort.label }}</text>
        <u-icon name="arrow-down" size="14" color="#666"></u-icon>
      </view>
      <view class="filter-item" @click="showFilter('price')">
        <text>价格</text>
        <u-icon name="arrow-down" size="14" color="#666"></u-icon>
      </view>
      <view class="filter-item" @click="showFilter('year')">
        <text>年代</text>
        <u-icon name="arrow-down" size="14" color="#666"></u-icon>
      </view>
      <view class="filter-item" @click="showFilter('size')">
        <text>尺寸</text>
        <u-icon name="arrow-down" size="14" color="#666"></u-icon>
      </view>
      <view class="filter-item" @click="showFilter('more')">
        <text>更多</text>
        <u-icon name="arrow-down" size="14" color="#666"></u-icon>
      </view>
    </view>

    <!-- 商品列表 -->
    <scroll-view class="product-list" scroll-y @scrolltolower="loadMore">
      <view class="waterfall-container">
        <view class="waterfall-column">
          <view class="product-card" v-for="item in leftList" :key="item.id" @click="goDetail(item.id)">
            <image class="product-image" :src="item.cover" mode="widthFix"></image>
            <view class="product-info">
              <view class="product-title">{{ item.title }}</view>
              <view class="product-meta">
                <text>{{ item.artistName }}</text>
                <text class="meta-sep">|</text>
                <text>{{ item.category }}</text>
              </view>
              <view class="product-size">{{ item.size }}</view>
              <view class="product-footer">
                <view class="price-info">
                  <text class="current-price">¥{{ item.price }}</text>
                  <text class="original-price" v-if="item.originalPrice">¥{{ item.originalPrice }}</text>
                </view>
                <view class="price-change" v-if="item.priceChange > 0">
                  <u-icon name="arrow-up" size="10" color="#e74c3c"></u-icon>
                  <text>{{ item.priceChange }}%</text>
                </view>
              </view>
            </view>
          </view>
        </view>
        <view class="waterfall-column">
          <view class="product-card" v-for="item in rightList" :key="item.id" @click="goDetail(item.id)">
            <image class="product-image" :src="item.cover" mode="widthFix"></image>
            <view class="product-info">
              <view class="product-title">{{ item.title }}</view>
              <view class="product-meta">
                <text>{{ item.artistName }}</text>
                <text class="meta-sep">|</text>
                <text>{{ item.category }}</text>
              </view>
              <view class="product-size">{{ item.size }}</view>
              <view class="product-footer">
                <view class="price-info">
                  <text class="current-price">¥{{ item.price }}</text>
                  <text class="original-price" v-if="item.originalPrice">¥{{ item.originalPrice }}</text>
                </view>
                <view class="price-change" v-if="item.priceChange > 0">
                  <u-icon name="arrow-up" size="10" color="#e74c3c"></u-icon>
                  <text>{{ item.priceChange }}%</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
      
      <view class="load-more" v-if="hasMore">
        <u-loading mode="circle"></u-loading>
        <text>加载中...</text>
      </view>
      <view class="no-more" v-else-if="productList.length > 0">
        <text>没有更多了</text>
      </view>
    </scroll-view>

    <!-- 筛选弹出层 -->
    <u-popup v-model:show="showFilterPopup" mode="top" :round="20">
      <view class="filter-popup">
        <view class="filter-content" v-if="filterType === 'sort'">
          <view class="filter-title">排序方式</view>
          <view class="filter-options">
            <view 
              class="filter-option" 
              :class="{ active: filterParams.sort === item.value }"
              v-for="item in sortOptions" 
              :key="item.value"
              @click="selectSort(item)"
            >
              {{ item.label }}
            </view>
          </view>
        </view>
        
        <view class="filter-content" v-if="filterType === 'price'">
          <view class="filter-title">价格区间</view>
          <view class="price-range">
            <input class="price-input" type="number" v-model="filterParams.minPrice" placeholder="最低价" />
            <text class="range-sep">-</text>
            <input class="price-input" type="number" v-model="filterParams.maxPrice" placeholder="最高价" />
          </view>
          <view class="quick-price">
            <view class="quick-item" @click="setPriceRange(0, 10000)">1万以下</view>
            <view class="quick-item" @click="setPriceRange(10000, 50000)">1-5万</view>
            <view class="quick-item" @click="setPriceRange(50000, 100000)">5-10万</view>
            <view class="quick-item" @click="setPriceRange(100000, 0)">10万以上</view>
          </view>
        </view>
        
        <view class="filter-content" v-if="filterType === 'year'">
          <view class="filter-title">创作年代</view>
          <view class="filter-options">
            <view 
              class="filter-option" 
              :class="{ active: filterParams.year === item.value }"
              v-for="item in yearOptions" 
              :key="item.value"
              @click="selectYear(item)"
            >
              {{ item.label }}
            </view>
          </view>
        </view>
        
        <view class="filter-content" v-if="filterType === 'size'">
          <view class="filter-title">尺寸</view>
          <view class="filter-options">
            <view 
              class="filter-option" 
              :class="{ active: filterParams.size === item.value }"
              v-for="item in sizeOptions" 
              :key="item.value"
              @click="selectSize(item)"
            >
              {{ item.label }}
            </view>
          </view>
        </view>
        
        <view class="filter-actions">
          <view class="btn-reset" @click="resetFilter">重置</view>
          <view class="btn-confirm" @click="confirmFilter">确定</view>
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script>
import { getGalleryList, getCategories } from '@/api/product.js'

export default {
  data() {
    return {
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
      sortOptions: [
        { label: '综合排序', value: 'default' },
        { label: '最新上架', value: 'time_desc' },
        { label: '价格从低到高', value: 'price_asc' },
        { label: '价格从高到低', value: 'price_desc' }
      ],
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
      currentSort: { label: '综合排序', value: 'default' },
      productList: [],
      page: 1,
      pageSize: 10,
      hasMore: true,
      showFilterPopup: false,
      filterType: 'sort',
      filterParams: {
        sort: 'default',
        minPrice: '',
        maxPrice: '',
        year: '',
        size: '',
        holdTime: '',
        region: ''
      }
    }
  },
  
  computed: {
    leftList() {
      return this.productList.filter((item, index) => index % 2 === 0)
    },
    rightList() {
      return this.productList.filter((item, index) => index % 2 === 1)
    }
  },
  
  onLoad() {
    this.loadCategories()
    this.loadProducts()
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
        uni.showLoading({ title: '加载中...' })
        const res = await getGalleryList({
          category: this.currentCategory,
          page: this.page,
          pageSize: this.pageSize,
          ...this.filterParams
        })
        
        if (this.page === 1) {
          this.productList = res.list || []
        } else {
          this.productList = [...this.productList, ...(res.list || [])]
        }
        
        this.hasMore = res.list?.length >= this.pageSize
        uni.hideLoading()
      } catch (e) {
        uni.hideLoading()
        this.loadMockData()
      }
    },
    
    loadMockData() {
      const mockData = [
        { id: 1, cover: '/static/product/demo1.jpg', title: '山水长卷', artistName: '张大千', category: '油画', size: '100x200cm', price: 128000, originalPrice: 150000, priceChange: 5.2 },
        { id: 2, cover: '/static/product/demo2.jpg', title: '虾趣图', artistName: '齐白石', category: '水墨', size: '50x80cm', price: 88000, originalPrice: 100000, priceChange: 3.8 },
        { id: 3, cover: '/static/product/demo3.jpg', title: '奔马图', artistName: '徐悲鸿', category: '油画', size: '120x80cm', price: 256000, priceChange: 8.5 },
        { id: 4, cover: '/static/product/demo4.jpg', title: '松鹰图', artistName: '潘天寿', category: '水墨', size: '80x150cm', price: 158000, originalPrice: 180000, priceChange: 2.1 }
      ]
      
      if (this.page === 1) {
        this.productList = mockData
      } else {
        this.productList = [...this.productList, ...mockData]
      }
      this.hasMore = false
    },
    
    selectCategory(id) {
      this.currentCategory = id
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    
    showFilter(type) {
      this.filterType = type
      this.showFilterPopup = true
    },
    
    selectSort(item) {
      this.currentSort = item
      this.filterParams.sort = item.value
    },
    
    selectYear(item) {
      this.filterParams.year = item.value
    },
    
    selectSize(item) {
      this.filterParams.size = item.value
    },
    
    setPriceRange(min, max) {
      this.filterParams.minPrice = min || ''
      this.filterParams.maxPrice = max || ''
    },
    
    resetFilter() {
      this.filterParams = {
        sort: 'default',
        minPrice: '',
        maxPrice: '',
        year: '',
        size: '',
        holdTime: '',
        region: ''
      }
      this.currentSort = { label: '综合排序', value: 'default' }
    },
    
    confirmFilter() {
      this.showFilterPopup = false
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    
    loadMore() {
      if (this.hasMore) {
        this.page++
        this.loadProducts()
      }
    },
    
    goDetail(id) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.gallery-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.category-tabs {
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 99;
}

.category-scroll {
  white-space: nowrap;
  padding: 0 20rpx;
}

.category-item {
  display: inline-block;
  padding: 24rpx 30rpx;
  font-size: 28rpx;
  color: #666;
  position: relative;
}

.category-item.active {
  color: #333;
  font-weight: 600;
}

.category-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40rpx;
  height: 4rpx;
  background: #333;
  border-radius: 2rpx;
}

.filter-bar {
  display: flex;
  background: #fff;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #eee;
}

.filter-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  color: #666;
}

.product-list {
  height: calc(100vh - 200rpx);
  padding: 20rpx;
}

.waterfall-container {
  display: flex;
  justify-content: space-between;
}

.waterfall-column {
  width: 49%;
}

.product-card {
  background: #fff;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.product-image {
  width: 100%;
  display: block;
}

.product-info {
  padding: 20rpx;
}

.product-title {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 10rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.meta-sep {
  margin: 0 10rpx;
}

.product-size {
  font-size: 22rpx;
  color: #999;
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
}

.current-price {
  font-size: 30rpx;
  color: #e74c3c;
  font-weight: 600;
}

.original-price {
  font-size: 22rpx;
  color: #999;
  text-decoration: line-through;
  margin-left: 10rpx;
}

.price-change {
  display: flex;
  align-items: center;
  background: rgba(231, 76, 60, 0.1);
  padding: 4rpx 10rpx;
  border-radius: 4rpx;
  font-size: 20rpx;
  color: #e74c3c;
}

.load-more, .no-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 30rpx 0;
  font-size: 24rpx;
  color: #999;
}

.load-more text {
  margin-left: 12rpx;
}

.filter-popup {
  padding: 30rpx;
}

.filter-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 30rpx;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.filter-option {
  padding: 16rpx 30rpx;
  background: #f5f5f5;
  border-radius: 8rpx;
  font-size: 26rpx;
  color: #666;
}

.filter-option.active {
  background: #333;
  color: #fff;
}

.price-range {
  display: flex;
  align-items: center;
  margin-bottom: 30rpx;
}

.price-input {
  flex: 1;
  height: 80rpx;
  background: #f5f5f5;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
}

.range-sep {
  margin: 0 20rpx;
  color: #999;
}

.quick-price {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.quick-item {
  padding: 16rpx 30rpx;
  background: #f5f5f5;
  border-radius: 8rpx;
  font-size: 24rpx;
  color: #666;
}

.filter-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 40rpx;
}

.btn-reset, .btn-confirm {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 44rpx;
  font-size: 30rpx;
}

.btn-reset {
  background: #f5f5f5;
  color: #666;
}

.btn-confirm {
  background: #333;
  color: #fff;
}
</style>
