<template>
  <view class="page">
    <view class="result-card">
      <view class="icon-wrap">
        <text class="icon">✓</text>
      </view>
      <view class="title">收藏意向已提交</view>
      <view class="sub">您的收藏请求 {{ intentId }} 已提交成功。</view>
      <view class="sub">平台艺术顾问将在 24 小时内与您联系确认作品状态、交付方式与收藏证书安排。</view>
    </view>

    <view class="info-card">
      <view class="info-title">接下来会发生什么</view>
      <view class="step">
        <text class="step-num">1</text>
        <view><b>艺术顾问联系确认</b><p>我们的专业艺术顾问将在 24 小时内通过电话或微信与您沟通确认意向单细节。</p></view>
      </view>
      <view class="step">
        <text class="step-num">2</text>
        <view><b>作品锁定与合同签署</b><p>确认后锁定作品状态并生成收藏合同，藏家在线签署后进入支付流程。</p></view>
      </view>
      <view class="step">
        <text class="step-num">3</text>
        <view><b>支付与收藏完成</b><p>完成支付后生成收藏证书，选择寄送或平台保管，作品进入流通记录。</p></view>
      </view>
    </view>

    <view class="actions">
      <button class="btn primary" @click="goHome">返回首页</button>
      <button class="btn light" @click="goOrderList">查看我的意向</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      intentId: ''
    }
  },

  onLoad(options) {
    this.intentId = options.intentId || ''
  },

  methods: {
    goHome() {
      uni.reLaunch({ url: '/pages/index/index' })
    },
    goOrderList() {
      uni.navigateTo({ url: '/pages/user/index?tab=intents' })
    }
  }
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f7f3eb; padding: 60rpx 32rpx; }
.result-card { text-align: center; padding: 60rpx 32rpx; background: #fff; border-radius: 24rpx; margin-bottom: 24rpx;
  .icon-wrap { width: 120rpx; height: 120rpx; border-radius: 50%; background: #315f42; display: flex; align-items: center; justify-content: center; margin: 0 auto 24rpx; }
  .icon { font-size: 48rpx; color: #fff; font-weight: 700; }
  .title { font-size: 36rpx; font-weight: 700; color: #1d1d1f; margin-bottom: 16rpx; }
  .sub { font-size: 26rpx; color: #8a8178; line-height: 1.6; margin-top: 12rpx; }
}
.info-card { padding: 32rpx; background: #fff; border-radius: 24rpx; margin-bottom: 40rpx;
  .info-title { font-size: 30rpx; font-weight: 700; color: #1d1d1f; margin-bottom: 28rpx; }
  .step { display: flex; gap: 20rpx; margin-bottom: 28rpx;
    &:last-child { margin-bottom: 0; }
    .step-num { width: 48rpx; height: 48rpx; border-radius: 50%; background: #f5f0e8; display: flex; align-items: center; justify-content: center; font-size: 24rpx; font-weight: 700; color: #b3261e; flex-shrink: 0; }
    b { display: block; font-size: 28rpx; margin-bottom: 6rpx; color: #1d1d1f; }
    p { margin: 0; font-size: 24rpx; color: #8a8178; line-height: 1.5; }
  }
}
.actions { display: flex; flex-direction: column; gap: 16rpx;
  .btn { height: 88rpx; border-radius: 999rpx; border: none; font-size: 28rpx; &.primary { background: #b3261e; color: #fff; } &.light { background: #fff; color: #1d1d1f; border: 2rpx solid #e2d8cc; } }
}
</style>
