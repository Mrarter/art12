<template>
  <view class="index-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: safeAreaTop + 'px', height: (safeAreaTop + 88) + 'rpx' }">
      <view class="search-box" @click="goSearch">
        <image class="search-icon-img" src="/static/icons/search-magnifier.png" mode="aspectFit"></image>
        <text class="placeholder">搜索作品/艺术家</text>
      </view>
      <view class="message-icon" @click="goMessage">
        <image class="message-icon-img" src="/static/icons/bell.svg" mode="aspectFit"></image>
        <view class="badge-dot" v-if="hasMessage"></view>
      </view>
    </view>
    
    <!-- 内容区域 -->
    <scroll-view 
      class="content" 
      :style="{ paddingTop: (safeAreaTop * 2 + 88) + 'rpx' }"
      scroll-y 
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <!-- Banner轮播 - 深色主题 -->
      <view class="banner-section">
        <swiper class="banner-swiper" indicator-dots autoplay circular indicator-active-color="#D4AF37">
          <swiper-item v-for="(banner, index) in banners" :key="index">
            <view class="banner-item" @click="onBannerClick(banner)">
              <image class="banner-image" :src="banner.imageUrl" mode="aspectFill"></image>
              <view class="banner-overlay"></view>
              <view class="banner-content">
                <text class="banner-tag">{{ banner.tag || '艺术展' }}</text>
                <text class="banner-title">{{ banner.title }}</text>
              </view>
            </view>
          </swiper-item>
        </swiper>
      </view>
      
      <!-- 金刚区 - 深色主题 -->
      <view class="nav-grid">
        <view class="nav-item" v-for="item in navItems" :key="item.id" @click="goNav(item)">
          <view class="nav-icon-wrapper">
            <image class="nav-icon-img" :src="item.icon" mode="aspectFit"></image>
          </view>
          <text class="nav-text">{{ item.text }}</text>
        </view>
      </view>
      
      <!-- Tab切换 - 深色主题 -->
      <view class="content-tabs">
        <view 
          class="tab-item" 
          :class="{ active: currentTab === 'recommend' }"
          @click="switchTab('recommend')"
        >
          <text>推荐</text>
          <view class="tab-line" v-if="currentTab === 'recommend'"></view>
        </view>
        <view 
          class="tab-item"
          :class="{ active: currentTab === 'following' }"
          @click="switchTab('following')"
        >
          <text>关注</text>
          <view class="tab-line" v-if="currentTab === 'following'"></view>
        </view>
      </view>
      
      <!-- 作品瀑布流 - 深色主题 -->
      <view class="waterfall">
        <view 
          class="waterfall-item" 
          v-for="(item, index) in productList" 
          :key="item.id"
          @click="goDetail(item)"
        >
          <view class="artwork-card">
            <view class="artwork-image-wrapper">
              <image class="artwork-card-image" :src="item.cover" mode="aspectFill"></image>
              <view class="artwork-tag" v-if="item.isHot">热</view>
              <view class="artwork-tag new-tag" v-if="item.isNew">新</view>
            </view>
            <view class="artwork-card-info">
              <text class="artwork-title">{{ item.title }}</text>
              <view class="artwork-author">
                <text class="author-name">{{ item.artistName || item.authorName }}</text>
                <view class="identity-tag" v-if="item.authorIdentity">
                  <text>{{ getIdentityText(item.authorIdentity) }}</text>
                </view>
              </view>
              <view class="artwork-footer">
                <view class="price-info">
                  <text class="price-symbol">¥</text>
                  <text class="price-value">{{ formatPrice(item.price) }}</text>
                  <text class="price-change up" v-if="item.priceChange > 0">+{{ item.priceChange }}%</text>
                  <text class="price-change down" v-else-if="item.priceChange < 0">{{ item.priceChange }}%</text>
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
        <text class="empty-icon">🎨</text>
        <text class="empty-text">暂无作品</text>
      </view>
      
      <!-- 底部安全区 -->
      <view class="safe-area-bottom"></view>
    </scroll-view>
    
    <!-- 自定义TabBar -->
    <CustomTabBar :currentIndex="0" />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import CustomTabBar from '@/components/custom-tab-bar/index.vue'
import { getBanners, getRecommend, getFollowingWorks } from '@/api/product'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

// 安全区域高度
const safeAreaTop = ref(0)
const navBarHeight = ref(88) // 默认导航栏高度

// 获取状态栏高度
const getSystemInfo = () => {
  try {
    const systemInfo = uni.getSystemInfoSync()
    safeAreaTop.value = systemInfo.statusBarHeight || 0
    console.log('状态栏高度:', safeAreaTop.value)
  } catch (e) {
    console.error('获取系统信息失败', e)
  }
}

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

// 金刚区配置 - 深色主题
const navItems = [
  { id: 1, text: '画廊', icon: '/static/icons/nav-gallery.svg', path: '/pages/gallery/index' },
  { id: 2, text: '艺术家', icon: '/static/icons/nav-artist.svg', path: '/pages/artist/home' },
  { id: 3, text: '购物车', icon: '/static/icons/nav-cart.svg', path: '/pages/cart/index' },
  { id: 4, text: '鉴赏', icon: '/static/icons/nav-appreciate.svg', path: '/pages/common/coming-soon?title=鉴赏专区&desc=鉴赏频道正在整理中，后续会接入专题内容与策展推荐。' }
]

// 获取Banner
const fetchBanners = async () => {
  try {
    const list = await getBanners()
    banners.value = list || []
    if (banners.value.length === 0) {
      banners.value = [
        { id: 1, imageUrl: 'https://picsum.photos/750/400?random=10', title: '致敬经典：当代写实油画展', tag: '艺术展' },
        { id: 2, imageUrl: 'https://picsum.photos/750/400?random=11', title: '当代艺术名家联展', tag: '展览' },
        { id: 3, imageUrl: 'https://picsum.photos/750/400?random=12', title: '青年艺术家扶持计划', tag: '活动' }
      ]
    }
  } catch (e) {
    banners.value = [
      { id: 1, imageUrl: 'https://picsum.photos/750/400?random=10', title: '致敬经典：当代写实油画展', tag: '艺术展' },
      { id: 2, imageUrl: 'https://picsum.photos/750/400?random=11', title: '当代艺术名家联展', tag: '展览' },
      { id: 3, imageUrl: 'https://picsum.photos/750/400?random=12', title: '青年艺术家扶持计划', tag: '活动' }
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
      const result = await getRecommend(params)
      // 处理 PageResult 格式：{ records: [], total: xxx, page: xxx }
      list = result?.records || result || []
    } else {
      const result = await getFollowingWorks(params)
      list = result?.records || result || []
    }

    // 分页处理
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
    console.error('获取作品列表失败:', e)
  } finally {
    loading.value = false
  }
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
  uni.navigateTo({ url: '/pages/message/index' })
}

// 金刚区导航
const goNav = (item) => {
  const tabBarPages = new Set([
    '/pages/index/index',
    '/pages/gallery/index',
    '/pages/auction/index',
    '/pages/cart/index',
    '/pages/user/index'
  ])

  const purePath = item.path.split('?')[0]
  if (tabBarPages.has(purePath)) {
    uni.switchTab({ url: item.path })
  } else {
    uni.navigateTo({ url: item.path })
  }
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
  const yuan = price / 100  // 分转元
  if (yuan >= 10000) {
    return (yuan / 10000).toFixed(yuan % 10000 === 0 ? 0 : 1) + '万'
  }
  return yuan.toLocaleString()
}

// 获取身份文字
const getIdentityText = (identity) => {
  const map = {
    artist: '艺术家',
    agent: '经纪人',
    promoter: '艺荐官',
    collector: '持有者',
    master: '大师'
  }
  return map[identity] || ''
}

// 初始化
onMounted(() => {
  getSystemInfo()
  fetchBanners()
  fetchProductList(true)
})
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
$price-up: #FF6B6B;
$price-down: #4CAF50;

.index-page {
  min-height: 100vh;
  background-color: $bg-primary;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  padding: 16rpx 30rpx;
  background-color: $bg-primary;

  .search-box {
    flex: 1;
    display: flex;
    align-items: center;
    height: 72rpx;
    padding: 0 24rpx;
    background-color: $bg-secondary;
    border-radius: 36rpx;
    border: 1rpx solid rgba(255, 255, 255, 0.1);
    
    .search-icon-img {
      width: 36rpx;
      height: 36rpx;
      margin-right: 12rpx;
      opacity: 0.6;
    }
    
    .placeholder {
      font-size: 26rpx;
      color: $text-muted;
    }
  }
  
  .message-icon {
    position: relative;
    width: 72rpx;
    height: 72rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-left: 20rpx;
    
    .message-icon-img {
      width: 44rpx;
      height: 44rpx;
    }
    
    .badge-dot {
      position: absolute;
      top: 12rpx;
      right: 12rpx;
      width: 16rpx;
      height: 16rpx;
      background-color: $accent-gold;
      border-radius: 50%;
    }
  }
}

.content {
  padding-top: 120rpx;
  height: calc(100vh - 120rpx);
}

.banner-section {
  padding: 0 30rpx;
  margin-bottom: 30rpx;
}

.banner-swiper {
  height: 320rpx;
  border-radius: 20rpx;
  overflow: hidden;
  
  .banner-item {
    position: relative;
    width: 100%;
    height: 100%;
    
    .banner-image {
      width: 100%;
      height: 100%;
    }
    
    .banner-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(to bottom, rgba(0,0,0,0) 0%, rgba(0,0,0,0.6) 100%);
    }
    
    .banner-content {
      position: absolute;
      bottom: 30rpx;
      left: 30rpx;
      right: 30rpx;
      
      .banner-tag {
        display: inline-block;
        font-size: 22rpx;
        color: $bg-primary;
        background-color: $accent-gold;
        padding: 6rpx 16rpx;
        border-radius: 6rpx;
        margin-bottom: 12rpx;
      }
      
      .banner-title {
        display: block;
        font-size: 32rpx;
        color: $text-primary;
        font-weight: 600;
      }
    }
  }
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
  padding: 0 30rpx;
  margin-bottom: 40rpx;
  
  .nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .nav-icon-wrapper {
      width: 108rpx;
      height: 108rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      background: radial-gradient(circle at 30% 30%, rgba(36, 42, 56, 0.9), rgba(22, 24, 31, 0.98));
      border-radius: 50%;
      margin-bottom: 14rpx;
      border: 1rpx solid rgba(71, 95, 132, 0.35);
      box-shadow: inset 0 0 0 1rpx rgba(255, 255, 255, 0.03);
    }
    
    .nav-icon-img {
      width: 52rpx;
      height: 52rpx;
    }
    
    .nav-text {
      font-size: 24rpx;
      color: $text-secondary;
      letter-spacing: 1rpx;
    }
  }
}

.content-tabs {
  display: flex;
  padding: 0 30rpx;
  margin-bottom: 30rpx;
  
  .tab-item {
    position: relative;
    font-size: 32rpx;
    color: $text-muted;
    padding-bottom: 8rpx;
    margin-right: 50rpx;
    
    &.active {
      color: $text-primary;
      font-weight: 600;
    }
    
    .tab-line {
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 40rpx;
      height: 4rpx;
      background-color: $accent-gold;
      border-radius: 2rpx;
    }
  }
}

.waterfall {
  display: flex;
  flex-wrap: wrap;
  width: 100%;
  padding: 0 20rpx;
  box-sizing: border-box;
  
  .waterfall-item {
    width: calc(50% - 10rpx);
    margin-bottom: 20rpx;
    
    &:nth-child(2n) {
      margin-left: 20rpx;
    }
  }
}

.artwork-card {
  background-color: $bg-card;
  border-radius: 16rpx;
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.05);
  
  .artwork-image-wrapper {
    position: relative;
    width: 100%;
    aspect-ratio: 1;
    
    .artwork-card-image {
      width: 100%;
      height: 100%;
    }
    
    .artwork-tag {
      position: absolute;
      top: 16rpx;
      right: 16rpx;
      width: 44rpx;
      height: 44rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: $accent-orange;
      border-radius: 8rpx;
      font-size: 22rpx;
      color: $bg-primary;
      font-weight: 600;
      
      &.new-tag {
        background-color: $accent-gold;
        right: 70rpx;
      }
    }
  }
  
  .artwork-card-info {
    padding: 20rpx;
  }
  
  .artwork-title {
    display: block;
    font-size: 28rpx;
    color: $text-primary;
    font-weight: 500;
    margin-bottom: 12rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .artwork-author {
    display: flex;
    align-items: center;
    margin-bottom: 16rpx;
    
    .author-name {
      font-size: 24rpx;
      color: $text-secondary;
    }
    
    .identity-tag {
      margin-left: 12rpx;
      padding: 4rpx 10rpx;
      background-color: rgba($accent-gold, 0.15);
      border-radius: 6rpx;
      
      text {
        font-size: 18rpx;
        color: $accent-gold;
      }
    }
  }
  
  .artwork-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  
  .price-info {
    display: flex;
    align-items: baseline;
    
    .price-symbol {
      font-size: 24rpx;
      color: $accent-gold;
      margin-right: 4rpx;
    }
    
    .price-value {
      font-size: 32rpx;
      color: $accent-gold;
      font-weight: 600;
    }
    
    .price-change {
      font-size: 22rpx;
      margin-left: 10rpx;
      
      &.up {
        color: $price-up;
      }
      
      &.down {
        color: $price-down;
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
  
  .empty-icon {
    font-size: 120rpx;
    margin-bottom: 30rpx;
    opacity: 0.5;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: $text-muted;
  }
}

.load-more {
  text-align: center;
  padding: 40rpx 0;
  font-size: 24rpx;
  color: $text-muted;
}

.safe-area-bottom {
  height: calc(100rpx + env(safe-area-inset-bottom));
}
</style>
