<template>
  <view class="message-page">
    <!-- 消息分类标签 -->
    <view class="message-tabs">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'system' }"
        @click="switchTab('system')"
      >
        <text>系统消息</text>
        <view class="tab-badge" v-if="unreadCount.system > 0">{{ unreadCount.system > 99 ? '99+' : unreadCount.system }}</view>
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'order' }"
        @click="switchTab('order')"
      >
        <text>订单通知</text>
        <view class="tab-badge" v-if="unreadCount.order > 0">{{ unreadCount.order > 99 ? '99+' : unreadCount.order }}</view>
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'chat' }"
        @click="switchTab('chat')"
      >
        <text>私信</text>
        <view class="tab-badge" v-if="unreadCount.chat > 0">{{ unreadCount.chat > 99 ? '99+' : unreadCount.chat }}</view>
      </view>
    </view>

    <!-- 系统/订单消息列表 -->
    <scroll-view 
      class="message-list" 
      scroll-y 
      v-if="currentTab !== 'chat'"
      @scrolltolower="loadMore"
    >
      <view 
        class="message-item" 
        v-for="item in messageList" 
        :key="item.id"
        @click="goMessageDetail(item)"
      >
        <view class="message-icon" :class="item.type">
          
        </view>
        <view class="message-content">
          <view class="message-header">
            <text class="message-title">{{ item.title }}</text>
            <text class="message-time">{{ formatTime(item.createTime) }}</text>
          </view>
          <text class="message-desc">{{ item.content }}</text>
          <view class="message-tags" v-if="item.tags && item.tags.length">
            <view class="tag" v-for="(tag, index) in item.tags" :key="index">{{ tag }}</view>
          </view>
        </view>
        <view class="message-arrow">
          
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="messageList.length === 0 && !loading">
        <image src="/static/empty/message.png" mode="aspectFit" class="empty-icon"></image>
        <text class="empty-text">暂无消息</text>
      </view>

      <!-- 加载更多 -->
      <view class="load-more" v-if="loading">
        <text class="loading-text">加载中...</text>
      </view>
    </scroll-view>

    <!-- 私信列表 -->
    <scroll-view 
      class="message-list chat-list" 
      scroll-y 
      v-if="currentTab === 'chat'"
      @scrolltolower="loadMoreChat"
    >
      <view 
        class="chat-item" 
        v-for="item in chatList" 
        :key="item.id"
        @click="goChat(item)"
      >
        <view class="chat-avatar">
          <image :src="item.avatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
          <view class="online-dot" v-if="item.online"></view>
        </view>
        <view class="chat-content">
          <view class="chat-header">
            <text class="chat-name">{{ item.name }}</text>
            <text class="chat-time">{{ formatTime(item.lastTime) }}</text>
          </view>
          <text class="chat-preview">{{ item.lastMessage }}</text>
        </view>
        <view class="chat-badge" v-if="item.unread > 0">
          <text>{{ item.unread > 99 ? '99+' : item.unread }}</text>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="chatList.length === 0 && !loading">
        <image src="/static/empty/chat.png" mode="aspectFit" class="empty-icon"></image>
        <text class="empty-text">暂无私信</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const currentTab = ref('system')
const loading = ref(false)
const page = ref(1)
const hasMore = ref(true)

const unreadCount = ref({
  system: 0,
  order: 3,
  chat: 2
})

const messageList = ref([
  {
    id: 1,
    type: 'order',
    title: '订单已发货',
    content: '您的订单 #ORDER20240101001 已发货，快递单号：SF1234567890',
    createTime: Date.now() - 3600000,
    tags: ['订单'],
    link: '/pages/order/detail?id=1'
  },
  {
    id: 2,
    type: 'promotion',
    title: '恭喜获得优惠券',
    content: '您已获得一张满500减50的优惠券，有效期至2024-01-31',
    createTime: Date.now() - 86400000,
    tags: ['优惠券']
  },
  {
    id: 3,
    type: 'auction',
    title: '拍卖提醒',
    content: '您关注的「江南春晓」拍卖即将开始，1月15日 20:00 开拍',
    createTime: Date.now() - 172800000,
    tags: ['拍卖']
  },
  {
    id: 4,
    type: 'system',
    title: '账户安全提醒',
    content: '您的账户在新设备登录，如非本人操作请及时修改密码',
    createTime: Date.now() - 259200000,
    tags: ['安全']
  }
])

const chatList = ref([
  {
    id: 1,
    name: '李明（艺术家）',
    avatar: 'https://pic.imgdb.cn/item/1.jpg',
    lastMessage: '好的，我这边的作品已经准备好了...',
    lastTime: Date.now() - 600000,
    unread: 2,
    online: true,
    userId: 1001
  },
  {
    id: 2,
    name: '张伟（收藏家）',
    avatar: 'https://pic.imgdb.cn/item/2.jpg',
    lastMessage: '这幅画很有意思，想了解更多...',
    lastTime: Date.now() - 3600000,
    unread: 0,
    online: false,
    userId: 1002
  },
  {
    id: 3,
    name: '王芳',
    avatar: 'https://pic.imgdb.cn/item/3.jpg',
    lastMessage: '请问这幅作品还在吗？',
    lastTime: Date.now() - 86400000,
    unread: 1,
    online: true,
    userId: 1003
  }
])

const getIconName = (type) => {
  const icons = {
    order: 'file-text',
    promotion: 'gift',
    auction: 'hammer',
    system: 'info-circle'
  }
  return icons[type] || 'bell'
}

const formatTime = (timestamp) => {
  const now = Date.now()
  const diff = now - timestamp
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 7 * day) return Math.floor(diff / day) + '天前'
  
  const date = new Date(timestamp)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const switchTab = (tab) => {
  currentTab.value = tab
  page.value = 1
  hasMore.value = true
}

const loadMore = () => {
  if (!hasMore.value || loading.value) return
  loading.value = true
  setTimeout(() => {
    loading.value = false
    // hasMore.value = false // 模拟没有更多数据
  }, 1000)
}

const loadMoreChat = () => {
  if (!hasMore.value || loading.value) return
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 1000)
}

const goMessageDetail = (item) => {
  if (item.link) {
    uni.navigateTo({ url: item.link })
  }
}

const goChat = (item) => {
  uni.navigateTo({ url: `/pages/message/chat?userId=${item.userId}&name=${item.name}` })
}

onMounted(() => {
  // 获取消息列表
  // getMessageList()
})
</script>

<style lang="scss" scoped>
.message-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6f8;
}

.message-tabs {
  display: flex;
  padding: 0 20rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;

  .tab-item {
    flex: 1;
    height: 96rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    font-size: 28rpx;
    color: #666;
    position: relative;

    &.active {
      color: #667eea;
      font-weight: 600;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 4rpx;
        background: #667eea;
        border-radius: 2rpx;
      }
    }

    .tab-badge {
      min-width: 32rpx;
      height: 32rpx;
      padding: 0 8rpx;
      background: #ff4d4f;
      color: #fff;
      font-size: 20rpx;
      border-radius: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
}

.message-list {
  flex: 1;
  padding: 20rpx;

  &.chat-list {
    padding: 0;
  }
}

.message-item {
  display: flex;
  align-items: flex-start;
  padding: 30rpx;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 16rpx;

  .message-icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: 20rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;

    &.order {
      background: linear-gradient(135deg, #667eea, #764ba2);
    }

    &.promotion {
      background: linear-gradient(135deg, #f093fb, #f5576c);
    }

    &.auction {
      background: linear-gradient(135deg, #4facfe, #00f2fe);
    }

    &.system {
      background: linear-gradient(135deg, #43e97b, #38f9d7);
    }
  }

  .message-content {
    flex: 1;

    .message-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12rpx;

      .message-title {
        font-size: 28rpx;
        font-weight: 600;
        color: #333;
      }

      .message-time {
        font-size: 22rpx;
        color: #999;
      }
    }

    .message-desc {
      font-size: 26rpx;
      color: #666;
      line-height: 1.5;
      display: block;
    }

    .message-tags {
      display: flex;
      gap: 12rpx;
      margin-top: 12rpx;

      .tag {
        padding: 6rpx 16rpx;
        background: #f0f2f5;
        color: #666;
        font-size: 22rpx;
        border-radius: 6rpx;
      }
    }
  }

  .message-arrow {
    margin-left: 16rpx;
    align-self: center;
  }
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 30rpx 20rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;

  .chat-avatar {
    position: relative;
    margin-right: 20rpx;

    image {
      width: 100rpx;
      height: 100rpx;
      border-radius: 50%;
    }

    .online-dot {
      position: absolute;
      bottom: 4rpx;
      right: 4rpx;
      width: 20rpx;
      height: 20rpx;
      background: #4caf50;
      border: 4rpx solid #fff;
      border-radius: 50%;
    }
  }

  .chat-content {
    flex: 1;
    overflow: hidden;

    .chat-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12rpx;

      .chat-name {
        font-size: 30rpx;
        font-weight: 600;
        color: #333;
      }

      .chat-time {
        font-size: 22rpx;
        color: #999;
      }
    }

    .chat-preview {
      font-size: 26rpx;
      color: #999;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      display: block;
    }
  }

  .chat-badge {
    min-width: 36rpx;
    height: 36rpx;
    padding: 0 10rpx;
    background: #ff4d4f;
    color: #fff;
    font-size: 22rpx;
    border-radius: 18rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-left: 16rpx;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;

  .empty-icon {
    width: 200rpx;
    height: 200rpx;
    opacity: 0.5;
  }

  .empty-text {
    margin-top: 30rpx;
    font-size: 28rpx;
    color: #999;
  }
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 30rpx;
}
</style>