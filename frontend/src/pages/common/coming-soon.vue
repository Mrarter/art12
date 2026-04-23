<template>
  <view class="coming-soon-page">
    <view class="icon-wrap">
      <text class="icon">敬</text>
    </view>
    <text class="title">{{ pageTitle }}</text>
    <text class="desc">{{ pageDesc }}</text>
    <button class="back-btn" @click="goBack">返回上一页</button>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const pageTitle = computed(() => decodeURIComponent(getQuery('title', '功能建设中')))
const pageDesc = computed(() => decodeURIComponent(getQuery('desc', '这个页面还在完善中，先把跳转兜住，避免出现页面不存在。')))

function getQuery(key, fallback) {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  return current?.options?.[key] || fallback
}

function goBack() {
  if (getCurrentPages().length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.coming-soon-page {
  min-height: 100vh;
  padding: 120rpx 48rpx 80rpx;
  background: linear-gradient(180deg, #101114 0%, #181b22 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.icon-wrap {
  width: 144rpx;
  height: 144rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 35% 35%, rgba(53, 61, 79, 0.95), rgba(27, 31, 40, 1));
  border: 1rpx solid rgba(216, 181, 74, 0.3);
  box-shadow: inset 0 0 0 1rpx rgba(255, 255, 255, 0.04);
  margin-bottom: 36rpx;
}

.icon {
  color: #d8b54a;
  font-size: 54rpx;
  font-weight: 600;
}

.title {
  color: #ffffff;
  font-size: 38rpx;
  font-weight: 600;
  margin-bottom: 18rpx;
}

.desc {
  color: #a7adba;
  font-size: 28rpx;
  line-height: 1.7;
  margin-bottom: 56rpx;
}

.back-btn {
  width: 320rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  background: #d8b54a;
  color: #111111;
  font-size: 30rpx;
  font-weight: 600;
}
</style>
