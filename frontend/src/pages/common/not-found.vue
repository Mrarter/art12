<template>
  <view class="not-found-page">
    <view class="hero">
      <text class="hero-kicker">ART SALON</text>
      <text class="hero-title">今夜不如先看几幅油画</text>
      <text class="hero-desc">
        页面暂时失联，但审美不必中断。我们挑了三幅偏安静的作品，陪您把这几秒钟过得值得一点。
      </text>
      <text class="hero-note">当前服务器有点 404，稍后回来</text>
    </view>

    <view class="gallery">
      <view v-for="item in paintings" :key="item.title" class="painting-card">
        <image class="painting-image" :src="item.image" mode="aspectFill"></image>
        <view class="painting-meta">
          <text class="painting-title">{{ item.title }}</text>
          <text class="painting-subtitle">{{ item.subtitle }}</text>
          <text class="painting-review">{{ item.review }}</text>
        </view>
      </view>
    </view>

    <view class="actions">
      <button class="action action-primary" @click="goHome">回到首页</button>
      <button class="action action-secondary" @click="goBack">稍后再看</button>
    </view>

    <view v-if="sourcePath" class="path-chip">
      <text class="path-label">您刚刚访问的是</text>
      <text class="path-value">{{ sourcePath }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import boatPainting from '@/asset-archive/static/images/artist-home-work-boat.png'
import girlPainting from '@/asset-archive/static/images/artist-home-work-girl.png'
import stillPainting from '@/asset-archive/static/images/artist-home-work-still.png'

const paintings = [
  {
    title: '湖舟微光',
    subtitle: '光线从深水里慢慢浮上来',
    review: '这一幅最动人的不是船，而是压低后的天空和水面反光。画面很静，却留住了临近傍晚时那种呼吸感。',
    image: boatPainting
  },
  {
    title: '侧影与花束',
    subtitle: '人物被柔光轻轻托住',
    review: '颜色控制得很克制，皮肤、裙摆和背景之间没有硬碰硬的对比，所以人物显得温柔，也更有停留感。',
    image: girlPainting
  },
  {
    title: '桌上静物',
    subtitle: '把日常摆成了耐看的秩序',
    review: '静物画最难的是“稳”。这张的器物关系不吵闹，明暗层次慢慢铺开，看久了会觉得空间很松弛。',
    image: stillPainting
  }
]

const sourcePath = computed(() => {
  const pages = getCurrentPages?.() || []
  const current = pages[pages.length - 1]
  const options = current?.options || {}
  return decodeURIComponent(options.from || options.path || '')
})

const goHome = () => {
  uni.reLaunch({ url: '/pages/index/index' })
}

const goBack = () => {
  const pages = getCurrentPages?.() || []
  if (pages.length > 1) {
    uni.navigateBack({ delta: 1 })
    return
  }
  goHome()
}
</script>

<style lang="scss" scoped>
$gold: #d8b25a;
$gold-soft: #f0e0b6;
$text: #f8f2e7;
$muted: rgba(248, 242, 231, 0.7);
$line: rgba(216, 178, 90, 0.24);

.not-found-page {
  min-height: 100vh;
  padding: calc(56rpx + env(safe-area-inset-top)) 28rpx calc(48rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background:
    radial-gradient(circle at 15% 8%, rgba(216, 178, 90, 0.15), transparent 22%),
    radial-gradient(circle at 85% 18%, rgba(216, 178, 90, 0.1), transparent 20%),
    linear-gradient(180deg, #0c0a08 0%, #090807 46%, #050505 100%);
  color: $text;
}

.hero {
  margin-bottom: 36rpx;
}

.hero-kicker {
  display: inline-block;
  margin-bottom: 18rpx;
  padding: 8rpx 18rpx;
  border: 1rpx solid rgba(216, 178, 90, 0.28);
  border-radius: 999rpx;
  color: rgba(240, 224, 182, 0.92);
  font-size: 20rpx;
  letter-spacing: 4rpx;
}

.hero-title {
  display: block;
  margin-bottom: 18rpx;
  font-size: 56rpx;
  line-height: 1.2;
  font-weight: 600;
}

.hero-desc {
  display: block;
  margin-bottom: 14rpx;
  color: $muted;
  font-size: 28rpx;
  line-height: 1.8;
}

.hero-note {
  display: block;
  color: rgba(240, 224, 182, 0.72);
  font-size: 22rpx;
  line-height: 1.6;
}

.gallery {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.painting-card {
  overflow: hidden;
  border-radius: 30rpx;
  border: 1rpx solid $line;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0.015)),
    linear-gradient(145deg, rgba(23, 19, 15, 0.96), rgba(9, 8, 7, 0.98));
  box-shadow: 0 24rpx 60rpx rgba(0, 0, 0, 0.35);
}

.painting-image {
  width: 100%;
  height: 380rpx;
  display: block;
}

.painting-meta {
  padding: 24rpx;
}

.painting-title {
  display: block;
  margin-bottom: 8rpx;
  font-size: 34rpx;
  font-weight: 600;
}

.painting-subtitle {
  display: block;
  margin-bottom: 14rpx;
  color: rgba(240, 224, 182, 0.78);
  font-size: 22rpx;
  letter-spacing: 1rpx;
}

.painting-review {
  display: block;
  color: $muted;
  font-size: 26rpx;
  line-height: 1.8;
}

.actions {
  margin-top: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.action {
  height: 92rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
}

.action-primary {
  color: #201708;
  background: linear-gradient(135deg, #f0e0b6 0%, #d8b25a 54%, #b8862d 100%);
  box-shadow: 0 14rpx 28rpx rgba(184, 134, 45, 0.24);
}

.action-secondary {
  color: $text;
  border: 1rpx solid rgba(216, 178, 90, 0.2);
  background: rgba(255, 255, 255, 0.03);
}

.path-chip {
  margin-top: 26rpx;
  padding: 18rpx 22rpx;
  border-radius: 22rpx;
  border: 1rpx solid rgba(216, 178, 90, 0.12);
  background: rgba(255, 255, 255, 0.02);
}

.path-label {
  display: block;
  margin-bottom: 8rpx;
  color: rgba(240, 224, 182, 0.7);
  font-size: 22rpx;
}

.path-value {
  display: block;
  color: rgba(248, 242, 231, 0.62);
  font-size: 22rpx;
  line-height: 1.7;
  word-break: break-all;
}
</style>
