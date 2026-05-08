<template>
  <view class="artist-home-page">
    <view class="top-nav">
      <view class="nav-icon" @click="goBack">
        <image src="/static/art-icons/icon-back.svg" mode="aspectFit"></image>
      </view>
      <view class="nav-title">{{ artist.name }}</view>
      <view class="nav-icon external" @click="shareArtist">
        <image src="/static/art-icons/icon-share.svg" mode="aspectFit"></image>
      </view>
    </view>

    <view class="profile-hero">
      <image class="cover-image" :src="artist.cover" mode="aspectFill"></image>
      <view class="cover-shade"></view>

      <view class="profile-core">
        <view class="avatar-wrap">
          <image class="avatar" :src="artist.avatar" mode="aspectFill"></image>
          <view class="avatar-cert">✓</view>
        </view>
        <view class="identity-block">
          <view class="artist-name">{{ artist.name }}</view>
          <view class="artist-title">{{ artist.title }}</view>
          <view class="tag-row">
            <view class="tag">
              <image class="tag-icon-img" src="/static/art-icons/icon-verify.svg" mode="aspectFit"></image>
              <text>平台认证</text>
            </view>
            <view class="tag">
              <image class="tag-icon-img" src="/static/art-icons/icon-star.svg" mode="aspectFit"></image>
              <text>签约艺术家</text>
            </view>
            <view class="tag">青年艺术家</view>
            <view class="tag">布面油画</view>
          </view>
        </view>
      </view>
    </view>

    <view class="stats-actions">
      <view class="stats">
        <view class="stat-item" v-for="item in stats" :key="item.label">
          <text class="stat-value">{{ item.value }}</text>
          <text class="stat-label">{{ item.label }}</text>
        </view>
      </view>
      <view class="hero-actions">
        <button class="gold-btn" @click="followArtist">{{ followed ? '已关注' : '关注' }}</button>
        <button class="outline-btn" @click="consult">咨询顾问</button>
      </view>
    </view>

    <view class="intro-card">
      <view class="section-title gold-title">艺术家介绍</view>
      <view class="intro-text" :class="{ expanded: introExpanded }">
        {{ artist.intro }}
      </view>
      <view class="quote">“ 我试图用色彩记录内心的风景，让观者在画面中找到属于自己的记忆。 ”</view>
      <view class="expand" @click="introExpanded = !introExpanded">
        {{ introExpanded ? '收起' : '展开' }} <text>{{ introExpanded ? '⌃' : '⌄' }}</text>
      </view>
    </view>

    <view class="section works-section">
      <view class="section-head">
        <view class="section-title">代表作品</view>
        <view class="more-link" @click="goWorks">查看全部 ›</view>
      </view>
      <view class="work-scroll">
        <view class="work-row">
          <view class="work-card" v-for="work in works" :key="work.id" @click="goWork(work.id)">
            <view class="work-image-wrap">
              <image class="work-image" :src="work.cover" mode="aspectFill"></image>
              <view class="new-badge">新</view>
            </view>
            <view class="work-info">
              <view class="work-title">{{ work.title }}</view>
              <view class="work-meta">{{ work.material }} / {{ work.size }} / {{ work.year }}</view>
              <view class="work-bottom">
                <text class="price">{{ work.priceText }}</text>
                <text class="collect-tag">可收藏</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="section circulation-section">
      <view class="section-title">作品流通入口</view>
      <view class="flow-list">
        <view class="flow-item" v-for="work in flowWorks" :key="work.id" @click="goWork(work.id)">
          <image class="flow-cover" :src="work.cover" mode="aspectFill"></image>
          <view class="flow-main">
            <view class="flow-title">{{ work.title }}</view>
            <view class="flow-price">{{ work.priceText }}</view>
          </view>
          <view class="flow-feature">
            <image src="/static/art-icons/icon-certificate.svg" mode="aspectFit"></image>
            <text>收藏证书</text>
          </view>
          <view class="flow-feature">
            <image src="/static/art-icons/icon-circulation.svg" mode="aspectFit"></image>
            <text>流通记录</text>
          </view>
          <view class="flow-feature">
            <image src="/static/art-icons/icon-verify.svg" mode="aspectFit"></image>
            <text>平台保障</text>
          </view>
          <view class="flow-arrow">›</view>
        </view>
      </view>
    </view>

    <view class="trust-section">
      <view class="section-title">收藏信任</view>
      <view class="trust-row">
        <view class="trust-item" v-for="item in trustItems" :key="item.title">
          <image class="trust-icon" :src="item.icon" mode="aspectFit"></image>
          <view>
            <view class="trust-title">{{ item.title }}</view>
            <view class="trust-desc">{{ item.desc }}</view>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-actions">
      <button class="all-works-btn" @click="goWorks">查看全部作品</button>
      <button class="consult-btn" @click="consult">
        <image src="/static/art-icons/icon-consultant.svg" mode="aspectFit"></image>
        <text>发起收藏咨询</text>
      </button>
    </view>

    <view class="home-indicator"></view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      followed: false,
      introExpanded: false,
      loading: true,
      artist: {
        id: 0,
        name: '',
        title: '',
        avatar: '',
        cover: '',
        intro: ''
      },
      works: [],
      trustItems: [
        { icon: '/static/art-icons/icon-verify.svg', title: '平台认证', desc: '严格审核' },
        { icon: '/static/art-icons/icon-certificate.svg', title: '收藏证书', desc: '权威出具' },
        { icon: '/static/art-icons/icon-circulation.svg', title: '流通记录', desc: '全程可查' },
        { icon: '/static/art-icons/icon-platform-custody.svg', title: '保管支持', desc: '专业保管' },
        { icon: '/static/art-icons/icon-consultant.svg', title: '顾问服务', desc: '专属顾问' }
      ],
      stats: [
        { label: '作品', value: '0' },
        { label: '收藏', value: '0' },
        { label: '粉丝', value: '0' }
      ]
    }
  },

  async onLoad(options = {}) {
    const artistId = options.id || options.userId || options.artistId
    if (artistId) {
      this.artist.id = artistId
      await this.loadArtistData(artistId)
    }
    this.loading = false
  },

  computed: {
    flowWorks() {
      return this.works.slice(0, 2)
    }
  },

  methods: {
    async loadArtistData(artistId) {
      try {
        const res = await getArtistInfo(artistId)
        if (res) {
          const data = res.data || res
          this.artist = {
            id: data.id || data.userId || artistId,
            name: data.nickname || data.name || data.realName || '',
            title: data.title || data.artistTitle || '',
            avatar: data.avatar || data.avatarUrl || '',
            cover: data.cover || data.coverUrl || '',
            intro: data.intro || data.bio || data.resume || ''
          }
          this.works = (data.works || data.artworks || []).map(w => ({
            id: w.id,
            title: w.title || w.name || '',
            material: w.material || '',
            size: w.size || '',
            year: w.year || w.createYear || '',
            priceText: w.priceText || (w.price ? '¥' + this.formatPrice(w.price) : ''),
            cover: w.cover || w.coverImage || w.coverUrl || ''
          }))
          this.stats = [
            { label: '作品', value: String(data.workCount || data.artworkCount || this.works.length || '0') },
            { label: '收藏', value: String(data.collectCount || data.favoriteCount || '0') },
            { label: '粉丝', value: String(data.fansCount || data.followerCount || '0') }
          ]
        }
      } catch (e) {
        console.error('加载艺术家数据失败，使用本地模拟数据', e)
        this.loadFromMock()
      }
    },

    loadFromMock() {
      this.artist.name = '孟儒'
      this.artist.title = '当代油画艺术家'
      this.artist.avatar = '/static/images/artist-avatar.png'
      this.artist.cover = ''
      this.artist.intro = '孟儒长期关注日常光线与空间关系的变化'
      this.works = [
        { id: 49, title: '晨曦·归航', material: '布面油画', size: '100×80cm', year: '2024', priceText: '¥8,000', cover: '/static/images/museum-v12-work-boat.png' },
        { id: 47, title: '秋日', material: '布面油画', size: '80×60cm', year: '2024', priceText: '¥12,000', cover: '/static/images/museum-v12-work-girl.png' },
        { id: 46, title: '静物 No.0751', material: '布面油画', size: '40×40cm', year: '2024', priceText: '¥3,200', cover: '/static/images/museum-v12-work-still.png' }
      ]
      this.stats = [
        { label: '作品', value: '12' },
        { label: '收藏', value: '86' },
        { label: '粉丝', value: '233' }
      ]
    },

    formatPrice(v) {
      if (!v) return '0'
      return String(Math.round(v / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    },
    goBack() {
      const pages = getCurrentPages()
      if (pages && pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.reLaunch({ url: '/pages/index/index' })
      }
    },
    async followArtist() {
      try {
        if (this.followed) {
          await unfollowArtist(this.artist.id)
          this.followed = false
        } else {
          await followArtist(this.artist.id)
          this.followed = true
        }
      } catch (e) {
        console.error('关注操作失败', e)
      }
    },
    shareArtist() {
      uni.showToast({ title: '分享艺术家主页', icon: 'none' })
    },
    consult() { uni.showToast({ title: '已为你连接收藏顾问', icon: 'none' }) },
    goGallery() { uni.navigateTo({ url: `/pages/artist/gallery/index?id=${this.artist.id}` }) },
    goWorks() { uni.navigateTo({ url: `/pages/artist/works/index?id=${this.artist.id}` }) },
    goWork(id) { uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` }) },
    goIndex() { uni.reLaunch({ url: '/pages/index/index' }) },
    goPublish() { uni.navigateTo({ url: '/pages/artist/publish' }) },
    goCart() { uni.navigateTo({ url: '/pages/cart/index' }) },
    goMine() { uni.navigateTo({ url: '/pages/user/index' }) }
  }
}
</script>

<style lang="scss" scoped>
$bg: #050505;
$surface: #151515;
$surface-2: #1e1e1e;
$gold: #d7a51d;
$gold-light: #f3c43d;
$text: #f7f7f7;
$muted: #a7a7a7;
$dim: #6f6f6f;
$line: rgba(255, 255, 255, 0.1);
$gold-line: rgba(215, 165, 29, 0.65);

.artist-home-page {
  min-height: 100vh;
  padding: calc(88rpx + env(safe-area-inset-top)) 24rpx calc(178rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  color: $text;
  background:
    radial-gradient(circle at 70% 5%, rgba(215, 165, 29, 0.12), transparent 26%),
    linear-gradient(180deg, #080808 0%, #040404 100%);
}

.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 40;
  height: calc(88rpx + env(safe-area-inset-top));
  padding: env(safe-area-inset-top) 24rpx 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(5, 5, 5, 0.86);
  backdrop-filter: blur(18rpx);
}

.nav-title {
  font-size: 32rpx;
  font-weight: 800;
}

.nav-icon {
  width: 58rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 58rpx;
  line-height: 1;
}

.nav-icon.external {
  font-size: 40rpx;
}

.profile-hero {
  position: relative;
  height: 288rpx;
  overflow: visible;
  border-radius: 10rpx;
}

.cover-image,
.cover-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border-radius: 10rpx;
}

.cover-shade {
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.02), rgba(0, 0, 0, 0.2) 48%, rgba(0, 0, 0, 0.86) 100%),
    linear-gradient(90deg, rgba(0, 0, 0, 0.45), transparent 42%, rgba(0, 0, 0, 0.12));
}

.profile-core {
  position: absolute;
  left: 42rpx;
  right: 28rpx;
  bottom: -72rpx;
  display: flex;
  align-items: flex-end;
  gap: 26rpx;
}

.avatar-wrap {
  position: relative;
  flex: 0 0 auto;
}

.avatar {
  width: 166rpx;
  height: 166rpx;
  border-radius: 50%;
  border: 4rpx solid $gold-light;
  background: #222;
}

.avatar-cert {
  position: absolute;
  right: 4rpx;
  bottom: 8rpx;
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffdc62, $gold);
  color: #fff;
  font-size: 30rpx;
  font-weight: 900;
}

.identity-block {
  flex: 1;
  min-width: 0;
  padding-bottom: 18rpx;
}

.artist-name {
  font-size: 42rpx;
  line-height: 1.08;
  font-weight: 900;
}

.artist-title {
  margin-top: 8rpx;
  color: #d4d4d4;
  font-size: 26rpx;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx 14rpx;
  margin-top: 14rpx;
}

.tag {
  height: 42rpx;
  padding: 0 16rpx;
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  border: 1rpx solid $gold-line;
  border-radius: 7rpx;
  color: $gold-light;
  background: rgba(0, 0, 0, 0.34);
  font-size: 22rpx;
  font-weight: 700;
}

.tag-icon {
  font-size: 22rpx;
}

.stats-actions {
  display: grid;
  grid-template-columns: 1fr 360rpx;
  gap: 24rpx;
  align-items: center;
  margin-top: 90rpx;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
}

.stat-item {
  text-align: center;
  border-right: 1rpx solid rgba(255, 255, 255, 0.22);
}

.stat-item:last-child {
  border-right: 0;
}

.stat-value {
  display: block;
  font-size: 32rpx;
  line-height: 1;
  font-weight: 900;
}

.stat-label {
  display: block;
  margin-top: 10rpx;
  color: $muted;
  font-size: 22rpx;
}

.hero-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}

.gold-btn,
.outline-btn,
.gallery-btn,
.all-works-btn,
.consult-btn {
  height: 62rpx;
  border-radius: 8rpx;
  font-size: 25rpx;
  font-weight: 800;
}

.gold-btn,
.gallery-btn,
.consult-btn {
  color: #201600;
  background: linear-gradient(135deg, #f3c43d 0%, #d6a51d 100%);
}

.outline-btn,
.all-works-btn {
  color: $gold-light;
  border: 1rpx solid $gold-line;
  background: rgba(5, 5, 5, 0.5);
}

.intro-card {
  position: relative;
  margin-top: 28rpx;
  padding: 24rpx 28rpx 30rpx;
  border-radius: 12rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(135deg, #202020, #151515);
}

.section-title {
  color: $text;
  font-size: 32rpx;
  line-height: 1;
  font-weight: 900;
}

.gold-title {
  color: $gold-light;
}

.intro-text {
  margin-top: 22rpx;
  color: #b9b9b9;
  font-size: 25rpx;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.intro-text.expanded {
  display: block;
}

.quote {
  margin-top: 14rpx;
  padding-right: 96rpx;
  color: $gold-light;
  font-size: 24rpx;
  line-height: 1.5;
  font-weight: 700;
}

.expand {
  position: absolute;
  right: 26rpx;
  bottom: 28rpx;
  color: $dim;
  font-size: 24rpx;
}

.section {
  margin-top: 28rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.more-link {
  color: $muted;
  font-size: 24rpx;
}

.work-scroll {
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
}

.work-row {
  display: inline-flex;
  gap: 16rpx;
}

.work-card {
  width: 292rpx;
  overflow: hidden;
  border-radius: 8rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  background: #1a1a1a;
}

.work-image-wrap {
  position: relative;
  height: 180rpx;
}

.work-image {
  width: 100%;
  height: 100%;
}

.new-badge {
  position: absolute;
  top: 14rpx;
  right: 14rpx;
  width: 40rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 7rpx;
  color: #151000;
  background: $gold-light;
  font-size: 22rpx;
  font-weight: 900;
}

.work-info {
  padding: 16rpx;
}

.work-title {
  color: #fff;
  font-size: 25rpx;
  font-weight: 900;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.work-meta {
  margin-top: 8rpx;
  color: $muted;
  font-size: 20rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.work-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 12rpx;
}

.price {
  color: $gold-light;
  font-size: 27rpx;
  font-weight: 900;
}

.collect-tag {
  padding: 3rpx 10rpx;
  border-radius: 6rpx;
  border: 1rpx solid $gold-line;
  color: $gold-light;
  font-size: 19rpx;
}

.gallery-banner {
  position: relative;
  min-height: 122rpx;
  margin-top: 26rpx;
  padding: 18rpx 22rpx;
  display: grid;
  grid-template-columns: 92rpx 1fr 190rpx;
  gap: 20rpx;
  align-items: center;
  overflow: hidden;
  border-radius: 10rpx;
  border: 1rpx solid $gold-line;
  background: #0c0c0c;
}

.gallery-bg,
.gallery-mask {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.gallery-bg {
  opacity: 0.46;
}

.gallery-mask {
  background: linear-gradient(90deg, rgba(5, 5, 5, 0.9), rgba(26, 17, 4, 0.72), rgba(5, 5, 5, 0.82));
}

.gallery-icon {
  position: relative;
  z-index: 1;
  color: $gold-light;
  font-size: 66rpx;
  line-height: 1;
  text-align: center;
}

.gallery-title {
  position: relative;
  z-index: 1;
  color: $gold-light;
  font-size: 34rpx;
  font-weight: 900;
}

.gallery-desc {
  position: relative;
  z-index: 1;
  margin-top: 8rpx;
  color: $muted;
  font-size: 22rpx;
}

.gallery-btn {
  position: relative;
  z-index: 1;
  height: 58rpx;
  font-size: 23rpx;
}

.flow-list {
  margin-top: 18rpx;
}

.flow-item {
  display: grid;
  grid-template-columns: 126rpx 1fr 118rpx 118rpx 118rpx 28rpx;
  gap: 12rpx;
  align-items: center;
  margin-top: 10rpx;
  padding: 10rpx;
  border-radius: 8rpx;
  background: linear-gradient(135deg, #222, #151515);
  border: 1rpx solid rgba(255, 255, 255, 0.08);
}

.flow-cover {
  width: 126rpx;
  height: 72rpx;
  border-radius: 6rpx;
}

.flow-title {
  color: #e9e9e9;
  font-size: 24rpx;
  font-weight: 800;
}

.flow-price {
  margin-top: 6rpx;
  color: $gold-light;
  font-size: 24rpx;
  font-weight: 900;
}

.flow-feature {
  color: $gold-light;
  font-size: 21rpx;
  text-align: center;
}

.flow-arrow {
  color: $muted;
  font-size: 38rpx;
}

.trust-section {
  margin-top: 28rpx;
}

.trust-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10rpx;
  margin-top: 22rpx;
}

.trust-item {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding-right: 10rpx;
  border-right: 1rpx solid rgba(255, 255, 255, 0.16);
}

.trust-item:last-child {
  border-right: 0;
}

.trust-icon {
  flex: 0 0 auto;
  color: $gold-light;
  font-size: 42rpx;
}

.trust-title {
  color: #cfcfcf;
  font-size: 20rpx;
  line-height: 1.2;
}

.trust-desc {
  margin-top: 4rpx;
  color: $muted;
  font-size: 18rpx;
  line-height: 1.2;
}

.bottom-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  margin-top: 30rpx;
}

.all-works-btn,
.consult-btn {
  height: 68rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  border-radius: 8rpx;
  font-size: 27rpx;
}

.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  height: 126rpx;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  padding-top: 14rpx;
  box-sizing: border-box;
  background: rgba(24, 24, 24, 0.96);
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(18rpx);
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  color: #8c8c8c;
  font-size: 22rpx;
}

.tab-item.active {
  color: $gold-light;
}

.tab-icon {
  font-size: 38rpx;
  line-height: 1;
}

button::after {
  border: 0;
}

/* ===== art_profile_page_with_modern_design 1:1 rebuild ===== */
.artist-home-page {
  min-height: 100vh;
  padding: 0 20rpx 30rpx;
  box-sizing: border-box;
  color: #f7f7f7;
  background:
    radial-gradient(circle at 80% 5%, rgba(214, 166, 32, 0.08), transparent 24%),
    linear-gradient(180deg, #000 0%, #050505 100%);
}

.top-nav {
  position: relative;
  z-index: 2;
  height: 76rpx;
  padding: 0 0;
  background: transparent;
  backdrop-filter: none;
}

.nav-title {
  color: #fff;
  font-size: 30rpx;
  line-height: 1;
  font-weight: 800;
}

.nav-icon {
  width: 58rpx;
  height: 58rpx;
  font-size: 0;
}

.nav-icon image {
  width: 40rpx;
  height: 40rpx;
}

.nav-icon.external image {
  width: 38rpx;
  height: 38rpx;
}

.profile-hero {
  height: 356rpx;
  margin-top: 0;
  border-radius: 14rpx;
}

.cover-image,
.cover-shade {
  border-radius: 14rpx;
}

.cover-image {
  height: 230rpx;
}

.cover-shade {
  height: 356rpx;
  background:
    linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.06) 34%, rgba(0, 0, 0, 0.9) 70%, #000 100%),
    linear-gradient(90deg, rgba(0, 0, 0, 0.18), transparent 45%, rgba(0, 0, 0, 0.08));
}

.profile-core {
  left: 36rpx;
  right: 22rpx;
  bottom: 12rpx;
  gap: 20rpx;
  align-items: flex-end;
}

.avatar {
  width: 166rpx;
  height: 166rpx;
  border: 4rpx solid #f2c14e;
  box-shadow: 0 0 0 1rpx rgba(0, 0, 0, 0.45), 0 16rpx 38rpx rgba(0, 0, 0, 0.45);
}

.avatar-cert {
  right: -6rpx;
  bottom: 14rpx;
  width: 46rpx;
  height: 46rpx;
  color: #fff;
  font-size: 30rpx;
  background: linear-gradient(180deg, #f7d46c, #d9a935);
}

.identity-block {
  padding-bottom: 8rpx;
}

.artist-name {
  color: #fff;
  font-size: 38rpx;
  line-height: 1;
  font-weight: 900;
}

.artist-title {
  margin-top: 14rpx;
  color: #d0d0d0;
  font-size: 23rpx;
}

.tag-row {
  gap: 12rpx;
  margin-top: 22rpx;
}

.tag {
  height: 40rpx;
  padding: 0 14rpx;
  border-radius: 7rpx;
  border-color: rgba(242, 193, 78, 0.9);
  color: #f2c14e;
  background: rgba(0, 0, 0, 0.42);
  font-size: 20rpx;
  font-weight: 700;
}

.tag-icon-img {
  width: 22rpx;
  height: 22rpx;
}

.stats-actions {
  margin-top: 18rpx;
  display: grid;
  grid-template-columns: 395rpx 1fr;
  gap: 24rpx;
}

.stats {
  grid-template-columns: repeat(3, 1fr);
}

.stat-value {
  color: #fff;
  font-size: 34rpx;
}

.stat-label {
  margin-top: 10rpx;
  color: #a9a9a9;
  font-size: 22rpx;
}

.stat-item {
  border-right-color: rgba(255, 255, 255, 0.24);
}

.hero-actions {
  gap: 14rpx;
}

.gold-btn,
.outline-btn {
  height: 58rpx;
  border-radius: 7rpx;
  font-size: 22rpx;
  font-weight: 900;
}

.gold-btn {
  background: linear-gradient(180deg, #f4c653, #d8a81e);
}

.outline-btn {
  color: #f2c14e;
  border-color: rgba(242, 193, 78, 0.75);
  background: rgba(0, 0, 0, 0.28);
}

.intro-card {
  margin-top: 30rpx;
  padding: 28rpx 28rpx 26rpx;
  border-radius: 11rpx;
  border-color: rgba(255, 255, 255, 0.09);
  background: linear-gradient(135deg, #202020, #141414);
}

.section-title {
  color: #fff;
  font-size: 32rpx;
  line-height: 1;
}

.gold-title {
  color: #f2c14e;
  font-size: 27rpx;
}

.intro-text {
  margin-top: 22rpx;
  color: #c1c1c1;
  font-size: 22rpx;
  line-height: 1.72;
  -webkit-line-clamp: 2;
}

.quote {
  margin-top: 16rpx;
  padding-right: 110rpx;
  color: #f2c14e;
  font-size: 21rpx;
  line-height: 1.5;
}

.expand {
  right: 28rpx;
  bottom: 27rpx;
  color: #8c8c8c;
  font-size: 21rpx;
}

.section {
  margin-top: 30rpx;
}

.section-head {
  margin-bottom: 20rpx;
}

.more-link {
  color: #aaa;
  font-size: 22rpx;
}

.work-row {
  gap: 12rpx;
}

.work-card {
  width: 274rpx;
  border-radius: 8rpx;
  background: #171717;
}

.work-image-wrap {
  height: 190rpx;
}

.new-badge {
  top: 12rpx;
  right: 12rpx;
  width: 38rpx;
  height: 42rpx;
  border-radius: 8rpx;
  font-size: 21rpx;
  background: linear-gradient(180deg, #f6d98a, #d9a935);
}

.work-info {
  padding: 16rpx;
}

.work-title {
  font-size: 24rpx;
}

.work-meta {
  font-size: 18rpx;
}

.price {
  font-size: 27rpx;
  color: #f2c14e;
}

.collect-tag {
  padding: 4rpx 11rpx;
  border-radius: 6rpx;
  font-size: 18rpx;
}

.circulation-section {
  margin-top: 30rpx;
}

.flow-list {
  margin-top: 18rpx;
}

.flow-item {
  min-height: 76rpx;
  grid-template-columns: 138rpx 150rpx 104rpx 104rpx 104rpx 18rpx;
  gap: 8rpx;
  margin-top: 7rpx;
  padding: 0 12rpx 0 0;
  border-radius: 7rpx;
  background: linear-gradient(135deg, #202020, #141414);
}

.flow-cover {
  width: 138rpx;
  height: 76rpx;
  border-radius: 7rpx 0 0 7rpx;
}

.flow-title {
  font-size: 21rpx;
  line-height: 1.15;
}

.flow-price {
  font-size: 22rpx;
}

.flow-feature {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5rpx;
  color: #f2c14e;
  font-size: 17rpx;
  white-space: nowrap;
}

.flow-feature image {
  width: 24rpx;
  height: 24rpx;
}

.flow-arrow {
  color: #aaa;
  font-size: 34rpx;
}

.trust-section {
  margin-top: 30rpx;
}

.trust-row {
  margin-top: 22rpx;
  gap: 0;
}

.trust-item {
  gap: 12rpx;
  padding: 0 16rpx 0 0;
  border-right-color: rgba(255, 255, 255, 0.18);
}

.trust-icon {
  width: 43rpx;
  height: 43rpx;
}

.trust-title {
  color: #cfcfcf;
  font-size: 19rpx;
}

.trust-desc {
  color: #9a9a9a;
  font-size: 18rpx;
}

.bottom-actions {
  margin-top: 30rpx;
  gap: 18rpx;
}

.all-works-btn,
.consult-btn {
  height: 72rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
}

.all-works-btn {
  color: #fff;
  border: 1rpx solid rgba(242, 193, 78, 0.8);
  background: linear-gradient(135deg, #202020, #111);
}

.consult-btn {
  color: #111;
  background: linear-gradient(180deg, #f6d269, #d9a935);
}

.consult-btn image {
  width: 30rpx;
  height: 30rpx;
}

.tabbar {
  display: none;
}

.home-indicator {
  width: 326rpx;
  height: 9rpx;
  margin: 36rpx auto 0;
  border-radius: 999rpx;
  background: #fff;
}
</style>
