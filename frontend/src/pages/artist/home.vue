<template>
  <view class="artist-home-page">
    <view class="top-nav">
      <view class="nav-icon" @click="goBack">
        <image src="/static/art-icons/icon-back.svg" mode="aspectFit"></image>
      </view>
      <view class="nav-title"></view>
      <view class="nav-icon external" @click="shareArtist">
        <image src="/static/art-icons/icon-share.svg" mode="aspectFit"></image>
      </view>
    </view>

    <view class="profile-hero">
      <image v-if="artist.cover" class="cover-image" :src="artist.cover" mode="aspectFill"></image>
      <view class="cover-shade"></view>

      <view class="profile-core">
        <view class="avatar-wrap">
          <image class="avatar" :src="artist.avatar" mode="aspectFill"></image>
        <view v-if="artist.certified" class="avatar-cert">✓</view>
        </view>
        <view class="identity-block">
          <view class="artist-name">{{ artist.name }}</view>
          <view v-if="artist.title" class="artist-title">{{ artist.title }}</view>
          <view v-if="displayTags.length" class="tag-row">
            <view class="tag" v-for="tag in displayTags" :key="tag">
              <image class="tag-icon-img" src="/static/art-icons/icon-verify.svg" mode="aspectFit"></image>
              <text>{{ tag }}</text>
            </view>
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
      <view v-if="artist.quote" class="quote">“ {{ artist.quote }} ”</view>
      <view v-if="artist.intro && artist.intro.length > 42" class="expand" @click="introExpanded = !introExpanded">
        {{ introExpanded ? '收起' : '展开' }} <text>{{ introExpanded ? '⌃' : '⌄' }}</text>
      </view>
    </view>

    <view class="section works-section">
      <view class="section-head" v-if="works.length">
        <view class="section-title">代表作品</view>
        <view class="more-link" @click="goWorks">查看全部 ›</view>
      </view>
      <view v-if="works.length" class="work-scroll">
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
      <view v-else class="empty-block">该艺术家暂未上架作品</view>
    </view>

    <view v-if="flowWorks.length" class="section circulation-section">
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
        <view class="consult-btn__icon">
          <image src="/static/art-icons/icon-consultant.svg" mode="aspectFit"></image>
        </view>
        <text>发起收藏咨询</text>
      </button>
    </view>

    <view class="home-indicator"></view>
  </view>
</template>

<script>
import * as userApi from '@/api/user'

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
        intro: '',
        quote: '',
        tags: [],
        certified: false
      },
      works: [],
      trustItems: [
        { icon: '/static/art-icons/icon-verify.svg', title: '平台认证', desc: '严格审核' },
        { icon: '/static/art-icons/icon-certificate.svg', title: '收藏证书', desc: '权威出具' },
        { icon: '/static/art-icons/icon-circulation.svg', title: '流通记录', desc: '全程可查' },
        { icon: '/static/art-icons/icon-platform-custody.svg', title: '保管支持', desc: '专业保管' }
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
    // H5 右上角分享走浏览器能力，小程序走原生分享菜单
    // #ifdef MP-WEIXIN
    uni.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    })
    // #endif
  },

  computed: {
    flowWorks() {
      return this.works.slice(0, 2)
    },
    sharePath() {
      return `/pages/artist/home?userId=${this.artist.id}`
    },
    shareTitle() {
      return `${this.artist.name || '艺术家'}的艺术主页`
    },
    displayTags() {
      const tags = Array.isArray(this.artist.tags) ? this.artist.tags.filter(Boolean) : []
      if (tags.length) return tags
      const fallback = []
      if (this.artist.certified) fallback.push('平台认证')
      if (this.artist.title) fallback.push(this.artist.title)
      const firstWork = this.works[0]
      if (firstWork?.material) fallback.push(firstWork.material)
      return fallback.slice(0, 4)
    }
  },

  methods: {
    async loadArtistData(artistId) {
      try {
        const data = await userApi.getArtistInfo(artistId)
        this.applyArtistData(data, artistId)
      } catch (e) {
        console.error('加载艺术家数据失败', e)
        this.applyArtistData({}, artistId)
      }
    },

    applyArtistData(data = {}, artistId) {
      const works = (data.works || data.artworks || []).map(w => ({
        id: w.id,
        title: w.title || w.name || '',
        material: w.material || w.artType || w.medium || '',
        size: w.size || '',
        year: w.year || w.createYear || '',
        priceText: w.priceText || (w.price ? '¥' + this.formatPrice(w.price) : ''),
        cover: w.cover || w.coverImage || w.coverUrl || '/static/images/museum-v12-work-boat.png'
      }))
      const artistTags = this.normalizeTags(data.artistTags || data.tags || data.badges)
      const intro = data.intro || data.bio || data.resume || '暂未补充艺术家介绍'
      const cover = data.homepageCover || data.cover || data.coverUrl || works[0]?.cover || data.avatar || '/static/images/museum-v12-hero-bg.png'

      this.artist = {
        id: data.id || data.userId || artistId,
        name: data.nickname || data.name || data.realName || '艺术家',
        title: data.artistTitle || data.title || data.identityTypeLabel || '',
        avatar: data.avatar || data.avatarUrl || '/static/images/artist-avatar.png',
        cover,
        intro,
        quote: data.quote || '',
        tags: artistTags,
        certified: !!(data.certified || data.certStatus === 1 || data.isArtist || artistTags.includes('平台认证'))
      }
      this.works = works
      this.followed = !!data.followed
      this.stats = [
        { label: '作品', value: String(data.workCount || data.artworkCount || works.length || 0) },
        { label: '收藏', value: String(data.collectCount || data.favoriteCount || 0) },
        { label: '粉丝', value: String(data.fansCount || data.followerCount || 0) }
      ]
    },

    normalizeTags(rawValue) {
      if (Array.isArray(rawValue)) {
        return rawValue.map(item => String(item || '').trim()).filter(Boolean)
      }
      if (typeof rawValue === 'string' && rawValue.trim()) {
        return rawValue.split(/[,\n，|]/).map(item => item.trim()).filter(Boolean)
      }
      return []
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
          await userApi.unfollowArtist(this.artist.id)
          this.followed = false
        } else {
          await userApi.followArtist(this.artist.id)
          this.followed = true
        }
      } catch (e) {
        console.error('关注操作失败', e)
      }
    },
    async shareArtist() {
      const shareUrl = typeof window !== 'undefined'
        ? `${window.location.origin}/#${this.sharePath}`
        : this.sharePath

      // #ifdef H5
      if (navigator?.share) {
        try {
          await navigator.share({
            title: this.shareTitle,
            text: `${this.artist.title || '艺术家主页'}，来看看TA的代表作品`,
            url: shareUrl
          })
          return
        } catch (error) {
          if (error?.name === 'AbortError') {
            return
          }
        }
      }
      if (typeof window !== 'undefined') {
        uni.showActionSheet({
          itemList: ['复制链接', '新窗口打开'],
          success: ({ tapIndex }) => {
            if (tapIndex === 0) {
              uni.setClipboardData({
                data: shareUrl,
                success: () => {
                  uni.showToast({ title: '链接已复制，可分享到微信或小红书', icon: 'none' })
                },
                fail: () => {
                  uni.showToast({ title: '当前环境暂不支持复制', icon: 'none' })
                }
              })
              return
            }
            window.open(shareUrl, '_blank', 'noopener,noreferrer')
          },
          fail: () => {
            uni.showToast({ title: '当前环境暂不支持系统分享', icon: 'none' })
          }
        })
        return
      }
      // #endif

      uni.setClipboardData({
        data: shareUrl,
        success: () => {
          uni.showToast({ title: '分享链接已复制', icon: 'none' })
        },
        fail: () => {
          uni.showToast({ title: '当前环境暂不支持分享', icon: 'none' })
        }
      })
    },
    consult() { uni.showToast({ title: '已为你连接收藏顾问', icon: 'none' }) },
    goGallery() { uni.navigateTo({ url: `/pages/artist/gallery/index?id=${this.artist.id}` }) },
    goWorks() { uni.navigateTo({ url: `/pages/artist/works/index?id=${this.artist.id}` }) },
    goWork(id) { uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` }) },
    goIndex() { uni.reLaunch({ url: '/pages/index/index' }) },
    goPublish() { uni.navigateTo({ url: '/pages/artist/publish' }) },
    goCart() { uni.navigateTo({ url: '/pages/cart/index' }) },
    goMine() { uni.navigateTo({ url: '/pages/user/index' }) }
  },
  onShareAppMessage() {
    return {
      title: this.shareTitle,
      path: this.sharePath,
      imageUrl: this.artist.cover || this.artist.avatar || ''
    }
  },
  onShareTimeline() {
    return {
      title: this.shareTitle,
      query: `userId=${this.artist.id}`,
      imageUrl: this.artist.cover || this.artist.avatar || ''
    }
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
  background: transparent;
}

.nav-title {
  flex: 1;
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
  background:
    radial-gradient(circle at 72% 20%, rgba(215, 165, 29, 0.16), transparent 28%),
    linear-gradient(135deg, rgba(8, 36, 68, 0.96), rgba(7, 20, 38, 0.98));
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

.empty-block {
  padding: 36rpx 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 8rpx;
  background: rgba(255, 255, 255, 0.03);
  color: $muted;
  font-size: 24rpx;
  text-align: center;
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
  padding: calc(env(safe-area-inset-top) + 12rpx) 20rpx calc(170rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  color: #f7f7f7;
  background:
    radial-gradient(circle at 80% 5%, rgba(214, 166, 32, 0.08), transparent 24%),
    linear-gradient(180deg, #000 0%, #050505 100%);
}

.top-nav {
  position: absolute;
  top: env(safe-area-inset-top);
  left: 20rpx;
  right: 20rpx;
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10rpx;
}

.trust-item {
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  gap: 4rpx;
  min-height: 94rpx;
  padding: 12rpx 10rpx;
  border-right: 0;
  border-radius: 10rpx;
  background: linear-gradient(135deg, rgba(28, 28, 28, 0.94), rgba(17, 17, 17, 0.96));
}

.trust-item:last-child {
  border-right: 0;
}

.trust-icon {
  width: 32rpx;
  height: 32rpx;
}

.trust-title {
  color: #e2e2e2;
  font-size: 17rpx;
  line-height: 1.25;
  font-weight: 700;
}

.trust-desc {
  color: #999;
  font-size: 16rpx;
  line-height: 1.25;
}

.bottom-actions {
  position: fixed;
  left: 20rpx;
  right: 20rpx;
  bottom: calc(22rpx + env(safe-area-inset-bottom));
  z-index: 30;
  grid-template-columns: minmax(0, 0.88fr) minmax(0, 1.12fr);
  gap: 16rpx;
  margin-top: 34rpx;
  padding: 16rpx;
  border-radius: 18rpx;
  background: rgba(6, 6, 6, 0.92);
  border: 1rpx solid rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(16rpx);
  box-shadow: 0 -10rpx 30rpx rgba(0, 0, 0, 0.22);
  align-items: stretch;
}

.all-works-btn,
.consult-btn {
  height: 80rpx;
  border-radius: 10rpx;
  font-size: 24rpx;
  font-weight: 800;
}

.all-works-btn {
  color: #fff;
  border: 1rpx solid rgba(242, 193, 78, 0.8);
  background: linear-gradient(135deg, #202020, #111);
}

.consult-btn {
  color: #111;
  background: linear-gradient(180deg, #f6d269, #d9a935);
  box-shadow: 0 12rpx 28rpx rgba(217, 169, 53, 0.18);
}

.consult-btn__icon {
  width: 38rpx;
  height: 38rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(17, 17, 17, 0.16);
}

.consult-btn__icon image {
  width: 22rpx;
  height: 22rpx;
  filter: brightness(0) saturate(100%);
}

@media (max-width: 420px) {
  .bottom-actions {
    grid-template-columns: 1fr;
  }
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
