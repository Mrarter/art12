<template>
  <view class="message-page">
    <!-- 顶部标签 -->
    <view class="tab-header">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'all' }"
        @click="switchTab('all')"
      >
        全部
        <view class="tab-badge" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</view>
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'system' }"
        @click="switchTab('system')"
      >
        系统
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'order' }"
        @click="switchTab('order')"
      >
        订单
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'auction' }"
        @click="switchTab('auction')"
      >
        拍卖
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'social' }"
        @click="switchTab('social')"
      >
        社交
      </view>
    </view>

    <!-- 消息列表 -->
    <scroll-view 
      class="message-list" 
      scroll-y 
      @scrolltolower="loadMore"
    >
      <view class="message-group" v-for="(group, date) in groupedMessages" :key="date">
        <view class="date-header">{{ date }}</view>
        <view 
          class="message-item" 
          v-for="msg in group" 
          :key="msg.id"
          :class="{ unread: !msg.isRead }"
          @click="goDetail(msg)"
        >
          <view class="msg-icon-wrap">
            <image class="msg-icon" :src="getMsgIcon(msg.type)" mode="aspectFit"></image>
            <view class="unread-dot" v-if="!msg.isRead"></view>
          </view>
          <view class="msg-content">
            <view class="msg-header">
              <text class="msg-title">{{ msg.title }}</text>
              <text class="msg-time">{{ formatTime(msg.createTime) }}</text>
            </view>
            <text class="msg-desc">{{ msg.content }}</text>
            <view class="msg-tags" v-if="msg.tags && msg.tags.length > 0">
              <view class="tag-item" v-for="(tag, index) in msg.tags" :key="index">{{ tag }}</view>
            </view>
          </view>
        </view>
      </view>

      <!-- 加载状态 -->
      <view class="loading-more" v-if="loading">
        <text>加载中...</text>
      </view>
      <view class="no-more" v-if="!hasMore && messages.length > 0">
        <text>没有更多了</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="messages.length === 0 && !loading">
        <image class="empty-icon" src="/static/icons/empty-message.png" mode="aspectFit"></image>
        <text class="empty-text">暂无消息</text>
        <text class="empty-hint">快去发现有趣的内容吧</text>
      </view>
    </scroll-view>

    <!-- 底部操作 -->
    <view class="bottom-actions">
      <view class="action-btn" @click="markAllRead">
        
        <text>全部已读</text>
      </view>
      <view class="action-btn" @click="showSettings">
        
        <text>消息设置</text>
      </view>
    </view>

    <!-- 消息详情弹窗 -->
    <!-- 弹窗开始 -->
      <view class="detail-modal" v-if="currentMsg">
        <view class="detail-header">
          <view class="detail-icon">
            <image :src="getMsgIcon(currentMsg.type)" mode="aspectFit"></image>
          </view>
          <view class="detail-title">{{ currentMsg.title }}</view>
          <view class="detail-time">{{ currentMsg.createTime }}</view>
        </view>
        <view class="detail-body">
          <text class="detail-content">{{ currentMsg.content }}</text>
          <view class="detail-images" v-if="currentMsg.images && currentMsg.images.length > 0">
            <image 
              v-for="(img, index) in currentMsg.images" 
              :key="index"
              :src="img" 
              mode="aspectFill"
              @click="previewImage(currentMsg.images, index)"
            ></image>
          </view>
          <view class="detail-action" v-if="currentMsg.actionText" @click="handleAction(currentMsg)">
            <text>{{ currentMsg.actionText }}</text>
            
          </view>
        </view>
      </view>
<!-- 弹窗结束 -->

    <!-- 消息设置弹窗 -->
    <!-- 弹窗开始 -->
      <view class="settings-modal">
        <view class="modal-title">消息通知设置</view>
        
        <view class="settings-list">
          <view class="settings-item">
            <view class="settings-info">
              <text class="settings-label">系统消息</text>
              <text class="settings-desc">平台公告、系统通知等</text>
            </view>
            <switch :checked="settings.system" @change="(e) => settings.system = e.detail.value" color="#667eea" />
          </view>
          <view class="settings-item">
            <view class="settings-info">
              <text class="settings-label">订单消息</text>
              <text class="settings-desc">订单状态变更、物流通知等</text>
            </view>
            <switch :checked="settings.order" @change="(e) => settings.order = e.detail.value" color="#667eea" />
          </view>
          <view class="settings-item">
            <view class="settings-info">
              <text class="settings-label">拍卖消息</text>
              <text class="settings-desc">竞拍提醒、出价通知等</text>
            </view>
            <switch :checked="settings.auction" @change="(e) => settings.auction = e.detail.value" color="#667eea" />
          </view>
          <view class="settings-item">
            <view class="settings-info">
              <text class="settings-label">社交消息</text>
              <text class="settings-desc">关注、点赞、评论等</text>
            </view>
            <switch :checked="settings.social" @change="(e) => settings.social = e.detail.value" color="#667eea" />
          </view>
        </view>

        <button class="save-btn" @click="saveSettings">保存设置</button>
      </view>
<!-- 弹窗结束 -->
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// 状态
const currentTab = ref('all')
const messages = ref([])
const loading = ref(false)
const hasMore = ref(true)
const page = ref(1)
const showDetail = ref(false)
const currentMsg = ref(null)
const showSettingsModal = ref(false)

// 消息设置
const settings = ref({
  system: true,
  order: true,
  auction: true,
  social: true
})

// 统计
const unreadCount = ref(5)

// 标签页
const tabs = [
  { value: 'all', label: '全部' },
  { value: 'system', label: '系统' },
  { value: 'order', label: '订单' },
  { value: 'auction', label: '拍卖' },
  { value: 'social', label: '社交' }
]

// 消息类型配置
const msgTypes = {
  system: { icon: '/static/icons/msg-system.png', color: '#667eea' },
  order: { icon: '/static/icons/msg-order.png', color: '#07c160' },
  auction: { icon: '/static/icons/msg-auction.png', color: '#f39c12' },
  social: { icon: '/static/icons/msg-social.png', color: '#e74c3c' }
}

// TODO: 对接真实消息列表 API
const mockMessages = []

// 按日期分组
const groupedMessages = computed(() => {
  const groups = {}
  const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
  const yesterday = new Date(Date.now() - 86400000).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
  
  filteredMessages.value.forEach(msg => {
    const msgDate = new Date(msg.createTime).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
    let displayDate = msgDate
    if (msgDate === today) displayDate = '今天'
    else if (msgDate === yesterday) displayDate = '昨天'
    
    if (!groups[displayDate]) {
      groups[displayDate] = []
    }
    groups[displayDate].push(msg)
  })
  
  return groups
})

// 筛选后的消息
const filteredMessages = computed(() => {
  if (currentTab.value === 'all') {
    return messages.value
  }
  return messages.value.filter(msg => msg.type === currentTab.value)
})

// 获取消息图标
const getMsgIcon = (type) => {
  return msgTypes[type]?.icon || '/static/icons/msg-default.png'
}

// 格式化时间
const formatTime = (time) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 切换标签
const switchTab = (tab) => {
  currentTab.value = tab
  page.value = 1
  hasMore.value = true
  loadData()
}

// 加载数据
const loadData = () => {
  if (loading.value) return
  loading.value = true
  
  setTimeout(() => {
    messages.value = mockMessages
    loading.value = false
    hasMore.value = false
  }, 500)
}

// 加载更多
const loadMore = () => {
  if (!hasMore.value || loading.value) return
  page.value++
  loadData()
}

// 查看详情
const goDetail = (msg) => {
  currentMsg.value = msg
  showDetail.value = true
  
  // 标记已读
  if (!msg.isRead) {
    msg.isRead = true
    updateUnreadCount()
  }
}

// 全部已读
const markAllRead = () => {
  messages.value.forEach(msg => {
    msg.isRead = true
  })
  updateUnreadCount()
  uni.showToast({ title: '已全部标记为已读', icon: 'success' })
}

// 更新未读数
const updateUnreadCount = () => {
  unreadCount.value = messages.value.filter(msg => !msg.isRead).length
}

// 显示设置
const showSettings = () => {
  showSettingsModal.value = true
}

// 保存设置
const saveSettings = () => {
  uni.showToast({ title: '设置已保存', icon: 'success' })
  showSettingsModal.value = false
}

// 处理操作
const handleAction = (msg) => {
  showDetail.value = false
  
  switch (msg.type) {
    case 'order':
      if (msg.actionText === '查看物流') {
        uni.navigateTo({ url: '/pages/order/logistics' })
      } else if (msg.actionText === '立即发货') {
        uni.navigateTo({ url: '/pages/common/coming-soon?title=发货处理&desc=发货处理页正在开发中，后续会补充物流录入与状态流转。' })
      } else if (msg.actionText === '去评价') {
        uni.navigateTo({ url: '/pages/order/review' })
      }
      break
    case 'auction':
      uni.navigateTo({ url: '/pages/auction/detail?id=' + msg.id })
      break
    default:
      break
  }
}

// 预览图片
const previewImage = (images, index) => {
  uni.previewImage({
    urls: images,
    current: index
  })
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.message-page {
  min-height: 100vh;
  background: #f5f6f8;
  display: flex;
  flex-direction: column;
}

.tab-header {
  display: flex;
  background: #fff;
  padding: 0 20rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);

  .tab-item {
    flex: 1;
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    color: #666;
    position: relative;

    &.active {
      color: #667eea;
      font-weight: 500;

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
      position: absolute;
      top: 12rpx;
      right: 20rpx;
      min-width: 32rpx;
      height: 32rpx;
      padding: 0 8rpx;
      background: #e74c3c;
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

  .message-group {
    margin-bottom: 20rpx;

    .date-header {
      font-size: 26rpx;
      color: #999;
      padding: 16rpx 0;
    }

    .message-item {
      display: flex;
      background: #fff;
      border-radius: 16rpx;
      padding: 24rpx;
      margin-bottom: 16rpx;
      transition: all 0.2s;

      &:active {
        transform: scale(0.98);
      }

      &.unread {
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
        border-left: 4rpx solid #667eea;
      }

      .msg-icon-wrap {
        position: relative;
        margin-right: 20rpx;

        .msg-icon {
          width: 80rpx;
          height: 80rpx;
          border-radius: 50%;
          background: #f5f6f8;
        }

        .unread-dot {
          position: absolute;
          top: 0;
          right: 0;
          width: 16rpx;
          height: 16rpx;
          background: #e74c3c;
          border-radius: 50%;
          border: 2rpx solid #fff;
        }
      }

      .msg-content {
        flex: 1;
        min-width: 0;

        .msg-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8rpx;

          .msg-title {
            font-size: 30rpx;
            font-weight: 500;
            color: #333;
            flex: 1;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .msg-time {
            font-size: 24rpx;
            color: #999;
            margin-left: 16rpx;
            flex-shrink: 0;
          }
        }

        .msg-desc {
          font-size: 26rpx;
          color: #666;
          line-height: 1.5;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .msg-tags {
          display: flex;
          gap: 12rpx;
          margin-top: 12rpx;

          .tag-item {
            padding: 4rpx 12rpx;
            background: rgba(102, 126, 234, 0.1);
            color: #667eea;
            font-size: 22rpx;
            border-radius: 6rpx;
          }
        }
      }
    }
  }

  .loading-more,
  .no-more {
    text-align: center;
    padding: 30rpx;
    font-size: 26rpx;
    color: #999;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;

  .empty-icon {
    width: 200rpx;
    height: 200rpx;
    opacity: 0.5;
  }

  .empty-text {
    font-size: 30rpx;
    color: #333;
    margin-top: 20rpx;
  }

  .empty-hint {
    font-size: 26rpx;
    color: #999;
    margin-top: 12rpx;
  }
}

.bottom-actions {
  display: flex;
  background: #fff;
  padding: 20rpx 40rpx;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);

  .action-btn {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    font-size: 28rpx;
    color: #666;

    &:active {
      opacity: 0.7;
    }
  }
}

// 详情弹窗
.detail-modal {
  width: 650rpx;
  max-height: 80vh;
  overflow-y: auto;

  .detail-header {
    text-align: center;
    padding: 40rpx 30rpx 30rpx;
    border-bottom: 1rpx solid #f0f0f0;

    .detail-icon {
      width: 100rpx;
      height: 100rpx;
      margin: 0 auto 20rpx;

      image {
        width: 100%;
        height: 100%;
      }
    }

    .detail-title {
      font-size: 34rpx;
      font-weight: 600;
      color: #333;
      margin-bottom: 12rpx;
    }

    .detail-time {
      font-size: 24rpx;
      color: #999;
    }
  }

  .detail-body {
    padding: 30rpx;

    .detail-content {
      font-size: 28rpx;
      color: #666;
      line-height: 1.8;
      display: block;
    }

    .detail-images {
      display: flex;
      flex-wrap: wrap;
      gap: 16rpx;
      margin-top: 20rpx;

      image {
        width: 180rpx;
        height: 180rpx;
        border-radius: 12rpx;
      }
    }

    .detail-action {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8rpx;
      margin-top: 30rpx;
      padding: 20rpx;
      background: rgba(102, 126, 234, 0.1);
      border-radius: 12rpx;
      color: #667eea;
      font-size: 28rpx;

      &:active {
        background: rgba(102, 126, 234, 0.2);
      }
    }
  }
}

// 设置弹窗
.settings-modal {
  padding: 30rpx;

  .modal-title {
    font-size: 34rpx;
    font-weight: 600;
    text-align: center;
    margin-bottom: 30rpx;
  }

  .settings-list {
    .settings-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24rpx 0;
      border-bottom: 1rpx solid #f0f0f0;

      .settings-info {
        .settings-label {
          font-size: 28rpx;
          color: #333;
          display: block;
          margin-bottom: 4rpx;
        }

        .settings-desc {
          font-size: 24rpx;
          color: #999;
        }
      }
    }
  }

  .save-btn {
    width: 100%;
    height: 88rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 30rpx;
    border-radius: 44rpx;
    margin-top: 30rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
  }
}
</style>
