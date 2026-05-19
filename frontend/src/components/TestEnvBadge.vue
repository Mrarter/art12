<template>
  <view class="test-env-badge" v-if="visible" @click="onTap">
    <text class="badge-dot"></text>
    <text class="badge-text">测试</text>
  </view>
</template>

<script>
import { getEnvInfo } from '@/utils/env'

export default {
  name: 'TestEnvBadge',
  data() {
    return {
      visible: false
    }
  },
  created() {
    const env = getEnvInfo()
    this.visible = env.isTest
  },
  methods: {
    onTap() {
      const env = getEnvInfo()
      uni.showToast({
        title: `当前环境: ${env.label || '测试'}\nAPI: ${import.meta.env?.VITE_API_BASE_URL || '—'}`,
        icon: 'none',
        duration: 3000
      })
    }
  }
}
</script>

<style scoped>
.test-env-badge {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 99999;
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 4rpx 14rpx 4rpx 10rpx;
  background: linear-gradient(135deg, rgba(255, 77, 79, 0.92), rgba(255, 77, 79, 0.78));
  border-bottom-right-radius: 12rpx;
  pointer-events: auto;
}
.badge-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #fff;
  animation: blink 1.2s ease-in-out infinite;
}
.badge-text {
  font-size: 20rpx;
  color: #fff;
  font-weight: 700;
  letter-spacing: 1rpx;
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
