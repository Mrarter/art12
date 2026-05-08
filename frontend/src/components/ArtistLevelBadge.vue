<template>
  <view class="level-badge" :class="levelClass">
    <view class="level-inner">
      <view class="level-main">{{ level || 'D' }}</view>
      <view class="level-text">艺术家等级</view>
    </view>
    <view class="score-section">
      <text class="score-value">{{ score || 0 }}</text>
      <text class="score-unit">分</text>
    </view>
  </view>
</template>

<script>
export default {
  name: 'ArtistLevelBadge',
  props: {
    level: { type: String, default: 'D' },
    score: { type: Number, default: 0 }
  },
  computed: {
    levelClass() {
      return `level-${this.level === 'A+' ? 'Aplus' : (this.level || 'D')}`
    }
  }
}
</script>

<style lang="scss" scoped>
.level-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  border-radius: 20rpx;
  background: #1a1a1a;
  border: 1rpx solid rgba(255, 255, 255, 0.06);

  &.level-Aplus { .level-main { background: linear-gradient(135deg, #d4af37, #f0d68a); } }
  &.level-A  { .level-main { background: linear-gradient(135deg, #c9a227, #e6c65c); } }
  &.level-B  { .level-main { background: linear-gradient(135deg, #8a8a8a, #b8b8b8); } }
  &.level-C  { .level-main { background: linear-gradient(135deg, #a67c52, #c9a87c); } }
  &.level-D  { .level-main { background: linear-gradient(135deg, #666, #999); } }
}

.level-inner {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.level-main {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  font-weight: 700;
  color: #0d0d0d;
  flex-shrink: 0;
}

.level-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #fff;
}

.score-section {
  display: flex;
  align-items: baseline;
  gap: 4rpx;
}

.score-value {
  font-size: 40rpx;
  font-weight: 700;
  color: #d4af37;
}

.score-unit {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.5);
}
</style>
