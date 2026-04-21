<template>
  <view class="message-detail">
    <!-- 消息头部 -->
    <view class="message-header">
      <text class="message-title">系统升级通知</text>
      <text class="message-time">2024年3月15日 10:30</text>
    </view>

    <!-- 消息内容 -->
    <view class="message-content">
      <view class="content-section">
        <text class="section-text">
          尊敬的拾艺局用户：

          为了给您提供更好的服务，我们计划进行系统升级维护。

          【升级时间】
          2024年3月15日 22:00 - 24:00

          【影响范围】
          1. 部分页面可能无法访问
          2. 订单支付功能暂停
          3. 竞拍出价功能暂停

          【温馨提示】
          请您提前完成正在进行的交易和竞拍。升级期间给您带来的不便，敬请谅解！

          感谢您的理解与支持！
        </text>
      </view>

      <!-- 相关操作 -->
      <view class="action-section" v-if="hasAction">
        <text class="action-title">相关操作</text>
        <view class="action-btn" @click="viewOrder">
          <text class="btn-text">查看订单</text>
        </view>
      </view>
    </view>

    <!-- 底部反馈 -->
    <view class="feedback-bar">
      <view class="feedback-item" @click="markUseful">
        <text class="feedback-icon">👍</text>
        <text class="feedback-text">有帮助</text>
      </view>
      <view class="feedback-item" @click="submitFeedback">
        <text class="feedback-icon">📝</text>
        <text class="feedback-text">问题反馈</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      hasAction: true,
      messageId: ''
    }
  },
  onLoad(options) {
    this.messageId = options.id || ''
    // 根据 messageId 加载消息详情
  },
  methods: {
    viewOrder() {
      uni.navigateTo({ url: '/pages/order/list' })
    },
    markUseful() {
      uni.showToast({ title: '感谢您的反馈', icon: 'success' })
    },
    submitFeedback() {
      uni.navigateTo({ url: '/pages/user/feedback' })
    }
  }
}
</script>

<style lang="scss" scoped>
.message-detail {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.message-header {
  background: #fff;
  padding: 30rpx 24rpx;
  border-bottom: 2rpx solid #f5f5f5;
}

.message-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #333;
  display: block;
  margin-bottom: 12rpx;
}

.message-time {
  font-size: 24rpx;
  color: #999;
}

.message-content {
  padding: 24rpx;
}

.content-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx 24rpx;
  margin-bottom: 24rpx;
}

.section-text {
  font-size: 28rpx;
  color: #333;
  line-height: 1.8;
  white-space: pre-wrap;
}

.action-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}

.action-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
}

.action-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24rpx;
  border-radius: 12rpx;
  text-align: center;
}

.btn-text {
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
}

.feedback-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  display: flex;
  justify-content: center;
  gap: 60rpx;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.feedback-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.feedback-icon {
  font-size: 40rpx;
}

.feedback-text {
  font-size: 24rpx;
  color: #666;
}
</style>