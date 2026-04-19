<template>
  <view class="artist-home">
    <!-- 头部信息 -->
    <view class="artist-header">
      <image class="header-bg" src="/static/banner/artist-bg.jpg" mode="aspectFill"></image>
      <view class="header-content">
        <view class="artist-info">
          <image class="artist-avatar" :src="artistInfo.avatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
          <view class="artist-detail">
            <view class="artist-name">
              {{ artistInfo.name }}
              <view class="identity-tag">{{ getIdentityLabel(artistInfo.identityType) }}</view>
            </view>
            <text class="artist-signature">{{ artistInfo.signature || '暂无签名' }}</text>
          </view>
        </view>
        <view class="artist-stats">
          <view class="stat-item">
            <text class="stat-value">{{ artistInfo.worksCount }}</text>
            <text class="stat-label">作品</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ artistInfo.fansCount }}</text>
            <text class="stat-label">粉丝</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ artistInfo.followCount }}</text>
            <text class="stat-label">关注</text>
          </view>
        </view>
        <view class="artist-actions">
          <view class="action-btn btn-follow" v-if="!artistInfo.isFollowed" @click="followArtist">
            + 关注
          </view>
          <view class="action-btn btn-followed" v-else @click="unfollowArtist">
            已关注
          </view>
          <view class="action-btn btn-message" @click="sendMessage">
            <u-icon name="chat" size="18" color="#666"></u-icon>
            私信
          </view>
        </view>
      </view>
    </view>

    <!-- 简介 -->
    <view class="intro-section" v-if="artistInfo.intro">
      <view class="section-title">个人简介</view>
      <view class="intro-content" :class="{ collapsed: !introExpanded }">
        {{ artistInfo.intro }}
      </view>
      <view class="intro-toggle" v-if="introExpanded" @click="introExpanded = false">
        <text>收起</text>
        <u-icon name="arrow-up" size="14" color="#666"></u-icon>
      </view>
      <view class="intro-toggle" v-else @click="introExpanded = true">
        <text>展开</text>
        <u-icon name="arrow-down" size="14" color="#666"></u-icon>
      </view>
    </view>

    <!-- 作品列表 -->
    <view class="works-section">
      <view class="section-header">
        <text class="section-title">作品</text>
        <view class="works-count">{{ worksList.length }}件</view>
      </view>
      <view class="works-tabs">
        <view class="tab-item" :class="{ active: worksTab === 'list' }" @click="worksTab = 'list'">
          作品列表
        </view>
        <view class="tab-item" :class="{ active: worksTab === 'gallery' }" @click="worksTab = 'gallery'">
          画廊陈列
        </view>
      </view>
      
      <!-- 列表模式 -->
      <view class="works-list" v-if="worksTab === 'list'">
        <view class="works-grid">
          <view class="work-card" v-for="work in worksList" :key="work.id" @click="goWorkDetail(work.id)">
            <image class="work-image" :src="work.cover" mode="aspectFill"></image>
            <view class="work-info">
              <text class="work-title">{{ work.title }}</text>
              <text class="work-price">¥{{ work.price }}</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 画廊模式 -->
      <view class="works-gallery" v-if="worksTab === 'gallery'">
        <view class="gallery-item" v-for="work in worksList" :key="work.id" @click="goWorkDetail(work.id)">
          <image class="gallery-image" :src="work.cover" mode="aspectFill"></image>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getArtistInfo } from '@/api/user.js'

export default {
  data() {
    return {
      artistId: '',
      artistInfo: {
        identityType: 'artist'
      },
      worksList: [],
      introExpanded: false,
      worksTab: 'list'
    }
  },
  
  onLoad(options) {
    if (options.id) {
      this.artistId = options.id
      this.loadArtistInfo()
    }
  },
  
  methods: {
    async loadArtistInfo() {
      try {
        uni.showLoading({ title: '加载中...' })
        const res = await getArtistInfo(this.artistId)
        this.artistInfo = res
        this.worksList = res.works || []
        uni.hideLoading()
      } catch (e) {
        uni.hideLoading()
        this.loadMockData()
      }
    },
    
    loadMockData() {
      this.artistInfo = {
        id: 1,
        name: '张大千',
        avatar: '/static/avatar/demo.jpg',
        signature: '画坛巨匠，国画大师',
        identityType: 'artist',
        worksCount: 128,
        fansCount: 56000,
        followCount: 320,
        isFollowed: false,
        intro: '张大千（1899-1983），四川内江人，中国著名画家、书法家、诗人。他年轻时即擅长绘画，待人真挚诚恳。绘画方面融合了传统与创新，在山水画方面有突出贡献。'
      }
      
      this.worksList = [
        { id: 1, title: '山水长卷', cover: '/static/product/demo1.jpg', price: 128000 },
        { id: 2, title: '荷花图', cover: '/static/product/demo2.jpg', price: 88000 },
        { id: 3, title: '松云图', cover: '/static/product/demo3.jpg', price: 156000 },
        { id: 4, title: '庐山图', cover: '/static/product/demo4.jpg', price: 280000 }
      ]
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
    
    followArtist() {
      this.artistInfo.isFollowed = true
      this.artistInfo.fansCount++
      uni.showToast({ title: '关注成功', icon: 'success' })
    },
    
    unfollowArtist() {
      uni.showModal({
        title: '提示',
        content: '确定取消关注吗？',
        success: (res) => {
          if (res.confirm) {
            this.artistInfo.isFollowed = false
            this.artistInfo.fansCount--
          }
        }
      })
    },
    
    sendMessage() {
      uni.navigateTo({ url: `/pages/chat/index?userId=${this.artistId}` })
    },
    
    goWorkDetail(id) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.artist-home {
  min-height: 100vh;
  background: #f5f5f5;
}

.artist-header {
  position: relative;
}

.header-bg {
  width: 100%;
  height: 300rpx;
}

.header-content {
  position: relative;
  background: #fff;
  padding: 0 30rpx 30rpx;
  margin-top: -80rpx;
  border-radius: 24rpx 24rpx 0 0;
}

.artist-info {
  display: flex;
  align-items: flex-end;
  margin-bottom: 30rpx;
}

.artist-avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 6rpx solid #fff;
  margin-right: 24rpx;
  margin-top: -70rpx;
}

.artist-detail {
  flex: 1;
}

.artist-name {
  display: flex;
  align-items: center;
  font-size: 36rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 10rpx;
}

.identity-tag {
  font-size: 20rpx;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 4rpx 16rpx;
  border-radius: 6rpx;
  margin-left: 16rpx;
}

.artist-signature {
  font-size: 26rpx;
  color: #999;
}

.artist-stats {
  display: flex;
  margin-bottom: 30rpx;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 36rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 6rpx;
}

.stat-label {
  font-size: 24rpx;
  color: #999;
}

.artist-actions {
  display: flex;
  gap: 20rpx;
}

.action-btn {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  border-radius: 36rpx;
  font-size: 28rpx;
}

.btn-follow {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.btn-followed {
  background: #f5f5f5;
  color: #999;
}

.btn-message {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #666;
}

.intro-section {
  background: #fff;
  padding: 30rpx;
  margin: 20rpx;
  border-radius: 16rpx;
}

.section-title {
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 20rpx;
}

.intro-content {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
}

.intro-content.collapsed {
  max-height: 200rpx;
  overflow: hidden;
}

.intro-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 20rpx;
  font-size: 26rpx;
  color: #666;
}

.works-section {
  background: #fff;
  margin: 0 20rpx 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.works-count {
  font-size: 24rpx;
  color: #999;
}

.works-tabs {
  display: flex;
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 6rpx;
  margin-bottom: 30rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 26rpx;
  color: #666;
  border-radius: 8rpx;
}

.tab-item.active {
  background: #fff;
  color: #333;
  font-weight: 500;
}

.works-grid {
  display: flex;
  flex-wrap: wrap;
  margin: 0 -10rpx;
}

.work-card {
  width: 50%;
  padding: 10rpx;
}

.work-image {
  width: 100%;
  height: 300rpx;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
}

.work-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.work-title {
  font-size: 26rpx;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-price {
  font-size: 26rpx;
  color: #e74c3c;
  font-weight: 600;
}

.works-gallery {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.gallery-image {
  width: 100%;
  height: 400rpx;
  border-radius: 12rpx;
}
</style>
