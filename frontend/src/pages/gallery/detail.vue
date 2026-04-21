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
          <view class="commission-tag" v-if="commission > 0">
            赚¥{{ commission }}
          </view>
        </view>
      </view>
      
      <view class="author-row">
        <image class="author-avatar" :src="detail.authorAvatar || '/static/images/avatar.png'" @click="goArtistHome"></image>
        <view class="author-info" @click="goArtistHome">
          <text class="author-name">{{ detail.authorName }}</text>
          <view class="identity-tag" :class="'identity-' + detail.authorIdentity">
            {{ getIdentityText(detail.authorIdentity) }}
          </view>
        </view>
        <view class="contact-artist" @click="contactArtist">
          <u-icon name="chat" size="18" color="#667eea"></u-icon>
          <text>联系</text>
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
      
      <!-- 分享赚佣金提示 -->
      <view class="commission-tip" v-if="commission > 0" @click="showShareModal">
        <u-icon name="star" size="18" color="#ff9800"></u-icon>
        <text>分享推广可获得佣金 ¥{{ commission }}</text>
        <text class="tip-more">去分享</text>
        <u-icon name="arrow-right" size="12" color="#ff9800"></u-icon>
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
        <view class="action-item" @click="onShare">
          <text class="iconfont icon-share"></text>
          <text>分享</text>
        </view>
      </view>
      <view class="action-buttons">
        <button class="btn-add-cart" @click="onAddCart">加入购物车</button>
        <button class="btn-buy" @click="onBuy">立即购买</button>
      </view>
    </view>
    
    <!-- 分享面板 -->
    <view class="share-modal" v-if="showSharePanel" @click="showSharePanel = false">
      <view class="share-content" @click.stop>
        <view class="share-title">分享到</view>
        <view class="share-icons">
          <view class="share-icon-item" @click="shareToFriend">
            <image class="share-icon" src="/static/icons/wechat.png" mode="aspectFit"></image>
            <text>微信好友</text>
          </view>
          <view class="share-icon-item" @click="shareToTimeline">
            <image class="share-icon" src="/static/icons/moments.png" mode="aspectFit"></image>
            <text>朋友圈</text>
          </view>
          <view class="share-icon-item" @click="copyLink">
            <image class="share-icon" src="/static/icons/link.png" mode="aspectFit"></image>
            <text>复制链接</text>
          </view>
        </view>
        <view class="share-close" @click="showSharePanel = false">取消</view>
      </view>
    </view>
    
    <!-- 联系艺术家弹窗 -->
    <view class="contact-modal" v-if="showContactModal" @click="showContactModal = false">
      <view class="contact-content" @click.stop>
        <view class="contact-header">
          <text class="contact-title">联系艺术家</text>
          <view class="contact-close" @click="showContactModal = false">
            <u-icon name="close" size="20" color="#999"></u-icon>
          </view>
        </view>
        <view class="contact-artist-info">
          <image class="artist-avatar" :src="detail.authorAvatar || '/static/avatar/default.jpg'"></image>
          <text class="artist-name">{{ detail.authorName }}</text>
        </view>
        <view class="contact-actions">
          <view class="contact-item" @click="sendMessage">
            <u-icon name="chat" size="28" color="#667eea"></u-icon>
            <text>发送消息</text>
          </view>
          <view class="contact-item" @click="makePhoneCall">
            <u-icon name="phone" size="28" color="#50c878"></u-icon>
            <text>拨打电话</text>
          </view>
        </view>
        <view class="contact-phone" v-if="detail.authorPhone">
          <text class="phone-label">艺术家电话</text>
          <text class="phone-value">{{ detail.authorPhone }}</text>
        </view>
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
import { getProductCommission } from '@/api/promoter'

const userStore = useUserStore()

// 状态
const detail = ref({})
const images = ref([])
const currentImageIndex = ref(0)
const storyExpanded = ref(false)
const commission = ref(0)
const showSharePanel = ref(false)
const showContactModal = ref(false)

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
    
    // 获取佣金信息
    loadCommission(id)
  } catch (e) {
    console.error('获取详情失败', e)
  }
}

// 获取佣金
const loadCommission = async (productId) => {
  try {
    const res = await getProductCommission(productId)
    commission.value = res.commission || 0
  } catch (e) {
    // 无佣金权限时不显示
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
    uni.showToast({ title: '操作失败', icon: 'none' })
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
    uni.showToast({ title: '操作失败', icon: 'none' })
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
  showSharePanel.value = true
}

// 分享给好友
const shareToFriend = () => {
  uni.share({
    provider: 'weixin',
    scene: 'WXSceneSession',
    title: detail.value.title,
    imageUrl: images.value[0],
    query: `id=${detail.value.id}&from=share`,
    success: () => {
      uni.showToast({ title: '分享成功', icon: 'success' })
      showSharePanel.value = false
    },
    fail: () => {
      uni.showToast({ title: '分享失败', icon: 'none' })
    }
  })
}

// 分享到朋友圈
const shareToTimeline = () => {
  uni.share({
    provider: 'weixin',
    scene: 'WXSenceTimeline',
    title: detail.value.title,
    imageUrl: images.value[0],
    query: `id=${detail.value.id}&from=share`,
    success: () => {
      uni.showToast({ title: '分享成功', icon: 'success' })
      showSharePanel.value = false
    },
    fail: () => {
      uni.showToast({ title: '分享失败', icon: 'none' })
    }
  })
}

// 复制链接
const copyLink = () => {
  const link = `${getApp().globalData.domain || ''}/pages/gallery/detail?id=${detail.value.id}&from=share`
  uni.setClipboardData({
    data: link,
    success: () => {
      uni.showToast({ title: '链接已复制', icon: 'success' })
      showSharePanel.value = false
    }
  })
}

// 显示分享提示
const showShareModal = () => {
  showSharePanel.value = true
}

// 联系艺术家
const contactArtist = () => {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  showContactModal.value = true
}

// 发送消息
const sendMessage = () => {
  showContactModal.value = false
  uni.navigateTo({
    url: `/pages/chat/index?userId=${detail.value.authorId}`
  })
}

// 拨打电话
const makePhoneCall = () => {
  if (detail.value.authorPhone) {
    uni.makePhoneCall({
      phoneNumber: detail.value.authorPhone
    })
  } else {
    uni.showToast({ title: '暂无电话号码', icon: 'none' })
  }
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
  top: 720rpx;
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
      position: relative;
      
      .iconfont {
        font-size: 40rpx;
        margin-bottom: 4rpx;
      }
      
      .commission-tag {
        position: absolute;
        top: -10rpx;
        right: -10rpx;
        background: linear-gradient(135deg, #ff9800, #ffb74d);
        color: #fff;
        font-size: 18rpx;
        padding: 4rpx 8rpx;
        border-radius: 10rpx;
        white-space: nowrap;
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
    
    .contact-artist {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 8rpx 16rpx;
      margin-right: 16rpx;
      background: #f5f5f5;
      border-radius: 20rpx;
      
      text {
        font-size: 20rpx;
        color: #667eea;
        margin-top: 4rpx;
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
  
  .commission-tip {
    display: flex;
    align-items: center;
    margin-top: 20rpx;
    padding: 16rpx 20rpx;
    background: linear-gradient(90deg, rgba(255, 152, 0, 0.1), rgba(255, 183, 77, 0.1));
    border-radius: 12rpx;
    
    text {
      font-size: 24rpx;
      color: #ff9800;
      margin-left: 10rpx;
    }
    
    .tip-more {
      margin-left: auto;
      font-weight: 600;
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

// 分享面板
.share-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  
  .share-content {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: #fff;
    border-radius: 24rpx 24rpx 0 0;
    padding: 40rpx 30rpx;
    padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
    
    .share-title {
      text-align: center;
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
      margin-bottom: 40rpx;
    }
    
    .share-icons {
      display: flex;
      justify-content: center;
      gap: 80rpx;
      margin-bottom: 40rpx;
      
      .share-icon-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        
        .share-icon {
          width: 100rpx;
          height: 100rpx;
          margin-bottom: 16rpx;
        }
        
        text {
          font-size: 24rpx;
          color: #666;
        }
      }
    }
    
    .share-close {
      text-align: center;
      padding: 20rpx;
      font-size: 28rpx;
      color: #999;
      background: #f5f5f5;
      border-radius: 16rpx;
    }
  }
}

// 联系艺术家弹窗
.contact-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  
  .contact-content {
    width: 600rpx;
    background: #fff;
    border-radius: 24rpx;
    padding: 40rpx;
    
    .contact-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 30rpx;
      
      .contact-title {
        font-size: 32rpx;
        font-weight: 600;
        color: #333;
      }
      
      .contact-close {
        padding: 10rpx;
      }
    }
    
    .contact-artist-info {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 40rpx;
      
      .artist-avatar {
        width: 100rpx;
        height: 100rpx;
        border-radius: 50%;
        margin-bottom: 16rpx;
      }
      
      .artist-name {
        font-size: 30rpx;
        font-weight: 600;
        color: #333;
      }
    }
    
    .contact-actions {
      display: flex;
      gap: 40rpx;
      margin-bottom: 30rpx;
      
      .contact-item {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 30rpx;
        background: #f5f5f5;
        border-radius: 16rpx;
        
        text {
          font-size: 26rpx;
          color: #333;
          margin-top: 12rpx;
        }
      }
    }
    
    .contact-phone {
      text-align: center;
      
      .phone-label {
        display: block;
        font-size: 22rpx;
        color: #999;
        margin-bottom: 8rpx;
      }
      
      .phone-value {
        font-size: 28rpx;
        color: #333;
      }
    }
  }
}
</style>