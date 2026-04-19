<template>
  <view class="artcircle-page">
    <!-- 顶部Tab -->
    <view class="header-tabs">
      <view class="tab-item" :class="{ active: currentTab === 'follow' }" @click="switchTab('follow')">
        关注
      </view>
      <view class="tab-item" :class="{ active: currentTab === 'hot' }" @click="switchTab('hot')">
        热门
      </view>
      <view class="tab-item" :class="{ active: currentTab === 'latest' }" @click="switchTab('latest')">
        最新
      </view>
    </view>

    <!-- 内容列表 -->
    <scroll-view class="content-list" scroll-y @scrolltolower="loadMore">
      <view class="post-card" v-for="post in postList" :key="post.id">
        <!-- 用户信息 -->
        <view class="post-header">
          <image class="user-avatar" :src="post.userAvatar" mode="aspectFill"></image>
          <view class="user-info">
            <view class="user-name">
              {{ post.userName }}
              <view class="identity-badge" v-if="post.identityType">{{ getIdentityLabel(post.identityType) }}</view>
            </view>
            <text class="post-time">{{ post.createTime }}</text>
          </view>
          <view class="follow-btn" v-if="!post.isFollowed" @click="followUser(post)">+ 关注</view>
        </view>
        
        <!-- 内容 -->
        <view class="post-content">
          <text class="content-text">{{ post.content }}</text>
          <!-- 图片 -->
          <view class="content-images" v-if="post.images && post.images.length > 0" :class="'images-' + post.images.length">
            <image 
              v-for="(img, idx) in post.images" 
              :key="idx" 
              :src="img" 
              mode="aspectFill"
              @click="previewImages(post.images, idx)"
            ></image>
          </view>
        </view>
        
        <!-- 作品关联 -->
        <view class="post-product" v-if="post.product" @click="goProduct(post.product.id)">
          <image class="product-image" :src="post.product.cover" mode="aspectFill"></image>
          <view class="product-info">
            <text class="product-title">{{ post.product.title }}</text>
            <text class="product-price">¥{{ post.product.price }}</text>
          </view>
        </view>
        
        <!-- 互动栏 -->
        <view class="interaction-bar">
          <view class="interaction-item" @click="toggleLike(post)">
            <u-icon :name="post.isLiked ? 'thumb-up-fill' : 'thumb-up'" :color="post.isLiked ? '#667eea' : '#999'" size="20"></u-icon>
            <text>{{ post.likeCount }}</text>
          </view>
          <view class="interaction-item" @click="goComment(post.id)">
            <u-icon name="chat" size="20" color="#999"></u-icon>
            <text>{{ post.commentCount }}</text>
          </view>
          <view class="interaction-item" @click="sharePost(post)">
            <u-icon name="share" size="20" color="#999"></u-icon>
            <text>分享</text>
          </view>
        </view>
      </view>
      
      <!-- 加载更多 -->
      <view class="load-more" v-if="hasMore">
        <u-loading mode="circle"></u-loading>
        <text>加载中...</text>
      </view>
    </scroll-view>
    
    <!-- 发布按钮 -->
    <view class="publish-btn" @click="goPublish">
      <u-icon name="plus" size="28" color="#fff"></u-icon>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentTab: 'follow',
      postList: [],
      page: 1,
      pageSize: 10,
      hasMore: false
    }
  },
  
  onLoad() {
    this.loadPosts()
  },
  
  methods: {
    async loadPosts() {
      // 模拟数据
      const mockPosts = [
        {
          id: 1,
          userName: '艺术馆',
          userAvatar: '/static/avatar/demo.jpg',
          identityType: 'gallery',
          isFollowed: false,
          content: '今日上新一幅张大千先生的山水长卷，欢迎各位艺术爱好者鉴赏。',
          images: ['/static/product/demo1.jpg', '/static/product/demo2.jpg'],
          product: {
            id: 1,
            title: '山水长卷',
            cover: '/static/product/demo1.jpg',
            price: 128000
          },
          likeCount: 128,
          commentCount: 23,
          isLiked: false,
          createTime: '2小时前'
        },
        {
          id: 2,
          userName: '李老师',
          userAvatar: '/static/avatar/demo.jpg',
          identityType: 'promoter',
          isFollowed: true,
          content: '分享一幅齐白石的虾趣图，栩栩如生，让人叹为观止！',
          images: ['/static/product/demo2.jpg'],
          product: null,
          likeCount: 256,
          commentCount: 45,
          isLiked: true,
          createTime: '5小时前'
        }
      ]
      
      this.postList = mockPosts
      this.hasMore = false
    },
    
    switchTab(tab) {
      this.currentTab = tab
      this.page = 1
      this.postList = []
      this.loadPosts()
    },
    
    loadMore() {
      if (this.hasMore) {
        this.page++
        this.loadPosts()
      }
    },
    
    getIdentityLabel(type) {
      const labels = {
        artist: '艺术家',
        gallery: '画廊',
        dealer: '画商',
        promoter: '艺荐官'
      }
      return labels[type] || ''
    },
    
    followUser(post) {
      post.isFollowed = true
      uni.showToast({ title: '关注成功', icon: 'success' })
    },
    
    toggleLike(post) {
      post.isLiked = !post.isLiked
      post.likeCount += post.isLiked ? 1 : -1
    },
    
    previewImages(images, index) {
      uni.previewImage({
        current: index,
        urls: images
      })
    },
    
    goProduct(id) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    },
    
    goComment(postId) {
      uni.navigateTo({ url: `/pages/artcircle/comment?id=${postId}` })
    },
    
    sharePost(post) {
      uni.showShareMenu({
        withShareTicket: true
      })
    },
    
    goPublish() {
      uni.navigateTo({ url: '/pages/artcircle/publish' })
    }
  }
}
</script>

<style lang="scss" scoped>
.artcircle-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.header-tabs {
  display: flex;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 99;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 30rpx 0;
  font-size: 30rpx;
  color: #666;
  position: relative;
}

.tab-item.active {
  color: #333;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60rpx;
  height: 4rpx;
  background: #333;
  border-radius: 2rpx;
}

.content-list {
  height: calc(100vh - 100rpx);
  padding: 20rpx;
}

.post-card {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.post-header {
  display: flex;
  align-items: center;
  padding: 24rpx;
}

.user-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  margin-right: 20rpx;
}

.user-info {
  flex: 1;
}

.user-name {
  display: flex;
  align-items: center;
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 6rpx;
}

.identity-badge {
  font-size: 18rpx;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2rpx 10rpx;
  border-radius: 4rpx;
  margin-left: 10rpx;
}

.post-time {
  font-size: 22rpx;
  color: #999;
}

.follow-btn {
  padding: 10rpx 24rpx;
  background: #333;
  color: #fff;
  border-radius: 30rpx;
  font-size: 24rpx;
}

.post-content {
  padding: 0 24rpx 24rpx;
}

.content-text {
  font-size: 28rpx;
  color: #333;
  line-height: 1.6;
  margin-bottom: 16rpx;
}

.content-images {
  display: grid;
  gap: 8rpx;
}

.images-1 {
  grid-template-columns: 1fr;
}

.images-2, .images-3 {
  grid-template-columns: repeat(2, 1fr);
}

.images-4 {
  grid-template-columns: repeat(2, 1fr);
}

.content-images image {
  width: 100%;
  height: 300rpx;
  border-radius: 8rpx;
}

.images-1 image {
  height: 400rpx;
}

.post-product {
  display: flex;
  align-items: center;
  margin: 0 24rpx 24rpx;
  padding: 20rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
}

.product-image {
  width: 120rpx;
  height: 120rpx;
  border-radius: 8rpx;
  margin-right: 20rpx;
}

.product-info {
  flex: 1;
}

.product-title {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 10rpx;
}

.product-price {
  font-size: 28rpx;
  color: #e74c3c;
  font-weight: 600;
}

.interaction-bar {
  display: flex;
  padding: 20rpx 24rpx;
  border-top: 1rpx solid #f0f0f0;
}

.interaction-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.interaction-item text {
  margin-left: 10rpx;
  font-size: 24rpx;
  color: #999;
}

.load-more {
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

.publish-btn {
  position: fixed;
  right: 30rpx;
  bottom: 60rpx;
  width: 100rpx;
  height: 100rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 20rpx rgba(102, 126, 234, 0.4);
}
</style>
