<template>
  <view class="museum-page">
    <view class="phone-status">
      <text>9:41</text>
      <view class="status-right">
        <view class="signal-bars"><text></text><text></text><text></text><text></text></view>
        <view class="wifi-icon"></view>
        <view class="battery-icon"><text></text></view>
      </view>
    </view>

    <view class="museum-nav">
      <view class="nav-icon" @click="goBack">
        <image src="/static/art-icons/icon-back.svg" mode="aspectFit"></image>
      </view>
      <view class="nav-title">个人美术馆</view>
      <view class="mini-capsule">
        <text class="dot">•••</text>
        <text class="divider"></text>
        <text class="circle"></text>
      </view>
    </view>

    <view class="hero">
      <image class="hero-bg" src="/static/images/museum-v12-hero-bg.png" mode="aspectFill"></image>
      <view class="hero-mask"></view>
      <view class="artist-row">
        <view class="avatar-wrap">
          <image class="avatar" src="/static/images/profile-v12-avatar.png" mode="aspectFill"></image>
          <view class="avatar-cert">✓</view>
        </view>
        <view>
          <view class="artist-name">孟儒</view>
          <view class="artist-title">当代油画艺术家</view>
        </view>
      </view>
      <view class="hero-copy">
        <view class="hero-title">光影与记忆</view>
        <view class="hero-desc">沉浸式体验孟儒的油画世界</view>
        <button class="start-btn" @click="enterExhibition">
          <view class="start-play"></view>
          <text>开始观展</text>
        </button>
      </view>
    </view>

    <view class="section-head current-head">
      <view class="section-title">当前展览</view>
    </view>

    <view class="current-card" @click="goExhibition">
      <view class="current-cover-wrap">
        <image class="current-cover" src="/static/images/museum-v12-current-work.png" mode="aspectFill"></image>
        <text class="current-badge">当前展出</text>
      </view>
      <view class="current-info">
        <view class="current-title">光影与记忆</view>
        <view class="current-subtitle">孟儒作品沉浸式线上展</view>
        <view class="current-quote">“光影流转之间，记忆在油彩中沉淀。”</view>
        <view class="meta-line">
          <image src="/static/art-icons/icon-calendar.svg" mode="aspectFit"></image>
          <text>2024.05.20 - 2024.08.20</text>
        </view>
        <view class="meta-line">
          <image src="/static/art-icons/icon-location.svg" mode="aspectFit"></image>
          <text>个人美术馆 · 主展厅</text>
        </view>
        <button class="outline-btn" @click.stop="goExhibition">进入展厅 ›</button>
      </view>
    </view>

    <view class="section-head">
      <view class="section-title">分厅导览</view>
    </view>
    <view class="hall-tabs">
      <view v-for="hall in halls" :key="hall" :class="['hall-tab', { active: activeHall === hall }]" @click="activeHall = hall">
        {{ hall }}
      </view>
    </view>
    <view class="tool-strip">
      <view class="tool-cell" v-for="tool in tools" :key="tool.title" @click="toast(tool.title)">
        <image :src="tool.icon" mode="aspectFit"></image>
        <view>
          <view class="tool-title">{{ tool.title }}</view>
          <view class="tool-desc">{{ tool.desc }}</view>
        </view>
        <text class="tool-arrow">›</text>
      </view>
    </view>

    <view class="section-head works-head">
      <view class="section-title">精选展作</view>
      <view class="more-link" @click="goWorks">查看全部 ›</view>
    </view>
    <view class="work-strip">
      <view class="work-card" v-for="work in works" :key="work.id" @click="goWork(work.id)">
        <view class="work-image-wrap">
          <image class="work-image" :src="work.cover" mode="aspectFill"></image>
          <text class="recommend-badge">推荐</text>
        </view>
        <view class="work-info">
          <view class="work-title">{{ work.title }}</view>
          <view class="work-meta">{{ work.material }} / {{ work.size }}</view>
          <view class="work-price">{{ work.priceText }}</view>
        </view>
      </view>
    </view>

    <view class="bottom-spacer"></view>
    <view class="bottom-panel">
      <view class="cta-row">
        <button class="main-cta" @click="enterExhibition">
          <view class="cta-museum-icon"></view>
          <text>进入沉浸式观展</text>
        </button>
        <button class="collect-cta" @click="collectMuseum">
          <image src="/static/art-icons/icon-star.svg" mode="aspectFit"></image>
          <text>收藏美术馆</text>
        </button>
      </view>
      <view class="tab-row">
        <view class="edit-pill">编辑</view>
        <view class="tab-item" v-for="tab in bottomTabs" :key="tab.text" @click="tab.action && tab.action()">
          <image :src="tab.icon" mode="aspectFit"></image>
          <text>{{ tab.text }}</text>
        </view>
        <view class="upload-float" @click="shareMuseum">
          <image src="/static/art-icons/icon-share.svg" mode="aspectFit"></image>
        </view>
      </view>
      <view class="home-indicator"></view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      activeHall: '风景',
      halls: ['风景', '人物', '静物', '纸本', '花卉', '抽象'],
      tools: [
        { title: '语音导览', desc: '聆听作品背后的故事', icon: '/static/art-icons/icon-headset.svg' },
        { title: '展厅地图', desc: '探索美术馆空间', icon: '/static/art-icons/icon-map.svg' },
        { title: '策展说明', desc: '了解展览策划理念', icon: '/static/art-icons/icon-document.svg' }
      ],
      works: [
        { id: 37, title: '晨曦·归航', material: '布面油画', size: '100×80cm', priceText: '¥8,000', cover: '/static/images/museum-v12-work-boat.png' },
        { id: 38, title: '秋日', material: '布面油画', size: '80×60cm', priceText: '¥1.2万', cover: '/static/images/museum-v12-work-girl.png' },
        { id: 39, title: '静物 No.0751', material: '布面油画', size: '40×40cm', priceText: '¥3,200', cover: '/static/images/museum-v12-work-still.png' },
        { id: 40, title: '湖畔·微光', material: '布面油画', size: '60×80cm', priceText: '¥1.2万', cover: '/static/images/profile-v12-work-boat.png' }
      ]
    }
  },
  computed: {
    bottomTabs() {
      return [
        { text: '拍卖', icon: '/static/art-icons/icon-appreciate.svg' },
        { text: '发布', icon: '/static/art-icons/icon-work.svg', action: () => uni.navigateTo({ url: '/pages/artist/publish' }) },
        { text: '购物车', icon: '/static/art-icons/icon-cart.svg', action: () => uni.navigateTo({ url: '/pages/cart/index' }) }
      ]
    }
  },
  methods: {
    goBack() {
      const pages = getCurrentPages()
      if (pages && pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.reLaunch({ url: '/pages/index/index' })
      }
    },
    goExhibition() { uni.navigateTo({ url: '/pages/artist/detail/index?id=10001' }) },
    goWorks() { uni.navigateTo({ url: '/pages/artist/works/index?id=10001' }) },
    goWork(id) { uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` }) },
    enterExhibition() { uni.showToast({ title: '进入沉浸式观展', icon: 'none' }) },
    collectMuseum() { uni.showToast({ title: '已收藏美术馆', icon: 'none' }) },
    shareMuseum() { uni.showToast({ title: '分享美术馆', icon: 'none' }) },
    toast(title) { uni.showToast({ title, icon: 'none' }) }
  }
}
</script>

<style lang="scss" scoped>
$gold: #f0bd32;
$gold-deep: #dca31f;
$muted: #a8a8a8;

.museum-page {
  min-height: 100vh;
  padding: 0 24rpx;
  box-sizing: border-box;
  color: #fff;
  background:
    radial-gradient(circle at 70% 8%, rgba(217, 169, 53, 0.12), transparent 24%),
    linear-gradient(180deg, #050505 0%, #030303 100%);
}

.phone-status {
  height: 78rpx;
  padding: 31rpx 52rpx 0;
  margin: 0 -24rpx;
  box-sizing: border-box;
  display: flex;
  justify-content: space-between;
  color: #fff;
  font-size: 29rpx;
  line-height: 1;
  font-weight: 800;
}

.status-right { display: flex; align-items: center; gap: 12rpx; }
.signal-bars { height: 22rpx; display: flex; align-items: flex-end; gap: 4rpx; }
.signal-bars text { width: 5rpx; border-radius: 999rpx; background: #fff; }
.signal-bars text:nth-child(1) { height: 8rpx; }
.signal-bars text:nth-child(2) { height: 12rpx; }
.signal-bars text:nth-child(3) { height: 17rpx; }
.signal-bars text:nth-child(4) { height: 22rpx; }
.wifi-icon {
  width: 30rpx;
  height: 22rpx;
  border: 5rpx solid #fff;
  border-left-color: transparent;
  border-right-color: transparent;
  border-bottom: 0;
  border-radius: 28rpx 28rpx 0 0;
  box-sizing: border-box;
}
.battery-icon {
  position: relative;
  width: 42rpx;
  height: 22rpx;
  border: 3rpx solid #fff;
  border-radius: 6rpx;
  box-sizing: border-box;
}
.battery-icon::after {
  content: '';
  position: absolute;
  right: -7rpx;
  top: 5rpx;
  width: 4rpx;
  height: 10rpx;
  border-radius: 0 3rpx 3rpx 0;
  background: #fff;
}
.battery-icon text { position: absolute; inset: 3rpx; border-radius: 3rpx; background: #fff; }

.museum-nav {
  height: 78rpx;
  margin: 0 -24rpx;
  padding: 0 36rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-icon {
  width: 54rpx;
  height: 54rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-icon image { width: 37rpx; height: 37rpx; }
.nav-title { font-size: 32rpx; line-height: 1; font-weight: 900; }

.mini-capsule {
  width: 166rpx;
  height: 58rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(255,255,255,.28);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 22rpx;
  background: rgba(0,0,0,.35);
}
.dot { font-size: 32rpx; letter-spacing: 3rpx; transform: translateY(-4rpx); }
.divider { width: 1rpx; height: 36rpx; background: rgba(255,255,255,.24); }
.circle { width: 34rpx; height: 34rpx; border: 5rpx solid #fff; border-radius: 50%; box-sizing: border-box; }

.hero {
  position: relative;
  height: 438rpx;
  margin: 0 -24rpx;
  overflow: hidden;
}
.hero-bg { position: absolute; inset: 0; width: 100%; height: 100%; }
.hero-mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(0,0,0,.95) 0%, rgba(0,0,0,.67) 36%, rgba(0,0,0,.15) 76%),
    linear-gradient(180deg, rgba(0,0,0,.18), rgba(0,0,0,.76));
}
.artist-row {
  position: absolute;
  top: 54rpx;
  left: 64rpx;
  display: flex;
  align-items: center;
  gap: 28rpx;
}
.avatar-wrap { position: relative; }
.avatar {
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  border: 2rpx solid $gold;
}
.avatar-cert {
  position: absolute;
  right: -2rpx;
  bottom: 3rpx;
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(180deg, #f6d98a, #d9a935);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18rpx;
  font-weight: 900;
}
.artist-name { font-size: 31rpx; font-weight: 900; }
.artist-title { margin-top: 15rpx; color: #c8c8c8; font-size: 22rpx; }
.hero-copy { position: absolute; left: 64rpx; bottom: 52rpx; }
.hero-title {
  color: $gold;
  font-family: "Songti SC", "STSong", "SimSun", serif;
  font-size: 50rpx;
  line-height: 1;
  font-weight: 700;
}
.hero-desc { margin-top: 16rpx; color: #d0d0d0; font-size: 24rpx; }
.start-btn {
  width: 174rpx;
  height: 54rpx;
  margin-top: 26rpx;
  border-radius: 8rpx;
  color: #111;
  background: linear-gradient(180deg, #f8d56b 0%, #e4ad25 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-size: 23rpx;
  font-weight: 900;
}
.start-play {
  width: 26rpx;
  height: 26rpx;
  border: 3rpx solid #151515;
  border-radius: 50%;
  box-sizing: border-box;
  position: relative;
}
.start-play::after {
  content: '';
  position: absolute;
  left: 8rpx;
  top: 5rpx;
  width: 0;
  height: 0;
  border-top: 5rpx solid transparent;
  border-bottom: 5rpx solid transparent;
  border-left: 8rpx solid #151515;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: 32rpx;
  margin-bottom: 18rpx;
}
.current-head { margin-top: 31rpx; }
.section-title {
  color: #fff;
  font-size: 35rpx;
  line-height: 1;
  font-weight: 900;
}
.section-title::after {
  content: '';
  display: block;
  width: 31rpx;
  height: 6rpx;
  margin-top: 12rpx;
  border-radius: 999rpx;
  background: $gold;
}
.more-link { color: #a7a7a7; font-size: 22rpx; }

.current-card {
  display: grid;
  grid-template-columns: 334rpx 1fr;
  min-height: 282rpx;
  overflow: hidden;
  border-radius: 12rpx;
  border: 1rpx solid rgba(255,255,255,.09);
  background: linear-gradient(135deg, #222, #111);
}
.current-cover-wrap { position: relative; }
.current-cover { width: 100%; height: 100%; }
.current-badge {
  position: absolute;
  top: 19rpx;
  left: 0;
  height: 38rpx;
  min-width: 112rpx;
  padding: 0 14rpx 0 19rpx;
  border-radius: 0 6rpx 6rpx 0;
  display: flex;
  align-items: center;
  color: #111;
  background: linear-gradient(180deg, #f4c84f 0%, #dfaa25 100%);
  font-size: 18rpx;
  font-weight: 900;
}
.current-info {
  min-width: 0;
  padding: 30rpx 24rpx 22rpx;
  display: flex;
  flex-direction: column;
}
.current-title { font-size: 32rpx; font-weight: 900; }
.current-subtitle { margin-top: 20rpx; color: #b9b9b9; font-size: 20rpx; }
.current-quote { margin-top: 18rpx; color: #929292; font-size: 20rpx; line-height: 1.45; }
.meta-line { margin-top: 15rpx; display: flex; align-items: center; gap: 12rpx; color: #a8a8a8; font-size: 20rpx; }
.meta-line image { width: 24rpx; height: 24rpx; }
.outline-btn {
  align-self: flex-end;
  width: 148rpx;
  height: 48rpx;
  margin: 14rpx 0 0 auto;
  padding: 0;
  border-radius: 999rpx;
  border: 1rpx solid rgba(242,193,78,.8);
  color: $gold;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 900;
}

.hall-tabs {
  display: flex;
  gap: 16rpx;
  overflow: hidden;
}
.hall-tab {
  min-width: 84rpx;
  height: 40rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a6a6a6;
  background: linear-gradient(180deg, #242424, #171717);
  border: 1rpx solid rgba(255,255,255,.16);
  font-size: 19rpx;
  font-weight: 700;
}
.hall-tab.active { color: #111; background: linear-gradient(180deg, #f8d56b 0%, #e4ad25 100%); border-color: transparent; }

.tool-strip {
  min-height: 92rpx;
  margin-top: 22rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-radius: 8rpx;
  border: 1rpx solid rgba(240,189,50,.58);
  background: linear-gradient(135deg, #1f1f1f, #111);
}
.tool-cell {
  position: relative;
  min-width: 0;
  display: grid;
  grid-template-columns: 50rpx 1fr;
  gap: 12rpx;
  align-items: center;
  padding: 0 25rpx 0 18rpx;
}
.tool-cell::after {
  content: '';
  position: absolute;
  right: 0;
  top: 22rpx;
  width: 1rpx;
  height: 48rpx;
  background: rgba(255,255,255,.13);
}
.tool-cell:last-child::after { display: none; }
.tool-cell image { width: 42rpx; height: 42rpx; }
.tool-title { font-size: 20rpx; font-weight: 900; white-space: nowrap; }
.tool-desc { margin-top: 5rpx; color: #8f8f8f; font-size: 15rpx; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tool-arrow {
  position: absolute;
  right: 9rpx;
  top: 50%;
  transform: translateY(-50%);
  color: #9a9a9a;
  font-size: 28rpx;
  line-height: 1;
}

.works-head { margin-top: 32rpx; }
.work-strip {
  display: flex;
  gap: 16rpx;
  overflow: hidden;
}
.work-card {
  flex: 0 0 255rpx;
  overflow: hidden;
  border-radius: 8rpx;
  background: #171717;
  border: 1rpx solid rgba(255,255,255,.08);
}
.work-image-wrap { position: relative; height: 181rpx; }
.work-image { width: 100%; height: 100%; }
.recommend-badge {
  position: absolute;
  right: 14rpx;
  top: 14rpx;
  height: 38rpx;
  padding: 0 13rpx;
  border-radius: 6rpx;
  display: flex;
  align-items: center;
  color: #111;
  background: linear-gradient(180deg, #f5cf5b 0%, #e2aa25 100%);
  font-size: 19rpx;
  font-weight: 900;
}
.work-info { padding: 15rpx 16rpx 16rpx; }
.work-title { font-size: 21rpx; font-weight: 900; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.work-meta { margin-top: 7rpx; color: #a8a8a8; font-size: 18rpx; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.work-price { margin-top: 10rpx; color: $gold; font-size: 24rpx; font-weight: 700; }

.bottom-spacer { height: 225rpx; }
.bottom-panel {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 60;
  padding: 14rpx 24rpx 10rpx;
  background: linear-gradient(180deg, rgba(5,5,5,.35), rgba(15,15,15,.96) 32%, rgba(15,15,15,.98));
  box-sizing: border-box;
}
.cta-row { display: grid; grid-template-columns: minmax(0, 1fr) 166rpx; gap: 18rpx; }
.main-cta,
.collect-cta {
  height: 70rpx;
  width: 100%;
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 17rpx;
  font-weight: 900;
}
.main-cta {
  color: #111;
  background: linear-gradient(180deg, #f6ca45 0%, #e7ad21 100%);
  font-size: 27rpx;
}
.cta-museum-icon {
  width: 34rpx;
  height: 28rpx;
  position: relative;
  box-sizing: border-box;
  border-left: 4rpx solid #121212;
  border-right: 4rpx solid #121212;
  border-bottom: 4rpx solid #121212;
}
.cta-museum-icon::before {
  content: '';
  position: absolute;
  left: -8rpx;
  top: -12rpx;
  width: 42rpx;
  height: 18rpx;
  border: 4rpx solid #121212;
  border-bottom: 0;
  transform: perspective(80rpx) rotateX(42deg);
  box-sizing: border-box;
}
.cta-museum-icon::after {
  content: '';
  position: absolute;
  top: 4rpx;
  left: 8rpx;
  width: 4rpx;
  height: 20rpx;
  background: #121212;
  box-shadow: 10rpx 0 0 #121212;
}
.collect-cta {
  color: $gold;
  background: #151515;
  font-size: 17rpx;
  gap: 8rpx;
}
.collect-cta image { width: 27rpx; height: 27rpx; }
.tab-row {
  height: 88rpx;
  margin-top: 9rpx;
  display: grid;
  grid-template-columns: 106rpx repeat(3, 1fr) 74rpx;
  align-items: center;
  gap: 16rpx;
}
.edit-pill {
  width: 84rpx;
  height: 64rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 50% 20%, rgba(242,193,78,.16), rgba(255,255,255,.05));
  color: #fff;
  font-size: 25rpx;
  font-weight: 900;
}
.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  color: #8e8e8e;
  font-size: 18rpx;
}
.tab-item image { width: 29rpx; height: 29rpx; opacity: .7; }
.upload-float {
  width: 58rpx;
  height: 58rpx;
  border-radius: 50%;
  background: rgba(255,255,255,.05);
  display: flex;
  align-items: center;
  justify-content: center;
}
.upload-float image { width: 36rpx; height: 36rpx; }
.home-indicator {
  width: 250rpx;
  height: 8rpx;
  margin: 3rpx auto 0;
  border-radius: 999rpx;
  background: #fff;
}

button::after { border: 0; }
</style>
