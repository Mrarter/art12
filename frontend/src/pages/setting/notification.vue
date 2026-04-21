<template>
  <view class="setting-page">
    <!-- 消息通知 -->
    <view class="setting-section">
      <view class="section-header">
        <text class="section-title">消息通知</text>
      </view>
      <view class="setting-list">
        <view class="setting-item">
          <view class="item-info">
            <text class="item-title">订单消息</text>
            <text class="item-desc">订单状态变更、发货提醒等</text>
          </view>
          <switch 
            :checked="notifications.order" 
            @change="(e) => toggleSwitch('order', e.detail.value)"
            color="#667eea"
          />
        </view>
        <view class="setting-item">
          <view class="item-info">
            <text class="item-title">拍卖提醒</text>
            <text class="item-desc">拍卖开始、结拍提醒</text>
          </view>
          <switch 
            :checked="notifications.auction" 
            @change="(e) => toggleSwitch('auction', e.detail.value)"
            color="#667eea"
          />
        </view>
        <view class="setting-item">
          <view class="item-info">
            <text class="item-title">私信消息</text>
            <text class="item-desc">艺术家、藏家私信通知</text>
          </view>
          <switch 
            :checked="notifications.chat" 
            @change="(e) => toggleSwitch('chat', e.detail.value)"
            color="#667eea"
          />
        </view>
        <view class="setting-item">
          <view class="item-info">
            <text class="item-title">系统公告</text>
            <text class="item-desc">平台活动、系统更新通知</text>
          </view>
          <switch 
            :checked="notifications.system" 
            @change="(e) => toggleSwitch('system', e.detail.value)"
            color="#667eea"
          />
        </view>
        <view class="setting-item">
          <view class="item-info">
            <text class="item-title">推广收益</text>
            <text class="item-desc">佣金到账、团队收益通知</text>
          </view>
          <switch 
            :checked="notifications.promotion" 
            @change="(e) => toggleSwitch('promotion', e.detail.value)"
            color="#667eea"
          />
        </view>
      </view>
    </view>

    <!-- 推送设置 -->
    <view class="setting-section">
      <view class="section-header">
        <text class="section-title">推送方式</text>
      </view>
      <view class="setting-list">
        <view class="setting-item">
          <view class="item-info">
            <text class="item-title">应用内推送</text>
            <text class="item-desc">APP内消息提醒</text>
          </view>
          <switch 
            :checked="pushSetting.inApp" 
            @change="(e) => togglePush('inApp', e.detail.value)"
            color="#667eea"
          />
        </view>
        <view class="setting-item">
          <view class="item-info">
            <text class="item-title">短信通知</text>
            <text class="item-desc">重要消息短信提醒</text>
          </view>
          <switch 
            :checked="pushSetting.sms" 
            @change="(e) => togglePush('sms', e.detail.value)"
            color="#667eea"
          />
        </view>
        <view class="setting-item">
          <view class="item-info">
            <text class="item-title">邮件通知</text>
            <text class="item-desc">重要通知邮件发送</text>
          </view>
          <switch 
            :checked="pushSetting.email" 
            @change="(e) => togglePush('email', e.detail.value)"
            color="#667eea"
          />
        </view>
      </view>
    </view>

    <!-- 免打扰设置 -->
    <view class="setting-section">
      <view class="section-header">
        <text class="section-title">免打扰</text>
      </view>
      <view class="setting-list">
        <view class="setting-item">
          <view class="item-info">
            <text class="item-title">开启免打扰</text>
            <text class="item-desc">定时静音推送通知</text>
          </view>
          <switch 
            :checked="quietMode.enabled" 
            @change="(e) => toggleQuiet(e.detail.value)"
            color="#667eea"
          />
        </view>
        <view class="setting-item" v-if="quietMode.enabled" @click="showTimePicker">
          <view class="item-info">
            <text class="item-title">免打扰时段</text>
            <text class="item-desc">{{ quietMode.startTime }} - {{ quietMode.endTime }}</text>
          </view>
          <u-icon name="arrow-right" size="18" color="#ccc"></u-icon>
        </view>
      </view>
    </view>

    <!-- 清空消息 -->
    <view class="setting-section">
      <view class="setting-list">
        <view class="setting-item danger" @click="clearMessages">
          <text class="item-title">清空消息记录</text>
        </view>
        <view class="setting-item danger" @click="clearCache">
          <text class="item-title">清除缓存</text>
          <text class="item-value">{{ cacheSize }}</text>
        </view>
      </view>
    </view>

    <!-- 版本信息 -->
    <view class="version-info">
      <text class="version-text">拾艺局 v1.0.0</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const notifications = ref({
  order: true,
  auction: true,
  chat: true,
  system: false,
  promotion: true
})

const pushSetting = ref({
  inApp: true,
  sms: false,
  email: false
})

const quietMode = ref({
  enabled: false,
  startTime: '22:00',
  endTime: '08:00'
})

const cacheSize = ref('128 MB')

const toggleSwitch = (key, value) => {
  notifications.value[key] = value
  saveSettings()
}

const togglePush = (key, value) => {
  pushSetting.value[key] = value
  saveSettings()
}

const toggleQuiet = (value) => {
  quietMode.value.enabled = value
  saveSettings()
}

const showTimePicker = () => {
  uni.showToast({ title: '时间选择器', icon: 'none' })
}

const clearMessages = () => {
  uni.showModal({
    title: '清空消息',
    content: '确定要清空所有消息记录吗？',
    success: (res) => {
      if (res.confirm) {
        uni.showToast({ title: '已清空', icon: 'success' })
      }
    }
  })
}

const clearCache = () => {
  uni.showModal({
    title: '清除缓存',
    content: '确定要清除缓存吗？',
    success: (res) => {
      if (res.confirm) {
        cacheSize.value = '0 MB'
        uni.showToast({ title: '已清除', icon: 'success' })
      }
    }
  })
}

const saveSettings = () => {
  // 保存设置到本地存储或服务器
  uni.setStorageSync('notificationSettings', {
    notifications: notifications.value,
    pushSetting: pushSetting.value,
    quietMode: quietMode.value
  })
}

onMounted(() => {
  // 从本地存储加载设置
  const settings = uni.getStorageSync('notificationSettings')
  if (settings) {
    notifications.value = settings.notifications || notifications.value
    pushSetting.value = settings.pushSetting || pushSetting.value
    quietMode.value = settings.quietMode || quietMode.value
  }
})
</script>

<style lang="scss" scoped>
.setting-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: env(safe-area-inset-bottom);
}

.setting-section {
  margin-top: 20rpx;

  .section-header {
    padding: 24rpx 30rpx;

    .section-title {
      font-size: 26rpx;
      color: #999;
    }
  }

  .setting-list {
    background: #fff;

    .setting-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 30rpx;
      border-bottom: 1rpx solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      &.danger {
        .item-title {
          color: #ff4d4f;
        }
      }

      .item-info {
        flex: 1;

        .item-title {
          font-size: 30rpx;
          color: #333;
          display: block;
          margin-bottom: 8rpx;
        }

        .item-desc {
          font-size: 24rpx;
          color: #999;
          display: block;
        }
      }

      .item-value {
        font-size: 26rpx;
        color: #999;
      }
    }
  }
}

.version-info {
  padding: 60rpx;
  text-align: center;

  .version-text {
    font-size: 24rpx;
    color: #ccc;
  }
}
</style>