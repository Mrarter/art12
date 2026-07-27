<template>
  <view class="artist-detail-page">
    <view class="hero">
      <image class="hero-bg" src="../static/artist-ui/artist-homepage-dark.png" mode="aspectFill"></image>
      <view class="hero-mask"></view>
      <view class="topbar">
        <text class="back" @click="goBack">‹</text>
        <text class="title">艺术家主页</text>
        <text class="more">•••</text>
      </view>
      <view class="artist-card">
        <image class="avatar" :src="artist.avatar" mode="aspectFill"></image>
        <view class="artist-main">
          <view class="name-row">
            <text class="artist-name">{{ artist.name }}</text>
            <text class="cert">平台认证</text>
          </view>
          <text class="artist-title">{{ artist.title }}</text>
          <view class="tags">
            <text v-for="tag in artist.tags" :key="tag" class="tag">{{ tag }}</text>
          </view>
        </view>
      </view>
      <view class="stats">
        <view v-for="item in stats" :key="item.label" class="stat-item">
          <text class="stat-value">{{ item.value }}</text>
          <text class="stat-label">{{ item.label }}</text>
        </view>
      </view>
      <view class="hero-actions">
        <button class="ghost-btn" @click="toast('已关注艺术家')">关注</button>
        <button class="gold-btn" @click="consult">咨询顾问</button>
      </view>
    </view>

    <view class="section">
      <view class="section-title">艺术家介绍</view>
      <text class="intro">{{ artist.intro }}</text>
    </view>

    <view class="section works-section">
      <view class="section-head">
        <text class="section-title">代表作品</text>
        <text class="link" @click="goWorks">查看全部</text>
      </view>
      <scroll-view scroll-x class="work-scroll">
        <view class="work-row">
          <view v-for="work in works" :key="work.id" class="work-card" @click="goWork(work.id)">
            <image class="work-image" :src="work.cover" mode="aspectFill"></image>
            <view class="work-info">
              <text class="work-title">{{ work.title }}</text>
              <text class="work-meta">{{ work.material }} / {{ work.size }} / {{ work.year }}</text>
              <text class="price">{{ work.priceText }}</text>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="entry-grid">
      <view class="entry-card" @click="goGallery">
        <text class="entry-kicker">Personal Gallery</text>
        <text class="entry-title">个人美术馆</text>
        <text class="entry-desc">进入艺术家的沉浸式展厅</text>
      </view>
      <view class="entry-card" @click="goCirculation">
        <text class="entry-kicker">Circulation</text>
        <text class="entry-title">作品流通</text>
        <text class="entry-desc">查看在售、收藏与再流通</text>
      </view>
      <view class="entry-card wide" @click="goTrust">
        <text class="entry-kicker">Trust</text>
        <text class="entry-title">收藏信任</text>
        <text class="entry-desc">平台认证、证书、保管与顾问服务</text>
      </view>
    </view>

    <view class="bottom-cta">
      <button class="ghost-btn" @click="goWorks">查看全部作品</button>
      <button class="gold-btn" @click="consult">发起收藏咨询</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      artist: {
        id: 10001,
        name: '孟儒',
        title: '当代油画艺术家',
        avatar: '/static/images/avatar.png',
        tags: ['签约艺术家', '青年艺术家', '布面油画'],
        intro: '孟儒长期关注日常光线与静物之间的情绪张力，以厚涂油画语言重构熟悉的生活片段。作品强调可收藏、可溯源与持续流通的艺术家品牌价值。'
      },
      works: [
        { id: 49, title: '晨曦·归航', material: '布面油画', size: '100×80cm', year: '2024', priceText: '¥8,000', cover: '/pages/artist/static/artist-ui/personal-gallery.png' },
        { id: 47, title: '秋日', material: '布面油画', size: '80×60cm', year: '2024', priceText: '¥12,000', cover: '/pages/artist/static/artist-ui/circulation-entry.png' },
        { id: 46, title: '静物 No.0751', material: '布面油画', size: '40×40cm', year: '2024', priceText: '¥3,200', cover: '/pages/artist/static/artist-ui/collection-trust.png' }
      ]
    }
  },
  onLoad(options = {}) {
    const artistId = options.id || options.userId || options.artistId
    if (artistId) {
      this.artist.id = artistId
    }
  },
  computed: {
    stats() {
      return [
        { label: '作品', value: '36' },
        { label: '喜欢', value: '128' },
        { label: '粉丝', value: '2381' }
      ]
    }
  },
  methods: {
    goBack() { uni.navigateBack() },
    goGallery() { uni.navigateTo({ url: `/pages/artist/gallery/index?id=${this.artist.id}` }) },
    goCirculation() { uni.navigateTo({ url: `/pages/artist/circulation/index?id=${this.artist.id}` }) },
    goTrust() { uni.navigateTo({ url: `/pages/artist/trust/index?id=${this.artist.id}` }) },
    goWorks() { uni.navigateTo({ url: `/pages/artist/works/index?id=${this.artist.id}` }) },
    goWork(id) { uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` }) },
    consult() { uni.showToast({ title: '已为你连接收藏顾问', icon: 'none' }) },
    toast(title) { uni.showToast({ title, icon: 'none' }) }
  }
}
</script>

<style lang="scss" scoped>
$page-bg: #050505;
$card-bg: #171717;
$card-bg-2: #202020;
$text-main: #fff;
$text-secondary: #b7b7b7;
$text-muted: #7d7d7d;
$gold: #d6a827;
$gold-light: #f1c84b;
$border-dark: rgba(214, 168, 39, 0.35);

.artist-detail-page {
  min-height: 100vh;
  padding-bottom: 150rpx;
  background: $page-bg;
  color: $text-main;
}

.hero {
  position: relative;
  min-height: 760rpx;
  padding: 0 28rpx 34rpx;
  overflow: hidden;
}

.hero-bg,
.hero-mask {
  position: absolute;
  inset: 0;
}

.hero-bg {
  width: 100%;
  height: 100%;
}

.hero-mask {
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.15), rgba(5, 5, 5, 0.74) 48%, #050505 100%);
}

.topbar,
.artist-card,
.stats,
.hero-actions {
  position: relative;
  z-index: 1;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 96rpx;
  padding-top: 20rpx;
}

.back,
.more {
  width: 72rpx;
  color: $text-main;
  font-size: 44rpx;
}

.more {
  text-align: right;
  font-size: 30rpx;
}

.title {
  font-size: 32rpx;
  font-weight: 700;
}

.artist-card {
  display: flex;
  align-items: flex-end;
  gap: 24rpx;
  margin-top: 310rpx;
}

.avatar {
  width: 132rpx;
  height: 132rpx;
  border: 3rpx solid rgba(241, 200, 75, 0.9);
  border-radius: 50%;
}

.artist-main {
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.artist-name {
  font-size: 44rpx;
  font-weight: 700;
}

.cert,
.tag {
  flex-shrink: 0;
  white-space: nowrap;
  padding: 6rpx 14rpx;
  border: 1rpx solid $border-dark;
  border-radius: 999rpx;
  color: $gold-light;
  font-size: 22rpx;
}

.artist-title,
.intro,
.entry-desc,
.work-meta {
  color: $text-secondary;
  font-size: 26rpx;
  line-height: 40rpx;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 16rpx;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 34rpx;
}

.stat-item {
  padding: 24rpx 10rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 24rpx;
  background: rgba(23, 23, 23, 0.78);
  text-align: center;
}

.stat-value {
  display: block;
  color: $gold-light;
  font-size: 34rpx;
  font-weight: 700;
}

.stat-label {
  color: $text-muted;
  font-size: 22rpx;
}

.hero-actions,
.bottom-cta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
  margin-top: 28rpx;
}

.ghost-btn,
.gold-btn {
  height: 82rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
}

.ghost-btn {
  border: 1rpx solid $border-dark;
  background: rgba(23, 23, 23, 0.88);
  color: $gold-light;
}

.gold-btn {
  background: linear-gradient(135deg, $gold-light, $gold);
  color: #1a1203;
  font-weight: 700;
}

.section,
.entry-grid {
  margin: 28rpx;
}

.section {
  padding: 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.07);
  border-radius: 28rpx;
  background: linear-gradient(180deg, $card-bg-2, $card-bg);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  display: block;
  margin-bottom: 20rpx;
  font-size: 34rpx;
  font-weight: 700;
}

.link {
  color: $gold-light;
  font-size: 24rpx;
}

.work-scroll {
  width: 100%;
  white-space: nowrap;
}

.work-row {
  display: inline-flex;
  gap: 20rpx;
}

.work-card {
  width: 290rpx;
  overflow: hidden;
  border-radius: 24rpx;
  background: #111;
}

.work-image {
  width: 290rpx;
  height: 260rpx;
}

.work-info {
  padding: 18rpx;
}

.work-title,
.price {
  display: block;
  font-weight: 700;
}

.work-title {
  font-size: 28rpx;
}

.price {
  margin-top: 10rpx;
  color: $gold-light;
  font-size: 32rpx;
}

.entry-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}

.entry-card {
  min-height: 190rpx;
  padding: 26rpx;
  border: 1rpx solid $border-dark;
  border-radius: 28rpx;
  background: radial-gradient(circle at 20% 0%, rgba(214, 168, 39, 0.18), transparent 45%), #171717;
}

.entry-card.wide {
  grid-column: span 2;
}

.entry-kicker {
  display: block;
  color: $gold;
  font-size: 20rpx;
}

.entry-title {
  display: block;
  margin: 16rpx 0 10rpx;
  font-size: 32rpx;
  font-weight: 700;
}

.bottom-cta {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  margin: 0;
  padding: 20rpx 28rpx 34rpx;
  background: rgba(5, 5, 5, 0.96);
}
</style>
