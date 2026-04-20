<template>
  <view class="index-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar">
      <view class="search-box" @click="goSearch">
        <text class="placeholder">搜索作品/艺术家</text>
      </view>
      <view class="message-icon" @click="goMessage">
        <view class="badge-dot" v-if="hasMessage"></view>
      </view>
    </view>
    
    <!-- 内容区域 -->
    <scroll-view 
      class="content" 
      scroll-y 
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <!-- Banner轮播 -->
      <swiper class="banner-swiper" indicator-dots autoplay circular>
        <swiper-item v-for="(banner, index) in banners" :key="index">
          <image class="banner-image" :src="banner.imageUrl" mode="aspectFill" @click="onBannerClick(banner)"></image>
        </swiper-item>
      </swiper>
      
      <!-- 金刚区 -->
      <view class="nav-grid">
        <view class="nav-item" v-for="item in navItems" :key="item.id" @click="goNav(item)">
          <image class="nav-icon" :src="item.icon" mode="aspectFit"></image>
          <text class="nav-text">{{ item.text }}</text>
        </view>
      </view>
      
      <!-- Tab切换 -->
      <view class="content-tabs">
        <view 
          class="tab-item" 
          :class="{ active: currentTab === 'recommend' }"
          @click="switchTab('recommend')"
        >
          推荐
        </view>
        <view 
          class="tab-item"
          :class="{ active: currentTab === 'following' }"
          @click="switchTab('following')"
        >
          关注
        </view>
      </view>
      
      <!-- 作品瀑布流 -->
      <view class="waterfall">
        <view 
          class="waterfall-item" 
          v-for="(item, index) in productList" 
          :key="item.id"
          @click="goDetail(item)"
        >
          <view class="artwork-card">
            <image class="artwork-card-image" :src="item.coverImage" mode="aspectFill"></image>
            <view class="artwork-card-info">
              <view class="artwork-info-row">
                <text class="artwork-title">{{ item.authorName }} · {{ item.title }}</text>
              </view>
              <view class="artwork-meta">
                <text>{{ item.artType }}</text>
                <text v-if="item.size">{{ item.size }}</text>
                <text v-if="item.createYear">{{ item.createYear }}年</text>
              </view>
              <view class="artwork-footer">
                <view class="author-info" @click.stop="goArtistHome(item.authorId)">
                  <image class="author-avatar" :src="item.authorAvatar || '/static/images/avatar.png'"></image>
                  <text class="author-name">{{ item.authorName }}</text>
                  <view class="identity-tag" :class="'identity-' + item.authorIdentity">
                    {{ getIdentityText(item.authorIdentity) }}
                  </view>
                </view>
                <view class="price-info">
                  <text class="price">¥{{ formatPrice(item.price) }}</text>
                  <text class="price-tag" v-if="item.priceChangeRate > 0">+{{ item.priceChangeRate }}%</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 加载更多 -->
      <view class="load-more" v-if="productList.length > 0">
        <text v-if="loading">加载中...</text>
        <text v-else-if="noMore">没有更多了</text>
        <text v-else>上滑加载更多</text>
      </view>
      
      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && productList.length === 0">
        <image class="empty-image" src="/static/images/empty.png" mode="aspectFit"></image>
        <text class="empty-text">暂无作品</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBanners, getRecommend, getFollowingWorks } from '@/api/product'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

// 状态
const refreshing = ref(false)
const loading = ref(false)
const noMore = ref(false)
const currentTab = ref('recommend')
const banners = ref([])
const productList = ref([])
const hasMessage = ref(false)
const page = ref(1)
const pageSize = 10

// 金刚区配置
const navItems = [
  { id: 1, text: '画廊', icon: '/static/icons/gallery.png', path: '/pages/gallery/index' },
  { id: 2, text: '拍卖', icon: '/static/icons/auction.png', path: '/pages/auction/index' },
  { id: 3, text: '鉴赏', icon: '/static/icons/appreciate.png', path: '/pages/artcircle/index' },
  { id: 4, text: '艺术圈', icon: '/static/icons/circle.png', path: '/pages/artcircle/index' }
]

// 获取Banner
const fetchBanners = async () => {
  try {
    const list = await getBanners()
    banners.value = list || []
    // 如果没有数据，使用 mock 数据
    if (banners.value.length === 0) {
      banners.value = [
        { id: 1, imageUrl: 'https://picsum.photos/750/320?random=1', linkType: 'gallery', linkValue: '1', title: '首页Banner 1' },
        { id: 2, imageUrl: 'https://picsum.photos/750/320?random=2', linkType: 'gallery', linkValue: '2', title: '首页Banner 2' },
        { id: 3, imageUrl: 'https://picsum.photos/750/320?random=3', linkType: 'gallery', linkValue: '3', title: '首页Banner 3' }
      ]
    }
  } catch (e) {
    // 请求失败时使用 mock 数据
    banners.value = [
      { id: 1, imageUrl: 'https://picsum.photos/750/320?random=1', linkType: 'gallery', linkValue: '1', title: '首页Banner 1' },
      { id: 2, imageUrl: 'https://picsum.photos/750/320?random=2', linkType: 'gallery', linkValue: '2', title: '首页Banner 2' },
      { id: 3, imageUrl: 'https://picsum.photos/750/320?random=3', linkType: 'gallery', linkValue: '3', title: '首页Banner 3' }
    ]
  }
}

// 获取作品列表
const fetchProductList = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    noMore.value = false
  }

  loading.value = true
  try {
    const params = { page: page.value, pageSize }
    let list = []

    if (currentTab.value === 'recommend') {
      list = await getRecommend(params)
    } else {
      list = await getFollowingWorks(params)
    }

    // 如果没有数据，使用 mock 数据
    if (!list || list.length === 0) {
      list = getMockArtworks()
    }

    if (reset) {
      productList.value = list || []
    } else {
      productList.value = [...productList.value, ...(list || [])]
    }

    if (list && list.length < pageSize) {
      noMore.value = true
    } else {
      page.value++
    }
  } catch (e) {
    // 请求失败时使用 mock 数据
    if (reset) {
      productList.value = getMockArtworks()
    }
  } finally {
    loading.value = false
  }
}

// Mock 数据
const getMockArtworks = () => {
  return [
    { id: 1, title: '山水意境', authorName: '张大千', authorId: 1, authorAvatar: 'https://picsum.photos/100/100?random=10', coverImage: 'https://picsum.photos/400/500?random=1', artType: '国画', size: '68x68cm', createYear: 2023, price: 128000, priceChangeRate: 5.2, authorIdentity: 'artist' },
    { id: 2, title: '晨曦', authorName: '李娜', authorId: 2, authorAvatar: 'https://picsum.photos/100/100?random=11', coverImage: 'https://picsum.photos/400/400?random=2', artType: '油画', size: '80x100cm', createYear: 2024, price: 88000, priceChangeRate: 3.8, authorIdentity: 'artist' },
    { id: 3, title: '书法对联', authorName: '王羲之', authorId: 3, authorAvatar: 'https://picsum.photos/100/100?random=12', coverImage: 'https://picsum.photos/400/600?random=3', artType: '书法', size: '138x35cmx2', createYear: 2023, price: 58000, priceChangeRate: 2.1, authorIdentity: 'master' },
    { id: 4, title: '静物写生', authorName: '莫奈', authorId: 4, authorAvatar: 'https://picsum.photos/100/100?random=13', coverImage: 'https://picsum.photos/400/500?random=4', artType: '油画', size: '65x81cm', createYear: 2022, price: 256000, priceChangeRate: 8.5, authorIdentity: 'artist' },
    { id: 5, title: '抽象系列', authorName: '赵无极', authorId: 5, authorAvatar: 'https://picsum.photos/100/100?random=14', coverImage: 'https://picsum.photos/400/400?random=5', artType: '油画', size: '97x130cm', createYear: 2024, price: 520000, priceChangeRate: 12.3, authorIdentity: 'artist' },
    { id: 6, title: '青花瓷韵', authorName: '景德镇', authorId: 6, authorAvatar: 'https://picsum.photos/100/100?random=15', coverImage: 'https://picsum.photos/400/500?random=6', artType: '瓷板画', size: '直径50cm', createYear: 2023, price: 36000, priceChangeRate: 1.5, authorIdentity: '' }
  ]
}

// 刷新
const onRefresh = async () => {
  refreshing.value = true
  await fetchProductList(true)
  refreshing.value = false
}

// 加载更多
const loadMore = () => {
  if (!noMore.value) {
    fetchProductList()
  }
}

// Tab切换
const switchTab = (tab) => {
  currentTab.value = tab
  fetchProductList(true)
}

// 跳转搜索
const goSearch = () => {
  uni.navigateTo({ url: '/pages/gallery/index?type=search' })
}

// 跳转消息
const goMessage = () => {
  uni.navigateTo({ url: '/pages/user/messages' })
}

// 金刚区导航
const goNav = (item) => {
  uni.switchTab({ url: item.path })
}

// Banner点击
const onBannerClick = (banner) => {
  if (!banner.linkValue) return
  switch (banner.linkType) {
    case 'gallery':
      uni.navigateTo({ url: `/pages/gallery/detail?id=${banner.linkValue}` })
      break
    case 'auction':
      uni.navigateTo({ url: `/pages/auction/session?id=${banner.linkValue}` })
      break
    case 'artist':
      uni.navigateTo({ url: `/pages/artist/home?userId=${banner.linkValue}` })
      break
  }
}

// 跳转作品详情
const goDetail = (item) => {
  uni.navigateTo({ url: `/pages/gallery/detail?id=${item.id}` })
}

// 跳转艺术家主页
const goArtistHome = (userId) => {
  uni.navigateTo({ url: `/pages/artist/home?userId=${userId}` })
}

// 格式化价格
const formatPrice = (price) => {
  if (!price) return '0'
  if (price >= 10000) {
    return (price / 10000).toFixed(1) + '万'
  }
  return price.toLocaleString()
}

// 获取身份文字
const getIdentityText = (identity) => {
  const map = {
    artist: '艺术家',
    agent: '经纪人',
    promoter: '艺荐官',
    collector: '持有者'
  }
  return map[identity] || ''
}

// 初始化
onMounted(() => {
  fetchBanners()
  fetchProductList(true)
})
</script>

<style lang="scss" scoped>
.index-page {
  min-height: 100vh;
  background-color: #f8f8f8;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  padding-top: calc(20rpx + env(safe-area-inset-top));
  background-color: #ffffff;
  
  .search-box {
    flex: 1;
    display: flex;
    align-items: center;
    height: 64rpx;
    padding: 0 24rpx;
    background-color: #f5f5f5;
    border-radius: 32rpx;
    
    .iconfont {
      font-size: 28rpx;
      color: #999999;
      margin-right: 12rpx;
    }
    
    .placeholder {
      font-size: 26rpx;
      color: #999999;
    }
  }
  
  .message-icon {
    position: relative;
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-left: 20rpx;
    
    .iconfont {
      font-size: 40rpx;
      color: #333333;
    }
    
    .badge-dot {
      position: absolute;
      top: 8rpx;
      right: 8rpx;
      width: 16rpx;
      height: 16rpx;
      background-color: #ff4d4f;
      border-radius: 50%;
    }
  }
}

.content {
  padding-top: 100rpx;
  height: calc(100vh - 100rpx);
}

.banner-swiper {
  height: 320rpx;
  margin: 20rpx;
  border-radius: 16rpx;
  overflow: hidden;
  
  .banner-image {
    width: 100%;
    height: 100%;
  }
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
  padding: 0 30rpx;
  margin-bottom: 30rpx;
  
  .nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .nav-icon {
      width: 80rpx;
      height: 80rpx;
      margin-bottom: 12rpx;
    }
    
    .nav-text {
      font-size: 24rpx;
      color: #333333;
    }
  }
}

.content-tabs {
  display: flex;
  padding: 0 30rpx;
  margin-bottom: 20rpx;
  
  .tab-item {
    font-size: 32rpx;
    color: #999999;
    padding-bottom: 8rpx;
    margin-right: 40rpx;
    
    &.active {
      color: #333333;
      font-weight: 600;
      border-bottom: 4rpx solid #333333;
    }
  }
}

.waterfall {
  display: flex;
  flex-wrap: wrap;
  padding: 0 20rpx;
  
  .waterfall-item {
    width: calc(50% - 10rpx);
    margin-bottom: 20rpx;
    
    &:nth-child(2n) {
      margin-left: 20rpx;
    }
  }
}

.artwork-card {
  background-color: #ffffff;
  border-radius: 12rpx;
  overflow: hidden;
  
  .artwork-card-image {
    width: 100%;
    aspect-ratio: 1;
    background-color: #f5f5f5;
  }
  
  .artwork-card-info {
    padding: 16rpx;
  }
  
  .artwork-info-row {
    margin-bottom: 8rpx;
  }
  
  .artwork-title {
    font-size: 26rpx;
    color: #333333;
    @include ellipsis(1);
  }
  
  .artwork-meta {
    display: flex;
    font-size: 22rpx;
    color: #999999;
    margin-bottom: 12rpx;
    
    text:not(:last-child)::after {
      content: '/';
      margin: 0 6rpx;
    }
  }
  
  .artwork-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  
  .author-info {
    display: flex;
    align-items: center;
    
    .author-avatar {
      width: 36rpx;
      height: 36rpx;
      border-radius: 50%;
      margin-right: 8rpx;
    }
    
    .author-name {
      font-size: 22rpx;
      color: #666666;
      max-width: 80rpx;
      @include ellipsis(1);
    }
    
    .identity-tag {
      font-size: 18rpx;
      padding: 2rpx 8rpx;
      border-radius: 10rpx;
      margin-left: 6rpx;
      
      &.identity-artist {
        background-color: rgba(230, 190, 138, 0.1);
        color: #e6be8a;
      }
    }
  }
  
  .price-info {
    display: flex;
    align-items: center;
    
    .price {
      font-size: 28rpx;
      color: #333333;
      font-weight: 600;
    }
    
    .price-tag {
      font-size: 20rpx;
      color: #ff4d4f;
      margin-left: 8rpx;
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
  
  .empty-image {
    width: 240rpx;
    height: 240rpx;
    margin-bottom: 30rpx;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: #999999;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx 0;
  font-size: 24rpx;
  color: #999999;
}
</style>
