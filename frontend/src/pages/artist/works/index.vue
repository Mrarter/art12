<template>
  <view class="works-page">
    <view class="topbar">
      <text class="back" @click="goBack">‹</text>
      <text class="title">艺术家全部作品</text>
      <text class="spacer"></text>
    </view>

    <view class="artist-strip">
      <image class="avatar" src="/static/images/avatar.png" mode="aspectFill"></image>
      <view>
        <text class="artist-name">孟儒</text>
        <text class="artist-desc">36 件作品正在平台展示与流通</text>
      </view>
    </view>

    <scroll-view scroll-x class="tabs-scroll">
      <view class="tabs">
        <text v-for="tab in tabs" :key="tab.value" class="tab" :class="{ active: activeTab === tab.value }" @click="activeTab = tab.value">{{ tab.label }}</text>
      </view>
    </scroll-view>

    <view class="works-grid">
      <view v-for="work in filteredWorks" :key="work.id" class="work-card" @click="goWork(work.id)">
        <image class="work-image" :src="work.cover" mode="aspectFill"></image>
        <view class="work-body">
          <view class="work-line">
            <text class="work-title">{{ work.title }}</text>
            <text class="status">{{ work.statusText }}</text>
          </view>
          <text class="work-meta">{{ work.material }} / {{ work.size }} / {{ work.year }}</text>
          <text class="price">{{ work.priceText }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      activeTab: 'ALL',
      tabs: [
        { label: '全部', value: 'ALL' },
        { label: '可收藏', value: 'ON_SALE' },
        { label: '已收藏', value: 'COLLECTED' },
        { label: '再次流通', value: 'CAN_APPLY' },
        { label: '代表作', value: 'FEATURED' }
      ],
      works: [
        { id: 49, title: '晨曦·归航', material: '布面油画', size: '100×80cm', year: '2024', priceText: '¥8,000', status: 'FEATURED', statusText: '代表作', cover: '/static/artist-ui/personal-gallery.png' },
        { id: 47, title: '秋日', material: '布面油画', size: '80×60cm', year: '2024', priceText: '¥12,000', status: 'ON_SALE', statusText: '可收藏', cover: '/static/artist-ui/artist-homepage-dark.png' },
        { id: 46, title: '静物 No.0751', material: '布面油画', size: '40×40cm', year: '2024', priceText: '¥3,200', status: 'COLLECTED', statusText: '已收藏', cover: '/static/artist-ui/collection-trust.png' },
        { id: 45, title: '海边风景', material: '布面油画', size: '60×60cm', year: '2023', priceText: '待估值', status: 'CAN_APPLY', statusText: '可再次流通', cover: '/static/artist-ui/artist-homepage-alt.png' }
      ]
    }
  },
  computed: {
    filteredWorks() {
      return this.activeTab === 'ALL' ? this.works : this.works.filter(item => item.status === this.activeTab)
    }
  },
  methods: {
    goBack() { uni.navigateBack() },
    goWork(id) { uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` }) }
  }
}
</script>

<style lang="scss" scoped>
$page-bg: #050505;
$card-bg: #171717;
$gold: #d6a827;
$gold-light: #f1c84b;
$text-main: #fff;
$text-secondary: #b7b7b7;
$text-muted: #7d7d7d;
$border-dark: rgba(214, 168, 39, 0.35);

.works-page {
  min-height: 100vh;
  padding: 0 28rpx 50rpx;
  background: $page-bg;
  color: $text-main;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 98rpx;
}

.back,
.spacer {
  width: 72rpx;
}

.back {
  font-size: 44rpx;
}

.title {
  font-size: 32rpx;
  font-weight: 700;
}

.artist-strip {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx;
  border: 1rpx solid $border-dark;
  border-radius: 28rpx;
  background: radial-gradient(circle at 0 0, rgba(214, 168, 39, 0.18), transparent 45%), $card-bg;
}

.avatar {
  width: 92rpx;
  height: 92rpx;
  border-radius: 50%;
}

.artist-name,
.artist-desc {
  display: block;
}

.artist-name {
  font-size: 34rpx;
  font-weight: 700;
}

.artist-desc,
.work-meta {
  color: $text-secondary;
  font-size: 24rpx;
  line-height: 36rpx;
}

.tabs-scroll {
  width: 100%;
  margin: 28rpx 0;
  white-space: nowrap;
}

.tabs {
  display: inline-flex;
  gap: 16rpx;
}

.tab {
  padding: 16rpx 26rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.09);
  border-radius: 999rpx;
  color: $text-muted;
  font-size: 24rpx;
}

.tab.active {
  border-color: $border-dark;
  color: $gold-light;
  background: rgba(214, 168, 39, 0.12);
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.work-card {
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.07);
  border-radius: 24rpx;
  background: $card-bg;
}

.work-image {
  width: 100%;
  height: 280rpx;
}

.work-body {
  padding: 18rpx;
}

.work-line {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10rpx;
}

.work-title {
  font-size: 28rpx;
  font-weight: 700;
}

.status {
  flex-shrink: 0;
  color: $gold-light;
  font-size: 20rpx;
}

.price {
  display: block;
  margin-top: 12rpx;
  color: $gold-light;
  font-size: 32rpx;
  font-weight: 700;
}
</style>
