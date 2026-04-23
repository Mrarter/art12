<template>
  <view class="chat-page">
    <!-- 聊天头部 -->
    <view class="chat-header">
      <view class="header-left" @click="goBack">
        
      </view>
      <view class="header-center">
        <view class="user-info">
          <text class="user-name">{{ chatUser.name }}</text>
          <view class="online-status" v-if="chatUser.online">
            <view class="status-dot"></view>
            <text>在线</text>
          </view>
        </view>
      </view>
      <view class="header-right">
        
      </view>
    </view>

    <!-- 消息列表 -->
    <scroll-view 
      class="message-container" 
      scroll-y 
      :scroll-top="scrollTop"
      :scroll-into-view="scrollIntoView"
      @scrolltoupper="loadMoreMessages"
    >
      <!-- 加载更多提示 -->
      <view class="load-more-tip" v-if="loadingMore">
        <text>加载更多...</text>
      </view>

      <!-- 消息列表 -->
      <view class="message-list" id="messageList">
        <view 
          v-for="(msg, index) in messages" 
          :key="msg.id"
          :class="['message-item', msg.isMine ? 'mine' : 'other']"
          :id="'msg-' + msg.id"
        >
          <!-- 时间分割线 -->
          <view class="time-divider" v-if="showTimeDivider(index)">
            <text>{{ formatDate(msg.createTime) }}</text>
          </view>

          <!-- 消息内容 -->
          <view class="message-content" v-if="!msg.isSystem">
            <!-- 对方消息 -->
            <view class="avatar" v-if="!msg.isMine">
              <image :src="chatUser.avatar" mode="aspectFill"></image>
            </view>

            <view class="bubble-wrapper">
              <!-- 文本消息 -->
              <view class="bubble" v-if="msg.type === 'text'">
                <text>{{ msg.content }}</text>
              </view>

              <!-- 图片消息 -->
              <view class="bubble image-bubble" v-else-if="msg.type === 'image'">
                <image :src="msg.content" mode="widthFix" @click="previewImage(msg.content)"></image>
              </view>

              <!-- 作品消息 -->
              <view class="bubble work-bubble" v-else-if="msg.type === 'work'" @click="goWorkDetail(msg.workId)">
                <view class="work-card">
                  <image :src="msg.workCover" mode="aspectFill"></image>
                  <view class="work-info">
                    <text class="work-title">{{ msg.workTitle }}</text>
                    <text class="work-price">¥{{ msg.workPrice }}</text>
                  </view>
                </view>
              </view>

              <!-- 订单消息 -->
              <view class="bubble order-bubble" v-else-if="msg.type === 'order'">
                <view class="order-card">
                  
                  <view class="order-info">
                    <text class="order-title">订单 {{ msg.orderNo }}</text>
                    <text class="order-status">{{ msg.orderStatus }}</text>
                  </view>
                </view>
              </view>
            </view>

            <!-- 我的消息 -->
            <view class="avatar" v-if="msg.isMine">
              <image :src="myAvatar" mode="aspectFill"></image>
            </view>

            <!-- 发送状态 -->
            <view class="send-status" v-if="msg.isMine">
              
              
            </view>
          </view>

          <!-- 系统消息 -->
          <view class="system-message" v-else>
            <text>{{ msg.content }}</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 输入区域 -->
    <view class="input-container">
      <!-- 快捷操作 -->
      <view class="quick-actions">
        <view class="action-btn" @click="toggleEmoji">
          
        </view>
        <view class="action-btn" @click="chooseImage">
          <text>🖼</text>
        </view>
        <view class="action-btn" @click="sendWork">
          
        </view>
      </view>

      <!-- 表情选择器 -->
      <view class="emoji-picker" v-if="showEmoji">
        <scroll-view scroll-x class="emoji-scroll">
          <view class="emoji-list">
            <text 
              v-for="emoji in emojiList" 
              :key="emoji"
              class="emoji-item"
              @click="selectEmoji(emoji)"
            >{{ emoji }}</text>
          </view>
        </scroll-view>
      </view>

      <!-- 输入框 -->
      <view class="input-wrapper">
        <input 
          class="input-field" 
          v-model="inputText" 
          :placeholder="placeholder"
          :focus="inputFocus"
          confirm-type="send"
          @confirm="sendTextMessage"
          @focus="onInputFocus"
        />
        <button 
          class="send-btn" 
          :class="{ active: canSend }"
          @click="sendTextMessage"
        >发送</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'

const chatUser = ref({
  id: 0,
  name: '聊天中...',
  avatar: '/static/avatar/default.jpg',
  online: false
})

const messages = ref([
  {
    id: 1,
    type: 'text',
    content: '您好，请问这幅作品还在售吗？',
    isMine: false,
    createTime: Date.now() - 3600000 * 2,
    status: 'sent'
  },
  {
    id: 2,
    type: 'text',
    content: '在的，这幅《山水意境》是我的原创作品',
    isMine: true,
    createTime: Date.now() - 3600000 * 2 + 60000,
    status: 'sent'
  },
  {
    id: 3,
    type: 'work',
    workId: 1,
    workCover: 'https://pic.imgdb.cn/item/1.jpg',
    workTitle: '山水意境',
    workPrice: '8888',
    isMine: false,
    createTime: Date.now() - 3600000 * 2 + 120000,
    status: 'sent'
  },
  {
    id: 4,
    type: 'text',
    content: '好的，我很喜欢这种风格，可以便宜点吗？',
    isMine: false,
    createTime: Date.now() - 3600000,
    status: 'sent'
  },
  {
    id: 5,
    type: 'order',
    orderNo: 'ORDER20240101001',
    orderStatus: '已发货',
    isMine: true,
    createTime: Date.now() - 1800000,
    status: 'sent'
  }
])

const inputText = ref('')
const showEmoji = ref(false)
const inputFocus = ref(false)
const scrollTop = ref(0)
const scrollIntoView = ref('')
const loadingMore = ref(false)
const canSend = computed(() => inputText.value.trim().length > 0)

const myAvatar = '/static/avatar/default.jpg'

const placeholder = '输入消息...'

const emojiList = [
  '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂',
  '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '🤩',
  '😘', '😗', '😚', '😋', '😛', '😜', '🤪', '😝',
  '🤗', '🤭', '🤫', '🤔', '🤐', '🤨', '😐', '😑',
  '😶', '😏', '😒', '🙄', '😬', '😮‍💨', '🤥', '😌',
  '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🤢',
  '👍', '👎', '👏', '🙌', '🤝', '🙏', '💪', '🤘'
]

const goBack = () => {
  uni.navigateBack()
}

const showUserMenu = () => {
  uni.showActionSheet({
    itemList: ['查看主页', '清空聊天记录', '举报用户'],
    success: (res) => {
      console.log('选择了', res.tapIndex)
    }
  })
}

const showTimeDivider = (index) => {
  if (index === 0) return true
  const current = messages.value[index].createTime
  const prev = messages.value[index - 1].createTime
  return current - prev > 300000 // 5分钟内不显示
}

const formatDate = (timestamp) => {
  const now = new Date()
  const date = new Date(timestamp)
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = today - 86400000

  if (date >= today) {
    return '今天 ' + formatTime(timestamp)
  } else if (date >= yesterday) {
    return '昨天 ' + formatTime(timestamp)
  } else {
    return `${date.getMonth() + 1}月${date.getDate()}日 ${formatTime(timestamp)}`
  }
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

const toggleEmoji = () => {
  showEmoji.value = !showEmoji.value
  if (showEmoji.value) {
    inputFocus.value = false
  }
}

const selectEmoji = (emoji) => {
  inputText.value += emoji
}

const onInputFocus = () => {
  showEmoji.value = false
}

const sendTextMessage = () => {
  if (!canSend.value) return
  
  const content = inputText.value.trim()
  messages.value.push({
    id: Date.now(),
    type: 'text',
    content: content,
    isMine: true,
    createTime: Date.now(),
    status: 'sending'
  })
  
  inputText.value = ''
  showEmoji.value = false
  scrollToBottom()
  
  // 模拟发送成功
  setTimeout(() => {
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg.status === 'sending') {
      lastMsg.status = 'sent'
    }
  }, 500)
}

const chooseImage = () => {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      messages.value.push({
        id: Date.now(),
        type: 'image',
        content: res.tempFilePaths[0],
        isMine: true,
        createTime: Date.now(),
        status: 'sending'
      })
      scrollToBottom()
    }
  })
}

const sendWork = () => {
  // 跳转到选择作品页面
  uni.showToast({ title: '选择作品发送', icon: 'none' })
}

const previewImage = (url) => {
  uni.previewImage({ urls: [url] })
}

const goWorkDetail = (workId) => {
  uni.navigateTo({ url: `/pages/gallery/detail?id=${workId}` })
}

const loadMoreMessages = () => {
  if (loadingMore.value) return
  loadingMore.value = true
  
  setTimeout(() => {
    loadingMore.value = false
  }, 1000)
}

const scrollToBottom = () => {
  nextTick(() => {
    scrollIntoView.value = ''
    setTimeout(() => {
      scrollIntoView.value = `msg-${messages.value[messages.value.length - 1]?.id}`
    }, 100)
  })
}

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}
  
  if (options.userId) {
    chatUser.value.id = options.userId
    chatUser.value.name = options.name || '用户'
  }
  
  scrollToBottom()
})
</script>

<style lang="scss" scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
}

.chat-header {
  display: flex;
  align-items: center;
  height: 100rpx;
  padding: 0 20rpx;
  background: #fff;
  box-shadow: 0 2rpx 10rpx rgba(0,0,0,0.05);

  .header-left, .header-right {
    width: 80rpx;
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .header-center {
    flex: 1;
    text-align: center;

    .user-info {
      display: flex;
      flex-direction: column;
      align-items: center;

      .user-name {
        font-size: 32rpx;
        font-weight: 600;
        color: #333;
      }

      .online-status {
        display: flex;
        align-items: center;
        gap: 6rpx;
        margin-top: 4rpx;
        font-size: 22rpx;
        color: #4caf50;

        .status-dot {
          width: 12rpx;
          height: 12rpx;
          background: #4caf50;
          border-radius: 50%;
        }
      }
    }
  }
}

.message-container {
  flex: 1;
  padding: 20rpx;
}

.load-more-tip {
  text-align: center;
  padding: 20rpx;
  font-size: 24rpx;
  color: #999;
}

.message-list {
  padding-bottom: 20rpx;
}

.message-item {
  margin-bottom: 30rpx;

  &.mine {
    .message-content {
      flex-direction: row-reverse;

      .avatar {
        margin-left: 16rpx;
        margin-right: 0;
      }

      .bubble-wrapper {
        .bubble {
          background: #667eea;
          color: #fff;
          border-radius: 24rpx 24rpx 4rpx 24rpx;
        }
      }
    }
  }

  &.other {
    .message-content {
      flex-direction: row;

      .avatar {
        margin-right: 16rpx;
      }

      .bubble-wrapper {
        .bubble {
          background: #fff;
          color: #333;
          border-radius: 24rpx 24rpx 24rpx 4rpx;
        }
      }
    }
  }
}

.time-divider {
  text-align: center;
  margin-bottom: 30rpx;

  text {
    display: inline-block;
    padding: 8rpx 24rpx;
    background: rgba(0,0,0,0.1);
    color: #999;
    font-size: 22rpx;
    border-radius: 20rpx;
  }
}

.message-content {
  display: flex;
  align-items: flex-start;

  .avatar {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    overflow: hidden;
    flex-shrink: 0;

    image {
      width: 100%;
      height: 100%;
    }
  }

  .bubble-wrapper {
    max-width: 70%;
  }

  .bubble {
    padding: 20rpx 28rpx;
    font-size: 28rpx;
    line-height: 1.5;
    word-break: break-word;

    &.image-bubble {
      padding: 8rpx;
      background: transparent;

      image {
        max-width: 300rpx;
        border-radius: 16rpx;
      }
    }

    &.work-bubble, &.order-bubble {
      padding: 0;
      background: transparent;
    }
  }

  .send-status {
    margin-left: 12rpx;
    align-self: flex-end;
  }
}

.work-card {
  width: 400rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;

  image {
    width: 100%;
    height: 280rpx;
  }

  .work-info {
    padding: 20rpx;

    .work-title {
      font-size: 28rpx;
      color: #333;
      display: block;
      margin-bottom: 8rpx;
    }

    .work-price {
      font-size: 32rpx;
      color: #ff4d4f;
      font-weight: 600;
    }
  }
}

.order-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 16rpx;
  width: 400rpx;

  .order-info {
    flex: 1;

    .order-title {
      font-size: 28rpx;
      color: #333;
      display: block;
      margin-bottom: 8rpx;
    }

    .order-status {
      font-size: 24rpx;
      color: #667eea;
    }
  }
}

.system-message {
  text-align: center;
  margin-bottom: 20rpx;

  text {
    display: inline-block;
    padding: 10rpx 24rpx;
    background: rgba(0,0,0,0.05);
    color: #999;
    font-size: 22rpx;
    border-radius: 20rpx;
  }
}

.input-container {
  background: #fff;
  padding: 16rpx 20rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -2rpx 10rpx rgba(0,0,0,0.05);
}

.quick-actions {
  display: flex;
  gap: 32rpx;
  margin-bottom: 16rpx;

  .action-btn {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f6f8;
    border-radius: 16rpx;
  }
}

.emoji-picker {
  height: 240rpx;
  margin-bottom: 16rpx;

  .emoji-scroll {
    height: 100%;
  }

  .emoji-list {
    display: flex;
    flex-wrap: wrap;
    padding: 20rpx;

    .emoji-item {
      width: 80rpx;
      height: 80rpx;
      font-size: 48rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 16rpx;

  .input-field {
    flex: 1;
    height: 80rpx;
    padding: 0 24rpx;
    background: #f5f6f8;
    border-radius: 40rpx;
    font-size: 28rpx;
  }

  .send-btn {
    width: 120rpx;
    height: 72rpx;
    background: #ddd;
    color: #fff;
    font-size: 28rpx;
    border-radius: 36rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    padding: 0;

    &.active {
      background: #667eea;
    }
  }
}
</style>