<template>
  <view class="detail-page">
    <!-- 图片轮播 -->
    <swiper class="image-swiper" indicator-dots @change="onSwiperChange">
      <swiper-item v-for="(img, index) in images" :key="index">
        <image class="product-image" :src="img" mode="aspectFit" @click="previewImage(index)"></image>
      </swiper-item>
    </swiper>
    <view class="image-indicator">{{ currentImageIndex + 1 }}/{{ images.length }}</view>
    
    <!-- 视频按钮 -->
    <view class="video-btn" v-if="detail.videoUrl" @click="playVideo">
      <text class="iconfont icon-play"></text>
      <text>观看视频</text>
    </view>
    
    <!-- 商品信息 -->
    <view class="product-info card">
      <view class="title-section">
        <text class="title">{{ detail.title }}</text>
        <view class="share-btn" @click="onShare">
          <text class="iconfont icon-share"></text>
          <text>分享</text>
        </view>
      </view>
      
      <view class="author-row">
        <image class="author-avatar" :src="detail.authorAvatar || '/static/images/avatar.png'"></image>
        <view class="author-info" @click="goArtistHome">
          <text class="author-name">{{ detail.authorName }}</text>
          <view class="identity-tag" :class="'identity-' + detail.authorIdentity">
            {{ getIdentityText(detail.authorIdentity) }}
          </view>
        </view>
        <button class="follow-btn" v-if="!detail.isFollowing" @click="onFollow">关注</button>
        <text class="following-text" v-else>已关注</text>
      </view>
      
      <view class="price-section">
        <view class="price-row">
          <text class="current-price">¥{{ formatPrice(detail.price) }}</text>
          <text class="original-price" v-if="detail.originalPrice">¥{{ formatPrice(detail.originalPrice) }}</text>
          <view class="rate-badge" v-if="detail.priceRise > 0">
            <text>该作品已累计上涨 +{{ (detail.priceRise * 100).toFixed(1) }}%</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 基本信息 -->
    <view class="info-section card">
      <view class="section-title">基本信息</view>
      <view class="info-grid">
        <view class="info-item" v-if="detail.artType">
          <text class="info-label">画种</text>
          <text class="info-value">{{ detail.artType }}</text>
        </view>
        <view class="info-item" v-if="detail.size">
          <text class="info-label">尺寸</text>
          <text class="info-value">{{ detail.size }}</text>
        </view>
        <view class="info-item" v-if="detail.material">
          <text class="info-label">材质</text>
          <text class="info-value">{{ detail.material }}</text>
        </view>
        <view class="info-item" v-if="detail.createYear">
          <text class="info-label">创作年代</text>
          <text class="info-value">{{ detail.createYear }}年</text>
        </view>
        <view class="info-item" v-if="detail.edition">
          <text class="info-label">版数</text>
          <text class="info-value">{{ detail.edition }}</text>
        </view>
        <view class="info-item" v-if="detail.holdDuration">
          <text class="info-label">持有时长</text>
          <text class="info-value">{{ formatHoldDuration(detail.holdDuration) }}</text>
        </view>
      </view>
    </view>
    
    <!-- 作品故事 -->
    <view class="story-section card" v-if="detail.description">
      <view class="section-title">作品故事</view>
      <view class="story-content" :class="{ expanded: storyExpanded }">
        <text>{{ detail.description }}</text>
      </view>
      <view class="story-toggle" v-if="storyCanExpand" @click="storyExpanded = !storyExpanded">
        <text>{{ storyExpanded ? '收起' : '展开全部' }}</text>
        <text class="iconfont" :class="storyExpanded ? 'icon-arrow-up' : 'icon-arrow-down'"></text>
      </view>
    </view>
    
    <!-- 底部操作栏 -->
    <view class="action-bar safe-area-bottom">
      <view class="action-icons">
        <view class="action-item" @click="onFavorite">
          <text class="iconfont" :class="detail.isFavorite ? 'icon-star-filled' : 'icon-star'"></text>
          <text>{{ detail.favoriteCount || 0 }}</text>
        </view>
      </view>
      <view class="action-buttons">
        <button class="btn-add-cart" @click="onAddCart">加入购物车</button>
        <button class="btn-buy" @click="onBuy">立即购买</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getProductDetail, addFavorite, removeFavorite } from '@/api/product'
import { addToCart } from '@/api/cart'
import { followArtist, unfollowArtist } from '@/api/user'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

// 状态
const detail = ref({})
const images = ref([])
const currentImageIndex = ref(0)
const storyExpanded = ref(false)

// 是否可展开故事
const storyCanExpand = computed(() => {
  return detail.value.description && detail.value.description.length > 100
})

// 获取详情
const fetchDetail = async () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options?.id
  
  if (!id) return
  
  try {
    const data = await getProductDetail(id)
    detail.value = data
    
    // 处理图片
    if (data.images && data.images.length > 0) {
      images.value = data.images
    } else if (data.coverImage) {
      images.value = [data.coverImage]
    }
  } catch (e) {
    console.error('获取详情失败', e)
  }
}

// 轮播切换
const onSwiperChange = (e) => {
  currentImageIndex.value = e.detail.current
}

// 预览图片
const previewImage = (index) => {
  uni.previewImage({
    current: index,
    urls: images.value
  })
}

// 播放视频
const playVideo = () => {
  if (detail.value.videoUrl) {
    uni.navigateTo({
      url: `/pages/common/video?url=${encodeURIComponent(detail.value.videoUrl)}`
    })
  }
}

// 收藏/取消收藏
const onFavorite = async () => {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  
  try {
    if (detail.value.isFavorite) {
      await removeFavorite(detail.value.artworkId)
      detail.value.isFavorite = false
      detail.value.favoriteCount = (detail.value.favoriteCount || 1) - 1
    } else {
      await addFavorite(detail.value.id)
      detail.value.isFavorite = true
      detail.value.favoriteCount = (detail.value.favoriteCount || 0) + 1
    }
  } catch (e) {
    console.error('收藏失败', e)
  }
}

// 关注/取消关注
const onFollow = async () => {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  
  try {
    if (detail.value.isFollowing) {
      await unfollowArtist(detail.value.authorId)
      detail.value.isFollowing = false
    } else {
      await followArtist(detail.value.authorId)
      detail.value.isFollowing = true
    }
  } catch (e) {
    console.error('关注失败', e)
  }
}

// 加入购物车
const onAddCart = async () => {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  
  try {
    await addToCart(detail.value.id)
    uni.showToast({ title: '已加入购物车', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '加入失败', icon: 'none' })
  }
}

// 立即购买
const onBuy = () => {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  
  uni.navigateTo({
    url: `/pages/order/confirm?artworkId=${detail.value.id}`
  })
}

// 分享
const onShare = () => {
  uni.showShareMenu({
    withShareTicket: true
  })
}

// 跳转艺术家主页
const goArtistHome = () => {
  uni.navigateTo({
    url: `/pages/artist/home?userId=${detail.value.authorId}`
  })
}

// 格式化价格
const formatPrice = (price) => {
  if (!price) return '0'
  if (price >= 10000) {
    return (price / 10000).toFixed(1) + '万'
  }
  return price.toLocaleString()
}

// 格式化持有时长
const formatHoldDuration = (days) => {
  if (days < 30) return `${days}天`
  if (days < 365) return `${Math.floor(days / 30)}个月`
  return `${(days / 365).toFixed(1)}年`
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
  fetchDetail()
})
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  padding-bottom: 140rpx;
  background-color: #f8f8f8;
}

.image-swiper {
  height: 750rpx;
  background-color: #f5f5f5;
  
  .product-image {
    width: 100%;
    height: 100%;
  }
}

.image-indicator {
  position: absolute;
  right: 30rpx;
  bottom: 20rpx;
  padding: 8rpx 20rpx;
  background-color: rgba(0, 0, 0, 0.5);
  color: #ffffff;
  font-size: 24rpx;
  border-radius: 20rpx;
}

.video-btn {
  position: absolute;
  right: 30rpx;
  top: 650rpx;
  display: flex;
  align-items: center;
  padding: 12rpx 24rpx;
  background-color: rgba(0, 0, 0, 0.6);
  color: #ffffff;
  font-size: 24rpx;
  border-radius: 30rpx;
  
  .iconfont {
    margin-right: 8rpx;
  }
}

.product-info {
  .title-section {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24rpx;
    
    .title {
      flex: 1;
      font-size: 36rpx;
      font-weight: 600;
      color: #333333;
      @include ellipsis(2);
    }
    
    .share-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-left: 30rpx;
      color: #666666;
      font-size: 22rpx;
      
      .iconfont {
        font-size: 40rpx;
        margin-bottom: 4rpx;
      }
    }
  }
  
  .author-row {
    display: flex;
    align-items: center;
    margin-bottom: 24rpx;
    
    .author-avatar {
      width: 64rpx;
      height: 64rpx;
      border-radius: 50%;
      margin-right: 16rpx;
    }
    
    .author-info {
      flex: 1;
      
      .author-name {
        font-size: 28rpx;
        color: #333333;
        margin-bottom: 4rpx;
      }
      
      .identity-tag {
        display: inline-block;
        padding: 4rpx 12rpx;
        font-size: 20rpx;
        border-radius: 16rpx;
        
        &.identity-artist {
          background-color: rgba(230, 190, 138, 0.1);
          color: #e6be8a;
        }
      }
    }
    
    .follow-btn {
      padding: 12rpx 30rpx;
      font-size: 26rpx;
      background-color: #333333;
      color: #ffffff;
      border-radius: 30rpx;
    }
    
    .following-text {
      font-size: 26rpx;
      color: #999999;
    }
  }
  
  .price-section {
    .price-row {
      display: flex;
      align-items: baseline;
      
      .current-price {
        font-size: 44rpx;
        font-weight: 600;
        color: #333333;
      }
      
      .original-price {
        font-size: 28rpx;
        color: #cccccc;
        text-decoration: line-through;
        margin-left: 16rpx;
      }
      
      .rate-badge {
        margin-left: auto;
        padding: 8rpx 16rpx;
        background-color: rgba(255, 77, 79, 0.1);
        color: #ff4d4f;
        font-size: 22rpx;
        border-radius: 8rpx;
      }
    }
  }
}

.card {
  margin: 20rpx;
  padding: 24rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
}

.info-section {
  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #333333;
    margin-bottom: 20rpx;
  }
  
  .info-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20rpx;
    
    .info-item {
      display: flex;
      
      .info-label {
        width: 140rpx;
        font-size: 26rpx;
        color: #999999;
      }
      
      .info-value {
        flex: 1;
        font-size: 26rpx;
        color: #333333;
      }
    }
  }
}

.story-section {
  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #333333;
    margin-bottom: 20rpx;
  }
  
  .story-content {
    font-size: 28rpx;
    color: #666666;
    line-height: 1.8;
    
    &.expanded {
      display: block;
    }
    
    text {
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 3;
      overflow: hidden;
    }
    
    &.expanded text {
      -webkit-line-clamp: unset;
      overflow: visible;
    }
  }
  
  .story-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 20rpx;
    font-size: 26rpx;
    color: #e6be8a;
    
    .iconfont {
      margin-left: 8rpx;
    }
  }
}

.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background-color: #ffffff;
  border-top: 1rpx solid #f0f0f0;
  
  .action-icons {
    display: flex;
    
    .action-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-right: 40rpx;
      
      .iconfont {
        font-size: 44rpx;
        color: #666666;
        margin-bottom: 4rpx;
        
        &.icon-star-filled {
          color: #e6be8a;
        }
      }
      
      text {
        font-size: 22rpx;
        color: #666666;
      }
    }
  }
  
  .action-buttons {
    flex: 1;
    display: flex;
    margin-left: 30rpx;
    
    button {
      flex: 1;
      height: 80rpx;
      border-radius: 40rpx;
      font-size: 30rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    
    .btn-add-cart {
      background-color: #f5f5f5;
      color: #333333;
      margin-right: 16rpx;
    }
    
    .btn-buy {
      background-color: #333333;
      color: #ffffff;
    }
  }
}
</style>
