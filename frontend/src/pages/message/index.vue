<template>
  <view class="message-center">
    <!-- Tab切换 -->
    <view class="tab-bar">
      <view class="tab-item" 
            v-for="(tab, index) in tabs" 
            :key="index"
            :class="{ active: currentTab === tab.key }"
            @click="switchTab(tab.key)">
        <text class="tab-name">{{ tab.name }}</text>
        <text class="tab-badge" v-if="tab.count > 0">{{ tab.count }}</text>
      </view>
    </view>

    <!-- 消息列表 -->
    <scroll-view class="message-list" scroll-y>
      <!-- 系统通知 -->
      <view class="message-group" v-if="currentTab === 'all' || currentTab === 'system'">
        <view class="group-header" v-if="currentTab === 'all'">系统通知</view>
        <view class="message-item" v-for="(msg, index) in systemMessages" :key="index" @click="viewDetail(msg)">
          <view class="message-icon" :class="'icon-' + msg.type">
            <image src="/static/icon-system.png" mode="aspectFit" v-if="msg.type === 'system'" />
            <image src="/static/icon-activity.png" mode="aspectFit" v-else-if="msg.type === 'activity'" />
            <image src="/static/icon-order.png" mode="aspectFit" v-else-if="msg.type === 'order'" />
            <image src="/static/icon-auction.png" mode="aspectFit" v-else-if="msg.type === 'auction'" />
          </view>
          <view class="message-content">
            <view class="message-header">
              <text class="message-title">{{ msg.title }}</text>
              <text class="message-time">{{ msg.time }}</text>
            </view>
            <text class="message-desc">{{ msg.content }}</text>
          </view>
          <view class="unread-dot" v-if="msg.unread"></view>
        </view>
      </view>

      <!-- 互动消息 -->
      <view class="message-group" v-if="currentTab === 'all' || currentTab === '互动'">
        <view class="group-header" v-if="currentTab === 'all'">互动消息</view>
        <view class="message-item" v-for="(msg, index) in interactMessages" :key="index" @click="viewDetail(msg)">
          <image class="message-avatar" :src="msg.avatar" mode="aspectFill" />
          <view class="message-content">
            <view class="message-header">
              <text class="message-title">{{ msg.nickname }}</text>
              <text class="message-time">{{ msg.time }}</text>
            </view>
            <text class="message-desc">{{ msg.action }}了你的{{ msg.target }}</text>
          </view>
          <view class="unread-dot" v-if="msg.unread"></view>
        </view>
      </view>

      <!-- 交易消息 -->
      <view class="message-group" v-if="currentTab === 'all' || currentTab === 'transaction'">
        <view class="group-header" v-if="currentTab === 'all'">交易消息</view>
        <view class="message-item" v-for="(msg, index) in transactionMessages" :key="index" @click="viewDetail(msg)">
          <view class="message-icon order">
            <text class="icon-text">订单</text>
          </view>
          <view class="message-content">
            <view class="message-header">
              <text class="message-title">{{ msg.title }}</text>
              <text class="message-time">{{ msg.time }}</text>
            </view>
            <text class="message-desc">{{ msg.content }}</text>
          </view>
          <view class="unread-dot" v-if="msg.unread"></view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="isEmpty">
        <image class="empty-icon" src="/static/empty-message.png" mode="aspectFit" />
        <text class="empty-text">暂无消息</text>
      </view>
    </scroll-view>

    <!-- 底部操作 -->
    <view class="bottom-actions">
      <view class="action-item" @click="markAllRead">
        <text class="action-text">全部已读</text>
      </view>
      <view class="action-item" @click="openSettings">
        <text class="action-text">消息设置</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentTab: 'all',
      tabs: [
        { key: 'all', name: '全部', count: 3 },
        { key: 'system', name: '系统', count: 1 },
        { key: '互动', name: '互动', count: 1 },
        { key: 'transaction', name: '交易', count: 1 }
      ],
      systemMessages: [
        {
          type: 'system',
          title: '系统升级通知',
          content: '平台将于今晚22:00-24:00进行系统升级，届时部分功能暂停使用。',
          time: '10:30',
          unread: true
        },
        {
          type: 'activity',
          title: '活动提醒',
          content: '您关注的"张大千专场拍卖"即将开始，请准时参与！',
          time: '昨天',
          unread: true
        },
        {
          type: 'order',
          title: '订单更新',
          content: '您的订单已发货，快递单号：SF1234567890',
          time: '3天前',
          unread: false
        }
      ],
      interactMessages: [
        {
          avatar: '/static/avatar-default.png',
          nickname: '李先生',
          action: '点赞',
          target: '帖子',
          time: '5分钟前',
          unread: true
        }
      ],
      transactionMessages: [
        {
          title: '拍卖成交提醒',
          content: '恭喜您竞拍成功，请尽快完成付款',
          time: '1小时前',
          unread: true
        }
      ]
    }
  },
  computed: {
    isEmpty() {
      if (this.currentTab === 'all') {
        return this.systemMessages.length === 0 && this.interactMessages.length === 0
      } else if (this.currentTab === 'system') {
        return this.systemMessages.length === 0
      } else if (this.currentTab === '互动') {
        return this.interactMessages.length === 0
      } else {
        return this.transactionMessages.length === 0
      }
    }
  },
  methods: {
    switchTab(key) {
      this.currentTab = key
    },
    viewDetail(msg) {
      msg.unread = false
      uni.navigateTo({ url: '/pages/message/detail' })
    },
    markAllRead() {
      this.systemMessages.forEach(m => m.unread = false)
      this.interactMessages.forEach(m => m.unread = false)
      this.transactionMessages.forEach(m => m.unread = false)
      uni.showToast({ title: '已全部标记为已读', icon: 'success' })
    },
    openSettings() {
      uni.navigateTo({ url: '/pages/setting/message-settings' })
    }
  }
}
</script>

<style lang="scss" scoped>
.message-center {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.tab-bar {
  background: #fff;
  display: flex;
  padding: 20rpx;
  border-bottom: 2rpx solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  position: relative;
}

.tab-name {
  font-size: 28rpx;
  color: #666;
}

.tab-item.active .tab-name {
  color: #667eea;
  font-weight: 600;
}

.tab-badge {
  position: absolute;
  top: -6rpx;
  right: 20rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  background: #e63946;
  color: #fff;
  font-size: 20rpx;
  border-radius: 16rpx;
  text-align: center;
  padding: 0 8rpx;
}

.message-list {
  flex: 1;
  padding: 24rpx;
}

.message-group {
  margin-bottom: 30rpx;
}

.group-header {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 16rpx;
  padding-left: 10rpx;
}

.message-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  display: flex;
  align-items: flex-start;
  position: relative;
}

.message-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
}

.message-icon.icon-system {
  background: #e8f5e9;
}

.message-icon.icon-activity {
  background: #fff3e0;
}

.message-icon.icon-order {
  background: #e3f2fd;
}

.message-icon.icon-auction {
  background: #fce4ec;
}

.message-icon image {
  width: 48rpx;
  height: 48rpx;
}

.message-icon.order {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.icon-text {
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
}

.message-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  margin-right: 20rpx;
}

.message-content {
  flex: 1;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.message-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.message-time {
  font-size: 22rpx;
  color: #999;
}

.message-desc {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.unread-dot {
  position: absolute;
  top: 24rpx;
  right: 24rpx;
  width: 16rpx;
  height: 16rpx;
  background: #e63946;
  border-radius: 50%;
}

.empty-state {
  text-align: center;
  padding: 100rpx 0;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  opacity: 0.5;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
  margin-top: 20rpx;
  display: block;
}

.bottom-actions {
  background: #fff;
  padding: 20rpx 30rpx;
  display: flex;
  justify-content: space-around;
  border-top: 2rpx solid #eee;
}

.action-item {
  flex: 1;
  text-align: center;
}

.action-text {
  font-size: 28rpx;
  color: #667eea;
}
</style>