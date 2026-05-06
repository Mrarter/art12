<template>
  <view class="trust-page">
    <view class="topbar">
      <text class="back" @click="goBack">‹</text>
      <text class="title">收藏信任</text>
      <text class="spacer"></text>
    </view>

    <view class="hero">
      <image class="hero-image" src="/static/artist-ui/collection-trust.png" mode="aspectFill"></image>
      <view class="hero-copy">
        <text class="kicker">PLATFORM TRUST</text>
        <text class="hero-title">从认证到流通，每一步都有记录</text>
        <text class="hero-desc">拾艺局为艺术家作品提供收藏证书、流通记录、保管支持和顾问服务。</text>
      </view>
    </view>

    <view class="trust-grid">
      <view v-for="item in trustItems" :key="item.title" class="trust-card">
        <text class="trust-title">{{ item.title }}</text>
        <text class="trust-desc">{{ item.desc }}</text>
      </view>
    </view>

    <view class="faq-card">
      <view class="section-title">收藏说明 FAQ</view>
      <view v-for="faq in faqs" :key="faq.q" class="faq-item">
        <text class="question">{{ faq.q }}</text>
        <text class="answer">{{ faq.a }}</text>
      </view>
    </view>

    <view class="bottom-cta">
      <button class="ghost-btn" @click="toast('证书样例已打开')">查看证书样例</button>
      <button class="gold-btn" @click="toast('已联系收藏顾问')">咨询收藏顾问</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      trustItems: [
        { title: '平台认证', desc: '艺术家身份与作品信息由平台审核留档' },
        { title: '收藏证书', desc: '每件作品生成唯一证书与展示编号' },
        { title: '流通记录', desc: '收藏、转让、托管节点可追溯' },
        { title: '保管支持', desc: '为高价值作品提供平台保管协助' },
        { title: '顾问服务', desc: '收藏顾问协助完成咨询与撮合' },
        { title: '支付保障', desc: '交易资金按平台规则完成安全结算' }
      ],
      faqs: [
        { q: '证书如何生成？', a: '作品上架后由平台根据作品编号、作者信息和图像记录生成。' },
        { q: '再次流通需要审核吗？', a: '需要。平台会复核权属、作品状态和当前展示信息。' },
        { q: '可以先咨询再收藏吗？', a: '可以。收藏顾问会协助确认作品信息、价格区间和交付方式。' }
      ]
    }
  },
  methods: {
    goBack() { uni.navigateBack() },
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
$border-dark: rgba(214, 168, 39, 0.35);

.trust-page {
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

.hero {
  position: relative;
  height: 500rpx;
  overflow: hidden;
  border-radius: 32rpx;
}

.hero-image {
  width: 100%;
  height: 100%;
}

.hero::after {
  position: absolute;
  inset: 0;
  content: "";
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.82));
}

.hero-copy {
  position: absolute;
  right: 30rpx;
  bottom: 34rpx;
  left: 30rpx;
  z-index: 1;
}

.kicker {
  color: $gold-light;
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
.answer {
  color: $text-secondary;
  font-size: 24rpx;
  line-height: 36rpx;
}

.trust-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18rpx;
  margin: 28rpx 0;
}

.trust-card,
.faq-card {
  border: 1rpx solid rgba(255, 255, 255, 0.07);
  background: linear-gradient(180deg, $card-bg-2, $card-bg);
}

.trust-card {
  min-height: 178rpx;
  padding: 24rpx;
  border-radius: 26rpx;
}

.trust-title {
  display: block;
  margin-bottom: 12rpx;
  color: $gold-light;
  font-size: 28rpx;
  font-weight: 700;
}

.faq-card {
  padding: 28rpx;
  border-radius: 30rpx;
}

.faq-item {
  padding: 24rpx 0;
  border-top: 1rpx solid rgba(255, 255, 255, 0.07);
}

.question,
.answer {
  display: block;
}

.question {
  margin-bottom: 10rpx;
  font-size: 28rpx;
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
