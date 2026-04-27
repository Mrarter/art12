<template>
  <view class="detail-page">
    <!-- 顶部导航栏 -->
    <view class="nav-bar">
      <view class="nav-back" @click="goBack">
        
      </view>
      <view class="nav-actions">
        <view class="nav-action" @click="onShare">
          
        </view>
      </view>
    </view>
    
    <!-- 图片轮播 -->
    <swiper class="image-swiper" indicator-dots @change="onSwiperChange">
      <swiper-item v-for="(img, index) in images" :key="index">
        <image class="product-image" :src="img" mode="aspectFit" @click="previewImage(index)"></image>
      </swiper-item>
    </swiper>
    
    <view class="image-indicator">{{ currentImageIndex + 1 }}/{{ images.length }}</view>
    
    <!-- 视频按钮 -->
    <view class="video-btn" v-if="detail.videoUrl" @click="playVideo">
      <text class="play-icon">▶</text>
      <text>观看视频</text>
    </view>
    
    <!-- 商品信息卡片 -->
    <view class="product-card">
      <view class="product-title">{{ detail.title }}</view>
      
      <view class="author-row">
        <image class="author-avatar" :src="detail.authorAvatar || '/static/avatar/default.png'" @click="goArtistHome"></image>
        <view class="author-info" @click="goArtistHome">
          <text class="author-name">{{ detail.authorName }}</text>
          <view class="identity-tag" :class="'identity-' + detail.authorIdentity">
            {{ getIdentityText(detail.authorIdentity) }}
          </view>
        </view>
        <view class="contact-artist" @click="contactArtist">
          
        </view>
        <view class="follow-btn" :class="{ following: detail.isFollowing }" @click="onFollow">
          {{ detail.isFollowing ? '已关注' : '+ 关注' }}
        </view>
      </view>
      
      <view class="price-row">
        <text class="current-price">¥{{ formatPrice(detail.price) }}</text>
        <text class="original-price" v-if="detail.originalPrice">¥{{ formatPrice(detail.originalPrice) }}</text>
      </view>
      
      <view class="price-change" v-if="detail.priceRise > 0">
        <text class="arrow-up">↑</text>
        <text>该作品已累计上涨 +{{ (detail.priceRise * 100).toFixed(1) }}%</text>
      </view>
    </view>
    
    <!-- 基本信息 -->
    <view class="info-section">
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
        <!-- 新增：作品编号和艺术家ID显示 -->
        <view class="info-item" v-if="detail.artworkCode || detail.displayArtworkId">
          <text class="info-label">作品编号</text>
          <text class="info-value id-value">{{ detail.artworkCode || detail.displayArtworkId }}</text>
        </view>
        <view class="info-item" v-if="detail.authorUid || detail.authorId">
          <text class="info-label">艺术家ID</text>
          <text class="info-value id-value">{{ detail.authorUid || detail.authorId }}</text>
        </view>
      </view>
    </view>
    
    <!-- 作品故事 -->
    <view class="story-section" v-if="detail.description">
      <view class="section-title">作品故事</view>
      <view class="story-content" :class="{ expanded: storyExpanded }">
        <text>{{ detail.description }}</text>
      </view>
      <view class="story-toggle" v-if="storyCanExpand" @click="storyExpanded = !storyExpanded">
        <text>{{ storyExpanded ? '收起' : '展开全部' }}</text>
        <text>{{ storyExpanded ? '↑' : '↓' }}</text>
      </view>
    </view>
    
    <!-- 分享赚佣金提示 -->
    <view class="commission-tip" v-if="commission > 0" @click="showShareModal">
      <text class="star-icon">★</text>
      <text class="tip-text">分享推广可获得佣金</text>
      <text class="tip-amount">¥{{ commission }}</text>
      <text class="arrow-icon">›</text>
    </view>
    
    <!-- 底部操作栏 -->
    <view class="action-bar safe-area-bottom">
      <view class="action-icons">
        <view class="action-item" @click="onFavorite">
          <text :class="['icon-heart', detail.isFavorite ? 'active' : '']">{{ detail.isFavorite ? '❤' : '♡' }}</text>
          <text>{{ detail.favoriteCount || 0 }}</text>
        </view>
        <view class="action-item" @click="onShare">
          <text class="icon-share">↗</text>
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
            
          </view>
        </view>
        <view class="contact-artist-info">
          <image class="artist-avatar" :src="detail.authorAvatar || '/static/avatar/default.png'"></image>
          <text class="artist-name">{{ detail.authorName }}</text>
        </view>
        <view class="contact-actions">
          <view class="contact-item" @click="sendMessage">
            <text class="contact-icon">💬</text>
            <text>发送消息</text>
          </view>
          <view class="contact-item" @click="makePhoneCall">
            <text class="contact-icon">📞</text>
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

<script>
import { getProductDetail, addFavorite, removeFavorite } from '@/api/product'
import { addToCart } from '@/api/cart'
import { followArtist, unfollowArtist } from '@/api/user'
import { useUserStore } from '@/store/modules/user'
import { getProductCommission } from '@/api/promoter'

export default {
  data() {
    return {
      detail: {
        images: null,
        videoUrl: null,
        cover: null,
        coverImage: null,
        title: '',
        authorName: '',
        price: 0
      },
      images: [],
      currentImageIndex: 0,
      storyExpanded: false,
      commission: 0,
      showSharePanel: false,
      showContactModal: false
    }
  },
  
  computed: {
    storyCanExpand() {
      return this.detail.description && this.detail.description.length > 100
    }
  },
  
  onLoad() {
    this.fetchDetail()
  },
  
  methods: {
    async fetchDetail() {
      const pages = getCurrentPages()
      const currentPage = pages[pages.length - 1]
      const id = currentPage.options?.id
      
      if (!id) return
      
      try {
        const data = await getProductDetail(id)
        if (data) {
          this.detail = data
          
          // 处理图片显示
          if (data.images && Array.isArray(data.images) && data.images.length > 0) {
            this.images = data.images
          } else if (data.cover) {
            this.images = [data.cover]
          } else if (data.coverImage) {
            this.images = [data.coverImage]
          } else {
            // 使用默认图片
            this.images = ['https://picsum.photos/750/750?random=' + id]
          }
          
          // 商品价格加载完成后计算佣金
          this.loadCommission(id)
        } else {
          this.loadMockData()
          this.loadCommission(id)
        }
      } catch (e) {
        console.error('获取详情失败', e)
        this.loadMockData()
        this.loadCommission(id)
      }
    },
    
    loadMockData() {
      this.detail = {
        id: 1,
        title: '山水长卷',
        authorName: '张大千',
        authorAvatar: 'https://picsum.photos/100/100?random=avatar',
        authorIdentity: 'artist',
        isFollowing: false,
        isFavorite: false,
        favoriteCount: 128,
        price: 128000,
        originalPrice: 150000,
        priceRise: 0.052,
        stock: 1,
        artType: '油画',
        size: '100x200cm',
        material: '布面油画',
        createYear: '2021',
        edition: '原作',
        holdDuration: 180,
        description: '此幅《山水长卷》是张大千先生晚年精品之作，以传统山水画技法为基础，融合了泼墨泼彩的现代表现形式。画面气势恢宏，云雾缭绕间，群山层叠起伏，展现出祖国大好河山的壮丽景象。作品构图疏密有致，色彩丰富而不失雅致，是张大千先生艺术生涯中的代表作之一。',
        videoUrl: ''
      }
      this.images = ['https://picsum.photos/750/750?random=artwork1']
    },
    
    async loadCommission(productId) {
      try {
        const res = await getProductCommission(productId)
        // 优先使用后端返回的佣金比例，否则使用商品的佣金比例，默认 5%
        const rate = res.commissionRate || res.rate || this.detail.commissionRate || 5
        // price 单位是"分"，需要转为"元"显示
        const priceYuan = (this.detail.price || 0) / 100
        this.commission = Math.floor(priceYuan * rate) / 100
      } catch (e) {
        // API 失败时，根据商品价格计算佣金（默认 5%）
        const rate = this.detail.commissionRate || 5
        const priceYuan = (this.detail.price || 0) / 100
        this.commission = Math.floor(priceYuan * rate) / 100
      }
    },
    
    goBack() {
      uni.navigateBack()
    },
    
    onSwiperChange(e) {
      this.currentImageIndex = e.detail.current
    },
    
    previewImage(index) {
      uni.previewImage({
        current: index,
        urls: this.images
      })
    },
    
    playVideo() {
      if (this.detail.videoUrl) {
        uni.navigateTo({
          url: `/pages/common/video?url=${encodeURIComponent(this.detail.videoUrl)}`
        })
      }
    },
    
    async onFavorite() {
      const userStore = useUserStore()
      if (!userStore.isLogin) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      
      try {
        if (this.detail.isFavorite) {
          await removeFavorite(this.detail.id)
          this.detail.isFavorite = false
          this.detail.favoriteCount = (this.detail.favoriteCount || 1) - 1
        } else {
          await addFavorite(this.detail.id)
          this.detail.isFavorite = true
          this.detail.favoriteCount = (this.detail.favoriteCount || 0) + 1
        }
      } catch (e) {
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    },
    
    async onFollow() {
      const userStore = useUserStore()
      if (!userStore.isLogin) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      
      try {
        if (this.detail.isFollowing) {
          await unfollowArtist(this.detail.authorId)
          this.detail.isFollowing = false
        } else {
          await followArtist(this.detail.authorId)
          this.detail.isFollowing = true
        }
      } catch (e) {
        this.detail.isFollowing = !this.detail.isFollowing
        uni.showToast({ title: this.detail.isFollowing ? '关注成功' : '取消关注', icon: 'success' })
      }
    },
    
    async onAddCart() {
      const userStore = useUserStore()
      if (!userStore.isLogin) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      
      try {
        await addToCart(this.detail.id)
        uni.showToast({ title: '已加入购物车', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '加入成功', icon: 'success' })
      }
    },
    
    onBuy() {
      const userStore = useUserStore()
      if (!userStore.isLogin) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      
      uni.navigateTo({
        url: `/pages/order/confirm?artworkId=${this.detail.id}`
      })
    },
    
    onShare() {
      this.showSharePanel = true
    },
    
    shareToFriend() {
      uni.share({
        provider: 'weixin',
        scene: 'WXSceneSession',
        title: this.detail.title,
        imageUrl: this.images[0],
        query: `id=${this.detail.id}&from=share`,
        success: () => {
          uni.showToast({ title: '分享成功', icon: 'success' })
          this.showSharePanel = false
        },
        fail: () => {
          uni.showToast({ title: '分享失败', icon: 'none' })
        }
      })
    },
    
    shareToTimeline() {
      uni.share({
        provider: 'weixin',
        scene: 'WXSenceTimeline',
        title: this.detail.title,
        imageUrl: this.images[0],
        query: `id=${this.detail.id}&from=share`,
        success: () => {
          uni.showToast({ title: '分享成功', icon: 'success' })
          this.showSharePanel = false
        },
        fail: () => {
          uni.showToast({ title: '分享失败', icon: 'none' })
        }
      })
    },
    
    copyLink() {
      const link = `${getApp().globalData.domain || ''}/pages/gallery/detail?id=${this.detail.id}&from=share`
      uni.setClipboardData({
        data: link,
        success: () => {
          uni.showToast({ title: '链接已复制', icon: 'success' })
          this.showSharePanel = false
        }
      })
    },
    
    showShareModal() {
      this.showSharePanel = true
    },
    
    contactArtist() {
      const userStore = useUserStore()
      if (!userStore.isLogin) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      this.showContactModal = true
    },
    
    sendMessage() {
      this.showContactModal = false
      uni.navigateTo({
        url: `/pages/message/chat?userId=${this.detail.authorId}`
      })
    },
    
    makePhoneCall() {
      if (this.detail.authorPhone) {
        uni.makePhoneCall({
          phoneNumber: this.detail.authorPhone
        })
      } else {
        uni.showToast({ title: '暂无电话号码', icon: 'none' })
      }
    },
    
    goArtistHome() {
      uni.navigateTo({
        url: `/pages/artist/home?userId=${this.detail.authorId}`
      })
    },
    
    formatPrice(price) {
      if (!price) return '0'
      const yuan = price / 100  // 分转元
      if (yuan >= 10000) {
        return (yuan / 10000).toFixed(yuan % 10000 === 0 ? 0 : 1) + '万'
      }
      return yuan.toLocaleString()
    },
    
    formatHoldDuration(days) {
      if (days < 30) return `${days}天`
      if (days < 365) return `${Math.floor(days / 30)}个月`
      return `${(days / 365).toFixed(1)}年`
    },
    
    getIdentityText(identity) {
      const map = {
        artist: '艺术家',
        agent: '经纪人',
        promoter: '艺荐官',
        collector: '持有者'
      }
      return map[identity] || ''
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

.detail-page {
  min-height: 100vh;
  background: $bg-primary;
  padding-bottom: 140rpx;
}

// 顶部导航栏
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30rpx;
  z-index: 100;
  background: linear-gradient(to bottom, rgba(0,0,0,0.6), transparent);
  
  .nav-back, .nav-action {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.4);
    border-radius: 50%;
  }
  
  .nav-actions {
    display: flex;
    gap: 20rpx;
  }
}

// 图片轮播
.image-swiper {
  height: 750rpx;
  background: $bg-card;
  
  .product-image {
    width: 100%;
    height: 100%;
  }
}

.image-indicator {
  position: absolute;
  right: 30rpx;
  top: 680rpx;
  padding: 8rpx 20rpx;
  background: rgba(0, 0, 0, 0.6);
  color: $text-primary;
  font-size: 24rpx;
  border-radius: 20rpx;
}

.video-btn {
  position: absolute;
  right: 30rpx;
  top: 610rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  background: rgba(0, 0, 0, 0.6);
  color: $text-primary;
  font-size: 24rpx;
  border-radius: 30rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.2);
  
  .play-icon {
    font-size: 20rpx;
  }
}

// 商品信息卡片
.product-card {
  margin: 24rpx;
  padding: 32rpx;
  background: $bg-card;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
}

.product-title {
  font-size: 36rpx;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 24rpx;
  line-height: 1.4;
}

.author-row {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  
  .author-avatar {
    width: 56rpx;
    height: 56rpx;
    border-radius: 50%;
    margin-right: 16rpx;
    background: $bg-elevated;
  }
  
  .author-info {
    flex: 1;
    
    .author-name {
      font-size: 26rpx;
      color: $text-primary;
      margin-bottom: 4rpx;
    }
    
    .identity-tag {
      display: inline-block;
      padding: 4rpx 12rpx;
      font-size: 18rpx;
      border-radius: 16rpx;
      background: rgba(201, 162, 39, 0.15);
      color: $accent-gold;
    }
  }
  
  .contact-artist {
    width: 56rpx;
    height: 56rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(201, 162, 39, 0.1);
    border: 1rpx solid rgba(201, 162, 39, 0.3);
    border-radius: 50%;
    margin-right: 16rpx;
  }
  
  .follow-btn {
    padding: 12rpx 28rpx;
    font-size: 24rpx;
    background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
    color: $bg-primary;
    border-radius: 30rpx;
    font-weight: 500;
    
    &.following {
      background: $bg-elevated;
      color: $text-secondary;
    }
  }
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 12rpx;
  
  .current-price {
    font-size: 44rpx;
    font-weight: 600;
    color: $accent-gold;
    
    &::before {
      content: '¥';
      font-size: 28rpx;
    }
  }
  
  .original-price {
    font-size: 28rpx;
    color: $text-muted;
    text-decoration: line-through;
    margin-left: 16rpx;
    
    &::before {
      content: '¥';
      font-size: 20rpx;
    }
  }
}

.price-change {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 16rpx;
  background: rgba(231, 76, 60, 0.15);
  border-radius: 8rpx;
  font-size: 22rpx;
  color: #e74c3c;
  
  .arrow-up {
    font-size: 16rpx;
  }
}

// 基本信息
.info-section {
  margin: 0 24rpx 24rpx;
  padding: 32rpx;
  background: $bg-card;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
  
  .section-title {
    font-size: 28rpx;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 24rpx;
  }
  
  .info-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20rpx;
    
    .info-item {
      display: flex;
      
      .info-label {
        width: 140rpx;
        font-size: 24rpx;
        color: $text-muted;
      }
      
      .info-value {
        flex: 1;
        font-size: 24rpx;
        color: $text-secondary;
        
        &.id-value {
          font-family: 'Courier New', monospace;
          color: $accent-gold;
          font-weight: 600;
          letter-spacing: 2rpx;
        }
      }
    }
  }
}

// 作品故事
.story-section {
  margin: 0 24rpx 24rpx;
  padding: 32rpx;
  background: $bg-card;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
  
  .section-title {
    font-size: 28rpx;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 24rpx;
  }
  
  .story-content {
    font-size: 26rpx;
    color: $text-secondary;
    line-height: 1.8;
    
    text {
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 3;
      overflow: hidden;
    }
    
    &.expanded text {
      -webkit-line-clamp: unset;
      overflow: visible;
      display: block;
    }
  }
  
  .story-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    margin-top: 20rpx;
    font-size: 24rpx;
    color: $accent-gold;
  }
}

// 佣金提示
.commission-tip {
  display: flex;
  align-items: center;
  margin: 0 24rpx 24rpx;
  padding: 20rpx 24rpx;
  background: linear-gradient(135deg, rgba(201, 162, 39, 0.15), rgba(201, 162, 39, 0.05));
  border: 1rpx solid rgba(201, 162, 39, 0.3);
  border-radius: 16rpx;
  
  .star-icon {
    font-size: 16rpx;
    color: $accent-gold;
  }
  
  .tip-text {
    font-size: 24rpx;
    color: $text-secondary;
    margin-left: 12rpx;
  }
  
  .tip-amount {
    flex: 1;
    font-size: 28rpx;
    color: $accent-gold;
    font-weight: 600;
    margin-left: 12rpx;
  }
  
  .arrow-icon {
    font-size: 20rpx;
    color: $accent-gold;
  }
}

// 底部操作栏
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  padding: 16rpx 30rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: $bg-card;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
  
  .action-icons {
    display: flex;
    gap: 40rpx;
    
    .action-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      
      .icon-heart {
        font-size: 26rpx;
        color: #999;
        &.active {
          color: #c9a227;
        }
      }
      
      .icon-share {
        font-size: 24rpx;
        color: #999;
      }
      
      text {
        font-size: 20rpx;
        color: $text-muted;
        margin-top: 4rpx;
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
      font-size: 28rpx;
      font-weight: 500;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    
    .btn-add-cart {
      background: $bg-elevated;
      color: $text-primary;
      margin-right: 16rpx;
      border: 1rpx solid rgba(255, 255, 255, 0.1);
    }
    
    .btn-buy {
      background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
      color: $bg-primary;
      box-shadow: 0 4rpx 16rpx rgba(201, 162, 39, 0.3);
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
  background: rgba(0, 0, 0, 0.6);
  z-index: 999;
  
  .share-content {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: $bg-card;
    border-radius: 24rpx 24rpx 0 0;
    padding: 40rpx 30rpx;
    padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
    
    .share-title {
      text-align: center;
      font-size: 30rpx;
      font-weight: 600;
      color: $text-primary;
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
          color: $text-secondary;
        }
      }
    }
    
    .share-close {
      text-align: center;
      padding: 20rpx;
      font-size: 28rpx;
      color: $text-muted;
      background: $bg-elevated;
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
  background: rgba(0, 0, 0, 0.6);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  
  .contact-content {
    width: 600rpx;
    background: $bg-card;
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
        color: $text-primary;
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
        background: $bg-elevated;
      }
      
      .artist-name {
        font-size: 30rpx;
        font-weight: 600;
        color: $text-primary;
      }
    }
    
    .contact-actions {
      display: flex;
      gap: 30rpx;
      margin-bottom: 30rpx;
      
      .contact-item {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 30rpx;
        background: $bg-elevated;
        border-radius: 16rpx;
        
        .contact-icon {
          font-size: 32rpx;
        }
        
        text {
          font-size: 24rpx;
          color: $text-secondary;
          margin-top: 12rpx;
        }
      }
    }
    
    .contact-phone {
      text-align: center;
      
      .phone-label {
        display: block;
        font-size: 22rpx;
        color: $text-muted;
        margin-bottom: 8rpx;
      }
      
      .phone-value {
        font-size: 28rpx;
        color: $text-primary;
      }
    }
  }
}
</style>
