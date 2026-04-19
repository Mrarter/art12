<template>
  <view class="index-page">
    <!-- 顶部搜索栏 -->
    <view class="search-bar">
      <view class="search-input" @click="goSearch">
        <text class="iconfont icon-search">🔍</text>
        <text class="placeholder">搜索作品/艺术家/经纪人</text>
      </view>
      <view class="message-icon" @click="goMessage">
        <text class="iconfont icon-message">💬</text>
        <view v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</view>
      </view>
    </view>

    <!-- 内容区域 -->
    <scroll-view 
      scroll-y 
      class="content-scroll"
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <!-- Banner 轮播 -->
      <view class="banner-section">
        <swiper 
          class="banner-swiper" 
          indicator-dots 
          autoplay 
          circular
          indicator-color="rgba(255,255,255,0.5)"
          indicator-active-color="#fff"
          interval="3000"
        >
          <swiper-item v-for="(banner, index) in banners" :key="index">
            <image 
              class="banner-image" 
              :src="banner.imageUrl" 
              mode="aspectFill"
              @click="onBannerClick(banner)"
            />
          </swiper-item>
        </swiper>
      </view>

      <!-- 金刚区 -->
      <view class="nav-grid">
        <view 
          class="nav-item" 
          v-for="(nav, index) in navItems" 
          :key="index"
          @click="onNavClick(nav.path)"
        >
          <view class="nav-icon" :style="{ backgroundColor: nav.bgColor }">
            <text>{{ nav.icon }}</text>
          </view>
          <text class="nav-text">{{ nav.text }}</text>
        </view>
      </view>

      <!-- 内容 Tab -->
      <view class="content-tabs">
        <view 
          class="tab-item" 
          :class="{ active: activeTab === 'recommend' }"
          @click="switchTab('recommend')"
        >
          推荐
        </view>
        <view 
          class="tab-item"
          :class="{ active: activeTab === 'follow' }"
          @click="switchTab('follow')"
        >
          关注
        </view>
      </view>

      <!-- 作品瀑布流 -->
      <view class="artwork-waterfall">
        <view class="waterfall-column left">
          <view 
            class="artwork-card"
            v-for="(item, index) in leftArtworks" 
            :key="item.id"
            @click="goArtworkDetail(item.id)"
          >
            <image 
              class="artwork-image" 
              :src="item.coverImage" 
              mode="widthFix"
              lazy-load
            />
            <view class="artwork-info">
              <view class="artwork-title">{{ item.title }}</view>
              <view class="artwork-author">
                <image class="author-avatar" :src="item.authorAvatar" />
                <text class="author-name">{{ item.authorName }}</text>
                <view class="author-badge" v-if="item.authorBadge">{{ item.authorBadge }}</view>
              </view>
              <view class="artwork-price">
                <text class="price-symbol">¥</text>
                <text class="price-value">{{ formatPrice(item.price) }}</text>
                <view class="price-rise tag tag-danger" v-if="item.priceRise > 0">
                  +{{ item.priceRise }}%
                </view>
              </view>
            </view>
          </view>
        </view>
        <view class="waterfall-column right">
          <view 
            class="artwork-card"
            v-for="(item, index) in rightArtworks" 
            :key="item.id"
            @click="goArtworkDetail(item.id)"
          >
            <image 
              class="artwork-image" 
              :src="item.coverImage" 
              mode="widthFix"
              lazy-load
            />
            <view class="artwork-info">
              <view class="artwork-title">{{ item.title }}</view>
              <view class="artwork-author">
                <image class="author-avatar" :src="item.authorAvatar" />
                <text class="author-name">{{ item.authorName }}</text>
                <view class="author-badge" v-if="item.authorBadge">{{ item.authorBadge }}</view>
              </view>
              <view class="artwork-price">
                <text class="price-symbol">¥</text>
                <text class="price-value">{{ formatPrice(item.price) }}</text>
                <view class="price-rise tag tag-danger" v-if="item.priceRise > 0">
                  +{{ item.priceRise }}%
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 加载状态 -->
      <view class="loading-status" v-if="artworks.length > 0">
        <text v-if="isLoading">加载中...</text>
        <text v-else-if="noMore">没有更多了</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getHomepageBanners, getHomepageRecommend } from '@/api/product'

const userStore = useUserStore()

// 状态
const unreadCount = ref(0)
const banners = ref([])
const artworks = ref([])
const activeTab = ref('recommend')
const isRefreshing = ref(false)
const isLoading = ref(false)
const noMore = ref(false)
const page = ref(1)

// 导航项
const navItems = [
  { text: '画廊', icon: '🖼️', path: '/pages/gallery/index', bgColor: '#F5F5F5' },
  { text: '拍卖', icon: '🔨', path: '/pages/auction/index', bgColor: '#FFF8E1' },
  { text: '鉴赏', icon: '🔍', path: '/pages/gallery/appreciate', bgColor: '#E8F5E9' },
  { text: '艺术圈', icon: '👥', path: '/pages/community/index', bgColor: '#E3F2FD' }
]

// 瀑布流分组
const leftArtworks = computed(() => artworks.value.filter((_, i) => i % 2 === 0))
const rightArtworks = computed(() => artworks.value.filter((_, i) => i % 2 === 1))

// 生命周期
onMounted(() => {
  loadData()
})

// 方法
async function loadData() {
  try {
    // 加载 Banner
    const bannerRes = await getHomepageBanners()
    banners.value = bannerRes || []
    
    // 加载推荐作品
    const recommendRes = await getHomepageRecommend({ page: page.value, pageSize: 20 })
    artworks.value = recommendRes.records || []
    
    // 模拟数据
    if (banners.value.length === 0) {
      banners.value = [
        { id: 1, imageUrl: 'https://picsum.photos/750/400?random=1', linkType: 'gallery', linkId: 1 },
        { id: 2, imageUrl: 'https://picsum.photos/750/400?random=2', linkType: 'auction', linkId: 1 },
        { id: 3, imageUrl: 'https://picsum.photos/750/400?random=3', linkType: 'article', linkId: 1 }
      ]
    }
    
    if (artworks.value.length === 0) {
      artworks.value = generateMockArtworks()
    }
  } catch (e) {
    console.error('加载数据失败', e)
    // 使用模拟数据
    banners.value = [
      { id: 1, imageUrl: 'https://picsum.photos/750/400?random=1', linkType: 'gallery', linkId: 1 },
      { id: 2, imageUrl: 'https://picsum.photos/750/400?random=2', linkType: 'auction', linkId: 1 }
    ]
    artworks.value = generateMockArtworks()
  }
}

function generateMockArtworks() {
  const authors = [
    { name: '罗中立', badge: '艺术家', avatar: 'https://picsum.photos/50/50?random=10' },
    { name: '张大千', badge: '艺术家', avatar: 'https://picsum.photos/50/50?random=11' },
    { name: '陈丹青', badge: '艺术家', avatar: 'https://picsum.photos/50/50?random=12' },
    { name: '持有者小李', badge: '持有', avatar: 'https://picsum.photos/50/50?random=13' }
  ]
  
  return Array.from({ length: 20 }, (_, i) => {
    const author = authors[i % authors.length]
    const height = 300 + Math.floor(Math.random() * 200)
    return {
      id: i + 1,
      title: `《${['父亲', '奔马图', '山水间', '静物', '风景'][i % 5]}》`,
      coverImage: `https://picsum.photos/350/${height}?random=${i + 100}`,
      authorName: author.name,
      authorAvatar: author.avatar,
      authorBadge: author.badge,
      price: 5000 + Math.floor(Math.random() * 50000),
      priceRise: Math.random() > 0.7 ? Math.floor(Math.random() * 10) : 0
    }
  })
}

async function onRefresh() {
  isRefreshing.value = true
  page.value = 1
  noMore.value = false
  await loadData()
  isRefreshing.value = false
}

async function loadMore() {
  if (isLoading.value || noMore.value) return
  isLoading.value = true
  page.value++
  
  try {
    const res = await getHomepageRecommend({ page: page.value, pageSize: 20 })
    if (res.records?.length) {
      artworks.value.push(...res.records)
    } else {
      noMore.value = true
    }
  } catch (e) {
    // 模拟加载
    const newData = generateMockArtworks().map((item, i) => ({
      ...item,
      id: page.value * 20 + i + 1
    }))
    artworks.value.push(...newData)
  }
  
  isLoading.value = false
}

function switchTab(tab) {
  activeTab.value = tab
  page.value = 1
  noMore.value = false
  loadData()
}

function goSearch() {
  uni.navigateTo({ url: '/pages/index/search' })
}

function goMessage() {
  if (!userStore.isLoggedIn) {
    goLogin()
    return
  }
  uni.navigateTo({ url: '/pages/message/list' })
}

function onNavClick(path) {
  if (!userStore.isLoggedIn && ['/pages/promoter/index'].some(p => path.startsWith(p))) {
    goLogin()
    return
  }
  uni.switchTab({ url: path })
}

function onBannerClick(banner) {
  if (banner.linkType === 'gallery') {
    uni.navigateTo({ url: `/pages/gallery/detail?id=${banner.linkId}` })
  } else if (banner.linkType === 'auction') {
    uni.switchTab({ url: '/pages/auction/index' })
  }
}

function goArtworkDetail(id) {
  uni.navigateTo({ url: `/pages/artwork/detail?id=${id}` })
}

function formatPrice(price) {
  if (price >= 10000) {
    return (price / 10000).toFixed(1) + '万'
  }
  return price.toLocaleString()
}

function goLogin() {
  uni.navigateTo({ url: '/pages/index/login' })
}
</script>

<style lang="scss" scoped>
.index-page {
  min-height: 100vh;
  background-color: #f8f8f8;
}

.search-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background-color: #fff;
  gap: 16rpx;

  .search-input {
    flex: 1;
    display: flex;
    align-items: center;
    height: 72rpx;
    padding: 0 24rpx;
    background-color: #f5f5f5;
    border-radius: 36rpx;
    gap: 12rpx;

    .icon-search {
      font-size: 28rpx;
      color: #999;
    }

    .placeholder {
      font-size: 26rpx;
      color: #999;
    }
  }

  .message-icon {
    position: relative;
    width: 72rpx;
    height: 72rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40rpx;

    .badge {
      position: absolute;
      top: 8rpx;
      right: 8rpx;
      min-width: 32rpx;
      height: 32rpx;
      padding: 0 8rpx;
      background-color: #e53935;
      color: #fff;
      font-size: 20rpx;
      border-radius: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
}

.content-scroll {
  height: calc(100vh - 88rpx);
}

.banner-section {
  .banner-swiper {
    width: 100%;
    height: 400rpx;

    .banner-image {
      width: 100%;
      height: 100%;
    }
  }
}

.nav-grid {
  display: flex;
  padding: 32rpx 24rpx;
  background-color: #fff;
  margin-bottom: 16rpx;

  .nav-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12rpx;

    .nav-icon {
      width: 96rpx;
      height: 96rpx;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 48rpx;
    }

    .nav-text {
      font-size: 24rpx;
      color: #333;
    }
  }
}

.content-tabs {
  display: flex;
  padding: 0 24rpx;
  background-color: #fff;
  margin-bottom: 16rpx;

  .tab-item {
    flex: 1;
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 30rpx;
    color: #666;
    position: relative;

    &.active {
      color: #333;
      font-weight: 600;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 6rpx;
        background-color: #333;
        border-radius: 3rpx;
      }
    }
  }
}

.artwork-waterfall {
  display: flex;
  padding: 0 16rpx;
  gap: 16rpx;

  .waterfall-column {
    flex: 1;
  }

  .artwork-card {
    background-color: #fff;
    border-radius: 16rpx;
    overflow: hidden;
    margin-bottom: 16rpx;

    .artwork-image {
      width: 100%;
      display: block;
    }

    .artwork-info {
      padding: 20rpx;

      .artwork-title {
        font-size: 28rpx;
        color: #333;
        margin-bottom: 12rpx;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .artwork-author {
        display: flex;
        align-items: center;
        gap: 8rpx;
        margin-bottom: 12rpx;

        .author-avatar {
          width: 40rpx;
          height: 40rpx;
          border-radius: 50%;
        }

        .author-name {
          font-size: 22rpx;
          color: #666;
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .author-badge {
          font-size: 18rpx;
          color: #c9a962;
          background-color: rgba(201, 169, 98, 0.1);
          padding: 2rpx 8rpx;
          border-radius: 4rpx;
        }
      }

      .artwork-price {
        display: flex;
        align-items: baseline;
        gap: 4rpx;

        .price-symbol {
          font-size: 22rpx;
          color: #333;
        }

        .price-value {
          font-size: 32rpx;
          font-weight: 600;
          color: #333;
        }

        .price-rise {
          margin-left: 12rpx;
          font-size: 20rpx;
        }
      }
    }
  }
}

.loading-status {
  padding: 32rpx;
  text-align: center;
  color: #999;
  font-size: 24rpx;
}
</style>
