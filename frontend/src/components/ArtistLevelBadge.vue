<template>
  <view class="level-badge" :class="levelClass">
    <view class="level-inner">
      <view class="level-main">{{ displayLevel }}</view>
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
    level: { type: String, default: 'U' },
    score: { type: Number, default: 0 }
  },
  computed: {
    levelClass() {
      const levelMap = {
        'S+': 'Splus',
        'S': 'S',
        'A+': 'Aplus',
        'A': 'A',
        'B': 'B',
        'U': 'U'
      }
      return `level-${levelMap[this.level] || 'U'}`
    },
    displayLevel() {
      if (this.level === 'U') return '未认证'
      return this.level || '未认证'
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

  // S+等级 - 最高级
  &.level-Splus { .level-main { background: linear-gradient(135deg, #ff6b6b, #ffd93d); } }
  // S等级
  &.level-S { .level-main { background: linear-gradient(135deg, #ff4757, #ff6b81); } }
  // A+等级
  &.level-Aplus { .level-main { background: linear-gradient(135deg, #d4af37, #f0d68a); } }
  // A等级
  &.level-A  { .level-main { background: linear-gradient(135deg, #c9a227, #e6c65c); } }
  // B等级
  &.level-B  { .level-main { background: linear-gradient(135deg, #8a8a8a, #b8b8b8); } }
  // C等级
  &.level-C  { .level-main { background: linear-gradient(135deg, #a67c52, #c9a87c); } }
  // 未认证
  &.level-U  { .level-main { background: linear-gradient(135deg, #666, #999); color: #fff; font-size: 20px; } }
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
