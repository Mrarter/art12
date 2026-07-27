<template>
  <view class="bidding-page">
    <view v-if="loading" class="state-page">
      <view class="skeleton hero-skeleton"></view>
      <view class="skeleton line-skeleton"></view>
      <view class="skeleton card-skeleton"></view>
      <text class="state-copy">正在连接拍卖现场...</text>
    </view>

    <view v-else-if="loadError" class="state-page error-state">
      <view class="error-mark">!</view>
      <text class="error-title">拍品加载失败</text>
      <text class="state-copy">{{ loadError }}</text>
      <view class="retry-button" @click="initialize">重新加载</view>
    </view>

    <template v-else>
      <view class="hero">
        <image
          class="hero-image"
          :src="displayImage"
          mode="aspectFill"
          @error="imageFailed = true"
        />
        <view class="hero-shade"></view>
        <view class="hero-topline">
          <view class="lot-chip">LOT {{ lotInfo.lotNo || '--' }}</view>
          <view class="live-chip" :class="statusClass">
            <view v-if="effectiveStatus === 1" class="live-dot"></view>
            {{ statusText }}
          </view>
        </view>
        <view class="hero-copy">
          <text class="artwork-name">{{ lotInfo.title || '未命名拍品' }}</text>
          <text class="artwork-author">{{ lotInfo.artistName || '艺术家信息待补充' }}</text>
          <text v-if="artworkMeta" class="artwork-meta">{{ artworkMeta }}</text>
        </view>
      </view>

      <view class="content-shell">
        <view class="price-panel">
          <view class="price-main">
            <text class="eyebrow">{{ bidCount > 0 ? '当前领先价' : '当前起拍价' }}</text>
            <view class="price-value"><text class="currency">¥</text>{{ formatPrice(currentPrice) }}</view>
            <view class="price-foot">
              <text>起拍 ¥{{ formatPrice(lotInfo.startPrice) }}</text>
              <text class="divider">·</text>
              <text>每次加价 ¥{{ formatPrice(increment) }}</text>
            </view>
          </view>

          <view class="countdown-card" :class="{ urgent: remainSeconds > 0 && remainSeconds < 300 }">
            <text class="countdown-label">{{ countdownLabel }}</text>
            <text class="countdown-value">{{ formatCountdown(remainSeconds) }}</text>
            <text class="countdown-date">{{ formatEndTime(lotInfo.endTime) }}</text>
          </view>
        </view>

        <view class="facts-row">
          <view class="fact">
            <text class="fact-value">{{ bidCount }}</text>
            <text class="fact-label">出价次数</text>
          </view>
          <view class="fact-separator"></view>
          <view class="fact">
            <text class="fact-value">¥{{ formatPrice(minBidPrice) }}</text>
            <text class="fact-label">下一口价</text>
          </view>
          <view class="fact-separator"></view>
          <view class="fact">
            <text class="fact-value">¥{{ formatPrice(lotInfo.depositAmount) }}</text>
            <text class="fact-label">参拍保证金</text>
          </view>
        </view>

        <view class="notice-bar">
          <view class="notice-icon">i</view>
          <text>出价具有约束力，请确认金额后提交。未中标保证金将按原支付渠道退回。</text>
        </view>

        <view class="bids-card">
          <view class="section-header">
            <view>
              <text class="section-kicker">BID HISTORY</text>
              <text class="section-title">出价记录</text>
            </view>
            <text class="bid-count">共 {{ bidTotal }} 次</text>
          </view>

          <view v-if="bidsLoading" class="bids-loading">正在刷新出价...</view>
          <view v-else-if="bids.length" class="bids-list">
            <view v-for="(bid, index) in bids" :key="bid.id || index" class="bid-item">
              <view class="bid-rank" :class="{ leading: index === 0 }">{{ index === 0 ? '领先' : index + 1 }}</view>
              <view class="bid-user-info">
                <text class="bid-nickname">{{ maskedBidder(bid) }}</text>
                <text class="bid-time">{{ formatBidTime(bid.bidTime) }}</text>
              </view>
              <text class="bid-price">¥{{ formatPrice(bid.bidPrice) }}</text>
            </view>
          </view>
          <view v-else class="empty-bids">
            <view class="empty-hammer">◇</view>
            <text class="empty-title">等待第一口价</text>
            <text class="empty-copy">成为本件拍品的首位竞拍者</text>
          </view>
        </view>
      </view>

      <view class="danmu-container">
        <view v-for="(danmu, index) in danmuList" :key="index" class="danmu-item">
          {{ danmu.userName }} 出价 ¥{{ formatPrice(danmu.price) }}
        </view>
      </view>

      <view class="bid-dock">
        <template v-if="effectiveStatus === 1">
          <scroll-view class="quick-scroll" scroll-x :show-scrollbar="false">
            <view class="quick-list">
              <view v-for="amount in quickPrices" :key="amount" class="quick-button" @click="setBidPrice(amount)">
                +{{ formatPrice(amount) }}
              </view>
            </view>
          </scroll-view>
          <view class="bid-row">
            <view class="bid-input-wrap" :class="{ invalid: bidPrice && !canBid }">
              <text class="input-currency">¥</text>
              <input class="bid-input" type="digit" v-model="bidPrice" :placeholder="formatPrice(minBidPrice)" />
            </view>
            <view class="bid-submit" :class="{ disabled: !canBid || submitting }" @click="handleBid">
              {{ submitting ? '提交中' : '确认出价' }}
            </view>
          </view>
          <text class="dock-hint">最低出价 ¥{{ formatPrice(minBidPrice) }}，提交前请再次确认</text>
        </template>
        <view v-else class="closed-action">{{ statusText }}</view>
      </view>
    </template>
  </view>
</template>

<script>
import { getLotDetail, placeBid, getLotBids } from '@/api/auction'
import websocket from '@/utils/websocket.js'
import { formatYuanNumber } from '@/utils/price'
import { guardAuctionAccess } from '@/utils/platform.js'

const FALLBACK_IMAGE = '/static/icons/artwork-default.png'

export default {
  data() {
    return {
      lotId: null,
      lotInfo: {},
      bids: [],
      bidTotal: 0,
      bidPrice: '',
      minBidPrice: 0,
      quickPrices: [],
      remainSeconds: 0,
      timer: null,
      danmuList: [],
      wsConnected: false,
      loading: true,
      bidsLoading: true,
      submitting: false,
      loadError: '',
      imageFailed: false,
      listenersBound: false
    }
  },

  computed: {
    displayImage() {
      return this.imageFailed ? FALLBACK_IMAGE : (this.lotInfo.coverImage || this.lotInfo.image || FALLBACK_IMAGE)
    },
    currentPrice() {
      return Number(this.lotInfo.currentPrice ?? this.lotInfo.startPrice ?? 0)
    },
    increment() {
      return Number(this.lotInfo.bidIncrement ?? this.lotInfo.increment ?? 100)
    },
    bidCount() {
      return Number(this.lotInfo.bidCount || 0)
    },
    effectiveStatus() {
      const now = Date.now()
      const start = this.lotInfo.startTime ? new Date(this.lotInfo.startTime).getTime() : 0
      const end = this.lotInfo.endTime ? new Date(this.lotInfo.endTime).getTime() : 0
      if (start && now < start) return 0
      if (end && now >= end) return 2
      const status = Number(this.lotInfo.status)
      return status === 3 ? 2 : (Number.isFinite(status) ? status : 0)
    },
    canBid() {
      const value = Number(this.bidPrice)
      return this.effectiveStatus === 1 && this.remainSeconds > 0 && Number.isFinite(value) && value >= this.minBidPrice
    },
    statusText() {
      return ({ 0: '即将开始', 1: '竞拍进行中', 2: '竞拍已结束', 3: '竞拍已结束' })[this.effectiveStatus] || '状态待确认'
    },
    statusClass() {
      return { live: this.effectiveStatus === 1, upcoming: this.effectiveStatus === 0, ended: this.effectiveStatus >= 2 }
    },
    countdownLabel() {
      return this.effectiveStatus === 0 ? '距离开始' : (this.effectiveStatus === 1 ? '距离截拍' : '本场状态')
    },
    artworkMeta() {
      const parts = []
      if (this.lotInfo.material || this.lotInfo.medium) parts.push(this.lotInfo.material || this.lotInfo.medium)
      if (this.lotInfo.width && this.lotInfo.height) parts.push(`${this.lotInfo.width} × ${this.lotInfo.height} cm`)
      if (this.lotInfo.year) parts.push(this.lotInfo.year)
      return parts.join(' · ')
    }
  },

  onLoad(options) {
    if (guardAuctionAccess()) return
    this.lotId = Number(options.id)
    if (!Number.isFinite(this.lotId) || this.lotId <= 0) {
      this.loading = false
      this.loadError = '拍品参数无效'
      return
    }
    this.initialize()
  },

  onUnload() {
    this.stopCountdown()
    this.closeWebSocket()
    websocket.offAll()
  },

  methods: {
    async initialize() {
      this.loading = true
      this.loadError = ''
      this.imageFailed = false
      this.stopCountdown()
      this.closeWebSocket()
      try {
        await this.loadLotDetail()
        await this.loadBids()
        this.startCountdown()
        if (this.effectiveStatus === 1) this.initWebSocket()
      } catch (error) {
        console.error('拍品页面初始化失败', error)
        this.loadError = error?.message === 'NOT_FOUND' ? '拍品不存在或已下架' : (error?.message || '网络异常，请稍后重试')
      } finally {
        this.loading = false
      }
    },

    async loadLotDetail() {
      const response = await getLotDetail(this.lotId)
      const detail = response?.data || response
      if (!detail || !detail.id) throw new Error('拍品数据为空')
      this.lotInfo = { ...detail }
      this.syncTimeAndPrice()
    },

    async loadBids() {
      this.bidsLoading = true
      try {
        const response = await getLotBids(this.lotId, 1, 50)
        const page = response?.data || response || {}
        this.bids = Array.isArray(page.records) ? page.records : []
        this.bidTotal = Number(page.total ?? this.bids.length)
      } finally {
        this.bidsLoading = false
      }
    },

    syncTimeAndPrice() {
      const target = this.effectiveStatus === 0 ? this.lotInfo.startTime : this.lotInfo.endTime
      this.remainSeconds = target ? Math.max(0, Math.floor((new Date(target).getTime() - Date.now()) / 1000)) : 0
      this.minBidPrice = this.bidCount > 0 ? this.currentPrice + this.increment : Math.max(this.currentPrice, Number(this.lotInfo.startPrice || 0))
      this.bidPrice = String(this.minBidPrice)
      this.quickPrices = [this.increment, this.increment * 2, this.increment * 5]
    },

    setBidPrice(amount) {
      this.bidPrice = String(this.currentPrice + Number(amount))
    },

    async handleBid() {
      if (this.submitting) return
      if (!this.canBid) {
        uni.showToast({ title: `最低出价 ¥${this.formatPrice(this.minBidPrice)}`, icon: 'none' })
        return
      }
      const price = Number(this.bidPrice)
      const confirmed = await new Promise(resolve => uni.showModal({
        title: '确认出价',
        content: `本次出价 ¥${this.formatPrice(price)}，出价后不可撤销。`,
        confirmText: '确认出价',
        confirmColor: '#b9923b',
        success: result => resolve(result.confirm),
        fail: () => resolve(false)
      }))
      if (!confirmed) return
      this.submitting = true
      try {
        const requestId = `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
        await placeBid(this.lotId, price, requestId)
        uni.showToast({ title: '出价成功' })
        await Promise.all([this.loadLotDetail(), this.loadBids()])
        this.addDanmu({ userName: '我', price })
      } catch (error) {
        uni.showToast({ title: error?.message || '出价失败，请重试', icon: 'none' })
      } finally {
        this.submitting = false
      }
    },

    startCountdown() {
      this.stopCountdown()
      this.timer = setInterval(() => {
        const previousStatus = this.effectiveStatus
        const target = previousStatus === 0 ? this.lotInfo.startTime : this.lotInfo.endTime
        this.remainSeconds = target ? Math.max(0, Math.floor((new Date(target).getTime() - Date.now()) / 1000)) : 0
        if (this.remainSeconds === 0) {
          this.loadLotDetail().then(() => {
            if (this.effectiveStatus === 1 && !this.wsConnected) this.initWebSocket()
            if (this.effectiveStatus >= 2) this.stopCountdown()
          }).catch(() => {})
        }
      }, 1000)
    },

    stopCountdown() {
      if (this.timer) clearInterval(this.timer)
      this.timer = null
    },

    initWebSocket() {
      if (this.wsConnected || this.effectiveStatus !== 1) return
      if (!this.listenersBound) {
        websocket.onBid(this.handleRealtimeBid)
        websocket.onNewBid(this.handleRealtimeBid)
        websocket.onAuctionEnd(this.handleAuctionEnd)
        this.listenersBound = true
      }
      websocket.connectAuction(this.lotId)
        .then(() => { this.wsConnected = true })
        .catch(error => console.warn('实时竞价连接失败，保留手动刷新能力', error))
    },

    handleRealtimeBid(data) {
      if (!data || Number(data.lotId) !== this.lotId) return
      this.lotInfo = { ...this.lotInfo, currentPrice: Number(data.price), bidCount: Number(data.bidCount || this.bidCount + 1) }
      this.syncTimeAndPrice()
      this.loadBids().catch(() => {})
      this.addDanmu({ userName: data.userName || '竞拍者', price: data.price })
    },

    handleAuctionEnd(data) {
      if (!data || Number(data.lotId) !== this.lotId) return
      this.lotInfo = { ...this.lotInfo, status: 2 }
      this.remainSeconds = 0
      this.stopCountdown()
    },

    closeWebSocket() {
      websocket.disconnectAuction()
      this.wsConnected = false
    },

    addDanmu(bid) {
      const item = { userName: bid.userName || '竞拍者', price: bid.price }
      this.danmuList = [...this.danmuList.slice(-2), item]
      setTimeout(() => { this.danmuList = this.danmuList.filter(entry => entry !== item) }, 2600)
    },

    maskedBidder(bid) {
      if (bid.userName) return bid.userName
      const id = String(bid.userId || '')
      return id ? `竞拍者 ${id.slice(-4)}` : '匿名竞拍者'
    },

    formatPrice(value) {
      const number = Number(value)
      return Number.isFinite(number) ? formatYuanNumber(number) : '--'
    },

    formatCountdown(seconds) {
      if (this.effectiveStatus >= 2) return '已结束'
      const total = Math.max(0, Number(seconds || 0))
      const days = Math.floor(total / 86400)
      const hours = Math.floor((total % 86400) / 3600)
      const minutes = Math.floor((total % 3600) / 60)
      const secs = total % 60
      if (days) return `${days}天 ${hours}时 ${minutes}分`
      return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
    },

    formatEndTime(value) {
      if (!value) return '时间待定'
      const date = new Date(value)
      return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')} 截拍`
    },

    formatBidTime(value) {
      if (!value) return '--'
      const date = new Date(value)
      return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
    }
  }
}
</script>

<style lang="scss" scoped>
.bidding-page { min-height: 100vh; background: #0b0c0f; color: #f7f2e8; padding-bottom: calc(248rpx + env(safe-area-inset-bottom)); }
.state-page { min-height: 78vh; padding: 28rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #8d9098; }
.skeleton { width: 100%; border-radius: 22rpx; background: linear-gradient(100deg, #15171c 20%, #20232a 40%, #15171c 60%); background-size: 200% 100%; animation: shimmer 1.3s infinite; }
.hero-skeleton { height: 640rpx; }.line-skeleton { height: 70rpx; margin-top: 24rpx; }.card-skeleton { height: 300rpx; margin-top: 24rpx; }
.state-copy { margin-top: 32rpx; font-size: 25rpx; }.error-mark { width: 88rpx; height: 88rpx; border: 2rpx solid #b9923b; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #d6b86d; font: 600 42rpx serif; }
.error-title { margin-top: 28rpx; color: #f5f1e8; font-size: 34rpx; font-weight: 600; }.retry-button { margin-top: 34rpx; padding: 22rpx 60rpx; border-radius: 999rpx; background: #b9923b; color: #0b0c0f; font-weight: 600; }
.hero { position: relative; height: 680rpx; background: #17191e; overflow: hidden; }.hero-image { width: 100%; height: 100%; }.hero-shade { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(5,5,7,.08) 28%, rgba(8,9,11,.92) 100%); }
.hero-topline { position: absolute; top: 30rpx; left: 28rpx; right: 28rpx; display: flex; justify-content: space-between; align-items: center; }.lot-chip,.live-chip { padding: 12rpx 20rpx; border-radius: 999rpx; backdrop-filter: blur(16px); font-size: 21rpx; letter-spacing: 1rpx; }
.lot-chip { background: rgba(9,10,12,.72); color: #e2c77e; border: 1rpx solid rgba(226,199,126,.28); }.live-chip { display: flex; align-items: center; gap: 10rpx; background: rgba(9,10,12,.72); color: #d9d9d9; }.live-chip.live { color: #f0d78e; }.live-dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: #d8ad4e; box-shadow: 0 0 0 8rpx rgba(216,173,78,.14); animation: pulse 1.5s infinite; }
.hero-copy { position: absolute; left: 30rpx; right: 30rpx; bottom: 34rpx; display: flex; flex-direction: column; }.artwork-name { font-family: serif; font-size: 46rpx; font-weight: 600; letter-spacing: 1rpx; }.artwork-author { margin-top: 12rpx; color: #d8d4ca; font-size: 27rpx; }.artwork-meta { margin-top: 10rpx; color: #92959d; font-size: 22rpx; }
.content-shell { position: relative; margin-top: -2rpx; padding: 26rpx 24rpx 40rpx; }.price-panel,.facts-row,.notice-bar,.bids-card { border: 1rpx solid rgba(255,255,255,.07); background: #14161b; border-radius: 24rpx; }
.price-panel { padding: 32rpx 28rpx; display: flex; gap: 22rpx; }.price-main { flex: 1; min-width: 0; }.eyebrow,.section-kicker { display: block; color: #7f828a; font-size: 19rpx; letter-spacing: 2rpx; }.price-value { margin-top: 8rpx; color: #e0c26f; font-family: serif; font-size: 58rpx; line-height: 1.15; white-space: nowrap; }.currency { margin-right: 4rpx; font-size: 32rpx; }.price-foot { margin-top: 14rpx; color: #8f9299; font-size: 20rpx; display: flex; gap: 9rpx; }.divider { color: #4a4d54; }
.countdown-card { width: 220rpx; padding: 22rpx 18rpx; border-radius: 18rpx; background: #0e1014; border: 1rpx solid rgba(224,194,111,.16); display: flex; flex-direction: column; justify-content: center; }.countdown-card.urgent { border-color: rgba(205,91,75,.55); }.countdown-label { color: #858891; font-size: 19rpx; }.countdown-value { margin-top: 10rpx; color: #eee6d3; font-family: monospace; font-size: 29rpx; font-weight: 700; }.countdown-date { margin-top: 10rpx; color: #686b72; font-size: 18rpx; }
.facts-row { margin-top: 18rpx; padding: 26rpx 8rpx; display: flex; align-items: center; }.fact { flex: 1; min-width: 0; text-align: center; display: flex; flex-direction: column; }.fact-value { color: #e5e1d8; font-size: 27rpx; font-weight: 600; white-space: nowrap; }.fact-label { margin-top: 8rpx; color: #747780; font-size: 19rpx; }.fact-separator { width: 1rpx; height: 50rpx; background: #292c32; }
.notice-bar { margin-top: 18rpx; padding: 22rpx; display: flex; gap: 16rpx; color: #96989f; font-size: 21rpx; line-height: 1.6; }.notice-icon { flex: none; width: 32rpx; height: 32rpx; border: 1rpx solid #7f6b3d; border-radius: 50%; color: #c9aa60; display: flex; justify-content: center; align-items: center; font-family: serif; }
.bids-card { margin-top: 18rpx; padding: 30rpx 26rpx; min-height: 330rpx; }.section-header { display: flex; justify-content: space-between; align-items: flex-end; }.section-title { display: block; margin-top: 5rpx; font-family: serif; font-size: 35rpx; }.bid-count { color: #777a82; font-size: 21rpx; }.bids-loading { padding: 90rpx 0; text-align: center; color: #777a82; font-size: 23rpx; }
.bids-list { margin-top: 24rpx; }.bid-item { min-height: 104rpx; display: flex; align-items: center; border-top: 1rpx solid #24272d; }.bid-rank { width: 68rpx; height: 40rpx; border-radius: 999rpx; background: #23262c; color: #777a82; font-size: 18rpx; display: flex; align-items: center; justify-content: center; }.bid-rank.leading { background: rgba(185,146,59,.16); color: #d6b661; }.bid-user-info { flex: 1; margin-left: 18rpx; display: flex; flex-direction: column; }.bid-nickname { color: #dedad1; font-size: 24rpx; }.bid-time { margin-top: 5rpx; color: #656870; font-size: 18rpx; }.bid-price { color: #e0c26f; font-family: serif; font-size: 30rpx; }
.empty-bids { height: 250rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; }.empty-hammer { color: #5f6269; font-size: 48rpx; }.empty-title { margin-top: 14rpx; color: #bbb8b1; font-size: 26rpx; }.empty-copy { margin-top: 8rpx; color: #60636b; font-size: 20rpx; }
.danmu-container { position: fixed; left: 24rpx; bottom: 260rpx; z-index: 20; pointer-events: none; }.danmu-item { width: fit-content; margin-top: 10rpx; padding: 12rpx 20rpx; border-radius: 999rpx; background: rgba(19,21,26,.9); border: 1rpx solid rgba(224,194,111,.2); color: #e4d2a2; font-size: 21rpx; animation: toastIn 2.6s both; }
.bid-dock { position: fixed; z-index: 30; left: 0; right: 0; bottom: 0; padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom)); background: rgba(13,14,17,.97); border-top: 1rpx solid rgba(255,255,255,.08); backdrop-filter: blur(20px); }.quick-scroll { width: 100%; white-space: nowrap; }.quick-list { display: inline-flex; gap: 12rpx; }.quick-button { padding: 12rpx 24rpx; border: 1rpx solid #383b43; border-radius: 999rpx; color: #b9b7b1; font-size: 21rpx; }
.bid-row { margin-top: 14rpx; display: flex; gap: 14rpx; }.bid-input-wrap { flex: 1; height: 82rpx; display: flex; align-items: center; padding: 0 22rpx; border-radius: 16rpx; background: #191b20; border: 1rpx solid #34373e; }.bid-input-wrap.invalid { border-color: #815047; }.input-currency { color: #9f8750; font-size: 28rpx; }.bid-input { flex: 1; height: 100%; margin-left: 8rpx; color: #f4f0e7; font-size: 33rpx; font-weight: 600; }.bid-submit { width: 220rpx; height: 82rpx; display: flex; align-items: center; justify-content: center; border-radius: 16rpx; background: linear-gradient(135deg,#d8bb6a,#a67f2e); color: #111216; font-size: 27rpx; font-weight: 700; }.bid-submit.disabled { opacity: .38; }.dock-hint { display: block; margin-top: 10rpx; color: #62656d; font-size: 18rpx; text-align: center; }.closed-action { height: 82rpx; display: flex; align-items: center; justify-content: center; border-radius: 16rpx; background: #202228; color: #7f8289; font-size: 27rpx; }
@keyframes shimmer { to { background-position: -200% 0; } } @keyframes pulse { 50% { opacity: .45; } } @keyframes toastIn { 0% { opacity: 0; transform: translateY(20rpx); } 15%,80% { opacity: 1; transform: none; } 100% { opacity: 0; transform: translateY(-10rpx); } }
</style>
