<template>
  <view class="circulation-page">
    <view class="topbar">
      <text class="back" @click="goBack">‹</text>
      <text class="title">作品流通</text>
      <text class="spacer"></text>
    </view>

    <view class="hero-card">
      <image class="hero-image" src="../static/artist-ui/circulation-entry.png" mode="aspectFill"></image>
      <view class="hero-copy">
        <text class="kicker">ARTWORK CIRCULATION</text>
        <text class="hero-title">让收藏从确权开始流通</text>
        <text class="hero-desc">平台提供证书、流通记录、保管支持与顾问服务，帮助艺术家作品建立持续收藏价值。</text>
      </view>
    </view>

    <view class="trust-row">
      <view v-for="item in trustItems" :key="item.title" class="trust-card">
        <text class="trust-title">{{ item.title }}</text>
        <text class="trust-desc">{{ item.desc }}</text>
      </view>
    </view>

    <scroll-view scroll-x class="tabs-scroll">
      <view class="tabs">
        <text v-for="tab in tabs" :key="tab.value" class="tab" :class="{ active: activeTab === tab.value }" @click="activeTab = tab.value">{{ tab.label }}</text>
      </view>
    </scroll-view>

    <view class="work-list">
      <view v-for="work in filteredWorks" :key="work.id" class="work-card" @click="goWork(work.id)">
        <image class="work-cover" :src="work.cover" mode="aspectFill"></image>
        <view class="work-main">
          <view class="work-line">
            <text class="work-title">{{ work.title }}</text>
            <text class="status">{{ work.statusText }}</text>
          </view>
          <text class="work-meta">{{ work.material }} / {{ work.size }} / {{ work.year }}</text>
          <text class="price">{{ work.priceText }}</text>
        </view>
      </view>
    </view>

    <view class="flow-card">
      <view class="section-title">流通说明</view>
      <view v-for="(step, index) in steps" :key="step" class="step">
        <text class="step-no">0{{ index + 1 }}</text>
        <text class="step-text">{{ step }}</text>
      </view>
    </view>

    <view class="bottom-cta">
      <button class="ghost-btn" @click="toast('已发起收藏咨询')">发起收藏咨询</button>
      <button class="gold-btn" @click="toast('已提交再次流通申请')">申请再次流通</button>
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
        { label: '在售', value: 'ON_SALE' },
        { label: '流通中', value: 'CIRCULATING' },
        { label: '已收藏', value: 'COLLECTED' },
        { label: '可申请流通', value: 'CAN_APPLY' }
      ],
      trustItems: [
        { title: '收藏证书', desc: '一作一证' },
        { title: '流通记录', desc: '全链路留痕' },
        { title: '平台保管', desc: '可选托管' }
      ],
      steps: ['确认作品权属与证书信息', '平台审核作品状态与估值区间', '进入流通展示并由顾问撮合收藏'],
      works: [
        { id: 49, title: '晨曦·归航', material: '布面油画', size: '100×80cm', year: '2024', priceText: '¥8,000', status: 'ON_SALE', statusText: '在售', cover: '/pages/artist/static/artist-ui/personal-gallery.png' },
        { id: 47, title: '秋日', material: '布面油画', size: '80×60cm', year: '2024', priceText: '¥12,000', status: 'COLLECTED', statusText: '已收藏', cover: '/pages/artist/static/artist-ui/artist-homepage-dark.png' },
        { id: 46, title: '静物 No.0751', material: '布面油画', size: '40×40cm', year: '2024', priceText: '¥3,200', status: 'CIRCULATING', statusText: '流通中', cover: '/pages/artist/static/artist-ui/collection-trust.png' },
        { id: 45, title: '海边风景', material: '布面油画', size: '60×60cm', year: '2023', priceText: '待估值', status: 'CAN_APPLY', statusText: '可申请流通', cover: '/pages/artist/static/artist-ui/artist-homepage-alt.png' }
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
    goWork(id) { uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` }) },
    toast(title) { uni.showToast({ title, icon: 'none' }) }
  }
}
</script>

<style lang="scss" scoped>
$page-bg: #050505;
$card-bg: #171717;
$card-bg-2: #202020;
$gold: #d6a827;
$gold-light: #f1c84b;
$text-main: #fff;
$text-secondary: #b7b7b7;
$text-muted: #7d7d7d;
$border-dark: rgba(214, 168, 39, 0.35);

.circulation-page {
  min-height: 100vh;
  padding: 0 28rpx 150rpx;
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

.title,
.section-title {
  font-size: 32rpx;
  font-weight: 700;
}

.hero-card {
  position: relative;
  height: 470rpx;
  overflow: hidden;
  border-radius: 32rpx;
}

.hero-image {
  width: 100%;
  height: 100%;
}

.hero-card::after {
  position: absolute;
  inset: 0;
  content: "";
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.82));
}

.hero-copy {
  position: absolute;
  right: 28rpx;
  bottom: 30rpx;
  left: 28rpx;
  z-index: 1;
}

.kicker,
.price,
.status {
  color: $gold-light;
}

.kicker {
  font-size: 20rpx;
}

.hero-title {
  display: block;
  margin: 14rpx 0;
  font-size: 42rpx;
  font-weight: 700;
}

.hero-desc,
.trust-desc,
.work-meta,
.step-text {
  color: $text-secondary;
  font-size: 24rpx;
  line-height: 36rpx;
}

.trust-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin: 26rpx 0;
}

.trust-card,
.work-card,
.flow-card {
  border: 1rpx solid rgba(255, 255, 255, 0.07);
  background: linear-gradient(180deg, $card-bg-2, $card-bg);
}

.trust-card {
  min-height: 130rpx;
  padding: 18rpx;
  border-radius: 24rpx;
}

.trust-title {
  display: block;
  margin-bottom: 10rpx;
  font-size: 26rpx;
  font-weight: 700;
}

.tabs-scroll {
  width: 100%;
  white-space: nowrap;
}

.tabs {
  display: inline-flex;
  gap: 16rpx;
  padding-bottom: 20rpx;
}

.tab {
  flex-shrink: 0;
  white-space: nowrap;
  padding: 16rpx 26rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.09);
  border-radius: 999rpx;
  color: $text-muted;
  font-size: 24rpx;
  line-height: 1;
}

.tab.active {
  border-color: $border-dark;
  color: $gold-light;
  background: rgba(214, 168, 39, 0.12);
}

.work-list {
  display: grid;
  gap: 20rpx;
}

.work-card {
  display: flex;
  gap: 18rpx;
  padding: 18rpx;
  border-radius: 26rpx;
}

.work-cover {
  width: 172rpx;
  height: 172rpx;
  border-radius: 20rpx;
}

.work-main {
  flex: 1;
}

.work-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.work-title {
  font-size: 30rpx;
  font-weight: 700;
}

.status {
  flex-shrink: 0;
  padding: 6rpx 12rpx;
  border: 1rpx solid $border-dark;
  border-radius: 999rpx;
  font-size: 20rpx;
}

.price {
  display: block;
  margin-top: 20rpx;
  font-size: 32rpx;
  font-weight: 700;
}

.flow-card {
  margin-top: 28rpx;
  padding: 28rpx;
  border-radius: 28rpx;
}

.step {
  display: flex;
  gap: 18rpx;
  padding-top: 22rpx;
}

.step-no {
  color: $gold;
  font-size: 24rpx;
  font-weight: 700;
}

.bottom-cta {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  padding: 20rpx 28rpx 34rpx;
  background: rgba(5, 5, 5, 0.96);
}

.ghost-btn,
.gold-btn {
  height: 84rpx;
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
</style>
