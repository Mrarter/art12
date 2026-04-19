<template>
  <view class="artwork-detail">
    <!-- 图片轮播 -->
    <swiper class="image-swiper" indicator-dots circular>
      <swiper-item v-for="(img, index) in images" :key="index">
        <image :src="img" mode="aspectFill" @click="previewImage(index)" />
      </swiper-item>
    </swiper>

    <!-- 价格区域 -->
    <view class="price-section">
      <view class="price-row">
        <text class="current-price">¥{{ formatPrice(detail.price) }}</text>
        <view v-if="detail.priceRise > 0" class="price-rise">
          <text>↑ {{ detail.priceRise }}%</text>
          <text class="rise-label">累计涨幅</text>
        </view>
      </view>
      <view v-if="detail.originalPrice" class="original-price">
        原价: ¥{{ formatPrice(detail.originalPrice) }}
      </view>
    </view>

    <!-- 基本信息 -->
    <view class="info-section">
      <view class="section-title">作品信息</view>
      <view class="info-grid">
        <view class="info-item">
          <text class="label">名称</text>
          <text class="value">{{ detail.title }}</text>
        </view>
        <view class="info-item" v-if="detail.artType">
          <text class="label">画种</text>
          <text class="value">{{ detail.artType }}</text>
        </view>
        <view class="info-item" v-if="detail.medium">
          <text class="label">材质</text>
          <text class="value">{{ detail.medium }}</text>
        </view>
        <view class="info-item" v-if="detail.size">
          <text class="label">尺寸</text>
          <text class="value">{{ detail.size }}</text>
        </view>
        <view class="info-item" v-if="detail.year">
          <text class="label">创作年份</text>
          <text class="value">{{ detail.year }}年</text>
        </view>
        <view class="info-item" v-if="detail.edition">
          <text class="label">版数</text>
          <text class="value">{{ detail.edition }}</text>
        </view>
      </view>
    </view>

    <!-- 发布者信息 -->
    <view class="author-section" @click="goAuthorPage">
      <image class="author-avatar" :src="authorInfo.avatar || '/static/default-avatar.png'" />
      <view class="author-info">
        <view class="author-name">
          {{ authorInfo.name }}
          <view class="author-badge">{{ authorInfo.badge }}</view>
        </view>
        <text class="author-meta">查看主页</text>
      </view>
      <text class="arrow">›</text>
    </view>

    <!-- 作品故事 -->
    <view class="story-section" v-if="detail.description">
      <view class="section-title">作品故事</view>
      <view class="story-content" :class="{ expanded: showFullStory }">
        {{ detail.description }}
      </view>
      <view class="story-toggle" @click="showFullStory = !showFullStory">
        {{ showFullStory ? '收起' : '展开全部' }}
      </view>
    </view>

    <!-- 数据统计 -->
    <view class="stats-section">
      <view class="stat-item">
        <text class="stat-value">{{ detail.viewCount || 0 }}</text>
        <text class="stat-label">浏览</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ detail.favoriteCount || 0 }}</text>
        <text class="stat-label">收藏</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ detail.saleCount || 0 }}</text>
        <text class="stat-label">已售</text>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="action-bar safe-area-bottom">
      <view class="action-icons">
        <view class="action-icon" @click="toggleFavorite">
          <text>{{ isFavorited ? '❤️' : '🤍' }}</text>
          <text class="icon-label">收藏</text>
        </view>
        <view class="action-icon" @click="goCart">
          <text>🛒</text>
          <text class="icon-label">购物车</text>
        </view>
      </view>
      <view class="action-buttons">
        <button class="btn-add-cart" @click="addToCart">加入购物车</button>
        <button class="btn-buy" @click="buyNow">立即购买</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getArtworkDetail, favoriteArtwork, unfavoriteArtwork } from '@/api/product'
import { addToCart as addCart } from '@/api/order'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const detail = ref({})
const images = ref([])
const authorInfo = ref({ name: '艺术家', badge: '艺术家' })
const isFavorited = ref(false)
const showFullStory = ref(false)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options?.id || 1
  loadDetail(id)
})

async function loadDetail(id) {
  try {
    const res = await getArtworkDetail(id)
    detail.value = res
    images.value = res.images?.length ? res.images : [res.coverImage]
    isFavorited.value = res.isFavorited || false
    authorInfo.value = {
      name: res.authorName || '艺术家',
      badge: res.authorBadge || '艺术家',
      avatar: res.authorAvatar
    }
  } catch (e) {
    // 使用模拟数据
    detail.value = {
      id: 1,
      title: '《父亲》罗中立 布面油画',
      price: 2800000,
      originalPrice: 3200000,
      priceRise: 3.2,
      artType: '布面油画',
      medium: '油画颜料',
      size: '50*70cm',
      year: 2025,
      edition: '原作1/1',
      description: '这幅作品是罗中立先生的代表作之一，描绘了一位饱经风霜的老农形象...',
      viewCount: 1234,
      favoriteCount: 256,
      saleCount: 1
    }
    images.value = ['https://picsum.photos/750/500?random=1']
  }
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/index/login' })
    return
  }
  
  try {
    if (isFavorited.value) {
      await unfavoriteArtwork(detail.value.id)
    } else {
      await favoriteArtwork(detail.value.id)
    }
    isFavorited.value = !isFavorited.value
  } catch (e) {
    isFavorited.value = !isFavorited.value
  }
}

async function addToCart() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/index/login' })
    return
  }
  
  try {
    await addCart(detail.value.id, 1)
    uni.showToast({ title: '已加入购物车' })
  } catch (e) {
    uni.showToast({ title: '加入失败', icon: 'none' })
  }
}

function buyNow() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/index/login' })
    return
  }
  
  uni.navigateTo({ 
    url: `/pages/order/confirm?artworkId=${detail.value.id}&quantity=1` 
  })
}

function goCart() {
  uni.switchTab({ url: '/pages/cart/index' })
}

function goAuthorPage() {
  uni.navigateTo({ url: `/pages/profile/artist?id=${detail.value.authorId}` })
}

function previewImage(index) {
  uni.previewImage({
    current: index,
    urls: images.value
  })
}

function formatPrice(price) {
  if (!price) return '0'
  if (price >= 100000000) {
    return (price / 100000000).toFixed(1) + '亿'
  }
  if (price >= 10000) {
    return (price / 10000).toFixed(1) + '万'
  }
  return price.toLocaleString()
}
</script>

<style lang="scss" scoped>
.artwork-detail {
  padding-bottom: 140rpx;
}

.image-swiper {
  width: 100%;
  height: 500rpx;
  
  image {
    width: 100%;
    height: 100%;
  }
}

.price-section {
  padding: 32rpx;
  background: #fff;
  
  .price-row {
    display: flex;
    align-items: baseline;
    gap: 16rpx;
  }
  
  .current-price {
    font-size: 48rpx;
    font-weight: 700;
    color: #333;
  }
  
  .price-rise {
    display: flex;
    align-items: center;
    gap: 8rpx;
    font-size: 24rpx;
    color: #e53935;
    
    .rise-label {
      color: #999;
    }
  }
  
  .original-price {
    margin-top: 12rpx;
    font-size: 26rpx;
    color: #999;
    text-decoration: line-through;
  }
}

.info-section {
  padding: 32rpx;
  background: #fff;
  margin-top: 16rpx;
  
  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    margin-bottom: 24rpx;
  }
  
  .info-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24rpx;
  }
  
  .info-item {
    .label {
      display: block;
      font-size: 24rpx;
      color: #999;
      margin-bottom: 8rpx;
    }
    
    .value {
      font-size: 28rpx;
      color: #333;
    }
  }
}

.author-section {
  display: flex;
  align-items: center;
  padding: 32rpx;
  background: #fff;
  margin-top: 16rpx;
  
  .author-avatar {
    width: 96rpx;
    height: 96rpx;
    border-radius: 50%;
  }
  
  .author-info {
    flex: 1;
    margin-left: 24rpx;
    
    .author-name {
      font-size: 30rpx;
      color: #333;
      display: flex;
      align-items: center;
      gap: 12rpx;
      
      .author-badge {
        font-size: 20rpx;
        color: #c9a962;
        background: rgba(201, 169, 98, 0.1);
        padding: 4rpx 12rpx;
        border-radius: 4rpx;
      }
    }
    
    .author-meta {
      font-size: 24rpx;
      color: #999;
      margin-top: 8rpx;
    }
  }
  
  .arrow {
    font-size: 40rpx;
    color: #ccc;
  }
}

.story-section {
  padding: 32rpx;
  background: #fff;
  margin-top: 16rpx;
  
  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    margin-bottom: 24rpx;
  }
  
  .story-content {
    font-size: 28rpx;
    color: #666;
    line-height: 1.8;
    overflow: hidden;
    
    &.expanded {
      max-height: none;
    }
  }
  
  .story-toggle {
    text-align: center;
    color: #c9a962;
    font-size: 26rpx;
    margin-top: 16rpx;
  }
}

.stats-section {
  display: flex;
  padding: 32rpx;
  background: #fff;
  margin-top: 16rpx;
  
  .stat-item {
    flex: 1;
    text-align: center;
    
    .stat-value {
      display: block;
      font-size: 36rpx;
      font-weight: 600;
      color: #333;
    }
    
    .stat-label {
      font-size: 24rpx;
      color: #999;
    }
  }
}

.action-bar {
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
  
  .action-icons {
    display: flex;
    gap: 32rpx;
  }
  
  .action-icon {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4rpx;
    
    .icon-label {
      font-size: 20rpx;
      color: #666;
    }
  }
  
  .action-buttons {
    flex: 1;
    display: flex;
    gap: 16rpx;
    margin-left: 32rpx;
    
    button {
      flex: 1;
      height: 80rpx;
      border-radius: 40rpx;
      font-size: 28rpx;
      border: none;
    }
    
    .btn-add-cart {
      background: #f5f5f5;
      color: #333;
    }
    
    .btn-buy {
      background: #333;
      color: #fff;
    }
  }
}
</style>
