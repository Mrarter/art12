<template>
  <view class="message-page">
    <view class="page-glow"></view>

    <view class="page-header">
      <view>
        <text class="page-title">消息中心</text>
        <text class="page-desc">订单、系统通知和私信集中查看</text>
      </view>
      <view class="header-action" @click="goMessageSettings">
        <image class="header-icon" src="/static/icons/gear.svg" mode="aspectFit"></image>
      </view>
    </view>

    <view class="message-tabs">
      <view
        class="tab-item"
        v-for="item in tabs"
        :key="item.value"
        :class="{ active: currentTab === item.value }"
        @click="switchTab(item.value)"
      >
        <text>{{ item.label }}</text>
        <view class="tab-badge" v-if="unreadCount[item.value] > 0">{{ formatBadge(unreadCount[item.value]) }}</view>
      </view>
    </view>

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
        <view class="message-icon" :class="item.type">{{ getIconName(item.type) }}</view>
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
        <text class="message-arrow">›</text>
      </view>

      <view class="empty-state" v-if="messageList.length === 0 && !loading">
        <view class="empty-icon">息</view>
        <text class="empty-text">暂无消息</text>
      </view>

      <view class="load-more" v-if="loading">
        <text class="loading-text">加载中...</text>
      </view>
    </scroll-view>

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
          <image :src="item.avatar || '/static/images/avatar.png'" mode="aspectFill"></image>
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

      <view class="empty-state" v-if="chatList.length === 0 && !loading">
        <view class="empty-icon">私</view>
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

const tabs = [
  { value: 'system', label: '系统' },
  { value: 'order', label: '订单' },
  { value: 'chat', label: '私信' }
]

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
    order: '单',
    promotion: '券',
    auction: '拍',
    system: '系'
  }
  return icons[type] || '息'
}

const formatBadge = (count) => {
  return count > 99 ? '99+' : count
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

const goMessageSettings = () => {
  uni.navigateTo({ url: '/pages/setting/message-settings' })
}

onMounted(() => {
  // 获取消息列表
  // getMessageList()
})
</script>

<style lang="scss" scoped>
.message-page {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0b0b0c;
  color: #f6f2e8;
  box-sizing: border-box;
  overflow: hidden;
}

.page-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 320rpx;
  background: linear-gradient(180deg, rgba(201, 162, 39, 0.18), transparent);
  pointer-events: none;
}

.page-header {
  position: relative;
  z-index: 1;
  padding: 28rpx 24rpx 18rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title,
.page-desc {
  display: block;
}

.page-title {
  font-size: 38rpx;
  line-height: 48rpx;
  font-weight: 800;
}

.page-desc {
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #9b958a;
}

.header-action {
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon {
  width: 34rpx;
  height: 34rpx;
}

.message-tabs {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 12rpx;
  padding: 0 24rpx 20rpx;

  .tab-item {
    flex: 1;
    height: 66rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    border-radius: 12rpx;
    font-size: 25rpx;
    color: #9b958a;
    background: #202024;
    position: relative;

    &.active {
      color: #16130b;
      background: #c9a227;
      font-weight: 700;
    }

    .tab-badge {
      min-width: 32rpx;
      height: 32rpx;
      padding: 0 8rpx;
      background: #c96262;
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
  position: relative;
  z-index: 1;
  flex: 1;
  padding: 0 24rpx 24rpx;
  box-sizing: border-box;

  &.chat-list {
    padding: 0 24rpx 24rpx;
  }
}

.message-item {
  display: flex;
  align-items: flex-start;
  padding: 24rpx;
  background: #171719;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
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
    color: #f6f2e8;
    font-size: 28rpx;
    font-weight: 800;

    &.order {
      background: rgba(95, 143, 199, 0.22);
      color: #5f8fc7;
    }

    &.promotion {
      background: rgba(201, 162, 39, 0.18);
      color: #c9a227;
    }

    &.auction {
      background: rgba(201, 98, 98, 0.2);
      color: #c96262;
    }

    &.system {
      background: rgba(88, 185, 130, 0.18);
      color: #58b982;
    }
  }

  .message-content {
    flex: 1;
    min-width: 0;

    .message-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12rpx;

      .message-title {
        font-size: 28rpx;
        font-weight: 600;
        color: #f6f2e8;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .message-time {
        font-size: 22rpx;
        color: #68645c;
        flex-shrink: 0;
        margin-left: 16rpx;
      }
    }

    .message-desc {
      font-size: 26rpx;
      color: #9b958a;
      line-height: 1.5;
      display: block;
    }

    .message-tags {
      display: flex;
      gap: 12rpx;
      margin-top: 12rpx;

      .tag {
        padding: 6rpx 16rpx;
        background: rgba(201, 162, 39, 0.13);
        color: #c9a227;
        font-size: 22rpx;
        border-radius: 6rpx;
      }
    }
  }

  .message-arrow {
    margin-left: 16rpx;
    align-self: center;
    color: #68645c;
    font-size: 34rpx;
  }
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 24rpx;
  margin-bottom: 16rpx;
  background: #171719;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 16rpx;

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
      background: #58b982;
      border: 4rpx solid #171719;
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
        color: #f6f2e8;
      }

      .chat-time {
        font-size: 22rpx;
        color: #68645c;
      }
    }

    .chat-preview {
      font-size: 26rpx;
      color: #9b958a;
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
    background: #c96262;
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
    width: 112rpx;
    height: 112rpx;
    border-radius: 28rpx;
    background: rgba(201, 162, 39, 0.14);
    color: #c9a227;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 42rpx;
    font-weight: 800;
  }

  .empty-text {
    margin-top: 30rpx;
    font-size: 28rpx;
    color: #9b958a;
  }
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 30rpx;
  color: #9b958a;
}
</style>
