<template>
  <view class="message-settings">
    <!-- 消息提醒设置 -->
    <view class="settings-section">
      <text class="section-title">消息提醒</text>
      <view class="setting-item">
        <view class="setting-info">
          <text class="setting-name">系统通知</text>
          <text class="setting-desc">接收系统公告、活动通知等</text>
        </view>
        <switch :checked="settings.system" @change="toggleSetting('system')" color="#667eea" />
      </view>
      <view class="setting-item">
        <view class="setting-info">
          <text class="setting-name">订单消息</text>
          <text class="setting-desc">订单状态变更、物流通知等</text>
        </view>
        <switch :checked="settings.order" @change="toggleSetting('order')" color="#667eea" />
      </view>
      <view class="setting-item">
        <view class="setting-info">
          <text class="setting-name">拍卖提醒</text>
          <text class="setting-desc">拍卖开始、出价提醒等</text>
        </view>
        <switch :checked="settings.auction" @change="toggleSetting('auction')" color="#667eea" />
      </view>
      <view class="setting-item">
        <view class="setting-info">
          <text class="setting-name">互动消息</text>
          <text class="setting-desc">点赞、评论、关注等</text>
        </view>
        <switch :checked="settings.interact" @change="toggleSetting('interact')" color="#667eea" />
      </view>
    </view>

    <!-- 推送方式 -->
    <view class="settings-section">
      <text class="section-title">推送方式</text>
      <view class="setting-item">
        <view class="setting-info">
          <text class="setting-name">应用内推送</text>
          <text class="setting-desc">在应用内显示通知</text>
        </view>
        <switch :checked="settings.push" @change="toggleSetting('push')" color="#667eea" />
      </view>
      <view class="setting-item">
        <view class="setting-info">
          <text class="setting-name">短信通知</text>
          <text class="setting-desc">重要消息发送短信提醒</text>
        </view>
        <switch :checked="settings.sms" @change="toggleSetting('sms')" color="#667eea" />
      </view>
    </view>

    <!-- 静默时间 -->
    <view class="settings-section">
      <text class="section-title">消息免打扰</text>
      <view class="setting-item">
        <view class="setting-info">
          <text class="setting-name">开启免打扰</text>
          <text class="setting-desc">在指定时间段内不接收推送</text>
        </view>
        <switch :checked="settings.quiet" @change="toggleSetting('quiet')" color="#667eea" />
      </view>
      <view class="setting-item" v-if="settings.quiet" @click="selectTime">
        <view class="setting-info">
          <text class="setting-name">免打扰时段</text>
          <text class="setting-desc">{{ settings.startTime }} - {{ settings.endTime }}</text>
        </view>
        <text class="arrow">></text>
      </view>
    </view>

    <!-- 保存按钮 -->
    <view class="save-btn" @click="saveSettings">
      <text class="btn-text">保存设置</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      settings: {
        system: true,
        order: true,
        auction: true,
        interact: true,
        push: true,
        sms: false,
        quiet: false,
        startTime: '22:00',
        endTime: '08:00'
      }
    }
  },
  methods: {
    toggleSetting(key) {
      this.settings[key] = !this.settings[key]
    },
    selectTime() {
      uni.showToast({ title: '选择时间段', icon: 'none' })
    },
    saveSettings() {
      uni.showToast({ title: '设置已保存', icon: 'success' })
      setTimeout(() => {
        uni.navigateBack()
      }, 1000)
    }
  }
}
</script>

<style lang="scss" scoped>
.message-settings {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 24rpx;
  padding-bottom: 150rpx;
}

.settings-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 20rpx;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f5f5f5;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-info {
  flex: 1;
}

.setting-name {
  font-size: 28rpx;
  color: #333;
  display: block;
}

.setting-desc {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
  display: block;
}

.arrow {
  color: #ccc;
  font-size: 28rpx;
}

.save-btn {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.btn-text {
  display: block;
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 44rpx;
}
</style>