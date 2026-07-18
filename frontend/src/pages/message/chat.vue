<template>
  <view class="chat-page">
    <view class="chat-nav">
      <view class="nav-btn" @click="goBack">‹</view>
      <view class="nav-main">
        <text class="nav-title">{{ chatUser.name }}</text>
        <text class="nav-subtitle">{{ chatUser.online ? '在线，通常很快回复' : chatUser.roleText }}</text>
      </view>
      <view class="nav-btn more" @click="showUserMenu">⋯</view>
    </view>

    <view class="profile-strip">
      <image class="profile-avatar" :src="chatUser.avatar" mode="aspectFill" @error="onAvatarError"></image>
      <view class="profile-main">
        <view class="profile-line">
          <text class="profile-name">{{ chatUser.name }}</text>
          <text class="profile-badge">{{ chatUser.roleText }}</text>
        </view>
        <text class="profile-desc">{{ chatUser.desc }}</text>
      </view>
      <view class="profile-action" @click="goUserHome">主页</view>
    </view>

    <scroll-view
      class="message-container"
      scroll-y
      :scroll-into-view="scrollIntoView"
      :enhanced="true"
      @scrolltoupper="loadMoreMessages"
    >
      <view class="load-more-tip" v-if="loadingMore">加载更多消息...</view>

      <view class="message-list">
        <view
          v-for="(msg, index) in messages"
          :key="msg.id"
          :id="'msg-' + msg.id"
          class="message-item"
          :class="{ mine: msg.isMine, other: !msg.isMine, system: msg.isSystem }"
        >
          <view class="time-divider" v-if="showTimeDivider(index)">
            <text>{{ formatDate(msg.createTime) }}</text>
          </view>

          <view class="system-message" v-if="msg.isSystem">
            <text>{{ msg.content }}</text>
          </view>

          <view class="message-content" v-else>
            <image
              class="avatar"
              :src="msg.isMine ? myAvatar : chatUser.avatar"
              mode="aspectFill"
              @error="msg.isMine ? null : onAvatarError()"
            ></image>

            <view class="bubble-stack">
              <view class="bubble text-bubble" v-if="msg.type === 'text'">
                <text>{{ msg.content }}</text>
              </view>

              <view class="bubble image-bubble" v-else-if="msg.type === 'image'" @click="previewImage(msg.content)">
                <image :src="msg.content" mode="widthFix"></image>
              </view>

              <view class="rich-card work-card" v-else-if="msg.type === 'work'" @click="goWorkDetail(msg.workId)">
                <image class="rich-cover" :src="msg.workCover" mode="aspectFill"></image>
                <view class="rich-main">
                  <text class="rich-kicker">作品咨询</text>
                  <text class="rich-title">{{ msg.workTitle }}</text>
                  <text class="rich-price">¥{{ formatMoney(msg.workPrice) }}</text>
                </view>
              </view>

              <view class="rich-card order-card" v-else-if="msg.type === 'order'" @click="goOrderDetail(msg.orderId)">
                <view class="order-icon">单</view>
                <view class="rich-main">
                  <text class="rich-kicker">订单进度</text>
                  <text class="rich-title">{{ msg.orderNo }}</text>
                  <text class="rich-status">{{ msg.orderStatus }}</text>
                </view>
              </view>

              <view
                class="message-state"
                v-if="msg.isMine"
                :class="{ failed: msg.status === 'failed' }"
                @click="retryMessage(msg)"
              >
                <text>{{ statusText(msg.status) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="quick-replies" v-if="!showEmoji">
      <view class="quick-chip" v-for="item in quickReplies" :key="item" @click="useQuickReply(item)">
        {{ item }}
      </view>
    </view>

    <view class="composer">
      <view class="tool-row">
        <view class="tool-btn" :class="{ active: showEmoji }" aria-label="表情" @click="toggleEmoji">
          <image class="tool-icon" src="/static/art-icons/icon-comment.svg" mode="aspectFit"></image>
        </view>
        <view class="tool-btn" aria-label="发送图片" @click="chooseImage">
          <image class="tool-icon" src="/static/art-icons/icon-work.svg" mode="aspectFit"></image>
        </view>
        <view class="tool-btn" aria-label="发送作品" @click="sendWork">
          <image class="tool-icon" src="/static/art-icons/icon-gallery.svg" mode="aspectFit"></image>
        </view>
        <view class="tool-btn" aria-label="发送订单" @click="sendOrderCard">
          <image class="tool-icon" src="/static/art-icons/icon-document.svg" mode="aspectFit"></image>
        </view>
      </view>

      <view class="emoji-picker" v-if="showEmoji">
        <view class="emoji-grid">
          <text
            v-for="emoji in emojiList"
            :key="emoji"
            class="emoji-item"
            @click="selectEmoji(emoji)"
          >{{ emoji }}</text>
        </view>
      </view>

      <view class="input-row">
        <input
          class="input-field"
          v-model="inputText"
          :placeholder="placeholder"
          :focus="inputFocus"
          confirm-type="send"
          @confirm="sendTextMessage"
          @focus="onInputFocus"
        />
        <button class="send-btn" :class="{ active: canSend }" :disabled="!canSend" @click="sendTextMessage">发送</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { getProductDetail } from '@/api/product'
import { getArtistInfo } from '@/api/user'
import { uploadFile } from '@/api/file'
import { getChatHistory, markChatRead, sendChatMessage } from '@/api/message'
import { useUserStore } from '@/store/modules/user'
import { getCurrentUserIdentity } from '@/utils/auth'
import { getFullImageUrl } from '@/utils/image'
import { formatYuanNumber } from '@/utils/price'

const userStore = useUserStore()
const defaultAvatar = '/static/images/avatar.png'
const formatMoney = (value) => {
  const amount = Number(value)
  return formatYuanNumber(Number.isFinite(amount) ? amount : 0)
}

const chatUser = ref({
  id: '',
  name: '艺术顾问',
  avatar: defaultAvatar,
  online: false,
  roleText: '平台用户',
  desc: '关于作品、收藏、流通和售后都可以直接沟通。'
})

const messages = ref([])

const inputText = ref('')
const showEmoji = ref(false)
const inputFocus = ref(false)
const scrollIntoView = ref('')
const loadingMore = ref(false)
const currentWork = ref(null)
const historyPage = ref(1)
const historyHasMore = ref(true)
const historyRefreshing = ref(false)
let refreshTimer = null

const myAvatar = computed(() => getFullImageUrl(userStore.userInfo?.avatar || defaultAvatar, defaultAvatar))
const canSend = computed(() => inputText.value.trim().length > 0)
const placeholder = computed(() => `发送给 ${chatUser.value.name}`)

const quickReplies = ['作品还在吗', '可以介绍一下吗', '证书怎么查看', '支付后多久确认']
const emojiList = ['🙂', '😊', '👍', '🙏', '👏', '🤝', '🎨', '✨', '❤️', '👌', '🙌', '💬']

const decodeText = (value = '') => {
  try {
    return decodeURIComponent(value)
  } catch (e) {
    return value
  }
}

const parseExtraData = (value) => {
  if (!value) return {}
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch (e) {
    return {}
  }
}

const parseDateTime = (value) => {
  if (typeof value === 'number') return value
  const timestamp = new Date(String(value || '').replace(' ', 'T')).getTime()
  return Number.isFinite(timestamp) ? timestamp : Date.now()
}

const normalizeMessage = (message = {}) => {
  const extra = parseExtraData(message.extraData)
  const currentUserId = getCurrentUserIdentity().id
  return {
  id: message.id || Date.now() + Math.floor(Math.random() * 1000),
  type: message.messageType || message.type || 'text',
  content: message.content || '',
  isMine: message.senderId != null ? String(message.senderId) === String(currentUserId) : !!message.isMine,
  isSystem: !!message.isSystem,
  createTime: parseDateTime(message.createTime),
  status: message.status || 'sent',
  workId: message.workId || extra.workId,
  workCover: message.workCover || extra.workCover,
  workTitle: message.workTitle || extra.workTitle,
  workPrice: message.workPrice ?? extra.workPrice,
  orderId: message.orderId || extra.orderId,
  orderNo: message.orderNo || extra.orderNo,
  orderStatus: message.orderStatus || extra.orderStatus
  }
}

const buildWorkMessageFromOptions = (options = {}) => {
  const workId = options.workId || options.artworkId || options.productId || ''
  if (!workId) return null
  return {
    id: `work-${workId}`,
    type: 'work',
    workId,
    workCover: decodeText(options.workCover || options.cover || '/static/images/artwork-fallback.png'),
    workTitle: decodeText(options.workTitle || options.title || '咨询作品'),
    workPrice: options.workPrice || options.price || 0,
    isMine: false,
    createTime: Date.now() - 1000 * 60 * 3,
    status: 'sent'
  }
}

const readRouteOptions = () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const pageOptions = currentPage?.options || {}
  if (typeof window === 'undefined') return pageOptions
  const query = window.location.href.split('?')[1]?.split('#')[0] || window.location.hash.split('?')[1] || ''
  return { ...Object.fromEntries(new URLSearchParams(query)), ...pageOptions }
}

const goBack = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.redirectTo({ url: '/pages/message/list' })
}

const goUserHome = () => {
  if (!chatUser.value.id) return
  uni.navigateTo({ url: `/pages/artist/home?userId=${chatUser.value.id}` })
}

const showUserMenu = () => {
  uni.showActionSheet({
    itemList: ['查看主页', '举报用户'],
    success: ({ tapIndex }) => {
      if (tapIndex === 0) goUserHome()
      if (tapIndex === 1) uni.showToast({ title: '已收到反馈', icon: 'none' })
    }
  })
}

const showTimeDivider = (index) => {
  if (index === 0) return true
  return messages.value[index].createTime - messages.value[index - 1].createTime > 5 * 60 * 1000
}

const formatDate = (timestamp) => {
  const date = new Date(timestamp)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const target = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  if (target === today) return `今天 ${time}`
  if (target === today - 86400000) return `昨天 ${time}`
  return `${date.getMonth() + 1}月${date.getDate()}日 ${time}`
}

const statusText = (status) => {
  if (status === 'sending') return '发送中'
  if (status === 'failed') return '未送达'
  return '已送达'
}

const toggleEmoji = () => {
  showEmoji.value = !showEmoji.value
  inputFocus.value = !showEmoji.value
}

const selectEmoji = (emoji) => {
  inputText.value += emoji
}

const onInputFocus = () => {
  showEmoji.value = false
}

const finishMessageSending = (messageId, status = 'sent', serverMessage = null) => {
  const msg = messages.value.find(item => item.id === messageId)
  if (msg) {
    if (serverMessage?.id) msg.id = serverMessage.id
    if (serverMessage?.createTime) msg.createTime = parseDateTime(serverMessage.createTime)
    msg.status = status
  }
}

const messageExtraData = (message = {}) => {
  if (message.type === 'work') {
    return {
      workId: message.workId,
      workCover: message.workCover,
      workTitle: message.workTitle,
      workPrice: message.workPrice
    }
  }
  if (message.type === 'order') {
    return {
      orderId: message.orderId,
      orderNo: message.orderNo,
      orderStatus: message.orderStatus
    }
  }
  return null
}

const pushMineMessage = async (message, existingId = null) => {
  const id = existingId || `pending-${Date.now()}-${Math.floor(Math.random() * 1000)}`
  if (!existingId) messages.value.push({
    ...message,
    id,
    isMine: true,
    createTime: Date.now(),
    status: 'sending'
  })
  scrollToBottom()
  try {
    const serverMessage = await sendChatMessage({
      recipientId: Number(chatUser.value.id),
      messageType: message.type || 'text',
      content: message.content || message.workTitle || message.orderNo || '',
      extraData: messageExtraData(message) ? JSON.stringify(messageExtraData(message)) : null
    })
    finishMessageSending(id, 'sent', serverMessage)
  } catch (e) {
    finishMessageSending(id, 'failed')
    uni.showToast({ title: e?.message || '发送失败，请重试', icon: 'none' })
  }
  return id
}

const sendTextMessage = () => {
  const content = inputText.value.trim()
  if (!content) return
  inputText.value = ''
  showEmoji.value = false
  pushMineMessage({ type: 'text', content })
}

const useQuickReply = (text) => {
  inputText.value = text
  sendTextMessage()
}

const chooseImage = () => {
  uni.chooseImage({
    count: 1,
    success: async ({ tempFilePaths }) => {
      if (!tempFilePaths?.[0]) return
      try {
        uni.showLoading({ title: '上传中' })
        const uploaded = await uploadFile(tempFilePaths[0], 'image')
        const url = uploaded?.url || uploaded?.fileUrl || uploaded?.path || uploaded
        if (!url || typeof url !== 'string') throw new Error('图片地址无效')
        await pushMineMessage({ type: 'image', content: getFullImageUrl(url, url) })
      } catch (e) {
        uni.showToast({ title: e?.message || '图片发送失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

const sendWork = () => {
  const work = currentWork.value
  if (!work?.workId) {
    uni.showToast({ title: '请从作品详情进入聊天后发送', icon: 'none' })
    return
  }
  pushMineMessage({
    type: 'work',
    workId: work.workId,
    workCover: work.workCover,
    workTitle: work.workTitle,
    workPrice: work.workPrice
  })
}

const sendOrderCard = () => {
  uni.showToast({ title: '请从订单详情发送订单', icon: 'none' })
}

const retryMessage = (msg) => {
  if (!msg || msg.status !== 'failed') return
  msg.status = 'sending'
  pushMineMessage({ ...msg }, msg.id)
}

const previewImage = (url) => {
  uni.previewImage({ urls: [url] })
}

const goWorkDetail = (workId) => {
  uni.navigateTo({ url: `/pages/gallery/detail?id=${workId}` })
}

const goOrderDetail = (orderId) => {
  if (!orderId) return
  uni.navigateTo({ url: `/pages/order/detail?id=${orderId}` })
}

const fetchHistory = async (reset = false, silent = false) => {
  if (!chatUser.value.id || loadingMore.value || historyRefreshing.value) return
  if (reset) {
    historyPage.value = 1
    historyHasMore.value = true
  }
  if (silent) historyRefreshing.value = true
  else loadingMore.value = true
  try {
    const result = await getChatHistory(chatUser.value.id, {
      page: historyPage.value,
      pageSize: 30
    })
    const records = Array.isArray(result?.records) ? result.records.map(normalizeMessage) : []
    if (reset) {
      const pending = messages.value.filter(item => String(item.id).startsWith('pending-'))
      messages.value = [...records, ...pending]
    } else {
      messages.value = [
        ...records.filter(item => !messages.value.some(row => String(row.id) === String(item.id))),
        ...messages.value
      ]
    }
    historyHasMore.value = historyPage.value < Number(result?.totalPages || 0)
    if (historyHasMore.value) historyPage.value += 1
    await markChatRead(chatUser.value.id)
  } catch (e) {
    if (!silent) uni.showToast({ title: e?.message || '聊天记录加载失败', icon: 'none' })
  } finally {
    loadingMore.value = false
    historyRefreshing.value = false
  }
}

const loadMoreMessages = () => {
  if (!historyHasMore.value || loadingMore.value) return
  fetchHistory(false)
}

const scrollToBottom = () => {
  nextTick(() => {
    const last = messages.value[messages.value.length - 1]
    scrollIntoView.value = ''
    setTimeout(() => {
      if (last) scrollIntoView.value = `msg-${last.id}`
    }, 80)
  })
}

const onAvatarError = () => {
  chatUser.value.avatar = defaultAvatar
}

const hydrateChatUser = async (options) => {
  const userId = options.userId || options.id || ''
  chatUser.value.id = userId
  if (options.name) chatUser.value.name = decodeText(options.name)
  if (options.avatar) {
    const avatar = decodeText(options.avatar)
    chatUser.value.avatar = getFullImageUrl(avatar, defaultAvatar)
  }
  if (options.role) chatUser.value.roleText = decodeText(options.role)
  if (!userId) return

  try {
    const info = await getArtistInfo(userId)
    const name = info?.nickname || info?.name || info?.realName || info?.artistName
    if (name) chatUser.value.name = name
    chatUser.value.avatar = getFullImageUrl(info?.avatar || info?.avatarUrl || defaultAvatar, defaultAvatar)
    chatUser.value.roleText = info?.artistTitle || info?.identityTypeLabel || (info?.isArtist ? '认证艺术家' : '平台用户')
    chatUser.value.desc = info?.bio || info?.signature || chatUser.value.desc
  } catch (e) {
    chatUser.value.name = options.name ? decodeText(options.name) : `用户 ${userId}`
  }
}

const hydrateWorkContext = async (options = {}) => {
  const workId = options.workId || options.artworkId || options.productId || ''
  if (!workId) return null

  const existing = buildWorkMessageFromOptions(options)
  if (existing && existing.workTitle !== '咨询作品') {
    currentWork.value = existing
    return existing
  }

  try {
    const detail = await getProductDetail(workId)
    const workMessage = {
      id: `work-${workId}`,
      type: 'work',
      workId,
      workCover: getFullImageUrl(detail.coverImage || detail.cover || '/static/images/artwork-fallback.png', '/static/images/artwork-fallback.png'),
      workTitle: detail.title || detail.name || '咨询作品',
      workPrice: detail.price || detail.currentPrice || detail.publishPrice || 0,
      isMine: false,
      createTime: Date.now() - 1000 * 60 * 3,
      status: 'sent'
    }
    currentWork.value = workMessage
    return workMessage
  } catch (e) {
    if (existing) currentWork.value = existing
    return existing
  }
}

const initializeMessages = async (options = {}) => {
  await hydrateWorkContext(options)
  await fetchHistory(true)
}

onMounted(async () => {
  if (!userStore.isLogin) {
    uni.showToast({ title: '登录后才能使用私信', icon: 'none' })
    setTimeout(() => uni.redirectTo({ url: '/pages/login/index' }), 600)
    return
  }
  const options = readRouteOptions()
  await hydrateChatUser(options)
  await initializeMessages(options)
  scrollToBottom()
  refreshTimer = setInterval(() => fetchHistory(true, true), 5000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  refreshTimer = null
})
</script>

<style lang="scss" scoped>
.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background:
    radial-gradient(circle at 50% -10%, rgba(218, 176, 72, 0.18), transparent 36%),
    #0b0b0b;
  color: #f7f2e8;
}

.chat-nav {
  flex: 0 0 auto;
  height: 92rpx;
  padding: 0 22rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.07);
  background: rgba(10, 10, 10, 0.96);
  box-sizing: border-box;
}

.nav-btn {
  width: 58rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f7f2e8;
  font-size: 44rpx;
  line-height: 1;
}

.nav-btn.more {
  font-size: 38rpx;
  padding-bottom: 10rpx;
}

.nav-main {
  min-width: 0;
  flex: 1;
  text-align: center;
}

.nav-title,
.nav-subtitle,
.profile-name,
.profile-desc,
.rich-title,
.rich-price,
.rich-status,
.message-state text {
  display: block;
}

.nav-title {
  color: #fff;
  font-size: 30rpx;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-subtitle {
  margin-top: 4rpx;
  color: rgba(247, 242, 232, 0.46);
  font-size: 19rpx;
}

.profile-strip {
  flex: 0 0 auto;
  margin: 18rpx 20rpx 0;
  padding: 18rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.045);
}

.profile-avatar,
.avatar {
  border-radius: 50%;
  background: #242424;
}

.profile-avatar {
  width: 78rpx;
  height: 78rpx;
  flex: 0 0 78rpx;
  border: 1rpx solid rgba(218, 176, 72, 0.55);
}

.profile-main {
  min-width: 0;
  flex: 1;
}

.profile-line {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.profile-name {
  min-width: 0;
  color: #fff;
  font-size: 27rpx;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-badge {
  flex: 0 0 auto;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(218, 176, 72, 0.42);
  color: #d8b84f;
  font-size: 18rpx;
  font-weight: 700;
}

.profile-desc {
  margin-top: 7rpx;
  color: rgba(247, 242, 232, 0.5);
  font-size: 21rpx;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-action {
  flex: 0 0 auto;
  height: 52rpx;
  padding: 0 18rpx;
  display: flex;
  align-items: center;
  border-radius: 999rpx;
  background: rgba(218, 176, 72, 0.12);
  color: #d8b84f;
  font-size: 22rpx;
  font-weight: 800;
}

.message-container {
  flex: 1;
  min-height: 0;
  padding: 18rpx 20rpx 0;
  box-sizing: border-box;
}

.load-more-tip,
.time-divider,
.system-message {
  text-align: center;
}

.load-more-tip {
  padding: 16rpx 0;
  color: rgba(247, 242, 232, 0.42);
  font-size: 22rpx;
}

.message-list {
  padding-bottom: 24rpx;
}

.message-item {
  margin-bottom: 24rpx;
}

.time-divider {
  margin-bottom: 18rpx;
}

.time-divider text,
.system-message text {
  display: inline-flex;
  max-width: 86%;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.07);
  color: rgba(247, 242, 232, 0.42);
  font-size: 19rpx;
}

.message-content {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}

.message-item.mine .message-content {
  flex-direction: row-reverse;
}

.avatar {
  width: 68rpx;
  height: 68rpx;
  flex: 0 0 68rpx;
}

.bubble-stack {
  max-width: 76%;
  min-width: 0;
}

.bubble {
  padding: 18rpx 22rpx;
  border-radius: 18rpx;
  font-size: 26rpx;
  line-height: 1.5;
  word-break: break-word;
}

.message-item.other .text-bubble {
  border-top-left-radius: 6rpx;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(247, 242, 232, 0.9);
}

.message-item.mine .text-bubble {
  border-top-right-radius: 6rpx;
  background: linear-gradient(135deg, #e1bd4c, #b98916);
  color: #1b1608;
  font-weight: 650;
}

.image-bubble {
  padding: 6rpx;
  background: rgba(255, 255, 255, 0.08);
}

.image-bubble image {
  width: 330rpx;
  max-width: 100%;
  border-radius: 14rpx;
  display: block;
}

.rich-card {
  width: 430rpx;
  max-width: 100%;
  display: flex;
  gap: 16rpx;
  padding: 14rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.08);
  box-sizing: border-box;
}

.message-item.mine .rich-card {
  background: rgba(218, 176, 72, 0.14);
}

.rich-cover {
  width: 112rpx;
  height: 112rpx;
  flex: 0 0 112rpx;
  border-radius: 10rpx;
  background: #242424;
}

.rich-main {
  min-width: 0;
  flex: 1;
}

.rich-kicker {
  color: rgba(247, 242, 232, 0.44);
  font-size: 19rpx;
}

.rich-title {
  margin-top: 8rpx;
  color: #fff;
  font-size: 24rpx;
  font-weight: 800;
  line-height: 1.32;
}

.rich-price,
.rich-status {
  margin-top: 8rpx;
  color: #f0d36a;
  font-size: 25rpx;
  font-weight: 850;
}

.order-icon {
  width: 70rpx;
  height: 70rpx;
  flex: 0 0 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(218, 176, 72, 0.18);
  color: #f0d36a;
  font-size: 25rpx;
  font-weight: 900;
}

.message-state {
  margin-top: 8rpx;
  color: rgba(247, 242, 232, 0.34);
  font-size: 18rpx;
  text-align: right;
}

.message-state.failed {
  color: #ff8a76;
}

.typing-bubble {
  display: inline-flex;
  gap: 8rpx;
  padding: 20rpx 24rpx;
  border-top-left-radius: 6rpx;
  background: rgba(255, 255, 255, 0.1);
}

.typing-bubble text {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: rgba(247, 242, 232, 0.62);
  animation: typingPulse 1s infinite ease-in-out;
}

.typing-bubble text:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-bubble text:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typingPulse {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-4rpx);
  }
}

.quick-replies {
  flex: 0 0 auto;
  padding: 8rpx 20rpx 14rpx;
  display: flex;
  gap: 12rpx;
  overflow-x: auto;
  white-space: nowrap;
  box-sizing: border-box;
  scrollbar-width: none;
}

.quick-replies::-webkit-scrollbar {
  display: none;
}

.quick-chip {
  flex: 0 0 auto;
  height: 48rpx;
  padding: 0 18rpx;
  display: flex;
  align-items: center;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.055);
  color: rgba(247, 242, 232, 0.66);
  font-size: 21rpx;
}

.composer {
  flex: 0 0 auto;
  padding: 14rpx 18rpx calc(16rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
  background: rgba(13, 13, 13, 0.98);
  box-sizing: border-box;
}

.tool-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 14rpx;
}

.tool-btn {
  width: 58rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 1rpx solid rgba(242, 193, 78, 0.18);
  background: rgba(255, 255, 255, 0.07);
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.08);
  box-sizing: border-box;
}

.tool-btn.active {
  border-color: rgba(242, 193, 78, 0.48);
  background: rgba(242, 193, 78, 0.16);
}

.tool-icon {
  width: 31rpx;
  height: 31rpx;
  display: block;
}

.emoji-picker {
  max-height: 178rpx;
  margin-bottom: 12rpx;
  padding: 10rpx;
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.055);
  overflow: hidden;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8rpx;
}

.emoji-item {
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
}

.input-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.input-field {
  min-width: 0;
  flex: 1;
  height: 76rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.09);
  color: #fff;
  font-size: 26rpx;
  box-sizing: border-box;
}

.send-btn {
  width: 118rpx;
  height: 72rpx;
  padding: 0;
  border: 0;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(247, 242, 232, 0.42);
  font-size: 25rpx;
  font-weight: 800;
}

.send-btn.active {
  background: linear-gradient(135deg, #e1bd4c, #b98916);
  color: #181208;
}

.send-btn::after {
  border: 0;
}
</style>
