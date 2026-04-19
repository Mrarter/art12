<template>
  <view class="gallery-page">
    <!-- 顶部筛选栏 -->
    <view class="filter-bar">
      <view class="category-tabs">
        <scroll-view scroll-x class="category-scroll">
          <view 
            class="category-tab" 
            :class="{ active: currentCategory === null }"
            @click="switchCategory(null)"
          >
            全部
          </view>
          <view 
            v-for="cat in categories" 
            :key="cat.id"
            class="category-tab"
            :class="{ active: currentCategory === cat.id }"
            @click="switchCategory(cat.id)"
          >
            {{ cat.name }}
          </view>
        </scroll-view>
      </view>
      <view class="filter-actions">
        <view class="filter-btn" @click="showFilter = true">
          <text>筛选</text>
          <text class="icon">▼</text>
        </view>
      </view>
    </view>

    <!-- 作品列表 -->
    <scroll-view 
      scroll-y 
      class="artwork-list"
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <view class="waterfall">
        <view class="waterfall-column left">
          <view 
            v-for="(item, index) in leftArtworks" 
            :key="item.id"
            class="artwork-card"
            @click="goDetail(item.id)"
          >
            <image :src="item.coverImage" mode="widthFix" lazy-load />
            <view class="card-info">
              <view class="card-title">{{ item.title }}</view>
              <view class="card-meta">
                <image class="meta-avatar" :src="item.authorAvatar || '/static/default-avatar.png'" />
                <text class="meta-name">{{ item.authorName }}</text>
              </view>
              <view class="card-footer">
                <text class="card-price">¥{{ formatPrice(item.price) }}</text>
                <view v-if="item.priceRise > 0" class="price-rise">+{{ item.priceRise }}%</view>
              </view>
            </view>
          </view>
        </view>
        <view class="waterfall-column right">
          <view 
            v-for="(item, index) in rightArtworks" 
            :key="item.id"
            class="artwork-card"
            @click="goDetail(item.id)"
          >
            <image :src="item.coverImage" mode="widthFix" lazy-load />
            <view class="card-info">
              <view class="card-title">{{ item.title }}</view>
              <view class="card-meta">
                <image class="meta-avatar" :src="item.authorAvatar || '/static/default-avatar.png'" />
                <text class="meta-name">{{ item.authorName }}</text>
              </view>
              <view class="card-footer">
                <text class="card-price">¥{{ formatPrice(item.price) }}</text>
                <view v-if="item.priceRise > 0" class="price-rise">+{{ item.priceRise }}%</view>
              </view>
            </view>
          </view>
        </view>
      </view>
      
      <view class="loading-more" v-if="isLoading">
        <text>加载中...</text>
      </view>
      <view class="no-more" v-if="noMore && artworks.length > 0">
        <text>没有更多了</text>
      </view>
    </scroll-view>

    <!-- 筛选弹窗 -->
    <uni-popup ref="filterPopup" type="bottom">
      <view class="filter-popup">
        <view class="popup-header">
          <text>筛选</text>
          <text @click="resetFilter">重置</text>
        </view>
        <view class="popup-body">
          <view class="filter-section">
            <view class="section-title">价格区间</view>
            <view class="price-inputs">
              <input type="number" v-model="filter.minPrice" placeholder="最低价" />
              <text>-</text>
              <input type="number" v-model="filter.maxPrice" placeholder="最高价" />
            </view>
          </view>
          <view class="filter-section">
            <view class="section-title">排序方式</view>
            <view class="sort-options">
              <view 
                v-for="opt in sortOptions" 
                :key="opt.value"
                class="sort-option"
                :class="{ active: filter.sortBy === opt.value }"
                @click="filter.sortBy = opt.value"
              >
                {{ opt.label }}
              </view>
            </view>
          </view>
        </view>
        <view class="popup-footer">
          <button @click="applyFilter">确定</button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getCategoryList, getArtworkList } from '@/api/product'

const categories = ref([])
const artworks = ref([])
const currentCategory = ref(null)
const isRefreshing = ref(false)
const isLoading = ref(false)
const noMore = ref(false)
const page = ref(1)
const filterPopup = ref(null)
const showFilter = ref(false)

const filter = ref({
  minPrice: '',
  maxPrice: '',
  sortBy: 'createTime',
  sortOrder: 'desc'
})

const sortOptions = [
  { label: '综合', value: 'createTime' },
  { label: '价格最低', value: 'price_asc' },
  { label: '价格最高', value: 'price_desc' },
  { label: '销量优先', value: 'saleCount' }
]

const leftArtworks = computed(() => artworks.value.filter((_, i) => i % 2 === 0))
const rightArtworks = computed(() => artworks.value.filter((_, i) => i % 2 === 1))

onMounted(() => {
  loadCategories()
  loadArtworks()
})

async function loadCategories() {
  try {
    const res = await getCategoryList()
    categories.value = res || []
  } catch (e) {
    categories.value = [
      { id: 1, name: '油画' },
      { id: 2, name: '雕塑' },
      { id: 3, name: '水墨' },
      { id: 4, name: '版画' },
      { id: 5, name: '装置' },
      { id: 6, name: '摄影' }
    ]
  }
}

async function loadArtworks() {
  try {
    const params = {
      page: page.value,
      pageSize: 20,
      categoryId: currentCategory.value
    }
    if (filter.value.minPrice) params.minPrice = filter.value.minPrice
    if (filter.value.maxPrice) params.maxPrice = filter.value.maxPrice
    if (filter.value.sortBy === 'price_asc') {
      params.sortBy = 'price'
      params.sortOrder = 'asc'
    } else if (filter.value.sortBy === 'price_desc') {
      params.sortBy = 'price'
      params.sortOrder = 'desc'
    } else {
      params.sortBy = filter.value.sortBy
    }
    
    const res = await getArtworkList(params)
    if (page.value === 1) {
      artworks.value = res.records || []
    } else {
      artworks.value.push(...(res.records || []))
    }
    
    noMore.value = artworks.value.length >= res.total
  } catch (e) {
    // 使用模拟数据
    if (page.value === 1) {
      artworks.value = generateMockData()
    }
  }
}

function generateMockData() {
  const data = []
  for (let i = 0; i < 20; i++) {
    data.push({
      id: i + 1,
      title: `《作品${i + 1}》`,
      coverImage: `https://picsum.photos/350/${300 + Math.floor(Math.random() * 150)}?random=${i}`,
      authorName: ['罗中立', '张大千', '陈丹青'][i % 3],
      authorAvatar: 'https://picsum.photos/50/50?random=' + (i + 20),
      price: 5000 + Math.floor(Math.random() * 50000),
      priceRise: Math.random() > 0.7 ? Math.floor(Math.random() * 10) : 0
    })
  }
  return data
}

function switchCategory(id) {
  currentCategory.value = id
  page.value = 1
  noMore.value = false
  loadArtworks()
}

async function onRefresh() {
  isRefreshing.value = true
  page.value = 1
  noMore.value = false
  await loadArtworks()
  isRefreshing.value = false
}

async function loadMore() {
  if (isLoading.value || noMore.value) return
  isLoading.value = true
  page.value++
  await loadArtworks()
  isLoading.value = false
}

function showFilterPopup() {
  filterPopup.value.open()
}

function resetFilter() {
  filter.value = {
    minPrice: '',
    maxPrice: '',
    sortBy: 'createTime',
    sortOrder: 'desc'
  }
}

function applyFilter() {
  page.value = 1
  noMore.value = false
  loadArtworks()
  filterPopup.value.close()
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/artwork/detail?id=${id}` })
}

function formatPrice(price) {
  if (price >= 10000) {
    return (price / 10000).toFixed(1) + '万'
  }
  return price.toLocaleString()
}
</script>

<style lang="scss" scoped>
.gallery-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.filter-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
}

.category-scroll {
  white-space: nowrap;
  padding: 0 24rpx;
}

.category-tab {
  display: inline-block;
  padding: 24rpx 32rpx;
  font-size: 28rpx;
  color: #666;
  &.active {
    color: #333;
    font-weight: 600;
    border-bottom: 4rpx solid #333;
  }
}

.filter-actions {
  padding: 0 24rpx 16rpx;
  border-bottom: 1rpx solid #eee;
}

.filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  font-size: 26rpx;
  color: #666;
}

.artwork-list {
  height: calc(100vh - 100rpx);
  padding: 16rpx;
}

.waterfall {
  display: flex;
  gap: 16rpx;
}

.waterfall-column {
  flex: 1;
}

.artwork-card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
  
  image {
    width: 100%;
    display: block;
  }
  
  .card-info {
    padding: 20rpx;
  }
  
  .card-title {
    font-size: 28rpx;
    color: #333;
    margin-bottom: 12rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .card-meta {
    display: flex;
    align-items: center;
    gap: 8rpx;
    margin-bottom: 12rpx;
    
    .meta-avatar {
      width: 36rpx;
      height: 36rpx;
      border-radius: 50%;
    }
    
    .meta-name {
      font-size: 22rpx;
      color: #999;
    }
  }
  
  .card-footer {
    display: flex;
    align-items: center;
    gap: 12rpx;
    
    .card-price {
      font-size: 32rpx;
      font-weight: 600;
      color: #333;
    }
    
    .price-rise {
      font-size: 20rpx;
      color: #e53935;
      background: rgba(229, 57, 53, 0.1);
      padding: 2rpx 8rpx;
      border-radius: 4rpx;
    }
  }
}

.loading-more, .no-more {
  text-align: center;
  padding: 32rpx;
  color: #999;
  font-size: 24rpx;
}

.filter-popup {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
  
  .popup-header {
    display: flex;
    justify-content: space-between;
    padding: 32rpx;
    border-bottom: 1rpx solid #eee;
    font-size: 32rpx;
    font-weight: 600;
  }
  
  .popup-body {
    padding: 32rpx;
  }
  
  .filter-section {
    margin-bottom: 32rpx;
  }
  
  .section-title {
    font-size: 28rpx;
    color: #333;
    margin-bottom: 16rpx;
  }
  
  .price-inputs {
    display: flex;
    align-items: center;
    gap: 16rpx;
    
    input {
      flex: 1;
      height: 72rpx;
      background: #f5f5f5;
      border-radius: 8rpx;
      padding: 0 24rpx;
    }
  }
  
  .sort-options {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
  }
  
  .sort-option {
    padding: 16rpx 32rpx;
    background: #f5f5f5;
    border-radius: 8rpx;
    font-size: 26rpx;
    
    &.active {
      background: #333;
      color: #fff;
    }
  }
  
  .popup-footer {
    padding: 32rpx;
    
    button {
      width: 100%;
      height: 88rpx;
      background: #333;
      color: #fff;
      border-radius: 44rpx;
      font-size: 30rpx;
    }
  }
}
</style>
